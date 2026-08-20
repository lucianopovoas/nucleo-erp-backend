package br.com.nucleodasreformas.nucleoerp.material_fornecedor.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.fornecedor.entity.Fornecedor;
import br.com.nucleodasreformas.nucleoerp.fornecedor.repository.FornecedorRepository;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import br.com.nucleodasreformas.nucleoerp.material_fornecedor.dto.MaterialFornecedorRequest;
import br.com.nucleodasreformas.nucleoerp.material_fornecedor.dto.MaterialFornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.material_fornecedor.entity.MaterialFornecedor;
import br.com.nucleodasreformas.nucleoerp.material_fornecedor.repository.MaterialFornecedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialFornecedorServiceTest {

    @Mock
    private MaterialFornecedorRepository repository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private FornecedorRepository fornecedorRepository;

    @InjectMocks
    private MaterialFornecedorService service;

    @Test
    void deveSalvarVinculoValido() {
        MaterialFornecedorRequest request = request(1L, 2L, "125.50");
        Material material = material(1L, true);
        Fornecedor fornecedor = fornecedor(2L, true);

        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
        when(fornecedorRepository.findById(2L)).thenReturn(Optional.of(fornecedor));
        when(repository.findByMaterialIdAndFornecedorId(1L, 2L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(MaterialFornecedor.class))).thenAnswer(invocation -> {
            MaterialFornecedor salvo = invocation.getArgument(0);
            salvo.setId(10L);
            salvo.setCriadoEm(LocalDateTime.of(2026, 8, 19, 12, 0));
            return salvo;
        });

        MaterialFornecedorResponse response = service.salvar(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getMaterial().getId()).isEqualTo(1L);
        assertThat(response.getFornecedor().getId()).isEqualTo(2L);
        assertThat(response.getPrecoCompra()).isEqualByComparingTo("125.50");
        assertThat(response.getAtivo()).isTrue();
    }

    @Test
    void deveReativarVinculoExistenteMantendoIdECriadoEm() {
        Material material = material(1L, true);
        Fornecedor fornecedor = fornecedor(2L, true);
        MaterialFornecedor existente = vinculo(10L, material, fornecedor, false);
        LocalDateTime criadoEm = existente.getCriadoEm();

        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
        when(fornecedorRepository.findById(2L)).thenReturn(Optional.of(fornecedor));
        when(repository.findByMaterialIdAndFornecedorId(1L, 2L)).thenReturn(Optional.of(existente));
        when(repository.saveAndFlush(existente)).thenReturn(existente);

        MaterialFornecedorResponse response = service.salvar(request(1L, 2L, "150.00"));

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(response.getPrecoCompra()).isEqualByComparingTo("150.00");
        assertThat(response.getAtivo()).isTrue();
    }

    @Test
    void deveRejeitarMaterialInexistenteAoSalvar() {
        when(materialRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(request(1L, 2L, "10.00")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Material não encontrado");

        verifyNoInteractions(fornecedorRepository, repository);
    }

    @Test
    void deveRejeitarFornecedorInexistenteAoSalvar() {
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, true)));
        when(fornecedorRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(request(1L, 2L, "10.00")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Fornecedor não encontrado");

        verifyNoInteractions(repository);
    }

    @Test
    void deveRejeitarMaterialInativoAoSalvar() {
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, false)));

        assertThatThrownBy(() -> service.salvar(request(1L, 2L, "10.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular um material inativo.");
    }

    @Test
    void deveRejeitarFornecedorInativoAoSalvar() {
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, true)));
        when(fornecedorRepository.findById(2L)).thenReturn(Optional.of(fornecedor(2L, false)));

        assertThatThrownBy(() -> service.salvar(request(1L, 2L, "10.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular um fornecedor inativo.");
    }

    @Test
    void deveRejeitarVinculoAtivoDuplicado() {
        Material material = material(1L, true);
        Fornecedor fornecedor = fornecedor(2L, true);

        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
        when(fornecedorRepository.findById(2L)).thenReturn(Optional.of(fornecedor));
        when(repository.findByMaterialIdAndFornecedorId(1L, 2L))
                .thenReturn(Optional.of(vinculo(10L, material, fornecedor, true)));

        assertThatThrownBy(() -> service.salvar(request(1L, 2L, "10.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Este fornecedor já está vinculado a este material.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarPrecoNegativoNoService() {
        assertThatThrownBy(() -> service.salvar(request(1L, 2L, "-0.01")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O preço de compra não pode ser negativo.");

        verifyNoInteractions(materialRepository, fornecedorRepository, repository);
    }

    @Test
    void deveConverterViolacaoConcorrenteDeUnicidadeEmErroDeNegocio() {
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, true)));
        when(fornecedorRepository.findById(2L)).thenReturn(Optional.of(fornecedor(2L, true)));
        when(repository.findByMaterialIdAndFornecedorId(1L, 2L)).thenReturn(Optional.empty());
        ConstraintViolationException constraint = new ConstraintViolationException(
                "duplicidade",
                new SQLException(),
                "uk_material_fornecedor_material_fornecedor");
        when(repository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("constraint", constraint));

        assertThatThrownBy(() -> service.salvar(request(1L, 2L, "10.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Este fornecedor já está vinculado a este material.");
    }

    @Test
    void naoDeveMascararViolacaoDeIntegridadeNaoRelacionadaAUnicidade() {
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, true)));
        when(fornecedorRepository.findById(2L)).thenReturn(Optional.of(fornecedor(2L, true)));
        when(repository.findByMaterialIdAndFornecedorId(1L, 2L)).thenReturn(Optional.empty());
        DataIntegrityViolationException erro = new DataIntegrityViolationException("outra constraint");
        when(repository.saveAndFlush(any())).thenThrow(erro);

        assertThatThrownBy(() -> service.salvar(request(1L, 2L, "10.00")))
                .isSameAs(erro);
    }

    @Test
    void deveBuscarVinculoExistenteInclusiveInativo() {
        MaterialFornecedor inativo = vinculo(10L, material(1L, true), fornecedor(2L, true), false);
        when(repository.findById(10L)).thenReturn(Optional.of(inativo));

        MaterialFornecedorResponse response = service.buscarPorId(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getAtivo()).isFalse();
    }

    @Test
    void deveRejeitarBuscaDeVinculoInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveListarSomenteResultadoDaConsultaDeAtivos() {
        MaterialFornecedor ativo = vinculo(10L, material(1L, true), fornecedor(2L, true), true);
        when(repository.findByAtivoTrue()).thenReturn(List.of(ativo));

        List<MaterialFornecedorResponse> responses = service.listar();

        assertThat(responses).singleElement().extracting(MaterialFornecedorResponse::getAtivo).isEqualTo(true);
        verify(repository).findByAtivoTrue();
    }

    @Test
    void deveAtualizarVinculoParaNovaCombinacao() {
        MaterialFornecedor existente = vinculo(10L, material(1L, true), fornecedor(2L, true), true);
        Material novoMaterial = material(3L, true);
        Fornecedor novoFornecedor = fornecedor(4L, true);

        when(repository.findById(10L)).thenReturn(Optional.of(existente));
        when(materialRepository.findById(3L)).thenReturn(Optional.of(novoMaterial));
        when(fornecedorRepository.findById(4L)).thenReturn(Optional.of(novoFornecedor));
        when(repository.existsByMaterialIdAndFornecedorIdAndIdNot(3L, 4L, 10L)).thenReturn(false);
        when(repository.saveAndFlush(existente)).thenReturn(existente);

        MaterialFornecedorResponse response = service.atualizar(10L, request(3L, 4L, "200.00"));

        assertThat(response.getMaterial().getId()).isEqualTo(3L);
        assertThat(response.getFornecedor().getId()).isEqualTo(4L);
        assertThat(response.getPrecoCompra()).isEqualByComparingTo("200.00");
    }

    @Test
    void deveManterMesmaCombinacaoSemConflitoNaAtualizacao() {
        Material material = material(1L, true);
        Fornecedor fornecedor = fornecedor(2L, true);
        MaterialFornecedor existente = vinculo(10L, material, fornecedor, true);

        when(repository.findById(10L)).thenReturn(Optional.of(existente));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
        when(fornecedorRepository.findById(2L)).thenReturn(Optional.of(fornecedor));
        when(repository.existsByMaterialIdAndFornecedorIdAndIdNot(1L, 2L, 10L)).thenReturn(false);
        when(repository.saveAndFlush(existente)).thenReturn(existente);

        MaterialFornecedorResponse response = service.atualizar(10L, request(1L, 2L, "175.00"));

        assertThat(response.getPrecoCompra()).isEqualByComparingTo("175.00");
    }

    @Test
    void deveRejeitarCombinacaoPertencenteAOutroVinculoNaAtualizacao() {
        MaterialFornecedor existente = vinculo(10L, material(1L, true), fornecedor(2L, true), true);

        when(repository.findById(10L)).thenReturn(Optional.of(existente));
        when(materialRepository.findById(3L)).thenReturn(Optional.of(material(3L, true)));
        when(fornecedorRepository.findById(4L)).thenReturn(Optional.of(fornecedor(4L, true)));
        when(repository.existsByMaterialIdAndFornecedorIdAndIdNot(3L, 4L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(10L, request(3L, 4L, "20.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Este fornecedor já está vinculado a este material.");
    }

    @Test
    void deveRejeitarMaterialInexistenteNaAtualizacao() {
        when(repository.findById(10L))
                .thenReturn(Optional.of(vinculo(10L, material(1L, true), fornecedor(2L, true), true)));
        when(materialRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(10L, request(99L, 2L, "20.00")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Material não encontrado");
    }

    @Test
    void deveRejeitarFornecedorInexistenteNaAtualizacao() {
        when(repository.findById(10L))
                .thenReturn(Optional.of(vinculo(10L, material(1L, true), fornecedor(2L, true), true)));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, true)));
        when(fornecedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(10L, request(1L, 99L, "20.00")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Fornecedor não encontrado");
    }

    @Test
    void deveRejeitarMaterialInativoNaAtualizacao() {
        when(repository.findById(10L))
                .thenReturn(Optional.of(vinculo(10L, material(1L, true), fornecedor(2L, true), true)));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, false)));

        assertThatThrownBy(() -> service.atualizar(10L, request(1L, 2L, "20.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular um material inativo.");
    }

    @Test
    void deveRejeitarFornecedorInativoNaAtualizacao() {
        when(repository.findById(10L))
                .thenReturn(Optional.of(vinculo(10L, material(1L, true), fornecedor(2L, true), true)));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(material(1L, true)));
        when(fornecedorRepository.findById(2L)).thenReturn(Optional.of(fornecedor(2L, false)));

        assertThatThrownBy(() -> service.atualizar(10L, request(1L, 2L, "20.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular um fornecedor inativo.");
    }

    @Test
    void deveRejeitarAtualizacaoDeVinculoInativo() {
        when(repository.findById(10L))
                .thenReturn(Optional.of(vinculo(10L, material(1L, true), fornecedor(2L, true), false)));

        assertThatThrownBy(() -> service.atualizar(10L, request(1L, 2L, "20.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível atualizar um vínculo inativo.");
    }

    @Test
    void deveRealizarExclusaoLogica() {
        MaterialFornecedor existente = vinculo(10L, material(1L, true), fornecedor(2L, true), true);
        when(repository.findById(10L)).thenReturn(Optional.of(existente));

        service.deletar(10L);

        assertThat(existente.getAtivo()).isFalse();
        verify(repository).save(existente);
        verify(repository, never()).delete(any());
    }

    @Test
    void deveRejeitarExclusaoDeVinculoInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private MaterialFornecedorRequest request(Long materialId, Long fornecedorId, String preco) {
        MaterialFornecedorRequest request = new MaterialFornecedorRequest();
        request.setMaterialId(materialId);
        request.setFornecedorId(fornecedorId);
        request.setPrecoCompra(preco == null ? null : new BigDecimal(preco));
        return request;
    }

    private Material material(Long id, boolean ativo) {
        return Material.builder().id(id).nome("Material " + id).unidade("UN").ativo(ativo).build();
    }

    private Fornecedor fornecedor(Long id, boolean ativo) {
        return Fornecedor.builder().id(id).nome("Fornecedor " + id).ativo(ativo).build();
    }

    private MaterialFornecedor vinculo(
            Long id,
            Material material,
            Fornecedor fornecedor,
            boolean ativo) {

        return MaterialFornecedor.builder()
                .id(id)
                .material(material)
                .fornecedor(fornecedor)
                .precoCompra(new BigDecimal("100.00"))
                .ativo(ativo)
                .criadoEm(LocalDateTime.of(2026, 8, 19, 10, 0))
                .build();
    }
}
