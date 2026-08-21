package br.com.nucleodasreformas.nucleoerp.ordem_servico.dto;

import lombok.Builder;
import lombok.Data;

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
}
