package br.com.nucleodasreformas.nucleoerp.item_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.item_orcamento.entity.ItemOrcamento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ItemOrcamentoRepository extends JpaRepository<ItemOrcamento, Long> {

    @EntityGraph(attributePaths = "servico")
    List<ItemOrcamento> findByOrcamento_IdOrderByIdAsc(Long orcamentoId);

    @EntityGraph(attributePaths = "servico")
    Optional<ItemOrcamento> findByIdAndOrcamento_Id(Long id, Long orcamentoId);

    @Query("""
            SELECT new br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.TotalComercialOrcamentoProjection(
                item.orcamento.id,
                SUM(item.valorTotal)
            )
            FROM ItemOrcamento item
            WHERE item.orcamento.id IN :orcamentoIds
            GROUP BY item.orcamento.id
            """)
    List<TotalComercialOrcamentoProjection> somarValorTotalPorOrcamentos(
            @Param("orcamentoIds") Collection<Long> orcamentoIds);
}
