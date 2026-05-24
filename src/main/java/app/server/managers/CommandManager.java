package app.server.managers;

import app.common.network.Request;
import app.common.network.Response;
import app.common.network.ResponseStatus;
import app.common.models.Movie;
import app.server.db.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Менеджер обработки и выполнения серверных команд
 */
public class CommandManager {
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);

    private final CollectionManager collectionManager;
    private final AuthManager authManager;
    private final MovieRepository movieRepository;

    private final Map<String, CommandHandler> commands = new HashMap<>();

    @FunctionalInterface
    private interface CommandHandler {
        Response execute(Request request) throws Exception;
    }

    public CommandManager(CollectionManager collectionManager,
                          AuthManager authManager,
                          MovieRepository movieRepository) {
        this.collectionManager = collectionManager;
        this.authManager = authManager;
        this.movieRepository = movieRepository;
        registerCommands();
    }

    private void registerCommands() {
        commands.put("help", req -> handleHelp());
        commands.put("info", req -> handleInfo());
        commands.put("show", req -> handleShow());
        commands.put("insert", req -> handleInsert(req.getStringArgument(), (Movie) req.getObjectArgument(), req.getLogin()));
        commands.put("update", req -> handleUpdate(req.getStringArgument(), (Movie) req.getObjectArgument(), req.getLogin()));
        commands.put("remove_key", req -> handleRemoveKey(req.getStringArgument(), req.getLogin()));
        commands.put("clear", req -> handleClear(req.getLogin()));
        commands.put("remove_lower", req -> handleRemoveLower((Movie) req.getObjectArgument(), req.getLogin()));
        commands.put("replace_if_greater", req -> handleReplaceIfGreater(req.getStringArgument(), (Movie) req.getObjectArgument(), req.getLogin()));
        commands.put("replace_if_lowe", req -> handleReplaceIfLowe(req.getStringArgument(), (Movie) req.getObjectArgument(), req.getLogin()));
        commands.put("average_of_oscars_count", req -> handleAverageOscars());
        commands.put("count_less_than_length", req -> handleCountLessThanLength(req.getStringArgument()));
        commands.put("print_field_descending_length", req -> handlePrintFieldDescendingLength());
    }

    public Response execute(Request request) {
        String commandName = request.getCommandName();
        String argument = request.getStringArgument();
        String login = request.getLogin();
        String passwordHash = request.getPasswordHash();

        logger.info("Выполнение команды '{}' от пользователя '{}'", commandName, login);

        try {
            if ("register".equals(commandName)) {
                return handleRegister(login, passwordHash);
            }
            if ("login".equals(commandName)) {
                return handleLogin(login, passwordHash);
            }

            if (!authManager.authenticate(login, passwordHash)) {
                logger.warn("Неудачная попытка авторизации для команды '{}' от '{}'", commandName, login);
                return new Response(ResponseStatus.ERROR,
                        "Ошибка авторизации. Проверьте логин и пароль.");
            }

            CommandHandler handler = commands.get(commandName);
            if (handler != null) {
                return handler.execute(request);
            } else {
                logger.warn("Получена неизвестная команда: '{}'", commandName);
                return new Response(ResponseStatus.ERROR, "Неизвестная команда: " + commandName);
            }

        } catch (NumberFormatException e) {
            return new Response(ResponseStatus.ERROR, "Неверный формат числового аргумента: " + argument);
        } catch (ClassCastException e) {
            return new Response(ResponseStatus.ERROR, "Неверный тип объекта в запросе.");
        } catch (SQLException e) {
            logger.error("Ошибка базы данных при выполнении команды '{}': {}", commandName, e.getMessage());
            return new Response(ResponseStatus.ERROR, "Ошибка базы данных: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Непредвиденная ошибка при выполнении команды '{}'", commandName, e);
            return new Response(ResponseStatus.ERROR, "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    private Response handleRegister(String login, String passwordHash) {
        if (login == null || login.trim().isEmpty()) {
            return new Response(ResponseStatus.ERROR, "Логин не может быть пустым.");
        }
        if (passwordHash == null || passwordHash.isEmpty()) {
            return new Response(ResponseStatus.ERROR, "Пароль не может быть пустым.");
        }
        boolean success = authManager.register(login.trim(), passwordHash);
        if (success) {
            logger.info("Пользователь '{}' успешно зарегистрирован.", login);
            return new Response(ResponseStatus.OK, "Регистрация успешна. Добро пожаловать, " + login.trim() + "!");
        } else {
            return new Response(ResponseStatus.ERROR,
                    "Пользователь с логином '" + login.trim() + "' уже существует.");
        }
    }

    private Response handleLogin(String login, String passwordHash) {
        if (login == null || login.trim().isEmpty()) {
            return new Response(ResponseStatus.ERROR, "Логин не может быть пустым.");
        }
        boolean success = authManager.authenticate(login.trim(), passwordHash);
        if (success) {
            logger.info("Пользователь '{}' успешно авторизован.", login);
            return new Response(ResponseStatus.OK, "Авторизация успешна. Добро пожаловать, " + login.trim() + "!");
        } else {
            return new Response(ResponseStatus.ERROR,
                    "Неверный логин или пароль.");
        }
    }

    private Response handleHelp() {
        String helpMsg = "  help : вывести справку по доступным командам\n" +
                "  info : вывести информацию о коллекции\n" +
                "  show : вывести все элементы коллекции\n" +
                "  insert <ключ> : добавить новый элемент с заданным ключом\n" +
                "  update <id> : обновить значение элемента коллекции, id которого равен заданному\n" +
                "  remove_key <ключ> : удалить элемент из коллекции по его ключу (только свои)\n" +
                "  clear : очистить коллекцию (только свои элементы)\n" +
                "  remove_lower : удалить из коллекции все свои элементы, меньшие, чем заданный\n" +
                "  replace_if_greater <ключ> : заменить значение по ключу, если новое больше старого\n" +
                "  replace_if_lowe <ключ> : заменить значение по ключу, если новое меньше старого\n" +
                "  average_of_oscars_count : вывести среднее значение поля oscarsCount\n" +
                "  count_less_than_length <длина> : вывести количество элементов, длина которых меньше заданной\n" +
                "  print_field_descending_length : вывести значения поля length всех элементов по убыванию\n" +
                "  execute_script <файл> : считать и исполнить скрипт из указанного файла\n" +
                "  exit : завершить работу клиентского приложения";
        return new Response(ResponseStatus.OK, helpMsg);
    }

    private Response handleInfo() {
        String infoMsg = "--- Информация о коллекции ---\n" +
                "  Тип коллекции: " + collectionManager.getCollectionType() + "\n" +
                "  Дата инициализации: " + collectionManager.getInitializationDate() + "\n" +
                "  Количество элементов: " + collectionManager.getSize();
        return new Response(ResponseStatus.OK, infoMsg);
    }

    private Response handleShow() {
        if (collectionManager.getSize() == 0) {
            return new Response(ResponseStatus.OK, "Коллекция пуста.");
        }
        ArrayList<Movie> sortedMovies = new ArrayList<>(collectionManager.getSortedByLocation());
        return new Response(ResponseStatus.OK, "--- Элементы коллекции ---", sortedMovies);
    }

    private Response handleInsert(String argument, Movie movie, String ownerLogin) throws SQLException {
        if (argument == null || argument.trim().isEmpty()) {
            return new Response(ResponseStatus.ERROR, "Укажите ключ для вставки.");
        }
        if (movie == null) {
            return new Response(ResponseStatus.ERROR, "Объект фильма не передан.");
        }
        if (!movie.validate()) {
            return new Response(ResponseStatus.ERROR, "Невалидные данные фильма.");
        }

        Long key = Long.parseLong(argument.trim());

        if (collectionManager.containsKey(key)) {
            return new Response(ResponseStatus.ERROR, "Элемент с ключом " + key + " уже существует.");
        }

        movie.setCreationDate(ZonedDateTime.now());
        long generatedId = movieRepository.insert(key, movie, ownerLogin);
        movie.setId(generatedId);
        collectionManager.insertIntoMemory(key, movie, ownerLogin);

        return new Response(ResponseStatus.OK, "Элемент успешно добавлен с id=" + generatedId + ".");
    }

    private Response handleUpdate(String argument, Movie movie, String ownerLogin) throws SQLException {
        if (argument == null || argument.trim().isEmpty()) {
            return new Response(ResponseStatus.ERROR, "Укажите id для обновления.");
        }
        if (movie == null) {
            return new Response(ResponseStatus.ERROR, "Объект фильма не передан.");
        }
        if (!movie.validate()) {
            return new Response(ResponseStatus.ERROR, "Невалидные данные фильма.");
        }

        Long id = Long.parseLong(argument.trim());
        Long key = collectionManager.getKeyById(id);

        if (key == null) {
            return new Response(ResponseStatus.ERROR, "Элемент с id=" + id + " не найден.");
        }

        String actualOwner = collectionManager.getOwnerByKey(key);
        if (!ownerLogin.equals(actualOwner)) {
            return new Response(ResponseStatus.ERROR,
                    "Нет прав: элемент с id=" + id + " принадлежит другому пользователю.");
        }

        boolean dbUpdated = movieRepository.update(id, movie, ownerLogin);
        if (!dbUpdated) {
            return new Response(ResponseStatus.ERROR,
                    "Не удалось обновить элемент в БД (возможно, он уже удалён).");
        }
        collectionManager.updateInMemory(id, movie);
        return new Response(ResponseStatus.OK, "Элемент с id=" + id + " успешно обновлён.");
    }

    private Response handleRemoveKey(String argument, String ownerLogin) throws SQLException {
        if (argument == null || argument.trim().isEmpty()) {
            return new Response(ResponseStatus.ERROR, "Укажите ключ для удаления.");
        }

        Long key = Long.parseLong(argument.trim());

        if (!collectionManager.containsKey(key)) {
            return new Response(ResponseStatus.ERROR, "Элемент с ключом " + key + " не найден.");
        }

        String actualOwner = collectionManager.getOwnerByKey(key);
        if (!ownerLogin.equals(actualOwner)) {
            return new Response(ResponseStatus.ERROR,
                    "Нет прав: элемент с ключом " + key + " принадлежит другому пользователю.");
        }

        boolean dbDeleted = movieRepository.deleteByKey(key, ownerLogin);
        collectionManager.removeKeyFromMemory(key);

        return new Response(ResponseStatus.OK,
                dbDeleted ? "Элемент успешно удалён." : "Элемент удалён из памяти (не найден в БД).");
    }

    private Response handleClear(String ownerLogin) throws SQLException {
        int deleted = movieRepository.deleteAllByOwner(ownerLogin);
        collectionManager.clearOwnerFromMemory(ownerLogin);
        return new Response(ResponseStatus.OK,
                "Удалено " + deleted + " ваших элементов из коллекции.");
    }

    private Response handleRemoveLower(Movie compareMovie, String ownerLogin) throws SQLException {
        if (compareMovie == null) {
            return new Response(ResponseStatus.ERROR, "Объект для сравнения не передан.");
        }
        if (!compareMovie.validate()) {
            return new Response(ResponseStatus.ERROR, "Невалидные данные фильма для сравнения.");
        }

        List<Long> keysToRemove = collectionManager.getKeysLowerThan(compareMovie, ownerLogin);

        if (keysToRemove.isEmpty()) {
            return new Response(ResponseStatus.OK, "Нет ваших элементов, меньших заданного.");
        }

        List<Long> actuallyRemoved = new ArrayList<>();
        for (Long key : keysToRemove) {
            if (movieRepository.deleteByKey(key, ownerLogin)) {
                actuallyRemoved.add(key);
            }
        }
        collectionManager.removeLowerFromMemory(actuallyRemoved);

        return new Response(ResponseStatus.OK,
                "Удалено " + actuallyRemoved.size() + " ваших элементов, меньших заданного.");
    }

    private Response handleReplaceIfGreater(String argument, Movie newMovie, String ownerLogin) throws SQLException {
        if (argument == null || argument.trim().isEmpty()) {
            return new Response(ResponseStatus.ERROR, "Укажите ключ.");
        }
        if (newMovie == null) {
            return new Response(ResponseStatus.ERROR, "Объект фильма не передан.");
        }
        if (!newMovie.validate()) {
            return new Response(ResponseStatus.ERROR, "Невалидные данные фильма.");
        }

        Long key = Long.parseLong(argument.trim());

        Movie oldMovie = collectionManager.getMovieByKey(key);
        if (oldMovie == null) {
            return new Response(ResponseStatus.ERROR, "Элемент с ключом " + key + " не найден.");
        }

        String actualOwner = collectionManager.getOwnerByKey(key);
        if (!ownerLogin.equals(actualOwner)) {
            return new Response(ResponseStatus.ERROR,
                    "Нет прав: элемент с ключом " + key + " принадлежит другому пользователю.");
        }

        if (newMovie.compareTo(oldMovie) <= 0) {
            return new Response(ResponseStatus.OK, "Элемент не заменён (новый не больше старого).");
        }

        boolean dbUpdated = movieRepository.replaceByKey(key, newMovie, ownerLogin);
        if (!dbUpdated) {
            return new Response(ResponseStatus.ERROR, "Не удалось обновить элемент в БД.");
        }
        newMovie.setId(oldMovie.getId());
        newMovie.setCreationDate(oldMovie.getCreationDate());
        collectionManager.forceReplaceInMemory(key, newMovie);
        return new Response(ResponseStatus.OK, "Элемент заменён (новый больше старого).");
    }

    private Response handleReplaceIfLowe(String argument, Movie newMovie, String ownerLogin) throws SQLException {
        if (argument == null || argument.trim().isEmpty()) {
            return new Response(ResponseStatus.ERROR, "Укажите ключ.");
        }
        if (newMovie == null) {
            return new Response(ResponseStatus.ERROR, "Объект фильма не передан.");
        }
        if (!newMovie.validate()) {
            return new Response(ResponseStatus.ERROR, "Невалидные данные фильма.");
        }

        Long key = Long.parseLong(argument.trim());

        Movie oldMovie = collectionManager.getMovieByKey(key);
        if (oldMovie == null) {
            return new Response(ResponseStatus.ERROR, "Элемент с ключом " + key + " не найден.");
        }

        String actualOwner = collectionManager.getOwnerByKey(key);
        if (!ownerLogin.equals(actualOwner)) {
            return new Response(ResponseStatus.ERROR,
                    "Нет прав: элемент с ключом " + key + " принадлежит другому пользователю.");
        }

        if (newMovie.compareTo(oldMovie) >= 0) {
            return new Response(ResponseStatus.OK, "Элемент не заменён (новый не меньше старого).");
        }

        boolean dbUpdated = movieRepository.replaceByKey(key, newMovie, ownerLogin);
        if (!dbUpdated) {
            return new Response(ResponseStatus.ERROR, "Не удалось обновить элемент в БД.");
        }
        newMovie.setId(oldMovie.getId());
        newMovie.setCreationDate(oldMovie.getCreationDate());
        collectionManager.forceReplaceInMemory(key, newMovie);
        return new Response(ResponseStatus.OK, "Элемент заменён (новый меньше старого).");
    }

    private Response handleAverageOscars() {
        double avg = collectionManager.getAverageOscarsCount();
        return new Response(ResponseStatus.OK, "Среднее количество Оскаров: " + avg);
    }

    private Response handleCountLessThanLength(String argument) {
        if (argument == null || argument.trim().isEmpty()) {
            return new Response(ResponseStatus.ERROR, "Укажите значение длины.");
        }
        int length = Integer.parseInt(argument.trim());
        long count = collectionManager.countLessThanLength(length);
        return new Response(ResponseStatus.OK,
                "Количество фильмов с продолжительностью меньше " + length + ": " + count);
    }

    private Response handlePrintFieldDescendingLength() {
        List<Integer> lengths = collectionManager.getFieldDescendingLength();
        return new Response(ResponseStatus.OK,
                "Значения поля length в порядке убывания:",
                new ArrayList<>(lengths));
    }
}