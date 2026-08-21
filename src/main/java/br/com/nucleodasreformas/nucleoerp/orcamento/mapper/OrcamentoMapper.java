package br.com.nucleodasreformas.nucleoerp.orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.ClienteResumoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoVersaoResumoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.StatusOrcamentoResumoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;

public final class OrcamentoMapper {

    private OrcamentoMapper() {
    }

    public static Orcamento toEntity(Cliente cliente) {
        return Orcamento.builder().cliente(cliente).build();
    }

    public static OrcamentoResponse toResponse(Orcamento orcamento) {
        Cliente cliente = orcamento.getCliente();
        OrcamentoVersao versaoAtual = orcamento.getVersaoAtual();
        StatusOrcamento status = versaoAtual.getStatusOrcamento();

        return OrcamentoResponse.builder()
                .id(orcamento.getId())
                .numero(orcamento.getNumero())
                .cliente(ClienteResumoResponse.builder()
                        .id(cliente.getId())
                        .nome(cliente.getNome())
                        .build())
                .versaoAtual(OrcamentoVersaoResumoResponse.builder()
                        .id(versaoAtual.getId())
                        .numeroVersao(versaoAtual.getNumeroVersao())
                        .status(StatusOrcamentoResumoResponse.builder()
                                .id(status.getId())
                                .codigo(status.getCodigo())
                                .nome(status.getNome())
                                .build())
                        .criadoEm(versaoAtual.getCriadoEm())
                        .build())
                .criadoEm(orcamento.getCriadoEm())
                .build();
    }
}
