package br.com.nucleodasreformas.nucleoerp.orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrcamentoRepositoryIntegrationTest {

    @Autowired
    private OrcamentoRepository repository;

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
    void deveGerarNumerosDistintosPelaSequenceDoPostgreSql() {
        Cliente cliente = salvarCliente("Cliente sequência");
        StatusOrcamento status = buscarRascunho();

        Orcamento primeiro = repository.saveAndFlush(Orcamento.builder()
                .cliente(cliente).statusOrcamento(status).build());
        Orcamento segundo = repository.saveAndFlush(Orcamento.builder()
                .cliente(cliente).statusOrcamento(status).build());

        assertThat(primeiro.getNumero()).isNotNull();
        assertThat(segundo.getNumero()).isNotNull();
        assertThat(primeiro.getNumero()).isNotEqualTo(segundo.getNumero());
        assertThat(primeiro.getId()).isNotEqualTo(segundo.getId());
    }

    @Test
    void deveAplicarDefaultsDoPostgreSql() {
        Cliente cliente = salvarCliente("Cliente defaults");
        StatusOrcamento status = buscarRascunho();

        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO orcamento (cliente_id, status_orcamento_id)
                VALUES (?, ?)
                RETURNING id
                """, Long.class, cliente.getId(), status.getId());

        Long numero = jdbcTemplate.queryForObject(
                "SELECT numero FROM orcamento WHERE id = ?", Long.class, id);
        LocalDateTime criadoEm = jdbcTemplate.queryForObject(
                "SELECT criado_em FROM orcamento WHERE id = ?", LocalDateTime.class, id);

        assertThat(numero).isNotNull();
        assertThat(criadoEm).isNotNull();
    }

    @Test
    void deveGarantirUnicidadeDoNumeroNoBanco() {
        Cliente cliente = salvarCliente("Cliente unicidade");
        StatusOrcamento status = buscarRascunho();
        long numero = 9_000_000_000_000L + ThreadLocalRandom.current().nextLong(1_000_000L);
        jdbcTemplate.update("""
                INSERT INTO orcamento (numero, cliente_id, status_orcamento_id)
                VALUES (?, ?, ?)
                """, numero, cliente.getId(), status.getId());

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO orcamento (numero, cliente_id, status_orcamento_id)
                VALUES (?, ?, ?)
                """, numero, cliente.getId(), status.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarClienteInexistentePelaForeignKey() {
        StatusOrcamento status = buscarRascunho();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO orcamento (cliente_id, status_orcamento_id)
                VALUES (?, ?)
                """, Long.MAX_VALUE, status.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarStatusInexistentePelaForeignKey() {
        Cliente cliente = salvarCliente("Cliente FK status");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO orcamento (cliente_id, status_orcamento_id)
                VALUES (?, ?)
                """, cliente.getId(), Long.MAX_VALUE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveCarregarClienteEStatusNasBuscas() {
        Cliente cliente = salvarCliente("Cliente carregamento");
        StatusOrcamento status = salvarStatus("Status carregamento");
        Orcamento salvo = repository.saveAndFlush(Orcamento.builder()
                .cliente(cliente).statusOrcamento(status).build());
        entityManager.clear();
        PersistenceUnitUtil persistenceUnitUtil = entityManagerFactory.getPersistenceUnitUtil();

        Orcamento porId = repository.findById(salvo.getId()).orElseThrow();
        List<Orcamento> todos = repository.findAll();

        assertThat(persistenceUnitUtil.isLoaded(porId.getCliente())).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(porId.getStatusOrcamento())).isTrue();
        assertThat(todos).filteredOn(item -> item.getId().equals(salvo.getId()))
                .allMatch(item -> persistenceUnitUtil.isLoaded(item.getCliente()))
                .allMatch(item -> persistenceUnitUtil.isLoaded(item.getStatusOrcamento()));
    }

    @Test
    void devePreservarOrcamentoQuandoClienteEStatusForemInativados() {
        Cliente cliente = salvarCliente("Cliente histórico");
        StatusOrcamento status = salvarStatus("Status histórico");
        Orcamento salvo = repository.saveAndFlush(Orcamento.builder()
                .cliente(cliente).statusOrcamento(status).observacao("Histórico").build());

        cliente.setAtivo(false);
        status.setAtivo(false);
        clienteRepository.saveAndFlush(cliente);
        statusOrcamentoRepository.saveAndFlush(status);
        entityManager.clear();

        Orcamento preservado = repository.findById(salvo.getId()).orElseThrow();

        assertThat(preservado.getCliente().getAtivo()).isFalse();
        assertThat(preservado.getStatusOrcamento().getAtivo()).isFalse();
        assertThat(preservado.getObservacao()).isEqualTo("Histórico");
        assertThat(repository.findAll()).extracting(Orcamento::getId).contains(salvo.getId());
    }

    @Test
    void devePossuirSequencePropriaEConstraintUnica() {
        String defaultNumero = jdbcTemplate.queryForObject("""
                SELECT column_default
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'orcamento'
                  AND column_name = 'numero'
                """, String.class);
        Integer constraints = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'orcamento'
                  AND constraint_name = 'uk_orcamento_numero'
                  AND constraint_type = 'UNIQUE'
                """, Integer.class);

        assertThat(defaultNumero).contains("orcamento_numero_seq");
        assertThat(constraints).isEqualTo(1);
    }

    private Cliente salvarCliente(String prefixo) {
        return clienteRepository.saveAndFlush(Cliente.builder()
                .nome(prefixo + " " + UUID.randomUUID())
                .build());
    }

    private StatusOrcamento salvarStatus(String prefixo) {
        return statusOrcamentoRepository.saveAndFlush(StatusOrcamento.builder()
                .nome(prefixo + " " + UUID.randomUUID())
                .build());
    }

    private StatusOrcamento buscarRascunho() {
        return statusOrcamentoRepository.findByNomeNormalizado("Rascunho").orElseThrow();
    }
}
