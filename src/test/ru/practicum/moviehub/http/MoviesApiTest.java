package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store = new MoviesStore();
    private static Charset charset = StandardCharsets.UTF_8;
    private static String valueHeader = "application/json; charset=UTF-8";

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(store, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        store.clearStore();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returns200() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies должен вернуть 200"
        );

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку"
        );

        String body = resp.body().trim();
        assertTrue(
                body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив"
        );
    }

    @Test
    void getMovies_whenFill_returns200AndArraySize3() throws Exception {
        fillStore();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies должен вернуть 200"
        );

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(
                body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив"
        );

        List<Movie> movieListList =
                new GsonBuilder().create().fromJson(body, new ListOfMoviesTypeToken().getType());
        assertEquals(
                3,
                movieListList.size(),
                "Неверная длина JSON-массива"
        );
    }

    @Test
    void postMovie_whenCorrectRequest_returns201AndCorrectJson() throws Exception {

        Movie movieO = new Movie("NewMovie", 2026);
        String movie = new Gson().toJson(movieO);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", valueHeader)
                .POST(HttpRequest.BodyPublishers.ofString(movie, charset))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                201,
                resp.statusCode(),
                "GET /movies должен вернуть 201"
        );

        String body = resp.body().trim();

        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        assertEquals(
                1,
                json.get("id").getAsInt(),
                "Неверный id фильма"
        );

        assertTrue(
                movieO.equals(new Gson().fromJson(json.get("movie").getAsJsonObject(), Movie.class)),
                "Фильмы не равны"
        );
    }

    @Test
    void postMovie_whenIncorrectHeader_returns415() throws Exception {

        String movie = new Gson().toJson(new Movie("NewMovie", 1888));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(movie, charset))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                415,
                resp.statusCode(),
                "GET /movies должен вернуть 415"
        );
    }

    @Test
    void postMovie_whenEmptyTitle_returns422() throws Exception {

        String movie = new Gson().toJson(new Movie("", 2025));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", valueHeader)
                .POST(HttpRequest.BodyPublishers.ofString(movie, charset))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                422,
                resp.statusCode(),
                "GET /movies должен вернуть 422"
        );
    }

    @Test
    void postMovie_whenTitleMoreThan100_returns422() throws Exception {

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            stringBuilder.append("a");
        }
        String movie = new Gson().toJson(new Movie(stringBuilder.toString(), 2025));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", valueHeader)
                .POST(HttpRequest.BodyPublishers.ofString(movie, charset))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                422,
                resp.statusCode(),
                "GET /movies должен вернуть 422"
        );
    }

    @Test
    void postMovie_whenYear1887_returns422() throws Exception {

        String movie = new Gson().toJson(new Movie("Movie", 1887));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", valueHeader)
                .POST(HttpRequest.BodyPublishers.ofString(movie, charset))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                422,
                resp.statusCode(),
                "GET /movies должен вернуть 422"
        );
    }

    @Test
    void postMovie_whenYear2027_returns422() throws Exception {

        String movie = new Gson().toJson(new Movie("Movie", 2027));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", valueHeader)
                .POST(HttpRequest.BodyPublishers.ofString(movie, charset))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                422,
                resp.statusCode(),
                "GET /movies должен вернуть 422"
        );
    }

    @Test
    void getMovies_whenIdIntAndFound_returns200() throws Exception {
        fillStore();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies должен вернуть 200"
        );

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку"
        );

        String body = resp.body().trim();

        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(
                new Movie("first", 1990)
                        .equals(new Gson().fromJson(json, Movie.class)),
                "Фильмы не равны"
        );
    }

    @Test
    void getMovies_whenIdIntAndNotFound_returns404() throws Exception {
        fillStore();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/12"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                404,
                resp.statusCode(),
                "GET /movies должен вернуть 404"
        );

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку"
        );
    }

    @Test
    void getMovies_whenIdNotInt_returns400() throws Exception {
        fillStore();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/a"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                400,
                resp.statusCode(),
                "GET /movies должен вернуть 400"
        );

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку"
        );
    }

    @Test
    void deleteMovies_whenIdNotInt_returns400() throws Exception {
        fillStore();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/a"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                400,
                resp.statusCode(),
                "GET /movies должен вернуть 400"
        );

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку"
        );
    }

    @Test
    void deleteMovies_whenIdIntAndNotFound_returns404() throws Exception {
        fillStore();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/22"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                404,
                resp.statusCode(),
                "GET /movies должен вернуть 404"
        );

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку"
        );
    }

    @Test
    void deleteMovies_whenIdIntAndDeleted_returns204() throws Exception {
        fillStore();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                204,
                resp.statusCode(),
                "GET /movies должен вернуть 204"
        );

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку"
        );
    }

    @Test
    void getMoviesByYear_whenYearCorrectAndMoviesFound_returns200AndArraySize2() throws Exception {
        fillStore();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1991"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies должен вернуть 200"
        );

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку"
        );

        String body = resp.body().trim();
        assertTrue(
                body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив"
        );

        List<Movie> movieListList =
                new GsonBuilder().create().fromJson(body, new ListOfMoviesTypeToken().getType());
        assertEquals(
                2,
                movieListList.size(),
                "Неверная длина JSON-массива"
        );
    }

    @Test
    void getMoviesByYear_whenYearCorrectAndMoviesNotFound_returns200AndArraySize0() throws Exception {
        fillStore();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2000"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies должен вернуть 200"
        );

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку"
        );

        String body = resp.body().trim();
        assertTrue(
                body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив"
        );

        List<Movie> movieListList =
                new GsonBuilder().create().fromJson(body, new ListOfMoviesTypeToken().getType());
        assertEquals(
                0,
                movieListList.size(),
                "Неверная длина JSON-массива"
        );
    }

    @Test
    void getMoviesByYear_whenYearInCorrect_returns400() throws Exception {
        fillStore();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1887"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                400,
                resp.statusCode(),
                "GET /movies должен вернуть 400"
        );

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку"
        );

        String body = resp.body().trim();
        assertEquals(
                "Некорректный параметр запроса — 'year'",
                JsonParser.parseString(body).getAsJsonObject().get("error").getAsString(),
                "Неверный текст ошибки"
        );
    }

    @Test
    void getMoviesByYear_whenYearInCorrectNotNumber_returns400() throws Exception {
        fillStore();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=aaaa"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(charset));

        assertEquals(
                400,
                resp.statusCode(),
                "GET /movies должен вернуть 400"
        );

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку"
        );

        String body = resp.body().trim();
        assertEquals(
                "Некорректный параметр запроса — 'year'",
                JsonParser.parseString(body).getAsJsonObject().get("error").getAsString(),
                "Неверный текст ошибки"
        );
    }

    void fillStore() {
        store.addMovie(new Movie("first", 1990));
        store.addMovie(new Movie("second", 1991));
        store.addMovie(new Movie("third", 1991));
    }
}