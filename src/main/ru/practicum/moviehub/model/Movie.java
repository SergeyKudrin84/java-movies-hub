package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {
    private final String title;
    private final int year;

    public Movie(String title, int year) {
        this.title = title;
        this.year = year;
    }

    public int getYear() {
        return year;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return year == movie.year && Objects.equals(title, movie.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, year);
    }
}