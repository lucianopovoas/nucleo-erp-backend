package br.com.nucleodasreformas.nucleoerp.material.repository;

import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByNome(String nome);

}
