package br.com.nucleodasreformas.nucleoerp.categoria_servico.mapper;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.dto.CategoriaServicoRequest;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.dto.CategoriaServicoResponse;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CategoriaServicoMapperTest {

    @Test
    void deveConverterRequestParaEntidadeAtivaPorPadrao() {
        CategoriaServico categoria = CategoriaServicoMapper.toEntity(request("Pintura", false));

        assertThat(categoria.getNome()).isEqualTo("Pintura");
        assertThat(categoria.getAtivo()).isTrue();
    }

    @Test
    void deveConverterEntidadeParaResponseCompleto() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 20, 12, 0);
        CategoriaServico categoria = CategoriaServico.builder()
                .id(1L)
                .nome("Pintura")
                .ativo(false)
                .criadoEm(criadoEm)
                .build();

        CategoriaServicoResponse response = CategoriaServicoMapper.toResponse(categoria);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNome()).isEqualTo("Pintura");
        assertThat(response.getAtivo()).isFalse();
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void deveAtualizarNomeEAtivoQuandoInformado() {
        CategoriaServico categoria = CategoriaServico.builder().nome("Antiga").ativo(false).build();

        CategoriaServicoMapper.updateEntity(categoria, request("Nova", true));

        assertThat(categoria.getNome()).isEqualTo("Nova");
        assertThat(categoria.getAtivo()).isTrue();
    }

    @Test
    void devePreservarAtivoQuandoOmitidoNaAtualizacao() {
        CategoriaServico categoria = CategoriaServico.builder().nome("Antiga").ativo(false).build();

        CategoriaServicoMapper.updateEntity(categoria, request("Nova", null));

        assertThat(categoria.getNome()).isEqualTo("Nova");
        assertThat(categoria.getAtivo()).isFalse();
    }

    private CategoriaServicoRequest request(String nome, Boolean ativo) {
        CategoriaServicoRequest request = new CategoriaServicoRequest();
        request.setNome(nome);
        request.setAtivo(ativo);
        return request;
    }
}
