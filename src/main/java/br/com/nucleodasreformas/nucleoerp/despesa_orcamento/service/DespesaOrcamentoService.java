package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.entity.DespesaOrcamento;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.mapper.DespesaOrcamentoMapper;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository.DespesaOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DespesaOrcamentoService {

    private final DespesaOrcamentoRepository repository;
    private final OrcamentoVersaoGuard versaoGuard;

    public DespesaOrcamentoResponse salvar(
            Long orcamentoId, Long versaoId, DespesaOrcamentoRequest request) {
        OrcamentoVersao versao = versaoGuard.bloquearEditavel(orcamentoId, versaoId);
        DespesaOrcamento despesa = DespesaOrcamentoMapper.toEntity(
                versao, validarDescricao(request.getDescricao()), validarValor(request.getValor()));
        return DespesaOrcamentoMapper.toResponse(repository.saveAndFlush(despesa));
    }

    @Transactional(readOnly = true)
    public DespesaOrcamentoResponse buscarPorId(
            Long orcamentoId, Long versaoId, Long linhaId) {
        versaoGuard.buscar(orcamentoId, versaoId);
        return DespesaOrcamentoMapper.toResponse(buscarLinha(versaoId, linhaId));
    }

    @Transactional(readOnly = true)
    public List<DespesaOrcamentoResponse> listar(Long orcamentoId, Long versaoId) {
        versaoGuard.buscar(orcamentoId, versaoId);
        return repository.findByOrcamentoVersao_IdOrderByIdAsc(versaoId).stream()
                .map(DespesaOrcamentoMapper::toResponse).toList();
    }

    public DespesaOrcamentoResponse atualizar(
            Long orcamentoId, Long versaoId, Long linhaId,
            DespesaOrcamentoUpdateRequest request) {
        versaoGuard.bloquearEditavel(orcamentoId, versaoId);
        DespesaOrcamento despesa = buscarLinhaParaAtualizar(versaoId, linhaId);
        String descricao = request.isDescricaoInformada()
                ? validarDescricao(request.getDescricao()) : despesa.getDescricao();
        BigDecimal valor = request.getValor() != null
                ? validarValor(request.getValor()) : despesa.getValor();
        DespesaOrcamentoMapper.updateEntity(despesa, descricao, valor);
        return DespesaOrcamentoMapper.toResponse(repository.saveAndFlush(despesa));
    }

    public void deletar(Long orcamentoId, Long versaoId, Long linhaId) {
        versaoGuard.bloquearEditavel(orcamentoId, versaoId);
        repository.delete(buscarLinhaParaAtualizar(versaoId, linhaId));
    }

    private String validarDescricao(String descricao) {
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new BusinessException("A descrição informada não pode ser nula ou vazia.");
        }
        return descricao.trim();
    }

    private BigDecimal validarValor(BigDecimal valor) {
        if (valor == null) {
            throw new BusinessException("O valor é obrigatório.");
        }
        if (valor.signum() < 0) {
            throw new BusinessException("O valor não pode ser negativo.");
        }
        if (valor.scale() > 2) {
            throw new BusinessException("O valor deve ter no máximo 2 casas decimais.");
        }
        return valor;
    }

    private DespesaOrcamento buscarLinha(Long versaoId, Long linhaId) {
        return repository.findByIdAndOrcamentoVersao_Id(linhaId, versaoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Despesa do orçamento não encontrada. Id: " + linhaId
                                + ", versão: " + versaoId));
    }

    private DespesaOrcamento buscarLinhaParaAtualizar(Long versaoId, Long linhaId) {
        return repository.findByIdAndOrcamentoVersaoIdForUpdate(linhaId, versaoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Despesa do orçamento não encontrada. Id: " + linhaId
                                + ", versão: " + versaoId));
    }
}
