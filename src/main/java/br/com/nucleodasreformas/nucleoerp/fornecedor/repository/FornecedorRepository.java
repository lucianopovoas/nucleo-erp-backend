package br.com.nucleodasreformas.nucleoerp.fornecedor.repository;

import br.com.nucleodasreformas.nucleoerp.fornecedor.entity.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    Optional<Fornecedor> findByNome(String nome);

}
