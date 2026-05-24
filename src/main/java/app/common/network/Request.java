package app.common.network;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 2L;

    private final String commandName;
    private final String stringArgument;
    private final Serializable objectArgument;
    private final String login;
    private final String passwordHash;

    public Request(String commandName, String stringArgument, Serializable objectArgument,
                   String login, String passwordHash) {
        this.commandName = commandName;
        this.stringArgument = stringArgument;
        this.objectArgument = objectArgument;
        this.login = login;
        this.passwordHash = passwordHash;
    }

    public Request(String commandName, String stringArgument, Serializable objectArgument) {
        this(commandName, stringArgument, objectArgument, null, null);
    }

    public Request(String commandName, String stringArgument) {
        this(commandName, stringArgument, null, null, null);
    }

    public Request(String commandName) {
        this(commandName, "", null, null, null);
    }

    public String getCommandName() {
        return commandName;
    }

    public String getStringArgument() {
        return stringArgument;
    }

    public Serializable getObjectArgument() {
        return objectArgument;
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    @Override
    public String toString() {
        return "Request[" + commandName +
                (stringArgument != null && !stringArgument.isEmpty() ? ", " + stringArgument : "") +
                (objectArgument == null ? "" : ", " + objectArgument) +
                (login != null ? ", user=" + login : "") + "]";
    }
}