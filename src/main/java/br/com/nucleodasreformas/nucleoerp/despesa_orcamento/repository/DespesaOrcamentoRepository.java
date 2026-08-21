package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.entity.DespesaOrcamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DespesaOrcamentoRepository extends JpaRepository<DespesaOrcamento, Long> {

    List<DespesaOrcamento> findByOrcamentoVersao_IdOrderByIdAsc(Long orcamentoVersaoId);

    Optional<DespesaOrcamento> findByIdAndOrcamentoVersao_Id(Long id, Long orcamentoVersaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT despesa FROM DespesaOrcamento despesa
            WHERE despesa.id = :id AND despesa.orcamentoVersao.id = :versaoId
            """)
    Optional<DespesaOrcamento> findByIdAndOrcamentoVersaoIdForUpdate(
            @Param("id") Long id, @Param("versaoId") Long versaoId);

    @Query("""
            SELECT new br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository.CustoTotalDespesasOrcamentoProjection(
                despesaOrcamento.orcamentoVersao.id,
                SUM(despesaOrcamento.valor)
            )
            FROM DespesaOrcamento despesaOrcamento
            WHERE despesaOrcamento.orcamentoVersao.id IN :versaoIds
            GROUP BY despesaOrcamento.orcamentoVersao.id
            """)
    List<CustoTotalDespesasOrcamentoProjection> somarValorPorVersoes(
            @Param("versaoIds") Collection<Long> versaoIds);
}
