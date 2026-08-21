package br.com.nucleodasreformas.nucleoerp.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdemServicoMigrationTest {

    private static final String URL = "jdbc:postgresql://localhost:5432/nucleo_erp_test";
    private static final String USUARIO = "postgres";

    @Test
    void deveCriarDominioOperacionalSemAlterarDadosComerciais() throws Exception {
        String schema = novoSchema();
        try {
            flyway(schema, MigrationVersion.fromVersion("17")).migrate();
            prepararOrigens(schema);

            flyway(schema, null).migrate();

            try (Connection connection = conectar(schema);
                    Statement sql = connection.createStatement()) {
                ResultSet historico = sql.executeQuery("""
                        SELECT o.numero, v.numero_versao, v.observacao, s.codigo
                        FROM orcamento o
                        JOIN orcamento_versao v ON v.id = o.versao_atual_id
                        JOIN status_orcamento s ON s.id = v.status_orcamento_id
                        WHERE o.id = 601
                        """);
                assertThat(historico.next()).isTrue();
                assertThat(historico.getLong("numero")).isEqualTo(202600125L);
                assertThat(historico.getInt("numero_versao")).isEqualTo(3);
                assertThat(historico.getString("observacao")).isEqualTo("Documento aprovado");
                assertThat(historico.getString("codigo")).isEqualTo("APROVADO");

                ResultSet seed = sql.executeQuery("""
                        SELECT codigo, nome, ativo
                        FROM status_ordem_servico
                        ORDER BY id
                        """);
                assertThat(seed.next()).isTrue();
                assertThat(seed.getString("codigo")).isEqualTo("COMPRAR_MATERIAL");
                assertThat(seed.getString("nome")).isEqualTo("Comprar material");
                assertThat(seed.getBoolean("ativo")).isTrue();
                assertThat(seed.next()).isTrue();
                assertThat(seed.getString("codigo")).isEqualTo("EM_EXECUCAO");
                assertThat(seed.next()).isTrue();
                assertThat(seed.getString("codigo")).isEqualTo("INSTALAR");
                assertThat(seed.next()).isTrue();
                assertThat(seed.getString("codigo")).isEqualTo("CONCLUIDO");
                assertThat(seed.next()).isFalse();

                long numero = inserirOrdem(sql, 701L);
                assertThat(numero).isPositive();
                assertThatThrownBy(() -> inserirOrdem(sql, 701L))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("uk_ordem_servico_orcamento_versao");

                assertThatThrownBy(() -> sql.executeUpdate("""
                        INSERT INTO ordem_servico
                          (numero, orcamento_versao_id, status_ordem_servico_id)
                        SELECT %d, 702, id
                        FROM status_ordem_servico
                        WHERE codigo='COMPRAR_MATERIAL'
                        """.formatted(numero)))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("uk_ordem_servico_numero");

                assertThatThrownBy(() -> sql.executeUpdate("""
                        UPDATE status_ordem_servico
                        SET codigo='OUTRO'
                        WHERE codigo='COMPRAR_MATERIAL'
                        """))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("codigo do status de ordem de servico e imutavel");
            }
        } finally {
            flyway(schema, null).clean();
        }
    }

    private void prepararOrigens(String schema) throws Exception {
        try (Connection connection = conectar(schema); Statement sql = connection.createStatement()) {
            sql.executeUpdate("""
                    INSERT INTO cliente (id,nome,criado_em,ativo)
                    VALUES
                      (101,'Cliente um','2026-01-01 10:00:00',true),
                      (102,'Cliente dois','2026-01-01 10:00:00',true)
                    """);
            sql.executeUpdate("""
                    INSERT INTO orcamento (id,numero,cliente_id,criado_em)
                    VALUES
                      (601,202600125,101,'2026-02-01 10:00:00'),
                      (602,202600126,102,'2026-02-01 10:00:00')
                    """);
            sql.executeUpdate("""
                    INSERT INTO orcamento_versao
                      (id,orcamento_id,numero_versao,status_orcamento_id,observacao,criado_em)
                    SELECT 701,601,3,id,'Documento aprovado','2026-02-02 10:00:00'
                    FROM status_orcamento WHERE codigo='APROVADO'
                    """);
            sql.executeUpdate("""
                    INSERT INTO orcamento_versao
                      (id,orcamento_id,numero_versao,status_orcamento_id,observacao,criado_em)
                    SELECT 702,602,1,id,'Segundo documento','2026-02-02 10:00:00'
                    FROM status_orcamento WHERE codigo='APROVADO'
                    """);
            sql.executeUpdate("UPDATE orcamento SET versao_atual_id=701 WHERE id=601");
            sql.executeUpdate("UPDATE orcamento SET versao_atual_id=702 WHERE id=602");
        }
    }

    private long inserirOrdem(Statement sql, long versaoId) throws Exception {
        ResultSet result = sql.executeQuery("""
                INSERT INTO ordem_servico (orcamento_versao_id, status_ordem_servico_id)
                SELECT %d, id FROM status_ordem_servico WHERE codigo='COMPRAR_MATERIAL'
                RETURNING numero
                """.formatted(versaoId));
        assertThat(result.next()).isTrue();
        return result.getLong(1);
    }

    private Flyway flyway(String schema, MigrationVersion target) {
        var configuracao = Flyway.configure()
                .dataSource(URL, USUARIO, senha())
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(true)
                .locations("classpath:db/migration")
                .cleanDisabled(false);
        if (target != null) {
            configuracao.target(target);
        }
        return configuracao.load();
    }

    private Connection conectar(String schema) throws Exception {
        Connection connection = DriverManager.getConnection(URL, USUARIO, senha());
        try (Statement sql = connection.createStatement()) {
            sql.execute("SET search_path TO \"" + schema + "\"");
        }
        return connection;
    }

    private String novoSchema() {
        return "migration_os_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String senha() {
        String senha = System.getenv("DB_PASSWORD");
        if (senha == null) {
            throw new IllegalStateException("DB_PASSWORD é obrigatório para o teste de migration.");
        }
        return senha;
    }
}
