package br.com.nucleodasreformas.nucleoerp.material.mapper;

import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialRequest;
import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialResponse;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;

public class MaterialMapper {

    public MaterialMapper() {
    }

    public static Material toEntity(MaterialRequest request){

        return Material.builder()
                .nome(request.getNome())
                .descricao(request.getDescricao())
                .unidade(request.getUnidade())
                .largura(request.getLargura())
                .ativo(request.isAtivo())
                .build();
    }

    public static MaterialResponse toResponse(Material material){

        return MaterialResponse.builder()
                .nome(material.getNome())
                .descricao(material.getDescricao())
                .unidade(material.getUnidade())
                .largura(material.getLargura())
                .ativo(material.getAtivo())
                .build();
    }

    public static void updateEntity(Material material, MaterialRequest request){

        material.setNome(request.getNome());
        material.setDescricao(request.getDescricao());
        material.setUnidade(request.getUnidade());
        material.setLargura(request.getLargura());
        material.setAtivo(request.isAtivo());
    }

}