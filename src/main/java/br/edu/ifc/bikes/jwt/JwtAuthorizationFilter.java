package br.edu.ifc.bikes.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

//lê o token do header Authorization em cada requisição e autentica o usuário no Spring Security.
//não é um @Component para não ser registrado duas vezes como filtro do servlet
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final JwtUserDetailsService detailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(JwtUtils.JWT_AUTHORIZATION);

        if (token != null && token.startsWith(JwtUtils.JWT_BEARER) && jwtUtils.isTokenValid(token)) {
            try {
                String username = jwtUtils.getUsernameFromToken(token);
                UserDetails userDetails = detailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        UsernamePasswordAuthenticationToken.authenticated(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (UsernameNotFoundException ex) {
                //o usuário do token não existe mais: segue sem autenticação e o Spring Security devolve 401
            }
        }

        filterChain.doFilter(request, response);
    }
}
