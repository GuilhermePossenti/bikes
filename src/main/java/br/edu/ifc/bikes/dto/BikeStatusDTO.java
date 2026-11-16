package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BikeStatusDTO(
        @NotBlank(message = "O status é obrigatório")
        @Pattern(regexp = "DISPONIVEL|ALUGADA|MANUTENCAO",
                message = "O status deve ser DISPONIVEL, ALUGADA ou MANUTENCAO")
        String status
) { }
