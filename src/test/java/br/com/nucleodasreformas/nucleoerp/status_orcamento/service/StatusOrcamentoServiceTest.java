package br.com.nucleodasreformas.nucleoerp.status_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusOrcamentoServiceTest {

    @Mock
    private StatusOrcamentoRepository repository;

    @InjectMocks
    private StatusOrcamentoService service;

    @Test
    void deveSalvarStatusAtivoComNomeSemEspacosExternos() {
        StatusOrcamentoRequest request = request("  Em análise  ", false);
        when(repository.saveAndFlush(any(StatusOrcamento.class))).thenAnswer(invocation -> {
            StatusOrcamento status = invocation.getArgument(0);
            status.setId(6L);
            return status;
        });

        StatusOrcamentoResponse response = service.salvar(request);

        assertThat(response.getId()).isEqualTo(6L);
        assertThat(response.getCodigo()).isEqualTo("STATUS_TESTE");
        assertThat(response.getNome()).isEqualTo("Em análise");
        assertThat(response.getAtivo()).isTrue();
        verify(repository).existsByNomeNormalizado("Em análise");
    }

    @Test
    void deveRejeitarNomeDuplicadoIndependentementeDoEstadoDoRegistroExistente() {
        StatusOrcamentoRequest request = request("Rascunho", null);
        when(repository.existsByNomeNormalizado("Rascunho")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe um status de orçamento com esse nome.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarDuplicidadeComDiferencaDeCaixa() {
        StatusOrcamentoRequest request = request("rascunho", null);
        when(repository.existsByNomeNormalizado("rascunho")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request)).isInstanceOf(BusinessException.class);

        verify(repository).existsByNomeNormalizado("rascunho");
    }

    @Test
    void deveRejeitarDuplicidadeDepoisDeRemoverEspacosExternos() {
        StatusOrcamentoRequest request = request("  Rascunho  ", null);
        when(repository.existsByNomeNormalizado("Rascunho")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request)).isInstanceOf(BusinessException.class);

        verify(repository).existsByNomeNormalizado("Rascunho");
    }

    @Test
    void deveConverterViolacaoConcorrenteDoIndiceUnicoEmErroDeNegocio() {
        ConstraintViolationException constraint = new ConstraintViolationException(
                "duplicidade", new SQLException(), "uk_status_orcamento_nome_normalizado");
        when(repository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("índice", constraint));

        assertThatThrownBy(() -> service.salvar(request("Em análise", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe um status de orçamento com esse nome.");
    }

    @Test
    void naoDeveMascararViolacaoDeIntegridadeNaoRelacionadaAoIndiceDeNome() {
        DataIntegrityViolationException erro = new DataIntegrityViolationException("outra violação");
        when(repository.saveAndFlush(any())).thenThrow(erro);

        assertThatThrownBy(() -> service.salvar(request("Em análise", null))).isSameAs(erro);
    }

    @Test
    void deveBuscarStatusExistenteInclusiveInativo() {
        when(repository.findById(1L)).thenReturn(Optional.of(status(1L, "Rascunho", false)));

        StatusOrcamentoResponse response = service.buscarPorId(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAtivo()).isFalse();
    }

    @Test
    void deveFalharAoBuscarStatusInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Status de orçamento não encontrado. Id: 99");
    }

    @Test
    void deveListarSomenteStatusAtivos() {
        when(repository.findByAtivoTrue()).thenReturn(List.of(status(1L, "Rascunho", true)));

        List<StatusOrcamentoResponse> responses = service.listar();

        assertThat(responses).singleElement().extracting(StatusOrcamentoResponse::getAtivo).isEqualTo(true);
        verify(repository).findByAtivoTrue();
    }

    @Test
    void deveAtualizarNomeMantendoOProprioNomeNormalizado() {
        StatusOrcamento status = status(1L, "Rascunho", true);
        when(repository.findById(1L)).thenReturn(Optional.of(status));
        when(repository.saveAndFlush(status)).thenReturn(status);

        StatusOrcamentoResponse response = service.atualizar(
                1L, updateRequest("  Rascunho  ", null));

        assertThat(response.getNome()).isEqualTo("Rascunho");
        assertThat(response.getAtivo()).isTrue();
        verify(repository).existsByNomeNormalizadoAndIdNot("Rascunho", 1L);
    }

    @Test
    void deveAtualizarEInativarStatusPorPut() {
        StatusOrcamento status = status(1L, "Em análise", true);
        when(repository.findById(1L)).thenReturn(Optional.of(status));
        when(repository.saveAndFlush(status)).thenReturn(status);

        StatusOrcamentoResponse response = service.atualizar(1L, updateRequest("Revisão", false));

        assertThat(response.getNome()).isEqualTo("Revisão");
        assertThat(response.getAtivo()).isFalse();
    }

    @Test
    void deveReativarStatusInativoPorPut() {
        StatusOrcamento status = status(1L, "Em análise", false);
        when(repository.findById(1L)).thenReturn(Optional.of(status));
        when(repository.saveAndFlush(status)).thenReturn(status);

        StatusOrcamentoResponse response = service.atualizar(1L, updateRequest("Em análise", true));

        assertThat(response.getAtivo()).isTrue();
    }

    @Test
    void devePreservarAtivoQuandoOmitidoNaAtualizacao() {
        StatusOrcamento status = status(1L, "Em análise", false);
        when(repository.findById(1L)).thenReturn(Optional.of(status));
        when(repository.saveAndFlush(status)).thenReturn(status);

        StatusOrcamentoResponse response = service.atualizar(1L, updateRequest("Revisão", null));

        assertThat(response.getAtivo()).isFalse();
    }

    @Test
    void deveRejeitarNomeDeOutroStatusNaAtualizacao() {
        StatusOrcamento status = status(1L, "Em análise", true);
        when(repository.findById(1L)).thenReturn(Optional.of(status));
        when(repository.existsByNomeNormalizadoAndIdNot("Enviado", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, updateRequest("Enviado", null)))
                .isInstanceOf(BusinessException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveFalharAoAtualizarStatusInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, updateRequest("Revisão", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveDeletarStatusLogicamente() {
        StatusOrcamento status = status(1L, "Rascunho", true);
        when(repository.findById(1L)).thenReturn(Optional.of(status));

        service.deletar(1L);

        assertThat(status.getAtivo()).isFalse();
        verify(repository).save(status);
    }

    @Test
    void deveFalharAoDeletarStatusInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(99L)).isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    private StatusOrcamentoRequest request(String nome, Boolean ativo) {
        StatusOrcamentoRequest request = new StatusOrcamentoRequest();
        request.setCodigo(" status_teste ");
        request.setNome(nome);
        return request;
    }

    private StatusOrcamentoUpdateRequest updateRequest(String nome, Boolean ativo) {
        StatusOrcamentoUpdateRequest request = new StatusOrcamentoUpdateRequest();
        request.setNome(nome);
        request.setAtivo(ativo);
        return request;
    }

    private StatusOrcamento status(Long id, String nome, boolean ativo) {
        return StatusOrcamento.builder()
                .id(id)
                .codigo("STATUS_" + id)
                .nome(nome)
                .ativo(ativo)
                .criadoEm(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
    }
}
