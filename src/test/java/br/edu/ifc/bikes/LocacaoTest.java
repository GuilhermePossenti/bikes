package br.edu.ifc.bikes;

import br.edu.ifc.bikes.entity.Bike;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.repository.BikeRepository;
import br.edu.ifc.bikes.repository.ClienteRepository;
import br.edu.ifc.bikes.repository.LocacaoRepository;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import br.edu.ifc.bikes.service.LocacaoService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LocacaoTest {

    private static final String CARTAO = "4111111111111111";

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

    private String tokenCliente;
    private String tokenOutroCliente;
    private String tokenAdmin;
    private Bike bike;

    @BeforeEach
    void setUp() throws Exception {
        tokenCliente = criarClienteComLogin("cliente@email.com", "Maria da Silva", "52998224725");
        tokenOutroCliente = criarClienteComLogin("outro@email.com", "Joao Souza", "11144477735");
        criarUsuario("admin@email.com", Usuario.Role.ADMIN);
        tokenAdmin = login("admin@email.com");
        bike = criarBike("BIKE-001", Bike.Status.DISPONIVEL);
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

    private String criarClienteComLogin(String username, String nome, String cpf) throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setCpf(cpf);
        cliente.setUsuario(criarUsuario(username, Usuario.Role.CLIENTE));
        clienteRepository.save(cliente);
        return login(username);
    }

    private Bike criarBike(String codigo, Bike.Status status) {
        Bike nova = new Bike();
        nova.setCodigo(codigo);
        nova.setModelo("Aro 29");
        nova.setMarca("Caloi");
        nova.setCor("Azul");
        nova.setStatus(status);
        return bikeRepository.save(nova);
    }

    private String login(String username) throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"username\":\"%s\",\"password\":\"123456\"}", username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    private String locacaoJson(Long bikeId, String numeroCartao) {
        return String.format("{\"bikeId\":%d,\"horasPrevistas\":2,\"numeroCartao\":\"%s\","
                + "\"nomeTitular\":\"Maria da Silva\",\"validade\":\"12/30\",\"cvv\":\"123\"}", bikeId, numeroCartao);
    }

    private Long alugar(String token, Long bikeId) throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/locacoes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locacaoJson(bikeId, CARTAO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    private Bike.Status statusDaBike() {
        return bikeRepository.findById(bike.getId()).orElseThrow().getStatus();
    }

    @Test
    void alugar_bikeDisponivel_retornarStatus201EBikeFicaAlugada() throws Exception {
        mockMvc.perform(post("/api/v1/locacoes")
                        .header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locacaoJson(bike.getId(), CARTAO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andExpect(jsonPath("$.nomeCliente").value("Maria da Silva"))
                .andExpect(jsonPath("$.codigoBike").value("BIKE-001"))
                .andExpect(jsonPath("$.dataRetirada").isNotEmpty())
                .andExpect(jsonPath("$.dataPrevistaDevolucao").isNotEmpty())
                .andExpect(jsonPath("$.dataDevolucao").isEmpty())
                .andExpect(jsonPath("$.valor").isEmpty())
                .andExpect(jsonPath("$.cartaoFinal").value("1111"))
                .andExpect(jsonPath("$.numeroCartao").doesNotExist())
                .andExpect(jsonPath("$.cvv").doesNotExist());

        assertThat(statusDaBike()).isEqualTo(Bike.Status.ALUGADA);
        assertThat(locacaoRepository.findAll()).singleElement()
                .satisfies(l -> assertThat(l.getCartaoFinal()).isEqualTo("1111"));
    }

    @Test
    void alugar_bikeJaAlugada_retornarStatus409() throws Exception {
        alugar(tokenCliente, bike.getId());

        mockMvc.perform(post("/api/v1/locacoes")
                        .header("Authorization", tokenOutroCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locacaoJson(bike.getId(), CARTAO)))
                .andExpect(status().isConflict());

        assertThat(locacaoRepository.count()).isEqualTo(1);
    }

    @Test
    void alugar_bikeEmManutencao_retornarStatus409() throws Exception {
        Bike emManutencao = criarBike("BIKE-002", Bike.Status.MANUTENCAO);

        mockMvc.perform(post("/api/v1/locacoes")
                        .header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locacaoJson(emManutencao.getId(), CARTAO)))
                .andExpect(status().isConflict());
    }

    @Test
    void alugar_bikeInexistente_retornarStatus404() throws Exception {
        mockMvc.perform(post("/api/v1/locacoes")
                        .header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locacaoJson(999999L, CARTAO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void alugar_usuarioSemClienteCadastrado_retornarStatus404() throws Exception {
        criarUsuario("semcadastro@email.com", Usuario.Role.CLIENTE);
        String token = login("semcadastro@email.com");

        mockMvc.perform(post("/api/v1/locacoes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locacaoJson(bike.getId(), CARTAO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void alugar_comCartaoInvalido_retornarStatus422() throws Exception {
        mockMvc.perform(post("/api/v1/locacoes")
                        .header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locacaoJson(bike.getId(), "4111111111111112")))
                .andExpect(status().isUnprocessableContent());

        assertThat(statusDaBike()).isEqualTo(Bike.Status.DISPONIVEL);
    }

    @Test
    void alugar_semToken_retornarStatus401() throws Exception {
        mockMvc.perform(post("/api/v1/locacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locacaoJson(bike.getId(), CARTAO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void alugar_comPerfilAdmin_retornarStatus403() throws Exception {
        mockMvc.perform(post("/api/v1/locacoes")
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locacaoJson(bike.getId(), CARTAO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void devolver_locacaoAtiva_retornarStatus200EBikeFicaDisponivel() throws Exception {
        Long id = alugar(tokenCliente, bike.getId());

        mockMvc.perform(patch("/api/v1/locacoes/{id}/devolucao", id)
                        .header("Authorization", tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.dataDevolucao").isNotEmpty())
                .andExpect(jsonPath("$.duracaoMinutos").isNumber())
                .andExpect(jsonPath("$.valor").isNumber());

        assertThat(statusDaBike()).isEqualTo(Bike.Status.DISPONIVEL);
    }

    @Test
    void devolver_comProblema_bikeVaiParaManutencao() throws Exception {
        Long id = alugar(tokenCliente, bike.getId());

        mockMvc.perform(patch("/api/v1/locacoes/{id}/devolucao", id)
                        .header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bikeComProblema\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"));

        assertThat(statusDaBike()).isEqualTo(Bike.Status.MANUTENCAO);
    }

    @Test
    void devolver_bikeDevolvidaPodeSerAlugadaDeNovo() throws Exception {
        Long id = alugar(tokenCliente, bike.getId());
        mockMvc.perform(patch("/api/v1/locacoes/{id}/devolucao", id).header("Authorization", tokenCliente))
                .andExpect(status().isOk());

        alugar(tokenOutroCliente, bike.getId());
        assertThat(locacaoRepository.count()).isEqualTo(2);
    }

    @Test
    void devolver_locacaoJaDevolvida_retornarStatus409() throws Exception {
        Long id = alugar(tokenCliente, bike.getId());
        mockMvc.perform(patch("/api/v1/locacoes/{id}/devolucao", id).header("Authorization", tokenCliente))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/locacoes/{id}/devolucao", id).header("Authorization", tokenCliente))
                .andExpect(status().isConflict());
    }

    @Test
    void devolver_locacaoDeOutroCliente_retornarStatus403() throws Exception {
        Long id = alugar(tokenCliente, bike.getId());

        mockMvc.perform(patch("/api/v1/locacoes/{id}/devolucao", id).header("Authorization", tokenOutroCliente))
                .andExpect(status().isForbidden());

        assertThat(statusDaBike()).isEqualTo(Bike.Status.ALUGADA);
    }

    @Test
    void devolver_comPerfilAdmin_retornarStatus200() throws Exception {
        Long id = alugar(tokenCliente, bike.getId());

        mockMvc.perform(patch("/api/v1/locacoes/{id}/devolucao", id).header("Authorization", tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void devolver_locacaoInexistente_retornarStatus404() throws Exception {
        mockMvc.perform(patch("/api/v1/locacoes/{id}/devolucao", 999999L).header("Authorization", tokenCliente))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultarPorId_clienteVeApenasAsProprias() throws Exception {
        Long id = alugar(tokenCliente, bike.getId());

        mockMvc.perform(get("/api/v1/locacoes/{id}", id).header("Authorization", tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mockMvc.perform(get("/api/v1/locacoes/{id}", id).header("Authorization", tokenOutroCliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/locacoes/{id}", id).header("Authorization", tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void relatorio_comPerfilAdmin_retornarTodasAsLocacoes() throws Exception {
        Long devolvida = alugar(tokenCliente, bike.getId());
        mockMvc.perform(patch("/api/v1/locacoes/{id}/devolucao", devolvida).header("Authorization", tokenCliente))
                .andExpect(status().isOk());
        Bike outraBike = criarBike("BIKE-002", Bike.Status.DISPONIVEL);
        alugar(tokenOutroCliente, outraBike.getId());

        mockMvc.perform(get("/api/v1/locacoes/relatorio").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nomeCliente").value("Joao Souza"))
                .andExpect(jsonPath("$[0].codigoBike").value("BIKE-002"))
                .andExpect(jsonPath("$[0].status").value("ATIVA"))
                .andExpect(jsonPath("$[0].dataDevolucao").isEmpty())
                .andExpect(jsonPath("$[0].duracaoMinutos").isNumber())
                .andExpect(jsonPath("$[1].nomeCliente").value("Maria da Silva"))
                .andExpect(jsonPath("$[1].codigoBike").value("BIKE-001"))
                .andExpect(jsonPath("$[1].status").value("FINALIZADA"))
                .andExpect(jsonPath("$[1].dataRetirada").isNotEmpty())
                .andExpect(jsonPath("$[1].dataDevolucao").isNotEmpty());
    }

    @Test
    void relatorio_comPerfilCliente_retornarStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/locacoes/relatorio").header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void buscarBikePorCodigo_comPerfilAdmin_retornarStatus200() throws Exception {
        mockMvc.perform(get("/api/v1/bikes/codigo/{codigo}", "BIKE-001").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bike.getId()))
                .andExpect(jsonPath("$.codigo").value("BIKE-001"));
    }

    @Test
    void buscarBikePorCodigo_inexistente_retornarStatus404() throws Exception {
        mockMvc.perform(get("/api/v1/bikes/codigo/{codigo}", "NAO-EXISTE").header("Authorization", tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscarBikePorCodigo_comPerfilCliente_retornarStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/bikes/codigo/{codigo}", "BIKE-001").header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void calcularValor_proporcionalAoTempoEfetivo() {
        LocalDateTime retirada = LocalDateTime.of(2026, 1, 1, 10, 0);
        BigDecimal preco = new BigDecimal("10.00");

        assertThat(LocacaoService.calcularValor(retirada, retirada.plusHours(1), preco)).isEqualByComparingTo("10.00");
        assertThat(LocacaoService.calcularValor(retirada, retirada.plusMinutes(90), preco)).isEqualByComparingTo("15.00");
        assertThat(LocacaoService.calcularValor(retirada, retirada.plusMinutes(30), preco)).isEqualByComparingTo("5.00");
        assertThat(LocacaoService.calcularValor(retirada, retirada.plusSeconds(61), preco)).isEqualByComparingTo("0.33");
        assertThat(LocacaoService.calcularValor(retirada, retirada, preco)).isEqualByComparingTo("0.17");
    }
}
