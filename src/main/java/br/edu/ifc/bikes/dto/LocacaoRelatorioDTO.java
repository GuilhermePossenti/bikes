package br.edu.ifc.bikes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LocacaoRelatorioDTO(
        String nomeCliente,
        String codigoBike,
        LocalDateTime dataRetirada,
        LocalDateTime dataDevolucao,
        Long duracaoMinutos,
        BigDecimal valor,
        String status
) { }
