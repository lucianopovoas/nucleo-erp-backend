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
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoGuard;
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

    private final MaterialOrcamentoRepository repository;
    private final MaterialRepository materialRepository;
    private final OrcamentoVersaoGuard versaoGuard;

    public MaterialOrcamentoResponse salvar(
            Long orcamentoId, Long versaoId, MaterialOrcamentoRequest request) {
        OrcamentoVersao versao = versaoGuard.bloquearEditavel(orcamentoId, versaoId);
        Material material = buscarMaterialAtivo(request.getMaterialId());
        BigDecimal total = calcularCustoTotal(request.getQuantidade(), request.getCustoUnitario());
        MaterialOrcamento linha = MaterialOrcamentoMapper.toEntity(
                versao, material, material.getNome(), material.getUnidade(),
                request.getQuantidade(), request.getCustoUnitario(), total);
        return MaterialOrcamentoMapper.toResponse(repository.saveAndFlush(linha));
    }

    @Transactional(readOnly = true)
    public MaterialOrcamentoResponse buscarPorId(
            Long orcamentoId, Long versaoId, Long linhaId) {
        versaoGuard.buscar(orcamentoId, versaoId);
        return MaterialOrcamentoMapper.toResponse(buscarLinha(versaoId, linhaId));
    }

    @Transactional(readOnly = true)
    public List<MaterialOrcamentoResponse> listar(Long orcamentoId, Long versaoId) {
        versaoGuard.buscar(orcamentoId, versaoId);
        return repository.findByOrcamentoVersao_IdOrderByIdAsc(versaoId).stream()
                .map(MaterialOrcamentoMapper::toResponse).toList();
    }

    public MaterialOrcamentoResponse atualizar(
            Long orcamentoId, Long versaoId, Long linhaId,
            MaterialOrcamentoUpdateRequest request) {
        versaoGuard.bloquearEditavel(orcamentoId, versaoId);
        MaterialOrcamento linha = buscarLinhaParaAtualizar(versaoId, linhaId);
        Material atual = linha.getMaterial();
        boolean alterado = request.getMaterialId() != null
                && !request.getMaterialId().equals(atual.getId());
        Material material = alterado ? buscarMaterialAtivo(request.getMaterialId()) : atual;
        String descricao = resolverDescricao(linha, request, material, alterado);
        String unidade = alterado ? material.getUnidade() : linha.getUnidade();
        BigDecimal quantidade = request.getQuantidade() != null
                ? request.getQuantidade() : linha.getQuantidade();
        BigDecimal custoUnitario = request.getCustoUnitario() != null
                ? request.getCustoUnitario() : linha.getCustoUnitario();
        BigDecimal total = calcularCustoTotal(quantidade, custoUnitario);
        MaterialOrcamentoMapper.updateEntity(
                linha, material, descricao, unidade, quantidade, custoUnitario, total);
        return MaterialOrcamentoMapper.toResponse(repository.saveAndFlush(linha));
    }

    public void deletar(Long orcamentoId, Long versaoId, Long linhaId) {
        versaoGuard.bloquearEditavel(orcamentoId, versaoId);
        repository.delete(buscarLinhaParaAtualizar(versaoId, linhaId));
    }

    private String resolverDescricao(
            MaterialOrcamento linha, MaterialOrcamentoUpdateRequest request,
            Material material, boolean alterado) {
        if (request.isDescricaoInformada()) {
            if (request.getDescricao() == null || request.getDescricao().trim().isEmpty()) {
                throw new BusinessException("A descrição informada não pode ser nula ou vazia.");
            }
            return request.getDescricao().trim();
        }
        return alterado ? material.getNome() : linha.getDescricao();
    }

    private BigDecimal calcularCustoTotal(BigDecimal quantidade, BigDecimal unitario) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new BusinessException("A quantidade deve ser maior que zero.");
        }
        if (unitario == null || unitario.signum() < 0) {
            throw new BusinessException("O custo unitário não pode ser negativo.");
        }
        return quantidade.multiply(unitario).setScale(2, RoundingMode.HALF_UP);
    }

    private Material buscarMaterialAtivo(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material não encontrado. Id: " + id));
        if (!Boolean.TRUE.equals(material.getAtivo())) {
            throw new BusinessException("Não é possível vincular um material inativo ao orçamento.");
        }
        return material;
    }

    private MaterialOrcamento buscarLinha(Long versaoId, Long linhaId) {
        return repository.findByIdAndOrcamentoVersao_Id(linhaId, versaoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Material do orçamento não encontrado. Id: " + linhaId
                                + ", versão: " + versaoId));
    }

    private MaterialOrcamento buscarLinhaParaAtualizar(Long versaoId, Long linhaId) {
        return repository.findByIdAndOrcamentoVersaoIdForUpdate(linhaId, versaoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Material do orçamento não encontrado. Id: " + linhaId
                                + ", versão: " + versaoId));
    }
}
