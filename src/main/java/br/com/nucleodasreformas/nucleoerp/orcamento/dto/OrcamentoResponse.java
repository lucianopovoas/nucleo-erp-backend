package br.com.nucleodasreformas.nucleoerp.orcamento.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrcamentoResponse {

    private Long id;
    private Long numero;
    private ClienteResumoResponse cliente;
    private StatusOrcamentoResumoResponse status;
    private String observacao;
    private BigDecimal totalComercial;
    private BigDecimal custoTotalMateriais;
    private BigDecimal custoTotalMaoDeObra;
    private LocalDateTime criadoEm;
}
