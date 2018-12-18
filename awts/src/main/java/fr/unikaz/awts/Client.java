package fr.unikaz.awts;

import android.util.Log;

import java.security.Key;
import java.util.HashMap;

import fi.iki.elonen.NanoHTTPD;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public class Client {


    private static final String JWT_COOKIE_KEY = "token";
    private static int idCounter = 0;
    private static final HashMap<Integer, Client> clients = new HashMap<>();

    //    private static final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final Key key = Keys.hmacShaKeyFor(BuildConfig.jwtKey.getBytes());
    private static final String JWT_ID = "id";
    private static final String JWT_NAME = "name";


    private final int id = idCounter++;
    private HashMap<String, Object> claims = new HashMap<>();
    private String name = "Anon";
    private NanoHTTPD.Cookie cookie;
    private String token;

    public Client() {
        clients.put(id, this);
        recreateJWT();
    }

    private void recreateJWT() {
        claims.clear();
        claims.put(JWT_ID, id);
        claims.put(JWT_NAME, name);
        token = Jwts.builder()
                .addClaims(claims)
                .signWith(key)
                .compact();
        cookie = new NanoHTTPD.Cookie(JWT_COOKIE_KEY, token);
    }

    public static Client getClient(NanoHTTPD.IHTTPSession session) {
        String jwtToken = session.getCookies().read(JWT_COOKIE_KEY);
        Client client = null;
        if (jwtToken != null) {
            try {
                Jws<Claims> claims = Jwts.parser().setSigningKey(key).parseClaimsJws(jwtToken);
                //OK, we can trust this JWT
                if (claims.getBody().containsKey(JWT_ID)) {
                    int id = (int) claims.getBody().get(JWT_ID);
                    if (clients.containsKey(id)) {
                        client = clients.get(id);
                    } else {
                        //todo search in DB
                        // for the moment, create a new client but use the name store in JWT
                        client = new Client();
                        if (claims.getBody().containsKey(JWT_NAME)) {
                            client.setName((String) claims.getBody().get(JWT_NAME));
                        }
                    }
                }
            } catch (JwtException e) {
                //don't trust the JWT!
                Log.i("Client.getClient()", "Something is wrong with this token");
                Log.i("Client.getClient()", e.getMessage());
            }
        }
        if (client == null)
            client = new Client();
        Log.i("client is", client.getName() + " on " + session.getHeaders().get("remote-addr"));
        return client;
    }

    public void setName(String name) {
        this.name = name;
        recreateJWT();
    }

    public String getName() {
        return name;
    }


    NanoHTTPD.Cookie getCookie() {
        return cookie;
    }

    public String getToken() {
        return token;
    }
}
