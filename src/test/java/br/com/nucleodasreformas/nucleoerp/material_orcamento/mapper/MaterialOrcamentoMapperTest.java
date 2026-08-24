package br.com.nucleodasreformas.nucleoerp.material_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.entity.MaterialOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialOrcamentoMapperTest {

    @Test
    void devePreservarSnapshotNaEntidadeEAtualizacao() {
        OrcamentoVersao versao = OrcamentoVersao.builder().id(20L).build();
        Material material = Material.builder().id(5L).nome("Lona atual").build();
        MaterialOrcamento linha = MaterialOrcamentoMapper.toEntity(versao, material,
                "Lona negociada", "M2", valor("2.0000"), valor("75.00"), valor("150.00"));

        assertThat(linha.getOrcamentoVersao()).isSameAs(versao);
        assertThat(linha.getUnidade()).isEqualTo("M2");
        assertThat(linha.getCustoTotal()).isEqualByComparingTo("150.00");

        MaterialOrcamentoMapper.updateEntity(linha, material, "Revisada", "UN",
                valor("3.0000"), valor("50.00"), valor("150.00"));
        assertThat(linha.getDescricao()).isEqualTo("Revisada");
        assertThat(linha.getUnidade()).isEqualTo("UN");
    }

    @Test
    void deveSepararNomeAtualDoMaterialEDescricaoSnapshotNoResponse() {
        MaterialOrcamento linha = MaterialOrcamento.builder().id(30L)
                .material(Material.builder().id(5L).nome("Nome atual").build())
                .descricao("Snapshot histórico").unidade("M2")
                .quantidade(valor("2.0000")).custoUnitario(valor("75.00"))
                .custoTotal(valor("150.00")).build();

        var response = MaterialOrcamentoMapper.toResponse(linha);

        assertThat(response.getMaterial().getNome()).isEqualTo("Nome atual");
        assertThat(response.getDescricao()).isEqualTo("Snapshot histórico");
        assertThat(response.getUnidade()).isEqualTo("M2");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("150.00");
    }

    private BigDecimal valor(String valor) { return new BigDecimal(valor); }
}
