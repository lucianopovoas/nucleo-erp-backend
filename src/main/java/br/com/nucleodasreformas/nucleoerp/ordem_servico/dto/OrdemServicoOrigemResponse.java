package br.com.nucleodasreformas.nucleoerp.ordem_servico.dto;

import br.com.nucleodasreformas.nucleoerp.orcamento.dto.ClienteResumoResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrdemServicoOrigemResponse {

    private OrcamentoOrigemResumoResponse orcamento;
    private OrcamentoVersaoOrigemResumoResponse versao;
    private ClienteResumoResponse cliente;
}
