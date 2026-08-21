package br.com.nucleodasreformas.nucleoerp.material_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.entity.MaterialOrcamento;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.repository.MaterialOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class MaterialOrcamentoServiceTest {

    @Mock
    private MaterialOrcamentoRepository repository;

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private MaterialOrcamentoService service;

    @Test
    void deveCriarComSnapshotsDeNomeEUnidadeECustoCalculado() {
        Material material = material(5L, "Lona", "M2", true);
        prepararCriacao(material);

        MaterialOrcamentoResponse response = service.salvar(
                10L, request(5L, "2.5000", "75.00"));

        assertThat(response.getDescricao()).isEqualTo("Lona");
        assertThat(response.getUnidade()).isEqualTo("M2");
        assertThat(response.getQuantidade()).isEqualByComparingTo("2.5000");
        assertThat(response.getCustoUnitario()).isEqualByComparingTo("75.00");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("187.50");
    }

    @Test
    void deveArredondarCustoTotalComHalfUp() {
        prepararCriacao(material(5L, "Material decimal", "UN", true));

        MaterialOrcamentoResponse response = service.salvar(
                10L, request(5L, "1.0050", "1.00"));

        assertThat(response.getCustoTotal()).isEqualByComparingTo("1.01");
        assertThat(response.getCustoTotal().scale()).isEqualTo(2);
    }

    @Test
    void devePermitirCustoZero() {
        prepararCriacao(material(5L, "Sem custo", "UN", true));

        MaterialOrcamentoResponse response = service.salvar(
                10L, request(5L, "3.5000", "0.00"));

        assertThat(response.getCustoTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void deveFalharComOrcamentoInexistente() {
        when(orcamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(99L, request(5L, "1", "10")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Orçamento não encontrado. Id: 99");

        verifyNoInteractions(materialRepository, repository);
    }

    @Test
    void deveFalharComMaterialInexistente() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(materialRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(10L, request(99L, "1", "10")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Material não encontrado. Id: 99");

        verifyNoInteractions(repository);
    }

    @Test
    void deveFalharComMaterialInativoNaInclusao() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(materialRepository.findById(5L))
                .thenReturn(Optional.of(material(5L, "Inativo", "UN", false)));

        assertThatThrownBy(() -> service.salvar(10L, request(5L, "1", "10")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular um material inativo ao orçamento.");

        verifyNoInteractions(repository);
    }

    @Test
    void deveRejeitarQuantidadeZeroOuNegativa() {
        prepararReferenciasParaCriacao();

        assertThatThrownBy(() -> service.salvar(10L, request(5L, "0", "10")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A quantidade deve ser maior que zero.");
        assertThatThrownBy(() -> service.salvar(10L, request(5L, "-1", "10")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A quantidade deve ser maior que zero.");
    }

    @Test
    void deveRejeitarCustoUnitarioNegativo() {
        prepararReferenciasParaCriacao();

        assertThatThrownBy(() -> service.salvar(10L, request(5L, "1", "-0.01")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O custo unitário não pode ser negativo.");
    }

    @Test
    void devePermitirMesmoMaterialRepetidoNoOrcamento() {
        prepararReferenciasParaCriacao();
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.salvar(10L, request(5L, "1", "10"));
        service.salvar(10L, request(5L, "2", "10"));

        verify(repository, org.mockito.Mockito.times(2)).saveAndFlush(any());
    }

    @Test
    void deveBuscarSomenteLinhaPertencenteAoOrcamento() {
        when(repository.findByIdAndOrcamento_Id(20L, 10L))
                .thenReturn(Optional.of(registro(20L, 10L,
                        material(5L, "Nome atual", "UN", false))));

        MaterialOrcamentoResponse response = service.buscarPorId(10L, 20L);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getDescricao()).isEqualTo("Snapshot");
        assertThat(response.getUnidade()).isEqualTo("M2");
        verify(repository).findByIdAndOrcamento_Id(20L, 10L);
    }

    @Test
    void deveFalharQuandoLinhaNaoPertencerAoOrcamento() {
        when(repository.findByIdAndOrcamento_Id(20L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L, 20L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Material do orçamento não encontrado. Id: 20, orçamento: 99");
    }

    @Test
    void deveListarLinhasDoOrcamentoExistente() {
        when(orcamentoRepository.existsById(10L)).thenReturn(true);
        when(repository.findByOrcamento_IdOrderByIdAsc(10L)).thenReturn(List.of(
                registro(20L, 10L, material(5L, "A", "UN", true)),
                registro(21L, 10L, material(6L, "B", "M", false))));

        List<MaterialOrcamentoResponse> responses = service.listar(10L);

        assertThat(responses).extracting(MaterialOrcamentoResponse::getId)
                .containsExactly(20L, 21L);
    }

    @Test
    void deveFalharAoListarOrcamentoInexistente() {
        when(orcamentoRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.listar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Orçamento não encontrado. Id: 99");

        verifyNoInteractions(repository);
    }

    @Test
    void deveAtualizarQuantidadeECustoERecalcularTotal() {
        MaterialOrcamento registro = registro(
                20L, 10L, material(5L, "Lona", "M2", true));
        MaterialOrcamentoUpdateRequest request = new MaterialOrcamentoUpdateRequest();
        request.setQuantidade(new BigDecimal("3.0000"));
        request.setCustoUnitario(new BigDecimal("25.00"));
        prepararAtualizacao(registro);

        MaterialOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getQuantidade()).isEqualByComparingTo("3.0000");
        assertThat(response.getCustoUnitario()).isEqualByComparingTo("25.00");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("75.00");
    }

    @Test
    void deveTrocarMaterialEAtualizarDescricaoEUnidadeQuandoDescricaoForOmitida() {
        MaterialOrcamento registro = registro(
                20L, 10L, material(5L, "Anterior", "UN", false));
        Material novo = material(6L, "Nova lona", "M", true);
        MaterialOrcamentoUpdateRequest request = new MaterialOrcamentoUpdateRequest();
        request.setMaterialId(6L);
        prepararAtualizacao(registro);
        when(materialRepository.findById(6L)).thenReturn(Optional.of(novo));

        MaterialOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getMaterial().getId()).isEqualTo(6L);
        assertThat(response.getDescricao()).isEqualTo("Nova lona");
        assertThat(response.getUnidade()).isEqualTo("M");
    }

    @Test
    void deveTrocarMaterialComDescricaoExplicitaEUnidadeDoNovoMaterial() {
        MaterialOrcamento registro = registro(
                20L, 10L, material(5L, "Anterior", "UN", true));
        MaterialOrcamentoUpdateRequest request = new MaterialOrcamentoUpdateRequest();
        request.setMaterialId(6L);
        request.setDescricao("  Lona da área frontal  ");
        prepararAtualizacao(registro);
        when(materialRepository.findById(6L))
                .thenReturn(Optional.of(material(6L, "Nova lona", "M", true)));

        MaterialOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getDescricao()).isEqualTo("Lona da área frontal");
        assertThat(response.getUnidade()).isEqualTo("M");
    }

    @Test
    void devePreservarSnapshotsAoReinformarMesmoMaterialInativo() {
        MaterialOrcamento registro = registro(
                20L, 10L, material(5L, "Nome atual", "UN", false));
        MaterialOrcamentoUpdateRequest request = new MaterialOrcamentoUpdateRequest();
        request.setMaterialId(5L);
        prepararAtualizacao(registro);

        MaterialOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getDescricao()).isEqualTo("Snapshot");
        assertThat(response.getUnidade()).isEqualTo("M2");
        verifyNoInteractions(materialRepository);
    }

    @Test
    void devePreservarCamposQuandoNullForInformadoNoPut() {
        MaterialOrcamento registro = registro(
                20L, 10L, material(5L, "Lona", "UN", true));
        MaterialOrcamentoUpdateRequest request = new MaterialOrcamentoUpdateRequest();
        request.setMaterialId(null);
        request.setQuantidade(null);
        request.setCustoUnitario(null);
        prepararAtualizacao(registro);

        MaterialOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getDescricao()).isEqualTo("Snapshot");
        assertThat(response.getUnidade()).isEqualTo("M2");
        assertThat(response.getQuantidade()).isEqualByComparingTo("2.0000");
        assertThat(response.getCustoUnitario()).isEqualByComparingTo("50.00");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void devePermitirEditarDescricaoSemTrocarMaterialOuUnidade() {
        MaterialOrcamento registro = registro(
                20L, 10L, material(5L, "Lona", "UN", false));
        MaterialOrcamentoUpdateRequest request = new MaterialOrcamentoUpdateRequest();
        request.setDescricao("  Contexto negociado  ");
        prepararAtualizacao(registro);

        MaterialOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getDescricao()).isEqualTo("Contexto negociado");
        assertThat(response.getUnidade()).isEqualTo("M2");
        verifyNoInteractions(materialRepository);
    }

    @Test
    void deveRejeitarDescricaoExplicitaNulaOuVazia() {
        MaterialOrcamento registro = registro(
                20L, 10L, material(5L, "Lona", "UN", true));
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));
        MaterialOrcamentoUpdateRequest nula = new MaterialOrcamentoUpdateRequest();
        nula.setDescricao(null);

        assertThatThrownBy(() -> service.atualizar(10L, 20L, nula))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A descrição informada não pode ser nula ou vazia.");

        MaterialOrcamentoUpdateRequest vazia = new MaterialOrcamentoUpdateRequest();
        vazia.setDescricao("   ");
        assertThatThrownBy(() -> service.atualizar(10L, 20L, vazia))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A descrição informada não pode ser nula ou vazia.");
    }

    @Test
    void deveRejeitarTrocaParaMaterialInativo() {
        MaterialOrcamento registro = registro(
                20L, 10L, material(5L, "Atual", "UN", true));
        MaterialOrcamentoUpdateRequest request = new MaterialOrcamentoUpdateRequest();
        request.setMaterialId(6L);
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));
        when(materialRepository.findById(6L))
                .thenReturn(Optional.of(material(6L, "Inativo", "M", false)));

        assertThatThrownBy(() -> service.atualizar(10L, 20L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular um material inativo ao orçamento.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarTrocaParaMaterialInexistente() {
        MaterialOrcamento registro = registro(
                20L, 10L, material(5L, "Atual", "UN", true));
        MaterialOrcamentoUpdateRequest request = new MaterialOrcamentoUpdateRequest();
        request.setMaterialId(99L);
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));
        when(materialRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(10L, 20L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Material não encontrado. Id: 99");
    }

    @Test
    void deveExcluirFisicamenteSomenteLinhaPertencenteAoOrcamento() {
        MaterialOrcamento registro = registro(
                20L, 10L, material(5L, "Lona", "M2", true));
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));

        service.deletar(10L, 20L);

        verify(repository).delete(registro);
        verify(orcamentoRepository, never()).delete(any());
        verify(materialRepository, never()).delete(any());
    }

    private void prepararCriacao(Material material) {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            MaterialOrcamento registro = invocation.getArgument(0);
            registro.setId(20L);
            registro.setCriadoEm(LocalDateTime.of(2026, 8, 20, 12, 0));
            return registro;
        });
    }

    private void prepararReferenciasParaCriacao() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(materialRepository.findById(5L))
                .thenReturn(Optional.of(material(5L, "Lona", "M2", true)));
    }

    private void prepararAtualizacao(MaterialOrcamento registro) {
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));
        when(repository.saveAndFlush(registro)).thenReturn(registro);
    }

    private MaterialOrcamentoRequest request(
            Long materialId, String quantidade, String custoUnitario) {
        MaterialOrcamentoRequest request = new MaterialOrcamentoRequest();
        request.setMaterialId(materialId);
        request.setQuantidade(new BigDecimal(quantidade));
        request.setCustoUnitario(new BigDecimal(custoUnitario));
        return request;
    }

    private MaterialOrcamento registro(Long id, Long orcamentoId, Material material) {
        return MaterialOrcamento.builder()
                .id(id).orcamento(orcamento(orcamentoId)).material(material)
                .descricao("Snapshot").unidade("M2")
                .quantidade(new BigDecimal("2.0000"))
                .custoUnitario(new BigDecimal("50.00"))
                .custoTotal(new BigDecimal("100.00"))
                .criadoEm(LocalDateTime.of(2026, 8, 20, 12, 0)).build();
    }

    private Orcamento orcamento(Long id) {
        return Orcamento.builder().id(id).numero(1000L + id).build();
    }

    private Material material(Long id, String nome, String unidade, boolean ativo) {
        return Material.builder().id(id).nome(nome).unidade(unidade).ativo(ativo).build();
    }
}
