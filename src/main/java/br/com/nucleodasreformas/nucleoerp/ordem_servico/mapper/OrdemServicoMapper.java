package br.com.nucleodasreformas.nucleoerp.ordem_servico.mapper;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.ClienteResumoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrcamentoOrigemResumoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrcamentoVersaoOrigemResumoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoOrigemResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.StatusOrdemServicoResumoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.entity.OrdemServico;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;

public final class OrdemServicoMapper {

    private OrdemServicoMapper() {
    }

    public static OrdemServico toEntity(
            OrcamentoVersao versao, StatusOrdemServico status) {
        return OrdemServico.builder()
                .orcamentoVersao(versao)
                .statusOrdemServico(status)
                .build();
    }

    public static OrdemServicoResponse toResponse(OrdemServico ordemServico) {
        OrcamentoVersao versao = ordemServico.getOrcamentoVersao();
        Orcamento orcamento = versao.getOrcamento();
        Cliente cliente = orcamento.getCliente();
        StatusOrdemServico status = ordemServico.getStatusOrdemServico();

        return OrdemServicoResponse.builder()
                .id(ordemServico.getId())
                .numero(ordemServico.getNumero())
                .status(StatusOrdemServicoResumoResponse.builder()
                        .id(status.getId())
                        .codigo(status.getCodigo())
                        .nome(status.getNome())
                        .build())
                .observacao(ordemServico.getObservacao())
                .criadoEm(ordemServico.getCriadoEm())
                .origem(OrdemServicoOrigemResponse.builder()
                        .orcamento(OrcamentoOrigemResumoResponse.builder()
                                .id(orcamento.getId())
                                .numero(orcamento.getNumero())
                                .build())
                        .versao(OrcamentoVersaoOrigemResumoResponse.builder()
                                .id(versao.getId())
                                .numeroVersao(versao.getNumeroVersao())
                                .build())
                        .cliente(ClienteResumoResponse.builder()
                                .id(cliente.getId())
                                .nome(cliente.getNome())
                                .build())
                        .build())
                .build();
    }
}
