package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository;

import java.math.BigDecimal;

public record CustoTotalDespesasOrcamentoProjection(
        Long orcamentoVersaoId,
        BigDecimal custoTotalDespesas) {
}
