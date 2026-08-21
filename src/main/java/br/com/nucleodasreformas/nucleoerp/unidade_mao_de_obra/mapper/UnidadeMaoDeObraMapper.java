package br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.mapper;

import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.dto.UnidadeMaoDeObraRequest;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.dto.UnidadeMaoDeObraResponse;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;

public final class UnidadeMaoDeObraMapper {

    private UnidadeMaoDeObraMapper() {
    }

    public static UnidadeMaoDeObra toEntity(UnidadeMaoDeObraRequest request) {
        return UnidadeMaoDeObra.builder()
                .nome(request.getNome())
                .build();
    }

    public static UnidadeMaoDeObraResponse toResponse(UnidadeMaoDeObra unidadeMaoDeObra) {
        return UnidadeMaoDeObraResponse.builder()
                .id(unidadeMaoDeObra.getId())
                .nome(unidadeMaoDeObra.getNome())
                .ativo(unidadeMaoDeObra.getAtivo())
                .criadoEm(unidadeMaoDeObra.getCriadoEm())
                .build();
    }

    public static void updateEntity(
            UnidadeMaoDeObra unidadeMaoDeObra,
            UnidadeMaoDeObraRequest request) {
        unidadeMaoDeObra.setNome(request.getNome());
        if (request.getAtivo() != null) {
            unidadeMaoDeObra.setAtivo(request.getAtivo());
        }
    }
}
