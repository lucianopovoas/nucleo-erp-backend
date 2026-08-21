package br.com.nucleodasreformas.nucleoerp.material_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.entity.MaterialOrcamento;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.mapper.MaterialOrcamentoMapper;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.repository.MaterialOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialOrcamentoService {

    private static final String MENSAGEM_MATERIAL_INATIVO =
            "Não é possível vincular um material inativo ao orçamento.";

    private final MaterialOrcamentoRepository repository;
    private final OrcamentoRepository orcamentoRepository;
    private final MaterialRepository materialRepository;

    public MaterialOrcamentoResponse salvar(Long orcamentoId, MaterialOrcamentoRequest request) {
        Orcamento orcamento = buscarOrcamento(orcamentoId);
        Material material = buscarMaterialAtivo(request.getMaterialId());
        BigDecimal custoTotal = calcularCustoTotal(
                request.getQuantidade(), request.getCustoUnitario());

        MaterialOrcamento materialOrcamento = MaterialOrcamentoMapper.toEntity(
                orcamento,
                material,
                material.getNome(),
                material.getUnidade(),
                request.getQuantidade(),
                request.getCustoUnitario(),
                custoTotal);

        return MaterialOrcamentoMapper.toResponse(repository.saveAndFlush(materialOrcamento));
    }

    @Transactional(readOnly = true)
    public MaterialOrcamentoResponse buscarPorId(Long orcamentoId, Long materialOrcamentoId) {
        return MaterialOrcamentoMapper.toResponse(
                buscarMaterialOrcamento(orcamentoId, materialOrcamentoId));
    }

    @Transactional(readOnly = true)
    public List<MaterialOrcamentoResponse> listar(Long orcamentoId) {
        garantirOrcamentoExistente(orcamentoId);
        return repository.findByOrcamento_IdOrderByIdAsc(orcamentoId)
                .stream()
                .map(MaterialOrcamentoMapper::toResponse)
                .toList();
    }

    public MaterialOrcamentoResponse atualizar(
            Long orcamentoId,
            Long materialOrcamentoId,
            MaterialOrcamentoUpdateRequest request) {

        MaterialOrcamento materialOrcamento =
                buscarMaterialOrcamento(orcamentoId, materialOrcamentoId);
        Material materialAtual = materialOrcamento.getMaterial();
        boolean materialAlterado = request.getMaterialId() != null
                && !request.getMaterialId().equals(materialAtual.getId());
        Material material = materialAlterado
                ? buscarMaterialAtivo(request.getMaterialId())
                : materialAtual;

        String descricao = resolverDescricao(materialOrcamento, request, material, materialAlterado);
        String unidade = materialAlterado
                ? material.getUnidade()
                : materialOrcamento.getUnidade();
        BigDecimal quantidade = request.getQuantidade() != null
                ? request.getQuantidade()
                : materialOrcamento.getQuantidade();
        BigDecimal custoUnitario = request.getCustoUnitario() != null
                ? request.getCustoUnitario()
                : materialOrcamento.getCustoUnitario();
        BigDecimal custoTotal = calcularCustoTotal(quantidade, custoUnitario);

        MaterialOrcamentoMapper.updateEntity(
                materialOrcamento,
                material,
                descricao,
                unidade,
                quantidade,
                custoUnitario,
                custoTotal);
        return MaterialOrcamentoMapper.toResponse(repository.saveAndFlush(materialOrcamento));
    }

    public void deletar(Long orcamentoId, Long materialOrcamentoId) {
        MaterialOrcamento materialOrcamento =
                buscarMaterialOrcamento(orcamentoId, materialOrcamentoId);
        repository.delete(materialOrcamento);
    }

    private String resolverDescricao(
            MaterialOrcamento materialOrcamento,
            MaterialOrcamentoUpdateRequest request,
            Material material,
            boolean materialAlterado) {

        if (request.isDescricaoInformada()) {
            if (request.getDescricao() == null || request.getDescricao().trim().isEmpty()) {
                throw new BusinessException("A descrição informada não pode ser nula ou vazia.");
            }
            return request.getDescricao().trim();
        }
        return materialAlterado ? material.getNome() : materialOrcamento.getDescricao();
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

    private Material buscarMaterialAtivo(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Material não encontrado. Id: " + id));

        if (!Boolean.TRUE.equals(material.getAtivo())) {
            throw new BusinessException(MENSAGEM_MATERIAL_INATIVO);
        }
        return material;
    }

    private MaterialOrcamento buscarMaterialOrcamento(
            Long orcamentoId,
            Long materialOrcamentoId) {

        return repository.findByIdAndOrcamento_Id(materialOrcamentoId, orcamentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Material do orçamento não encontrado. Id: " + materialOrcamentoId
                                + ", orçamento: " + orcamentoId));
    }
}
