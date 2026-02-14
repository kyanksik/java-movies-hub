package ru.practicum.moviehub.http;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.api.JsonUtils;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private MoviesServer server;
    private HttpClient client;

    private final String baseUrl = "http://localhost:8080/movies";

    private HttpRequest reqGET(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
    }

    private HttpRequest reqPOST(String url, String json) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    private HttpRequest reqPostText(String url, String json) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    private HttpRequest reqDELETE(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();
    }

    HttpResponse<String> responseTest(HttpRequest req) throws Exception {
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void serverStart() throws IOException {

        // Инициализируем сервер с пустым хранилищем перед каждым тестом
        server = new MoviesServer(new MoviesStore(), 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

    }

    @AfterEach
    void serverStop() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        // System.out.println(body);
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_returnsArrayMovies() throws Exception {

        Movie title1 = server.getMoviesStore().create(new Movie("Title1", 1889));
        Movie title2 = server.getMoviesStore().create(new Movie("Title2", 1890));

        HttpResponse<String> resp = responseTest(reqGET(baseUrl));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
        assertTrue(body.contains(JsonUtils.toJson(title1)), "JSON должен содержать {\"id\":1,\"title\":\"Title1\",\"year\":1889}");
        assertTrue(body.contains(JsonUtils.toJson(title2)), "JSON должен содержать {\"id\":2,\"title\":\"Title2\",\"year\":1890}");
    }

    @Test
    public void shouldReturnMovieById() throws Exception {

        server.getMoviesStore().create(new Movie("Title1", 1889));

        HttpResponse<String> response = responseTest(reqGET(baseUrl + "/1"));

        assertEquals(200, response.statusCode());
        Movie returnedMovie = JsonUtils.fromJson(response.body(), Movie.class);
        assertEquals("Title1", returnedMovie.getTitle());
    }

    @Test
    public void shouldReturn404IfMovieNotFound() throws Exception {
        // Запрашиваем ID, которого точно нет - список пуст
        HttpResponse<String> response = responseTest(reqGET(baseUrl + "/9999"));

        assertEquals(404, response.statusCode());

        // Проверяем структуру ошибки через ErrorResponse
        ErrorResponse error = JsonUtils.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Фильм не найден", error.getDetails().get(0));
    }

    @Test
    public void shouldReturn400ForInvalidIdFormat() throws Exception {
        // Вместо числа передаем строку
        HttpResponse<String> response = responseTest(reqGET(baseUrl + "/abc"));

        assertEquals(400, response.statusCode());

        ErrorResponse error = JsonUtils.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Некорректный ID", error.getDetails().get(0));
    }

    @Test
    public void shouldReturnMoviesByYear() throws Exception {

        Movie title1 = server.getMoviesStore().create(new Movie("Title1", 1889));
        Movie title2 = server.getMoviesStore().create(new Movie("Title2", 1890));
        Movie title3 = server.getMoviesStore().create(new Movie("Title3", 1890));
        Movie title4 = server.getMoviesStore().create(new Movie("Title4", 1891));

        HttpResponse<String> response = responseTest(reqGET(baseUrl + "?year=1890"));

        String body = response.body().trim();
        List<Movie> movies = JsonUtils.fromJsonList(body, new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size());

        assertEquals(200, response.statusCode());

        assertTrue(body.contains(JsonUtils.toJson(title2)), "JSON должен содержать {\"id\":2,\"title\":\"Title2\",\"year\":1890}");
        assertTrue(body.contains(JsonUtils.toJson(title3)), "JSON должен содержать {\"id\":3,\"title\":\"Title2\",\"year\":1890}");
        assertFalse(body.contains(JsonUtils.toJson(title1)), "JSON не должен содержать {\"id\":1,\"title\":\"Title2\",\"year\":1889}");
        assertFalse(body.contains(JsonUtils.toJson(title4)), "JSON не должен содержать {\"id\":4,\"title\":\"Title2\",\"year\":1891}");
    }

    @Test
    public void shouldReturnEmptyArrayMoviesByYear() throws Exception {

        HttpResponse<String> response = responseTest(reqGET(baseUrl + "?year=1890"));
        String body = response.body().trim();

        assertEquals(200, response.statusCode());
        assertTrue(body.equals("[]"), "ожидается пустой массив");

    }

    @Test
    public void shouldReturn400ForInvalidYear() throws Exception {
        HttpResponse<String> response = responseTest(reqGET(baseUrl + "?year=not-a-year"));

        assertEquals(400, response.statusCode());
        ErrorResponse error = JsonUtils.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Некорректный параметр запроса — 'year'", error.getDetails().get(0));
    }

    @Test
    public void shouldCreateMovieAndReturn201() throws Exception {
        Movie title1 = new Movie("Title1", 1889);
        String json = JsonUtils.toJson(title1);

        HttpResponse<String> response = responseTest(reqPOST(baseUrl, json));

        assertEquals(201, response.statusCode());
        Movie createdMovie = JsonUtils.fromJson(response.body(), Movie.class);
        assertNotNull(createdMovie.getId()); // Проверяем, что ID присвоен
        assertEquals("Title1", createdMovie.getTitle());
        assertEquals(1889, createdMovie.getYear());
    }

    @Test
    public void shouldReturn422WhenTitleIsEmpty() throws Exception {
        String json = "{\"title\":\"\",\"year\":1889}";

        HttpResponse<String> response = responseTest(reqPOST(baseUrl, json));

        assertEquals(422, response.statusCode());
        ErrorResponse error = JsonUtils.fromJson(response.body(), ErrorResponse.class);
        assertTrue(error.getDetails().contains("название не должно быть пустым"));
    }

    @Test
    public void shouldReturn422WhenYearIsInvalid() throws Exception {

        String errString = "год должен быть между " + Movie.getMinYear() + " и " + Movie.getMaxYear();
        String json = "{\"title\":\"Title1\",\"year\":1800}";

        HttpResponse<String> response = responseTest(reqPOST(baseUrl, json));

        assertEquals(422, response.statusCode());
        ErrorResponse error = JsonUtils.fromJson(response.body(), ErrorResponse.class);
        assertTrue(error.getDetails().contains(errString));
    }

    @Test
    public void shouldReturn415WhenWrongContentType() throws Exception {
        HttpResponse<String> response = responseTest(reqPostText(baseUrl, "some text"));

        assertEquals(415, response.statusCode());
    }

    @Test
    public void shouldReturn422WhenTitleTooLong() throws Exception {
        String longTitle = "A".repeat(101);
        String json = "{\"title\":\"" + longTitle + "\",\"year\":1900}";

        HttpResponse<String> response = responseTest(reqPOST(baseUrl, json));

        assertEquals(422, response.statusCode());
        ErrorResponse error = JsonUtils.fromJson(response.body(), ErrorResponse.class);
        assertTrue(error.getDetails().contains("название не должно превышать 100 символов"));
    }

    @Test
    public void shouldDeleteMovieAndReturn204() throws Exception {
        // 1. Сначала добавляем фильм, чтобы было что удалять
        server.getMoviesStore().create(new Movie("Title1", 1889));

        HttpResponse<String> response = responseTest(reqDELETE(baseUrl + "/1"));

        assertEquals(204, response.statusCode());
        assertTrue(response.body().isEmpty()); // Тело должно быть пустым
    }

    @Test
    public void shouldReturn404WhenDeletingNonExistentMovie() throws Exception {
        HttpResponse<String> response = responseTest(reqDELETE(baseUrl + "/999"));

        assertEquals(404, response.statusCode());
        ErrorResponse error = JsonUtils.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ресурс не найден", error.getError());
    }

    @Test
    public void shouldReturn400WhenIdIsNotANumber() throws Exception {
        // Передаем строку "abc" вместо числового ID
        HttpResponse<String> response = responseTest(reqDELETE(baseUrl + "/abc"));

        assertEquals(400, response.statusCode());

        // Проверяем содержимое ошибки
        ErrorResponse error = JsonUtils.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Некорректный запрос", error.getError());
        assertTrue(error.getDetails().contains("Некорректный формат id"));
    }
}