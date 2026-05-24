package app.common.exceptions;

/**
 * Исключение при некорректном вводе
 */
public class InvalidInputException extends RuntimeException {
    public InvalidInputException(String message) {
        super(message);
    }
}