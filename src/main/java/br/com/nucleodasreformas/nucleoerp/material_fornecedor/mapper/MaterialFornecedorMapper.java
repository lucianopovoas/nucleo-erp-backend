package br.com.nucleodasreformas.nucleoerp.material_fornecedor.mapper;

import br.com.nucleodasreformas.nucleoerp.fornecedor.entity.Fornecedor;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material_fornecedor.dto.FornecedorResumoResponse;
import br.com.nucleodasreformas.nucleoerp.material_fornecedor.dto.MaterialFornecedorRequest;
import br.com.nucleodasreformas.nucleoerp.material_fornecedor.dto.MaterialFornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.material_fornecedor.dto.MaterialResumoResponse;
import br.com.nucleodasreformas.nucleoerp.material_fornecedor.entity.MaterialFornecedor;

public final class MaterialFornecedorMapper {

    private MaterialFornecedorMapper() {
    }

    public static MaterialFornecedor toEntity(
            MaterialFornecedorRequest request,
            Material material,
            Fornecedor fornecedor) {

        return MaterialFornecedor.builder()
                .material(material)
                .fornecedor(fornecedor)
                .precoCompra(request.getPrecoCompra())
                .build();
    }

    public static void updateEntity(
            MaterialFornecedor materialFornecedor,
            MaterialFornecedorRequest request,
            Material material,
            Fornecedor fornecedor) {

        materialFornecedor.setMaterial(material);
        materialFornecedor.setFornecedor(fornecedor);
        materialFornecedor.setPrecoCompra(request.getPrecoCompra());
    }

    public static MaterialFornecedorResponse toResponse(MaterialFornecedor materialFornecedor) {
        Material material = materialFornecedor.getMaterial();
        Fornecedor fornecedor = materialFornecedor.getFornecedor();

        return MaterialFornecedorResponse.builder()
                .id(materialFornecedor.getId())
                .material(MaterialResumoResponse.builder()
                        .id(material.getId())
                        .nome(material.getNome())
                        .build())
                .fornecedor(FornecedorResumoResponse.builder()
                        .id(fornecedor.getId())
                        .nome(fornecedor.getNome())
                        .build())
                .precoCompra(materialFornecedor.getPrecoCompra())
                .ativo(materialFornecedor.getAtivo())
                .criadoEm(materialFornecedor.getCriadoEm())
                .build();
    }
}
