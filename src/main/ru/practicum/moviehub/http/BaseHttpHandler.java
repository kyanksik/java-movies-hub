package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public abstract class BaseHttpHandler implements HttpHandler {

    private int statusCode = 200;

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Базовая логика обработки запроса
        try {
            // Получаем метод запроса
            String requestMethod = exchange.getRequestMethod();

            // Определяем ответ в зависимости от метода
            String response;
            switch (requestMethod) {
                case "GET" -> response = handleGet(exchange);
                case "POST" -> response = handlePost(exchange);
                case "DELETE" -> response = handleDelete(exchange);
                case null, default -> {
                    // Метод не поддерживается
                    exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed

                    return;
                }
            }

            // Отправляем ответ
            sendResponse(exchange, response, statusCode);
        } catch (Exception e) {
            sendResponse(exchange, "Internal Server Error", 500);
        }
    }

    // Абстрактные методы для реализации в наследниках
    protected abstract String handleGet(HttpExchange exchange) throws IOException;

    protected abstract String handlePost(HttpExchange exchange) throws IOException;

    protected abstract String handleDelete(HttpExchange exchange) throws IOException;

    // Вспомогательный метод для отправки ответа
    private void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}