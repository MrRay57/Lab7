package app.client.io;

import app.common.models.*;
import app.common.exceptions.InvalidInputException;

import java.time.ZonedDateTime;
import java.util.Arrays;

/**
 * Класс-строитель для пошагового интерактивного создания объекта Movie на основе пользовательского ввода
 */
public class MovieBuilder {
    private final ApplicationInputManager inputManager;

    public MovieBuilder(ApplicationInputManager inputManager, boolean ignoredInteractive) {
        this.inputManager = inputManager;
    }

    private boolean isInteractive() {
        return !inputManager.isScripting();
    }

    public Movie build() throws InvalidInputException {
        if (isInteractive()) {
            System.out.println("--- Ввод данных фильма ---");
        }

        String name = askString("Введите название фильма: ", false);
        Coordinates coordinates = askCoordinates();
        long oscarsCount = askLong("Введите количество Оскаров (от 1 до " + Long.MAX_VALUE + "): ", 1L, Long.MAX_VALUE, false);
        Float totalBoxOffice = askFloat("Введите кассовые сборы (больше 0): ", 0.0001f, Float.MAX_VALUE, false);
        int length = askInt("Введите продолжительность фильма (от 1 до 2147483647): ", 1, Integer.MAX_VALUE, false);
        MovieGenre genre = askEnum("Выберите жанр", MovieGenre.class, true);
        Person operator = askPerson();

        return new Movie(1L, name, coordinates, ZonedDateTime.now(), oscarsCount, totalBoxOffice, length, genre, operator);
    }

    private Coordinates askCoordinates() {
        if (isInteractive()) System.out.println("Ввод координат фильма: ");
        Long x = askLong("Введите координату X (до 626): ", Long.MIN_VALUE, 626L, false);
        double y = askDouble("Введите координату Y (любое число): ", false);
        return new Coordinates(x, y);
    }

    private Person askPerson() {
        String answer = askString("Хотите добавить оператора? (y для добавления или Enter для пропуска): ", true);
        if (answer == null || !answer.equalsIgnoreCase("y")) {
            return null;
        }

        if (isInteractive()) System.out.println("Ввод данных оператора: ");
        String name = askString("Введите имя оператора: ", false);
        int height = askInt("Введите рост оператора (от 1 до 2147483647): ", 1, Integer.MAX_VALUE, false);
        Color eyeColor = askEnum("Выберите цвет глаз", Color.class, true);
        Country nationality = askEnum("Выберите национальность", Country.class, false);
        Location location = askLocation();

        return new Person(name, height, eyeColor, nationality, location);
    }

    private Location askLocation() {
        if (isInteractive()) System.out.println("Ввод локации оператора: ");
        Float x = askFloat("Введите координату X (любое число): ", -Float.MAX_VALUE, Float.MAX_VALUE, false);
        int y = askInt("Введите координату Y (любое число): ", Integer.MIN_VALUE, Integer.MAX_VALUE, false);
        int z = askInt("Введите координату Z (любое число): ", Integer.MIN_VALUE, Integer.MAX_VALUE, false);
        return new Location(x, y, z);
    }

    private String askString(String message, boolean canBeNull) {
        while (true) {
            if (isInteractive()) System.out.print(message);
            String input = inputManager.readLine();
            if (input == null || input.isEmpty()) {
                if (canBeNull) return null;
                printError("Строка не может быть пустой! Попробуйте снова.");
                continue;
            }
            return input;
        }
    }

    private Long askLong(String message, long min, long max, boolean canBeNull) {
        while (true) {
            if (isInteractive()) System.out.print(message);
            String input = inputManager.readLine();
            if (input == null || input.isEmpty()) {
                if (canBeNull) return null;
                printError("Поле обязательно для заполнения! Попробуйте снова.");
                continue;
            }
            try {
                long value = Long.parseLong(input);
                if (value < min || value > max) {
                    printError("Значение должно быть от " + min + " до " + max + "!");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                printError("Введите корректное целое число!");
            }
        }
    }

    private int askInt(String message, int min, int max, boolean canBeNull) {
        while (true) {
            if (isInteractive()) System.out.print(message);
            String input = inputManager.readLine();
            if (input == null || input.isEmpty()) {
                if (canBeNull) throw new IllegalStateException("Внутренняя ошибка: int не может быть null");
                printError("Поле обязательно для заполнения! Попробуйте снова.");
                continue;
            }
            try {
                int value = Integer.parseInt(input);
                if (value < min || value > max) {
                    printError("Значение должно быть от " + min + " до " + max + "!");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                printError("Введите корректное целое число!");
            }
        }
    }

    private Float askFloat(String message, float min, float max, boolean canBeNull) {
        while (true) {
            if (isInteractive()) System.out.print(message);
            String input = inputManager.readLine();
            if (input == null || input.isEmpty()) {
                if (canBeNull) return null;
                printError("Поле обязательно для заполнения! Попробуйте снова.");
                continue;
            }
            try {
                float value = Float.parseFloat(input);
                if (value < min || value > max) {
                    printError("Значение вне допустимых границ!");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                printError("Введите корректное дробное число (через точку)!");
            }
        }
    }

    private double askDouble(String message, boolean canBeNull) {
        while (true) {
            if (isInteractive()) System.out.print(message);
            String input = inputManager.readLine();
            if (input == null || input.isEmpty()) {
                if (canBeNull) throw new IllegalStateException("Внутренняя ошибка: double не может быть null");
                printError("Поле обязательно для заполнения! Попробуйте снова.");
                continue;
            }
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                printError("Введите корректное дробное число (через точку)!");
            }
        }
    }

    private <T extends Enum<T>> T askEnum(String message, Class<T> enumClass, boolean canBeNull) {
        String options = Arrays.toString(enumClass.getEnumConstants());
        while (true) {
            if (isInteractive()) System.out.print(message + " " + options + (canBeNull ? " (Enter для пропуска): " : ": "));
            String input = inputManager.readLine();
            if (input == null || input.isEmpty()) {
                if (canBeNull) return null;
                printError("Поле обязательно! Выберите одно из значений: " + options);
                continue;
            }
            try {
                return Enum.valueOf(enumClass, input.toUpperCase());
            } catch (IllegalArgumentException e) {
                printError("Нет такого варианта! Выберите из предложенного списка: " + options);
            }
        }
    }

    private void printError(String message) {
        if (!isInteractive()) {
            throw new InvalidInputException("Ошибка в скрипте: " + message);
        }
        System.err.println("Ошибка: " + message);
    }
}