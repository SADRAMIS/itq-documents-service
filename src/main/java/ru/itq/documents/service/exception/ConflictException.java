package ru.itq.documents.service.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String msg) { super("Conflict: " + msg); }
}
