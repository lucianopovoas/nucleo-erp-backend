package br.com.nucleodasreformas.nucleoerp.status_ordem_servico.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoRequest;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoResponse;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.repository.StatusOrdemServicoRepository;
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
class StatusOrdemServicoServiceTest {

    @Mock private StatusOrdemServicoRepository repository;
    @InjectMocks private StatusOrdemServicoService service;

    @Test
    void deveCriarAtivoNormalizandoCodigoENome() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            StatusOrdemServico status = invocation.getArgument(0);
            status.setId(10L);
            return status;
        });

        StatusOrdemServicoResponse response = service.salvar(
                request("  em_execucao  ", "  Em execução  "));

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCodigo()).isEqualTo("EM_EXECUCAO");
        assertThat(response.getNome()).isEqualTo("Em execução");
        assertThat(response.getAtivo()).isTrue();
        verify(repository).existsByCodigo("EM_EXECUCAO");
        verify(repository).existsByNomeNormalizado("Em execução");
    }

    @Test
    void deveRejeitarNomeDuplicadoAntesDePersistir() {
        when(repository.existsByNomeNormalizado("Em execução")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request("NOVO", "Em execução")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe um status de ordem de serviço com esse nome.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarCodigoDuplicadoAntesDePersistir() {
        when(repository.existsByCodigo("EM_EXECUCAO")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request("em_execucao", "Outro")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe um status de ordem de serviço com esse código.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveTraduzirConflitoConcorrenteDeNome() {
        ConstraintViolationException constraint = new ConstraintViolationException(
                "duplicidade", new SQLException(),
                "uk_status_ordem_servico_nome_normalizado");
        when(repository.saveAndFlush(any())).thenThrow(
                new DataIntegrityViolationException("índice", constraint));

        assertThatThrownBy(() -> service.salvar(request("NOVO", "Novo")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe um status de ordem de serviço com esse nome.");
    }

    @Test
    void deveTraduzirConflitoConcorrenteDeCodigo() {
        ConstraintViolationException constraint = new ConstraintViolationException(
                "duplicidade", new SQLException(), "uk_status_ordem_servico_codigo");
        when(repository.saveAndFlush(any())).thenThrow(
                new DataIntegrityViolationException("constraint", constraint));

        assertThatThrownBy(() -> service.salvar(request("NOVO", "Novo")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe um status de ordem de serviço com esse código.");
    }

    @Test
    void deveListarSomenteAtivosEBuscarInativoPorId() {
        when(repository.findByAtivoTrue()).thenReturn(List.of(status(1L, true)));
        when(repository.findById(2L)).thenReturn(Optional.of(status(2L, false)));

        assertThat(service.listar()).singleElement()
                .extracting(StatusOrdemServicoResponse::getAtivo).isEqualTo(true);
        assertThat(service.buscarPorId(2L).getAtivo()).isFalse();
    }

    @Test
    void deveAtualizarNomeEEstadoSemAlterarCodigo() {
        StatusOrdemServico status = status(1L, true);
        when(repository.findById(1L)).thenReturn(Optional.of(status));
        when(repository.saveAndFlush(status)).thenReturn(status);

        StatusOrdemServicoResponse response = service.atualizar(
                1L, update("  Concluído  ", false));

        assertThat(response.getCodigo()).isEqualTo("STATUS_1");
        assertThat(response.getNome()).isEqualTo("Concluído");
        assertThat(response.getAtivo()).isFalse();
        verify(repository).existsByNomeNormalizadoAndIdNot("Concluído", 1L);
    }

    @Test
    void devePreservarAtivoQuandoOmitidoNaAtualizacao() {
        StatusOrdemServico status = status(1L, false);
        when(repository.findById(1L)).thenReturn(Optional.of(status));
        when(repository.saveAndFlush(status)).thenReturn(status);

        assertThat(service.atualizar(1L, update("Revisado", null)).getAtivo()).isFalse();
    }

    @Test
    void deveRejeitarNomeDeOutroStatusNaAtualizacao() {
        when(repository.findById(1L)).thenReturn(Optional.of(status(1L, true)));
        when(repository.existsByNomeNormalizadoAndIdNot("Concluído", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, update("Concluído", true)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe um status de ordem de serviço com esse nome.");
    }

    @Test
    void deveInativarLogicamente() {
        StatusOrdemServico status = status(1L, true);
        when(repository.findById(1L)).thenReturn(Optional.of(status));

        service.deletar(1L);

        assertThat(status.getAtivo()).isFalse();
        verify(repository).save(status);
    }

    @Test
    void deveFalharParaRecursoInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Status de ordem de serviço não encontrado. Id: 99");
    }

    private StatusOrdemServicoRequest request(String codigo, String nome) {
        StatusOrdemServicoRequest request = new StatusOrdemServicoRequest();
        request.setCodigo(codigo);
        request.setNome(nome);
        return request;
    }

    private StatusOrdemServicoUpdateRequest update(String nome, Boolean ativo) {
        StatusOrdemServicoUpdateRequest request = new StatusOrdemServicoUpdateRequest();
        request.setNome(nome);
        request.setAtivo(ativo);
        return request;
    }

    private StatusOrdemServico status(Long id, boolean ativo) {
        return StatusOrdemServico.builder().id(id).codigo("STATUS_" + id)
                .nome("Status " + id).ativo(ativo)
                .criadoEm(LocalDateTime.of(2026, 8, 23, 10, 0)).build();
    }
}
