package p4.server;

import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * The server implementation for server-side operations in the ID server application.
 * <p>
 * This class implements the {@link IdServerInterfaceForServer} interface.
 * </p>
 *
 * @author Sheikh Md Mushfiqur Rahman & Shaznin Sultana
 * @version 1.0
 */
public class IdServerForServer extends java.rmi.server.UnicastRemoteObject implements IdServerInterfaceForServer {

    private static final long serialVersionUID = 8510538827054962873L;
    private static int registryPort;
    private String LAMPORT_KEY = "lamport_key";
    /**
     * list to hold all servers informations
     */
    private static List<String> serverAddrs = new ArrayList<>();
    private HashMap<String, Integer> addressMap = new HashMap<>();
    // Define a lock object
    private final Object lock = new Object();
    // Flag to indicate whether a modification is in progress
    private boolean primaryIsWorking = false;

    /**
     * variable to set servers power
     */
    private int serverId;

    @Override
    public int getServerId() throws RemoteException {
        return serverId;
    }

    /**
     * variable to check if the server is the coordinator
     */
    private boolean isCoordinator = false;

    /**
     * Checks if the current server instance is the coordinator.
     *
     * @return true if the current server instance is the coordinator, false otherwise.
     * @throws RemoteException If an error occurs while determining the coordinator status.
     */
    public boolean isCoordinator() throws RemoteException {
        return isCoordinator;
    }

    /**
     * variables for election purposes
     */
    boolean foundgreater = false;
    static boolean electionInProgress = false;
    /**
     * variable to get the coordinators id
     */
    private static String coordinator;

    /**
     * variable to save server's own address
     */
    private static String serverAddr;

    /**
     * loads the redis pool
     *
     * @throws RemoteException
     */
    public IdServerForServer(String serverAddr, int registryPort, List<String> serverAddrs,
                             HashMap<String, Integer> addressMap)
            throws RemoteException, NotBoundException {
        super();
        /*
         * setting server uuid and server level
         */

        IdServerForServer.serverAddr = serverAddr;
        IdServerForServer.registryPort = registryPort;
        this.addressMap = addressMap;
        /**
         * setting a random power to server
         */
        // Random random = new Random();

        // // Generate a random integer between 1 and 100

        // serverId = random.nextInt(100) + 1;

        if (addressMap.containsKey(serverAddr)) {
            serverId = addressMap.get(serverAddr);
        }

        JedisPool pool = new JedisPool("localhost", 6379);

        /**
         * setting all server info
         */
        IdServerForServer.serverAddrs = serverAddrs;

        /**
         * I will get the database from
         */
        updateRedis();

        try (Jedis jedis = pool.getResource()) {

            Set<String> userKeys = jedis.keys("user-*"); // Get all keys from the hash
            System.out.println("Already Saved User:" + userKeys.size());
            pool.close();
        }
    }

