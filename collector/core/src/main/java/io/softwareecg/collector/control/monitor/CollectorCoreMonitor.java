package io.softwareecg.collector.control.monitor;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.control.CollectorCore;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;


/**
 * A monitor thread implementation to start and stop the CollectorCore.
 *
 */
public class CollectorCoreMonitor extends Thread {

    /**
     * The CollectorCore monitor thread host.
     */
    public static final String MONITOR_HOST = "127.0.0.1";

    private static final Logger LOGGER = LoggerFactory.create();


    /**
     * The stop key.
     */
    private final String stopKey;

    /**
     * The args.
     */
    private final String[] args;

    /**
     * The server socket.
     */
    private volatile ServerSocket serverSocket;

    /**
     * Initialize the monitor thread with the stop key and port and the arguments for the collector core.
     *
     * @param stopKey  the stop key
     * @param stopPort the stop port
     * @param daemon   start collector as daemon
     * @param args     the arguments
     * @throws java.io.IOException error starting the monitor
     */
    public CollectorCoreMonitor(final String stopKey, final int stopPort,
                                final boolean daemon, final String[] args) throws IOException {
        this.args = args.clone();
        this.stopKey = stopKey;

        setDaemon(daemon);
        setName("CollectorCoreMonitor");

        serverSocket = new ServerSocket(stopPort, 1, InetAddress.getByName(MONITOR_HOST));
        serverSocket.setReuseAddress(true);
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        try {
            CollectorCore.main(args);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Could not start monitor thread for the CollectorCore.", e);
        }

        while (serverSocket != null) {
            Socket socket = null;

            try {
                socket = serverSocket.accept();
                socket.setSoLinger(false, 0);

                String key = IOUtils.toString(socket.getInputStream(), Charset.defaultCharset());
                if (!stopKey.equals(key)) {
                    continue;
                }

                try {
                    socket.close();
                }
                catch (Exception e) {
                    LOGGER.warn("Could not close socket.", e);
                }
                finally {
                    socket = null;
                }

                try {
                    serverSocket.close();
                }
                catch (Exception e) {
                    LOGGER.warn("Could not close sever socket.", e);
                }
                finally {
                    serverSocket = null;
                }

                // and stop the collector
                CollectorCore.stop();

            }
            catch (Exception e) {
                LOGGER.warn("Could not stop the collector due to an exception.", e);
            }
            finally {
                if (socket != null) {
                    try {
                        socket.close();
                    }
                    catch (Exception e) {
                        LOGGER.warn("Could not close socket.", e);
                    }
                    finally {
                        socket = null;
                    }
                }
            }

        }
    }
}
