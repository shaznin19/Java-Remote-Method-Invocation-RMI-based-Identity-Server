package p4.server;

import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;

/**
 * The server implementation for client-side operations in the ID server application.
 * <p>
 * This class implements the {@link IdServerInterfaceForClient} interface and provides methods
 * for creating, modifying, and deleting users on the client side. It also manages the Lamport clock
 * for synchronization purposes.
 * </p>
 *
 * @author Sheikh Md Mushfiqur Rahman & Shaznin Sultana
 * @version 1.0
 */
public class IdServerForClient implements IdServerInterfaceForClient {

    private static final long serialVersionUID = 8510538827054962873L;
    public static int REGISTRY_PORT;
    public static final int PREFERRED_PORT = 5005;
    public static int SERVER_SIDE_REGISTRY_PORT;
    // Define a lock object
    private final Object lock = new Object();
    private String LAMPORT_KEY = "lamport_key";
    /**
     * list to hold all servers informations
     */
    private List<String> serverAddrs = new ArrayList<>();

    /**
     * variable to save server's own address
     */
    private String serverAddr;

    /**
     * loads the redis pool
     *
     * @throws RemoteException
     */
    public IdServerForClient(String serverAddr, int REGISTRY_PORT, int SERVER_SIDE_REGISTRY_PORT,
                             List<String> serverAddrs)
            throws RemoteException, NotBoundException {
        super();
        /*
         * setting server address and ports for fixed rmi port
         */
        this.serverAddr = serverAddr;
        this.REGISTRY_PORT = REGISTRY_PORT;
        this.SERVER_SIDE_REGISTRY_PORT = SERVER_SIDE_REGISTRY_PORT;
        JedisPool pool = new JedisPool("localhost", 6379);

        /**
         * adding all server info
         */
        this.serverAddrs = serverAddrs;

        try (Jedis jedis = pool.getResource()) {

            Set<String> userKeys = jedis.keys("user-*"); // Get all keys from the hash
            System.out.println("Already Saved User:" + userKeys.size());
            pool.close();
        }
    }

