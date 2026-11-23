package br.edu.ifc.bikes.exception;

public class LocacaoConflictException extends RuntimeException {
    public LocacaoConflictException(String message) {
        super(message);
    }
}
