package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesServer {

    private final HttpServer server;

    public MoviesServer(MoviesStore moviesStore, int port) {

        moviesStore.addMovie(new Movie("first", 1990));
        moviesStore.addMovie(new Movie("second", 1991));
        moviesStore.addMovie(new Movie("third", 1992));
        moviesStore.addMovie(new Movie("fifth", 1991));

        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/movies", new MoviesHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

}

class MoviesHandler extends BaseHttpHandler {
    private MoviesStore moviesStore;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String[] pathURI = exchange.getRequestURI().getPath().split("/");
        String query = exchange.getRequestURI().getQuery();
        switch (method) {
            case "GET":
                if (pathURI.length == 3) {
                    Integer id;
                    try {
                        id = Integer.parseInt(pathURI[2]);
                        findMovieByID(exchange, id);
                    } catch (IllegalArgumentException e) {
                        sendError(exchange, 400, new ErrorResponse("Некорректный ID"));
                    }
                } else if (query == null) {
                    if (moviesStore.getSize() == 0) {
                        sendNoContent(exchange, 204);
                    } else {
                        sendJson(exchange,
                                200,
                                new GsonBuilder().setPrettyPrinting().create().toJson(moviesStore.getListOfValue())
                        );
                    }
                } else {
                    Map<String, String> queryParams = splitQuery(query);
                    String yearParam = queryParams.get("year");
                    if (yearParam == null) {
                        sendNoContent(exchange, 405);
                        return;
                    }
                    Integer year;
                    try {
                        year = Integer.parseInt(yearParam);
                        filterMovieByYear(exchange, year);
                    } catch (IllegalArgumentException e) {
                        sendError(exchange, 400, new ErrorResponse("Некорректный параметр запроса — 'year'"));
                    }
                }
                return;
            case "POST":
                addMovie(exchange);
                return;
            case "DELETE":
                if (pathURI.length == 3) {
                    Integer id;
                    try {
                        id = Integer.parseInt(pathURI[2]);
                        deleteMovieByID(exchange, id);
                    } catch (IllegalArgumentException e) {
                        sendError(exchange, 400, new ErrorResponse("Некорректный ID"));
                    }
                } else {
                    sendNoContent(exchange, 405);
                }
                return;
            default:
                sendNoContent(exchange, 405);
                //return;
        }
        //sendNoContent(exchange, 405);
    }

    protected void deleteMovieByID(HttpExchange exchange, int id) throws IOException {
        if (moviesStore.deleteMovieByID(id)) {
            sendNoContent(exchange, 204);
        } else {
            sendNoContent(exchange, 404);
        }
    }

    protected void addMovie(HttpExchange exchange) throws IOException {
        if (exchange.getRequestHeaders().containsKey("Content-Type")
                && exchange.getRequestHeaders().getFirst("Content-Type").equals(CT_JSON)) {
            InputStream inputStream = exchange.getRequestBody();
            JsonObject json = JsonParser.parseString(
                    new String(inputStream.readAllBytes(), UTF8)).getAsJsonObject();
            String title = json.get("title").getAsString();
            Integer year = json.get("year").getAsInt();
            int statusCode = 201;
            List<String> details = new ArrayList<>();
            if (title.isEmpty() || title.length() >= 100) {
                statusCode = 422;
                details.add("название не должно быть пустым или превышать 100 символов");
            }
            int nowYear = LocalDate.now().getYear();
            if (!isCorrectYear(year)) {
                statusCode = 422;
                details.add("год должен быть между 1888 и " + nowYear);
            }
            if (statusCode == 422) {
                sendError(exchange, statusCode, new ErrorResponse("Ошибка валидации", details));
                return;
            }
            Movie movie = new Movie(title, year);
            int id = moviesStore.addMovie(movie);
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("id", id);
            jsonMap.put("movie", movie);
            sendJson(exchange, 201, new Gson().toJson(jsonMap));
        } else {
            sendError(exchange, 415, new ErrorResponse());
        }
    }

    protected void findMovieByID(HttpExchange exchange, int id) throws IOException {
        Movie movie = moviesStore.getMovieByID(id);
        if (movie == null) {
            sendError(exchange, 404, new ErrorResponse("Фильм не найден"));
        } else {
            sendJson(exchange, 200, new Gson().toJson(movie));
        }
    }

    protected void filterMovieByYear(HttpExchange exchange, int year) throws IOException {
        if (isCorrectYear(year)) {
            List<Movie> movieList = moviesStore.getMoviesByYear(year);
            sendJson(exchange, 200, new Gson().toJson(movieList));
        } else {
            sendError(exchange, 400, new ErrorResponse("Некорректный параметр запроса — 'year'"));
        }

    }

    protected static boolean isCorrectYear(int year) {
        int nowYear = LocalDate.now().getYear();
        return year >= 1888 && year <= nowYear;

    }

    public static Map<String, String> splitQuery(String query) {
        Map<String, String> result = new HashMap<>();

        if (query.isBlank()) {
            return result;
        }

        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            String key = URLDecoder.decode(pair[0], UTF8);
            String value = pair.length > 1
                    ? URLDecoder.decode(pair[1], UTF8)
                    : "";

            result.put(key, value);
        }
        return result;
    }
}