package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.UsuarioRequestDTO;
import br.edu.ifc.bikes.dto.UsuarioResponseDTO;
import br.edu.ifc.bikes.dto.UsuarioSenhaDTO;
import br.edu.ifc.bikes.dto.mapper.UsuarioMapper;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.PasswordInvalidException;
import br.edu.ifc.bikes.exception.UsernameUniqueViolationException;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    public UsuarioResponseDTO create(UsuarioRequestDTO usuarioRequestDTO) {
        try {
            Usuario usuario = usuarioMapper.toUsuario(usuarioRequestDTO);
            //a senha é salva criptografada (BCrypt), nunca em texto puro
            usuario.setPassword(passwordEncoder.encode(usuarioRequestDTO.password()));
            return usuarioMapper.toResponse(usuarioRepository.save(usuario));
        }catch (RuntimeException ex){

            throw new UsernameUniqueViolationException(String.format("O nome do usuário '%s' já existe", usuarioRequestDTO.username()));
        }
    }

    @Transactional()
    public UsuarioResponseDTO getById(Long id) {
        return usuarioMapper.toResponse(buscarPorId(id));
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id).orElseThrow(
                ()->new EntityNotFoundException(String.format("Usuário com id=%d não encontrado", id))
        );
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    public void updatePassword(Long id, UsuarioSenhaDTO dto) {
        Usuario usuario = buscarPorId(id);

        //compara a senha digitada com o hash salvo no banco
        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getPassword())) {
            throw new PasswordInvalidException("Sua senha atual não confere");
        }

        if (!dto.novaSenha().equals(dto.confirmacaoSenha())) {
            throw new PasswordInvalidException("Nova senha não confere com a confirmação de senha");
        }

        usuario.setPassword(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
    }

    public List<UsuarioResponseDTO> getAll(){
        return usuarioMapper.toResponse(usuarioRepository.findAll());
    }
}
