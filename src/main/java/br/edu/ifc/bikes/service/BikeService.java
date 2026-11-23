package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.BikeRequestDTO;
import br.edu.ifc.bikes.dto.BikeResponseDTO;
import br.edu.ifc.bikes.dto.mapper.BikeMapper;
import br.edu.ifc.bikes.entity.Bike;
import br.edu.ifc.bikes.exception.BikeUniqueViolationException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.repository.BikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class BikeService {

    private final BikeRepository bikeRepository;
    private final BikeMapper bikeMapper;

    @Transactional
    public BikeResponseDTO create(BikeRequestDTO dto) {
        if (bikeRepository.existsByCodigo(dto.codigo())) {
            throw codigoDuplicado(dto.codigo());
        }
        try {
            return bikeMapper.toResponse(bikeRepository.saveAndFlush(bikeMapper.toBike(dto)));
        } catch (DataIntegrityViolationException ex) {
            throw codigoDuplicado(dto.codigo());
        }
    }

    @Transactional(readOnly = true)
    public BikeResponseDTO getById(Long id) {
        return bikeMapper.toResponse(buscarPorId(id));
    }

    @Transactional(readOnly = true)
    public BikeResponseDTO getByCodigo(String codigo) {
        return bikeMapper.toResponse(bikeRepository.findByCodigo(codigo).orElseThrow(
                () -> new EntityNotFoundException(String.format("Bike com código '%s' não encontrada", codigo))));
    }

    @Transactional(readOnly = true)
    public List<BikeResponseDTO> getAll() {
        return bikeMapper.toResponse(bikeRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<BikeResponseDTO> getDisponiveis() {
        return bikeMapper.toResponse(bikeRepository.findByStatus(Bike.Status.DISPONIVEL));
    }

    @Transactional
    public BikeResponseDTO alterarStatus(Long id, String status) {
        Bike bike = buscarPorId(id);
        bike.setStatus(Bike.Status.valueOf(status));
        return bikeMapper.toResponse(bikeRepository.saveAndFlush(bike));
    }

    private Bike buscarPorId(Long id) {
        return bikeRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException(String.format("Bike com id=%d não encontrada", id)));
    }

    private BikeUniqueViolationException codigoDuplicado(String codigo) {
        return new BikeUniqueViolationException(
                String.format("Bike com código '%s' não pode ser cadastrada, já existe no sistema", codigo));
    }
}
