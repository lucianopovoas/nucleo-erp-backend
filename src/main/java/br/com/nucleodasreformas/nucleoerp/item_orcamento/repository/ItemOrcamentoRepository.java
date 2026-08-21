package br.com.nucleodasreformas.nucleoerp.item_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.item_orcamento.entity.ItemOrcamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ItemOrcamentoRepository extends JpaRepository<ItemOrcamento, Long> {

    @EntityGraph(attributePaths = "servico")
    List<ItemOrcamento> findByOrcamentoVersao_IdOrderByIdAsc(Long orcamentoVersaoId);

    @EntityGraph(attributePaths = "servico")
    Optional<ItemOrcamento> findByIdAndOrcamentoVersao_Id(Long id, Long orcamentoVersaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "servico")
    @Query("""
            SELECT item FROM ItemOrcamento item
            WHERE item.id = :id AND item.orcamentoVersao.id = :versaoId
            """)
    Optional<ItemOrcamento> findByIdAndOrcamentoVersaoIdForUpdate(
            @Param("id") Long id, @Param("versaoId") Long versaoId);

    @Query("""
            SELECT new br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.TotalComercialOrcamentoProjection(
                item.orcamentoVersao.id,
                SUM(item.valorTotal)
            )
            FROM ItemOrcamento item
            WHERE item.orcamentoVersao.id IN :versaoIds
            GROUP BY item.orcamentoVersao.id
            """)
    List<TotalComercialOrcamentoProjection> somarValorTotalPorVersoes(
            @Param("versaoIds") Collection<Long> versaoIds);
}
