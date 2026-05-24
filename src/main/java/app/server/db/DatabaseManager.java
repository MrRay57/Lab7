package app.server.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager(String host, String dbName, String user, String password) {
        this.url = "jdbc:postgresql://" + host + "/" + dbName;
        this.user = user;
        this.password = password;
    }

    public Connection createConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void initSchema() throws SQLException {
        String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                "login VARCHAR(255) PRIMARY KEY," +
                "password_hash VARCHAR(255) NOT NULL" +
                ");";

        String createSequence = "CREATE SEQUENCE IF NOT EXISTS movie_id_seq START 1 INCREMENT 1;";

        String createMovies = "CREATE TABLE IF NOT EXISTS movies (" +
                "id BIGINT PRIMARY KEY DEFAULT NEXTVAL('movie_id_seq')," +
                "map_key BIGINT NOT NULL," +
                "owner_login VARCHAR(255) NOT NULL REFERENCES users(login)," +
                "name VARCHAR(255) NOT NULL," +
                "coord_x BIGINT NOT NULL," +
                "coord_y DOUBLE PRECISION NOT NULL," +
                "creation_date TIMESTAMPTZ NOT NULL," +
                "oscars_count BIGINT NOT NULL," +
                "total_box_office REAL NOT NULL," +
                "length INTEGER NOT NULL," +
                "genre VARCHAR(50)," +
                "operator_name VARCHAR(255)," +
                "operator_height INTEGER," +
                "operator_eye_color VARCHAR(50)," +
                "operator_nationality VARCHAR(50)," +
                "operator_loc_x REAL," +
                "operator_loc_y INTEGER," +
                "operator_loc_z INTEGER" +
                ");";

        try (Connection conn = createConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createUsers);
            stmt.executeUpdate(createSequence);
            stmt.executeUpdate(createMovies);
            logger.info("Схема базы данных успешно инициализирована.");
        }
    }
}
