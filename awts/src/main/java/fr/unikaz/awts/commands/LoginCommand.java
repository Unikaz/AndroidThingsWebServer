package fr.unikaz.awts.commands;


import fr.unikaz.awts.CommandExecutor;
import fr.unikaz.awts.CommandResult;
import fr.unikaz.awts.MyWebSocket;

public class LoginCommand implements CommandExecutor {
    @Override
    public CommandResult onCommand(MyWebSocket webSocket, String arg) {
        try {
            if (arg.equals("")) {
                webSocket.send("You need to specify a name");
            } else {
                webSocket.client.setName(arg);
                webSocket.send("Welcome " + arg);
                webSocket.send("cmd:setcookie:token=" + webSocket.client.getToken() + "; expires=31 Dec 2037 01:00:00 UTC; path=/");
            }
            return CommandResult.OK;
        } catch (Exception e) {
            return CommandResult.ERROR;
        }
    }

    @Override
    public String getDescription() {
        return "<name> : to log under this name";
    }
}
