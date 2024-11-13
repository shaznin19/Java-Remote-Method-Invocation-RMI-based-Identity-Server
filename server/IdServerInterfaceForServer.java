package p4.server;


import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

/**
 * The interface for server operations on the ID server.
 * <p>
 * This interface defines methods for server operations on the ID server, such as creating, modifying,
 * and deleting user information, managing server coordination, and retrieving server information.
 * </p>
 * @author Sheikh Md Mushfiqur Rahman & Shaznin Sultana
 * @version 1.0
 */
public interface IdServerInterfaceForServer extends java.rmi.Remote {
    /**
     * Creates a new user in the other servers with provided login name, real name, and password.
     *
     * @param loginName         The login name for the user. This should be unique and used for authentication purposes.
     * @param realName          The real name of the user.
     * @param password          The password for the user's account.
     * @param ipAddress         The ipaddress of the user.
     * @param uuid              user uuid
     * @param createdAt         the time when user was created
     * @param lamportClockValue the sending server's lamport clock value
     * @return A string indicating the status of the user creation process. This could be the uuid if user is sucseccfully created
     * or an error message, depending on the outcome of the operation.
     * @throws RemoteException If there is an issue with the remote communication during the user creation process.
     */
    String createUser(String loginName, String realName, String password, String ipAddress, UUID uuid, LocalDateTime createdAt, int lamportClockValue) throws RemoteException;
    /**
     * modify a user in the other servers with provided login name, new login name, and password.
     *
     * @param loginName         The login name for the user. This should be unique and used for authentication purposes.
     * @param newLoginName          The real name of the user.
     * @param password          The password for the user's account.
     * @param updatedAt         when the update actually happened
     * @param lamportClockValue the sending server's lamport clock value
     * @return A string indicating the status of the user creation process. This could be the uuid if user is sucseccfully created
     * or an error message, depending on the outcome of the operation.
     * @throws RemoteException If there is an issue with the remote communication during the user creation process.
     */
    String modify(String loginName, String newLoginName, String password, LocalDateTime updatedAt, int lamportClockValue) throws RemoteException;
    /**
     * returns information of an user with an specific loginName. if deleted
     *
     * @param loginName the username
     * @param password
     * @param lamportClockValue the sending server's lamport clock value
     * @return success or failure message
     * @throws RemoteException If there is an issue with the remote communication
     *                         during the user creation process.
     */
    String delete(String loginName, String password, int lamportClockValue) throws RemoteException;

    /**
     * function to check if a server is the coordinator
     * @return
     * @throws RemoteException
     */
    boolean isCoordinator() throws RemoteException;

      /**
     * Starts an election process.
     * 
     * @param serverAddr The address of the server for starting the election.
     * @throws RemoteException If there is an error in the remote communication.
     */
    public String startElection(String serverAddr) throws RemoteException;

    /**
     * Notifies the system that the node has won the election.
     * 
     * @param node The identifier of the node that won the election.
     * @return A message indicating the result of the election process.
     * @throws RemoteException If there is an error in the remote communication.
     */
    public String iWon(String node) throws RemoteException;

    /**
     * Sends an "OK" message from one node to another.
     * 
     * @param from The identifier of the node sending the "OK" message.
     * @param to   The identifier of the node receiving the "OK" message.
     * @throws RemoteException If there is an error in the remote communication.
     */
    public String sendOk(String from, String to) throws RemoteException;

    /**
     *
     * function to check if a server is alive
     * @return
     * @throws RemoteException
     */
    public boolean isalive() throws RemoteException;

    /**
     * when a server gets up it will receive the full database from the coordinator.
     *
     * @return
     * @throws RemoteException
     */
    public HashSet<User> getFullDataBase() throws RemoteException;

    /**
     * function to return the server's lamport clock from redis
     *
     * @return
     * @throws RemoteException
     */
    public int getLamportClockValue() throws RemoteException;

    /**
     * function to return server's Id
     * @return
     * @throws RemoteException
     */
    public int getServerId() throws RemoteException;
}