    /**
     * Updates the local Redis database with data from the current coordinator.
     * <p>
     * This method retrieves the address of the current coordinator and attempts to
     * fetch data from it. If the local Lamport clock value is lower than the
     * coordinator's,
     * it fetches the full user database from the coordinator, deletes existing user
     * data
     * in the local Redis database, and saves the data obtained from the
     * coordinator.
     * Finally, it increments the local Lamport clock value to ensure
     * synchronization.
     * </p>
     *
     * @throws RemoteException   if there is a problem accessing the remote object.
     * @throws NotBoundException if there is an issue accessing the remote object's
     *                           registry.
     */
    private void updateRedis() throws RemoteException, NotBoundException {

        /**
         * getting the coordinator at the moment.
         */
        String currentCoordinatorAddr = searchCurrentCoordinatorAddr();

        if (currentCoordinatorAddr != null) {

            Registry registry = LocateRegistry.getRegistry(currentCoordinatorAddr, registryPort);
            IdServerInterfaceForServer stub = (IdServerInterfaceForServer) registry.lookup("IdServerForServer");
            /**
             * getting coordinators lamport clock value
             */
            int CoordinatorLamportClockValue = stub.getLamportClockValue();
            int myLamportClockValue = getLamportClockValue();
            if (myLamportClockValue <= CoordinatorLamportClockValue) {
                /**
                 * getting all users from the coordinator
                 */
                HashSet<User> usersInCoordinator = stub.getFullDataBase();
                /**
                 * now I will save the users
                 */

                /*
                 * connecting to jedispool
                 */
                JedisPool pool = new JedisPool("localhost", 6379);
                try (Jedis jedis = pool.getResource()) {
                    /**
                     * now I will delete every user I have first
                     */
                    // Example: Get all user keys
                    Set<String> userKeys = jedis.keys("user:*");

                    // Delete all user keys
                    for (String userKey : userKeys) {
                        jedis.del(userKey);
                    }
                    /**
                     * Now I will save every user that I have from the coordinator
                     */
                    for (User user : usersInCoordinator) {
                        /*
                         * creating and adding the user on redis
                         */

                        setUser(user, jedis);

                    }

                }
                pool.close();
                /**
                 * setting lamport clock value
                 */
                int max = Math.max(CoordinatorLamportClockValue, myLamportClockValue);
                setLamportClockValue(max + 1);
            }

        }
    }

    /**
     * Searches for the address of the current coordinator server.
     * <p>
     * This method iterates through the list of server addresses, excluding the
     * local server's address,
     * and attempts to communicate with each server to determine if it is the
     * coordinator. If a server
     * responds affirmatively, its address is returned as the current coordinator
     * address.
     * </p>
     *
     * @return the address of the current coordinator server, or {@code null} if the
     * coordinator
     * address cannot be determined.
     * @throws RemoteException   if there is a problem accessing the remote object.
     * @throws NotBoundException if there is an issue accessing the remote object's
     *                           registry.
     */
    private String searchCurrentCoordinatorAddr() throws RemoteException, NotBoundException {
        String currentCoordinatorAddr = null;
        for (String anyServerAddr : serverAddrs) {
            if (!anyServerAddr.equals(serverAddr)) {
                try {
                    Registry registry = LocateRegistry.getRegistry(anyServerAddr, registryPort);
                    IdServerInterfaceForServer stub = (IdServerInterfaceForServer) registry.lookup("IdServerForServer");
                    boolean response = stub.isCoordinator();
                    //System.out.println("is coordinator: " + response);
                    if (response) {
                        currentCoordinatorAddr = anyServerAddr;
                        break;
                    }
                } catch (RuntimeException e) {
                    System.out
                            .println("Error occurred while communicating with server: " + "IdServer_" + anyServerAddr);
                    continue;
                } catch (ConnectException e) {
                    continue;
                } catch (UnknownHostException e) {
                    continue;
                }
            }

        }
        return currentCoordinatorAddr;
    }

