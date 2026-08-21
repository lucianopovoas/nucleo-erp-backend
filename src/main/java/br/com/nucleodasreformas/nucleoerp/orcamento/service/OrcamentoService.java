package br.com.nucleodasreformas.nucleoerp.orcamento.service;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.ItemOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.TotalComercialOrcamentoProjection;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.mapper.OrcamentoMapper;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrcamentoService {

    private static final BigDecimal ZERO_MONETARIO = BigDecimal.ZERO.setScale(2);
    private static final String STATUS_INICIAL = "Rascunho";
    private static final String MENSAGEM_CLIENTE_INATIVO =
            "Não é possível vincular um orçamento a um cliente inativo.";
    private static final String MENSAGEM_STATUS_INATIVO =
            "Não é possível selecionar um status de orçamento inativo.";

    private final OrcamentoRepository repository;
    private final ItemOrcamentoRepository itemOrcamentoRepository;
    private final ClienteRepository clienteRepository;
    private final StatusOrcamentoRepository statusOrcamentoRepository;

    public OrcamentoResponse salvar(OrcamentoRequest request) {
        Cliente cliente = buscarClienteAtivo(request.getClienteId());
        StatusOrcamento statusInicial = buscarStatusInicial();

        Orcamento orcamento = OrcamentoMapper.toEntity(request, cliente, statusInicial);
        return OrcamentoMapper.toResponse(repository.saveAndFlush(orcamento), ZERO_MONETARIO);
    }

    @Transactional(readOnly = true)
    public OrcamentoResponse buscarPorId(Long id) {
        Orcamento orcamento = buscarOrcamento(id);
        return OrcamentoMapper.toResponse(orcamento, buscarTotalComercial(id));
    }

    @Transactional(readOnly = true)
    public List<OrcamentoResponse> listar() {
        List<Orcamento> orcamentos = repository.findAll();
        Map<Long, BigDecimal> totais = buscarTotaisComerciais(
                orcamentos.stream().map(Orcamento::getId).toList());

        return orcamentos.stream()
                .map(orcamento -> OrcamentoMapper.toResponse(
                        orcamento,
                        totais.getOrDefault(orcamento.getId(), ZERO_MONETARIO)))
                .toList();
    }

    public OrcamentoResponse atualizar(Long id, OrcamentoUpdateRequest request) {
        Orcamento orcamento = buscarOrcamento(id);

        Cliente cliente = request.getClienteId() != null
                ? buscarClienteAtivo(request.getClienteId())
                : orcamento.getCliente();
        StatusOrcamento statusOrcamento = request.getStatusOrcamentoId() != null
                ? buscarStatusAtivo(request.getStatusOrcamentoId())
                : orcamento.getStatusOrcamento();

        OrcamentoMapper.updateEntity(orcamento, request, cliente, statusOrcamento);
        Orcamento salvo = repository.saveAndFlush(orcamento);
        return OrcamentoMapper.toResponse(salvo, buscarTotalComercial(salvo.getId()));
    }

    private BigDecimal buscarTotalComercial(Long orcamentoId) {
        return buscarTotaisComerciais(List.of(orcamentoId))
                .getOrDefault(orcamentoId, ZERO_MONETARIO);
    }

    private Map<Long, BigDecimal> buscarTotaisComerciais(List<Long> orcamentoIds) {
        if (orcamentoIds.isEmpty()) {
            return Map.of();
        }

        return itemOrcamentoRepository.somarValorTotalPorOrcamentos(orcamentoIds)
                .stream()
                .collect(Collectors.toMap(
                        TotalComercialOrcamentoProjection::orcamentoId,
                        total -> normalizarTotal(total.totalComercial())));
    }

    private BigDecimal normalizarTotal(BigDecimal total) {
        return total == null ? ZERO_MONETARIO : total.setScale(2);
    }

    private Cliente buscarClienteAtivo(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado. Id: " + id));

        if (!Boolean.TRUE.equals(cliente.getAtivo())) {
            throw new BusinessException(MENSAGEM_CLIENTE_INATIVO);
        }
        return cliente;
    }

    private StatusOrcamento buscarStatusInicial() {
        StatusOrcamento statusInicial = statusOrcamentoRepository.findByNomeNormalizado(STATUS_INICIAL)
                .orElseThrow(() -> new BusinessException(
                        "O status inicial 'Rascunho' não está cadastrado."));

        if (!Boolean.TRUE.equals(statusInicial.getAtivo())) {
            throw new BusinessException("O status inicial 'Rascunho' está inativo.");
        }
        return statusInicial;
    }

    private StatusOrcamento buscarStatusAtivo(Long id) {
        StatusOrcamento statusOrcamento = statusOrcamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status de orçamento não encontrado. Id: " + id));

        if (!Boolean.TRUE.equals(statusOrcamento.getAtivo())) {
            throw new BusinessException(MENSAGEM_STATUS_INATIVO);
        }
        return statusOrcamento;
    }

    private Orcamento buscarOrcamento(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado. Id: " + id));
    }
}
