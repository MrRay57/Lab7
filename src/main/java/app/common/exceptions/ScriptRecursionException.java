package app.common.exceptions;

/**
 * Исключение предотвращение рекурсии
 */
public class ScriptRecursionException extends RuntimeException {
    public ScriptRecursionException(String message) {
        super(message);
    }
}