package br.edu.ifc.bikes.exception;

//CPF já cadastrado ou usuário que já possui um cliente
public class ClienteUniqueViolationException extends RuntimeException{
    public ClienteUniqueViolationException(String message){
        super(message);
    }
}
