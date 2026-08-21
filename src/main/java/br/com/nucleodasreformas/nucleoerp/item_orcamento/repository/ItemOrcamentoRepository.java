package br.com.nucleodasreformas.nucleoerp.item_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.item_orcamento.entity.ItemOrcamento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemOrcamentoRepository extends JpaRepository<ItemOrcamento, Long> {

    @EntityGraph(attributePaths = "servico")
    List<ItemOrcamento> findByOrcamento_IdOrderByIdAsc(Long orcamentoId);

    @EntityGraph(attributePaths = "servico")
    Optional<ItemOrcamento> findByIdAndOrcamento_Id(Long id, Long orcamentoId);
}
