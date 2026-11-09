package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

//o usuário do cliente não vem no corpo: ele é o usuário autenticado (token)
public record ClienteRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        String nome,
        @NotBlank(message = "O CPF é obrigatório")
        @Size(min = 11, max = 11, message = "O CPF deve ter 11 dígitos, sem pontos e traço")
        @CPF(message = "O CPF deve ser válido")
        String cpf
) { }
