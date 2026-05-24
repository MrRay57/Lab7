package app.client.io;

import java.util.Scanner;
import java.util.Stack;

/**
 * Менеджер пользовательского ввода
 */
public class ApplicationInputManager implements InputManager {
    private final Stack<Scanner> scanners = new Stack<>();
    private final Stack<String> activeScriptPaths = new Stack<>();

    public ApplicationInputManager(Scanner consoleScanner) {
        scanners.push(consoleScanner);
    }

    public void pushScanner(Scanner scanner, String path) {
        scanners.push(scanner);
        activeScriptPaths.push(path);
    }

    public void popScanner() {
        if (scanners.size() > 1) {
            scanners.pop();
            activeScriptPaths.pop();
        }
    }

    @Override
    public String readLine() {
        while (!scanners.isEmpty()) {
            Scanner currentScanner = scanners.peek();
            if (currentScanner.hasNextLine()) {
                return currentScanner.nextLine().trim();
            } else {
                if (scanners.size() > 1) {
                    popScanner();
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    public boolean isScripting() {
        return scanners.size() > 1;
    }

    public boolean isScriptActive(String path) {
        return activeScriptPaths.contains(path);
    }
}