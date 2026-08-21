package br.com.nucleodasreformas.nucleoerp.orcamento_versao.repository;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.service.OrcamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrcamentoVersaoConstraintIntegrationTest {

    @Autowired private OrcamentoService orcamentoService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void deveProtegerNumeroOwnershipEAprovacaoUnicaNoPostgreSql() {
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nome("Cliente constraints " + UUID.randomUUID()).build());
        OrcamentoRequest request = new OrcamentoRequest();
        request.setClienteId(cliente.getId());
        OrcamentoResponse orcamento = orcamentoService.salvar(request);

        Long aprovadoId = jdbcTemplate.queryForObject(
                "SELECT id FROM status_orcamento WHERE codigo='APROVADO'", Long.class);
        jdbcTemplate.update(
                "UPDATE orcamento_versao SET status_orcamento_id=? WHERE id=?",
                aprovadoId, orcamento.getVersaoAtual().getId());

        Integer constraints = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes
                WHERE schemaname='public'
                  AND indexname='uk_orcamento_versao_aprovada_por_orcamento'
                """, Integer.class);
        assertThat(constraints).isEqualTo(1);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO orcamento_versao
                    (orcamento_id,numero_versao,status_orcamento_id)
                VALUES (?,?,?)
                """, orcamento.getId(), 2, aprovadoId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
