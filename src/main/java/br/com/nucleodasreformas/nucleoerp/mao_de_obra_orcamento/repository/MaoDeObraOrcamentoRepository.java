package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.entity.MaoDeObraOrcamento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MaoDeObraOrcamentoRepository extends JpaRepository<MaoDeObraOrcamento, Long> {

    @EntityGraph(attributePaths = "unidadeMaoDeObra")
    List<MaoDeObraOrcamento> findByOrcamento_IdOrderByIdAsc(Long orcamentoId);

    @EntityGraph(attributePaths = "unidadeMaoDeObra")
    Optional<MaoDeObraOrcamento> findByIdAndOrcamento_Id(Long id, Long orcamentoId);

    @Query("""
            SELECT new br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository.CustoTotalMaoDeObraOrcamentoProjection(
                maoDeObraOrcamento.orcamento.id,
                SUM(maoDeObraOrcamento.custoTotal)
            )
            FROM MaoDeObraOrcamento maoDeObraOrcamento
            WHERE maoDeObraOrcamento.orcamento.id IN :orcamentoIds
            GROUP BY maoDeObraOrcamento.orcamento.id
            """)
    List<CustoTotalMaoDeObraOrcamentoProjection> somarCustoTotalPorOrcamentos(
            @Param("orcamentoIds") Collection<Long> orcamentoIds);
}
