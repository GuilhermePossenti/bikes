package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.dto.LocacaoRelatorioDTO;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JasperService {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final Map<String, JasperReport> modelos = new ConcurrentHashMap<>();
    private volatile Image logo;

    public byte[] gerarRelatorioClientes(List<ClienteResponseDTO> clientes) {
        List<Map<String, ?>> linhas = clientes.stream()
                .<Map<String, ?>>map(c -> Map.of(
                        "id", String.valueOf(c.id()),
                        "nome", c.nome(),
                        "cpf", formatarCpf(c.cpf())))
                .toList();
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TOTAL_CLIENTES", String.valueOf(clientes.size()));
        return gerarPdf("clientes", linhas, parametros);
    }

    public byte[] gerarRelatorioLocacoes(List<LocacaoRelatorioDTO> locacoes) {
        List<Map<String, ?>> linhas = locacoes.stream()
                .<Map<String, ?>>map(l -> Map.of(
                        "nomeCliente", l.nomeCliente(),
                        "codigoBike", l.codigoBike(),
                        "dataRetirada", l.dataRetirada().format(DATA_HORA),
                        "dataDevolucao", l.dataDevolucao() == null ? "-" : l.dataDevolucao().format(DATA_HORA),
                        "duracao", formatarDuracao(l.duracaoMinutos()),
                        "valor", l.valor() == null ? "-" : formatarMoeda(l.valor()),
                        "status", l.status()))
                .toList();

        long ativas = locacoes.stream().filter(l -> "ATIVA".equals(l.status())).count();
        BigDecimal faturado = locacoes.stream()
                .map(LocacaoRelatorioDTO::valor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TOTAL_LOCACOES", String.valueOf(locacoes.size()));
        parametros.put("TOTAL_ATIVAS", String.valueOf(ativas));
        parametros.put("TOTAL_FINALIZADAS", String.valueOf(locacoes.size() - ativas));
        parametros.put("TOTAL_FATURADO", formatarMoeda(faturado));
        return gerarPdf("locacoes", linhas, parametros);
    }

    private byte[] gerarPdf(String modelo, List<Map<String, ?>> linhas, Map<String, Object> parametros) {
        try {
            parametros.put("GERADO_EM", LocalDateTime.now().format(DATA_HORA));
            parametros.put("LOGO", carregarLogo());
            JasperPrint print = JasperFillManager.fillReport(
                    carregarModelo(modelo), parametros, new JRMapCollectionDataSource(linhas));
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException e) {
            throw new IllegalStateException("Erro ao gerar o relatório '" + modelo + "'", e);
        }
    }

    private JasperReport carregarModelo(String modelo) {
        return modelos.computeIfAbsent(modelo, nome -> {
            try (InputStream in = new ClassPathResource("reports/" + nome + ".jrxml").getInputStream()) {
                return JasperCompileManager.compileReport(in);
            } catch (IOException | JRException e) {
                throw new IllegalStateException("Erro ao compilar o modelo do relatório '" + nome + "'", e);
            }
        });
    }

    private Image carregarLogo() {
        if (logo == null) {
            try (InputStream in = new ClassPathResource("reports/images/logo.png").getInputStream()) {
                logo = ImageIO.read(in);
            } catch (IOException e) {
                throw new IllegalStateException("Erro ao carregar a logo do relatório", e);
            }
        }
        return logo;
    }

    static String formatarMoeda(BigDecimal valor) {
        return "R$ " + String.format(PT_BR, "%,.2f", valor);
    }

    static String formatarCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    static String formatarDuracao(Long minutos) {
        long total = minutos == null ? 0 : minutos;
        return total >= 60 ? (total / 60) + "h " + String.format("%02d", total % 60) + "min" : total + "min";
    }
}
