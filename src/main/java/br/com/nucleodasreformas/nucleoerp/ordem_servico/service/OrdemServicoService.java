package br.com.nucleodasreformas.nucleoerp.ordem_servico.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.ContextoOrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoGuard;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoStatusRequest;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.entity.OrdemServico;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.mapper.OrdemServicoMapper;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.repository.OrdemServicoRepository;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.repository.StatusOrdemServicoRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrdemServicoService {

    private static final String CONSTRAINT_ORIGEM = "uk_ordem_servico_orcamento_versao";
    private static final String CONSTRAINT_NUMERO = "uk_ordem_servico_numero";
    private static final String MENSAGEM_ORIGEM_DUPLICADA =
            "Já existe uma ordem de serviço para esta versão de orçamento.";

    private final OrdemServicoRepository repository;
    private final StatusOrdemServicoRepository statusRepository;
    private final OrcamentoVersaoGuard versaoGuard;
    private final OrdemServicoPolicy policy;

    public OrdemServicoResponse salvar(Long orcamentoId, Long versaoId) {
        ContextoOrcamentoVersao contexto = versaoGuard.bloquear(orcamentoId, versaoId);
        policy.garantirOrigemAprovada(contexto.orcamento(), contexto.versao());

        if (repository.existsByOrcamentoVersao_Id(versaoId)) {
            throw new BusinessException(MENSAGEM_ORIGEM_DUPLICADA);
        }

        StatusOrdemServico statusInicial = buscarStatusInicialAtivo();
        OrdemServico ordemServico = OrdemServicoMapper.toEntity(
                contexto.versao(), statusInicial);
        return OrdemServicoMapper.toResponse(salvarComTratamentoDeConflito(ordemServico));
    }

    @Transactional(readOnly = true)
    public OrdemServicoResponse buscarPorId(Long id) {
        return OrdemServicoMapper.toResponse(buscarOrdemServico(id));
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listar() {
        return repository.findAllByOrderByNumeroAsc().stream()
                .map(OrdemServicoMapper::toResponse)
                .toList();
    }

    public OrdemServicoResponse atualizar(Long id, OrdemServicoUpdateRequest request) {
        OrdemServico ordemServico = buscarOrdemServicoParaAtualizar(id);
        policy.garantirObservacaoEditavel(ordemServico);
        if (request.isObservacaoInformada()) {
            ordemServico.setObservacao(request.getObservacao());
        }
        return OrdemServicoMapper.toResponse(repository.saveAndFlush(ordemServico));
    }

    public OrdemServicoResponse alterarStatus(
            Long id, OrdemServicoStatusRequest request) {
        OrdemServico ordemServico = buscarOrdemServicoParaAtualizar(id);
        StatusOrdemServico atual = ordemServico.getStatusOrdemServico();
        if (atual.getId().equals(request.getStatusOrdemServicoId())) {
            return OrdemServicoMapper.toResponse(ordemServico);
        }

        StatusOrdemServico destino = statusRepository.findById(request.getStatusOrdemServicoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status de ordem de serviço não encontrado. Id: "
                                + request.getStatusOrdemServicoId()));
        if (!Boolean.TRUE.equals(destino.getAtivo())) {
            throw new BusinessException(
                    "Não é possível selecionar um status de ordem de serviço inativo.");
        }
        policy.validarTransicao(atual.getCodigo(), destino.getCodigo());
        ordemServico.setStatusOrdemServico(destino);
        return OrdemServicoMapper.toResponse(repository.saveAndFlush(ordemServico));
    }

    private StatusOrdemServico buscarStatusInicialAtivo() {
        StatusOrdemServico status = statusRepository
                .findByCodigo(OrdemServicoPolicy.COMPRAR_MATERIAL)
                .orElseThrow(() -> new BusinessException(
                        "O status inicial 'COMPRAR_MATERIAL' não está cadastrado."));
        if (!Boolean.TRUE.equals(status.getAtivo())) {
            throw new BusinessException("O status inicial 'COMPRAR_MATERIAL' está inativo.");
        }
        return status;
    }

    private OrdemServico salvarComTratamentoDeConflito(OrdemServico ordemServico) {
        try {
            return repository.saveAndFlush(ordemServico);
        } catch (DataIntegrityViolationException ex) {
            String constraint = buscarConstraint(ex);
            if (CONSTRAINT_ORIGEM.equals(constraint)) {
                throw new BusinessException(MENSAGEM_ORIGEM_DUPLICADA);
            }
            if (CONSTRAINT_NUMERO.equals(constraint)) {
                throw new BusinessException("Já existe uma ordem de serviço com esse número.");
            }
            throw ex;
        }
    }

    private String buscarConstraint(Throwable throwable) {
        Throwable causa = throwable;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violacao) {
                return violacao.getConstraintName();
            }
            causa = causa.getCause();
        }
        return null;
    }

    private OrdemServico buscarOrdemServico(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ordemServicoNaoEncontrada(id));
    }

    private OrdemServico buscarOrdemServicoParaAtualizar(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> ordemServicoNaoEncontrada(id));
    }

    private ResourceNotFoundException ordemServicoNaoEncontrada(Long id) {
        return new ResourceNotFoundException("Ordem de serviço não encontrada. Id: " + id);
    }
}
