package br.com.nucleodasreformas.nucleoerp.orcamento_versao.service;

import java.math.BigDecimal;

public record TotaisOrcamentoVersao(
        BigDecimal totalComercial,
        BigDecimal custoTotalMateriais,
        BigDecimal custoTotalMaoDeObra,
        BigDecimal custoTotalDespesas,
        BigDecimal margemPrevista,
        BigDecimal percentualMargem) {
}
