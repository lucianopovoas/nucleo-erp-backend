package br.com.nucleodasreformas.nucleoerp.item_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.entity.ItemOrcamento;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.mapper.ItemOrcamentoMapper;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.ItemOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoGuard;
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
    private final ItemOrcamentoRepository repository;
    private final ServicoRepository servicoRepository;
    private final OrcamentoVersaoGuard versaoGuard;

    public ItemOrcamentoResponse salvar(
            Long orcamentoId, Long versaoId, ItemOrcamentoRequest request) {
        OrcamentoVersao versao = versaoGuard.bloquearEditavel(orcamentoId, versaoId);
        Servico servico = buscarServicoAtivo(request.getServicoId());
        BigDecimal desconto = request.getDesconto() != null ? request.getDesconto() : ZERO;
        BigDecimal valorTotal = calcularValorTotal(
                request.getQuantidade(), request.getValorUnitario(), desconto);
        ItemOrcamento item = ItemOrcamentoMapper.toEntity(
                versao, servico, servico.getNome(), request.getQuantidade(),
                request.getValorUnitario(), desconto, valorTotal);
        return ItemOrcamentoMapper.toResponse(repository.saveAndFlush(item));
    }

    @Transactional(readOnly = true)
    public ItemOrcamentoResponse buscarPorId(Long orcamentoId, Long versaoId, Long itemId) {
        versaoGuard.buscar(orcamentoId, versaoId);
        return ItemOrcamentoMapper.toResponse(buscarItem(versaoId, itemId));
    }

    @Transactional(readOnly = true)
    public List<ItemOrcamentoResponse> listar(Long orcamentoId, Long versaoId) {
        versaoGuard.buscar(orcamentoId, versaoId);
        return repository.findByOrcamentoVersao_IdOrderByIdAsc(versaoId).stream()
                .map(ItemOrcamentoMapper::toResponse).toList();
    }

    public ItemOrcamentoResponse atualizar(
            Long orcamentoId, Long versaoId, Long itemId, ItemOrcamentoUpdateRequest request) {
        versaoGuard.bloquearEditavel(orcamentoId, versaoId);
        ItemOrcamento item = buscarItemParaAtualizar(versaoId, itemId);
        Servico atual = item.getServico();
        boolean alterado = request.getServicoId() != null
                && !request.getServicoId().equals(atual.getId());
        Servico servico = alterado ? buscarServicoAtivo(request.getServicoId()) : atual;
        String descricao = resolverDescricao(item, request, servico, alterado);
        BigDecimal quantidade = request.getQuantidade() != null
                ? request.getQuantidade() : item.getQuantidade();
        BigDecimal valorUnitario = request.getValorUnitario() != null
                ? request.getValorUnitario() : item.getValorUnitario();
        BigDecimal desconto = request.getDesconto() != null
                ? request.getDesconto() : item.getDesconto();
        BigDecimal total = calcularValorTotal(quantidade, valorUnitario, desconto);
        ItemOrcamentoMapper.updateEntity(
                item, servico, descricao, quantidade, valorUnitario, desconto, total);
        return ItemOrcamentoMapper.toResponse(repository.saveAndFlush(item));
    }

    public void deletar(Long orcamentoId, Long versaoId, Long itemId) {
        versaoGuard.bloquearEditavel(orcamentoId, versaoId);
        repository.delete(buscarItemParaAtualizar(versaoId, itemId));
    }

    private String resolverDescricao(
            ItemOrcamento item, ItemOrcamentoUpdateRequest request,
            Servico servico, boolean servicoAlterado) {
        if (request.isDescricaoInformada()) {
            if (request.getDescricao() == null || request.getDescricao().trim().isEmpty()) {
                throw new BusinessException("A descrição informada não pode ser nula ou vazia.");
            }
            return request.getDescricao().trim();
        }
        return servicoAlterado ? servico.getNome() : item.getDescricao();
    }

    private BigDecimal calcularValorTotal(
            BigDecimal quantidade, BigDecimal valorUnitario, BigDecimal desconto) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new BusinessException("A quantidade deve ser maior que zero.");
        }
        if (valorUnitario == null || valorUnitario.signum() < 0) {
            throw new BusinessException("O valor unitário não pode ser negativo.");
        }
        if (desconto == null || desconto.signum() < 0) {
            throw new BusinessException("O desconto não pode ser negativo.");
        }
        BigDecimal subtotal = quantidade.multiply(valorUnitario);
        if (desconto.compareTo(subtotal) > 0) {
            throw new BusinessException("O desconto não pode ser maior que o subtotal do item.");
        }
        return subtotal.subtract(desconto).setScale(2, RoundingMode.HALF_UP);
    }

    private Servico buscarServicoAtivo(Long id) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado. Id: " + id));
        if (!Boolean.TRUE.equals(servico.getAtivo())) {
            throw new BusinessException(
                    "Não é possível vincular um item de orçamento a um serviço inativo.");
        }
        return servico;
    }

    private ItemOrcamento buscarItem(Long versaoId, Long itemId) {
        return repository.findByIdAndOrcamentoVersao_Id(itemId, versaoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item de orçamento não encontrado. Id: " + itemId
                                + ", versão: " + versaoId));
    }

    private ItemOrcamento buscarItemParaAtualizar(Long versaoId, Long itemId) {
        return repository.findByIdAndOrcamentoVersaoIdForUpdate(itemId, versaoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item de orçamento não encontrado. Id: " + itemId
                                + ", versão: " + versaoId));
    }
}
