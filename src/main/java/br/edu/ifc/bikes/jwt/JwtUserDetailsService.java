package br.edu.ifc.bikes.jwt;

import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtUserDetailsService implements UserDetailsService {

    private final UsuarioService usuarioService;
    private final JwtUtils jwtUtils;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new JwtUserDetails(buscarUsuario(username));
    }

    public JwtToken getTokenAuthenticated(String username) {
        Usuario usuario = buscarUsuario(username);
        return jwtUtils.createToken(usuario.getUsername(), usuario.getRole().name());
    }

    private Usuario buscarUsuario(String username) {
        return usuarioService.buscarPorUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format("Usuário '%s' não encontrado", username)));
    }
}
