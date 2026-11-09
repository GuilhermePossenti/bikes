package br.edu.ifc.bikes.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

//resposta 401 quando um recurso protegido é acessado sem token (ou com token inválido)
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        response.setStatus(status.value());
        response.setHeader("WWW-Authenticate", "Bearer realm=\"/api/v1/auth\"");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        //mesmo formato do ErrorMessage usado no ApiExceptionHandler
        response.getWriter().write(String.format(
                "{\"path\":\"%s\",\"method\":\"%s\",\"status\":%d,\"statusMessage\":\"%s\",\"message\":\"%s\"}",
                request.getRequestURI(), request.getMethod(), status.value(), status.getReasonPhrase(),
                "Usuário não autenticado"));
    }
}
