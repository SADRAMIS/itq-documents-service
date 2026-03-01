package ru.itq.documents.service.exception;

public class RegistryErrorException extends RuntimeException {
    public RegistryErrorException(String message) {
        super(message);
    }

    public RegistryErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
