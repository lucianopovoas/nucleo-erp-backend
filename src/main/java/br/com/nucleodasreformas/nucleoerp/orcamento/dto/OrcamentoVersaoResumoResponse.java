package br.com.nucleodasreformas.nucleoerp.orcamento.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrcamentoVersaoResumoResponse {

    private Long id;
    private Integer numeroVersao;
    private StatusOrcamentoResumoResponse status;
    private LocalDateTime criadoEm;
}
