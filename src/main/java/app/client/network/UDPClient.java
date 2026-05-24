package app.client.network;

import app.common.network.Request;
import app.common.network.Response;
import app.common.network.SerializationUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Класс для взаимодействия клиента с сервером
 */
public class UDPClient {
    private final String host;
    private final int port;
    private DatagramChannel channel;
    private InetSocketAddress serverAddress;

    private static final int TIMEOUT = 3000;
    private static final int BUFFER_SIZE = 65536;

    public UDPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        serverAddress = new InetSocketAddress(host, port);
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
    }

    public Response sendAndReceive(Request request) {
        try {
            byte[] requestBytes = SerializationUtils.serialize(request);
            ByteBuffer sendBuffer = ByteBuffer.wrap(requestBytes);
            channel.send(sendBuffer, serverAddress);

            ByteBuffer receiveBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            long startTime = System.currentTimeMillis();
            InetSocketAddress senderAddress = null;

            while (senderAddress == null) {
                if (System.currentTimeMillis() - startTime > TIMEOUT) {
                    return new Response(app.common.network.ResponseStatus.ERROR,
                            "Сервер временно недоступен. Превышено время ожидания ответа.");
                }
                senderAddress = (InetSocketAddress) channel.receive(receiveBuffer);

                if (senderAddress == null) {
                    Thread.sleep(50);
                }
            }

            receiveBuffer.flip();
            byte[] responseBytes = new byte[receiveBuffer.remaining()];
            receiveBuffer.get(responseBytes);

            return (Response) SerializationUtils.deserialize(responseBytes);

        } catch (IOException e) {
            return new Response(app.common.network.ResponseStatus.ERROR,
                    "Ошибка сетевого взаимодействия: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            return new Response(app.common.network.ResponseStatus.ERROR,
                    "Получен неизвестный ответ от сервера.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Response(app.common.network.ResponseStatus.ERROR,
                    "Ожидание ответа было прервано.");
        }
    }

    public void stop() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии сетевого канала: " + e.getMessage());
        }
    }
}