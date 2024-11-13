package p4.server;

import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * The FixedPortSslRMIServerSocketFactory class extends the functionality of
 * the {@link SslRMIServerSocketFactory} class to ensure consistent port usage
 * in SSL-based RMI server socket creation.
 * <p>
 * This class overrides the {@link #createServerSocket(int)} method to enforce
 * a fixed port if the port argument provided is 0 (indicating a system-assigned port).
 * Otherwise, it delegates to the superclass implementation.
 * </p>
 * <p>
 * This class is particularly useful in scenarios where it's necessary to
 * maintain a consistent server port for SSL-based RMI communication.
 * </p>
 *
 * @see SslRMIServerSocketFactory
 * @author Sheikh Md Mushfiqur Rahman & Shaznin Sultana
 * @version 1.0
 */
class FixedPortSslRMIServerSocketFactory extends SslRMIServerSocketFactory {
    /**
     * Creates a new server socket on the specified port.
     *
     * @param port the port number, or 0 to use a system-assigned port.
     * @return the server socket bound to the specified port.
     * @throws IOException if an I/O error occurs while creating the socket.
     * 
     * @see SslRMIServerSocketFactory#createServerSocket(int)
     */
    public ServerSocket createServerSocket(int port) throws IOException {
        if (port == 0) {
            return super.createServerSocket(IdServerForClient.PREFERRED_PORT);
        }
        return super.createServerSocket(port);
    }
}

