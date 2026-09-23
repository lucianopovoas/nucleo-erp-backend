package br.com.nucleodasreformas.nucleoerp.cliente.repository;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCpf(String cpf);

    Optional<Cliente> findByCnpj(String cnpj);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    boolean existsByTelefone(String telefone);

    boolean existsByTelefoneAndIdNot(String telefone, Long id);

    boolean existsByCelular(String celular);

    boolean existsByCelularAndIdNot(String celular, Long id);

    boolean existsByEmail(String Email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Page<Cliente> findAllByAtivoTrue(Pageable pageable);
}
