package br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.repository;

import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UnidadeMaoDeObraRepositoryIntegrationTest {

    @Autowired
    private UnidadeMaoDeObraRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveAplicarSchemaEDefaultsDoPostgreSql() {
        String nome = "Unidade defaults " + UUID.randomUUID();

        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO unidade_mao_de_obra (nome) VALUES (?) RETURNING id",
                Long.class,
                nome);

        Boolean ativo = jdbcTemplate.queryForObject(
                "SELECT ativo FROM unidade_mao_de_obra WHERE id = ?", Boolean.class, id);
        LocalDateTime criadoEm = jdbcTemplate.queryForObject(
                "SELECT criado_em FROM unidade_mao_de_obra WHERE id = ?", LocalDateTime.class, id);
        Map<String, Object> colunaNome = jdbcTemplate.queryForMap("""
                SELECT character_maximum_length, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'unidade_mao_de_obra'
                  AND column_name = 'nome'
                """);

        assertThat(ativo).isTrue();
        assertThat(criadoEm).isNotNull();
        assertThat(colunaNome.get("character_maximum_length")).isEqualTo(100);
        assertThat(colunaNome.get("is_nullable")).isEqualTo("NO");
    }

    @Test
    void deveListarSomenteUnidadesAtivas() {
        String sufixo = UUID.randomUUID().toString();
        UnidadeMaoDeObra ativa = repository.saveAndFlush(UnidadeMaoDeObra.builder()
                .nome("Ativa " + sufixo)
                .build());
        repository.saveAndFlush(UnidadeMaoDeObra.builder()
                .nome("Inativa " + sufixo)
                .ativo(false)
                .build());

        List<UnidadeMaoDeObra> unidades = repository.findByAtivoTrue();

        assertThat(unidades).extracting(UnidadeMaoDeObra::getId).contains(ativa.getId());
        assertThat(unidades).noneMatch(unidade -> Boolean.FALSE.equals(unidade.getAtivo()));
    }

    @Test
    void deveConsultarNomeNormalizadoEExcluirOProprioIdNaAtualizacao() {
        UnidadeMaoDeObra unidade = repository.saveAndFlush(UnidadeMaoDeObra.builder()
                .nome("Hora " + UUID.randomUUID())
                .build());

        assertThat(repository.existsByNomeNormalizado(
                "  " + unidade.getNome().toUpperCase() + "  ")).isTrue();
        assertThat(repository.existsByNomeNormalizadoAndIdNot(
                unidade.getNome(), unidade.getId())).isFalse();
        assertThat(repository.existsByNomeNormalizadoAndIdNot(
                unidade.getNome(), unidade.getId() + 1)).isTrue();
    }

    @Test
    void deveGarantirUnicidadeIgnorandoMaiusculasEMinusculas() {
        String sufixo = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO unidade_mao_de_obra (nome) VALUES (?)", "Hora " + sufixo);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO unidade_mao_de_obra (nome) VALUES (?)", "hORA " + sufixo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveGarantirUnicidadeIgnorandoEspacosExternosMesmoParaRegistroInativo() {
        String nome = "Diária " + UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO unidade_mao_de_obra (nome, ativo) VALUES (?, FALSE)",
                "  " + nome + "  ");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO unidade_mao_de_obra (nome) VALUES (?)", nome))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void naoDeveNormalizarAcentosNaUnicidade() {
        String sufixo = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO unidade_mao_de_obra (nome) VALUES (?)", "Diaria " + sufixo);

        int inseridos = jdbcTemplate.update(
                "INSERT INTO unidade_mao_de_obra (nome) VALUES (?)", "Diária " + sufixo);

        assertThat(inseridos).isEqualTo(1);
    }

    @Test
    void devePossuirIndiceUnicoNormalizadoEsperado() {
        Integer quantidade = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'unidade_mao_de_obra'
                  AND indexname = 'uk_unidade_mao_de_obra_nome_normalizado'
                  AND indexdef ILIKE 'CREATE UNIQUE INDEX%'
                """, Integer.class);

        assertThat(quantidade).isEqualTo(1);
    }
}
