package br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.repository;

import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UnidadeMaoDeObraRepository extends JpaRepository<UnidadeMaoDeObra, Long> {

    List<UnidadeMaoDeObra> findByAtivoTrue();

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM unidade_mao_de_obra
                WHERE LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome))
            )
            """, nativeQuery = true)
    boolean existsByNomeNormalizado(@Param("nome") String nome);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM unidade_mao_de_obra
                WHERE LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome))
                  AND id <> :id
            )
            """, nativeQuery = true)
    boolean existsByNomeNormalizadoAndIdNot(@Param("nome") String nome, @Param("id") Long id);
}
