package br.com.nucleodasreformas.nucleoerp.material_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.entity.MaterialOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
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
class MaterialOrcamentoRepositoryIntegrationTest {

    @Autowired
    private MaterialOrcamentoRepository repository;

    @Autowired
    private OrcamentoRepository orcamentoRepository;

    @Autowired
    private MaterialRepository materialRepository;

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
    void devePersistirComSnapshotsPrecisaoEAuditoria() {
        Referencias referencias = salvarReferencias();

        MaterialOrcamento salvo = repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.material(), "Lona negociada", "M2",
                "2.5000", "75.00", "187.50"));

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getDescricao()).isEqualTo("Lona negociada");
        assertThat(salvo.getUnidade()).isEqualTo("M2");
        assertThat(salvo.getQuantidade()).isEqualByComparingTo("2.5000");
        assertThat(salvo.getCustoUnitario()).isEqualByComparingTo("75.00");
        assertThat(salvo.getCustoTotal()).isEqualByComparingTo("187.50");
        assertThat(salvo.getCriadoEm()).isNotNull();
    }

    @Test
    void deveAplicarDefaultDeCriacaoDoPostgreSql() {
        Referencias referencias = salvarReferencias();

        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO material_orcamento (
                    orcamento_id, material_id, descricao, unidade,
                    quantidade, custo_unitario, custo_total
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class,
                referencias.orcamento().getId(), referencias.material().getId(),
                "Defaults", "UN", new BigDecimal("1.0000"),
                new BigDecimal("10.00"), new BigDecimal("10.00"));

        LocalDateTime criadoEm = jdbcTemplate.queryForObject(
                "SELECT criado_em FROM material_orcamento WHERE id = ?",
                LocalDateTime.class, id);

        assertThat(criadoEm).isNotNull();
    }

    @Test
    void devePermitirMesmoMaterialMaisDeUmaVezNoOrcamento() {
        Referencias referencias = salvarReferencias();

        MaterialOrcamento primeiro = repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.material(), "Área frontal", "M2",
                "1", "100", "100"));
        MaterialOrcamento segundo = repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.material(), "Área lateral", "M2",
                "2", "100", "200"));

        assertThat(primeiro.getId()).isNotEqualTo(segundo.getId());
        assertThat(repository.findByOrcamento_IdOrderByIdAsc(referencias.orcamento().getId()))
                .extracting(MaterialOrcamento::getId)
                .contains(primeiro.getId(), segundo.getId());
    }

    @Test
    void deveRejeitarQuantidadeNaoPositivaNoPostgreSql() {
        Referencias referencias = salvarReferencias();

        assertThatThrownBy(() -> repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.material(), "Inválido", "UN",
                "0", "10", "0")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarCustoUnitarioNegativoNoPostgreSql() {
        Referencias referencias = salvarReferencias();

        assertThatThrownBy(() -> repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.material(), "Inválido", "UN",
                "1", "-0.01", "0")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarCustoTotalNegativoNoPostgreSql() {
        Referencias referencias = salvarReferencias();

        assertThatThrownBy(() -> repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.material(), "Inválido", "UN",
                "1", "10", "-0.01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarOrcamentoInexistentePelaForeignKey() {
        Material material = salvarMaterial();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO material_orcamento (
                    orcamento_id, material_id, descricao, unidade,
                    quantidade, custo_unitario, custo_total
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, Long.MAX_VALUE, material.getId(), "FK", "UN",
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarMaterialInexistentePelaForeignKey() {
        Orcamento orcamento = salvarOrcamento();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO material_orcamento (
                    orcamento_id, material_id, descricao, unidade,
                    quantidade, custo_unitario, custo_total
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, orcamento.getId(), Long.MAX_VALUE, "FK", "UN",
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveCarregarMaterialNasConsultas() {
        Referencias referencias = salvarReferencias();
        MaterialOrcamento salvo = repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.material(), "Carregamento", "UN",
                "1", "10", "10"));
        entityManager.clear();
        PersistenceUnitUtil persistence = entityManagerFactory.getPersistenceUnitUtil();

        MaterialOrcamento porId = repository
                .findByIdAndOrcamento_Id(salvo.getId(), referencias.orcamento().getId())
                .orElseThrow();
        List<MaterialOrcamento> listados = repository
                .findByOrcamento_IdOrderByIdAsc(referencias.orcamento().getId());

        assertThat(persistence.isLoaded(porId.getMaterial())).isTrue();
        assertThat(listados).allMatch(item -> persistence.isLoaded(item.getMaterial()));
    }

    @Test
    void devePreservarSnapshotsQuandoMaterialMudar() {
        Referencias referencias = salvarReferencias();
        MaterialOrcamento salvo = repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.material(), "Nome negociado", "M2",
                "1", "10", "10"));
        referencias.material().setNome("Nome atual " + UUID.randomUUID());
        referencias.material().setUnidade("UN");
        materialRepository.saveAndFlush(referencias.material());
        entityManager.clear();

        MaterialOrcamento preservado = repository
                .findByIdAndOrcamento_Id(salvo.getId(), referencias.orcamento().getId())
                .orElseThrow();

        assertThat(preservado.getDescricao()).isEqualTo("Nome negociado");
        assertThat(preservado.getUnidade()).isEqualTo("M2");
        assertThat(preservado.getMaterial().getNome()).isNotEqualTo("Nome negociado");
        assertThat(preservado.getMaterial().getUnidade()).isEqualTo("UN");
    }

    @Test
    void deveExcluirFisicamenteSomenteLinha() {
        Referencias referencias = salvarReferencias();
        MaterialOrcamento salvo = repository.saveAndFlush(registro(
                referencias.orcamento(), referencias.material(), "Remover", "UN",
                "1", "10", "10"));

        repository.delete(salvo);
        repository.flush();

        assertThat(repository.existsById(salvo.getId())).isFalse();
        assertThat(orcamentoRepository.existsById(referencias.orcamento().getId())).isTrue();
        assertThat(materialRepository.existsById(referencias.material().getId())).isTrue();
    }

    @Test
    void devePossuirSchemaPrecisaoChecksIndiceESemAtivoOuUnicidade() {
        List<Map<String, Object>> colunas = jdbcTemplate.queryForList("""
                SELECT column_name, data_type, character_maximum_length,
                       numeric_precision, numeric_scale, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'material_orcamento'
                """);
        List<String> checks = jdbcTemplate.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'material_orcamento'
                  AND constraint_type = 'CHECK'
                  AND constraint_name LIKE 'ck_material_orcamento_%'
                """, String.class);
        Integer ativo = contarColuna("ativo");
        Integer unicidade = contarConstraint("UNIQUE");
        Integer indice = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'material_orcamento'
                  AND indexname = 'idx_material_orcamento_orcamento_id'
                """, Integer.class);

        assertThat(colunas).filteredOn(c -> c.get("column_name").equals("descricao"))
                .singleElement().satisfies(c -> assertThat(c.get("character_maximum_length")).isEqualTo(200));
        assertThat(colunas).filteredOn(c -> c.get("column_name").equals("unidade"))
                .singleElement().satisfies(c -> assertThat(c.get("character_maximum_length")).isEqualTo(10));
        assertThat(colunas).filteredOn(c -> c.get("column_name").equals("quantidade"))
                .singleElement().satisfies(c -> {
                    assertThat(c.get("numeric_precision")).isEqualTo(15);
                    assertThat(c.get("numeric_scale")).isEqualTo(4);
                });
        assertThat(colunas).filteredOn(c -> c.get("column_name").equals("custo_unitario"))
                .singleElement().satisfies(c -> assertThat(c.get("numeric_scale")).isEqualTo(2));
        assertThat(colunas).filteredOn(c -> c.get("column_name").equals("custo_total"))
                .singleElement().satisfies(c -> assertThat(c.get("numeric_scale")).isEqualTo(2));
        assertThat(checks).containsExactlyInAnyOrder(
                "ck_material_orcamento_quantidade_positiva",
                "ck_material_orcamento_custo_unitario_nao_negativo",
                "ck_material_orcamento_custo_total_nao_negativo");
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
                  AND tc.table_name = 'material_orcamento'
                  AND tc.constraint_name IN (
                      'fk_material_orcamento_orcamento', 'fk_material_orcamento_material'
                  )
                ORDER BY tc.constraint_name
                """, String.class);

        assertThat(regras).containsExactly("NO ACTION", "NO ACTION");
    }

    private Integer contarColuna(String coluna) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'material_orcamento'
                  AND column_name = ?
                """, Integer.class, coluna);
    }

    private Integer contarConstraint(String tipo) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'material_orcamento'
                  AND constraint_type = ?
                """, Integer.class, tipo);
    }

    private Referencias salvarReferencias() {
        return new Referencias(salvarOrcamento(), salvarMaterial());
    }

    private Orcamento salvarOrcamento() {
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nome("Cliente material orçamento " + UUID.randomUUID()).build());
        StatusOrcamento rascunho = statusOrcamentoRepository
                .findByNomeNormalizado("Rascunho").orElseThrow();
        return orcamentoRepository.saveAndFlush(Orcamento.builder()
                .cliente(cliente).statusOrcamento(rascunho).build());
    }

    private Material salvarMaterial() {
        return materialRepository.saveAndFlush(Material.builder()
                .nome("Material orçamento " + UUID.randomUUID())
                .unidade("M2").build());
    }

    private MaterialOrcamento registro(
            Orcamento orcamento, Material material, String descricao, String unidade,
            String quantidade, String custoUnitario, String custoTotal) {
        return MaterialOrcamento.builder()
                .orcamento(orcamento).material(material)
                .descricao(descricao).unidade(unidade)
                .quantidade(new BigDecimal(quantidade))
                .custoUnitario(new BigDecimal(custoUnitario))
                .custoTotal(new BigDecimal(custoTotal)).build();
    }

    private record Referencias(Orcamento orcamento, Material material) {
    }
}
