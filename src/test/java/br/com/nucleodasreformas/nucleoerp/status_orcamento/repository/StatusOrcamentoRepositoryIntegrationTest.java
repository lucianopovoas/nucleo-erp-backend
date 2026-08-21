package br.com.nucleodasreformas.nucleoerp.status_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class StatusOrcamentoRepositoryIntegrationTest {

    private static final List<String> STATUS_INICIAIS = List.of(
            "Rascunho", "Enviado", "Aprovado", "Recusado", "Cancelado");

    @Autowired
    private StatusOrcamentoRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveAplicarSchemaEDefaultsDoPostgreSql() {
        String nome = "Status defaults " + UUID.randomUUID();
        String codigo = novoCodigo();

        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO status_orcamento (codigo, nome) VALUES (?, ?) RETURNING id",
                Long.class,
                codigo,
                nome);

        Boolean ativo = jdbcTemplate.queryForObject(
                "SELECT ativo FROM status_orcamento WHERE id = ?", Boolean.class, id);
        LocalDateTime criadoEm = jdbcTemplate.queryForObject(
                "SELECT criado_em FROM status_orcamento WHERE id = ?", LocalDateTime.class, id);
        Integer tamanhoNome = jdbcTemplate.queryForObject("""
                SELECT character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'status_orcamento'
                  AND column_name = 'nome'
                """, Integer.class);

        assertThat(ativo).isTrue();
        assertThat(criadoEm).isNotNull();
        assertThat(tamanhoNome).isEqualTo(100);
    }

    @Test
    void deveConterOsCincoStatusIniciaisAtivos() {
        List<String> nomes = jdbcTemplate.queryForList("""
                SELECT nome
                FROM status_orcamento
                WHERE nome IN ('Rascunho', 'Enviado', 'Aprovado', 'Recusado', 'Cancelado')
                  AND ativo = TRUE
                """, String.class);

        assertThat(nomes).containsExactlyInAnyOrderElementsOf(STATUS_INICIAIS);
        assertThat(repository.findByCodigo("RASCUNHO")).isPresent();
    }

    @Test
    void deveListarSomenteStatusAtivos() {
        String sufixo = UUID.randomUUID().toString();
        StatusOrcamento ativo = repository.saveAndFlush(StatusOrcamento.builder()
                .codigo(novoCodigo())
                .nome("Ativo " + sufixo)
                .build());
        repository.saveAndFlush(StatusOrcamento.builder()
                .codigo(novoCodigo())
                .nome("Inativo " + sufixo)
                .ativo(false)
                .build());

        List<StatusOrcamento> status = repository.findByAtivoTrue();

        assertThat(status).extracting(StatusOrcamento::getId).contains(ativo.getId());
        assertThat(status).noneMatch(item -> Boolean.FALSE.equals(item.getAtivo()));
    }

    @Test
    void deveConsultarNomeNormalizadoEExcluirOProprioIdNaAtualizacao() {
        StatusOrcamento status = repository.saveAndFlush(StatusOrcamento.builder()
                .codigo(novoCodigo())
                .nome("Em análise " + UUID.randomUUID())
                .build());

        assertThat(repository.existsByNomeNormalizado("  " + status.getNome().toUpperCase() + "  ")).isTrue();
        assertThat(repository.existsByNomeNormalizadoAndIdNot(status.getNome(), status.getId())).isFalse();
        assertThat(repository.existsByNomeNormalizadoAndIdNot(status.getNome(), status.getId() + 1)).isTrue();
    }

    @Test
    void deveGarantirUnicidadeIgnorandoMaiusculasEMinusculas() {
        String sufixo = UUID.randomUUID().toString();
        jdbcTemplate.update("INSERT INTO status_orcamento (codigo, nome) VALUES (?, ?)",
                novoCodigo(), "Em análise " + sufixo);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO status_orcamento (codigo, nome) VALUES (?, ?)",
                novoCodigo(), "eM ANÁLISE " + sufixo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveGarantirUnicidadeIgnorandoEspacosExternosMesmoParaRegistroInativo() {
        String nome = "Aguardando " + UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO status_orcamento (codigo, nome, ativo) VALUES (?, ?, FALSE)",
                novoCodigo(), "  " + nome + "  ");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO status_orcamento (codigo, nome) VALUES (?, ?)", novoCodigo(), nome))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void naoDeveNormalizarAcentosNaUnicidade() {
        String sufixo = UUID.randomUUID().toString();
        jdbcTemplate.update("INSERT INTO status_orcamento (codigo, nome) VALUES (?, ?)",
                novoCodigo(), "Analise " + sufixo);

        int inseridos = jdbcTemplate.update(
                "INSERT INTO status_orcamento (codigo, nome) VALUES (?, ?)",
                novoCodigo(), "Análise " + sufixo);

        assertThat(inseridos).isEqualTo(1);
    }

    @Test
    void devePossuirIndiceUnicoNormalizadoEsperado() {
        Integer quantidade = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'status_orcamento'
                  AND indexname = 'uk_status_orcamento_nome_normalizado'
                  AND indexdef ILIKE 'CREATE UNIQUE INDEX%'
                """, Integer.class);

        assertThat(quantidade).isEqualTo(1);
    }

    private String novoCodigo() {
        return "STATUS_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
