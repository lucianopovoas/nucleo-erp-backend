package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.entity.MaoDeObraOrcamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MaoDeObraOrcamentoRepository extends JpaRepository<MaoDeObraOrcamento, Long> {

    @EntityGraph(attributePaths = "unidadeMaoDeObra")
    List<MaoDeObraOrcamento> findByOrcamentoVersao_IdOrderByIdAsc(Long orcamentoVersaoId);

    @EntityGraph(attributePaths = "unidadeMaoDeObra")
    Optional<MaoDeObraOrcamento> findByIdAndOrcamentoVersao_Id(Long id, Long orcamentoVersaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "unidadeMaoDeObra")
    @Query("""
            SELECT linha FROM MaoDeObraOrcamento linha
            WHERE linha.id = :id AND linha.orcamentoVersao.id = :versaoId
            """)
    Optional<MaoDeObraOrcamento> findByIdAndOrcamentoVersaoIdForUpdate(
            @Param("id") Long id, @Param("versaoId") Long versaoId);

    @Query("""
            SELECT new br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository.CustoTotalMaoDeObraOrcamentoProjection(
                maoDeObraOrcamento.orcamentoVersao.id,
                SUM(maoDeObraOrcamento.custoTotal)
            )
            FROM MaoDeObraOrcamento maoDeObraOrcamento
            WHERE maoDeObraOrcamento.orcamentoVersao.id IN :versaoIds
            GROUP BY maoDeObraOrcamento.orcamentoVersao.id
            """)
    List<CustoTotalMaoDeObraOrcamentoProjection> somarCustoTotalPorVersoes(
            @Param("versaoIds") Collection<Long> versaoIds);
}
