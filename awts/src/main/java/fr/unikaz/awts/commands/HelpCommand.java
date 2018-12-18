package fr.unikaz.awts.commands;

import android.util.Log;

import fr.unikaz.awts.CommandExecutor;
import fr.unikaz.awts.CommandResult;
import fr.unikaz.awts.MyWebSocket;
import fr.unikaz.awts.WebServer;

public class HelpCommand implements CommandExecutor {

    WebServer webServer;

    public HelpCommand(WebServer webServer) {
        this.webServer = webServer;
    }

    @Override
    public CommandResult onCommand(MyWebSocket webSocket, String arg) {
        try {
            Log.i("cmd", "help command");
            StringBuffer stringBuffer = new StringBuffer();
            webServer.getCommandExecutors().forEach((k, v) -> {
                stringBuffer.append(k).append("  ").append(v.getDescription()).append("\n");
            });
            webSocket.send(stringBuffer.toString());
            return CommandResult.OK;
        } catch (Exception e) {
            return CommandResult.ERROR;
        }
    }

    @Override
    public String getDescription() {
        return "Display available commands";
    }
}