    /**
     * Binds the server to the provided name in the RMI registry, initiates an election, and starts a heartbeat.
     *
     * @param name The name to bind the server to in the RMI registry.
     * @throws RemoteException If an error occurs while communicating with the RMI registry or during the election process.
     */
    public void bind(String name) throws RemoteException {
        /*
         * connecting to jedispool
         */

        try {
            //System.out.println("server id:" + serverAddr);
            //System.out.println("server name: " + name);

            Registry registry = LocateRegistry.createRegistry(registryPort);
            //IdServerForServer obj = new IdServerForServer(serverAddr, registryPort, serverAddrs, addressMap);
            /*
             * binding the server name
             */
            registry.rebind(name, this);
            System.out.println(name + ": " + name + " bound in registry on port " + registryPort + " on this host");

            /**
             * After binding is done I will start the election
             */
            registry = LocateRegistry.getRegistry("localhost", registryPort);
            IdServerInterfaceForServer stub = (IdServerInterfaceForServer) registry.lookup("IdServerForServer");
            stub.startElection(serverAddr); // bully election call
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        }
        /**
         * running the heartbeat
         */
        System.out.println("Before starting the repeat, the coordinator is " + coordinator);
        repeat();

    }

    ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Initiates the bully algorithm for leader election.
     * <p>
     * This method is invoked to start the election process. If the current node's
     * address matches the
     * provided node address, it initiates the election process by comparing its
     * server ID with that
     * of other servers. If it finds a server with a higher ID, it sends an election
     * challenge to that
     * server. If no server with a higher ID is found, it declares itself as the
     * winner. If the current
     * node's address does not match the provided node address, it responds to the
     * received election
     * request with an acknowledgment.
     * </p>
     *
     * @param nodeAddr the address of the node initiating the election.
     * @return {@code null}.
     * @throws RemoteException if there is a problem accessing the remote object.
     */
    @Override
    public String startElection(String nodeAddr) throws RemoteException {
        electionInProgress = true;
        foundgreater = false;

        if (nodeAddr.equals(serverAddr)) {
            System.out.println("You started the elections");
            System.out.println("Your id is " + serverId);

            for (String anyOtherServerAddr : serverAddrs) {

                /**
                 * I will only challenge other servers
                 */

                if (!anyOtherServerAddr.equals(nodeAddr)) {
                    try {
                        Registry registry = LocateRegistry.getRegistry(anyOtherServerAddr, registryPort);
                        IdServerInterfaceForServer stub = (IdServerInterfaceForServer) registry
                                .lookup("IdServerForServer");
                        int otherServerId = stub.getServerId();
                        System.out.println(anyOtherServerAddr + " id is " + otherServerId);
                        /**
                         * now I will compare his power with mine
                         */
                        if (otherServerId > serverId) {
                            /**
                             * other server has got more power than me
                             */
                            System.out.println("Sending election challenge to " + anyOtherServerAddr);
                            stub.startElection(nodeAddr);
                            foundgreater = true;
                        }
                    } catch (NotBoundException e) {
                        System.out.println("Could not connect to " + anyOtherServerAddr);
                        continue;

                    } catch (RemoteException e) {
                        System.out.println("Could not connect to " + anyOtherServerAddr);
                        continue;
                    }
                }

            }
            if (!foundgreater) {
                iWon(serverAddr);
            }
            return null;
        } else {
            System.out.println("Received election request from " + nodeAddr);
            sendOk(serverAddr, nodeAddr);
            return null;
        }
    }

    /**
     * Notifies other servers that the current node has won the election.
     * <p>
     * This method is called when the current node wins the election. If the winning
     * node is the local server,
     * it notifies other servers of its victory by sending them a message. If the
     * winning node is a remote server,
     * it acknowledges the received victory message and updates its coordinator
     * status accordingly.
     * </p>
     *
     * @param node the address of the node that won the election.
     * @return {@code null}.
     * @throws RemoteException if there is a problem accessing the remote object.
     */
    @Override
    public String iWon(String node) throws RemoteException {
        coordinator = node;
        electionInProgress = false;

        if (node.equals(serverAddr)) {
            // send win
            System.out.println("You have won the election.");
            System.out.println("Letting other servers know that I (" + node + ") won the election");

            for (String otherServerAddr : serverAddrs) {

                if (!otherServerAddr.equals(serverAddr)) {
                    try {
                        Registry registry = LocateRegistry.getRegistry(otherServerAddr, registryPort);
                        IdServerInterfaceForServer stub = (IdServerInterfaceForServer) registry
                                .lookup("IdServerForServer");
                        stub.iWon(node);
                        isCoordinator = true;

                    } catch (NotBoundException e) {
                        System.out.println("Could not connect to " + otherServerAddr);
                        continue;
                    } catch (RemoteException e) {
                        System.out.println("Could not connect to " + otherServerAddr);
                        continue;
                    }
                }
            }

            System.out.println("Node " + node + " is the new Coodinator\n");
        } else {
            // receive win
            System.out.println("Node " + node + " has won the election.");
            System.out.println("Node " + node + " is the new Coodinator\n");
            /**
             * I have lost so I will turn off my isCoordinator flag
             */
            isCoordinator = false;
        }
        return null;
    }

