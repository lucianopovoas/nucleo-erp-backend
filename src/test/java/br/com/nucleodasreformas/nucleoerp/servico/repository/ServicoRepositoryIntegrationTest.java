package br.com.nucleodasreformas.nucleoerp.servico.repository;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.repository.CategoriaServicoRepository;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ServicoRepositoryIntegrationTest {

    @Autowired
    private ServicoRepository repository;

    @Autowired
    private CategoriaServicoRepository categoriaServicoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void deveAplicarDefaultsDoPostgreSql() {
        CategoriaServico categoria = salvarCategoria("Categoria defaults");

        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO servico (nome, categoria_servico_id)
                VALUES (?, ?)
                RETURNING id
                """, Long.class, "Serviço defaults " + UUID.randomUUID(), categoria.getId());

        Boolean ativo = jdbcTemplate.queryForObject("SELECT ativo FROM servico WHERE id = ?", Boolean.class, id);
        LocalDateTime criadoEm = jdbcTemplate.queryForObject(
                "SELECT criado_em FROM servico WHERE id = ?", LocalDateTime.class, id);

        assertThat(ativo).isTrue();
        assertThat(criadoEm).isNotNull();
    }

    @Test
    void deveRejeitarCategoriaInexistentePelaForeignKey() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO servico (nome, categoria_servico_id)
                VALUES (?, ?)
                """, "Serviço sem categoria", Long.MAX_VALUE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveGarantirUnicidadeNormalizadaNaMesmaCategoriaInclusiveInativo() {
        CategoriaServico categoria = salvarCategoria("Categoria unicidade");
        String nome = "Instalação " + UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO servico (nome, categoria_servico_id, ativo) VALUES (?, ?, FALSE)",
                "  " + nome + "  ", categoria.getId());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO servico (nome, categoria_servico_id) VALUES (?, ?)",
                nome.toUpperCase(), categoria.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void devePermitirMesmoNomeEmCategoriasDiferentes() {
        CategoriaServico primeira = salvarCategoria("Primeira categoria");
        CategoriaServico segunda = salvarCategoria("Segunda categoria");
        String nome = "Instalação " + UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO servico (nome, categoria_servico_id) VALUES (?, ?)", nome, primeira.getId());
        int inseridos = jdbcTemplate.update(
                "INSERT INTO servico (nome, categoria_servico_id) VALUES (?, ?)", nome, segunda.getId());

        assertThat(inseridos).isEqualTo(1);
    }

    @Test
    void deveConsultarNomeNormalizadoEExcluirProprioId() {
        CategoriaServico categoria = salvarCategoria("Categoria consultas");
        Servico servico = repository.saveAndFlush(Servico.builder()
                .nome("Instalação " + UUID.randomUUID())
                .categoriaServico(categoria)
                .build());

        assertThat(repository.existsByCategoriaENomeNormalizado(
                categoria.getId(), "  " + servico.getNome().toUpperCase() + "  ")).isTrue();
        assertThat(repository.existsByCategoriaENomeNormalizadoAndIdNot(
                categoria.getId(), servico.getNome(), servico.getId())).isFalse();
        assertThat(repository.existsByCategoriaENomeNormalizadoAndIdNot(
                categoria.getId(), servico.getNome(), servico.getId() + 1)).isTrue();
    }

    @Test
    void deveListarSomenteAtivosComCategoriaCarregada() {
        CategoriaServico categoria = salvarCategoria("Categoria listagem");
        Servico ativo = repository.saveAndFlush(Servico.builder()
                .nome("Ativo " + UUID.randomUUID()).categoriaServico(categoria).build());
        repository.saveAndFlush(Servico.builder()
                .nome("Inativo " + UUID.randomUUID()).categoriaServico(categoria).ativo(false).build());

        List<Servico> servicos = repository.findByAtivoTrue();
        PersistenceUnitUtil persistenceUnitUtil = entityManagerFactory.getPersistenceUnitUtil();

        assertThat(servicos).extracting(Servico::getId).contains(ativo.getId());
        assertThat(servicos).noneMatch(servico -> Boolean.FALSE.equals(servico.getAtivo()));
        assertThat(servicos).allMatch(servico -> persistenceUnitUtil.isLoaded(servico.getCategoriaServico()));
    }

    @Test
    void deveInativarSomenteServicosAtivosDaCategoriaInformada() {
        CategoriaServico primeira = salvarCategoria("Categoria cascata");
        CategoriaServico segunda = salvarCategoria("Categoria preservada");
        Servico ativoPrimeira = repository.saveAndFlush(Servico.builder()
                .nome("Ativo primeira " + UUID.randomUUID()).categoriaServico(primeira).build());
        Servico inativoPrimeira = repository.saveAndFlush(Servico.builder()
                .nome("Inativo primeira " + UUID.randomUUID()).categoriaServico(primeira).ativo(false).build());
        Servico ativoSegunda = repository.saveAndFlush(Servico.builder()
                .nome("Ativo segunda " + UUID.randomUUID()).categoriaServico(segunda).build());

        int atualizados = repository.inativarAtivosPorCategoriaId(primeira.getId());

        assertThat(atualizados).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT ativo FROM servico WHERE id = ?", Boolean.class, ativoPrimeira.getId())).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT ativo FROM servico WHERE id = ?", Boolean.class, inativoPrimeira.getId())).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT ativo FROM servico WHERE id = ?", Boolean.class, ativoSegunda.getId())).isTrue();
    }

    private CategoriaServico salvarCategoria(String prefixo) {
        return categoriaServicoRepository.saveAndFlush(CategoriaServico.builder()
                .nome(prefixo + " " + UUID.randomUUID())
                .build());
    }
}
