package br.com.nucleodasreformas.nucleoerp.orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "statusOrcamento"})
    Optional<Orcamento> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"cliente", "statusOrcamento"})
    List<Orcamento> findAll();
}
