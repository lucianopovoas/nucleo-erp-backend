package br.com.nucleodasreformas.nucleoerp.material_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.material_orcamento.entity.MaterialOrcamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MaterialOrcamentoRepository extends JpaRepository<MaterialOrcamento, Long> {

    @EntityGraph(attributePaths = "material")
    List<MaterialOrcamento> findByOrcamentoVersao_IdOrderByIdAsc(Long orcamentoVersaoId);

    @EntityGraph(attributePaths = "material")
    Optional<MaterialOrcamento> findByIdAndOrcamentoVersao_Id(Long id, Long orcamentoVersaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "material")
    @Query("""
            SELECT material FROM MaterialOrcamento material
            WHERE material.id = :id AND material.orcamentoVersao.id = :versaoId
            """)
    Optional<MaterialOrcamento> findByIdAndOrcamentoVersaoIdForUpdate(
            @Param("id") Long id, @Param("versaoId") Long versaoId);

    @Query("""
            SELECT new br.com.nucleodasreformas.nucleoerp.material_orcamento.repository.CustoTotalMateriaisOrcamentoProjection(
                materialOrcamento.orcamentoVersao.id,
                SUM(materialOrcamento.custoTotal)
            )
            FROM MaterialOrcamento materialOrcamento
            WHERE materialOrcamento.orcamentoVersao.id IN :versaoIds
            GROUP BY materialOrcamento.orcamentoVersao.id
            """)
    List<CustoTotalMateriaisOrcamentoProjection> somarCustoTotalPorVersoes(
            @Param("versaoIds") Collection<Long> versaoIds);
}
