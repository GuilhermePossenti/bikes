package br.edu.ifc.bikes;

import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioAlterarSenhaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Long id;

    //cria um usuário com a senha "123456" antes de cada teste
    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setUsername("teste@email.com");
        usuario.setPassword("123456");
        id = usuarioRepository.save(usuario).getId();
    }

    @AfterEach
    void tearDown() {
        usuarioRepository.deleteAll();
    }

    //monta o corpo da requisição com os campos do UsuarioSenhaDTO
    private String senhaJson(String senhaAtual, String novaSenha, String confirmacaoSenha) {
        return String.format("{\"senhaAtual\":\"%s\",\"novaSenha\":\"%s\",\"confirmacaoSenha\":\"%s\"}",
                senhaAtual, novaSenha, confirmacaoSenha);
    }

    //a alteração de senha não retorna corpo, então apenas o status é testado
    @Test
    void alterarSenha_comDadosValidos_retornarStatus204() throws Exception {
        mockMvc.perform(patch("/api/v1/usuarios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(senhaJson("123456", "654321", "654321")))
                .andExpect(status().isNoContent());
    }

    @Test
    void alterarSenha_comSenhaAtualInvalida_retornarStatus400() throws Exception {
        mockMvc.perform(patch("/api/v1/usuarios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(senhaJson("000000", "654321", "654321")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alterarSenha_comSenhaEConfirmacaoDiferentes_retornarStatus400() throws Exception {
        mockMvc.perform(patch("/api/v1/usuarios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(senhaJson("123456", "654321", "111111")))
                .andExpect(status().isBadRequest());
    }

    //campos vazios, senhas com menos e com mais de 6 caracteres
    @Test
    void alterarSenha_comCamposInvalidos_retornarStatus422() throws Exception {
        mockMvc.perform(patch("/api/v1/usuarios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(senhaJson("", "", "")))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(patch("/api/v1/usuarios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(senhaJson("12345", "65432", "65432")))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(patch("/api/v1/usuarios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(senhaJson("1234567", "6543210", "6543210")))
                .andExpect(status().isUnprocessableContent());
    }
}
