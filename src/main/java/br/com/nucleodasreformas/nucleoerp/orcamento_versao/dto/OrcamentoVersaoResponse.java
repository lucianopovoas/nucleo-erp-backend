package br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto;

import br.com.nucleodasreformas.nucleoerp.orcamento.dto.StatusOrcamentoResumoResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrcamentoVersaoResponse {

    private Long id;
    private Integer numeroVersao;
    private StatusOrcamentoResumoResponse status;
    private String observacao;
    private BigDecimal totalComercial;
    private BigDecimal custoTotalMateriais;
    private BigDecimal custoTotalMaoDeObra;
    private BigDecimal custoTotalDespesas;
    private BigDecimal margemPrevista;
    private BigDecimal percentualMargem;
    private LocalDateTime criadoEm;
}