    /**
     * Binds the current instance of the class as a remote object in the RMI
     * registry
     * with the specified name.
     * <p>
     * This method creates an RMI registry if it doesn't exist at the specified
     * port.
     * It then exports the current object using a fixed port SSL
     * RMIClientSocketFactory
     * and a fixed port SSL RMIServerSocketFactory. Finally, it binds the object to
     * the registry with the specified name.
     * </p>
     *
     * @param name the name to bind the object to in the RMI registry.
     */
    public void bind(String name) {

        try {
            Registry registry = LocateRegistry.createRegistry(IdServerForClient.REGISTRY_PORT);

            RMIClientSocketFactory rmiClientSocketFactory = new FixedPortSslRMIClientSocketFactory();
            RMIServerSocketFactory rmiServerSocketFactory = new FixedPortSslRMIServerSocketFactory();
            IdServerInterfaceForClient obj = (IdServerInterfaceForClient) UnicastRemoteObject.exportObject(this, 0,
                    rmiClientSocketFactory,
                    rmiServerSocketFactory);

            registry.rebind(name, obj);
            System.out.println(name + ": " + name + " bound in registry on port " + REGISTRY_PORT + " on this host");
        } catch (RemoteException e) {
            System.out.println("IdServer: " + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("here " + e);
        }
    }

    /**
     * an encryption method that uses SHA-512
     *
     * @param input the origina format password
     * @return the hashed password
     */
    private static String trySHA(String input) {

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        byte[] bytes = input.getBytes();
        md.reset();

        byte[] result = md.digest(bytes);

        StringBuilder sb = new StringBuilder();
        for (byte b : result) {
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }

    /**
     * Retrieves a set of User objects from a Redis database.
     * <p>
     * This method connects to a local Redis instance and fetches all saved user
     * data
     * stored under keys matching the pattern "user-*". It then iterates over the
     * retrieved
     * user data, converts it into User objects, and adds them to a HashSet.
     * Finally, it returns
     * the HashSet containing the User objects.
     * </p>
     *
     * @return a HashSet containing User objects retrieved from the Redis database.
     */
    private HashSet<User> getUsersFromRedis() {
        HashSet<User> users = new HashSet<User>();
        JedisPool pool = new JedisPool("localhost", 6379);
        try (Jedis jedis = pool.getResource()) {
            /**
             * fetch all saved users from redis
             */
            // Example: Get all user keys
            Set<String> userKeys = jedis.keys("user-*");

            // Iterate over user keys and retrieve user data
            for (String userKey : userKeys) {
                Map<String, String> userData = jedis.hgetAll(userKey);
                // Print user data
                System.out.println("User data for key " + userKey + ":");
                User user = User.userFromMap(userData);
                /**
                 * now adding it to the existing users list
                 */
                users.add(user);
            }
        }
        pool.close();
        return users;
    }

    /**
     * Creates a new user with the provided login name, real name, and password.
     * <p>
     * This method synchronizes on a lock object to ensure thread safety.
     * It prints debug information regarding the server address and client host.
     * It fetches all existing users from Redis and checks if the provided login
     * name
     * is already in use. If the login name is not in use, it proceeds to create the
     * new user.
     * </p>
     *
     * @param loginName the login name for the new user.
     * @param realName  the real name for the new user.
     * @param password  the password for the new user.
     * @return a response indicating the success or failure of the user creation
     * process.
     * @throws RemoteException if there is a problem accessing the remote object.
     */
    @Override
    public synchronized String createUser(String loginName, String realName, String password) throws RemoteException {

        try {
            System.out.println(
                    "getting a create user request in " + serverAddr + ", from " + RemoteServer.getClientHost());
        } catch (ServerNotActiveException e) {
            System.out.println("check line 1");
            e.printStackTrace();
        }
        /**
         * fetch all users from redis
         */
        System.out.println("check line 2");

        HashSet<User> users = new HashSet<User>();
        System.out.println("check line 3");

        users = getUsersFromRedis();
        /*
         * check if someone already uing the name
         */
        System.out.println("check line 4");

        for (User user : users) {
            if (user.getLoginName().equals(loginName)) {
                return "the new login name is already in use.";
            }
        }
        /**
         * creating an UUID, timestampt and getting the password
         */
        UUID uuid = UUID.randomUUID();
        String encryptedPassword = trySHA(password);
        LocalDateTime createdAt = LocalDateTime.now();
        System.out.println("check line 5");

        try {

            String ipAddress = RemoteServer.getClientHost();

            /*
             * connecting to jedispool
             */
            JedisPool pool = new JedisPool("localhost", 6379);
            Jedis jedis = pool.getResource();
            /*
             * creating and adding the user on redis
             */

            User user = new User(loginName, realName, encryptedPassword, uuid, ipAddress, createdAt, createdAt);

            System.out.println("check line 9");
            setUser(user, jedis);

            /**
             * incrementing my lamport clock.
             */
            try {
                incrementLamportClock();
            } catch (RemoteException e) {
                System.out.println("Exception in incrementLamportClock: " + e.getMessage());
                e.printStackTrace();
            } catch (NotBoundException e) {
                System.out.println("Exception in incrementLamportClock: " + e.getMessage());
                e.printStackTrace();
            }
            // saveOperation(loginName, realName, password, ipAddress, uuid, createdAt);
            pool.close();

            System.out.println("updating other server one by one");
            /**
             * getting lamport clock value
             */
            int lamportClockValue = getLamportClockValue();

            for (String otherServerAddr : serverAddrs) {
                /**
                 * I wont send it to my self.
                 */
                if (!serverAddr.equals(otherServerAddr)) {
                    System.out.println("updating " + otherServerAddr);
                    try {
                        Registry registry = LocateRegistry.getRegistry(otherServerAddr, SERVER_SIDE_REGISTRY_PORT);
                        IdServerInterfaceForServer stub = (IdServerInterfaceForServer) registry
                                .lookup("IdServerForServer");

                        String response = stub.createUser(loginName, realName, password, ipAddress, uuid, createdAt,
                                lamportClockValue);

                        System.out.println("response: " + response);

                    } catch (NotBoundException e) {
                        System.out.println("some error " + e);
                        continue;
                    } catch (RemoteException e) {
                        System.out.println("some error " + e);
                        continue;
                    }

                }
            }

            /**
             * send client the uuid
             */
            return uuid.toString();

        } catch (ServerNotActiveException e) {
            System.out.println("Exception in createUser of Server: " + e.getMessage());
            return "some error occured";

        } catch (JsonSyntaxException e) {
            System.err.println("Error occurred while serializing user object to JSON: " + e.getMessage());
            return "some error occured";

        }

    }

    /**
     * Stores the information of a user in a Redis database.
     * <p>
     * This method constructs a HashMap containing the information of the provided
     * user,
     * such as login name, real name, encrypted password, UUID, IP address, creation
     * timestamp,
     * and update timestamp. It then stores this user information in Redis under a
     * key derived
     * from the user's UUID.
     * </p>
     *
     * @param user  the user object whose information is to be stored in Redis.
     * @param jedis the Jedis object representing the connection to the Redis
     *              database.
     */
    private static void setUser(User user, Jedis jedis) {
        /**
         * saving user
         */
        // Store & Retrieve a HashMap
        Map<String, String> userMap = new HashMap<String, String>();
        userMap.put("loginName", user.getLoginName());
        userMap.put("realName", user.getRealName());
        userMap.put("encryptedPassword", user.getEncryptedPassword());
        userMap.put("uuid", String.valueOf(user.getUuid()));
        userMap.put("ipAddress", user.getIpAddress());
        userMap.put("createdAt", String.valueOf(user.getCreatedAt()));
        userMap.put("updatedAt", user.getUpdatedAt().toString());
        /**
         * saving user map
         */
        jedis.hset("user-" + user.getUuid().toString(), userMap);
    }

    /**
     * Increments the value of the Lamport clock stored in a Redis database.
     * <p>
     * This method retrieves the current Lamport clock value from Redis, increments
     * it by one,
     * and then sets the updated value back into the Redis database.
     * </p>
     *
     * @throws RemoteException   if there is a problem accessing the remote object.
     * @throws NotBoundException if there is an issue accessing the remote object's
     *                           registry.
     */
    private void incrementLamportClock() throws RemoteException, NotBoundException {
        int lamportClockValue = getLamportClockValue();
        lamportClockValue++;
        setLamportClockValue(lamportClockValue);
    }

    /**
     * Retrieves a string representation of all UUIDs associated with users stored
     * in Redis.
     *
     * @return a string representing all UUIDs retrieved from the Redis database.
     * @throws RemoteException if there is a problem accessing the remote object.
     */
    public String allUUIds() throws RemoteException {
        HashSet<UUID> uuids = new HashSet<UUID>();
        /**
         * fetch all saved users from redis
         */
        HashSet<User> users = new HashSet<User>();
        users = getUsersFromRedis();
        for (User user : users) {
            uuids.add(user.uuid);
        }
        return uuids.toString();
    }

    /**
     * Retrieves a string representation of all user names and real names stored in
     * Redis.
     *
     * @return a string representing all user names and real names retrieved from
     * the Redis database.
     * @throws RemoteException if there is a problem accessing the remote object.
     */
    public String allUsers() throws RemoteException {
        HashSet<String> userNames = new HashSet<String>();
        /**
         * fetch all saved users from redis
         */
        HashSet<User> users = new HashSet<User>();
        users = getUsersFromRedis();
        for (User user : users) {
            userNames.add(user.loginName + "(" + user.realName + ")");
        }
        return userNames.toString();
    }

    /**
     * Retrieves a string representation of all user details stored in Redis.
     *
     * @return a string representing all user details retrieved from the Redis
     * database.
     * @throws RemoteException if there is a problem accessing the remote object.
     */
    public String all() throws RemoteException {
        HashSet<String> userNames = new HashSet<String>();
        /**
         * fetch all saved users from redis
         */
        HashSet<User> users = new HashSet<User>();
        users = getUsersFromRedis();
        for (User user : users) {
            userNames.add("user: " + user.loginName + "(" + user.realName + ")" + "\tUUID: " + user.uuid
                    + "\tIp Address: " + user.ipAddress + "\tcreated at: " + user.createdAt + "\tmodified at: "
                    + user.updatedAt + "\n");
        }
        return userNames.toString();
    }

    /**
     * Retrieves a string representation of user information based on the provided
     * login name.
     * <p>
     * This method retrieves all users from the Redis database and searches for a
     * user with
     * the specified login name. If a matching user is found, its string
     * representation is added
     * to the result set. Finally, the method returns a string containing the
     * information of all
     * matching users found.
     * </p>
     *
     * @param loginName the login name to search for.
     * @return a string representing user information based on the provided login
     * name.
     * @throws RemoteException if there is a problem accessing the remote object.
     */
    @Override
    public String lookup(String loginName) throws RemoteException {
        HashSet<String> matchingUsers = new HashSet<String>();
        /**
         * fetch all saved users from redis
         */
        HashSet<User> users = new HashSet<User>();
        users = getUsersFromRedis();
        for (User user : users) {
            if (user.loginName.equals(loginName)) {
                matchingUsers.add(user.toString());
            }
        }
        return matchingUsers.toString();
    }

    /**
     * Retrieves a string representation of user information based on the provided
     * UUID.
     * <p>
     * This method retrieves all users from the Redis database and searches for a
     * user with
     * the specified UUID. If a matching user is found, its string representation is
     * added
     * to the result set. Finally, the method returns a string containing the
     * information of all
     * matching users found.
     * </p>
     *
     * @param uuid the UUID to search for.
     * @return a string representing user information based on the provided UUID.
     * @throws RemoteException if there is a problem accessing the remote object.
     */
    @Override
    public String reverseLookup(String uuid) throws RemoteException {
        HashSet<String> matchingUuid = new HashSet<String>();
        UUID targetUUID = UUID.fromString(uuid);
        /**
         * fetch all saved users from redis
         */
        HashSet<User> users = new HashSet<User>();
        users = getUsersFromRedis();
        for (User user : users) {
            if (user.uuid.equals(targetUUID)) {
                matchingUuid.add(user.toString());
            }
        }
        return matchingUuid.toString();
    }

    /**
     * Modifies the login name of a user in the database if the provided credentials match,
     * and propagates the modification to other servers.
     *
     * @param loginName    The current login name of the user.
     * @param newLoginName The new login name to set for the user.
     * @param password     The password of the user.
     * @return A message indicating the result of the modification attempt.
     * @throws RemoteException If an error occurs while communicating with the database or other servers.
     */

    public synchronized String modify(String loginName, String newLoginName, String password) throws RemoteException {
        synchronized (lock) {

            // Method implementation
            /**
             * fetch all saved users from redis
             */
            HashSet<User> users = new HashSet<User>();
            users = getUsersFromRedis();
            /*
             * check if someone already uing the new name
             */
            for (User user : users) {
                if (user.loginName.equals(newLoginName)) {
                    return "the new login name is already in use.";
                }
            }
            String encryptedPassword = trySHA(password);
            for (User user : users) {
                if (user.loginName.equals(loginName)) {
                    if (encryptedPassword.equals(user.encryptedPassword)) {

                        LocalDateTime updatedAt = LocalDateTime.now();

                        user.setLoginName(newLoginName, updatedAt);
                        JedisPool pool = new JedisPool("localhost", 6379);
                        try (Jedis jedis = pool.getResource()) {
                            setUser(user, jedis);
                            /**
                             * incrementing my lamport clock
                             */
                            incrementLamportClock();
                        } catch (NotBoundException e) {
                            throw new RuntimeException(e);
                        }
                        pool.close();
                        /**
                         * getting my lamport clock value
                         */
                        int lamportClockValue = getLamportClockValue();

                        /**
                         * I will pass the same request to other servers as well.
                         */

                        System.out.println("updating other server one by one");
                        for (String otherServerAddr : serverAddrs) {
                            /**
                             * I wont send it to my self.
                             */
                            if (!serverAddr.equals(otherServerAddr)) {
                                System.out.println("updating " + otherServerAddr);
                                try {
                                    Registry registry = LocateRegistry.getRegistry(otherServerAddr,
                                            SERVER_SIDE_REGISTRY_PORT);
                                    IdServerInterfaceForServer userDbStub = (IdServerInterfaceForServer) registry
                                            .lookup("IdServerForServer");

                                    String response = userDbStub.modify(loginName, newLoginName, password, updatedAt,
                                            lamportClockValue);

                                    System.out.println("response: " + response);

                                } catch (NotBoundException e) {
                                    System.out.println("some error " + e);
                                    continue;
                                } catch (RemoteException e) {
                                    System.out.println("some error " + e);
                                    continue;
                                }

                            }
                        }

                        return "login name updated.";
                    } else {
                        return "incorrect password.";
                    }
                }
            }

            return "no match found.";
        }
    }

    /**
     * Deletes a user with the specified login name and password from the system.
     * <p>
     * This method first synchronizes on a lock object to ensure thread safety.
     * It then attempts to delete the user from the system by comparing the provided
     * login name and password with the stored user data retrieved from Redis.
     * If a matching user is found, it deletes the user from Redis and increments
     * the Lamport clock value. It then retrieves the current Lamport clock value
     * and sends a delete message to other servers in the system, excluding itself
     * if it's the coordinator. Finally, it returns a response indicating the
     * success
     * or failure of the delete operation.
     * </p>
     *
     * @param loginName the login name of the user to be deleted.
     * @param password  the password of the user to be deleted.
     * @return a response indicating the result of the delete operation.
     * @throws RemoteException if there is a problem accessing the remote object.
     */
    public synchronized String delete(String loginName, String password) throws RemoteException {
        synchronized (lock) {
            String encryptedPassword = trySHA(password);
            /**
             * fetch all saved users from redis
             */
            HashSet<User> users = new HashSet<User>();
            users = getUsersFromRedis();
            for (User user : users) {
                if (user.loginName.equals(loginName)) {
                    if (encryptedPassword.equals(user.encryptedPassword)) {
                        System.out.println("found user to delete " + user.getUuid().toString());
                        JedisPool pool = new JedisPool("localhost", 6379);
                        try (Jedis jedis = pool.getResource()) {
                            jedis.del("user-" + user.getUuid().toString());
                            /**
                             * incrementing my lamport clock.
                             */
                            incrementLamportClock();
                        } catch (NotBoundException e) {
                            throw new RuntimeException(e);
                        }
                        pool.close();

                        /**
                         * getting my lamport clock value
                         */
                        int lamportClockValue = getLamportClockValue();

                        /**
                         * send other users the same message
                         */

                        System.out.println("updating other server one by one");
                        for (String otherServerAddr : serverAddrs) {
                            /**
                             * I wont send it to my self if I am the coordinator.
                             */
                            if (!serverAddr.equals(otherServerAddr)) {
                                System.out.println("updating " + otherServerAddr);
                                try {
                                    Registry registry = LocateRegistry.getRegistry(otherServerAddr,
                                            SERVER_SIDE_REGISTRY_PORT);
                                    IdServerInterfaceForServer stub = (IdServerInterfaceForServer) registry
                                            .lookup("IdServerForServer");

                                    String response = stub.delete(loginName, password, lamportClockValue);

                                    System.out.println("response: " + response);

                                } catch (NotBoundException e) {
                                    System.out.println("some error " + e);
                                    continue;
                                } catch (RemoteException e) {
                                    System.out.println("some error " + e);
                                    continue;
                                }

                            }
                        }

                        /**
                         * sending response to original request
                         */
                        return "user deleted.";
                    } else {
                        return "incorrect password.";
                    }
                }
            }
            return "no match found.";
        }

    }

    /**
     * Sets the value of the Lamport clock in a Redis database.
     * <p>
     * This method connects to a local Redis instance and sets the Lamport clock
     * value
     * using the specified integer value. It stores the Lamport clock value under a
     * specific
     * key in the Redis database.
     * </p>
     *
     * @param lamportClockValue the integer value representing the Lamport clock
     *                          value to be set.
     * @throws RemoteException if there is a problem accessing the remote object.
     */
    private void setLamportClockValue(int lamportClockValue) throws RemoteException {

        JedisPool pool = new JedisPool("localhost", 6379);
        try (Jedis jedis = pool.getResource()) {
            jedis.set(LAMPORT_KEY, Integer.toString(lamportClockValue));
        }
        pool.close();

    }

    /**
     * Retrieves the value of the Lamport clock from a Redis database.
     * <p>
     * This method connects to a local Redis instance and retrieves the Lamport
     * clock value
     * stored under a specific key. If the key exists and contains a non-empty
     * value, it parses
     * the value as an integer and returns it. If the key doesn't exist or contains
     * an empty value,
     * it returns 0.
     * </p>
     *
     * @return the integer value representing the Lamport clock retrieved from the
     * Redis database.
     */
    private int getLamportClockValue() {
        int lamportClockValue;
        JedisPool pool = new JedisPool("localhost", 6379);
        try (Jedis jedis = pool.getResource()) {
            String value = jedis.get(LAMPORT_KEY);
            if (value != null && !value.isEmpty()) {
                lamportClockValue = Integer.parseInt(value);
            } else {
                lamportClockValue = 0;
            }
        }
        pool.close();
        return lamportClockValue;
    }


    /**
     * Sends a greeting message to the server and returns a response. A simple testing function
     *
     * @return A string indicating the server's status or response to the greeting.
     * @throws RemoteException If an error occurs while communicating with the server.
     */
    @Override
    public String helloServer() throws RemoteException {
        try {
            System.out.println("Connect from: " + RemoteServer.getClientHost());
            // Thread.sleep(10000); // 10000 milliseconds = 10 seconds
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
        }
        return "I am okay";
    }

}
