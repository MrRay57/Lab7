package app.client.io;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Класс для безопасного чтения данных из консоли
 */
public class UserIO {
    private final Scanner scanner;

    public UserIO(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine() {
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException e) {
            System.out.println("\nОбнаружен конец потока ввода (Ctrl+D). Завершение работы клиента...");
            System.exit(0);
            return "";
        }
    }

    public String readString(String prompt, boolean canBeEmpty) {
        while (true) {
            System.out.print(prompt);
            String input = readLine();
            if (!canBeEmpty && input.isEmpty()) {
                System.out.println("Ошибка: Строка не может быть пустой. Повторите ввод.");
            } else {
                return input.isEmpty() ? null : input;
            }
        }
    }

    public Long readLong(String prompt, Long min, Long max, boolean canBeNull) {
        while (true) {
            System.out.print(prompt);
            String input = readLine();
            if (input.isEmpty()) {
                if (canBeNull)
                    return null;
                System.out.println("Ошибка: Значение не может быть пустым. Повторите ввод.");
                continue;
            }
            try {
                long value = Long.parseLong(input);
                if (min != null && value < min) {
                    System.out.println("Ошибка: Значение должно быть не меньше " + min);
                    continue;
                }
                if (max != null && value > max) {
                    System.out.println("Ошибка: Значение должно быть не больше " + max);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: Введите целое число.");
            }
        }
    }

    public Float readFloat(String prompt, Float min, boolean canBeNull) {
        while (true) {
            System.out.print(prompt);
            String input = readLine();
            if (input.isEmpty()) {
                if (canBeNull)
                    return null;
                System.out.println("Ошибка: Значение не может быть пустым. Повторите ввод.");
                continue;
            }
            try {
                float value = Float.parseFloat(input);
                if (min != null && value <= min) {
                    System.out.println("Ошибка: Значение должно быть больше " + min);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: Введите дробное число (через точку).");
            }
        }
    }

    public Double readDouble(String prompt, boolean canBeNull) {
        while (true) {
            System.out.print(prompt);
            String input = readLine();
            if (input.isEmpty()) {
                if (canBeNull)
                    return null;
                System.out.println("Ошибка: Значение не может быть пустым.");
                continue;
            }
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: Введите дробное число.");
            }
        }
    }

    public Integer readInt(String prompt, Integer min, boolean canBeNull) {
        while (true) {
            System.out.print(prompt);
            String input = readLine();
            if (input.isEmpty()) {
                if (canBeNull)
                    return null;
                System.out.println("Ошибка: Значение не может быть пустым.");
                continue;
            }
            try {
                int value = Integer.parseInt(input);
                if (min != null && value <= min) {
                    System.out.println("Ошибка: Значение должно быть больше " + min);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: Введите целое число.");
            }
        }
    }
}