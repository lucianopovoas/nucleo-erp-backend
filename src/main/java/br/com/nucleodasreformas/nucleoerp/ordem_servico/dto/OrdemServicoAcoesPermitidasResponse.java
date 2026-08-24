package br.com.nucleodasreformas.nucleoerp.ordem_servico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Capacidades atualmente disponíveis para a ordem. O backend revalida todas as operações.")
public class OrdemServicoAcoesPermitidasResponse {

    @Schema(description = "Indica se a observação da ordem pode ser alterada.")
    private boolean editarObservacao;

    @Schema(description = "Status ativos que constituem transições válidas a partir do estado atual.")
    private List<StatusOrdemServicoResumoResponse> alterarStatusPara;
}
