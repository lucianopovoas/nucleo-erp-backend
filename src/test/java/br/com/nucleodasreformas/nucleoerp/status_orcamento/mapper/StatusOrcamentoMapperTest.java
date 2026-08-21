package br.com.nucleodasreformas.nucleoerp.status_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class StatusOrcamentoMapperTest {

    @Test
    void deveConverterRequestParaEntidadeComCodigo() {
        StatusOrcamento status = StatusOrcamentoMapper.toEntity(request("EM_ANALISE", "Em análise"));

        assertThat(status.getCodigo()).isEqualTo("EM_ANALISE");
        assertThat(status.getNome()).isEqualTo("Em análise");
        assertThat(status.getAtivo()).isTrue();
    }

    @Test
    void deveConverterEntidadeParaResponseCompleto() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 20, 12, 0);
        StatusOrcamento status = StatusOrcamento.builder()
                .id(1L)
                .codigo("RASCUNHO")
                .nome("Rascunho")
                .ativo(false)
                .criadoEm(criadoEm)
                .build();

        StatusOrcamentoResponse response = StatusOrcamentoMapper.toResponse(status);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCodigo()).isEqualTo("RASCUNHO");
        assertThat(response.getNome()).isEqualTo("Rascunho");
        assertThat(response.getAtivo()).isFalse();
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void deveAtualizarNomeEAtivoQuandoInformado() {
        StatusOrcamento status = StatusOrcamento.builder().nome("Antigo").ativo(false).build();

        StatusOrcamentoMapper.updateEntity(status, updateRequest("Novo", true));

        assertThat(status.getNome()).isEqualTo("Novo");
        assertThat(status.getAtivo()).isTrue();
    }

    @Test
    void devePreservarAtivoQuandoOmitidoNaAtualizacao() {
        StatusOrcamento status = StatusOrcamento.builder().nome("Antigo").ativo(false).build();

        StatusOrcamentoMapper.updateEntity(status, updateRequest("Novo", null));

        assertThat(status.getNome()).isEqualTo("Novo");
        assertThat(status.getAtivo()).isFalse();
    }

    private StatusOrcamentoRequest request(String codigo, String nome) {
        StatusOrcamentoRequest request = new StatusOrcamentoRequest();
        request.setCodigo(codigo);
        request.setNome(nome);
        return request;
    }

    private StatusOrcamentoUpdateRequest updateRequest(String nome, Boolean ativo) {
        StatusOrcamentoUpdateRequest request = new StatusOrcamentoUpdateRequest();
        request.setNome(nome);
        request.setAtivo(ativo);
        return request;
    }
}
