package br.edu.ifc.bikes.jwt;

import br.edu.ifc.bikes.entity.Usuario;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

//usuário autenticado; guarda o id para as regras de "consultar apenas os próprios dados"
public class JwtUserDetails extends User {

    private final Long id;
    private final String role;

    public JwtUserDetails(Usuario usuario) {
        super(usuario.getUsername(), usuario.getPassword(),
                AuthorityUtils.createAuthorityList("ROLE_" + usuario.getRole().name()));
        this.id = usuario.getId();
        this.role = usuario.getRole().name();
    }

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }
}
