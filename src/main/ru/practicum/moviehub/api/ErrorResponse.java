package ru.practicum.moviehub.api;

import java.util.List;

public class ErrorResponse {
    private String error;
    private List<String> details;

    public ErrorResponse(String error, List<String> details) {
        this.error = error;
        this.details = details;
    }

    public ErrorResponse(String error) {
        this.error = error;
    }

    public ErrorResponse() {
    }

    public void addDetails(String detail) {
        details.add(detail);
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}