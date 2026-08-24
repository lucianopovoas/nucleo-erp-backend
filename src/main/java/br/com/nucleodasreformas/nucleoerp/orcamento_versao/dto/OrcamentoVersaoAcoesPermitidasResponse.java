package br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto;

import br.com.nucleodasreformas.nucleoerp.orcamento.dto.StatusOrcamentoResumoResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Capacidades atualmente disponíveis para a versão. O backend revalida todas as operações.")
public class OrcamentoVersaoAcoesPermitidasResponse {

    @Schema(description = "Indica se observação e linhas da versão podem ser alteradas.")
    private boolean editarConteudo;

    @Schema(description = "Indica se a versão pode originar uma nova versão.")
    private boolean criarNovaVersao;

    @Schema(description = "Status ativos que constituem transições válidas a partir do estado atual.")
    private List<StatusOrcamentoResumoResponse> alterarStatusPara;
}
