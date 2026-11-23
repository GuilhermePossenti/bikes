package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.config.SpringDocOpenApiConfig;
import br.edu.ifc.bikes.dto.BikeRequestDTO;
import br.edu.ifc.bikes.dto.BikeResponseDTO;
import br.edu.ifc.bikes.dto.BikeStatusDTO;
import br.edu.ifc.bikes.service.BikeService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bikes", description = "Contém todas as operações relativas ao recurso de uma bike. " +
        "Todas exigem autenticação com bearer token.")
@SecurityRequirement(name = SpringDocOpenApiConfig.SECURITY)
@RestController
@RequestMapping("api/v1/bikes")
@RequiredArgsConstructor
public class BikeController {

    private final BikeService bikeService;

    @Operation(summary = "Cadastrar uma nova bike",
            description = "Recurso para cadastrar uma bike, que começa com status DISPONIVEL. Requer perfil 'ADMIN'.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Recurso criado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BikeResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Recurso não permitido ao perfil CLIENTE",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "Código da bike já cadastrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "422", description = "Recurso não processado por dados de entrada inválidos",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BikeResponseDTO> create(@Valid @RequestBody BikeRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bikeService.create(dto));
    }

    @Operation(summary = "Listar bikes disponíveis",
            description = "Lista as bikes com status DISPONIVEL. Permitido a qualquer usuário autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de bikes disponíveis",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = BikeResponseDTO.class)))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/disponiveis")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<List<BikeResponseDTO>> getDisponiveis() {
        return ResponseEntity.ok(bikeService.getDisponiveis());
    }

    @Operation(summary = "Recuperar uma bike pelo id",
            description = "Permitido a qualquer usuário autenticado.",
            parameters = @Parameter(name = "id", description = "Id da bike", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recurso recuperado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BikeResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Bike não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<BikeResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bikeService.getById(id));
    }

    @Operation(summary = "Recuperar uma bike pelo código",
            description = "Localiza a bike pelo código único (diferente do id). Requer perfil 'ADMIN'.",
            parameters = @Parameter(name = "codigo", description = "Código da bike", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recurso recuperado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BikeResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Recurso não permitido ao perfil CLIENTE",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Bike não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/codigo/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BikeResponseDTO> getByCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(bikeService.getByCodigo(codigo));
    }

    @Operation(summary = "Listar todas as bikes",
            description = "Lista todas as bikes, com qualquer status. Requer perfil 'ADMIN'.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista com todas as bikes cadastradas",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = BikeResponseDTO.class)))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Recurso não permitido ao perfil CLIENTE",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BikeResponseDTO>> getAll() {
        return ResponseEntity.ok(bikeService.getAll());
    }

    @Operation(summary = "Alterar o status de uma bike",
            description = "Altera o status para DISPONIVEL, ALUGADA ou MANUTENCAO. Requer perfil 'ADMIN'.",
            parameters = @Parameter(name = "id", description = "Id da bike", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status alterado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BikeResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Recurso não permitido ao perfil CLIENTE",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Bike não encontrada",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "422", description = "Status inválido",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BikeResponseDTO> alterarStatus(@PathVariable Long id,
                                                         @Valid @RequestBody BikeStatusDTO dto) {
        return ResponseEntity.ok(bikeService.alterarStatus(id, dto.status()));
    }
}
