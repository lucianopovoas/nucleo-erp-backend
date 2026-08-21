package br.com.nucleodasreformas.nucleoerp.orcamento_versao.service;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.service.DespesaOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.service.OrcamentoService;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoStatusRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrcamentoVersaoRollbackIntegrationTest {

    @Autowired private OrcamentoService orcamentoService;
    @Autowired private OrcamentoVersaoService versaoService;
    @Autowired private DespesaOrcamentoService despesaService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private StatusOrcamentoRepository statusRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void deveReverterVersaoEClonesQuandoUmaCategoriaFalhar() {
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nome("Cliente rollback " + UUID.randomUUID()).build());
        OrcamentoRequest request = new OrcamentoRequest();
        request.setClienteId(cliente.getId());
        OrcamentoResponse orcamento = orcamentoService.salvar(request);
        Long v1 = orcamento.getVersaoAtual().getId();

        DespesaOrcamentoRequest despesa = new DespesaOrcamentoRequest();
        despesa.setDescricao("Forçar falha no clone");
        despesa.setValor(new BigDecimal("10.00"));
        despesaService.salvar(orcamento.getId(), v1, despesa);

        OrcamentoVersaoStatusRequest enviado = new OrcamentoVersaoStatusRequest();
        enviado.setStatusOrcamentoId(statusRepository.findByCodigo("ENVIADO").orElseThrow().getId());
        versaoService.alterarStatus(orcamento.getId(), v1, enviado);

        criarTriggerDeFalha();
        try {
            assertThatThrownBy(() -> versaoService.criarNovaVersao(orcamento.getId(), v1))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("falha deliberada no clone");
        } finally {
            removerTriggerDeFalha();
        }

        assertThat(versaoService.listar(orcamento.getId())).singleElement()
                .satisfies(versao -> assertThat(versao.getId()).isEqualTo(v1));
        assertThat(orcamentoService.buscarPorId(orcamento.getId()).getVersaoAtual().getId())
                .isEqualTo(v1);
    }

    private void criarTriggerDeFalha() {
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION teste_falhar_clone_despesa()
                RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    RAISE EXCEPTION 'falha deliberada no clone';
                END
                $$
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER trg_teste_falhar_clone_despesa
                BEFORE INSERT ON despesa_orcamento
                FOR EACH ROW EXECUTE FUNCTION teste_falhar_clone_despesa()
                """);
    }

    private void removerTriggerDeFalha() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_teste_falhar_clone_despesa ON despesa_orcamento");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS teste_falhar_clone_despesa()");
    }
}
