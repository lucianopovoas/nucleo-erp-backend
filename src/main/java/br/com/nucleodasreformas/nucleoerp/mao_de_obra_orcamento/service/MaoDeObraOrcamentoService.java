package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.entity.MaoDeObraOrcamento;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.mapper.MaoDeObraOrcamentoMapper;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository.MaoDeObraOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.repository.UnidadeMaoDeObraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MaoDeObraOrcamentoService {

    private static final String MENSAGEM_UNIDADE_INATIVA =
            "Não é possível vincular uma unidade de mão de obra inativa ao orçamento.";
    private static final String MENSAGEM_DESCRICAO_INVALIDA =
            "A descrição informada não pode ser nula ou vazia.";

    private final MaoDeObraOrcamentoRepository repository;
    private final OrcamentoRepository orcamentoRepository;
    private final UnidadeMaoDeObraRepository unidadeMaoDeObraRepository;

    public MaoDeObraOrcamentoResponse salvar(
            Long orcamentoId,
            MaoDeObraOrcamentoRequest request) {
        Orcamento orcamento = buscarOrcamento(orcamentoId);
        UnidadeMaoDeObra unidadeMaoDeObra =
                buscarUnidadeMaoDeObraAtiva(request.getUnidadeMaoDeObraId());
        String descricao = validarENormalizarDescricao(request.getDescricao());
        BigDecimal custoTotal = calcularCustoTotal(
                request.getQuantidade(), request.getCustoUnitario());

        MaoDeObraOrcamento maoDeObraOrcamento = MaoDeObraOrcamentoMapper.toEntity(
                orcamento,
                unidadeMaoDeObra,
                descricao,
                unidadeMaoDeObra.getNome(),
                request.getQuantidade(),
                request.getCustoUnitario(),
                custoTotal);

        return MaoDeObraOrcamentoMapper.toResponse(
                repository.saveAndFlush(maoDeObraOrcamento));
    }

    @Transactional(readOnly = true)
    public MaoDeObraOrcamentoResponse buscarPorId(
            Long orcamentoId,
            Long maoDeObraOrcamentoId) {
        return MaoDeObraOrcamentoMapper.toResponse(
                buscarMaoDeObraOrcamento(orcamentoId, maoDeObraOrcamentoId));
    }

    @Transactional(readOnly = true)
    public List<MaoDeObraOrcamentoResponse> listar(Long orcamentoId) {
        garantirOrcamentoExistente(orcamentoId);
        return repository.findByOrcamento_IdOrderByIdAsc(orcamentoId)
                .stream()
                .map(MaoDeObraOrcamentoMapper::toResponse)
                .toList();
    }

    public MaoDeObraOrcamentoResponse atualizar(
            Long orcamentoId,
            Long maoDeObraOrcamentoId,
            MaoDeObraOrcamentoUpdateRequest request) {
        MaoDeObraOrcamento maoDeObraOrcamento =
                buscarMaoDeObraOrcamento(orcamentoId, maoDeObraOrcamentoId);
        UnidadeMaoDeObra unidadeAtual = maoDeObraOrcamento.getUnidadeMaoDeObra();
        boolean unidadeAlterada = request.getUnidadeMaoDeObraId() != null
                && !request.getUnidadeMaoDeObraId().equals(unidadeAtual.getId());
        UnidadeMaoDeObra unidadeMaoDeObra = unidadeAlterada
                ? buscarUnidadeMaoDeObraAtiva(request.getUnidadeMaoDeObraId())
                : unidadeAtual;

        String descricao = request.isDescricaoInformada()
                ? validarENormalizarDescricao(request.getDescricao())
                : maoDeObraOrcamento.getDescricao();
        String unidade = unidadeAlterada
                ? unidadeMaoDeObra.getNome()
                : maoDeObraOrcamento.getUnidade();
        BigDecimal quantidade = request.getQuantidade() != null
                ? request.getQuantidade()
                : maoDeObraOrcamento.getQuantidade();
        BigDecimal custoUnitario = request.getCustoUnitario() != null
                ? request.getCustoUnitario()
                : maoDeObraOrcamento.getCustoUnitario();
        BigDecimal custoTotal = calcularCustoTotal(quantidade, custoUnitario);

        MaoDeObraOrcamentoMapper.updateEntity(
                maoDeObraOrcamento,
                unidadeMaoDeObra,
                descricao,
                unidade,
                quantidade,
                custoUnitario,
                custoTotal);
        return MaoDeObraOrcamentoMapper.toResponse(
                repository.saveAndFlush(maoDeObraOrcamento));
    }

    public void deletar(Long orcamentoId, Long maoDeObraOrcamentoId) {
        MaoDeObraOrcamento maoDeObraOrcamento =
                buscarMaoDeObraOrcamento(orcamentoId, maoDeObraOrcamentoId);
        repository.delete(maoDeObraOrcamento);
    }

    private String validarENormalizarDescricao(String descricao) {
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new BusinessException(MENSAGEM_DESCRICAO_INVALIDA);
        }
        return descricao.trim();
    }

    private BigDecimal calcularCustoTotal(BigDecimal quantidade, BigDecimal custoUnitario) {
        validarValores(quantidade, custoUnitario);
        return quantidade.multiply(custoUnitario).setScale(2, RoundingMode.HALF_UP);
    }

    private void validarValores(BigDecimal quantidade, BigDecimal custoUnitario) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new BusinessException("A quantidade deve ser maior que zero.");
        }
        if (custoUnitario == null || custoUnitario.signum() < 0) {
            throw new BusinessException("O custo unitário não pode ser negativo.");
        }
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

    private UnidadeMaoDeObra buscarUnidadeMaoDeObraAtiva(Long id) {
        UnidadeMaoDeObra unidadeMaoDeObra = unidadeMaoDeObraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unidade de mão de obra não encontrada. Id: " + id));

        if (!Boolean.TRUE.equals(unidadeMaoDeObra.getAtivo())) {
            throw new BusinessException(MENSAGEM_UNIDADE_INATIVA);
        }
        return unidadeMaoDeObra;
    }

    private MaoDeObraOrcamento buscarMaoDeObraOrcamento(
            Long orcamentoId,
            Long maoDeObraOrcamentoId) {
        return repository.findByIdAndOrcamento_Id(maoDeObraOrcamentoId, orcamentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mão de obra do orçamento não encontrada. Id: " + maoDeObraOrcamentoId
                                + ", orçamento: " + orcamentoId));
    }
}
