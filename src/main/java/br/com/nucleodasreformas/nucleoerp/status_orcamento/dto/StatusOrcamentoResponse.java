package br.com.nucleodasreformas.nucleoerp.status_orcamento.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StatusOrcamentoResponse {

    private Long id;
    private String codigo;
    private String nome;
    private Boolean ativo;
    private LocalDateTime criadoEm;
}
