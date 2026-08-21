package br.com.nucleodasreformas.nucleoerp.material_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.material_orcamento.entity.MaterialOrcamento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialOrcamentoRepository extends JpaRepository<MaterialOrcamento, Long> {

    @EntityGraph(attributePaths = "material")
    List<MaterialOrcamento> findByOrcamento_IdOrderByIdAsc(Long orcamentoId);

    @EntityGraph(attributePaths = "material")
    Optional<MaterialOrcamento> findByIdAndOrcamento_Id(Long id, Long orcamentoId);
}
