package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;

import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.api.JsonUtils;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    public MoviesStore moviesStore;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    protected String handleGet(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] pathParts = path.split("/");
        String query = exchange.getRequestURI().getQuery(); // Получаем строку после '?'

        // Устанавливаем заголовок JSON для всех ответов GET
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

        // 1. Обработка GET /movies/{id}
        if (pathParts.length == 3) {
            try {
                Integer id = Integer.parseInt(pathParts[2]);
                var movieOptional = moviesStore.findById(id);
                if (movieOptional.isPresent()) {
                    Movie movie = movieOptional.get();
                    statusCode = 200;
                    return JsonUtils.toJson(movie);
                } else {
                    statusCode = 404;
                    return JsonUtils.toJson(ErrorResponse.notFound("Фильм не найден"));
                }
            } catch (NumberFormatException e) {
                statusCode = 400;
                return JsonUtils.toJson(ErrorResponse.badRequest("Некорректный ID"));
            }
        }

// 2. Обработка фильтрации GET /movies?year=YYYY
        if (query != null && query.contains("year=")) {
            try {
                // Извлекаем значение года (упрощенно для одного параметра)
                String yearString = query.split("year=")[1].split("&")[0];
                int year = Integer.parseInt(yearString);

                // Фильтруем список (предполагается, что moviesStore.getAll() возвращает List<Movie>)
                var filteredMovies = moviesStore.getAll().stream()
                        .filter(m -> m.getYear() == year)
                        .toList();

                statusCode = 200;
                return JsonUtils.toJson(filteredMovies);
            } catch (Exception e) {
                statusCode = 400;
                return JsonUtils.toJson(ErrorResponse.badRequest("Некорректный параметр запроса — 'year'"));
            }
        }

        // 3. Обработка GET /movies (без параметров)
        statusCode = 200;
        return JsonUtils.toJson(moviesStore.getAll());
    }

    @Override
    protected String handlePost(HttpExchange exchange) throws IOException {
        // 1. Проверка Content-Type
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            statusCode = 415;
            return JsonUtils.toJson(ErrorResponse.unsupportedMediaType("Ожидается application/json"));
        }

        // 2. Чтение тела запроса
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Movie movieRequest;
        try {
            movieRequest = JsonUtils.fromJson(body, Movie.class);
        } catch (Exception e) {
            statusCode = 400;
            return JsonUtils.toJson(ErrorResponse.badRequest("Некорректный JSON"));
        }

        // 3. Валидация
        List<String> errors = new ArrayList<>();

        // Проверка title
        if (movieRequest.getTitle() == null || movieRequest.getTitle().isBlank()) {
            errors.add("название не должно быть пустым");
        } else if (movieRequest.getTitle().length() > 100) {
            errors.add("название не должно превышать 100 символов");
        }

        // Проверка year
        if (movieRequest.getYear() < Movie.MIN_YEAR || movieRequest.getYear() > Movie.MAX_YEAR) {
            errors.add("год должен быть между " + Movie.MIN_YEAR + " и " + Movie.MAX_YEAR);
        }

        // 4. Обработка результата валидации
        if (!errors.isEmpty()) {
            statusCode = 422; // Unprocessable Entity
            return JsonUtils.toJson(ErrorResponse.validationError(errors));
        }

        // 5. Сохранение и ответ
        Movie savedMovie = moviesStore.create(movieRequest); // Метод сохранения должен возвращать объект с ID
        statusCode = 201;
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        return JsonUtils.toJson(savedMovie);
    }

    @Override
    protected String handleDelete(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] pathParts = path.split("/");

        // Проверяем формат пути /movies/{id}
        if (pathParts.length == 3) {
            try {
                int id = Integer.parseInt(pathParts[2]);

                // Пытаемся удалить фильм. Предполагается, что метод delete возвращает boolean
                boolean isRemoved = moviesStore.delete(id);

                if (isRemoved) {
                    // Успешное удаление: статус 204 и пустое тело
                    statusCode = 204;
                    return "";
                } else {
                    // Фильм с таким ID не найден
                    statusCode = 404;
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    return JsonUtils.toJson(ErrorResponse.notFound("Фильм с id " + id + " не найден"));
                }
            } catch (NumberFormatException e) {
                statusCode = 400;
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                return JsonUtils.toJson(ErrorResponse.badRequest("Некорректный формат id"));
            }
        }

        // Если ID не указан в пути (например, просто DELETE /movies)
        statusCode = 400;
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        return JsonUtils.toJson(ErrorResponse.badRequest("Не указан идентификатор фильма"));
    }
}