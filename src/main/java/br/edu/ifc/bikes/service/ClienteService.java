package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.dto.mapper.ClienteMapper;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.exception.ClienteUniqueViolationException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final UsuarioService usuarioService;

    //o cliente é sempre associado ao usuário autenticado
    @Transactional
    public ClienteResponseDTO create(ClienteRequestDTO dto, Long usuarioId) {
        if (clienteRepository.existsByUsuarioId(usuarioId)) {
            throw new ClienteUniqueViolationException("Este usuário já possui um cliente cadastrado");
        }
        if (clienteRepository.existsByCpf(dto.cpf())) {
            throw new ClienteUniqueViolationException(
                    String.format("CPF '%s' não pode ser cadastrado, já existe no sistema", dto.cpf()));
        }

        Cliente cliente = clienteMapper.toCliente(dto);
        cliente.setUsuario(usuarioService.buscarPorId(usuarioId));
        try {
            return clienteMapper.toResponse(clienteRepository.saveAndFlush(cliente));
        } catch (DataIntegrityViolationException ex) {
            //duas requisições ao mesmo tempo podem passar pelas verificações acima; o banco garante o unique
            throw new ClienteUniqueViolationException(
                    String.format("CPF '%s' não pode ser cadastrado, já existe no sistema", dto.cpf()));
        }
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO getById(Long id) {
        return clienteMapper.toResponse(clienteRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException(String.format("Cliente com id=%d não encontrado", id))));
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO getByUsuarioId(Long usuarioId) {
        return clienteMapper.toResponse(clienteRepository.findByUsuarioId(usuarioId).orElseThrow(
                () -> new EntityNotFoundException("Nenhum cliente cadastrado para o usuário autenticado")));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> getAll() {
        return clienteMapper.toResponse(clienteRepository.findAll());
    }
}
