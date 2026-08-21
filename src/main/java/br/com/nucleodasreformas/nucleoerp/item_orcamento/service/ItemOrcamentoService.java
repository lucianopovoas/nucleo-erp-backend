package br.com.nucleodasreformas.nucleoerp.item_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.entity.ItemOrcamento;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.mapper.ItemOrcamentoMapper;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.ItemOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import br.com.nucleodasreformas.nucleoerp.servico.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemOrcamentoService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final String MENSAGEM_SERVICO_INATIVO =
            "Não é possível vincular um item de orçamento a um serviço inativo.";
    private static final String MENSAGEM_DESCONTO_MAIOR_SUBTOTAL =
            "O desconto não pode ser maior que o subtotal do item.";

    private final ItemOrcamentoRepository repository;
    private final OrcamentoRepository orcamentoRepository;
    private final ServicoRepository servicoRepository;

    public ItemOrcamentoResponse salvar(Long orcamentoId, ItemOrcamentoRequest request) {
        Orcamento orcamento = buscarOrcamento(orcamentoId);
        Servico servico = buscarServicoAtivo(request.getServicoId());
        BigDecimal desconto = request.getDesconto() != null ? request.getDesconto() : ZERO;
        BigDecimal valorTotal = calcularValorTotal(
                request.getQuantidade(), request.getValorUnitario(), desconto);

        ItemOrcamento item = ItemOrcamentoMapper.toEntity(
                orcamento,
                servico,
                servico.getNome(),
                request.getQuantidade(),
                request.getValorUnitario(),
                desconto,
                valorTotal);

        return ItemOrcamentoMapper.toResponse(repository.saveAndFlush(item));
    }

    @Transactional(readOnly = true)
    public ItemOrcamentoResponse buscarPorId(Long orcamentoId, Long itemId) {
        return ItemOrcamentoMapper.toResponse(buscarItem(orcamentoId, itemId));
    }

    @Transactional(readOnly = true)
    public List<ItemOrcamentoResponse> listar(Long orcamentoId) {
        garantirOrcamentoExistente(orcamentoId);
        return repository.findByOrcamento_IdOrderByIdAsc(orcamentoId)
                .stream()
                .map(ItemOrcamentoMapper::toResponse)
                .toList();
    }

    public ItemOrcamentoResponse atualizar(
            Long orcamentoId,
            Long itemId,
            ItemOrcamentoUpdateRequest request) {

        ItemOrcamento item = buscarItem(orcamentoId, itemId);
        Servico servicoAtual = item.getServico();
        boolean servicoAlterado = request.getServicoId() != null
                && !request.getServicoId().equals(servicoAtual.getId());
        Servico servico = servicoAlterado
                ? buscarServicoAtivo(request.getServicoId())
                : servicoAtual;

        String descricao = resolverDescricao(item, request, servico, servicoAlterado);
        BigDecimal quantidade = request.getQuantidade() != null
                ? request.getQuantidade()
                : item.getQuantidade();
        BigDecimal valorUnitario = request.getValorUnitario() != null
                ? request.getValorUnitario()
                : item.getValorUnitario();
        BigDecimal desconto = request.getDesconto() != null
                ? request.getDesconto()
                : item.getDesconto();
        BigDecimal valorTotal = calcularValorTotal(quantidade, valorUnitario, desconto);

        ItemOrcamentoMapper.updateEntity(
                item, servico, descricao, quantidade, valorUnitario, desconto, valorTotal);
        return ItemOrcamentoMapper.toResponse(repository.saveAndFlush(item));
    }

    public void deletar(Long orcamentoId, Long itemId) {
        ItemOrcamento item = buscarItem(orcamentoId, itemId);
        repository.delete(item);
    }

    private String resolverDescricao(
            ItemOrcamento item,
            ItemOrcamentoUpdateRequest request,
            Servico servico,
            boolean servicoAlterado) {

        if (request.isDescricaoInformada()) {
            if (request.getDescricao() == null || request.getDescricao().trim().isEmpty()) {
                throw new BusinessException("A descrição informada não pode ser nula ou vazia.");
            }
            return request.getDescricao().trim();
        }
        return servicoAlterado ? servico.getNome() : item.getDescricao();
    }

    private BigDecimal calcularValorTotal(
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            BigDecimal desconto) {

        validarValores(quantidade, valorUnitario, desconto);
        BigDecimal subtotal = quantidade.multiply(valorUnitario);
        if (desconto.compareTo(subtotal) > 0) {
            throw new BusinessException(MENSAGEM_DESCONTO_MAIOR_SUBTOTAL);
        }
        return subtotal.subtract(desconto).setScale(2, RoundingMode.HALF_UP);
    }

    private void validarValores(
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            BigDecimal desconto) {

        if (quantidade == null || quantidade.signum() <= 0) {
            throw new BusinessException("A quantidade deve ser maior que zero.");
        }
        if (valorUnitario == null || valorUnitario.signum() < 0) {
            throw new BusinessException("O valor unitário não pode ser negativo.");
        }
        if (desconto == null || desconto.signum() < 0) {
            throw new BusinessException("O desconto não pode ser negativo.");
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

    private Servico buscarServicoAtivo(Long id) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Serviço não encontrado. Id: " + id));

        if (!Boolean.TRUE.equals(servico.getAtivo())) {
            throw new BusinessException(MENSAGEM_SERVICO_INATIVO);
        }
        return servico;
    }

    private ItemOrcamento buscarItem(Long orcamentoId, Long itemId) {
        return repository.findByIdAndOrcamento_Id(itemId, orcamentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item de orçamento não encontrado. Id: " + itemId
                                + ", orçamento: " + orcamentoId));
    }
}
