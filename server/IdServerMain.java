package p4.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.*;

/**
 * The main class for the ID server application.
 * <p>
 * This class initializes and starts the ID server, binding it to the RMI registry for client and server-side
 * interactions. It also sets up SSL properties, defines the server address map, and implements a shutdown hook
 * to handle server shutdown processes.
 * </p>
 *
 * @author Sheikh Md Mushfiqur Rahman & Shaznin Sultana
 * @version 1.0
 */
public class IdServerMain {
    private static final long serialVersionUID = 8510538827054962873L;
    private static int CLIENT_SIDE_REGISTRY_PORT = 5181;
    public static int SERVER_SIDE_REGISTRY_PORT = 5182;
    /**
     * A HashMap storing the addresses and associated values.
     * <p>
     * This HashMap is used to store addresses mapped to integer values.
     * It provides a mapping between string keys (addresses) and integer values.
     * </p>
     */
    private static HashMap<String, Integer> addressMap;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println(
                    "Usage: java p4.server.IdServerMain <server address 1> <server address 2> <server address 3> {including the this server's address, you can provide in any order}");
            System.exit(1);
        }
        HashSet<String> serverAddrsSet = new HashSet<>();
        String serverAddr;
        try {
            System.out.println("My address : " + InetAddress.getLocalHost());
            System.out.println("Local Host Address: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("Local Host Name: " + InetAddress.getLocalHost().getHostName());
            /**
             * getting server's own address
             */
            serverAddr = InetAddress.getLocalHost().getHostName();

            serverAddrsSet.add(args[0]); // IdServer_{server uuid}
            serverAddrsSet.add(args[1]);
            serverAddrsSet.add(args[2]);

            if (!serverAddrsSet.contains(InetAddress.getLocalHost().getHostName())) {
                System.out.println("You have not added you own name");
                System.exit(1);
            }

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        // ssl
        System.setProperty("javax.net.ssl.keyStore", "p4/resources/Server_Keystore");
        // Warning: change to match your password! Also the password should be
        // stored encrypted in a file outside the program.
        System.setProperty("javax.net.ssl.keyStorePassword", "test123");
        System.setProperty("java.security.policy", "p4/resources/mysecurity.policy");

        /**
         * added this part from client
         */
        System.setProperty("javax.net.ssl.trustStore", "p4/resources/Client_Truststore");
        System.setProperty("java.security.policy", "p4/resources/mysecurity.policy");
        System.setProperty("javax.net.ssl.trustStorePassword", "clienttest123");

        // Convert set to list
        List<String> serverAddrs = new ArrayList<>(serverAddrsSet);
        // Sort the list
        Collections.sort(serverAddrs);


        System.out.println("Server Addresses: " + serverAddrs);

        // Create HashMap to map server addresses to positions
        addressMap = new HashMap<>();
        int position = 0;
        for (String addr : serverAddrs) {
            position = position + 10;
            addressMap.put(addr, position);
        }

        try {
            /**
             * run garbage calculator.
             */
            System.gc();
            IdServerForClient serverForClient = new IdServerForClient(serverAddr, CLIENT_SIDE_REGISTRY_PORT,
                    SERVER_SIDE_REGISTRY_PORT, serverAddrs);
            serverForClient.bind("IdServerForClient");
            IdServerForServer serverForServer = new IdServerForServer(serverAddr,
                    SERVER_SIDE_REGISTRY_PORT, serverAddrs, addressMap);
            serverForServer.bind("IdServerForServer");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    shutdownHook(serverForServer.isCoordinator());
                } catch (RemoteException e) {
                    //throw new RuntimeException(e);
                }
            }));
        } catch (Throwable th) {
            th.printStackTrace();
            System.out.println("Exception occurred: " + th);
        }

    }

    /**
     * Shutdown hook method that handles the shutdown process for an IdServer
     * instance.
     *
     * @param isCoordinator is to check whether a coordinator is going down
     */
    public static void shutdownHook(boolean isCoordinator) {
        // Access the instance field through the parameter
        if (isCoordinator) {
            System.out.println("Coordinator is shutting down.");
        } else {
            System.out.println("Other Server side instance is shutting down.");
        }
    }

}
