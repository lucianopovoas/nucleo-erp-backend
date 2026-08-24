package br.com.nucleodasreformas.nucleoerp.ordem_servico.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.ContextoOrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoGuard;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoFiltroRequest;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoAcoesPermitidasResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.StatusOrdemServicoResumoResponse;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

import static br.com.nucleodasreformas.nucleoerp.ordem_servico.repository.OrdemServicoSpecifications.comFiltros;

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
        return montarResponse(salvarComTratamentoDeConflito(ordemServico));
    }

    @Transactional(readOnly = true)
    public OrdemServicoResponse buscarPorId(Long id) {
        return montarResponse(buscarOrdemServico(id));
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listar(OrdemServicoFiltroRequest filtros) {
        validarIntervalo(filtros);

        String statusNormalizado = filtros.getStatus() == null
                ? null
                : filtros.getStatus().trim().toUpperCase(Locale.ROOT);

        var specification = comFiltros(
                filtros.getNumero(),
                statusNormalizado,
                filtros.getClienteId(),
                filtros.getOrcamentoId(),
                filtros.getCriadoDe() == null ? null : filtros.getCriadoDe().atStartOfDay(),
                filtros.getCriadoAte() == null
                        ? null
                        : filtros.getCriadoAte().plusDays(1).atStartOfDay());

        List<StatusOrdemServico> statusAtivos = statusRepository.findByAtivoTrue();
        return repository.findAll(specification, Sort.by(Sort.Direction.ASC, "numero")).stream()
                .map(ordem -> montarResponse(ordem, statusAtivos))
                .toList();
    }

    public OrdemServicoResponse atualizar(Long id, OrdemServicoUpdateRequest request) {
        OrdemServico ordemServico = buscarOrdemServicoParaAtualizar(id);
        policy.garantirObservacaoEditavel(ordemServico);
        if (request.isObservacaoInformada()) {
            ordemServico.setObservacao(request.getObservacao());
        }
        return montarResponse(repository.saveAndFlush(ordemServico));
    }

    public OrdemServicoResponse alterarStatus(
            Long id, OrdemServicoStatusRequest request) {
        OrdemServico ordemServico = buscarOrdemServicoParaAtualizar(id);
        StatusOrdemServico atual = ordemServico.getStatusOrdemServico();
        if (atual.getId().equals(request.getStatusOrdemServicoId())) {
            return montarResponse(ordemServico);
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
        return montarResponse(repository.saveAndFlush(ordemServico));
    }

    private OrdemServicoResponse montarResponse(OrdemServico ordemServico) {
        return montarResponse(ordemServico, statusRepository.findByAtivoTrue());
    }

    private OrdemServicoResponse montarResponse(
            OrdemServico ordemServico,
            List<StatusOrdemServico> statusAtivos) {
        String codigoAtual = ordemServico.getStatusOrdemServico().getCodigo();
        List<StatusOrdemServicoResumoResponse> transicoes = policy.destinosPermitidos(codigoAtual)
                .stream()
                .map(codigo -> statusAtivos.stream()
                        .filter(status -> codigo.equals(status.getCodigo()))
                        .findFirst()
                        .map(status -> StatusOrdemServicoResumoResponse.builder()
                                .id(status.getId())
                                .codigo(status.getCodigo())
                                .nome(status.getNome())
                                .build())
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        return OrdemServicoMapper.toResponse(
                ordemServico,
                OrdemServicoAcoesPermitidasResponse.builder()
                        .editarObservacao(policy.podeEditarObservacao(codigoAtual))
                        .alterarStatusPara(transicoes)
                        .build());
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

    private void validarIntervalo(OrdemServicoFiltroRequest filtros) {
        if (filtros.getCriadoDe() != null
                && filtros.getCriadoAte() != null
                && filtros.getCriadoDe().isAfter(filtros.getCriadoAte())) {
            throw new BusinessException(
                    "A data inicial de criação não pode ser posterior à data final.");
        }
    }
}
