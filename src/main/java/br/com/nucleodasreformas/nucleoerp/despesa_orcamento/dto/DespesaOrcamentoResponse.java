package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DespesaOrcamentoResponse {

    private Long id;
    private String descricao;
    private BigDecimal valor;
    private LocalDateTime criadoEm;
}
