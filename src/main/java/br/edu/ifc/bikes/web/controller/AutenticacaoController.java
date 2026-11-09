package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.UsuarioLoginDTO;
import br.edu.ifc.bikes.jwt.JwtToken;
import br.edu.ifc.bikes.jwt.JwtUserDetailsService;
import br.edu.ifc.bikes.web.exception.ErrorMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação", description = "Recurso para fazer login e receber o token JWT")
@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AuthenticationManager authenticationManager;
    private final JwtUserDetailsService detailsService;

    @Operation(summary = "Autenticar na API", description = "Recurso de autenticação na API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso e retorno de um bearer token",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtToken.class))),
                    @ApiResponse(responseCode = "400", description = "Credenciais inválidas",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "422", description = "Campo(s) inválido(s)",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PostMapping
    public ResponseEntity<?> autenticar(@Valid @RequestBody UsuarioLoginDTO dto, HttpServletRequest request) {
        try {
            //confere username e senha (BCrypt) usando o JwtUserDetailsService
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.username(), dto.password()));
            JwtToken token = detailsService.getTokenAuthenticated(dto.username());
            return ResponseEntity.ok(token);
        } catch (AuthenticationException ex) {
            return ResponseEntity.badRequest()
                    .body(new ErrorMessage(request, HttpStatus.BAD_REQUEST, "Credenciais inválidas"));
        }
    }
}
