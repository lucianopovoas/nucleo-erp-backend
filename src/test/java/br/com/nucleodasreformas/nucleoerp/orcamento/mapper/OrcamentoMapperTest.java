package br.com.nucleodasreformas.nucleoerp.orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrcamentoMapperTest {

    @Test
    void deveConverterRequestParaEntidadeSemDefinirNumero() {
        Cliente cliente = cliente(10L, "Cliente X", true);
        StatusOrcamento status = status(1L, "Rascunho", true);
        OrcamentoRequest request = new OrcamentoRequest();
        request.setClienteId(10L);
        request.setObservacao("Área externa");

        Orcamento orcamento = OrcamentoMapper.toEntity(request, cliente, status);

        assertThat(orcamento.getNumero()).isNull();
        assertThat(orcamento.getCliente()).isSameAs(cliente);
        assertThat(orcamento.getStatusOrcamento()).isSameAs(status);
        assertThat(orcamento.getObservacao()).isEqualTo("Área externa");
    }

    @Test
    void deveConverterEntidadeParaResponseCompleto() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 20, 12, 0);
        Orcamento orcamento = Orcamento.builder()
                .id(5L)
                .numero(1234L)
                .cliente(cliente(10L, "Cliente X", false))
                .statusOrcamento(status(2L, "Enviado", false))
                .observacao("Área externa")
                .criadoEm(criadoEm)
                .build();

        OrcamentoResponse response = OrcamentoMapper.toResponse(orcamento);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getNumero()).isEqualTo(1234L);
        assertThat(response.getCliente().getId()).isEqualTo(10L);
        assertThat(response.getCliente().getNome()).isEqualTo("Cliente X");
        assertThat(response.getStatus().getId()).isEqualTo(2L);
        assertThat(response.getStatus().getNome()).isEqualTo("Enviado");
        assertThat(response.getObservacao()).isEqualTo("Área externa");
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void deveAtualizarRelacionamentosSemAlterarNumeroEObservacaoOmitida() {
        Orcamento orcamento = Orcamento.builder()
                .numero(1234L)
                .cliente(cliente(1L, "Anterior", true))
                .statusOrcamento(status(1L, "Rascunho", true))
                .observacao("Preservar")
                .build();
        Cliente novoCliente = cliente(2L, "Novo", true);
        StatusOrcamento novoStatus = status(2L, "Enviado", true);

        OrcamentoMapper.updateEntity(orcamento, new OrcamentoUpdateRequest(), novoCliente, novoStatus);

        assertThat(orcamento.getNumero()).isEqualTo(1234L);
        assertThat(orcamento.getCliente()).isSameAs(novoCliente);
        assertThat(orcamento.getStatusOrcamento()).isSameAs(novoStatus);
        assertThat(orcamento.getObservacao()).isEqualTo("Preservar");
    }

    @Test
    void deveLimparObservacaoQuandoNullForInformadoExplicitamente() {
        Orcamento orcamento = Orcamento.builder()
                .cliente(cliente(1L, "Cliente", true))
                .statusOrcamento(status(1L, "Rascunho", true))
                .observacao("Remover")
                .build();
        OrcamentoUpdateRequest request = new OrcamentoUpdateRequest();
        request.setObservacao(null);

        OrcamentoMapper.updateEntity(
                orcamento, request, orcamento.getCliente(), orcamento.getStatusOrcamento());

        assertThat(request.isObservacaoInformada()).isTrue();
        assertThat(orcamento.getObservacao()).isNull();
    }

    private Cliente cliente(Long id, String nome, boolean ativo) {
        return Cliente.builder().id(id).nome(nome).ativo(ativo).build();
    }

    private StatusOrcamento status(Long id, String nome, boolean ativo) {
        return StatusOrcamento.builder().id(id).nome(nome).ativo(ativo).build();
    }
}
