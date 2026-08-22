package br.com.nucleodasreformas.nucleoerp.ordem_servico.repository;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.entity.OrdemServico;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class OrdemServicoSpecifications {

    private OrdemServicoSpecifications() {
    }

    public static Specification<OrdemServico> comFiltros(
            Long numero,
            String status,
            Long clienteId,
            Long orcamentoId,
            LocalDateTime criadoDe,
            LocalDateTime criadoAteExclusivo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (numero != null) {
                predicates.add(criteriaBuilder.equal(root.get("numero"), numero));
            }

            if (status != null) {
                Join<OrdemServico, StatusOrdemServico> statusJoin =
                        root.join("statusOrdemServico");
                predicates.add(criteriaBuilder.equal(statusJoin.get("codigo"), status));
            }

            Join<OrdemServico, OrcamentoVersao> versaoJoin = null;
            Join<OrcamentoVersao, Orcamento> orcamentoJoin = null;
            if (clienteId != null || orcamentoId != null) {
                versaoJoin = root.join("orcamentoVersao");
                orcamentoJoin = versaoJoin.join("orcamento");
            }

            if (clienteId != null) {
                Join<Orcamento, Cliente> clienteJoin = orcamentoJoin.join("cliente");
                predicates.add(criteriaBuilder.equal(clienteJoin.get("id"), clienteId));
            }

            if (orcamentoId != null) {
                predicates.add(criteriaBuilder.equal(orcamentoJoin.get("id"), orcamentoId));
            }

            if (criadoDe != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("criadoEm"), criadoDe));
            }

            if (criadoAteExclusivo != null) {
                predicates.add(criteriaBuilder.lessThan(
                        root.get("criadoEm"), criadoAteExclusivo));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
