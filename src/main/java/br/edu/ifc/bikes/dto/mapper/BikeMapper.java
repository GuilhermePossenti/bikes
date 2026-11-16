package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.BikeRequestDTO;
import br.edu.ifc.bikes.dto.BikeResponseDTO;
import br.edu.ifc.bikes.entity.Bike;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BikeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataModificacao", ignore = true)
    @Mapping(target = "criadoPor", ignore = true)
    @Mapping(target = "modificadoPor", ignore = true)
    Bike toBike(BikeRequestDTO dto);

    BikeResponseDTO toResponse(Bike bike);

    List<BikeResponseDTO> toResponse(List<Bike> bikes);
}
