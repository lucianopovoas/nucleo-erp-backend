package br.com.nucleodasreformas.nucleoerp.item_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.repository.CategoriaServicoRepository;
import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.entity.ItemOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import br.com.nucleodasreformas.nucleoerp.servico.repository.ServicoRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ItemOrcamentoRepositoryIntegrationTest {

    @Autowired
    private ItemOrcamentoRepository repository;

    @Autowired
    private OrcamentoRepository orcamentoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private CategoriaServicoRepository categoriaServicoRepository;

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
    void devePersistirItemComPrecisaoEAuditoria() {
        Orcamento orcamento = salvarOrcamento();
        Servico servico = salvarServico("Serviço persistência");

        ItemOrcamento salvo = repository.saveAndFlush(item(
                orcamento, servico, "Descrição negociada", "2.5000", "150.00", "20.00", "355.00"));

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getCriadoEm()).isNotNull();
        assertThat(salvo.getQuantidade()).isEqualByComparingTo("2.5000");
        assertThat(salvo.getValorUnitario()).isEqualByComparingTo("150.00");
        assertThat(salvo.getDesconto()).isEqualByComparingTo("20.00");
        assertThat(salvo.getValorTotal()).isEqualByComparingTo("355.00");
    }

    @Test
    void deveAplicarDefaultsDoPostgreSql() {
        Orcamento orcamento = salvarOrcamento();
        Servico servico = salvarServico("Serviço defaults");

        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO item_orcamento (
                    orcamento_id, servico_id, descricao, quantidade, valor_unitario, valor_total
                ) VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class,
                orcamento.getId(), servico.getId(), "Defaults",
                new BigDecimal("2.0000"), new BigDecimal("10.00"), new BigDecimal("20.00"));

        BigDecimal desconto = jdbcTemplate.queryForObject(
                "SELECT desconto FROM item_orcamento WHERE id = ?", BigDecimal.class, id);
        LocalDateTime criadoEm = jdbcTemplate.queryForObject(
                "SELECT criado_em FROM item_orcamento WHERE id = ?", LocalDateTime.class, id);

        assertThat(desconto).isEqualByComparingTo("0.00");
        assertThat(criadoEm).isNotNull();
    }

    @Test
    void devePermitirMesmoServicoMaisDeUmaVezNoMesmoOrcamento() {
        Orcamento orcamento = salvarOrcamento();
        Servico servico = salvarServico("Serviço repetido");

        ItemOrcamento primeiro = repository.saveAndFlush(item(
                orcamento, servico, "Área frontal", "1", "100", "0", "100"));
        ItemOrcamento segundo = repository.saveAndFlush(item(
                orcamento, servico, "Área lateral", "2", "100", "0", "200"));

        assertThat(primeiro.getId()).isNotEqualTo(segundo.getId());
        assertThat(repository.findByOrcamento_IdOrderByIdAsc(orcamento.getId()))
                .extracting(ItemOrcamento::getId)
                .contains(primeiro.getId(), segundo.getId());
    }

    @Test
    void deveRejeitarQuantidadeNaoPositivaNoPostgreSql() {
        Referencias referencias = salvarReferencias();

        assertThatThrownBy(() -> repository.saveAndFlush(item(
                referencias.orcamento(), referencias.servico(), "Inválido",
                "0", "10", "0", "0")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarValorUnitarioNegativoNoPostgreSql() {
        Referencias referencias = salvarReferencias();

        assertThatThrownBy(() -> repository.saveAndFlush(item(
                referencias.orcamento(), referencias.servico(), "Inválido",
                "1", "-0.01", "0", "0")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarDescontoNegativoNoPostgreSql() {
        Referencias referencias = salvarReferencias();

        assertThatThrownBy(() -> repository.saveAndFlush(item(
                referencias.orcamento(), referencias.servico(), "Inválido",
                "1", "10", "-0.01", "0")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarValorTotalNegativoNoPostgreSql() {
        Referencias referencias = salvarReferencias();

        assertThatThrownBy(() -> repository.saveAndFlush(item(
                referencias.orcamento(), referencias.servico(), "Inválido",
                "1", "10", "0", "-0.01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarOrcamentoInexistentePelaForeignKey() {
        Servico servico = salvarServico("Serviço FK orçamento");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO item_orcamento (
                    orcamento_id, servico_id, descricao, quantidade,
                    valor_unitario, desconto, valor_total
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, Long.MAX_VALUE, servico.getId(), "FK",
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarServicoInexistentePelaForeignKey() {
        Orcamento orcamento = salvarOrcamento();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO item_orcamento (
                    orcamento_id, servico_id, descricao, quantidade,
                    valor_unitario, desconto, valor_total
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, orcamento.getId(), Long.MAX_VALUE, "FK",
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveCarregarServicoNasConsultasDoItem() {
        Referencias referencias = salvarReferencias();
        ItemOrcamento salvo = repository.saveAndFlush(item(
                referencias.orcamento(), referencias.servico(), "Carregamento",
                "1", "10", "0", "10"));
        entityManager.clear();
        PersistenceUnitUtil persistence = entityManagerFactory.getPersistenceUnitUtil();

        ItemOrcamento porId = repository
                .findByIdAndOrcamento_Id(salvo.getId(), referencias.orcamento().getId())
                .orElseThrow();
        List<ItemOrcamento> listados = repository
                .findByOrcamento_IdOrderByIdAsc(referencias.orcamento().getId());

        assertThat(persistence.isLoaded(porId.getServico())).isTrue();
        assertThat(listados).allMatch(item -> persistence.isLoaded(item.getServico()));
    }

    @Test
    void devePreservarSnapshotQuandoNomeDoServicoMudar() {
        Referencias referencias = salvarReferencias();
        ItemOrcamento salvo = repository.saveAndFlush(item(
                referencias.orcamento(), referencias.servico(), "Nome negociado",
                "1", "10", "0", "10"));
        referencias.servico().setNome("Nome atual " + UUID.randomUUID());
        servicoRepository.saveAndFlush(referencias.servico());
        entityManager.clear();

        ItemOrcamento preservado = repository
                .findByIdAndOrcamento_Id(salvo.getId(), referencias.orcamento().getId())
                .orElseThrow();

        assertThat(preservado.getDescricao()).isEqualTo("Nome negociado");
        assertThat(preservado.getServico().getNome()).isNotEqualTo("Nome negociado");
    }

    @Test
    void deveExcluirFisicamenteSomenteOItem() {
        Referencias referencias = salvarReferencias();
        ItemOrcamento salvo = repository.saveAndFlush(item(
                referencias.orcamento(), referencias.servico(), "Remover",
                "1", "10", "0", "10"));

        repository.delete(salvo);
        repository.flush();

        assertThat(repository.existsById(salvo.getId())).isFalse();
        assertThat(orcamentoRepository.existsById(referencias.orcamento().getId())).isTrue();
        assertThat(servicoRepository.existsById(referencias.servico().getId())).isTrue();
    }

    @Test
    void devePossuirSomenteChecksSimplesESemCampoAtivo() {
        List<String> checks = jdbcTemplate.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'item_orcamento'
                  AND constraint_type = 'CHECK'
                  AND constraint_name LIKE 'ck_item_orcamento_%'
                ORDER BY constraint_name
                """, String.class);
        Integer ativo = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'item_orcamento'
                  AND column_name = 'ativo'
                """, Integer.class);
        Integer unicidadeServico = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'item_orcamento'
                  AND constraint_type = 'UNIQUE'
                """, Integer.class);

        assertThat(checks).containsExactlyInAnyOrder(
                "ck_item_orcamento_quantidade_positiva",
                "ck_item_orcamento_valor_unitario_nao_negativo",
                "ck_item_orcamento_desconto_nao_negativo",
                "ck_item_orcamento_valor_total_nao_negativo");
        assertThat(ativo).isZero();
        assertThat(unicidadeServico).isZero();
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
                  AND tc.table_name = 'item_orcamento'
                  AND tc.constraint_name IN (
                      'fk_item_orcamento_orcamento', 'fk_item_orcamento_servico'
                  )
                ORDER BY tc.constraint_name
                """, String.class);

        assertThat(regras).containsExactly("NO ACTION", "NO ACTION");
    }

    private Referencias salvarReferencias() {
        return new Referencias(salvarOrcamento(), salvarServico("Serviço constraint"));
    }

    private Orcamento salvarOrcamento() {
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nome("Cliente item " + UUID.randomUUID())
                .build());
        StatusOrcamento rascunho = statusOrcamentoRepository
                .findByNomeNormalizado("Rascunho")
                .orElseThrow();
        return orcamentoRepository.saveAndFlush(Orcamento.builder()
                .cliente(cliente)
                .statusOrcamento(rascunho)
                .build());
    }

    private Servico salvarServico(String prefixo) {
        CategoriaServico categoria = categoriaServicoRepository.saveAndFlush(
                CategoriaServico.builder()
                        .nome("Categoria item " + UUID.randomUUID())
                        .build());
        return servicoRepository.saveAndFlush(Servico.builder()
                .nome(prefixo + " " + UUID.randomUUID())
                .categoriaServico(categoria)
                .build());
    }

    private ItemOrcamento item(
            Orcamento orcamento,
            Servico servico,
            String descricao,
            String quantidade,
            String valorUnitario,
            String desconto,
            String valorTotal) {

        return ItemOrcamento.builder()
                .orcamento(orcamento)
                .servico(servico)
                .descricao(descricao)
                .quantidade(new BigDecimal(quantidade))
                .valorUnitario(new BigDecimal(valorUnitario))
                .desconto(new BigDecimal(desconto))
                .valorTotal(new BigDecimal(valorTotal))
                .build();
    }

    private record Referencias(Orcamento orcamento, Servico servico) {
    }
}
