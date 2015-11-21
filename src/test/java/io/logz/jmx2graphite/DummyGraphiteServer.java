package io.logz.jmx2graphite;

import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author amesika
 */
public class DummyGraphiteServer {
    private final static Logger logger = LoggerFactory.getLogger(DummyGraphiteServer.class);
    private int port;

    private Thread serverThread;
    private Socket clientSocket;
    private ServerSocket serverSocket;

    private CountDownLatch serverDownLatch;
    public DummyGraphiteServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        serverThread = new Thread(() -> {
            PrintWriter out = null;
            BufferedReader in = null;
            try {
                serverSocket = new ServerSocket(port);
                clientSocket = serverSocket.accept();
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                serverDownLatch = new CountDownLatch(1);
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    out.println(inputLine);
                }
            } catch (IOException e) {
                logger.info("Exception caught when trying to listen on port "
                        + port + " or listening for a connection", e);
            } finally {
                try {
                    Closeables.close(out, true);
                    Closeables.close(in, true);
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
                serverDownLatch.countDown();
            }
        });
        serverThread.start();
    }

    public void stop() {
        serverThread.interrupt();
        try {
            Closeables.close(clientSocket, true);
            Closeables.close(serverSocket, true);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        logger.info("Waiting for server to finish shutdown...");
        try {
            boolean success = serverDownLatch.await(5, TimeUnit.SECONDS);
            if (!success) {
                throw new RuntimeException("Timed out on waiting for dummy graphite server shutdown");
            }
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
        logger.info("Dummy graphite server closed");

    }
}
