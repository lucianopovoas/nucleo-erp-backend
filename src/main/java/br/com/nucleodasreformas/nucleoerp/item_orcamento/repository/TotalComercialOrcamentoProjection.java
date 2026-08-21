package br.com.nucleodasreformas.nucleoerp.item_orcamento.repository;

import java.math.BigDecimal;

public record TotalComercialOrcamentoProjection(
        Long orcamentoId,
        BigDecimal totalComercial) {
}
