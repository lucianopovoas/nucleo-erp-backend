package br.com.nucleodasreformas.nucleoerp.orcamento.service;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.mapper.OrcamentoMapper;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.mapper.OrcamentoVersaoMapper;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.repository.OrcamentoVersaoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoPolicy;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrcamentoService {

    private final OrcamentoRepository repository;
    private final OrcamentoVersaoRepository versaoRepository;
    private final ClienteRepository clienteRepository;
    private final StatusOrcamentoRepository statusRepository;

    public OrcamentoResponse salvar(OrcamentoRequest request) {
        Cliente cliente = buscarClienteAtivo(request.getClienteId());
        StatusOrcamento rascunho = buscarStatusAtivoPorCodigo(OrcamentoVersaoPolicy.RASCUNHO);

        Orcamento orcamento = repository.saveAndFlush(OrcamentoMapper.toEntity(cliente));
        OrcamentoVersao versao = versaoRepository.saveAndFlush(
                OrcamentoVersaoMapper.toEntity(orcamento, 1, rascunho, request.getObservacao()));
        orcamento.setVersaoAtual(versao);
        return OrcamentoMapper.toResponse(repository.saveAndFlush(orcamento));
    }

    @Transactional(readOnly = true)
    public OrcamentoResponse buscarPorId(Long id) {
        return OrcamentoMapper.toResponse(buscarOrcamento(id));
    }

    @Transactional(readOnly = true)
    public List<OrcamentoResponse> listar() {
        return repository.findAll().stream().map(OrcamentoMapper::toResponse).toList();
    }

    public OrcamentoResponse atualizar(Long id, OrcamentoUpdateRequest request) {
        Orcamento orcamento = repository.findByIdForUpdate(id)
                .orElseThrow(() -> orcamentoNaoEncontrado(id));

        if (orcamento.getCliente().getId().equals(request.getClienteId())) {
            return OrcamentoMapper.toResponse(orcamento);
        }

        Long versaoAtualId = orcamento.getVersaoAtual() == null
                ? null : orcamento.getVersaoAtual().getId();
        OrcamentoVersao atual = versaoAtualId == null ? null : versaoRepository
                .findByIdAndOrcamentoIdForUpdate(versaoAtualId, id)
                .orElseThrow(() -> new BusinessException(
                        "A versão atual do orçamento está inconsistente."));
        boolean estadoInicial = atual != null
                && atual.getNumeroVersao() == 1
                && OrcamentoVersaoPolicy.RASCUNHO.equals(atual.getStatusOrcamento().getCodigo())
                && versaoRepository.countByOrcamento_Id(id) == 1;
        if (!estadoInicial) {
            throw new BusinessException(
                    "O cliente não pode ser alterado depois que a V1 deixa o estado RASCUNHO.");
        }

        orcamento.setCliente(buscarClienteAtivo(request.getClienteId()));
        return OrcamentoMapper.toResponse(repository.saveAndFlush(orcamento));
    }

    private Cliente buscarClienteAtivo(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado. Id: " + id));
        if (!Boolean.TRUE.equals(cliente.getAtivo())) {
            throw new BusinessException("Não é possível vincular um orçamento a um cliente inativo.");
        }
        return cliente;
    }

    private StatusOrcamento buscarStatusAtivoPorCodigo(String codigo) {
        StatusOrcamento status = statusRepository.findByCodigo(codigo)
                .orElseThrow(() -> new BusinessException(
                        "O status inicial '" + codigo + "' não está cadastrado."));
        if (!Boolean.TRUE.equals(status.getAtivo())) {
            throw new BusinessException("O status inicial '" + codigo + "' está inativo.");
        }
        return status;
    }

    private Orcamento buscarOrcamento(Long id) {
        return repository.findById(id).orElseThrow(() -> orcamentoNaoEncontrado(id));
    }

    private ResourceNotFoundException orcamentoNaoEncontrado(Long id) {
        return new ResourceNotFoundException("Orçamento não encontrado. Id: " + id);
    }
}
