package p4.server;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import java.io.IOException;
import java.net.Socket;

/**
 * The FixedPortSslRMIClientSocketFactory class extends the functionality of
 * the {@link SslRMIClientSocketFactory} class to provide customized behavior
 * for creating SSL client sockets in an RMI context.
 * <p>
 * This class overrides the {@link #createSocket(String, int)} method to enforce
 * a fixed port, defined by {@link IdServerForClient#PREFERRED_PORT}, when creating
 * the client socket. Additionally, it sets a timeout of 10 seconds for the socket.
 * </p>
 * <p>
 * This class is particularly useful for cases where it's necessary to customize
 * SSL client socket creation behavior, such as enforcing a specific port and setting
 * a timeout for the socket.
 * </p>
 *
 * @see SslRMIClientSocketFactory
 * @author Sheikh Md Mushfiqur Rahman & Shaznin Sultana
 * @version 1.0
 */
class FixedPortSslRMIClientSocketFactory extends SslRMIClientSocketFactory {
    private static final long serialVersionUID = 3367830980062260938L;

    /**
     * Creates a new SSL client socket connected to the specified host and port.
     * <p>
     * This method overrides the superclass method to enforce the usage of a fixed port,
     * as defined by {@link IdServerForClient#PREFERRED_PORT}, and sets a socket timeout
     * of 10 seconds (10000 milliseconds).
     * </p>
     *
     * @param host the host name.
     * @param port the port number.
     * @return the SSL client socket connected to the specified host and port.
     * @throws IOException if an I/O error occurs while creating the socket.
     * 
     * @see SslRMIClientSocketFactory#createSocket(String, int)
     */
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket s = super.createSocket(host, IdServerForClient.PREFERRED_PORT);
        /**
         * setting timeout time to 1 seconds
         */
        s.setSoTimeout(10000); // ms
        return s;
    }
}