package app.server;

import app.server.db.DatabaseManager;
import app.server.db.MovieRepository;
import app.server.db.UserRepository;
import app.server.managers.AuthManager;
import app.server.managers.CollectionManager;
import app.server.managers.CommandManager;
import app.server.network.UDPServer;
import app.common.models.Movie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MainServer {
    private static final Logger logger = LoggerFactory.getLogger(MainServer.class);

    private static final String DEFAULT_DB_HOST = "pg";
    private static final String DEFAULT_DB_NAME = "studs";
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        logger.info("Инициализация серверного приложения...");

        String dbHost = getParam(args, 0, "DB_HOST", DEFAULT_DB_HOST);
        String dbName = getParam(args, 1, "DB_NAME", DEFAULT_DB_NAME);
        String dbUser = getParam(args, 2, "DB_USER", null);
        String dbPassword = getParam(args, 3, "DB_PASSWORD", null);
        int port = parsePort(getParam(args, 4, "PORT", String.valueOf(DEFAULT_PORT)));

        if (dbUser == null || dbUser.isEmpty()) {
            logger.error("Не указан пользователь БД. Передайте через аргументы или переменную DB_USER.");
            System.err.println("Использование: java -jar Server.jar [db_host] [db_name] [db_user] [db_password] [port]");
            System.err.println("Или через переменные окружения: DB_HOST, DB_NAME, DB_USER, DB_PASSWORD, PORT");
            System.exit(1);
        }
        if (dbPassword == null) {
            dbPassword = "";
        }

        DatabaseManager databaseManager = new DatabaseManager(dbHost, dbName, dbUser, dbPassword);

        try {
            databaseManager.initSchema();
        } catch (SQLException e) {
            logger.error("Не удалось инициализировать схему БД: {}", e.getMessage());
            System.exit(1);
        }

        UserRepository userRepository = new UserRepository(databaseManager);
        MovieRepository movieRepository = new MovieRepository(databaseManager);
        AuthManager authManager = new AuthManager(userRepository);
        CollectionManager collectionManager = new CollectionManager();

        Map<Long, String> owners = new HashMap<>();
        TreeMap<Long, Movie> loaded;
        try {
            loaded = movieRepository.loadAll(owners);
            collectionManager.setCollection(loaded, owners);
            logger.info("Коллекция загружена из БД. Элементов: {}", collectionManager.getSize());
        } catch (SQLException e) {
            logger.error("Не удалось загрузить коллекцию из БД: {}", e.getMessage());
            System.exit(1);
        }

        CommandManager commandManager = new CommandManager(collectionManager, authManager, movieRepository);
        UDPServer udpServer = new UDPServer(port);
        udpServer.setCommandManager(commandManager);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Завершение работы сервера...");
            udpServer.stop();
        }));

        try {
            udpServer.start();
            logger.info("Сервер готов. Консольные команды: exit");
        } catch (Exception e) {
            logger.error("Не удалось запустить сервер на порту {}: {}", port, e.getMessage());
            System.exit(1);
        }

        Thread consoleThread = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String line = reader.readLine();
                    if (line == null) break;
                    line = line.trim();
                    if (line.equalsIgnoreCase("exit")) {
                        logger.info("Получена команда exit. Завершение работы...");
                        System.exit(0);
                    } else if (!line.isEmpty()) {
                        logger.warn("Неизвестная команда консоли. Доступна только: exit");
                    }
                } catch (IOException e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        logger.error("Ошибка чтения консоли: {}", e.getMessage());
                    }
                    break;
                }
            }
        });
        consoleThread.setDaemon(true);
        consoleThread.setName("console-reader");
        consoleThread.start();

        udpServer.runLoop();
    }

    private static String getParam(String[] args, int index, String envKey, String defaultValue) {
        if (args != null && args.length > index && args[index] != null && !args[index].isEmpty()) {
            return args[index];
        }
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return defaultValue;
    }

    private static int parsePort(String value) {
        try {
            int p = Integer.parseInt(value);
            if (p < 1 || p > 65535) {
                logger.warn("Некорректный порт {}, используется {}", p, DEFAULT_PORT);
                return DEFAULT_PORT;
            }
            return p;
        } catch (NumberFormatException e) {
            logger.warn("Не удалось разобрать порт '{}', используется {}", value, DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }
}