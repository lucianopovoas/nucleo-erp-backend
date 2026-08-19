package br.com.nucleodasreformas.nucleoerp.materialfornecedor.repository;

import br.com.nucleodasreformas.nucleoerp.materialfornecedor.entity.MaterialFornecedor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialFornecedorRepository extends JpaRepository<MaterialFornecedor, Long> {

    @Override
    @EntityGraph(attributePaths = {"material", "fornecedor"})
    Optional<MaterialFornecedor> findById(Long id);

    @EntityGraph(attributePaths = {"material", "fornecedor"})
    List<MaterialFornecedor> findByAtivoTrue();

    Optional<MaterialFornecedor> findByMaterialIdAndFornecedorId(Long materialId, Long fornecedorId);

    boolean existsByMaterialIdAndFornecedorIdAndIdNot(Long materialId, Long fornecedorId, Long id);
}
