package ru.itq.documents.service.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
    
    public NotFoundException(Long id) {
        super("Document not found: " + id);
    }
}