    /**
     * Sends an acknowledgment message to a specified node.
     * <p>
     * This method sends an acknowledgment message to the specified destination
     * node.
     * If the destination node is not the local server, it sends the acknowledgment
     * and then starts an election
     * process on the local server. If the destination node is the local server, it
     * receives the acknowledgment.
     * </p>
     *
     * @param from the address of the sending node.
     * @param to   the address of the receiving node.
     * @return {@code null}.
     * @throws RemoteException if there is a problem accessing the remote object.
     */
    @Override
    public String sendOk(String from, String to) throws RemoteException {
        if (!serverAddr.equals(to)) {
            try {
                Registry registry = LocateRegistry.getRegistry(to, registryPort);
                IdServerInterfaceForServer stub = (IdServerInterfaceForServer) registry.lookup("IdServerForServer");
                System.out.println("Sending OK to " + to + "  from " + from);
                stub.sendOk(from, to);

                // start election after sending OK
                startElection(serverAddr);
            } catch (NotBoundException e) {
                System.out.println("Exception occurred: " + e);
            }
        } else {
            // receive OK
            System.out.println(from + " Replied with Ok..");
        }
        return null;
    }

    /**
     * Checks if the server is alive.
     * <p>
     * This method is invoked to check if the server is alive.
     * It always returns {@code true} to indicate that the server is alive.
     * </p>
     *
     * @return {@code true}.
     * @throws RemoteException if there is a problem accessing the remote object.
     */
    @Override
    public boolean isalive() throws RemoteException {
        return true;
    }

    /**
     * Schedules a task to be executed repeatedly after a specified delay.
     * <p>
     * This method schedules a task to be executed repeatedly with a fixed delay of
     * 5 seconds.
     * The task is executed by a timer thread.
     * </p>
     */
    public static void repeat() {
        Timer timer = new Timer();
        timer.schedule(new TimerCheck(), 5 * 1000);
    }

    /**
     * A task to check the status of servers periodically.
     * <p>
     * This class extends TimerTask and overrides the run method to perform a check
     * on the status of servers periodically.
     * It iterates through the list of server addresses (excluding the local
     * server's address) and checks if each server
     * is alive. If a server is found to be unreachable, it handles the situation
     * accordingly by either marking the
     * coordinator as crashed or indicating that a non-coordinator server has
     * crashed.
     * </p>
     */
    static class TimerCheck extends TimerTask {
        @Override
        public void run() {
            System.out.println("running repeater again to check if all the servers are alive");
            for (String serverAdd : serverAddrs) {
                if (!serverAdd.equals(serverAddr) && !electionInProgress) {
                    try {
                        System.out.println("I am " + serverAddr
                                + ", and I am now checking if other server " + serverAdd + " is alive.");

                        Registry registry = LocateRegistry.getRegistry(serverAdd, registryPort);
                        IdServerInterfaceForServer stub = (IdServerInterfaceForServer) registry
                                .lookup("IdServerForServer");
                        stub.isalive();
                    } catch (RemoteException e) {
                        if (serverAdd.equals(coordinator)) {
                            coordinatorCrashed();
                        } else {
                            System.out.println(serverAdd + " has crashed.");
                        }
                    } catch (NotBoundException e) {
                        System.out.println("Coordinator: " + coordinator + ", serverAdd: " + serverAdd);
                        if (serverAdd.equals(coordinator)) {
                            coordinatorCrashed();
                        } else {
                            System.out.println(serverAdd + " has crashed.");
                        }
                    }
                }
            }
            repeat();

        }
    }

