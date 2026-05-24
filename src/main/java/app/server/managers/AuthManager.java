package app.server.managers;

import app.common.utils.HashUtils;
import app.server.db.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Менеджер для аутентификации и регистрации пользователей
 */
public class AuthManager {
    private static final Logger logger = LoggerFactory.getLogger(AuthManager.class);

    private final UserRepository userRepository;

    public AuthManager(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static String hashPassword(String password) {
        return HashUtils.md2(password);
    }

    public boolean register(String login, String passwordHash) {
        if (login == null || login.trim().isEmpty()) {
            return false;
        }
        if (userRepository.userExists(login.trim())) {
            return false;
        }
        return userRepository.register(login.trim(), passwordHash);
    }

    public boolean authenticate(String login, String passwordHash) {
        if (login == null || passwordHash == null) {
            return false;
        }
        return userRepository.authenticate(login.trim(), passwordHash);
    }

    public boolean userExists(String login) {
        if (login == null) {
            return false;
        }
        return userRepository.userExists(login.trim());
    }
}
