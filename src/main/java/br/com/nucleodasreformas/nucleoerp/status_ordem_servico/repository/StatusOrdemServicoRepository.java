package br.com.nucleodasreformas.nucleoerp.status_ordem_servico.repository;

import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StatusOrdemServicoRepository extends JpaRepository<StatusOrdemServico, Long> {

    List<StatusOrdemServico> findByAtivoTrue();

    Optional<StatusOrdemServico> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM status_ordem_servico
                WHERE LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome))
            )
            """, nativeQuery = true)
    boolean existsByNomeNormalizado(@Param("nome") String nome);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM status_ordem_servico
                WHERE LOWER(BTRIM(nome)) = LOWER(BTRIM(:nome))
                  AND id <> :id
            )
            """, nativeQuery = true)
    boolean existsByNomeNormalizadoAndIdNot(@Param("nome") String nome, @Param("id") Long id);
}
