package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BikeRequestDTO(
        @NotBlank(message = "O código é obrigatório")
        @Pattern(regexp = "^[A-Z0-9-]{3,20}$",
                message = "O código deve ter de 3 a 20 caracteres: letras maiúsculas, números ou hífen")
        String codigo,
        @NotBlank(message = "O modelo é obrigatório")
        @Size(min = 2, max = 100, message = "O modelo deve ter entre 2 e 100 caracteres")
        String modelo,
        @NotBlank(message = "A marca é obrigatória")
        @Size(min = 2, max = 50, message = "A marca deve ter entre 2 e 50 caracteres")
        String marca,
        @NotBlank(message = "A cor é obrigatória")
        @Size(min = 3, max = 30, message = "A cor deve ter entre 3 e 30 caracteres")
        String cor
) { }
