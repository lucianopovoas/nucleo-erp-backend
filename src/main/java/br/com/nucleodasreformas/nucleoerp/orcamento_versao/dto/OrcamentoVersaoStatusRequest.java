package br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Status de destino da versão do orçamento")
public class OrcamentoVersaoStatusRequest {

    @NotNull(message = "O status é obrigatório.")
    @Schema(example = "2")
    private Long statusOrcamentoId;
}
