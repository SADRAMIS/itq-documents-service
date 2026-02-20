package ru.itq.documents.service.exception;

public class RegistryErrorException extends RuntimeException {
    public RegistryErrorException(String message) {
        super(message);
    }
}
