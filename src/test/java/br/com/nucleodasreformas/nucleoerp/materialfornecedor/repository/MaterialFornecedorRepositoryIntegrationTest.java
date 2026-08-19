package br.com.nucleodasreformas.nucleoerp.materialfornecedor.repository;

import br.com.nucleodasreformas.nucleoerp.fornecedor.entity.Fornecedor;
import br.com.nucleodasreformas.nucleoerp.fornecedor.repository.FornecedorRepository;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.entity.MaterialFornecedor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MaterialFornecedorRepositoryIntegrationTest {

    @Autowired
    private MaterialFornecedorRepository repository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void devePersistirComDefaultsEAuditoria() {
        Material material = salvarMaterial("Material persistência");
        Fornecedor fornecedor = salvarFornecedor("Fornecedor persistência");

        MaterialFornecedor salvo = repository.saveAndFlush(MaterialFornecedor.builder()
                .material(material)
                .fornecedor(fornecedor)
                .precoCompra(new BigDecimal("125.50"))
                .build());

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getAtivo()).isTrue();
        assertThat(salvo.getCriadoEm()).isNotNull();
        assertThat(salvo.getPrecoCompra()).isEqualByComparingTo("125.50");
    }

    @Test
    void deveGarantirUnicidadeAbsolutaMesmoComVinculoInativo() {
        Material material = salvarMaterial("Material único");
        Fornecedor fornecedor = salvarFornecedor("Fornecedor único");

        repository.saveAndFlush(MaterialFornecedor.builder()
                .material(material)
                .fornecedor(fornecedor)
                .ativo(false)
                .build());

        MaterialFornecedor duplicado = MaterialFornecedor.builder()
                .material(material)
                .fornecedor(fornecedor)
                .build();

        assertThatThrownBy(() -> repository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarPrecoNegativoNoPostgreSql() {
        Material material = salvarMaterial("Material preço");
        Fornecedor fornecedor = salvarFornecedor("Fornecedor preço");

        MaterialFornecedor invalido = MaterialFornecedor.builder()
                .material(material)
                .fornecedor(fornecedor)
                .precoCompra(new BigDecimal("-0.01"))
                .build();

        assertThatThrownBy(() -> repository.saveAndFlush(invalido))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void devePossuirForeignKeysEConstraintUnicaEsperadas() {
        assertThat(contarConstraint("fk_material_fornecedor_material", "FOREIGN KEY")).isEqualTo(1);
        assertThat(contarConstraint("fk_material_fornecedor_fornecedor", "FOREIGN KEY")).isEqualTo(1);
        assertThat(buscarTabelaReferenciada("fk_material_fornecedor_material")).isEqualTo("material");
        assertThat(buscarTabelaReferenciada("fk_material_fornecedor_fornecedor")).isEqualTo("fornecedor");
        assertThat(contarConstraint("uk_material_fornecedor_material_fornecedor", "UNIQUE")).isEqualTo(1);
        assertThat(contarConstraint("ck_material_fornecedor_preco_compra_nao_negativo", "CHECK")).isEqualTo(1);
    }

    private Integer contarConstraint(String nome, String tipo) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'material_fornecedor'
                  AND constraint_name = ?
                  AND constraint_type = ?
                """, Integer.class, nome, tipo);
    }

    private String buscarTabelaReferenciada(String nome) {
        return jdbcTemplate.queryForObject("""
                SELECT ccu.table_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.referential_constraints rc
                  ON rc.constraint_schema = tc.constraint_schema
                 AND rc.constraint_name = tc.constraint_name
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_schema = rc.unique_constraint_schema
                 AND ccu.constraint_name = rc.unique_constraint_name
                WHERE tc.table_schema = 'public'
                  AND tc.table_name = 'material_fornecedor'
                  AND tc.constraint_name = ?
                """, String.class, nome);
    }

    private Material salvarMaterial(String nome) {
        return materialRepository.saveAndFlush(Material.builder()
                .nome(nome)
                .unidade("UN")
                .build());
    }

    private Fornecedor salvarFornecedor(String nome) {
        return fornecedorRepository.saveAndFlush(Fornecedor.builder()
                .nome(nome)
                .build());
    }
}
