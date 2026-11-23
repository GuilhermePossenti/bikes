package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.LocacaoRelatorioDTO;
import br.edu.ifc.bikes.dto.LocacaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.entity.Bike;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Locacao;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.LocacaoConflictException;
import br.edu.ifc.bikes.repository.BikeRepository;
import br.edu.ifc.bikes.repository.ClienteRepository;
import br.edu.ifc.bikes.repository.LocacaoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LocacaoService {

    private final LocacaoRepository locacaoRepository;
    private final BikeRepository bikeRepository;
    private final ClienteRepository clienteRepository;
    private final BigDecimal precoHora;

    public LocacaoService(LocacaoRepository locacaoRepository, BikeRepository bikeRepository,
                          ClienteRepository clienteRepository,
                          @Value("${locacao.preco-hora:10.00}") BigDecimal precoHora) {
        this.locacaoRepository = locacaoRepository;
        this.bikeRepository = bikeRepository;
        this.clienteRepository = clienteRepository;
        this.precoHora = precoHora;
    }

    @Transactional
    public LocacaoResponseDTO create(LocacaoRequestDTO dto, Long usuarioId) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(
                () -> new EntityNotFoundException("Cadastre seus dados de cliente antes de alugar uma bike"));

        Bike bike = bikeRepository.findByIdForUpdate(dto.bikeId()).orElseThrow(
                () -> new EntityNotFoundException(String.format("Bike com id=%d não encontrada", dto.bikeId())));

        if (bike.getStatus() != Bike.Status.DISPONIVEL) {
            throw new LocacaoConflictException(
                    String.format("A bike '%s' não está disponível para locação", bike.getCodigo()));
        }

        LocalDateTime agora = LocalDateTime.now();
        Locacao locacao = new Locacao();
        locacao.setCliente(cliente);
        locacao.setBike(bike);
        locacao.setDataRetirada(agora);
        locacao.setDataPrevistaDevolucao(agora.plusHours(dto.horasPrevistas()));
        locacao.setCartaoFinal(dto.numeroCartao().substring(dto.numeroCartao().length() - 4));
        locacao.setCartaoTitular(dto.nomeTitular());

        bike.setStatus(Bike.Status.ALUGADA);
        bikeRepository.save(bike);
        return toResponse(locacaoRepository.saveAndFlush(locacao));
    }

    @Transactional
    public LocacaoResponseDTO devolver(Long id, boolean bikeComProblema, Long usuarioId, boolean admin) {
        Locacao locacao = buscarPermitida(id, usuarioId, admin);
        if (locacao.getStatus() == Locacao.Status.FINALIZADA) {
            throw new LocacaoConflictException(String.format("A locação id=%d já foi devolvida", id));
        }

        Bike bike = bikeRepository.findByIdForUpdate(locacao.getBike().getId()).orElseThrow();

        LocalDateTime agora = LocalDateTime.now();
        locacao.setDataDevolucao(agora);
        locacao.setValor(calcularValor(locacao.getDataRetirada(), agora, precoHora));
        locacao.setStatus(Locacao.Status.FINALIZADA);

        bike.setStatus(bikeComProblema ? Bike.Status.MANUTENCAO : Bike.Status.DISPONIVEL);
        bikeRepository.save(bike);
        return toResponse(locacaoRepository.saveAndFlush(locacao));
    }

    @Transactional(readOnly = true)
    public LocacaoResponseDTO getById(Long id, Long usuarioId, boolean admin) {
        return toResponse(buscarPermitida(id, usuarioId, admin));
    }

    @Transactional(readOnly = true)
    public List<LocacaoRelatorioDTO> relatorio() {
        LocalDateTime agora = LocalDateTime.now();
        return locacaoRepository.findAllComClienteEBike().stream()
                .map(l -> new LocacaoRelatorioDTO(
                        l.getCliente().getNome(),
                        l.getBike().getCodigo(),
                        l.getDataRetirada(),
                        l.getDataDevolucao(),
                        duracaoMinutos(l, agora),
                        l.getValor(),
                        l.getStatus().name()))
                .toList();
    }

    public static BigDecimal calcularValor(LocalDateTime retirada, LocalDateTime devolucao, BigDecimal precoHora) {
        return precoHora
                .multiply(BigDecimal.valueOf(minutos(retirada, devolucao)))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    static long minutos(LocalDateTime inicio, LocalDateTime fim) {
        long segundos = Math.max(Duration.between(inicio, fim).getSeconds(), 0);
        return Math.max((segundos + 59) / 60, 1);
    }

    private static long duracaoMinutos(Locacao locacao, LocalDateTime agora) {
        LocalDateTime fim = locacao.getDataDevolucao() != null ? locacao.getDataDevolucao() : agora;
        return minutos(locacao.getDataRetirada(), fim);
    }

    private Locacao buscarPermitida(Long id, Long usuarioId, boolean admin) {
        Locacao locacao = locacaoRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException(String.format("Locação com id=%d não encontrada", id)));
        if (!admin && !locacao.getCliente().getUsuario().getId().equals(usuarioId)) {
            throw new AccessDeniedException("Locação de outro cliente");
        }
        return locacao;
    }

    private LocacaoResponseDTO toResponse(Locacao l) {
        return new LocacaoResponseDTO(
                l.getId(),
                l.getCliente().getId(),
                l.getCliente().getNome(),
                l.getBike().getId(),
                l.getBike().getCodigo(),
                l.getDataRetirada(),
                l.getDataPrevistaDevolucao(),
                l.getDataDevolucao(),
                duracaoMinutos(l, LocalDateTime.now()),
                l.getValor(),
                l.getCartaoFinal(),
                l.getStatus().name());
    }
}
