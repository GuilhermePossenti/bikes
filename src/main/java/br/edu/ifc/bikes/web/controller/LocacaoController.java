package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.config.SpringDocOpenApiConfig;
import br.edu.ifc.bikes.dto.LocacaoDevolucaoDTO;
import br.edu.ifc.bikes.dto.LocacaoRelatorioDTO;
import br.edu.ifc.bikes.dto.LocacaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.jwt.JwtUserDetails;
import br.edu.ifc.bikes.service.JasperService;
import br.edu.ifc.bikes.service.LocacaoService;
import br.edu.ifc.bikes.web.exception.ErrorMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Locações", description = "Contém as operações de aluguel e devolução de bikes e o relatório de locações. " +
        "Todas exigem autenticação com bearer token.")
@SecurityRequirement(name = SpringDocOpenApiConfig.SECURITY)
@RestController
@RequestMapping("api/v1/locacoes")
@RequiredArgsConstructor
public class LocacaoController {

    private final LocacaoService locacaoService;
    private final JasperService jasperService;

    @Operation(summary = "Solicitar o aluguel de uma bike",
            description = "O cliente autenticado aluga uma bike DISPONIVEL e informa os dados do cartão de crédito. " +
                    "Registra a data/hora de retirada e a data/hora prevista de devolução, e a bike passa a ALUGADA. " +
                    "Do cartão só os 4 últimos dígitos são guardados. Requer perfil 'CLIENTE'.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Locação criada com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LocacaoResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Recurso não permitido ao perfil ADMIN",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Bike não encontrada ou usuário ainda não cadastrou o cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "Bike indisponível (já alugada ou em manutenção)",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "422", description = "Recurso não processado por dados de entrada inválidos",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<LocacaoResponseDTO> create(@Valid @RequestBody LocacaoRequestDTO dto,
                                                     @AuthenticationPrincipal JwtUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locacaoService.create(dto, userDetails.getId()));
    }

    @Operation(summary = "Registrar a devolução da bike",
            description = "Registra a data/hora efetiva de devolução, calcula o tempo de uso e o valor a ser debitado no " +
                    "cartão (preço por hora, proporcional ao tempo) e atualiza a bike para DISPONIVEL. " +
                    "Se o corpo trouxer 'bikeComProblema': true, a bike vai para MANUTENCAO. " +
                    "O cliente devolve apenas as próprias locações; o ADMIN, qualquer uma.",
            parameters = @Parameter(name = "id", description = "Id da locação", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Devolução registrada com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LocacaoResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Locação pertence a outro cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Locação não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "Locação já devolvida",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PatchMapping("/{id}/devolucao")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<LocacaoResponseDTO> devolver(@PathVariable Long id,
                                                       @RequestBody(required = false) LocacaoDevolucaoDTO dto,
                                                       @AuthenticationPrincipal JwtUserDetails userDetails) {
        boolean comProblema = dto != null && Boolean.TRUE.equals(dto.bikeComProblema());
        return ResponseEntity.ok(locacaoService.devolver(id, comProblema, userDetails.getId(), isAdmin(userDetails)));
    }

    @Operation(summary = "Relatório de locações e devoluções",
            description = "Lista todas as locações com nome do cliente, código da bike, data de retirada e devolução, " +
                    "duração (em minutos) e status. Requer perfil 'ADMIN'.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Relatório de locações",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = LocacaoRelatorioDTO.class)))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Recurso não permitido ao perfil CLIENTE",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/relatorio")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LocacaoRelatorioDTO>> relatorio() {
        return ResponseEntity.ok(locacaoService.relatorio());
    }

    @Operation(summary = "Relatório de locações e devoluções em PDF",
            description = "Gera um PDF com o mesmo conteúdo do relatório em JSON. Requer perfil 'ADMIN'.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF gerado com sucesso",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Recurso não permitido ao perfil CLIENTE",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/relatorio/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> relatorioPdf() {
        byte[] pdf = jasperService.gerarRelatorioLocacoes(locacaoService.relatorio());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=locacoes-" + System.currentTimeMillis() + ".pdf")
                .body(pdf);
    }

    @Operation(summary = "Recuperar uma locação pelo id",
            description = "O cliente consulta apenas as próprias locações; o ADMIN, qualquer uma.",
            parameters = @Parameter(name = "id", description = "Id da locação", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recurso recuperado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LocacaoResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Locação pertence a outro cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Locação não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<LocacaoResponseDTO> getById(@PathVariable Long id,
                                                      @AuthenticationPrincipal JwtUserDetails userDetails) {
        return ResponseEntity.ok(locacaoService.getById(id, userDetails.getId(), isAdmin(userDetails)));
    }

    private boolean isAdmin(JwtUserDetails userDetails) {
        return "ADMIN".equals(userDetails.getRole());
    }
}
