package br.com.nucleodasreformas.nucleoerp.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrcamentoVersionamentoMigrationTest {

    private static final String URL = "jdbc:postgresql://localhost:5432/nucleo_erp_test";
    private static final String USUARIO = "postgres";

    @Test
    void deveMigrarOrcamentoLegadoParaV1SemAlterarDadosDocumentais() throws Exception {
        String schema = novoSchema();
        Flyway flyway = flyway(schema, MigrationVersion.fromVersion("14"));
        try {
            flyway.migrate();
            LocalDateTime criadoOrcamento = LocalDateTime.of(2025, 3, 4, 10, 11, 12);
            LocalDateTime criadoLinha = LocalDateTime.of(2025, 3, 5, 13, 14, 15);
            try (Connection connection = conectar(schema); Statement sql = connection.createStatement()) {
                sql.executeUpdate("INSERT INTO status_orcamento (nome) VALUES ('Em análise')");
                sql.executeUpdate("INSERT INTO cliente (id,nome,criado_em,ativo) VALUES (101,'Cliente histórico','2025-01-01 08:00:00',true)");
                sql.executeUpdate("INSERT INTO categoria_servico (id,nome) VALUES (201,'Categoria histórica')");
                sql.executeUpdate("INSERT INTO servico (id,nome,categoria_servico_id) VALUES (301,'Serviço atual',201)");
                sql.executeUpdate("INSERT INTO material (id,nome,unidade,criado_em) VALUES (401,'Material atual','M2','2025-01-01 08:00:00')");
                sql.executeUpdate("INSERT INTO unidade_mao_de_obra (id,nome) VALUES (501,'Diária atual')");
                sql.executeUpdate("""
                        INSERT INTO orcamento (id,numero,cliente_id,status_orcamento_id,observacao,criado_em)
                        SELECT 601,202600125,101,id,'Observação histórica','2025-03-04 10:11:12'
                        FROM status_orcamento WHERE nome='Enviado'
                        """);
                sql.executeUpdate("""
                        INSERT INTO item_orcamento
                          (id,orcamento_id,servico_id,descricao,quantidade,valor_unitario,desconto,valor_total,criado_em)
                        VALUES (701,601,301,'Serviço snapshot',2.5000,123.45,10.00,298.63,'2025-03-05 13:14:15')
                        """);
                sql.executeUpdate("""
                        INSERT INTO material_orcamento
                          (id,orcamento_id,material_id,descricao,unidade,quantidade,custo_unitario,custo_total,criado_em)
                        VALUES (702,601,401,'Material snapshot','UN',3.0000,20.00,60.00,'2025-03-05 13:14:15')
                        """);
                sql.executeUpdate("""
                        INSERT INTO mao_de_obra_orcamento
                          (id,orcamento_id,unidade_mao_de_obra_id,descricao,unidade,quantidade,custo_unitario,custo_total,criado_em)
                        VALUES (703,601,501,'Mão de obra snapshot','Equipe',4.0000,30.00,120.00,'2025-03-05 13:14:15')
                        """);
                sql.executeUpdate("""
                        INSERT INTO despesa_orcamento
                          (id,orcamento_id,descricao,valor,criado_em)
                        VALUES (704,601,'Despesa snapshot',15.75,'2025-03-05 13:14:15')
                        """);
            }

            flyway(schema, null).migrate();

            try (Connection connection = conectar(schema); Statement sql = connection.createStatement()) {
                ResultSet orcamento = sql.executeQuery("SELECT numero,cliente_id,criado_em,versao_atual_id FROM orcamento WHERE id=601");
                assertThat(orcamento.next()).isTrue();
                assertThat(orcamento.getLong("numero")).isEqualTo(202600125L);
                assertThat(orcamento.getLong("cliente_id")).isEqualTo(101L);
                assertThat(orcamento.getTimestamp("criado_em").toLocalDateTime()).isEqualTo(criadoOrcamento);
                long versaoId = orcamento.getLong("versao_atual_id");

                ResultSet versao = sql.executeQuery("""
                        SELECT v.numero_versao,v.observacao,v.criado_em,s.codigo
                        FROM orcamento_versao v
                        JOIN status_orcamento s ON s.id=v.status_orcamento_id
                        WHERE v.id=""" + versaoId);
                assertThat(versao.next()).isTrue();
                assertThat(versao.getInt("numero_versao")).isEqualTo(1);
                assertThat(versao.getString("observacao")).isEqualTo("Observação histórica");
                assertThat(versao.getString("codigo")).isEqualTo("ENVIADO");
                assertThat(versao.getTimestamp("criado_em").toLocalDateTime()).isEqualTo(criadoOrcamento);

                assertLinha(sql, "item_orcamento", 701, versaoId, "descricao", "Serviço snapshot", criadoLinha);
                assertLinha(sql, "material_orcamento", 702, versaoId, "unidade", "UN", criadoLinha);
                assertLinha(sql, "mao_de_obra_orcamento", 703, versaoId, "unidade", "Equipe", criadoLinha);
                assertLinha(sql, "despesa_orcamento", 704, versaoId, "descricao", "Despesa snapshot", criadoLinha);

                ResultSet item = sql.executeQuery("SELECT * FROM item_orcamento WHERE id=701");
                assertThat(item.next()).isTrue();
                assertThat(item.getLong("servico_id")).isEqualTo(301L);
                assertThat(item.getBigDecimal("quantidade")).isEqualByComparingTo("2.5000");
                assertThat(item.getBigDecimal("valor_unitario")).isEqualByComparingTo("123.45");
                assertThat(item.getBigDecimal("desconto")).isEqualByComparingTo("10.00");
                assertThat(item.getBigDecimal("valor_total")).isEqualByComparingTo("298.63");

                ResultSet material = sql.executeQuery("SELECT * FROM material_orcamento WHERE id=702");
                assertThat(material.next()).isTrue();
                assertThat(material.getLong("material_id")).isEqualTo(401L);
                assertThat(material.getString("descricao")).isEqualTo("Material snapshot");
                assertThat(material.getBigDecimal("quantidade")).isEqualByComparingTo("3.0000");
                assertThat(material.getBigDecimal("custo_unitario")).isEqualByComparingTo("20.00");
                assertThat(material.getBigDecimal("custo_total")).isEqualByComparingTo("60.00");

                ResultSet maoDeObra = sql.executeQuery("SELECT * FROM mao_de_obra_orcamento WHERE id=703");
                assertThat(maoDeObra.next()).isTrue();
                assertThat(maoDeObra.getLong("unidade_mao_de_obra_id")).isEqualTo(501L);
                assertThat(maoDeObra.getString("descricao")).isEqualTo("Mão de obra snapshot");
                assertThat(maoDeObra.getBigDecimal("quantidade")).isEqualByComparingTo("4.0000");
                assertThat(maoDeObra.getBigDecimal("custo_unitario")).isEqualByComparingTo("30.00");
                assertThat(maoDeObra.getBigDecimal("custo_total")).isEqualByComparingTo("120.00");

                ResultSet despesa = sql.executeQuery("SELECT valor FROM despesa_orcamento WHERE id=704");
                assertThat(despesa.next()).isTrue();
                assertThat(despesa.getBigDecimal("valor")).isEqualByComparingTo("15.75");

                ResultSet colunasAntigas = sql.executeQuery("""
                        SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_schema=current_schema()
                          AND ((table_name='orcamento' AND column_name IN ('status_orcamento_id','observacao'))
                            OR (table_name IN ('item_orcamento','material_orcamento','mao_de_obra_orcamento','despesa_orcamento')
                                AND column_name='orcamento_id'))
                        """);
                assertThat(colunasAntigas.next()).isTrue();
                assertThat(colunasAntigas.getInt(1)).isZero();

                ResultSet codigoLegado = sql.executeQuery(
                        "SELECT codigo FROM status_orcamento WHERE nome='Em análise'");
                assertThat(codigoLegado.next()).isTrue();
                assertThat(codigoLegado.getString(1)).isEqualTo("EM_ANALISE");
            }
        } finally {
            limpar(schema);
        }
    }

    @Test
    void deveAbortarBackfillQuandoNomesDistintosColidiremNoMesmoCodigo() throws Exception {
        String schema = novoSchema();
        Flyway flyway = flyway(schema, MigrationVersion.fromVersion("14"));
        try {
            flyway.migrate();
            try (Connection connection = conectar(schema); Statement sql = connection.createStatement()) {
                sql.executeUpdate("INSERT INTO status_orcamento (nome) VALUES ('Em análise'),('Em analise')");
            }

            assertThatThrownBy(() -> flyway(schema, null).migrate())
                    .isInstanceOf(FlywayException.class)
                    .hasMessageContaining("V15__adicionar_codigo_status_orcamento.sql");
        } finally {
            limpar(schema);
        }
    }

    private void assertLinha(
            Statement sql, String tabela, long id, long versaoId,
            String snapshotColuna, String snapshot, LocalDateTime criadoEm) throws Exception {
        ResultSet linha = sql.executeQuery("SELECT * FROM " + tabela + " WHERE id=" + id);
        assertThat(linha.next()).isTrue();
        assertThat(linha.getLong("orcamento_versao_id")).isEqualTo(versaoId);
        assertThat(linha.getString(snapshotColuna)).isEqualTo(snapshot);
        assertThat(linha.getTimestamp("criado_em").toLocalDateTime()).isEqualTo(criadoEm);
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

    private void limpar(String schema) {
        flyway(schema, null).clean();
    }

    private String novoSchema() {
        return "migration_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String senha() {
        String senha = System.getenv("DB_PASSWORD");
        if (senha == null) {
            throw new IllegalStateException("DB_PASSWORD é obrigatório para o teste de migration.");
        }
        return senha;
    }
}
