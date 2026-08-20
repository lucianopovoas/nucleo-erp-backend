package br.com.nucleodasreformas.nucleoerp.categoria_servico.mapper;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.dto.CategoriaServicoRequest;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.dto.CategoriaServicoResponse;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;

public final class CategoriaServicoMapper {

    private CategoriaServicoMapper() {
    }

    public static CategoriaServico toEntity(CategoriaServicoRequest request) {
        return CategoriaServico.builder()
                .nome(request.getNome())
                .build();
    }

    public static CategoriaServicoResponse toResponse(CategoriaServico categoriaServico) {
        return CategoriaServicoResponse.builder()
                .id(categoriaServico.getId())
                .nome(categoriaServico.getNome())
                .ativo(categoriaServico.getAtivo())
                .criadoEm(categoriaServico.getCriadoEm())
                .build();
    }

    public static void updateEntity(CategoriaServico categoriaServico, CategoriaServicoRequest request) {
        categoriaServico.setNome(request.getNome());
        if (request.getAtivo() != null) {
            categoriaServico.setAtivo(request.getAtivo());
        }
    }
}
