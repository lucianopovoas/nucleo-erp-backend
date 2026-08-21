package br.com.nucleodasreformas.nucleoerp.material_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.material_orcamento.entity.MaterialOrcamento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MaterialOrcamentoRepository extends JpaRepository<MaterialOrcamento, Long> {

    @EntityGraph(attributePaths = "material")
    List<MaterialOrcamento> findByOrcamento_IdOrderByIdAsc(Long orcamentoId);

    @EntityGraph(attributePaths = "material")
    Optional<MaterialOrcamento> findByIdAndOrcamento_Id(Long id, Long orcamentoId);

    @Query("""
            SELECT new br.com.nucleodasreformas.nucleoerp.material_orcamento.repository.CustoTotalMateriaisOrcamentoProjection(
                materialOrcamento.orcamento.id,
                SUM(materialOrcamento.custoTotal)
            )
            FROM MaterialOrcamento materialOrcamento
            WHERE materialOrcamento.orcamento.id IN :orcamentoIds
            GROUP BY materialOrcamento.orcamento.id
            """)
    List<CustoTotalMateriaisOrcamentoProjection> somarCustoTotalPorOrcamentos(
            @Param("orcamentoIds") Collection<Long> orcamentoIds);
}
