package br.edu.ifc.bikes;

import br.edu.ifc.bikes.entity.Bike;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Locacao;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.BikeRepository;
import br.edu.ifc.bikes.repository.ClienteRepository;
import br.edu.ifc.bikes.repository.LocacaoRepository;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RelatorioPdfTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private BikeRepository bikeRepository;
    @Autowired
    private LocacaoRepository locacaoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenAdmin;
    private String tokenCliente;

    @BeforeEach
    void setUp() throws Exception {
        Usuario usuarioCliente = criarUsuario("cliente@email.com", Usuario.Role.CLIENTE);
        criarUsuario("admin@email.com", Usuario.Role.ADMIN);

        Cliente cliente = new Cliente();
        cliente.setNome("Maria da Silva");
        cliente.setCpf("52998224725");
        cliente.setUsuario(usuarioCliente);
        clienteRepository.save(cliente);

        Bike bike = new Bike();
        bike.setCodigo("BIKE-001");
        bike.setModelo("Aro 29");
        bike.setMarca("Caloi");
        bike.setCor("Azul");
        bikeRepository.save(bike);

        Locacao locacao = new Locacao();
        locacao.setCliente(cliente);
        locacao.setBike(bike);
        locacao.setDataRetirada(LocalDateTime.now().minusMinutes(90));
        locacao.setDataPrevistaDevolucao(LocalDateTime.now().plusHours(1));
        locacao.setDataDevolucao(LocalDateTime.now());
        locacao.setCartaoFinal("1111");
        locacao.setCartaoTitular("Maria da Silva");
        locacao.setStatus(Locacao.Status.FINALIZADA);
        locacaoRepository.save(locacao);

        tokenAdmin = login("admin@email.com");
        tokenCliente = login("cliente@email.com");
    }

    @AfterEach
    void tearDown() {
        locacaoRepository.deleteAll();
        clienteRepository.deleteAll();
        bikeRepository.deleteAll();
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

    private void assertEhPdf(byte[] corpo) {
        assertThat(corpo).isNotEmpty();
        assertThat(new String(corpo, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void relatorioClientes_comPerfilAdmin_retornarPdf() throws Exception {
        byte[] corpo = mockMvc.perform(get("/api/v1/clientes/relatorio").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertEhPdf(corpo);
    }

    @Test
    void relatorioClientes_comPerfilCliente_retornarStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/clientes/relatorio").header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void relatorioClientes_semToken_retornarStatus401() throws Exception {
        mockMvc.perform(get("/api/v1/clientes/relatorio"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void relatorioLocacoes_comPerfilAdmin_retornarPdf() throws Exception {
        byte[] corpo = mockMvc.perform(get("/api/v1/locacoes/relatorio/pdf").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertEhPdf(corpo);
    }

    @Test
    void relatorioLocacoes_comPerfilCliente_retornarStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/locacoes/relatorio/pdf").header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void relatorios_semNenhumRegistro_aindaRetornamPdf() throws Exception {
        locacaoRepository.deleteAll();
        clienteRepository.deleteAll();

        assertEhPdf(mockMvc.perform(get("/api/v1/clientes/relatorio").header("Authorization", tokenAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        assertEhPdf(mockMvc.perform(get("/api/v1/locacoes/relatorio/pdf").header("Authorization", tokenAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
    }
}
