package br.com.nucleodasreformas.nucleoerp.orcamento_versao.service;

import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.repository.OrcamentoVersaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrcamentoVersaoGuard {

    private final OrcamentoRepository orcamentoRepository;
    private final OrcamentoVersaoRepository versaoRepository;
    private final OrcamentoVersaoPolicy policy;

    public ContextoOrcamentoVersao bloquear(Long orcamentoId, Long versaoId) {
        Orcamento orcamento = orcamentoRepository.findByIdForUpdate(orcamentoId)
                .orElseThrow(() -> orcamentoNaoEncontrado(orcamentoId));
        OrcamentoVersao versao = versaoRepository
                .findByIdAndOrcamentoIdForUpdate(versaoId, orcamentoId)
                .orElseThrow(() -> versaoNaoEncontrada(orcamentoId, versaoId));
        return new ContextoOrcamentoVersao(orcamento, versao);
    }

    public OrcamentoVersao bloquearEditavel(Long orcamentoId, Long versaoId) {
        ContextoOrcamentoVersao contexto = bloquear(orcamentoId, versaoId);
        policy.garantirEditavel(contexto.orcamento(), contexto.versao());
        return contexto.versao();
    }

    public OrcamentoVersao buscar(Long orcamentoId, Long versaoId) {
        if (!orcamentoRepository.existsById(orcamentoId)) {
            throw orcamentoNaoEncontrado(orcamentoId);
        }
        return versaoRepository.findByIdAndOrcamento_Id(versaoId, orcamentoId)
                .orElseThrow(() -> versaoNaoEncontrada(orcamentoId, versaoId));
    }

    private ResourceNotFoundException orcamentoNaoEncontrado(Long id) {
        return new ResourceNotFoundException("Orçamento não encontrado. Id: " + id);
    }

    private ResourceNotFoundException versaoNaoEncontrada(Long orcamentoId, Long versaoId) {
        return new ResourceNotFoundException(
                "Versão de orçamento não encontrada. Id: " + versaoId
                        + ", orçamento: " + orcamentoId);
    }
}
