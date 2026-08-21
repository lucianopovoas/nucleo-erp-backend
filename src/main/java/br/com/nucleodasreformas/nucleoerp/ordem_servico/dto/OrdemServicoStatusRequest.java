package br.com.nucleodasreformas.nucleoerp.ordem_servico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Status de destino da ordem de serviço")
public class OrdemServicoStatusRequest {

    @NotNull(message = "O status é obrigatório.")
    @Schema(example = "2")
    private Long statusOrdemServicoId;
}
