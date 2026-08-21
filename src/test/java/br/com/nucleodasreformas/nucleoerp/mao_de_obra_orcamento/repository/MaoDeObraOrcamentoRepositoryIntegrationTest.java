package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.entity.MaoDeObraOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.repository.UnidadeMaoDeObraRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
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
class MaoDeObraOrcamentoRepositoryIntegrationTest {

    @Autowired
    private MaoDeObraOrcamentoRepository repository;

    @Autowired
    private OrcamentoRepository orcamentoRepository;

    @Autowired
    private UnidadeMaoDeObraRepository unidadeMaoDeObraRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private StatusOrcamentoRepository statusOrcamentoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void devePersistirComSnapshotPrecisaoEAuditoria() {
        Referencias referencias = salvarReferencias();

        MaoDeObraOrcamento salvo = repository.saveAndFlush(registro(
                referencias.orcamento(),
                referencias.unidade(),
                "Instalação de toldo",
                "Diária",
                "2.0000",
                "250.00",
                "500.00"));

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getDescricao()).isEqualTo("Instalação de toldo");
        assertThat(salvo.getUnidade()).isEqualTo("Diária");
        assertThat(salvo.getQuantidade()).isEqualByComparingTo("2.0000");
        assertThat(salvo.getCustoUnitario()).isEqualByComparingTo("250.00");
        assertThat(salvo.getCustoTotal()).isEqualByComparingTo("500.00");
        assertThat(salvo.getCriadoEm()).isNotNull();
    }

    @Test
    void deveAplicarDefaultDeCriacaoDoPostgreSql() {
        Referencias referencias = salvarReferencias();

        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO mao_de_obra_orcamento (
                    orcamento_id, unidade_mao_de_obra_id, descricao, unidade,
                    quantidade, custo_unitario, custo_total
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class,
                referencias.orcamento().getId(),
                referencias.unidade().getId(),
                "Defaults",
                "Hora",
                new BigDecimal("1.0000"),
                new BigDecimal("10.00"),
                new BigDecimal("10.00"));

        LocalDateTime criadoEm = jdbcTemplate.queryForObject(
                "SELECT criado_em FROM mao_de_obra_orcamento WHERE id = ?",
                LocalDateTime.class,
                id);

        assertThat(criadoEm).isNotNull();
    }

    @Test
    void devePermitirMesmaUnidadeMaisDeUmaVezNoOrcamento() {
        Referencias referencias = salvarReferencias();

        MaoDeObraOrcamento primeira = repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.unidade(),
                "Instalação principal", "Diária", "1", "100", "100"));
        MaoDeObraOrcamento segunda = repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.unidade(),
                "Instalação complementar", "Diária", "2", "100", "200"));

        assertThat(primeira.getId()).isNotEqualTo(segunda.getId());
        assertThat(repository.findByOrcamento_IdOrderByIdAsc(
                referencias.orcamento().getId()))
                .extracting(MaoDeObraOrcamento::getId)
                .contains(primeira.getId(), segunda.getId());
    }

    @Test
    void deveSomarCustosTotaisAgrupadosPorOrcamento() {
        UnidadeMaoDeObra unidade = salvarUnidade();
        Orcamento primeiro = salvarOrcamento();
        Orcamento segundo = salvarOrcamento();
        Orcamento semLinhas = salvarOrcamento();
        repository.saveAllAndFlush(List.of(
                registro(primeiro, unidade, "Primeira", "Hora", "1", "10", "100.25"),
                registro(primeiro, unidade, "Segunda", "Hora", "1", "10", "49.75"),
                registro(segundo, unidade, "Custo zero", "Hora", "1", "0", "0.00")));

        List<CustoTotalMaoDeObraOrcamentoProjection> totais = repository
                .somarCustoTotalPorOrcamentos(List.of(
                        primeiro.getId(), segundo.getId(), semLinhas.getId()));

        assertThat(totais).anySatisfy(total -> {
            assertThat(total.orcamentoId()).isEqualTo(primeiro.getId());
            assertThat(total.custoTotalMaoDeObra()).isEqualByComparingTo("150.00");
        });
        assertThat(totais).anySatisfy(total -> {
            assertThat(total.orcamentoId()).isEqualTo(segundo.getId());
            assertThat(total.custoTotalMaoDeObra()).isEqualByComparingTo("0.00");
        });
        assertThat(totais).noneMatch(total -> total.orcamentoId().equals(semLinhas.getId()));
    }

    @Test
    void deveDelimitarBuscaPeloOrcamento() {
        Referencias referencias = salvarReferencias();
        Orcamento outroOrcamento = salvarOrcamento();
        MaoDeObraOrcamento salvo = repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.unidade(),
                "Instalação", "Diária", "1", "100", "100"));

        assertThat(repository.findByIdAndOrcamento_Id(
                salvo.getId(), referencias.orcamento().getId())).isPresent();
        assertThat(repository.findByIdAndOrcamento_Id(
                salvo.getId(), outroOrcamento.getId())).isEmpty();
    }

    @Test
    void deveRejeitarQuantidadeNaoPositivaNoPostgreSql() {
        Referencias referencias = salvarReferencias();

        assertThatThrownBy(() -> repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.unidade(),
                "Inválido", "Hora", "0", "10", "0")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarCustoUnitarioNegativoNoPostgreSql() {
        Referencias referencias = salvarReferencias();

        assertThatThrownBy(() -> repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.unidade(),
                "Inválido", "Hora", "1", "-0.01", "0")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarCustoTotalNegativoNoPostgreSql() {
        Referencias referencias = salvarReferencias();

        assertThatThrownBy(() -> repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.unidade(),
                "Inválido", "Hora", "1", "10", "-0.01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarOrcamentoInexistentePelaForeignKey() {
        UnidadeMaoDeObra unidade = salvarUnidade();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO mao_de_obra_orcamento (
                    orcamento_id, unidade_mao_de_obra_id, descricao, unidade,
                    quantidade, custo_unitario, custo_total
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, Long.MAX_VALUE, unidade.getId(), "FK", "Hora",
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarUnidadeInexistentePelaForeignKey() {
        Orcamento orcamento = salvarOrcamento();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO mao_de_obra_orcamento (
                    orcamento_id, unidade_mao_de_obra_id, descricao, unidade,
                    quantidade, custo_unitario, custo_total
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, orcamento.getId(), Long.MAX_VALUE, "FK", "Hora",
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveCarregarUnidadeNasConsultas() {
        Referencias referencias = salvarReferencias();
        MaoDeObraOrcamento salvo = repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.unidade(),
                "Carregamento", "Hora", "1", "10", "10"));
        entityManager.clear();
        PersistenceUnitUtil persistence = entityManagerFactory.getPersistenceUnitUtil();

        MaoDeObraOrcamento porId = repository.findByIdAndOrcamento_Id(
                salvo.getId(), referencias.orcamento().getId()).orElseThrow();
        List<MaoDeObraOrcamento> listados = repository
                .findByOrcamento_IdOrderByIdAsc(referencias.orcamento().getId());

        assertThat(persistence.isLoaded(porId.getUnidadeMaoDeObra())).isTrue();
        assertThat(listados).allMatch(
                item -> persistence.isLoaded(item.getUnidadeMaoDeObra()));
    }

    @Test
    void devePreservarSnapshotQuandoNomeDaUnidadeMudar() {
        Referencias referencias = salvarReferencias();
        String snapshot = referencias.unidade().getNome();
        MaoDeObraOrcamento salvo = repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.unidade(),
                "Instalação", snapshot, "1", "10", "10"));
        referencias.unidade().setNome("Nome atual " + UUID.randomUUID());
        unidadeMaoDeObraRepository.saveAndFlush(referencias.unidade());
        entityManager.clear();

        MaoDeObraOrcamento preservado = repository.findByIdAndOrcamento_Id(
                salvo.getId(), referencias.orcamento().getId()).orElseThrow();

        assertThat(preservado.getUnidade()).isEqualTo(snapshot);
        assertThat(preservado.getUnidadeMaoDeObra().getNome()).isNotEqualTo(snapshot);
    }

    @Test
    void deveExcluirFisicamenteSomenteLinha() {
        Referencias referencias = salvarReferencias();
        MaoDeObraOrcamento salvo = repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.unidade(),
                "Remover", "Hora", "1", "10", "10"));

        repository.delete(salvo);
        repository.flush();

        assertThat(repository.existsById(salvo.getId())).isFalse();
        assertThat(orcamentoRepository.existsById(referencias.orcamento().getId())).isTrue();
        assertThat(unidadeMaoDeObraRepository.existsById(
                referencias.unidade().getId())).isTrue();
    }

    @Test
    void devePossuirSchemaPrecisaoChecksIndiceESemAtivoOuUnicidade() {
        List<Map<String, Object>> colunas = jdbcTemplate.queryForList("""
                SELECT column_name, data_type, character_maximum_length,
                       numeric_precision, numeric_scale, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'mao_de_obra_orcamento'
                """);
        List<String> checks = jdbcTemplate.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'mao_de_obra_orcamento'
                  AND constraint_type = 'CHECK'
                  AND constraint_name LIKE 'ck_mao_de_obra_orcamento_%'
                """, String.class);
        Integer ativo = contarColuna("ativo");
        Integer unicidade = contarConstraint("UNIQUE");
        Integer indice = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'mao_de_obra_orcamento'
                  AND indexname = 'idx_mao_de_obra_orcamento_orcamento_id'
                """, Integer.class);

        assertThat(colunas).filteredOn(c -> c.get("column_name").equals("descricao"))
                .singleElement().satisfies(c ->
                        assertThat(c.get("character_maximum_length")).isEqualTo(200));
        assertThat(colunas).filteredOn(c -> c.get("column_name").equals("unidade"))
                .singleElement().satisfies(c ->
                        assertThat(c.get("character_maximum_length")).isEqualTo(100));
        assertThat(colunas).filteredOn(c -> c.get("column_name").equals("quantidade"))
                .singleElement().satisfies(c -> {
                    assertThat(c.get("numeric_precision")).isEqualTo(15);
                    assertThat(c.get("numeric_scale")).isEqualTo(4);
                });
        assertThat(colunas).filteredOn(c -> c.get("column_name").equals("custo_unitario"))
                .singleElement().satisfies(c -> {
                    assertThat(c.get("numeric_precision")).isEqualTo(15);
                    assertThat(c.get("numeric_scale")).isEqualTo(2);
                });
        assertThat(colunas).filteredOn(c -> c.get("column_name").equals("custo_total"))
                .singleElement().satisfies(c -> {
                    assertThat(c.get("numeric_precision")).isEqualTo(15);
                    assertThat(c.get("numeric_scale")).isEqualTo(2);
                });
        assertThat(checks).containsExactlyInAnyOrder(
                "ck_mao_de_obra_orcamento_quantidade_positiva",
                "ck_mao_de_obra_orcamento_custo_unitario_nao_negativo",
                "ck_mao_de_obra_orcamento_custo_total_nao_negativo");
        assertThat(ativo).isZero();
        assertThat(unicidade).isZero();
        assertThat(indice).isEqualTo(1);
    }

    @Test
    void devePossuirForeignKeysSemCascadeDelete() {
        List<String> regras = jdbcTemplate.queryForList("""
                SELECT rc.delete_rule
                FROM information_schema.referential_constraints rc
                JOIN information_schema.table_constraints tc
                  ON tc.constraint_schema = rc.constraint_schema
                 AND tc.constraint_name = rc.constraint_name
                WHERE tc.table_schema = 'public'
                  AND tc.table_name = 'mao_de_obra_orcamento'
                  AND tc.constraint_name IN (
                      'fk_mao_de_obra_orcamento_orcamento',
                      'fk_mao_de_obra_orcamento_unidade_mao_de_obra'
                  )
                ORDER BY tc.constraint_name
                """, String.class);

        assertThat(regras).containsExactly("NO ACTION", "NO ACTION");
    }

    private Integer contarColuna(String coluna) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'mao_de_obra_orcamento'
                  AND column_name = ?
                """, Integer.class, coluna);
    }

    private Integer contarConstraint(String tipo) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'mao_de_obra_orcamento'
                  AND constraint_type = ?
                """, Integer.class, tipo);
    }

    private Referencias salvarReferencias() {
        return new Referencias(salvarOrcamento(), salvarUnidade());
    }

    private Orcamento salvarOrcamento() {
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nome("Cliente mão de obra " + UUID.randomUUID())
                .build());
        StatusOrcamento rascunho = statusOrcamentoRepository
                .findByNomeNormalizado("Rascunho")
                .orElseThrow();
        return orcamentoRepository.saveAndFlush(Orcamento.builder()
                .cliente(cliente)
                .statusOrcamento(rascunho)
                .build());
    }

    private UnidadeMaoDeObra salvarUnidade() {
        return unidadeMaoDeObraRepository.saveAndFlush(UnidadeMaoDeObra.builder()
                .nome("Unidade mão de obra " + UUID.randomUUID())
                .build());
    }

    private MaoDeObraOrcamento registro(
            Orcamento orcamento,
            UnidadeMaoDeObra unidade,
            String descricao,
            String snapshotUnidade,
            String quantidade,
            String custoUnitario,
            String custoTotal) {
        return MaoDeObraOrcamento.builder()
                .orcamento(orcamento)
                .unidadeMaoDeObra(unidade)
                .descricao(descricao)
                .unidade(snapshotUnidade)
                .quantidade(new BigDecimal(quantidade))
                .custoUnitario(new BigDecimal(custoUnitario))
                .custoTotal(new BigDecimal(custoTotal))
                .build();
    }

    private record Referencias(Orcamento orcamento, UnidadeMaoDeObra unidade) {
    }
}
