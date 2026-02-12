package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore {
    private final Map<Integer, Movie> storage = new HashMap<>();

    // Создать новый фильм (ID генерируется автоматически)
    public Movie create(Movie movie) {
        if (movie == null) {
            throw new IllegalArgumentException("Фильм не может быть пустым");
        }
        int newId = generateId();
        movie.setId(newId);
        storage.put(newId, movie);
        return movie;
    }

    // Найти фильм по ID
    public Optional<Movie> findById(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    // Удалить фильм
    public boolean delete(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID не может быть пустым");
        }
        Movie removedMovie = storage.remove(id);
        if (removedMovie != null) {
            return true;
        }
        return false;
    }

    // Получить все фильмы
    public Collection<Movie> getAll() {
        return Collections.unmodifiableCollection(storage.values());
    }

    // Вспомогательный метод: генерация уникального ID
    private Integer generateId() {
        return storage.keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

}