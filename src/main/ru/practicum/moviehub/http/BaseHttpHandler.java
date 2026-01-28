package ru.practicum.moviehub.http;

import com.google.gson.*;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {

    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected static final Charset UTF8 = StandardCharsets.UTF_8;

    protected void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", CT_JSON);
        exchange.sendResponseHeaders(statusCode, 0);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(UTF8));
        }
    }

    protected void sendNoContent(HttpExchange exchange, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", CT_JSON);
        exchange.sendResponseHeaders(statusCode, -1);
    }

    protected void sendError(
            HttpExchange exchange,
            int statusCode,
            ErrorResponse error) throws IOException {

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", CT_JSON);
        exchange.sendResponseHeaders(statusCode, 0);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(new Gson().toJson(error).getBytes(UTF8));
        }
    }

}