package br.com.nucleodasreformas.nucleoerp.material_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.entity.MaterialOrcamento;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.repository.MaterialOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialOrcamentoServiceTest {

    @Mock private MaterialOrcamentoRepository repository;
    @Mock private MaterialRepository materialRepository;
    @Mock private OrcamentoVersaoGuard versaoGuard;
    @InjectMocks private MaterialOrcamentoService service;

    @Test
    void deveCalcularCustoTotalEGuardarSnapshotsDoMaterial() {
        prepararCriacao(material(5L, "Lona Premium", "M2", true));

        MaterialOrcamentoResponse response = service.salvar(1L, 2L, request(5L, "2.5000", "75.00"));

        assertThat(response.getDescricao()).isEqualTo("Lona Premium");
        assertThat(response.getUnidade()).isEqualTo("M2");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("187.50");
        assertThat(response.getCustoTotal().scale()).isEqualTo(2);
    }

    @Test
    void deveArredondarCustoTotalComHalfUp() {
        prepararCriacao(material(5L, "Material", "UN", true));

        MaterialOrcamentoResponse response = service.salvar(1L, 2L, request(5L, "0.3333", "0.05"));

        assertThat(response.getCustoTotal()).isEqualByComparingTo("0.02");
    }

    @Test
    void deveAceitarCustoUnitarioZero() {
        prepararCriacao(material(5L, "Material", "UN", true));

        MaterialOrcamentoResponse response = service.salvar(1L, 2L, request(5L, "1.0000", "0.00"));

        assertThat(response.getCustoTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void deveRejeitarMaterialInativoNaInclusao() {
        when(versaoGuard.bloquearEditavel(1L, 2L)).thenReturn(versao());
        when(materialRepository.findById(5L)).thenReturn(Optional.of(material(5L, "Inativo", "UN", false)));

        assertThatThrownBy(() -> service.salvar(1L, 2L, request(5L, "1.0000", "10.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("material inativo");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveAtualizarDescricaoEUnidadeAoTrocarParaMaterialAtivo() {
        MaterialOrcamento linha = linha(material(5L, "Atual", "UN", false), "Snapshot", "CX");
        Material novo = material(6L, "Novo catálogo", "M2", true);
        MaterialOrcamentoUpdateRequest request = new MaterialOrcamentoUpdateRequest();
        request.setMaterialId(6L);

        when(repository.findByIdAndOrcamentoVersaoIdForUpdate(10L, 2L)).thenReturn(Optional.of(linha));
        when(materialRepository.findById(6L)).thenReturn(Optional.of(novo));
        when(repository.saveAndFlush(linha)).thenReturn(linha);

        MaterialOrcamentoResponse response = service.atualizar(1L, 2L, 10L, request);

        assertThat(response.getMaterial().getId()).isEqualTo(6L);
        assertThat(response.getDescricao()).isEqualTo("Novo catálogo");
        assertThat(response.getUnidade()).isEqualTo("M2");
    }

    @Test
    void devePreservarMaterialInativoESnapshotsSemTrocaEfetiva() {
        MaterialOrcamento linha = linha(
                material(5L, "Catálogo alterado", "KG", false), "Descrição negociada", "CX");
        MaterialOrcamentoUpdateRequest request = new MaterialOrcamentoUpdateRequest();
        request.setCustoUnitario(new BigDecimal("12.00"));

        when(repository.findByIdAndOrcamentoVersaoIdForUpdate(10L, 2L)).thenReturn(Optional.of(linha));
        when(repository.saveAndFlush(linha)).thenReturn(linha);

        MaterialOrcamentoResponse response = service.atualizar(1L, 2L, 10L, request);

        assertThat(response.getMaterial().getId()).isEqualTo(5L);
        assertThat(response.getDescricao()).isEqualTo("Descrição negociada");
        assertThat(response.getUnidade()).isEqualTo("CX");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("24.00");
        verifyNoInteractions(materialRepository);
    }

    private void prepararCriacao(Material material) {
        when(versaoGuard.bloquearEditavel(1L, 2L)).thenReturn(versao());
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(repository.saveAndFlush(any(MaterialOrcamento.class))).thenAnswer(invocation -> {
            MaterialOrcamento linha = invocation.getArgument(0);
            linha.setId(10L);
            return linha;
        });
    }

    private MaterialOrcamentoRequest request(Long materialId, String quantidade, String custo) {
        MaterialOrcamentoRequest request = new MaterialOrcamentoRequest();
        request.setMaterialId(materialId);
        request.setQuantidade(new BigDecimal(quantidade));
        request.setCustoUnitario(new BigDecimal(custo));
        return request;
    }

    private MaterialOrcamento linha(Material material, String descricao, String unidade) {
        return MaterialOrcamento.builder()
                .id(10L).orcamentoVersao(versao()).material(material)
                .descricao(descricao).unidade(unidade)
                .quantidade(new BigDecimal("2.0000"))
                .custoUnitario(new BigDecimal("10.00"))
                .custoTotal(new BigDecimal("20.00"))
                .build();
    }

    private Material material(Long id, String nome, String unidade, boolean ativo) {
        return Material.builder().id(id).nome(nome).unidade(unidade).ativo(ativo).build();
    }

    private OrcamentoVersao versao() {
        return OrcamentoVersao.builder().id(2L).build();
    }
}
