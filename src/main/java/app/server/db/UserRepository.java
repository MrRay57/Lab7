package app.server.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private final DatabaseManager dbManager;

    public UserRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public boolean register(String login, String passwordHash) {
        if (login == null || login.trim().isEmpty() || passwordHash == null) {
            return false;
        }
        String sql = "INSERT INTO users (login, password_hash) VALUES (?, ?)";
        try (Connection conn = dbManager.createConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login.trim());
            ps.setString(2, passwordHash);
            ps.executeUpdate();
            logger.info("Зарегистрирован новый пользователь: {}", login);
            return true;
        } catch (SQLException e) {
            logger.warn("Ошибка при регистрации пользователя '{}': {}", login, e.getMessage());
            return false;
        }
    }

    public boolean authenticate(String login, String passwordHash) {
        if (login == null || passwordHash == null) {
            return false;
        }
        String sql = "SELECT 1 FROM users WHERE login = ? AND password_hash = ?";
        try (Connection conn = dbManager.createConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login.trim());
            ps.setString(2, passwordHash);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Ошибка при аутентификации пользователя '{}': {}", login, e.getMessage());
            return false;
        }
    }

    public boolean userExists(String login) {
        if (login == null) {
            return false;
        }
        String sql = "SELECT 1 FROM users WHERE login = ?";
        try (Connection conn = dbManager.createConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Ошибка при проверке пользователя '{}': {}", login, e.getMessage());
            return false;
        }
    }
}
