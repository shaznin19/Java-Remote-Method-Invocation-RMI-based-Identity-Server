package p4.server;

import java.rmi.RemoteException;

/**
 * The interface for client operations on the ID server.
 * <p>
 * This interface defines methods for client operations on the ID server, such as creating, retrieving,
 * modifying, and deleting user information.
 * </p>
 * @author Sheikh Md Mushfiqur Rahman & Shaznin Sultana
 * @version 1.0
 */
public interface IdServerInterfaceForClient extends java.rmi.Remote {
    /**
     * Creates a new user with the provided login name, real name, and password.
     *
     * @param loginName The login name for the user. This should be unique and used
     *                  for authentication purposes.
     * @param realName  The real name of the user.
     * @param password  The password for the user's account.
     * @return A string indicating the status of the user creation process. This
     *         could be the uuid if user is sucseccfully created
     *         or an error message, depending on the outcome of the operation.
     * @throws RemoteException If there is an issue with the remote communication
     *                         during the user creation process.
     */
    String createUser(String loginName, String realName, String password) throws RemoteException;

    /**
     * returns all existing uuids
     *
     * @return a stringified version of the list of all uuids.
     * @throws RemoteException If there is an issue with the remote communication
     *                         during the user creation process.
     */
    String allUUIds() throws RemoteException;

    /**
     * returns all existing users login name and real name
     *
     * @return a stringified version of the list of all users name
     * @throws RemoteException If there is an issue with the remote communication
     *                         during the user creation process.
     */
    String allUsers() throws RemoteException;

    /**
     * return all information about all users.
     * @return
     * @throws RemoteException
     */
    String all() throws RemoteException;

    /**
     * returns information of an user with an specific loginName.
     *
     * @param loginName the username
     * @return stringified version of the user object.
     * @throws RemoteException If there is an issue with the remote communication
     *                         during the user creation process.
     */
    String lookup(String loginName) throws RemoteException;

    /**
     * updates the user
     *
     * @param loginName    the username
     * @param newLoginName the new name that the user wants to set
     * @param password     the user's password
     * @return success or failure message
     * @throws RemoteException If there is an issue with the remote communication
     *                         during the user creation process.
     */
    String modify(String loginName, String newLoginName, String password) throws RemoteException;

    /**
     * returns information of an user with an specific loginName. if deleted
     *
     * @param loginName the username
     * @param password
     * @return success or failure message
     * @throws RemoteException If there is an issue with the remote communication
     *                         during the user creation process.
     */
    String delete(String loginName, String password) throws RemoteException;

    /**
     * @param uuid
     * @return users infromation in a string format
     * @throws RemoteException
     */
    String reverseLookup(String uuid) throws RemoteException;
    /**
     * @param
     * @return a simple greeting message
     * @throws RemoteException
     */
    String helloServer() throws RemoteException;
}
