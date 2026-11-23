package br.edu.ifc.bikes;

import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario admin;
    private Usuario cliente;
    private String tokenAdmin;
    private String tokenCliente;

    @BeforeEach
    void setUp() throws Exception {
        admin = criarUsuario("admin@email.com", Usuario.Role.ADMIN);
        cliente = criarUsuario("cliente@email.com", Usuario.Role.CLIENTE);
        tokenAdmin = login("admin@email.com");
        tokenCliente = login("cliente@email.com");
    }

    @AfterEach
    void tearDown() {
        usuarioRepository.deleteAll();
    }

    private Usuario criarUsuario(String username, Usuario.Role role) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode("123456"));
        usuario.setRole(role);
        return usuarioRepository.save(usuario);
    }

    private String login(String username) throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"username\":\"%s\",\"password\":\"123456\"}", username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    private String usuarioJson(String username, String password) {
        return String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
    }

    @Test
    void criarUsuario_UsernameEPasswordValidos_RetornaUsuarioCriado() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson("novo@email.com", "123456")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("novo@email.com"))
                .andExpect(jsonPath("$.role").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(usuarioRepository.findByUsername("novo@email.com")).isPresent();
    }

    @Test
    void criarUsuario_UsernameRepetido_RetornaErrorMessageStatus409() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson("cliente@email.com", "123456")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(usuarioRepository.count()).isEqualTo(2);
    }

    @Test
    void criarUsuario_UsernameInvalido_RetornaErrorMessageStatus422() throws Exception {
        for (String username : new String[]{"", "   ", "semarroba", "@email.com", "maria@"}) {
            mockMvc.perform(post("/api/v1/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(usuarioJson(username, "123456")))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.status").value(422));
        }
        assertThat(usuarioRepository.count()).isEqualTo(2);
    }

    @Test
    void criarUsuario_PasswordInvalido_RetornaErrorMessageStatus422() throws Exception {
        for (String password : new String[]{"", "12345", "1234567"}) {
            mockMvc.perform(post("/api/v1/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(usuarioJson("novo@email.com", password)))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.status").value(422));
        }
        assertThat(usuarioRepository.findByUsername("novo@email.com")).isEmpty();
    }

    @Test
    void buscarUsuario_IdExistente_RetornaUsuario() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/{id}", cliente.getId()).header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cliente.getId()))
                .andExpect(jsonPath("$.username").value("cliente@email.com"))
                .andExpect(jsonPath("$.role").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void buscarUsuario_IdInexistente_RetornaErrorMessageStatus404() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/{id}", 999999L).header("Authorization", tokenAdmin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void buscarUsuario_ClienteBuscaOProprioUsuario_RetornaStatus200() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/{id}", cliente.getId()).header("Authorization", tokenCliente))
                .andExpect(status().isOk());
    }

    @Test
    void buscarUsuario_ClienteBuscaOutroUsuario_RetornaStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/{id}", admin.getId()).header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void buscarUsuario_SemToken_RetornaStatus401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/{id}", cliente.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarUsuarios_ComPerfilAdmin_RetornaTodos() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void listarUsuarios_ComPerfilCliente_RetornaStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
    }
}
