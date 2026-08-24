package br.com.nucleodasreformas.nucleoerp.ordem_servico.dto;

import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@Builder
public class OrdemServicoResponse {

    private Long id;
    private Long numero;
    private StatusOrdemServicoResumoResponse status;
    private String observacao;
    private LocalDateTime criadoEm;
    private OrdemServicoOrigemResponse origem;

    @Schema(description = "Ações calculadas no estado atual; não substituem a validação feita pelo backend.")
    private OrdemServicoAcoesPermitidasResponse acoesPermitidas;
}
