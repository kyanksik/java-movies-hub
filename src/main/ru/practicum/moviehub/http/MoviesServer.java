package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    public MoviesStore moviesStore;

    public MoviesServer(MoviesStore moviesStore, int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            this.moviesStore = moviesStore;
            server.createContext("/movies", new MoviesHandler(moviesStore));

        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }

    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

}