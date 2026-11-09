package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioLoginDTO(
        @NotBlank(message = "O username é obrigatório")
        @Email(message = "O email deve ser válido")
        String username,
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 6)
        String password
) { }
