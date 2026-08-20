package br.com.nucleodasreformas.nucleoerp.servico.mapper;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.servico.dto.CategoriaServicoResumoResponse;
import br.com.nucleodasreformas.nucleoerp.servico.dto.ServicoRequest;
import br.com.nucleodasreformas.nucleoerp.servico.dto.ServicoResponse;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;

public final class ServicoMapper {

    private ServicoMapper() {
    }

    public static Servico toEntity(ServicoRequest request, CategoriaServico categoriaServico) {
        return Servico.builder()
                .nome(request.getNome())
                .categoriaServico(categoriaServico)
                .build();
    }

    public static void updateEntity(
            Servico servico,
            ServicoRequest request,
            CategoriaServico categoriaServico) {

        servico.setNome(request.getNome());
        servico.setCategoriaServico(categoriaServico);
        if (request.getAtivo() != null) {
            servico.setAtivo(request.getAtivo());
        }
    }

    public static ServicoResponse toResponse(Servico servico) {
        CategoriaServico categoriaServico = servico.getCategoriaServico();

        return ServicoResponse.builder()
                .id(servico.getId())
                .nome(servico.getNome())
                .categoriaServico(CategoriaServicoResumoResponse.builder()
                        .id(categoriaServico.getId())
                        .nome(categoriaServico.getNome())
                        .build())
                .ativo(servico.getAtivo())
                .criadoEm(servico.getCriadoEm())
                .build();
    }
}
