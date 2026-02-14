package ru.practicum.moviehub.api;

import java.util.List;

public class ErrorResponse {
    private final String error;           // Краткое описание ошибки
    private final List<String> details;   // Детализированные сообщения (список причин)

    public ErrorResponse(String error, List<String> details) {
        this.error = error;
        this.details = details;
    }

    public String getError() {
        return error;
    }

    public List<String> getDetails() {
        return details;
    }

    public static ErrorResponse of(String errorMessage, String... details) {
        List<String> detailList = List.of(details);
        return new ErrorResponse(errorMessage, detailList);
    }

    public static ErrorResponse validationError(List<String> validationErrors) {
        return new ErrorResponse("Ошибка валидации", validationErrors);
    }

    public static ErrorResponse unsupportedMediaType(String message) {
        return new ErrorResponse("Неподдерживаемый тип контента", List.of(message));
    }

    public static ErrorResponse badRequest(String message) {
        return new ErrorResponse("Некорректный запрос", List.of(message));
    }

    public static ErrorResponse notFound(String message) {
        return new ErrorResponse("Ресурс не найден", List.of(message));
    }

    public static ErrorResponse methodNotAllowed(String message) {
        return new ErrorResponse("Метод не поддерживается", List.of(message));
    }
}