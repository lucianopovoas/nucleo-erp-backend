package br.com.nucleodasreformas.nucleoerp.material_orcamento.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MaterialOrcamentoResponse {

    private Long id;
    private MaterialResumoResponse material;
    private String descricao;
    private String unidade;
    private BigDecimal quantidade;
    private BigDecimal custoUnitario;
    private BigDecimal custoTotal;
    private LocalDateTime criadoEm;
}
