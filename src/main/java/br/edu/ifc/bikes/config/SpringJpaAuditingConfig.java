package br.edu.ifc.bikes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

//preenche automaticamente os campos de auditoria (dataCriacao, criadoPor, ...) de Usuario e Cliente
@EnableJpaAuditing
@Configuration
public class SpringJpaAuditingConfig implements AuditorAware<String> {

    //o usuário logado é o autor da criação/modificação; sem login (ex.: cadastro de usuário) fica vazio
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(authentication.getName());
    }
}
