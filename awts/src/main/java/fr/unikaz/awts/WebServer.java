package fr.unikaz.awts;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import fr.unikaz.awts.commands.HelpCommand;
import fr.unikaz.awts.commands.LoginCommand;

public class WebServer extends NanoWSD {

    private final Context context;
    private ArrayList<WebServerListener> listeners = new ArrayList<>();
    private HashMap<Client, MyWebSocket> connections = new HashMap<>();
    private HashMap<String, CommandExecutor> commandExecutors = new HashMap<>();

    public WebServer(Context context, int port) throws IOException {
        super(port);
        this.context = context;
        // start server
        start();
        startWebSocketKeepAliveThread();

        // create defaults commands
        registerCommand("help", new HelpCommand(this));
        registerCommand("login", new LoginCommand());
    }

    private void startWebSocketKeepAliveThread() {
        // keep-alive websocket by ping
        final byte[] pingBytes = new byte[]{};
        Log.i("PING", "launch ping thread");
        new Thread(() -> {
            while (true) {
                connections.forEach((client, connection) -> {
                    try {
                        if (connection.isOpen())
                            connection.ping(pingBytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected MyWebSocket openWebSocket(IHTTPSession handshake) {
        Log.i("WebServer", "openWebSocket");
        MyWebSocket mws = new MyWebSocket(handshake, this);
        return mws;
    }

    @Override
    public Response serveHttp(IHTTPSession session) {
        Log.i("WebServer", "serve: " + session.getUri());

        // check cookies
        Client client = Client.getClient(session);


        String uri = session.getUri();
        if (uri.equals("/"))
            uri = "/index.html";
        if (!uri.contains("."))
            uri += ".html";

        // parse post values
        HashMap<String, String> postParams = new HashMap<>();
        if (session.getMethod() == NanoHTTPD.Method.POST || session.getMethod() == Method.PUT) {
            try {
                session.parseBody(postParams);
            } catch (IOException | NanoHTTPD.ResponseException e) {
                e.printStackTrace();
            }
        }

        Response r = null;
        try {
            InputStream is = context.getResources().getAssets().open("www" + uri);
            r = newChunkedResponse(Response.Status.OK, null, is);
        } catch (Exception e) {
            try {
                Log.i("server", "404 for " + uri);
                InputStream is = context.getResources().getAssets().open("www/404.html");
                r = newChunkedResponse(Response.Status.NOT_FOUND, null, is);
            } catch (IOException e1) {
                r = newFixedLengthResponse("Error 404: Page Not Found");
            }
        }
        // notify listeners
        listeners.forEach(l -> l.onServe(session));

        // add JWT token
        CookieHandler ch = new CookieHandler(session.getHeaders());
        ch.set(client.getCookie());
        ch.unloadQueue(r);

        return r;
    }


    public void broadcast(String message) {
        connections.forEach((client, socket) -> {
            try {
                if (socket.isOpen()) {
                    socket.send(message);
                }
            } catch (IOException e) {
            }
        });
    }


    public HashMap<Client, MyWebSocket> getConnections() {
        return this.connections;
    }

    public void addListener(WebServerListener listener) {
        listeners.add(listener);
    }

    public ArrayList<WebServerListener> getListeners() {
        return listeners;
    }

    public void registerCommand(String commandName, CommandExecutor executor) {
        commandExecutors.put(commandName, executor);
    }

    public CommandResult executeCommand(MyWebSocket webSocket, String cmd, String args) {
        if (commandExecutors.containsKey(cmd))
            return commandExecutors.get(cmd).onCommand(webSocket, args);
        return CommandResult.ERROR;
    }

    public HashMap<String, CommandExecutor> getCommandExecutors() {
        return commandExecutors;
    }

    public interface WebServerListener {
        void onServe(IHTTPSession session);
    }



}
