package br.com.nucleodasreformas.nucleoerp.material.mapper;

import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialRequest;
import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialResponse;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;

public class MaterialMapper {

    public MaterialMapper() {
    }

    public static Material toEntity(MaterialRequest request){

        Material.MaterialBuilder builder = Material.builder()
                .nome(request.getNome())
                .descricao(request.getDescricao())
                .unidade(request.getUnidade())
                .largura(request.getLargura());

        if (request.getAtivo() != null) {
            builder.ativo(request.getAtivo());
        }

        return builder.build();
    }

    public static MaterialResponse toResponse(Material material){

        return MaterialResponse.builder()
                .id(material.getId())
                .nome(material.getNome())
                .descricao(material.getDescricao())
                .unidade(material.getUnidade())
                .largura(material.getLargura())
                .ativo(material.getAtivo())
                .criadoEm(material.getCriadoEm())
                .build();
    }

    public static void updateEntity(Material material, MaterialRequest request){

        material.setNome(request.getNome());
        material.setDescricao(request.getDescricao());
        material.setUnidade(request.getUnidade());
        material.setLargura(request.getLargura());
        if (request.getAtivo() != null) {
            material.setAtivo(request.getAtivo());
        }
    }

}
