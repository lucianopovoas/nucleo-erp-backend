package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.entity.DespesaOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DespesaOrcamentoRepository extends JpaRepository<DespesaOrcamento, Long> {

    List<DespesaOrcamento> findByOrcamento_IdOrderByIdAsc(Long orcamentoId);

    Optional<DespesaOrcamento> findByIdAndOrcamento_Id(Long id, Long orcamentoId);

    @Query("""
            SELECT SUM(despesaOrcamento.valor)
            FROM DespesaOrcamento despesaOrcamento
            WHERE despesaOrcamento.orcamento.id = :orcamentoId
            """)
    BigDecimal somarValorPorOrcamento(@Param("orcamentoId") Long orcamentoId);

    @Query("""
            SELECT new br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository.CustoTotalDespesasOrcamentoProjection(
                despesaOrcamento.orcamento.id,
                SUM(despesaOrcamento.valor)
            )
            FROM DespesaOrcamento despesaOrcamento
            WHERE despesaOrcamento.orcamento.id IN :orcamentoIds
            GROUP BY despesaOrcamento.orcamento.id
            """)
    List<CustoTotalDespesasOrcamentoProjection> somarValorPorOrcamentos(
            @Param("orcamentoIds") Collection<Long> orcamentoIds);
}
