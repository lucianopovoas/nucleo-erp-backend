package br.com.nucleodasreformas.nucleoerp.orcamento_versao.repository;

import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrcamentoVersaoRepository extends JpaRepository<OrcamentoVersao, Long> {

    @EntityGraph(attributePaths = "statusOrcamento")
    List<OrcamentoVersao> findByOrcamento_IdOrderByNumeroVersaoAsc(Long orcamentoId);

    @EntityGraph(attributePaths = "statusOrcamento")
    Optional<OrcamentoVersao> findByIdAndOrcamento_Id(Long id, Long orcamentoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "statusOrcamento")
    @Query("""
            SELECT versao
            FROM OrcamentoVersao versao
            WHERE versao.id = :versaoId
              AND versao.orcamento.id = :orcamentoId
            """)
    Optional<OrcamentoVersao> findByIdAndOrcamentoIdForUpdate(
            @Param("versaoId") Long versaoId,
            @Param("orcamentoId") Long orcamentoId);

    boolean existsByOrcamento_IdAndStatusOrcamento_Codigo(Long orcamentoId, String codigo);

    long countByOrcamento_Id(Long orcamentoId);
}
