package br.com.nucleodasreformas.nucleoerp.categoria_servico.repository;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
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
class CategoriaServicoRepositoryIntegrationTest {

    @Autowired
    private CategoriaServicoRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveAplicarDefaultsDoPostgreSql() {
        String nome = "Categoria defaults " + UUID.randomUUID();

        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO categoria_servico (nome) VALUES (?) RETURNING id",
                Long.class,
                nome);

        Boolean ativo = jdbcTemplate.queryForObject(
                "SELECT ativo FROM categoria_servico WHERE id = ?", Boolean.class, id);
        LocalDateTime criadoEm = jdbcTemplate.queryForObject(
                "SELECT criado_em FROM categoria_servico WHERE id = ?", LocalDateTime.class, id);

        assertThat(ativo).isTrue();
        assertThat(criadoEm).isNotNull();
    }

    @Test
    void deveListarSomenteCategoriasAtivas() {
        String sufixo = UUID.randomUUID().toString();
        CategoriaServico ativa = repository.saveAndFlush(CategoriaServico.builder()
                .nome("Ativa " + sufixo)
                .build());
        repository.saveAndFlush(CategoriaServico.builder()
                .nome("Inativa " + sufixo)
                .ativo(false)
                .build());

        List<CategoriaServico> categorias = repository.findByAtivoTrue();

        assertThat(categorias).extracting(CategoriaServico::getId).contains(ativa.getId());
        assertThat(categorias).noneMatch(categoria -> Boolean.FALSE.equals(categoria.getAtivo()));
    }

    @Test
    void deveConsultarNomeNormalizadoEExcluirOProprioIdNaAtualizacao() {
        CategoriaServico categoria = repository.saveAndFlush(CategoriaServico.builder()
                .nome("Pintura " + UUID.randomUUID())
                .build());

        assertThat(repository.existsByNomeNormalizado("  " + categoria.getNome().toUpperCase() + "  ")).isTrue();
        assertThat(repository.existsByNomeNormalizadoAndIdNot(categoria.getNome(), categoria.getId())).isFalse();
        assertThat(repository.existsByNomeNormalizadoAndIdNot(categoria.getNome(), categoria.getId() + 1)).isTrue();
    }

    @Test
    void deveGarantirUnicidadeIgnorandoMaiusculasEMinusculas() {
        String sufixo = UUID.randomUUID().toString();
        jdbcTemplate.update("INSERT INTO categoria_servico (nome) VALUES (?)", "Pintura " + sufixo);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO categoria_servico (nome) VALUES (?)", "pINTURA " + sufixo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveGarantirUnicidadeIgnorandoEspacosExternosMesmoParaRegistroInativo() {
        String nome = "Elétrica " + UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO categoria_servico (nome, ativo) VALUES (?, FALSE)", "  " + nome + "  ");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO categoria_servico (nome) VALUES (?)", nome))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void devePossuirIndiceUnicoNormalizadoEsperado() {
        Integer quantidade = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'categoria_servico'
                  AND indexname = 'uk_categoria_servico_nome_normalizado'
                  AND indexdef ILIKE 'CREATE UNIQUE INDEX%'
                """, Integer.class);

        assertThat(quantidade).isEqualTo(1);
    }
}
