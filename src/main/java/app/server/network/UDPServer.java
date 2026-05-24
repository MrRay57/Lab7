package app.server.network;

import app.common.network.Request;
import app.common.network.Response;
import app.common.network.SerializationUtils;
import app.server.managers.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ForkJoinPool;

/**
 * Класс, реализующий UDP-сервер для обработки сетевых запросов
 */
public class UDPServer {
    private static final Logger logger = LoggerFactory.getLogger(UDPServer.class);

    private final int port;
    private DatagramSocket socket;
    private volatile boolean running = false;
    private Thread listenerThread;
    private CommandManager commandManager;

    private static final int BUFFER_SIZE = 65536;

    private final ForkJoinPool readPool    = new ForkJoinPool();
    private final ForkJoinPool processPool = new ForkJoinPool();
    private final ForkJoinPool sendPool    = new ForkJoinPool();

    public UDPServer(int port) {
        this.port = port;
    }

    public void setCommandManager(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    public void start() throws SocketException {
        socket = new DatagramSocket(port);
        socket.setSoTimeout(500);
        running = true;

        listenerThread = new Thread(this::listen, "udp-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        logger.info("UDP Сервер запущен на порту {}.", port);
    }

    public void runLoop() {
        while (running) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void listen() {
        logger.info("Поток прослушивания UDP-сокета запущен.");
        while (running) {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                final byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                final SocketAddress clientAddress = packet.getSocketAddress();

                readPool.execute(() -> handleRead(data, clientAddress));

            } catch (SocketTimeoutException ignored) {
            } catch (IOException e) {
                if (running) {
                    logger.error("Ошибка в потоке прослушивания: {}", e.getMessage());
                }
            }
        }
    }

    private void handleRead(byte[] data, SocketAddress clientAddress) {
        try {
            Request request = (Request) SerializationUtils.deserialize(data);
            logger.info("Запрос '{}' от {} десериализован.", request.getCommandName(), clientAddress);
            processPool.execute(() -> handleProcess(request, clientAddress));
        } catch (Exception e) {
            logger.warn("Ошибка десериализации запроса от {}: {}", clientAddress, e.getMessage());
        }
    }

    private void handleProcess(Request request, SocketAddress clientAddress) {
        Response response = commandManager.execute(request);
        sendPool.execute(() -> handleSend(response, clientAddress));
    }

    private void handleSend(Response response, SocketAddress clientAddress) {
        try {
            byte[] data = SerializationUtils.serialize(response);
            DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress);
            synchronized (socket) {
                socket.send(packet);
            }
            logger.info("Ответ [{}] отправлен {}.", response.getStatus(), clientAddress);
        } catch (IOException e) {
            logger.error("Ошибка при отправке ответа {}: {}", clientAddress, e.getMessage());
        }
    }

    public void stop() {
        running = false;
        readPool.shutdown();
        processPool.shutdown();
        sendPool.shutdown();
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        logger.info("UDP Сервер остановлен.");
    }
}