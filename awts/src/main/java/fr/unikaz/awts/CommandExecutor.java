package fr.unikaz.awts;

public interface CommandExecutor {
    CommandResult onCommand(MyWebSocket webSocket, String arg);
    String getDescription();
}
