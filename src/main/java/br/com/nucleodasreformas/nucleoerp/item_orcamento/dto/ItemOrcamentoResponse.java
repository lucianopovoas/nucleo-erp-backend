package br.com.nucleodasreformas.nucleoerp.item_orcamento.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ItemOrcamentoResponse {

    private Long id;
    private ServicoResumoResponse servico;
    private String descricao;
    private BigDecimal quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal desconto;
    private BigDecimal valorTotal;
    private LocalDateTime criadoEm;
}
