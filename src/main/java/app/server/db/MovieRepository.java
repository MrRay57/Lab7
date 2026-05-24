package app.server.db;

import app.common.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 * Репозиторий для взаимодействия с таблицей фильмов в базе данных
 */
public class MovieRepository {
    private static final Logger logger = LoggerFactory.getLogger(MovieRepository.class);

    private final DatabaseManager dbManager;

    public MovieRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public long insert(long mapKey, Movie movie, String ownerLogin) throws SQLException {
        String sql = "INSERT INTO movies (map_key, owner_login, name, coord_x, coord_y, " +
                "creation_date, oscars_count, total_box_office, length, genre, " +
                "operator_name, operator_height, operator_eye_color, operator_nationality, " +
                "operator_loc_x, operator_loc_y, operator_loc_z) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING id";

        try (Connection conn = dbManager.createConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, mapKey);
            ps.setString(2, ownerLogin);
            ps.setString(3, movie.getName());
            ps.setLong(4, movie.getCoordinates().getX());
            ps.setDouble(5, movie.getCoordinates().getY());
            ps.setTimestamp(6, Timestamp.from(movie.getCreationDate().toInstant()));
            ps.setLong(7, movie.getOscarsCount());
            ps.setFloat(8, movie.getTotalBoxOffice());
            ps.setInt(9, movie.getLength());
            ps.setString(10, movie.getGenre() != null ? movie.getGenre().name() : null);

            setOperatorParams(ps, 11, movie.getOperator());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long generatedId = rs.getLong("id");
                    logger.info("Фильм добавлен в БД: id={}, ключ={}, владелец={}", generatedId, mapKey, ownerLogin);
                    return generatedId;
                }
                throw new SQLException("Не удалось получить сгенерированный id.");
            }
        }
    }

    public boolean update(long movieId, Movie movie, String ownerLogin) throws SQLException {
        String sql = "UPDATE movies SET name=?, coord_x=?, coord_y=?, oscars_count=?, " +
                "total_box_office=?, length=?, genre=?, operator_name=?, operator_height=?, " +
                "operator_eye_color=?, operator_nationality=?, operator_loc_x=?, " +
                "operator_loc_y=?, operator_loc_z=? " +
                "WHERE id=? AND owner_login=?";

        try (Connection conn = dbManager.createConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setMovieBaseParams(ps, 1, movie);
            ps.setLong(15, movieId);
            ps.setString(16, ownerLogin);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteByKey(long mapKey, String ownerLogin) throws SQLException {
        String sql = "DELETE FROM movies WHERE map_key=? AND owner_login=?";
        try (Connection conn = dbManager.createConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, mapKey);
            ps.setString(2, ownerLogin);
            return ps.executeUpdate() > 0;
        }
    }

    public int deleteAllByOwner(String ownerLogin) throws SQLException {
        String sql = "DELETE FROM movies WHERE owner_login=?";
        try (Connection conn = dbManager.createConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerLogin);
            return ps.executeUpdate();
        }
    }

    public boolean replaceByKey(long mapKey, Movie movie, String ownerLogin) throws SQLException {
        String sql = "UPDATE movies SET name=?, coord_x=?, coord_y=?, oscars_count=?, " +
                "total_box_office=?, length=?, genre=?, operator_name=?, operator_height=?, " +
                "operator_eye_color=?, operator_nationality=?, operator_loc_x=?, " +
                "operator_loc_y=?, operator_loc_z=? " +
                "WHERE map_key=? AND owner_login=?";

        try (Connection conn = dbManager.createConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setMovieBaseParams(ps, 1, movie);
            ps.setLong(15, mapKey);
            ps.setString(16, ownerLogin);

            return ps.executeUpdate() > 0;
        }
    }

    public TreeMap<Long, Movie> loadAll(Map<Long, String> ownersOut) throws SQLException {
        TreeMap<Long, Movie> result = new TreeMap<>();
        String sql = "SELECT * FROM movies ORDER BY id ASC";

        try (Connection conn = dbManager.createConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                long id = rs.getLong("id");
                long mapKey = rs.getLong("map_key");
                String ownerLogin = rs.getString("owner_login");

                Coordinates coordinates = new Coordinates(
                        rs.getLong("coord_x"),
                        rs.getDouble("coord_y")
                );

                ZonedDateTime creationDate = rs.getTimestamp("creation_date")
                        .toInstant()
                        .atZone(ZoneOffset.UTC);

                String genreStr = rs.getString("genre");
                MovieGenre genre = genreStr != null ? MovieGenre.valueOf(genreStr) : null;

                Person operator = null;
                String opName = rs.getString("operator_name");
                if (opName != null) {
                    String eyeColorStr = rs.getString("operator_eye_color");
                    Color eyeColor = eyeColorStr != null ? Color.valueOf(eyeColorStr) : null;
                    String natStr = rs.getString("operator_nationality");
                    Country nationality = natStr != null ? Country.valueOf(natStr) : null;
                    Location location = new Location(
                            rs.getFloat("operator_loc_x"),
                            rs.getInt("operator_loc_y"),
                            rs.getInt("operator_loc_z")
                    );
                    operator = new Person(opName, rs.getInt("operator_height"),
                            eyeColor, nationality, location);
                }

                Movie movie = new Movie(id,
                        rs.getString("name"),
                        coordinates,
                        creationDate,
                        rs.getLong("oscars_count"),
                        rs.getFloat("total_box_office"),
                        rs.getInt("length"),
                        genre,
                        operator);

                result.put(mapKey, movie);
                if (ownersOut != null) {
                    ownersOut.put(mapKey, ownerLogin);
                }
            }
        }

        logger.info("Загружено {} фильмов из БД.", result.size());
        return result;
    }

    private void setMovieBaseParams(PreparedStatement ps, int offset, Movie movie) throws SQLException {
        ps.setString(offset, movie.getName());
        ps.setLong(offset + 1, movie.getCoordinates().getX());
        ps.setDouble(offset + 2, movie.getCoordinates().getY());
        ps.setLong(offset + 3, movie.getOscarsCount());
        ps.setFloat(offset + 4, movie.getTotalBoxOffice());
        ps.setInt(offset + 5, movie.getLength());
        ps.setString(offset + 6, movie.getGenre() != null ? movie.getGenre().name() : null);
        setOperatorParams(ps, offset + 7, movie.getOperator());
    }

    private void setOperatorParams(PreparedStatement ps, int offset, Person op) throws SQLException {
        if (op != null) {
            ps.setString(offset, op.getName());
            ps.setInt(offset + 1, op.getHeight());
            ps.setString(offset + 2, op.getEyeColor() != null ? op.getEyeColor().name() : null);
            ps.setString(offset + 3, op.getNationality() != null ? op.getNationality().name() : null);
            Location loc = op.getLocation();
            if (loc != null) {
                ps.setFloat(offset + 4, loc.getX());
                ps.setInt(offset + 5, loc.getY());
                ps.setInt(offset + 6, loc.getZ());
            } else {
                ps.setNull(offset + 4, Types.REAL);
                ps.setNull(offset + 5, Types.INTEGER);
                ps.setNull(offset + 6, Types.INTEGER);
            }
        } else {
            ps.setNull(offset, Types.VARCHAR);
            ps.setNull(offset + 1, Types.INTEGER);
            ps.setNull(offset + 2, Types.VARCHAR);
            ps.setNull(offset + 3, Types.VARCHAR);
            ps.setNull(offset + 4, Types.REAL);
            ps.setNull(offset + 5, Types.INTEGER);
            ps.setNull(offset + 6, Types.INTEGER);
        }
    }
}
