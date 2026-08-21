package br.com.nucleodasreformas.nucleoerp.orcamento.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrcamentoResponse {

    private Long id;
    private Long numero;
    private ClienteResumoResponse cliente;
    private OrcamentoVersaoResumoResponse versaoAtual;
    private LocalDateTime criadoEm;
}
