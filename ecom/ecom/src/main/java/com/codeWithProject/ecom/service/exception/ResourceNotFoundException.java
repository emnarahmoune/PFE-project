package com.codeWithProject.ecom.service.exception;

/**
 * Exception lancée lorsqu'une ressource n'est pas trouvée
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s non trouvé avec l'id : %d", resource, id));
    }

    public ResourceNotFoundException(String resource, String field, String value) {
        super(String.format("%s non trouvé avec %s : %s", resource, field, value));
    }
}