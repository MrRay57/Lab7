package app.client.managers;

import app.client.io.ApplicationInputManager;
import app.client.io.MovieBuilder;
import app.client.network.UDPClient;
import app.common.network.Request;
import app.common.network.Response;
import app.common.network.ResponseStatus;
import app.common.models.Movie;
import app.common.exceptions.InvalidInputException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class ClientCommandManager {
    private final ApplicationInputManager inputManager;
    private final UDPClient client;
    private final MovieBuilder movieBuilder;

    private String login;
    private String passwordHash;

    public ClientCommandManager(ApplicationInputManager inputManager, UDPClient client, MovieBuilder movieBuilder) {
        this.inputManager = inputManager;
        this.client = client;
        this.movieBuilder = movieBuilder;
    }

    public void setCredentials(String login, String passwordHash) {
        this.login = login;
        this.passwordHash = passwordHash;
    }

    public void startInteractiveMode() {
        System.out.println("Введите 'help' для получения списка команд.");
        while (true) {
            if (!inputManager.isScripting()) {
                System.out.print("\n> ");
            }

            String input = inputManager.readLine();

            if (input == null) break;

            input = input.trim();
            if (input.isEmpty()) continue;

            String[] tokens = input.split("\\s+", 2);
            String commandName = tokens[0].toLowerCase();
            String argument = tokens.length > 1 ? tokens[1] : "";

            if (commandName.equals("exit")) {
                System.out.println("Завершение работы клиента.");
                break;
            }

            if (commandName.equals("save")) {
                System.out.println("Команда 'save' недоступна клиенту. Данные сохраняются в БД автоматически.");
                continue;
            }

            if (commandName.equals("execute_script")) {
                handleExecuteScript(argument);
                continue;
            }

            try {
                Request request = createRequest(commandName, argument);
                if (request != null) {
                    Response response = client.sendAndReceive(request);
                    handleResponse(response);
                }
            } catch (InvalidInputException e) {
                System.out.println("Отмена команды: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Непредвиденная ошибка: " + e.getMessage());
            }
        }
    }

    private void handleExecuteScript(String argument) {
        if (argument == null || argument.trim().isEmpty()) {
            System.out.println("Ошибка: укажите путь к скрипту.");
            return;
        }
        File scriptFile = new File(argument.trim());
        if (!scriptFile.exists() || !scriptFile.canRead()) {
            System.out.println("Ошибка: файл скрипта не существует или недоступен для чтения.");
            return;
        }
        try {
            String absPath = scriptFile.getAbsolutePath();
            if (inputManager.isScriptActive(absPath)) {
                System.out.println("Ошибка: обнаружена рекурсия в скрипте (" + absPath + ").");
                return;
            }
            Scanner fileScanner = new Scanner(scriptFile);
            inputManager.pushScanner(fileScanner, absPath);
            System.out.println("Выполнение скрипта: " + argument);
        } catch (FileNotFoundException e) {
            System.out.println("Ошибка при открытии файла скрипта.");
        }
    }

    private Request createRequest(String commandName, String argument) throws InvalidInputException {
        switch (commandName) {
            case "insert":
            case "update":
            case "replace_if_greater":
            case "replace_if_lowe":
                if (argument.isEmpty()) {
                    System.out.println("Ошибка: для этой команды требуется аргумент (ключ или id).");
                    return null;
                }
                Movie movieForCommand = movieBuilder.build();
                return new Request(commandName, argument, movieForCommand, login, passwordHash);

            case "remove_lower":
                Movie movieLower = movieBuilder.build();
                return new Request(commandName, "", movieLower, login, passwordHash);

            default:
                return new Request(commandName, argument, null, login, passwordHash);
        }
    }

    private void handleResponse(Response response) {
        if (response.getStatus() == ResponseStatus.ERROR) {
            System.err.println("Ошибка: " + response.getMessage());
        } else {
            if (response.getMessage() != null && !response.getMessage().isEmpty()) {
                System.out.println(response.getMessage());
            }
            if (response.getPayload() != null) {
                if (response.getPayload() instanceof Iterable) {
                    for (Object item : (Iterable<?>) response.getPayload()) {
                        System.out.println("  " + item.toString());
                    }
                } else {
                    System.out.println("  " + response.getPayload().toString());
                }
            }
        }
    }
}