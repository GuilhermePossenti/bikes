package br.edu.ifc.bikes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LocacaoResponseDTO(
        Long id,
        Long clienteId,
        String nomeCliente,
        Long bikeId,
        String codigoBike,
        LocalDateTime dataRetirada,
        LocalDateTime dataPrevistaDevolucao,
        LocalDateTime dataDevolucao,
        Long duracaoMinutos,
        BigDecimal valor,
        String cartaoFinal,
        String status
) { }
