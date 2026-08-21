package br.com.nucleodasreformas.nucleoerp.orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "versaoAtual", "versaoAtual.statusOrcamento"})
    Optional<Orcamento> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"cliente", "versaoAtual", "versaoAtual.statusOrcamento"})
    List<Orcamento> findAll();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT orcamento FROM Orcamento orcamento WHERE orcamento.id = :id")
    Optional<Orcamento> findByIdForUpdate(@Param("id") Long id);
}
