package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.LuhnCheck;

public record LocacaoRequestDTO(
        @NotNull(message = "A bike é obrigatória")
        Long bikeId,
        @NotNull(message = "O tempo previsto de uso é obrigatório")
        @Min(value = 1, message = "O tempo previsto deve ser de no mínimo 1 hora")
        @Max(value = 72, message = "O tempo previsto deve ser de no máximo 72 horas")
        Integer horasPrevistas,
        @NotBlank(message = "O número do cartão é obrigatório")
        @Pattern(regexp = "\\d{13,19}", message = "O número do cartão deve ter de 13 a 19 dígitos")
        @LuhnCheck(message = "O número do cartão é inválido")
        String numeroCartao,
        @NotBlank(message = "O nome do titular é obrigatório")
        @Size(min = 3, max = 100, message = "O nome do titular deve ter entre 3 e 100 caracteres")
        String nomeTitular,
        @NotBlank(message = "A validade do cartão é obrigatória")
        @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "A validade deve estar no formato MM/AA")
        String validade,
        @NotBlank(message = "O CVV é obrigatório")
        @Pattern(regexp = "\\d{3,4}", message = "O CVV deve ter 3 ou 4 dígitos")
        String cvv
) { }
