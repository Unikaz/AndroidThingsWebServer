package fr.unikaz.awts;

import android.util.Log;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

public class MyWebSocket extends NanoWSD.WebSocket {

    private WebServer webServer;
    private NanoHTTPD.IHTTPSession httpSession;
    public Client client;
    private CommandExecutor waitingCommandExecutor;


    public MyWebSocket(NanoHTTPD.IHTTPSession handshakeRequest, WebServer webServer) {
        super(handshakeRequest);
        this.webServer = webServer;
        this.httpSession = handshakeRequest;
    }

    @Override
    protected void onOpen() {
        client = Client.getClient(httpSession);
        webServer.getConnections().put(client, this);
    }

    @Override
    protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        webServer.getConnections().remove(client);
    }

    @Override
    protected void onMessage(NanoWSD.WebSocketFrame message) {
        if (message.getTextPayload().startsWith("cmd:")) {
            final String cmd = message.getTextPayload().substring(4);
            CommandResult cr;
            if (waitingCommandExecutor != null) {
                cr = waitingCommandExecutor.onCommand(this, cmd);
                if (cr != CommandResult.WAITING)
                    waitingCommandExecutor = null;
            } else {
                String[] cmds = cmd.trim().split("\\s+", 2);
                if (cmds.length >= 2)
                    cr = webServer.executeCommand(this, cmds[0], cmds[1]);
                else
                    cr = webServer.executeCommand(this, cmds[0], "");
                if (cr == CommandResult.WAITING)
                    waitingCommandExecutor = webServer.getCommandExecutors().get(cmds[0]);
            }
        } else {
            Log.i("onMessage", "Client " + client.getName() + " send : " + message);
        }
    }

    @Override
    protected void onPong(NanoWSD.WebSocketFrame pong) {
    }

    @Override
    protected void onException(IOException exception) {
        Log.i("MyWebSocket", exception.getMessage());
    }
}
