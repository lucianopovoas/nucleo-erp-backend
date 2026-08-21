package br.com.nucleodasreformas.nucleoerp.material_orcamento.repository;

import java.math.BigDecimal;

public record CustoTotalMateriaisOrcamentoProjection(
        Long orcamentoVersaoId,
        BigDecimal custoTotalMateriais) {
}
