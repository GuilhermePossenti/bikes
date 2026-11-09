package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.entity.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    //id, usuário e auditoria são definidos pelo service/JPA, não pelo corpo da requisição
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataModificacao", ignore = true)
    @Mapping(target = "criadoPor", ignore = true)
    @Mapping(target = "modificadoPor", ignore = true)
    Cliente toCliente(ClienteRequestDTO dto);

    ClienteResponseDTO toResponse(Cliente cliente);

    List<ClienteResponseDTO> toResponse(List<Cliente> clientes);
}
