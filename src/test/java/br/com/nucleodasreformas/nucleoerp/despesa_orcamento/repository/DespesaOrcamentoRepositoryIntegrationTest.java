package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.entity.DespesaOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class DespesaOrcamentoRepositoryIntegrationTest {

    @Autowired
    private DespesaOrcamentoRepository repository;

    @Autowired
    private OrcamentoRepository orcamentoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private StatusOrcamentoRepository statusOrcamentoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveAplicarMigrationV14EPersistirDespesa() {
        Orcamento orcamento = salvarOrcamento();

        DespesaOrcamento salvo = repository.saveAndFlush(registro(
                orcamento, "Frete", "180.00"));
        Integer migrationAplicada = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version = '14' AND success = TRUE
                """, Integer.class);

        assertThat(migrationAplicada).isEqualTo(1);
        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getDescricao()).isEqualTo("Frete");
        assertThat(salvo.getValor()).isEqualByComparingTo("180.00");
        assertThat(salvo.getCriadoEm()).isNotNull();
    }

    @Test
    void deveAplicarDefaultDeCriacaoDoPostgreSql() {
        Orcamento orcamento = salvarOrcamento();

        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO despesa_orcamento (orcamento_id, descricao, valor)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, orcamento.getId(), "Pedágio", new BigDecimal("25.00"));
        LocalDateTime criadoEm = jdbcTemplate.queryForObject(
                "SELECT criado_em FROM despesa_orcamento WHERE id = ?",
                LocalDateTime.class,
                id);

        assertThat(criadoEm).isNotNull();
    }

    @Test
    void devePermitirValorZero() {
        DespesaOrcamento salvo = repository.saveAndFlush(registro(
                salvarOrcamento(), "Cortesia", "0.00"));

        assertThat(salvo.getValor()).isEqualByComparingTo("0.00");
    }

    @Test
    void devePermitirDescricoesRepetidasNoMesmoOrcamento() {
        Orcamento orcamento = salvarOrcamento();

        DespesaOrcamento primeira = repository.saveAndFlush(
                registro(orcamento, "Frete", "100.00"));
        DespesaOrcamento segunda = repository.saveAndFlush(
                registro(orcamento, "Frete", "50.00"));

        assertThat(primeira.getId()).isNotEqualTo(segunda.getId());
        assertThat(repository.findByOrcamento_IdOrderByIdAsc(orcamento.getId()))
                .extracting(DespesaOrcamento::getId)
                .containsExactly(primeira.getId(), segunda.getId());
    }

    @Test
    void deveSomarValorDeUmaOuVariasDespesasDoOrcamento() {
        Orcamento orcamento = salvarOrcamento();
        repository.saveAndFlush(registro(orcamento, "Frete", "180.00"));

        assertThat(repository.somarValorPorOrcamento(orcamento.getId()))
                .isEqualByComparingTo("180.00");

        repository.saveAndFlush(registro(orcamento, "Pedágio", "60.00"));
        assertThat(repository.somarValorPorOrcamento(orcamento.getId()))
                .isEqualByComparingTo("240.00");
    }

    @Test
    void deveSomarValorZeroERetornarNullQuandoNaoHouverDespesas() {
        Orcamento comCustoZero = salvarOrcamento();
        Orcamento semDespesas = salvarOrcamento();
        repository.saveAndFlush(registro(comCustoZero, "Cortesia", "0.00"));

        assertThat(repository.somarValorPorOrcamento(comCustoZero.getId()))
                .isEqualByComparingTo("0.00");
        assertThat(repository.somarValorPorOrcamento(semDespesas.getId())).isNull();
    }

    @Test
    void deveSomarValoresEmLoteSemMisturarOrcamentos() {
        Orcamento primeiro = salvarOrcamento();
        Orcamento segundo = salvarOrcamento();
        Orcamento semDespesas = salvarOrcamento();
        repository.saveAllAndFlush(List.of(
                registro(primeiro, "Frete", "180.00"),
                registro(primeiro, "Pedágio", "60.00"),
                registro(segundo, "Custo zero", "0.00")));

        List<CustoTotalDespesasOrcamentoProjection> totais = repository
                .somarValorPorOrcamentos(List.of(
                        primeiro.getId(), segundo.getId(), semDespesas.getId()));

        assertThat(totais).anySatisfy(total -> {
            assertThat(total.orcamentoId()).isEqualTo(primeiro.getId());
            assertThat(total.custoTotalDespesas()).isEqualByComparingTo("240.00");
        });
        assertThat(totais).anySatisfy(total -> {
            assertThat(total.orcamentoId()).isEqualTo(segundo.getId());
            assertThat(total.custoTotalDespesas()).isEqualByComparingTo("0.00");
        });
        assertThat(totais).noneMatch(total ->
                total.orcamentoId().equals(semDespesas.getId()));
    }

    @Test
    void deveDelimitarBuscaPeloOrcamento() {
        Orcamento orcamento = salvarOrcamento();
        Orcamento outroOrcamento = salvarOrcamento();
        DespesaOrcamento salvo = repository.saveAndFlush(
                registro(orcamento, "Frete", "180.00"));

        assertThat(repository.findByIdAndOrcamento_Id(
                salvo.getId(), orcamento.getId())).isPresent();
        assertThat(repository.findByIdAndOrcamento_Id(
                salvo.getId(), outroOrcamento.getId())).isEmpty();
    }

    @Test
    void deveRejeitarValorNegativoNoPostgreSql() {
        assertThatThrownBy(() -> repository.saveAndFlush(registro(
                salvarOrcamento(), "Inválida", "-0.01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarOrcamentoInexistentePelaForeignKey() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO despesa_orcamento (orcamento_id, descricao, valor)
                VALUES (?, ?, ?)
                """, Long.MAX_VALUE, "FK", BigDecimal.TEN))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveExcluirFisicamenteSomenteDespesa() {
        Orcamento orcamento = salvarOrcamento();
        DespesaOrcamento salvo = repository.saveAndFlush(
                registro(orcamento, "Remover", "10.00"));

        repository.delete(salvo);
        repository.flush();

        assertThat(repository.existsById(salvo.getId())).isFalse();
        assertThat(orcamentoRepository.existsById(orcamento.getId())).isTrue();
    }

    @Test
    void devePossuirSchemaPrecisaoCheckIndiceESemAtivoOuUnicidade() {
        List<Map<String, Object>> colunas = jdbcTemplate.queryForList("""
                SELECT column_name, data_type, character_maximum_length,
                       numeric_precision, numeric_scale, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'despesa_orcamento'
                """);
        List<String> checks = jdbcTemplate.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'despesa_orcamento'
                  AND constraint_type = 'CHECK'
                  AND constraint_name LIKE 'ck_despesa_orcamento_%'
                """, String.class);
        Integer ativo = contarColuna("ativo");
        Integer unicidade = contarConstraint("UNIQUE");
        Integer indice = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'despesa_orcamento'
                  AND indexname = 'idx_despesa_orcamento_orcamento_id'
                """, Integer.class);

        assertThat(colunas).filteredOn(c -> c.get("column_name").equals("descricao"))
                .singleElement().satisfies(c -> {
                    assertThat(c.get("character_maximum_length")).isEqualTo(200);
                    assertThat(c.get("is_nullable")).isEqualTo("NO");
                });
        assertThat(colunas).filteredOn(c -> c.get("column_name").equals("valor"))
                .singleElement().satisfies(c -> {
                    assertThat(c.get("numeric_precision")).isEqualTo(15);
                    assertThat(c.get("numeric_scale")).isEqualTo(2);
                    assertThat(c.get("is_nullable")).isEqualTo("NO");
                });
        assertThat(colunas).filteredOn(c -> c.get("column_name").equals("criado_em"))
                .singleElement().satisfies(c -> {
                    assertThat(c.get("is_nullable")).isEqualTo("NO");
                    assertThat(c.get("column_default")).isNotNull();
                });
        assertThat(checks).containsExactly("ck_despesa_orcamento_valor_nao_negativo");
        assertThat(ativo).isZero();
        assertThat(unicidade).isZero();
        assertThat(indice).isEqualTo(1);
    }

    @Test
    void devePossuirForeignKeySemCascadeDelete() {
        String regra = jdbcTemplate.queryForObject("""
                SELECT rc.delete_rule
                FROM information_schema.referential_constraints rc
                JOIN information_schema.table_constraints tc
                  ON tc.constraint_schema = rc.constraint_schema
                 AND tc.constraint_name = rc.constraint_name
                WHERE tc.table_schema = 'public'
                  AND tc.table_name = 'despesa_orcamento'
                  AND tc.constraint_name = 'fk_despesa_orcamento_orcamento'
                """, String.class);

        assertThat(regra).isEqualTo("NO ACTION");
    }

    private Integer contarColuna(String coluna) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'despesa_orcamento'
                  AND column_name = ?
                """, Integer.class, coluna);
    }

    private Integer contarConstraint(String tipo) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'despesa_orcamento'
                  AND constraint_type = ?
                """, Integer.class, tipo);
    }

    private Orcamento salvarOrcamento() {
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nome("Cliente despesa " + UUID.randomUUID())
                .build());
        StatusOrcamento rascunho = statusOrcamentoRepository
                .findByNomeNormalizado("Rascunho")
                .orElseThrow();
        return orcamentoRepository.saveAndFlush(Orcamento.builder()
                .cliente(cliente)
                .statusOrcamento(rascunho)
                .build());
    }

    private DespesaOrcamento registro(
            Orcamento orcamento,
            String descricao,
            String valor) {
        return DespesaOrcamento.builder()
                .orcamento(orcamento)
                .descricao(descricao)
                .valor(new BigDecimal(valor))
                .build();
    }
}