    /**
     * Initiates a new election process when the current coordinator has crashed.
     * <p>
     * This method is invoked when the current coordinator server is detected as
     * crashed.
     * It prints a notification about the coordinator crash and initiates a new
     * election process
     * by invoking the {@code startElection} method on the local server's
     * {@code IdServerInterfaceForServer} stub.
     * </p>
     */
    private static void coordinatorCrashed() {
        System.out.println("the current coordinator " + coordinator + " has crushed. Iniating new election");
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", registryPort);
            IdServerInterfaceForServer stub = (IdServerInterfaceForServer) registry.lookup("IdServerForServer");
            stub.startElection(serverAddr);
        } catch (RemoteException e) {
            System.out.println("Exception occurred: " + e);
        } catch (NotBoundException e) {
            System.out.println("Exception occurred: " + e);
        }

    }

    ///////////////////////////////////////////////////////////////////////////////////////////

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
     * Retrieves all users stored in Redis and returns them as a HashSet.
     *
     * @return A HashSet containing all users retrieved from Redis.
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
     * Creates a new user in the database with the provided information (from another server).
     *
     * @param loginName         The login name for the new user.
     * @param realName          The real name of the new user.
     * @param password          The password for the new user.
     * @param ipAddress         The IP address of the client creating the user.
     * @param uuid              The UUID of the new user (if not provided, a new UUID will be generated).
     * @param createdAt         The date and time when the user is created.
     * @param lamportClockValue The Lamport logical clock value associated with the creation.
     * @return The UUID of the newly created user, or an error message if another creation is already in progress or the login name is already in use.
     * @throws RemoteException If an error occurs while communicating with the database or if another creation is already in progress.
     */
    @Override
    public synchronized String createUser(String loginName, String realName, String password, String ipAddress,
                                          UUID uuid, LocalDateTime createdAt, int lamportClockValue) throws RemoteException {
        synchronized (lock) {
            if (primaryIsWorking) {
                return "Another creation is already in progress. Please try again later.";
            }
            primaryIsWorking = true;
            /**
             * fetch all users from redis
             */
            HashSet<User> users = new HashSet<User>();

            users = getUsersFromRedis();
            /*
             * check if someone already uing the name
             */
            for (User user : users) {
                if (user.loginName.equals(loginName)) {
                    /*
                     * access available again
                     */
                    primaryIsWorking = false;
                    return "the new login name is already in use.";
                }
            }
            /**
             * creating an UUID (if the param does not contain uuid)
             */
            if (uuid == null) {
                uuid = UUID.randomUUID();
            }
            String encryptedPassword = trySHA(password);

            /*
             * connecting to jedispool
             */
            JedisPool pool = new JedisPool("localhost", 6379);
            try (Jedis jedis = pool.getResource()) {
                /*
                 * creating and adding the user on redis
                 */
                User user = new User(loginName, realName, encryptedPassword, uuid, ipAddress, createdAt, createdAt);
                setUser(user, jedis);
                /**
                 * setting lamport clock value
                 */
                int max = Math.max(getLamportClockValue(), lamportClockValue);
                setLamportClockValue(max + 1);

            }
            pool.close();
            /**
             * send client the uuid
             */
            /**
             * access available again
             */
            primaryIsWorking = false;
            return uuid.toString();
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
     * Modifies the login name of a user in the database if the provided credentials match.
     *
     * @param loginName         The current login name of the user.
     * @param newLoginName      The new login name to set for the user.
     * @param password          The password of the user.
     * @param updatedAt         The date and time when the modification was requested.
     * @param lamportClockValue The Lamport logical clock value associated with the modification.
     * @return A message indicating the result of the modification attempt.
     * @throws RemoteException If an error occurs while communicating with the database or if another modification is already in progress.
     */
    public synchronized String modify(String loginName, String newLoginName, String password, LocalDateTime updatedAt,
                                      int lamportClockValue) throws RemoteException {
        synchronized (lock) {
            if (primaryIsWorking) {
                return "Another modification is already in progress. Please try again later.";
            }

            primaryIsWorking = true;
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
                    /*
                     * access available again
                     */
                    primaryIsWorking = false;
                    return "the new login name is already in use.";
                }
            }
            String encryptedPassword = trySHA(password);
            for (User user : users) {
                if (user.loginName.equals(loginName)) {
                    if (encryptedPassword.equals(user.encryptedPassword)) {

                        user.setLoginName(newLoginName, updatedAt);
                        JedisPool pool = new JedisPool("localhost", 6379);
                        try (Jedis jedis = pool.getResource()) {
                            setUser(user, jedis);
                            /**
                             * setting lamport clock value
                             */
                            int max = Math.max(getLamportClockValue(), lamportClockValue);
                            setLamportClockValue(max + 1);
                        }
                        pool.close();
                        /**
                         * access available again
                         */
                        primaryIsWorking = false;
                        return "login name updated.";
                    } else {
                        /**
                         * access available again
                         */
                        primaryIsWorking = false;
                        return "incorrect password.";
                    }
                }
            }
            /**
             * access available again
             */
            primaryIsWorking = false;
            return "no match found.";
        }

    }

    /**
     * Deletes a user from the database if the provided credentials match (request generated from another server).
     *
     * @param loginName         The login name of the user to delete.
     * @param password          The password of the user.
     * @param lamportClockValue The Lamport logical clock value associated with the deletion from the primary.
     * @return A message indicating the result of the deletion attempt.
     * @throws RemoteException If an error occurs while communicating with the database or if another modification is already in progress.
     */
    public String delete(String loginName, String password, int lamportClockValue) throws RemoteException {
        synchronized (lock) {
            if (primaryIsWorking) {
                return "Another modification is already in progress. Please try again later.";
            }
            primaryIsWorking = true;
            String encryptedPassword = trySHA(password);
            /**
             * fetch all saved users from redis
             */
            HashSet<User> users = new HashSet<User>();
            users = getUsersFromRedis();
            for (User user : users) {
                if (user.loginName.equals(loginName)) {
                    if (encryptedPassword.equals(user.encryptedPassword)) {
                        JedisPool pool = new JedisPool("localhost", 6379);
                        try (Jedis jedis = pool.getResource()) {
                            jedis.del("user-" + user.getUuid().toString());
                            /**
                             * setting lamport clock value
                             */
                            int max = Math.max(getLamportClockValue(), lamportClockValue);
                            setLamportClockValue(max + 1);
                        }
                        pool.close();
                        /**
                         * access available again
                         */
                        primaryIsWorking = false;
                        return "user deleted.";
                    } else {
                        /**
                         * access available again
                         */
                        primaryIsWorking = false;
                        return "incorrect password.";
                    }
                }
            }
            /**
             * access available again
             */
            primaryIsWorking = false;
            return "no match found.";
        }
    }

    /**
     * Retrieves the full database of users from a Redis database.
     *
     * @return A HashSet containing all the users retrieved from the database.
     * @throws RemoteException If an error occurs while communicating with the Redis database.
     */
    @Override
    public HashSet<User> getFullDataBase() throws RemoteException {
        HashSet<User> users = getUsersFromRedis();
        return users;
    }

    /**
     * Retrieves the current value of the Lamport logical clock from a Redis database.
     *
     * @return The current value of the Lamport logical clock.
     * @throws RemoteException If an error occurs while communicating with the Redis database.
     */
    @Override
    public int getLamportClockValue() throws RemoteException {
        int lamportClockValue = 0;
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
     * Sets the value of the Lamport logical clock in a Redis database.
     *
     * @param lamportClockValue The new value to set for the Lamport logical clock.
     * @throws RemoteException If an error occurs while communicating with the Redis database.
     */
    private void setLamportClockValue(int lamportClockValue) throws RemoteException {

        JedisPool pool = new JedisPool("localhost", 6379);
        try (Jedis jedis = pool.getResource()) {
            jedis.set(LAMPORT_KEY, Integer.toString(lamportClockValue));
        }
        pool.close();

    }
}
