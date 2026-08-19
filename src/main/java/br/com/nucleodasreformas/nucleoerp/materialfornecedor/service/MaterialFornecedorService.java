package br.com.nucleodasreformas.nucleoerp.materialfornecedor.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.fornecedor.entity.Fornecedor;
import br.com.nucleodasreformas.nucleoerp.fornecedor.repository.FornecedorRepository;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.dto.MaterialFornecedorRequest;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.dto.MaterialFornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.entity.MaterialFornecedor;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.mapper.MaterialFornecedorMapper;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.repository.MaterialFornecedorRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialFornecedorService {

    private static final String MENSAGEM_DUPLICIDADE = "Este fornecedor já está vinculado a este material.";
    private static final String CONSTRAINT_UNICIDADE = "uk_material_fornecedor_material_fornecedor";

    private final MaterialFornecedorRepository repository;
    private final MaterialRepository materialRepository;
    private final FornecedorRepository fornecedorRepository;

    public MaterialFornecedorResponse salvar(MaterialFornecedorRequest request) {
        validarPreco(request.getPrecoCompra());

        Material material = buscarMaterialAtivo(request.getMaterialId());
        Fornecedor fornecedor = buscarFornecedorAtivo(request.getFornecedorId());

        MaterialFornecedor materialFornecedor = repository
                .findByMaterialIdAndFornecedorId(material.getId(), fornecedor.getId())
                .map(existente -> reativar(existente, request, material, fornecedor))
                .orElseGet(() -> MaterialFornecedorMapper.toEntity(request, material, fornecedor));

        return MaterialFornecedorMapper.toResponse(salvarComTratamentoDeConflito(materialFornecedor));
    }

    @Transactional(readOnly = true)
    public MaterialFornecedorResponse buscarPorId(Long id) {
        return MaterialFornecedorMapper.toResponse(buscarMaterialFornecedor(id));
    }

    @Transactional(readOnly = true)
    public List<MaterialFornecedorResponse> listar() {
        return repository.findByAtivoTrue()
                .stream()
                .map(MaterialFornecedorMapper::toResponse)
                .toList();
    }

    public MaterialFornecedorResponse atualizar(Long id, MaterialFornecedorRequest request) {
        MaterialFornecedor materialFornecedor = buscarMaterialFornecedor(id);

        if (!Boolean.TRUE.equals(materialFornecedor.getAtivo())) {
            throw new BusinessException("Não é possível atualizar um vínculo inativo.");
        }

        validarPreco(request.getPrecoCompra());

        Material material = buscarMaterialAtivo(request.getMaterialId());
        Fornecedor fornecedor = buscarFornecedorAtivo(request.getFornecedorId());

        if (repository.existsByMaterialIdAndFornecedorIdAndIdNot(material.getId(), fornecedor.getId(), id)) {
            throw new BusinessException(MENSAGEM_DUPLICIDADE);
        }

        MaterialFornecedorMapper.updateEntity(materialFornecedor, request, material, fornecedor);

        return MaterialFornecedorMapper.toResponse(salvarComTratamentoDeConflito(materialFornecedor));
    }

    public void deletar(Long id) {
        MaterialFornecedor materialFornecedor = buscarMaterialFornecedor(id);
        materialFornecedor.setAtivo(false);
        repository.save(materialFornecedor);
    }

    private MaterialFornecedor reativar(
            MaterialFornecedor existente,
            MaterialFornecedorRequest request,
            Material material,
            Fornecedor fornecedor) {

        if (Boolean.TRUE.equals(existente.getAtivo())) {
            throw new BusinessException(MENSAGEM_DUPLICIDADE);
        }

        MaterialFornecedorMapper.updateEntity(existente, request, material, fornecedor);
        existente.setAtivo(true);
        return existente;
    }

    private MaterialFornecedor salvarComTratamentoDeConflito(MaterialFornecedor materialFornecedor) {
        try {
            return repository.saveAndFlush(materialFornecedor);
        } catch (DataIntegrityViolationException ex) {
            if (causadoPelaConstraintUnica(ex)) {
                throw new BusinessException(MENSAGEM_DUPLICIDADE);
            }
            throw ex;
        }
    }

    private boolean causadoPelaConstraintUnica(Throwable throwable) {
        Throwable causa = throwable;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violacao
                    && CONSTRAINT_UNICIDADE.equals(violacao.getConstraintName())) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }

    private Material buscarMaterialAtivo(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material não encontrado. Id: " + id));

        if (!Boolean.TRUE.equals(material.getAtivo())) {
            throw new BusinessException("Não é possível vincular um material inativo.");
        }

        return material;
    }

    private Fornecedor buscarFornecedorAtivo(Long id) {
        Fornecedor fornecedor = fornecedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado. Id: " + id));

        if (!Boolean.TRUE.equals(fornecedor.getAtivo())) {
            throw new BusinessException("Não é possível vincular um fornecedor inativo.");
        }

        return fornecedor;
    }

    private MaterialFornecedor buscarMaterialFornecedor(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo entre material e fornecedor não encontrado. Id: " + id));
    }

    private void validarPreco(BigDecimal precoCompra) {
        if (precoCompra != null && precoCompra.signum() < 0) {
            throw new BusinessException("O preço de compra não pode ser negativo.");
        }
    }
}
