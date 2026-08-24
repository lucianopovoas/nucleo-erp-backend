package br.com.nucleodasreformas.nucleoerp.status_ordem_servico.mapper;

import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoRequest;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class StatusOrdemServicoMapperTest {

    @Test
    void deveConverterRequestParaEntidade() {
        StatusOrdemServico status = StatusOrdemServicoMapper.toEntity(
                request("EM_EXECUCAO", "Em execução"));

        assertThat(status.getCodigo()).isEqualTo("EM_EXECUCAO");
        assertThat(status.getNome()).isEqualTo("Em execução");
        assertThat(status.getAtivo()).isTrue();
    }

    @Test
    void deveConverterEntidadeParaResponseCompleto() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 23, 10, 0);
        StatusOrdemServico status = StatusOrdemServico.builder().id(1L)
                .codigo("CONCLUIDO").nome("Concluído").ativo(false)
                .criadoEm(criadoEm).build();

        var response = StatusOrdemServicoMapper.toResponse(status);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCodigo()).isEqualTo("CONCLUIDO");
        assertThat(response.getNome()).isEqualTo("Concluído");
        assertThat(response.getAtivo()).isFalse();
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void deveAtualizarNomeEAtivoQuandoInformado() {
        StatusOrdemServico status = StatusOrdemServico.builder()
                .nome("Antigo").ativo(false).build();

        StatusOrdemServicoMapper.updateEntity(status, update("Novo", true));

        assertThat(status.getNome()).isEqualTo("Novo");
        assertThat(status.getAtivo()).isTrue();
    }

    @Test
    void devePreservarAtivoQuandoOmitido() {
        StatusOrdemServico status = StatusOrdemServico.builder()
                .nome("Antigo").ativo(false).build();

        StatusOrdemServicoMapper.updateEntity(status, update("Novo", null));

        assertThat(status.getNome()).isEqualTo("Novo");
        assertThat(status.getAtivo()).isFalse();
    }

    private StatusOrdemServicoRequest request(String codigo, String nome) {
        StatusOrdemServicoRequest request = new StatusOrdemServicoRequest();
        request.setCodigo(codigo);
        request.setNome(nome);
        return request;
    }

    private StatusOrdemServicoUpdateRequest update(String nome, Boolean ativo) {
        StatusOrdemServicoUpdateRequest request = new StatusOrdemServicoUpdateRequest();
        request.setNome(nome);
        request.setAtivo(ativo);
        return request;
    }
}
