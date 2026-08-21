package br.com.nucleodasreformas.nucleoerp.orcamento_versao.service;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository.CustoTotalDespesasOrcamentoProjection;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository.DespesaOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.ItemOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.TotalComercialOrcamentoProjection;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository.CustoTotalMaoDeObraOrcamentoProjection;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository.MaoDeObraOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.repository.CustoTotalMateriaisOrcamentoProjection;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.repository.MaterialOrcamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrcamentoVersaoTotaisService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal CEM = new BigDecimal("100");

    private final ItemOrcamentoRepository itemRepository;
    private final MaterialOrcamentoRepository materialRepository;
    private final MaoDeObraOrcamentoRepository maoDeObraRepository;
    private final DespesaOrcamentoRepository despesaRepository;

    public Map<Long, TotaisOrcamentoVersao> buscarPorVersoes(Collection<Long> versaoIds) {
        if (versaoIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, BigDecimal> comerciais = itemRepository.somarValorTotalPorVersoes(versaoIds)
                .stream().collect(Collectors.toMap(
                        TotalComercialOrcamentoProjection::orcamentoVersaoId,
                        projection -> normalizar(projection.totalComercial())));
        Map<Long, BigDecimal> materiais = materialRepository.somarCustoTotalPorVersoes(versaoIds)
                .stream().collect(Collectors.toMap(
                        CustoTotalMateriaisOrcamentoProjection::orcamentoVersaoId,
                        projection -> normalizar(projection.custoTotalMateriais())));
        Map<Long, BigDecimal> maoDeObra = maoDeObraRepository.somarCustoTotalPorVersoes(versaoIds)
                .stream().collect(Collectors.toMap(
                        CustoTotalMaoDeObraOrcamentoProjection::orcamentoVersaoId,
                        projection -> normalizar(projection.custoTotalMaoDeObra())));
        Map<Long, BigDecimal> despesas = despesaRepository.somarValorPorVersoes(versaoIds)
                .stream().collect(Collectors.toMap(
                        CustoTotalDespesasOrcamentoProjection::orcamentoVersaoId,
                        projection -> normalizar(projection.custoTotalDespesas())));

        return versaoIds.stream().distinct().collect(Collectors.toMap(
                Function.identity(),
                id -> calcular(
                        comerciais.getOrDefault(id, ZERO),
                        materiais.getOrDefault(id, ZERO),
                        maoDeObra.getOrDefault(id, ZERO),
                        despesas.getOrDefault(id, ZERO))));
    }

    public TotaisOrcamentoVersao buscarPorVersao(Long versaoId) {
        return buscarPorVersoes(java.util.List.of(versaoId)).get(versaoId);
    }

    private TotaisOrcamentoVersao calcular(
            BigDecimal comercial,
            BigDecimal materiais,
            BigDecimal maoDeObra,
            BigDecimal despesas) {
        BigDecimal margem = comercial.subtract(materiais).subtract(maoDeObra).subtract(despesas)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal percentual = comercial.compareTo(BigDecimal.ZERO) == 0
                ? ZERO
                : margem.multiply(CEM).divide(comercial, 2, RoundingMode.HALF_UP);
        return new TotaisOrcamentoVersao(
                comercial, materiais, maoDeObra, despesas, margem, percentual);
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? ZERO : valor.setScale(2, RoundingMode.HALF_UP);
    }
}
