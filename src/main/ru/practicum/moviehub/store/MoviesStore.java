package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {

    private final Map<Integer, Movie> store = new HashMap<>();

    public int addMovie(Movie movie) {
        int newId = store.size() + 1;
        store.put(newId, movie);
        return newId;
    }

    public void clearStore() {
        store.clear();
    }

    public int getSize() {
        return store.size();
    }

    public List<Movie> getListOfValue() {
        return new ArrayList<>(store.values());
    }

    public Movie getMovieByID(int id) {
        if (store.containsKey(id)) {
            return store.get(id);
        }
        return null;
    }

    public boolean deleteMovieByID(int id) {
        boolean isDeleted = false;
        if (store.containsKey(id)) {
            store.remove(id);
            isDeleted = true;
        }
        return isDeleted;
    }

    public List<Movie> getMoviesByYear(int year) {
        return store.values()
                .stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

}