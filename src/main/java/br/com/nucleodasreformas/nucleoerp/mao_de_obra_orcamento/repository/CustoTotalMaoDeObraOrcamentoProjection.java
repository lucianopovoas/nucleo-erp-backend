package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository;

import java.math.BigDecimal;

public record CustoTotalMaoDeObraOrcamentoProjection(
        Long orcamentoId,
        BigDecimal custoTotalMaoDeObra) {
}
