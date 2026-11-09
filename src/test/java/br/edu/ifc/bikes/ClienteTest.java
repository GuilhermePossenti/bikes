package br.edu.ifc.bikes;

import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.ClienteRepository;
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
class ClienteTest {

    //CPFs válidos (dígitos verificadores corretos)
    private static final String CPF_1 = "52998224725";
    private static final String CPF_2 = "11144477735";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenCliente;
    private String tokenOutroCliente;
    private String tokenAdmin;

    @BeforeEach
    void setUp() throws Exception {
        criarUsuario("cliente@email.com", Usuario.Role.CLIENTE);
        criarUsuario("outro@email.com", Usuario.Role.CLIENTE);
        criarUsuario("admin@email.com", Usuario.Role.ADMIN);
        tokenCliente = login("cliente@email.com");
        tokenOutroCliente = login("outro@email.com");
        tokenAdmin = login("admin@email.com");
    }

    @AfterEach
    void tearDown() {
        clienteRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private void criarUsuario(String username, Usuario.Role role) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode("123456"));
        usuario.setRole(role);
        usuarioRepository.save(usuario);
    }

    private String login(String username) throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"username\":\"%s\",\"password\":\"123456\"}", username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    private String clienteJson(String nome, String cpf) {
        return String.format("{\"nome\":\"%s\",\"cpf\":\"%s\"}", nome, cpf);
    }

    private Long cadastrarCliente(String token, String nome, String cpf) throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clienteJson(nome, cpf)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    // ===== autenticação =====

    @Test
    void login_comSenhaErrada_retornarStatus400() throws Exception {
        mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cliente@email.com\",\"password\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarUsuario_salvaSenhaCriptografada() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"novo@email.com\",\"password\":\"123456\"}"))
                .andExpect(status().isCreated());

        Usuario salvo = usuarioRepository.findByUsername("novo@email.com").orElseThrow();
        assertThat(salvo.getPassword()).isNotEqualTo("123456");
        assertThat(passwordEncoder.matches("123456", salvo.getPassword())).isTrue();
    }

    //a documentação OpenAPI fica pública e já descreve o recurso Cliente e o esquema bearer
    @Test
    void documentacao_semToken_retornarStatus200ComRecursoCliente() throws Exception {
        mockMvc.perform(get("/docs-bikes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/clientes']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/clientes/detalhes']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.security.scheme").value("bearer"));
    }

    // ===== cadastro de cliente =====

    @Test
    void criarCliente_autenticado_retornarStatus201ComAuditoria() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clienteJson("Maria Silva", CPF_1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.cpf").value(CPF_1));

        //o cliente fica vinculado ao usuário do token e os campos de auditoria são preenchidos
        Cliente salvo = clienteRepository.findAll().getFirst();
        assertThat(salvo.getUsuario().getUsername()).isEqualTo("cliente@email.com");
        assertThat(salvo.getDataCriacao()).isNotNull();
        assertThat(salvo.getCriadoPor()).isEqualTo("cliente@email.com");
    }

    @Test
    void criarCliente_semToken_retornarStatus401() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clienteJson("Maria Silva", CPF_1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void criarCliente_comTokenInvalido_retornarStatus401() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", "Bearer token.invalido.123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clienteJson("Maria Silva", CPF_1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void criarCliente_comPerfilAdmin_retornarStatus403() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clienteJson("Admin", CPF_1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void criarCliente_comCpfRepetido_retornarStatus409() throws Exception {
        cadastrarCliente(tokenCliente, "Maria Silva", CPF_1);

        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", tokenOutroCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clienteJson("João Souza", CPF_1)))
                .andExpect(status().isConflict());
    }

    @Test
    void criarCliente_usuarioQueJaPossuiCliente_retornarStatus409() throws Exception {
        cadastrarCliente(tokenCliente, "Maria Silva", CPF_1);

        mockMvc.perform(post("/api/v1/clientes")
                        .header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clienteJson("Maria Silva", CPF_2)))
                .andExpect(status().isConflict());
    }

    //nome vazio, nome curto, cpf vazio, cpf com tamanho errado e cpf com dígito verificador errado
    @Test
    void criarCliente_comCamposInvalidos_retornarStatus422() throws Exception {
        String[][] invalidos = {
                {"", CPF_1},
                {"Ma", CPF_1},
                {"Maria Silva", ""},
                {"Maria Silva", "5299822472"},
                {"Maria Silva", "529.982.247-25"},
                {"Maria Silva", "52998224726"}
        };
        for (String[] dados : invalidos) {
            mockMvc.perform(post("/api/v1/clientes")
                            .header("Authorization", tokenCliente)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(clienteJson(dados[0], dados[1])))
                    .andExpect(status().isUnprocessableContent());
        }
    }

    // ===== consultas =====

    @Test
    void consultarDetalhes_clienteVeApenasOsProprioDados_retornarStatus200() throws Exception {
        cadastrarCliente(tokenCliente, "Maria Silva", CPF_1);
        cadastrarCliente(tokenOutroCliente, "João Souza", CPF_2);

        mockMvc.perform(get("/api/v1/clientes/detalhes").header("Authorization", tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.cpf").value(CPF_1));
    }

    @Test
    void consultarDetalhes_semClienteCadastrado_retornarStatus404() throws Exception {
        mockMvc.perform(get("/api/v1/clientes/detalhes").header("Authorization", tokenCliente))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultarPorId_comPerfilCliente_retornarStatus403() throws Exception {
        Long id = cadastrarCliente(tokenOutroCliente, "João Souza", CPF_2);

        mockMvc.perform(get("/api/v1/clientes/{id}", id).header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarTodos_comPerfilCliente_retornarStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/clientes").header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void consultarPorId_comPerfilAdmin_retornarStatus200() throws Exception {
        Long id = cadastrarCliente(tokenCliente, "Maria Silva", CPF_1);

        mockMvc.perform(get("/api/v1/clientes/{id}", id).header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"));
    }

    @Test
    void consultarPorId_inexistente_retornarStatus404() throws Exception {
        mockMvc.perform(get("/api/v1/clientes/{id}", 999999).header("Authorization", tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarTodos_comPerfilAdmin_retornarStatus200() throws Exception {
        cadastrarCliente(tokenCliente, "Maria Silva", CPF_1);
        cadastrarCliente(tokenOutroCliente, "João Souza", CPF_2);

        mockMvc.perform(get("/api/v1/clientes").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
