package br.com.nucleodasreformas.nucleoerp.servico.repository;

import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    @Override
    @EntityGraph(attributePaths = "categoriaServico")
    Optional<Servico> findById(Long id);

    @EntityGraph(attributePaths = "categoriaServico")
    List<Servico> findByAtivoTrue();

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM servico
                WHERE categoria_servico_id = :categoriaServicoId
                  AND LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome))
            )
            """, nativeQuery = true)
    boolean existsByCategoriaENomeNormalizado(
            @Param("categoriaServicoId") Long categoriaServicoId,
            @Param("nome") String nome);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM servico
                WHERE categoria_servico_id = :categoriaServicoId
                  AND LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome))
                  AND id <> :id
            )
            """, nativeQuery = true)
    boolean existsByCategoriaENomeNormalizadoAndIdNot(
            @Param("categoriaServicoId") Long categoriaServicoId,
            @Param("nome") String nome,
            @Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Servico s
            SET s.ativo = false
            WHERE s.categoriaServico.id = :categoriaServicoId
              AND s.ativo = true
            """)
    int inativarAtivosPorCategoriaId(@Param("categoriaServicoId") Long categoriaServicoId);
}
