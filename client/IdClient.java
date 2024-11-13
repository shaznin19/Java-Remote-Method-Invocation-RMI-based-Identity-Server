package p4.client;

import org.apache.commons.cli.*;

import p4.server.IdServerInterfaceForClient;

import java.net.SocketTimeoutException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashSet;
import java.util.Random;

/**
 * The {@code IdClient} class represents a client
 * that maintains connection with a rmi server through an ecrypted SSL
 * connection.
 * <p>
 *
 * @author Shaznin Sultana
 * @version 1.0
 */
public class IdClient {
    /**
     * saving server list
     */
    static String host;
    static int registryPort;


    public static void main(String[] args) {

        System.setProperty("javax.net.ssl.trustStore", "p4/resources/Client_Truststore");
        System.setProperty("java.security.policy", "p4/resources/mysecurity.policy");
        System.setProperty("javax.net.ssl.trustStorePassword", "clienttest123");

        CommandLineParser parser = new DefaultParser();
        Options options = setupOptions();
        if (args.length == 0) {
            printUsage(options);
        }
        /**
         * run garbage calculator.
         */
        System.gc();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("s")) {
                if (!line.hasOption("n")) {
                    throw new ParseException("Missing required arg --numport");
                }
                registryPort = Integer.parseInt(line.getOptionValue("n"));
                host = line.getOptionValue("s");
                System.out.println("Client running.");
            }

            if (line.hasOption("d")) {
                if (!line.hasOption("p")) {
                    throw new ParseException("Missing required arg --password");
                }
                String value = line.getOptionValue("d");

                Registry registry = LocateRegistry.getRegistry(host, registryPort);
                IdServerInterfaceForClient stub = (IdServerInterfaceForClient) registry.lookup("IdServerForClient");
                String response = stub.delete(value, line.getOptionValue("p"));
                System.out.println("response: " + response);

            }

            if (line.hasOption("l")) {
                Registry registry = LocateRegistry.getRegistry(host, registryPort);
                IdServerInterfaceForClient stub = (IdServerInterfaceForClient) registry.lookup("IdServerForClient");
                String response = stub.lookup(line.getOptionValue("l"));
                System.out.println("response: " + response);
            }
            if (line.hasOption("r")) {
                Registry registry = LocateRegistry.getRegistry(host, registryPort);
                IdServerInterfaceForClient stub = (IdServerInterfaceForClient) registry.lookup("IdServerForClient");
                String response = stub.reverseLookup(line.getOptionValue("r"));
                System.out.println("response: " + response);

            }
            if (line.hasOption("g")) {
                String value = line.getOptionValue("g");
                if (value.equals("users")) {
                    System.out.println("List of all users");

                    Registry registry = LocateRegistry.getRegistry(host, registryPort);
                    IdServerInterfaceForClient stub = (IdServerInterfaceForClient) registry.lookup("IdServerForClient");
                    String response = stub.allUsers();
                    System.out.println("response: " + response);

                }
                if (value.equals("uuids")) {
                    System.out.println("List of all UUIDs");

                    Registry registry = LocateRegistry.getRegistry(host, registryPort);
                    IdServerInterfaceForClient stub = (IdServerInterfaceForClient) registry.lookup("IdServerForClient");
                    String response = stub.allUUIds();
                    System.out.println("response: " + response);

                }
                if (value.equals("all")) {
                    System.out.println("List of all UUIDs and users details:");

                    Registry registry = LocateRegistry.getRegistry(host, registryPort);
                    IdServerInterfaceForClient stub = (IdServerInterfaceForClient) registry.lookup("IdServerForClient");
                    String response = stub.all();
                    System.out.println("response: " + response);

                }
            }

            if (line.hasOption("m")) {
                if (!line.hasOption("p")) {
                    throw new ParseException("Missing required arg --password");
                }
                String[] values = line.getOptionValues("m");

                Registry registry = LocateRegistry.getRegistry(host, registryPort);
                IdServerInterfaceForClient stub = (IdServerInterfaceForClient) registry.lookup("IdServerForClient");
                String response = stub.modify(values[0], values[1], line.getOptionValue("p"));
                System.out.println("response: " + response);

            }

            if (line.hasOption("c")) {
                String realName;
                String loginName;
                String password = null;

                String[] values = line.getOptionValues("c");
                loginName = values[0];
                if (values.length > 1) {
                    // Concatenate multiple real names into a single string
                    StringBuilder realNameBuilder = new StringBuilder();
                    for (int i = 1; i < values.length; i++) {
                        realNameBuilder.append(values[i]);
                        if (i < values.length - 1) {
                            realNameBuilder.append(" ");
                        }
                    }
                    realName = realNameBuilder.toString();
                } else {
                    realName = System.getProperty("user.name");
                }
                if (line.hasOption("p")) {
                    password = line.getOptionValue("p");
                }

                Registry registry = LocateRegistry.getRegistry(host, registryPort);
                IdServerInterfaceForClient stub = (IdServerInterfaceForClient) registry.lookup("IdServerForClient");
                String response = stub.createUser(loginName, realName, password);
                //String response = stub.helloServer();
                System.out.println("response: " + response);

            }

            // process other options...

        } catch (UnmarshalException e) {
            /**
             * timeout error handle
             */
            if (e.getCause() instanceof SocketTimeoutException) {
                System.err.println("RMI call timed out: " + e);
            }
        } catch (ParseException exp) {
            System.out.println("ParseTest: " + exp.getMessage());
            printUsage(options);
        } catch (RemoteException e) {
            // throw new RuntimeException(e);
            System.out.println("Remote Exception:" + e.getMessage());
            e.printStackTrace();
        } catch (NotBoundException e) {
            // throw new RuntimeException(e);
            System.out.println("Not Bound Exception:" + e.getMessage());
            e.printStackTrace();
        }

    }

//    private static String getRandomServer() {
//        // Convert HashSet to an array
//        String[] array = serverList.toArray(new String[0]);
//
//        // Generate a random index
//        Random random = new Random();
//        int randomIndex = random.nextInt(array.length);
//
//        // Retrieve the element at the random index
//        String randomItem = array[randomIndex];
//        System.out.println("Random server picked: " + randomItem);
//        return randomItem;
//    }

    /**
     * Prints help (a summary of command line queries) in a formatted way
     *
     * @param options a collection of Options objects
     * @return void
     */
    public static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Command line queries:", options);
    }

    /**
     * Creates and sets up a collection of options to be used as command line
     * queries
     *
     * @return Options
     */
    public static Options setupOptions() {
        // create the Options
        Options options = new Options();

        // this option requires one value
        options.addOption("l", "lookup", true, "lookup an account with the given login name");
        options.addOption("r", "reverse-lookup", true, "lookup an account with the given UUID");
        options.addOption("g", "get", true, "obtains list of all login names or UUIDs or user,UUID and description");
        options.addOption("s", "server", true, "takes serverhost to connect");
        options.addOption("n", "numport", true, "takes port number to connect");

        // this option requires one value (but we will only look for it if a password is
        // required)
        Option passwordOption = new Option("p", "password", true, "supply password (if needed)");
        options.addOption(passwordOption);

        // one way to create an option that requires multiple values
        Option modifyOption = new Option("m", "modify", true, "modify existing login name");
        modifyOption.setArgs(2);
        options.addOption(modifyOption);

        Option createOption = new Option("c", "create", true, "create new login name request");
        createOption.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(createOption);

        Option deleteOption = new Option("d", "delete", true, "delete existing login name");
        deleteOption.setArgs(1);
        options.addOption(deleteOption);

        return options;
    }
}
