package app.client;

import app.client.io.ApplicationInputManager;
import app.client.io.MovieBuilder;
import app.client.managers.AuthClientManager;
import app.client.managers.ClientCommandManager;
import app.client.network.UDPClient;

import java.io.IOException;
import java.util.Scanner;

public class MainClient {
    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("Запуск клиентского приложения...");

        String host = (args.length > 0 && !args[0].isEmpty()) ? args[0] : HOST;
        int port = PORT;
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Неверный порт, используется " + PORT);
            }
        }

        UDPClient udpClient = new UDPClient(host, port);
        try {
            udpClient.connect();
            System.out.println("Сетевой канал открыт (хост: " + host + ", порт: " + port + ").");
        } catch (IOException e) {
            System.err.println("Не удалось открыть сетевой канал: " + e.getMessage());
            return;
        }

        Scanner scanner = new Scanner(System.in);
        ApplicationInputManager inputManager = new ApplicationInputManager(scanner);

        AuthClientManager authManager = new AuthClientManager(udpClient, scanner);
        boolean authenticated = authManager.runAuthFlow();

        if (!authenticated) {
            System.out.println("Авторизация не выполнена. Завершение работы.");
            udpClient.stop();
            scanner.close();
            return;
        }

        MovieBuilder movieBuilder = new MovieBuilder(inputManager, true);
        ClientCommandManager commandManager = new ClientCommandManager(inputManager, udpClient, movieBuilder);
        commandManager.setCredentials(authManager.getLogin(), authManager.getPasswordHash());
        commandManager.startInteractiveMode();

        udpClient.stop();
        scanner.close();
    }
}