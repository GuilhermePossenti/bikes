package br.edu.ifc.bikes.exception;

public class BikeUniqueViolationException extends RuntimeException {
    public BikeUniqueViolationException(String message) {
        super(message);
    }
}
