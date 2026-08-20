package br.com.nucleodasreformas.nucleoerp.status_orcamento.repository;

import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StatusOrcamentoRepository extends JpaRepository<StatusOrcamento, Long> {

    List<StatusOrcamento> findByAtivoTrue();

    @Query(value = """
            SELECT *
            FROM status_orcamento
            WHERE LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome))
            """, nativeQuery = true)
    Optional<StatusOrcamento> findByNomeNormalizado(@Param("nome") String nome);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM status_orcamento
                WHERE LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome))
            )
            """, nativeQuery = true)
    boolean existsByNomeNormalizado(@Param("nome") String nome);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM status_orcamento
                WHERE LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome))
                  AND id <> :id
            )
            """, nativeQuery = true)
    boolean existsByNomeNormalizadoAndIdNot(@Param("nome") String nome, @Param("id") Long id);
}
