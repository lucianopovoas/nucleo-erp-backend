package br.com.nucleodasreformas.nucleoerp.servico.mapper;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.servico.dto.ServicoRequest;
import br.com.nucleodasreformas.nucleoerp.servico.dto.ServicoResponse;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ServicoMapperTest {

    @Test
    void deveConverterRequestParaEntidadeAtivaComCategoria() {
        CategoriaServico categoria = categoria(3L, "Toldos");

        Servico servico = ServicoMapper.toEntity(request("Instalação", 3L, false), categoria);

        assertThat(servico.getNome()).isEqualTo("Instalação");
        assertThat(servico.getCategoriaServico()).isSameAs(categoria);
        assertThat(servico.getAtivo()).isTrue();
    }

    @Test
    void deveConverterEntidadeParaResponseComResumoDaCategoria() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 20, 12, 0);
        Servico servico = Servico.builder()
                .id(10L)
                .nome("Instalação")
                .categoriaServico(categoria(3L, "Toldos"))
                .ativo(false)
                .criadoEm(criadoEm)
                .build();

        ServicoResponse response = ServicoMapper.toResponse(servico);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getNome()).isEqualTo("Instalação");
        assertThat(response.getCategoriaServico().getId()).isEqualTo(3L);
        assertThat(response.getCategoriaServico().getNome()).isEqualTo("Toldos");
        assertThat(response.getAtivo()).isFalse();
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void deveAtualizarNomeCategoriaEAtivo() {
        Servico servico = Servico.builder()
                .nome("Antigo")
                .categoriaServico(categoria(3L, "Toldos"))
                .ativo(false)
                .build();
        CategoriaServico novaCategoria = categoria(4L, "Comunicação visual");

        ServicoMapper.updateEntity(servico, request("Novo", 4L, true), novaCategoria);

        assertThat(servico.getNome()).isEqualTo("Novo");
        assertThat(servico.getCategoriaServico()).isSameAs(novaCategoria);
        assertThat(servico.getAtivo()).isTrue();
    }

    @Test
    void devePreservarAtivoQuandoOmitido() {
        Servico servico = Servico.builder()
                .nome("Antigo")
                .categoriaServico(categoria(3L, "Toldos"))
                .ativo(false)
                .build();

        ServicoMapper.updateEntity(servico, request("Novo", 3L, null), servico.getCategoriaServico());

        assertThat(servico.getAtivo()).isFalse();
    }

    private ServicoRequest request(String nome, Long categoriaServicoId, Boolean ativo) {
        ServicoRequest request = new ServicoRequest();
        request.setNome(nome);
        request.setCategoriaServicoId(categoriaServicoId);
        request.setAtivo(ativo);
        return request;
    }

    private CategoriaServico categoria(Long id, String nome) {
        return CategoriaServico.builder().id(id).nome(nome).ativo(true).build();
    }
}
