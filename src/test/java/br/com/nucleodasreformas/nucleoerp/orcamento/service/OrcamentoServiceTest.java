package br.com.nucleodasreformas.nucleoerp.orcamento.service;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository.CustoTotalDespesasOrcamentoProjection;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository.DespesaOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.ItemOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.TotalComercialOrcamentoProjection;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository.CustoTotalMaoDeObraOrcamentoProjection;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository.MaoDeObraOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.repository.CustoTotalMateriaisOrcamentoProjection;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.repository.MaterialOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock
    private OrcamentoRepository repository;

    @Mock
    private ItemOrcamentoRepository itemOrcamentoRepository;

    @Mock
    private MaterialOrcamentoRepository materialOrcamentoRepository;

    @Mock
    private MaoDeObraOrcamentoRepository maoDeObraOrcamentoRepository;

    @Mock
    private DespesaOrcamentoRepository despesaOrcamentoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private StatusOrcamentoRepository statusOrcamentoRepository;

    @InjectMocks
    private OrcamentoService service;

    @Test
    void deveCriarOrcamentoComNumeroGeradoEStatusInicialRascunho() {
        Cliente cliente = cliente(10L, "Cliente X", true);
        StatusOrcamento rascunho = status(1L, "Rascunho", true);
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(statusOrcamentoRepository.findByNomeNormalizado("Rascunho"))
                .thenReturn(Optional.of(rascunho));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            Orcamento orcamento = invocation.getArgument(0);
            orcamento.setId(5L);
            orcamento.setNumero(1234L);
            orcamento.setCriadoEm(LocalDateTime.of(2026, 8, 20, 12, 0));
            return orcamento;
        });

        OrcamentoResponse response = service.salvar(request(10L, "Área externa"));

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getNumero()).isEqualTo(1234L);
        assertThat(response.getCliente().getId()).isEqualTo(10L);
        assertThat(response.getStatus().getNome()).isEqualTo("Rascunho");
        assertThat(response.getTotalComercial()).isEqualTo(new BigDecimal("0.00"));
        assertThat(response.getTotalComercial().scale()).isEqualTo(2);
        assertThat(response.getCustoTotalMateriais()).isEqualTo(new BigDecimal("0.00"));
        assertThat(response.getCustoTotalMateriais().scale()).isEqualTo(2);
        assertThat(response.getCustoTotalMaoDeObra()).isEqualTo(new BigDecimal("0.00"));
        assertThat(response.getCustoTotalMaoDeObra().scale()).isEqualTo(2);
        assertThat(response.getCustoTotalDespesas()).isEqualTo(new BigDecimal("0.00"));
        assertThat(response.getCustoTotalDespesas().scale()).isEqualTo(2);
        assertThat(response.getMargemPrevista()).isEqualTo(new BigDecimal("0.00"));
        assertThat(response.getMargemPrevista().scale()).isEqualTo(2);
        assertThat(response.getPercentualMargem()).isEqualTo(new BigDecimal("0.00"));
        assertThat(response.getPercentualMargem().scale()).isEqualTo(2);
        verify(statusOrcamentoRepository).findByNomeNormalizado("Rascunho");
        verifyNoInteractions(
                itemOrcamentoRepository,
                materialOrcamentoRepository,
                maoDeObraOrcamentoRepository,
                despesaOrcamentoRepository);
    }

    @Test
    void deveFalharAoCriarComClienteInexistente() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(request(99L, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cliente não encontrado. Id: 99");

        verifyNoInteractions(statusOrcamentoRepository, repository);
    }

    @Test
    void deveFalharAoCriarComClienteInativo() {
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente(10L, "Cliente", false)));

        assertThatThrownBy(() -> service.salvar(request(10L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular um orçamento a um cliente inativo.");

        verifyNoInteractions(statusOrcamentoRepository, repository);
    }

    @Test
    void deveFalharQuandoRascunhoNaoEstiverCadastrado() {
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente(10L, "Cliente", true)));
        when(statusOrcamentoRepository.findByNomeNormalizado("Rascunho")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(request(10L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O status inicial 'Rascunho' não está cadastrado.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveFalharQuandoRascunhoEstiverInativo() {
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente(10L, "Cliente", true)));
        when(statusOrcamentoRepository.findByNomeNormalizado("Rascunho"))
                .thenReturn(Optional.of(status(1L, "Rascunho", false)));

        assertThatThrownBy(() -> service.salvar(request(10L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O status inicial 'Rascunho' está inativo.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveBuscarOrcamentoExistenteComReferenciasInativas() {
        when(repository.findById(5L)).thenReturn(Optional.of(
                orcamento(5L, 1234L, cliente(10L, "Cliente", false), status(2L, "Enviado", false), null)));
        when(itemOrcamentoRepository.somarValorTotalPorOrcamentos(List.of(5L)))
                .thenReturn(List.of(new TotalComercialOrcamentoProjection(
                        5L, new BigDecimal("350.00"))));
        when(materialOrcamentoRepository.somarCustoTotalPorOrcamentos(List.of(5L)))
                .thenReturn(List.of(new CustoTotalMateriaisOrcamentoProjection(
                        5L, new BigDecimal("180.00"))));
        when(maoDeObraOrcamentoRepository.somarCustoTotalPorOrcamentos(List.of(5L)))
                .thenReturn(List.of(new CustoTotalMaoDeObraOrcamentoProjection(
                        5L, new BigDecimal("95.50"))));
        when(despesaOrcamentoRepository.somarValorPorOrcamento(5L))
                .thenReturn(new BigDecimal("24.50"));

        OrcamentoResponse response = service.buscarPorId(5L);

        assertThat(response.getCliente().getId()).isEqualTo(10L);
        assertThat(response.getStatus().getId()).isEqualTo(2L);
        assertThat(response.getTotalComercial()).isEqualTo(new BigDecimal("350.00"));
        assertThat(response.getCustoTotalMateriais()).isEqualTo(new BigDecimal("180.00"));
        assertThat(response.getCustoTotalMaoDeObra()).isEqualTo(new BigDecimal("95.50"));
        assertThat(response.getCustoTotalDespesas()).isEqualTo(new BigDecimal("24.50"));
        assertThat(response.getMargemPrevista()).isEqualTo(new BigDecimal("50.00"));
        assertThat(response.getPercentualMargem()).isEqualTo(new BigDecimal("14.29"));
    }

    @ParameterizedTest
    @MethodSource("cenariosDeMargem")
    void deveCalcularMargemEPercentualComPoliticaMonetaria(
            String totalComercial,
            String custoTotalMateriais,
            String custoTotalMaoDeObra,
            String custoTotalDespesas,
            String margemPrevista,
            String percentualMargem) {
        when(repository.findById(5L)).thenReturn(Optional.of(
                orcamento(5L, 1234L, cliente(10L, "Cliente", true),
                        status(1L, "Rascunho", true), null)));
        when(itemOrcamentoRepository.somarValorTotalPorOrcamentos(List.of(5L)))
                .thenReturn(List.of(new TotalComercialOrcamentoProjection(
                        5L, new BigDecimal(totalComercial))));
        when(materialOrcamentoRepository.somarCustoTotalPorOrcamentos(List.of(5L)))
                .thenReturn(List.of(new CustoTotalMateriaisOrcamentoProjection(
                        5L, new BigDecimal(custoTotalMateriais))));
        when(maoDeObraOrcamentoRepository.somarCustoTotalPorOrcamentos(List.of(5L)))
                .thenReturn(List.of(new CustoTotalMaoDeObraOrcamentoProjection(
                        5L, new BigDecimal(custoTotalMaoDeObra))));
        when(despesaOrcamentoRepository.somarValorPorOrcamento(5L))
                .thenReturn(new BigDecimal(custoTotalDespesas));

        OrcamentoResponse response = service.buscarPorId(5L);

        assertThat(response.getMargemPrevista()).isEqualTo(new BigDecimal(margemPrevista));
        assertThat(response.getPercentualMargem()).isEqualTo(new BigDecimal(percentualMargem));
        assertThat(response.getMargemPrevista().scale()).isEqualTo(2);
        assertThat(response.getPercentualMargem().scale()).isEqualTo(2);
    }

    @Test
    void deveRetornarCustosZeroQuandoAgregadosNaoPossuiremEntrada() {
        when(repository.findById(5L)).thenReturn(Optional.of(
                orcamento(5L, 1234L, cliente(10L, "Cliente", true),
                        status(1L, "Rascunho", true), null)));
        when(maoDeObraOrcamentoRepository.somarCustoTotalPorOrcamentos(List.of(5L)))
                .thenReturn(List.of());

        OrcamentoResponse response = service.buscarPorId(5L);

        assertThat(response.getCustoTotalMaoDeObra()).isEqualTo(new BigDecimal("0.00"));
        assertThat(response.getCustoTotalMaoDeObra().scale()).isEqualTo(2);
        assertThat(response.getCustoTotalDespesas()).isEqualTo(new BigDecimal("0.00"));
        assertThat(response.getCustoTotalDespesas().scale()).isEqualTo(2);
        verify(despesaOrcamentoRepository).somarValorPorOrcamento(5L);
    }

    @Test
    void deveFalharAoBuscarOrcamentoInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Orçamento não encontrado. Id: 99");
    }

    @Test
    void deveListarTodosOsOrcamentosIndependentementeDoStatus() {
        when(repository.findAll()).thenReturn(List.of(
                orcamento(1L, 100L, cliente(1L, "A", true), status(1L, "Rascunho", true), null),
                orcamento(2L, 101L, cliente(2L, "B", false), status(5L, "Cancelado", false), null)));
        when(itemOrcamentoRepository.somarValorTotalPorOrcamentos(List.of(1L, 2L)))
                .thenReturn(List.of(new TotalComercialOrcamentoProjection(
                        1L, new BigDecimal("125.50"))));
        when(materialOrcamentoRepository.somarCustoTotalPorOrcamentos(List.of(1L, 2L)))
                .thenReturn(List.of(new CustoTotalMateriaisOrcamentoProjection(
                        2L, new BigDecimal("80.25"))));
        when(maoDeObraOrcamentoRepository.somarCustoTotalPorOrcamentos(List.of(1L, 2L)))
                .thenReturn(List.of(
                        new CustoTotalMaoDeObraOrcamentoProjection(
                                1L, new BigDecimal("0.00")),
                        new CustoTotalMaoDeObraOrcamentoProjection(
                                2L, new BigDecimal("140.75"))));
        when(despesaOrcamentoRepository.somarValorPorOrcamentos(List.of(1L, 2L)))
                .thenReturn(List.of(new CustoTotalDespesasOrcamentoProjection(
                        1L, new BigDecimal("25.50"))));

        List<OrcamentoResponse> responses = service.listar();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(response -> response.getStatus().getNome())
                .containsExactly("Rascunho", "Cancelado");
        assertThat(responses).extracting(OrcamentoResponse::getTotalComercial)
                .containsExactly(new BigDecimal("125.50"), new BigDecimal("0.00"));
        assertThat(responses).extracting(OrcamentoResponse::getCustoTotalMateriais)
                .containsExactly(new BigDecimal("0.00"), new BigDecimal("80.25"));
        assertThat(responses).extracting(OrcamentoResponse::getCustoTotalMaoDeObra)
                .containsExactly(new BigDecimal("0.00"), new BigDecimal("140.75"));
        assertThat(responses).extracting(OrcamentoResponse::getCustoTotalDespesas)
                .containsExactly(new BigDecimal("25.50"), new BigDecimal("0.00"));
        assertThat(responses).extracting(OrcamentoResponse::getMargemPrevista)
                .containsExactly(new BigDecimal("100.00"), new BigDecimal("-221.00"));
        assertThat(responses).extracting(OrcamentoResponse::getPercentualMargem)
                .containsExactly(new BigDecimal("79.68"), new BigDecimal("0.00"));
        verify(itemOrcamentoRepository).somarValorTotalPorOrcamentos(List.of(1L, 2L));
        verify(materialOrcamentoRepository).somarCustoTotalPorOrcamentos(List.of(1L, 2L));
        verify(maoDeObraOrcamentoRepository).somarCustoTotalPorOrcamentos(List.of(1L, 2L));
        verify(despesaOrcamentoRepository).somarValorPorOrcamentos(List.of(1L, 2L));
        verify(despesaOrcamentoRepository, never()).somarValorPorOrcamento(any());
    }

    @Test
    void deveListarVazioSemExecutarConsultaDeTotais() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();

        verifyNoInteractions(
                itemOrcamentoRepository,
                materialOrcamentoRepository,
                maoDeObraOrcamentoRepository,
                despesaOrcamentoRepository);
    }

    @Test
    void deveAtualizarSomenteObservacaoPreservandoClienteEStatusInativosOmitidos() {
        Cliente clienteInativo = cliente(10L, "Cliente", false);
        StatusOrcamento statusInativo = status(2L, "Enviado", false);
        Orcamento orcamento = orcamento(5L, 1234L, clienteInativo, statusInativo, "Anterior");
        OrcamentoUpdateRequest request = new OrcamentoUpdateRequest();
        request.setObservacao("Nova");
        when(repository.findById(5L)).thenReturn(Optional.of(orcamento));
        when(repository.saveAndFlush(orcamento)).thenReturn(orcamento);
        when(itemOrcamentoRepository.somarValorTotalPorOrcamentos(List.of(5L)))
                .thenReturn(List.of(new TotalComercialOrcamentoProjection(
                        5L, new BigDecimal("425.00"))));
        when(materialOrcamentoRepository.somarCustoTotalPorOrcamentos(List.of(5L)))
                .thenReturn(List.of(new CustoTotalMateriaisOrcamentoProjection(
                        5L, new BigDecimal("210.00"))));
        when(maoDeObraOrcamentoRepository.somarCustoTotalPorOrcamentos(List.of(5L)))
                .thenReturn(List.of(new CustoTotalMaoDeObraOrcamentoProjection(
                        5L, new BigDecimal("125.25"))));
        when(despesaOrcamentoRepository.somarValorPorOrcamento(5L))
                .thenReturn(new BigDecimal("20.00"));

        OrcamentoResponse response = service.atualizar(5L, request);

        assertThat(response.getObservacao()).isEqualTo("Nova");
        assertThat(response.getTotalComercial()).isEqualTo(new BigDecimal("425.00"));
        assertThat(response.getCustoTotalMateriais()).isEqualTo(new BigDecimal("210.00"));
        assertThat(response.getCustoTotalMaoDeObra()).isEqualTo(new BigDecimal("125.25"));
        assertThat(response.getCustoTotalDespesas()).isEqualTo(new BigDecimal("20.00"));
        assertThat(response.getMargemPrevista()).isEqualTo(new BigDecimal("69.75"));
        assertThat(response.getPercentualMargem()).isEqualTo(new BigDecimal("16.41"));
        assertThat(orcamento.getCliente()).isSameAs(clienteInativo);
        assertThat(orcamento.getStatusOrcamento()).isSameAs(statusInativo);
        verifyNoInteractions(clienteRepository, statusOrcamentoRepository);
    }

    @Test
    void devePreservarObservacaoQuandoOmitida() {
        Orcamento orcamento = orcamento(
                5L, 1234L, cliente(10L, "Cliente", true), status(1L, "Rascunho", true), "Preservar");
        OrcamentoUpdateRequest request = new OrcamentoUpdateRequest();
        when(repository.findById(5L)).thenReturn(Optional.of(orcamento));
        when(repository.saveAndFlush(orcamento)).thenReturn(orcamento);

        OrcamentoResponse response = service.atualizar(5L, request);

        assertThat(request.isObservacaoInformada()).isFalse();
        assertThat(response.getObservacao()).isEqualTo("Preservar");
    }

    @Test
    void deveLimparObservacaoQuandoNullForInformado() {
        Orcamento orcamento = orcamento(
                5L, 1234L, cliente(10L, "Cliente", true), status(1L, "Rascunho", true), "Remover");
        OrcamentoUpdateRequest request = new OrcamentoUpdateRequest();
        request.setObservacao(null);
        when(repository.findById(5L)).thenReturn(Optional.of(orcamento));
        when(repository.saveAndFlush(orcamento)).thenReturn(orcamento);

        OrcamentoResponse response = service.atualizar(5L, request);

        assertThat(response.getObservacao()).isNull();
    }

    @Test
    void devePermitirTrocarClienteEmQualquerStatus() {
        Orcamento orcamento = orcamento(
                5L, 1234L, cliente(10L, "Anterior", true), status(3L, "Aprovado", true), null);
        Cliente novoCliente = cliente(20L, "Novo", true);
        OrcamentoUpdateRequest request = new OrcamentoUpdateRequest();
        request.setClienteId(20L);
        when(repository.findById(5L)).thenReturn(Optional.of(orcamento));
        when(clienteRepository.findById(20L)).thenReturn(Optional.of(novoCliente));
        when(repository.saveAndFlush(orcamento)).thenReturn(orcamento);

        OrcamentoResponse response = service.atualizar(5L, request);

        assertThat(response.getCliente().getId()).isEqualTo(20L);
        assertThat(response.getStatus().getNome()).isEqualTo("Aprovado");
    }

    @Test
    void deveRejeitarTrocaParaClienteInativo() {
        Orcamento orcamento = orcamento(
                5L, 1234L, cliente(10L, "Atual", true), status(1L, "Rascunho", true), null);
        OrcamentoUpdateRequest request = new OrcamentoUpdateRequest();
        request.setClienteId(20L);
        when(repository.findById(5L)).thenReturn(Optional.of(orcamento));
        when(clienteRepository.findById(20L)).thenReturn(Optional.of(cliente(20L, "Inativo", false)));

        assertThatThrownBy(() -> service.atualizar(5L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular um orçamento a um cliente inativo.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveAlterarParaQualquerStatusAtivoSemRegraDeTransicao() {
        Orcamento orcamento = orcamento(
                5L, 1234L, cliente(10L, "Cliente", true), status(1L, "Rascunho", true), null);
        StatusOrcamento cancelado = status(5L, "Cancelado", true);
        OrcamentoUpdateRequest request = new OrcamentoUpdateRequest();
        request.setStatusOrcamentoId(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(orcamento));
        when(statusOrcamentoRepository.findById(5L)).thenReturn(Optional.of(cancelado));
        when(repository.saveAndFlush(orcamento)).thenReturn(orcamento);

        OrcamentoResponse response = service.atualizar(5L, request);

        assertThat(response.getStatus().getNome()).isEqualTo("Cancelado");
    }

    @Test
    void deveRejeitarStatusInexistenteNaAtualizacao() {
        Orcamento orcamento = orcamento(
                5L, 1234L, cliente(10L, "Cliente", true), status(1L, "Rascunho", true), null);
        OrcamentoUpdateRequest request = new OrcamentoUpdateRequest();
        request.setStatusOrcamentoId(99L);
        when(repository.findById(5L)).thenReturn(Optional.of(orcamento));
        when(statusOrcamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(5L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Status de orçamento não encontrado. Id: 99");
    }

    @Test
    void deveRejeitarStatusInativoNaAtualizacao() {
        Orcamento orcamento = orcamento(
                5L, 1234L, cliente(10L, "Cliente", true), status(1L, "Rascunho", true), null);
        OrcamentoUpdateRequest request = new OrcamentoUpdateRequest();
        request.setStatusOrcamentoId(2L);
        when(repository.findById(5L)).thenReturn(Optional.of(orcamento));
        when(statusOrcamentoRepository.findById(2L))
                .thenReturn(Optional.of(status(2L, "Enviado", false)));

        assertThatThrownBy(() -> service.atualizar(5L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível selecionar um status de orçamento inativo.");

        verify(repository, never()).saveAndFlush(any());
    }

    private OrcamentoRequest request(Long clienteId, String observacao) {
        OrcamentoRequest request = new OrcamentoRequest();
        request.setClienteId(clienteId);
        request.setObservacao(observacao);
        return request;
    }

    private static Stream<Arguments> cenariosDeMargem() {
        return Stream.of(
                Arguments.of("0.00", "0.00", "0.00", "0.00", "0.00", "0.00"),
                Arguments.of("1000.00", "0.00", "0.00", "0.00", "1000.00", "100.00"),
                Arguments.of("1000.00", "200.00", "0.00", "0.00", "800.00", "80.00"),
                Arguments.of("1000.00", "0.00", "300.00", "0.00", "700.00", "70.00"),
                Arguments.of("1000.00", "0.00", "0.00", "250.00", "750.00", "75.00"),
                Arguments.of("1000.00", "200.00", "300.00", "100.00", "400.00", "40.00"),
                Arguments.of("1000.00", "400.00", "300.00", "300.00", "0.00", "0.00"),
                Arguments.of("1000.00", "400.00", "400.00", "500.00", "-300.00", "-30.00"),
                Arguments.of("6.00", "1.00", "1.00", "3.00", "1.00", "16.67"),
                Arguments.of("0.00", "60.00", "40.00", "50.00", "-150.00", "0.00"));
    }

    private Cliente cliente(Long id, String nome, boolean ativo) {
        return Cliente.builder().id(id).nome(nome).ativo(ativo).build();
    }

    private StatusOrcamento status(Long id, String nome, boolean ativo) {
        return StatusOrcamento.builder().id(id).nome(nome).ativo(ativo).build();
    }

    private Orcamento orcamento(
            Long id,
            Long numero,
            Cliente cliente,
            StatusOrcamento status,
            String observacao) {

        return Orcamento.builder()
                .id(id)
                .numero(numero)
                .cliente(cliente)
                .statusOrcamento(status)
                .observacao(observacao)
                .criadoEm(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
    }
}
