package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {

    private int id;
    private final String title;
    private final Integer year;

    private static final int MIN_YEAR = 1888;
    private static final int MAX_YEAR = java.time.Year.now().getValue() + 1;

    public Movie(String title, Integer year) {
        this.title = validateTitle(title);
        this.year = validateYear(year);
    }

    // Геттеры и сеттеры
    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public Integer getYear() {
        return year;
    }

    public static int getMinYear() {
        return MIN_YEAR;
    }

    public static int getMaxYear() {
        return MAX_YEAR;
    }

    // Валидация заголовка
    private String validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("название не должно быть пустым");
        }
        return title.trim();
    }

    // Валидация года
    private Integer validateYear(Integer year) {
        if (year == null) {
            throw new IllegalArgumentException("год не должен быть пустым");
        }
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException("год должен быть между " + MIN_YEAR + " и " + MAX_YEAR);
        }
        return year;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return Objects.equals(id, movie.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Movie{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", year=" + year +
                '}';
    }
}