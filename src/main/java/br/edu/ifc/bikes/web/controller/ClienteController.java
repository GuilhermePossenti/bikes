package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.config.SpringDocOpenApiConfig;
import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.jwt.JwtUserDetails;
import br.edu.ifc.bikes.service.ClienteService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Clientes", description = "Contém todas as operações relativas ao recurso de um cliente. " +
        "Todas exigem autenticação com bearer token.")
@SecurityRequirement(name = SpringDocOpenApiConfig.SECURITY)
@RestController
@RequestMapping("api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(summary = "Criar um novo cliente",
            description = "Recurso para criar um cliente vinculado ao usuário autenticado. Requer perfil 'CLIENTE'.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Recurso criado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Recurso não permitido ao perfil ADMIN",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "409", description = "CPF já cadastrado ou usuário já possui cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "422", description = "Recurso não processado por dados de entrada inválidos",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClienteResponseDTO> create(@Valid @RequestBody ClienteRequestDTO dto,
                                                     @AuthenticationPrincipal JwtUserDetails userDetails) {
        ClienteResponseDTO cliente = clienteService.create(dto, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(cliente);
    }

    @Operation(summary = "Recuperar os dados do cliente autenticado",
            description = "O cliente consulta apenas os seus próprios dados. Requer perfil 'CLIENTE'.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recurso recuperado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Recurso não permitido ao perfil ADMIN",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Usuário autenticado ainda não cadastrou o cliente",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/detalhes")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClienteResponseDTO> getDetalhes(@AuthenticationPrincipal JwtUserDetails userDetails) {
        return ResponseEntity.ok(clienteService.getByUsuarioId(userDetails.getId()));
    }

    @Operation(summary = "Recuperar um cliente pelo id",
            description = "Recurso para recuperar qualquer cliente pelo id. Requer perfil 'ADMIN'.",
            parameters = @Parameter(name = "id", description = "Id do cliente", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recurso recuperado com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Recurso não permitido ao perfil CLIENTE",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClienteResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.getById(id));
    }

    @Operation(summary = "Listar todos os clientes",
            description = "Recurso para listar todos os clientes cadastrados. Requer perfil 'ADMIN'.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista com todos os clientes cadastrados",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ClienteResponseDTO.class)))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "403", description = "Recurso não permitido ao perfil CLIENTE",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClienteResponseDTO>> getAll() {
        return ResponseEntity.ok(clienteService.getAll());
    }
}
