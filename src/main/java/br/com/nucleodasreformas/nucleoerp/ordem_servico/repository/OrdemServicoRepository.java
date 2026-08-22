package br.com.nucleodasreformas.nucleoerp.ordem_servico.repository;

import br.com.nucleodasreformas.nucleoerp.ordem_servico.entity.OrdemServico;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long>,
        JpaSpecificationExecutor<OrdemServico> {

    @Override
    @EntityGraph(attributePaths = {
            "statusOrdemServico",
            "orcamentoVersao",
            "orcamentoVersao.orcamento",
            "orcamentoVersao.orcamento.cliente"
    })
    Optional<OrdemServico> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {
            "statusOrdemServico",
            "orcamentoVersao",
            "orcamentoVersao.orcamento",
            "orcamentoVersao.orcamento.cliente"
    })
    List<OrdemServico> findAll(Specification<OrdemServico> spec, Sort sort);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "statusOrdemServico",
            "orcamentoVersao",
            "orcamentoVersao.orcamento",
            "orcamentoVersao.orcamento.cliente"
    })
    @Query("SELECT ordem FROM OrdemServico ordem WHERE ordem.id = :id")
    Optional<OrdemServico> findByIdForUpdate(@Param("id") Long id);

    boolean existsByOrcamentoVersao_Id(Long versaoId);
}
