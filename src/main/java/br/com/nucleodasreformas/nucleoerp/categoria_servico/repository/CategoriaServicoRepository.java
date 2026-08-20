package br.com.nucleodasreformas.nucleoerp.categoria_servico.repository;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoriaServicoRepository extends JpaRepository<CategoriaServico, Long> {

    List<CategoriaServico> findByAtivoTrue();

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM categoria_servico
                WHERE LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome))
            )
            """, nativeQuery = true)
    boolean existsByNomeNormalizado(@Param("nome") String nome);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM categoria_servico
                WHERE LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome))
                  AND id <> :id
            )
            """, nativeQuery = true)
    boolean existsByNomeNormalizadoAndIdNot(@Param("nome") String nome, @Param("id") Long id);
}
