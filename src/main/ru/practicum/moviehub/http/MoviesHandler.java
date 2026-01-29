package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MoviesHandler extends BaseHttpHandler {
    private MoviesStore moviesStore;
    private final int OK_200 = 200;
    private final int CREATED_201 = 201;
    private final int NO_CONTENT_204 = 204;
    private final int BAD_REQUEST_400 = 400;
    private final int NOT_FOUND_404 = 404;
    private final int UNSUPPORTED_MEDIA_TYPE_415 = 415;
    private final int UNPROCESSABLE_ENTITY_422 = 422;

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
                        sendError(exchange, BAD_REQUEST_400, new ErrorResponse("Некорректный ID"));
                    }
                } else if (query == null) {
                    if (moviesStore.getSize() == 0) {
                        sendJson(exchange, OK_200, "[]");
                    } else {
                        sendJson(exchange,
                                OK_200,
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
                        sendError(exchange, BAD_REQUEST_400, new ErrorResponse("Некорректный параметр запроса — 'year'"));
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
                        sendError(exchange, BAD_REQUEST_400, new ErrorResponse("Некорректный ID"));
                    }
                } else {
                    sendNoContent(exchange, 405);
                }
                return;
            default:
                sendNoContent(exchange, 405);
        }
    }

    protected void deleteMovieByID(HttpExchange exchange, int id) throws IOException {
        if (moviesStore.deleteMovieByID(id)) {
            sendNoContent(exchange, NO_CONTENT_204);
        } else {
            sendNoContent(exchange, NOT_FOUND_404);
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
            int statusCode = CREATED_201;
            List<String> details = new ArrayList<>();
            if (title.isEmpty() || title.length() >= 100) {
                statusCode = UNPROCESSABLE_ENTITY_422;
                details.add("название не должно быть пустым или превышать 100 символов");
            }
            int nowYear = LocalDate.now().getYear();
            if (!isCorrectYear(year)) {
                statusCode = UNPROCESSABLE_ENTITY_422;
                details.add("год должен быть между 1888 и " + nowYear);
            }
            if (statusCode == UNPROCESSABLE_ENTITY_422) {
                sendError(exchange, statusCode, new ErrorResponse("Ошибка валидации", details));
                return;
            }
            Movie movie = new Movie(title, year);
            int id = moviesStore.addMovie(movie);
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("id", id);
            jsonMap.put("movie", movie);
            sendJson(exchange, CREATED_201, new Gson().toJson(jsonMap));
        } else {
            sendError(exchange, UNSUPPORTED_MEDIA_TYPE_415, new ErrorResponse());
        }
    }

    protected void findMovieByID(HttpExchange exchange, int id) throws IOException {
        Movie movie = moviesStore.getMovieByID(id);
        if (movie == null) {
            sendError(exchange, NOT_FOUND_404, new ErrorResponse("Фильм не найден"));
        } else {
            sendJson(exchange, OK_200, new Gson().toJson(movie));
        }
    }

    protected void filterMovieByYear(HttpExchange exchange, int year) throws IOException {
        if (isCorrectYear(year)) {
            List<Movie> movieList = moviesStore.getMoviesByYear(year);
            sendJson(exchange, OK_200, new Gson().toJson(movieList));
        } else {
            sendError(exchange, BAD_REQUEST_400, new ErrorResponse("Некорректный параметр запроса — 'year'"));
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
