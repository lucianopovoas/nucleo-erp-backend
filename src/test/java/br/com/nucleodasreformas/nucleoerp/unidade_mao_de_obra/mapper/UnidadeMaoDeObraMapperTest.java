package br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.mapper;

import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.dto.UnidadeMaoDeObraRequest;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.dto.UnidadeMaoDeObraResponse;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UnidadeMaoDeObraMapperTest {

    @Test
    void deveConverterRequestParaEntidadeAtivaPorPadrao() {
        UnidadeMaoDeObra unidade = UnidadeMaoDeObraMapper.toEntity(request("Hora", false));

        assertThat(unidade.getNome()).isEqualTo("Hora");
        assertThat(unidade.getAtivo()).isTrue();
    }

    @Test
    void deveConverterEntidadeParaResponseCompleto() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 20, 12, 0);
        UnidadeMaoDeObra unidade = UnidadeMaoDeObra.builder()
                .id(1L)
                .nome("Hora")
                .ativo(false)
                .criadoEm(criadoEm)
                .build();

        UnidadeMaoDeObraResponse response = UnidadeMaoDeObraMapper.toResponse(unidade);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNome()).isEqualTo("Hora");
        assertThat(response.getAtivo()).isFalse();
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void deveAtualizarNomeEAtivoQuandoInformado() {
        UnidadeMaoDeObra unidade = UnidadeMaoDeObra.builder()
                .nome("Hora")
                .ativo(false)
                .build();

        UnidadeMaoDeObraMapper.updateEntity(unidade, request("Diária", true));

        assertThat(unidade.getNome()).isEqualTo("Diária");
        assertThat(unidade.getAtivo()).isTrue();
    }

    @Test
    void devePreservarAtivoQuandoOmitidoNaAtualizacao() {
        UnidadeMaoDeObra unidade = UnidadeMaoDeObra.builder()
                .nome("Hora")
                .ativo(false)
                .build();

        UnidadeMaoDeObraMapper.updateEntity(unidade, request("Diária", null));

        assertThat(unidade.getNome()).isEqualTo("Diária");
        assertThat(unidade.getAtivo()).isFalse();
    }

    private UnidadeMaoDeObraRequest request(String nome, Boolean ativo) {
        UnidadeMaoDeObraRequest request = new UnidadeMaoDeObraRequest();
        request.setNome(nome);
        request.setAtivo(ativo);
        return request;
    }
}
