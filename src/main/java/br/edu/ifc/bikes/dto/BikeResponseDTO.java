package br.edu.ifc.bikes.dto;

public record BikeResponseDTO(
        Long id,
        String codigo,
        String modelo,
        String marca,
        String cor,
        String status
) { }
