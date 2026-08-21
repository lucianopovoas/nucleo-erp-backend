package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MaoDeObraOrcamentoResponse {

    private Long id;
    private UnidadeMaoDeObraResumoResponse unidadeMaoDeObra;
    private String descricao;
    private String unidade;
    private BigDecimal quantidade;
    private BigDecimal custoUnitario;
    private BigDecimal custoTotal;
    private LocalDateTime criadoEm;
}
