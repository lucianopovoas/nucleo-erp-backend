package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.entity.DespesaOrcamento;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.mapper.DespesaOrcamentoMapper;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository.DespesaOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DespesaOrcamentoService {

    private static final String MENSAGEM_DESCRICAO_INVALIDA =
            "A descrição informada não pode ser nula ou vazia.";
    private static final String MENSAGEM_VALOR_INVALIDO =
            "O valor não pode ser negativo.";
    private static final String MENSAGEM_ESCALA_VALOR_INVALIDA =
            "O valor deve ter no máximo 2 casas decimais.";

    private final DespesaOrcamentoRepository repository;
    private final OrcamentoRepository orcamentoRepository;

    public DespesaOrcamentoResponse salvar(
            Long orcamentoId,
            DespesaOrcamentoRequest request) {
        Orcamento orcamento = buscarOrcamento(orcamentoId);
        String descricao = validarENormalizarDescricao(request.getDescricao());
        BigDecimal valor = validarValor(request.getValor());

        DespesaOrcamento despesaOrcamento = DespesaOrcamentoMapper.toEntity(
                orcamento, descricao, valor);
        return DespesaOrcamentoMapper.toResponse(
                repository.saveAndFlush(despesaOrcamento));
    }

    @Transactional(readOnly = true)
    public DespesaOrcamentoResponse buscarPorId(
            Long orcamentoId,
            Long despesaOrcamentoId) {
        return DespesaOrcamentoMapper.toResponse(
                buscarDespesaOrcamento(orcamentoId, despesaOrcamentoId));
    }

    @Transactional(readOnly = true)
    public List<DespesaOrcamentoResponse> listar(Long orcamentoId) {
        garantirOrcamentoExistente(orcamentoId);
        return repository.findByOrcamento_IdOrderByIdAsc(orcamentoId)
                .stream()
                .map(DespesaOrcamentoMapper::toResponse)
                .toList();
    }

    public DespesaOrcamentoResponse atualizar(
            Long orcamentoId,
            Long despesaOrcamentoId,
            DespesaOrcamentoUpdateRequest request) {
        DespesaOrcamento despesaOrcamento =
                buscarDespesaOrcamento(orcamentoId, despesaOrcamentoId);
        String descricao = request.isDescricaoInformada()
                ? validarENormalizarDescricao(request.getDescricao())
                : despesaOrcamento.getDescricao();
        BigDecimal valor = request.getValor() != null
                ? validarValor(request.getValor())
                : despesaOrcamento.getValor();

        DespesaOrcamentoMapper.updateEntity(despesaOrcamento, descricao, valor);
        return DespesaOrcamentoMapper.toResponse(
                repository.saveAndFlush(despesaOrcamento));
    }

    public void deletar(Long orcamentoId, Long despesaOrcamentoId) {
        DespesaOrcamento despesaOrcamento =
                buscarDespesaOrcamento(orcamentoId, despesaOrcamentoId);
        repository.delete(despesaOrcamento);
    }

    private String validarENormalizarDescricao(String descricao) {
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new BusinessException(MENSAGEM_DESCRICAO_INVALIDA);
        }
        return descricao.trim();
    }

    private BigDecimal validarValor(BigDecimal valor) {
        if (valor == null) {
            throw new BusinessException("O valor é obrigatório.");
        }
        if (valor.signum() < 0) {
            throw new BusinessException(MENSAGEM_VALOR_INVALIDO);
        }
        if (valor.scale() > 2) {
            throw new BusinessException(MENSAGEM_ESCALA_VALOR_INVALIDA);
        }
        return valor;
    }

    private Orcamento buscarOrcamento(Long id) {
        return orcamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Orçamento não encontrado. Id: " + id));
    }

    private void garantirOrcamentoExistente(Long id) {
        if (!orcamentoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Orçamento não encontrado. Id: " + id);
        }
    }

    private DespesaOrcamento buscarDespesaOrcamento(
            Long orcamentoId,
            Long despesaOrcamentoId) {
        return repository.findByIdAndOrcamento_Id(despesaOrcamentoId, orcamentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Despesa do orçamento não encontrada. Id: " + despesaOrcamentoId
                                + ", orçamento: " + orcamentoId));
    }
}
