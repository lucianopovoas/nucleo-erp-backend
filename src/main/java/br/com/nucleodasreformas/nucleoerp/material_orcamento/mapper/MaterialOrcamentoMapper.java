package br.com.nucleodasreformas.nucleoerp.material_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialResumoResponse;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.entity.MaterialOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;

import java.math.BigDecimal;

public final class MaterialOrcamentoMapper {

    private MaterialOrcamentoMapper() {
    }

    public static MaterialOrcamento toEntity(
            Orcamento orcamento,
            Material material,
            String descricao,
            String unidade,
            BigDecimal quantidade,
            BigDecimal custoUnitario,
            BigDecimal custoTotal) {

        return MaterialOrcamento.builder()
                .orcamento(orcamento)
                .material(material)
                .descricao(descricao)
                .unidade(unidade)
                .quantidade(quantidade)
                .custoUnitario(custoUnitario)
                .custoTotal(custoTotal)
                .build();
    }

    public static void updateEntity(
            MaterialOrcamento materialOrcamento,
            Material material,
            String descricao,
            String unidade,
            BigDecimal quantidade,
            BigDecimal custoUnitario,
            BigDecimal custoTotal) {

        materialOrcamento.setMaterial(material);
        materialOrcamento.setDescricao(descricao);
        materialOrcamento.setUnidade(unidade);
        materialOrcamento.setQuantidade(quantidade);
        materialOrcamento.setCustoUnitario(custoUnitario);
        materialOrcamento.setCustoTotal(custoTotal);
    }

    public static MaterialOrcamentoResponse toResponse(MaterialOrcamento materialOrcamento) {
        Material material = materialOrcamento.getMaterial();

        return MaterialOrcamentoResponse.builder()
                .id(materialOrcamento.getId())
                .material(MaterialResumoResponse.builder()
                        .id(material.getId())
                        .nome(material.getNome())
                        .build())
                .descricao(materialOrcamento.getDescricao())
                .unidade(materialOrcamento.getUnidade())
                .quantidade(materialOrcamento.getQuantidade())
                .custoUnitario(materialOrcamento.getCustoUnitario())
                .custoTotal(materialOrcamento.getCustoTotal())
                .criadoEm(materialOrcamento.getCriadoEm())
                .build();
    }
}
