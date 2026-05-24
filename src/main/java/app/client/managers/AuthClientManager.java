package app.client.managers;

import app.client.network.UDPClient;
import app.common.network.Request;
import app.common.network.Response;
import app.common.network.ResponseStatus;
import app.common.utils.HashUtils;

import java.util.Scanner;

public class AuthClientManager {
    private final UDPClient client;
    private final Scanner scanner;

    private String login;
    private String passwordHash;

    public AuthClientManager(UDPClient client, Scanner scanner) {
        this.client = client;
        this.scanner = scanner;
    }

    public boolean runAuthFlow() {
        System.out.println("=== Добро пожаловать в систему управления фильмами ===");
        System.out.println("Для работы необходимо войти в аккаунт или зарегистрироваться.");

        while (true) {
            System.out.println("\n1 — Войти");
            System.out.println("2 — Зарегистрироваться");
            System.out.print("Ваш выбор: ");

            String choice = readLine();
            if (choice == null) {
                System.out.println("Завершение работы.");
                return false;
            }

            switch (choice.trim()) {
                case "1":
                    if (doLogin()) return true;
                    break;
                case "2":
                    if (doRegister()) return true;
                    break;
                default:
                    System.err.println("Введите 1 или 2.");
            }
        }
    }

    private boolean doLogin() {
        System.out.println("\n--- Вход ---");
        String inputLogin = askLogin();
        if (inputLogin == null) return false;

        String inputPassword = askPassword();
        if (inputPassword == null) return false;

        String hash = HashUtils.md2(inputPassword);
        Request request = new Request("login", "", null, inputLogin, hash);
        Response response = client.sendAndReceive(request);

        if (response.getStatus() == ResponseStatus.OK) {
            System.out.println(response.getMessage());
            this.login = inputLogin;
            this.passwordHash = hash;
            return true;
        } else {
            System.err.println("Ошибка: " + response.getMessage());
            return false;
        }
    }

    private boolean doRegister() {
        System.out.println("\n--- Регистрация ---");
        String inputLogin = askLogin();
        if (inputLogin == null) return false;

        String inputPassword = askPassword();
        if (inputPassword == null) return false;

        String confirmedPassword = askPasswordConfirm();
        if (confirmedPassword == null) return false;

        if (!inputPassword.equals(confirmedPassword)) {
            System.err.println("Ошибка: пароли не совпадают. Попробуйте снова.");
            return false;
        }

        String hash = HashUtils.md2(inputPassword);
        Request request = new Request("register", "", null, inputLogin, hash);
        Response response = client.sendAndReceive(request);

        if (response.getStatus() == ResponseStatus.OK) {
            System.out.println(response.getMessage());
            this.login = inputLogin;
            this.passwordHash = hash;
            return true;
        } else {
            System.err.println("Ошибка: " + response.getMessage());
            return false;
        }
    }

    private String askLogin() {
        while (true) {
            System.out.print("Введите логин: ");
            String input = readLine();
            if (input == null) return null;
            input = input.trim();
            if (input.isEmpty()) {
                System.err.println("Логин не может быть пустым.");
                continue;
            }
            if (input.length() > 255) {
                System.err.println("Логин слишком длинный (максимум 255 символов).");
                continue;
            }
            return input;
        }
    }

    private String askPassword() {
        while (true) {
            System.out.print("Введите пароль: ");
            String input = readLine();
            if (input == null) return null;
            if (input.isEmpty()) {
                System.err.println("Пароль не может быть пустым.");
                continue;
            }
            return input;
        }
    }

    private String askPasswordConfirm() {
        System.out.print("Повторите пароль: ");
        return readLine();
    }

    private String readLine() {
        try {
            if (scanner.hasNextLine()) {
                return scanner.nextLine();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
