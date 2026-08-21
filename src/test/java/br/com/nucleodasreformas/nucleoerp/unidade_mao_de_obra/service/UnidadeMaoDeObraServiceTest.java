package br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.dto.UnidadeMaoDeObraRequest;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.dto.UnidadeMaoDeObraResponse;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.repository.UnidadeMaoDeObraRepository;
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
class UnidadeMaoDeObraServiceTest {

    @Mock
    private UnidadeMaoDeObraRepository repository;

    @InjectMocks
    private UnidadeMaoDeObraService service;

    @Test
    void deveSalvarUnidadeAtivaComNomeSemEspacosExternos() {
        UnidadeMaoDeObraRequest request = request("  Hora  ", false);
        when(repository.saveAndFlush(any(UnidadeMaoDeObra.class))).thenAnswer(invocation -> {
            UnidadeMaoDeObra unidade = invocation.getArgument(0);
            unidade.setId(1L);
            return unidade;
        });

        UnidadeMaoDeObraResponse response = service.salvar(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNome()).isEqualTo("Hora");
        assertThat(response.getAtivo()).isTrue();
        verify(repository).existsByNomeNormalizado("Hora");
    }

    @Test
    void deveRejeitarNomeDuplicadoIndependentementeDoEstadoDoRegistroExistente() {
        when(repository.existsByNomeNormalizado("Hora")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request("Hora", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe uma unidade de mão de obra com esse nome.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarDuplicidadeComDiferencaDeCaixa() {
        when(repository.existsByNomeNormalizado("hora")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request("hora", null)))
                .isInstanceOf(BusinessException.class);

        verify(repository).existsByNomeNormalizado("hora");
    }

    @Test
    void deveRejeitarDuplicidadeDepoisDeRemoverEspacosExternos() {
        when(repository.existsByNomeNormalizado("Hora")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request("  Hora  ", null)))
                .isInstanceOf(BusinessException.class);

        verify(repository).existsByNomeNormalizado("Hora");
    }

    @Test
    void deveConverterViolacaoConcorrenteDoIndiceUnicoEmErroDeNegocio() {
        ConstraintViolationException constraint = new ConstraintViolationException(
                "duplicidade", new SQLException(), "uk_unidade_mao_de_obra_nome_normalizado");
        when(repository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("índice", constraint));

        assertThatThrownBy(() -> service.salvar(request("Hora", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe uma unidade de mão de obra com esse nome.");
    }

    @Test
    void naoDeveMascararViolacaoDeIntegridadeNaoRelacionadaAoIndiceDeNome() {
        DataIntegrityViolationException erro = new DataIntegrityViolationException("outra violação");
        when(repository.saveAndFlush(any())).thenThrow(erro);

        assertThatThrownBy(() -> service.salvar(request("Hora", null))).isSameAs(erro);
    }

    @Test
    void deveBuscarUnidadeExistenteInclusiveInativa() {
        when(repository.findById(1L)).thenReturn(Optional.of(unidade(1L, "Hora", false)));

        UnidadeMaoDeObraResponse response = service.buscarPorId(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAtivo()).isFalse();
    }

    @Test
    void deveFalharAoBuscarUnidadeInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Unidade de mão de obra não encontrada. Id: 99");
    }

    @Test
    void deveListarSomenteUnidadesAtivas() {
        when(repository.findByAtivoTrue()).thenReturn(List.of(unidade(1L, "Hora", true)));

        List<UnidadeMaoDeObraResponse> responses = service.listar();

        assertThat(responses).singleElement()
                .extracting(UnidadeMaoDeObraResponse::getAtivo)
                .isEqualTo(true);
        verify(repository).findByAtivoTrue();
    }

    @Test
    void deveAtualizarNomeMantendoOProprioNomeNormalizado() {
        UnidadeMaoDeObra unidade = unidade(1L, "Hora", true);
        when(repository.findById(1L)).thenReturn(Optional.of(unidade));
        when(repository.saveAndFlush(unidade)).thenReturn(unidade);

        UnidadeMaoDeObraResponse response = service.atualizar(1L, request("  Hora  ", null));

        assertThat(response.getNome()).isEqualTo("Hora");
        assertThat(response.getAtivo()).isTrue();
        verify(repository).existsByNomeNormalizadoAndIdNot("Hora", 1L);
    }

    @Test
    void deveAtualizarEInativarUnidadePorPut() {
        UnidadeMaoDeObra unidade = unidade(1L, "Hora", true);
        when(repository.findById(1L)).thenReturn(Optional.of(unidade));
        when(repository.saveAndFlush(unidade)).thenReturn(unidade);

        UnidadeMaoDeObraResponse response = service.atualizar(1L, request("Diária", false));

        assertThat(response.getNome()).isEqualTo("Diária");
        assertThat(response.getAtivo()).isFalse();
    }

    @Test
    void deveReativarUnidadeInativaPorPut() {
        UnidadeMaoDeObra unidade = unidade(1L, "Hora", false);
        when(repository.findById(1L)).thenReturn(Optional.of(unidade));
        when(repository.saveAndFlush(unidade)).thenReturn(unidade);

        UnidadeMaoDeObraResponse response = service.atualizar(1L, request("Hora", true));

        assertThat(response.getAtivo()).isTrue();
    }

    @Test
    void devePreservarAtivoQuandoOmitidoNaAtualizacao() {
        UnidadeMaoDeObra unidade = unidade(1L, "Hora", false);
        when(repository.findById(1L)).thenReturn(Optional.of(unidade));
        when(repository.saveAndFlush(unidade)).thenReturn(unidade);

        UnidadeMaoDeObraResponse response = service.atualizar(1L, request("Diária", null));

        assertThat(response.getAtivo()).isFalse();
    }

    @Test
    void deveRejeitarNomeDeOutraUnidadeNaAtualizacao() {
        UnidadeMaoDeObra unidade = unidade(1L, "Hora", true);
        when(repository.findById(1L)).thenReturn(Optional.of(unidade));
        when(repository.existsByNomeNormalizadoAndIdNot("Diária", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, request("Diária", null)))
                .isInstanceOf(BusinessException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveFalharAoAtualizarUnidadeInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, request("Hora", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveDeletarUnidadeLogicamente() {
        UnidadeMaoDeObra unidade = unidade(1L, "Hora", true);
        when(repository.findById(1L)).thenReturn(Optional.of(unidade));

        service.deletar(1L);

        assertThat(unidade.getAtivo()).isFalse();
        verify(repository).save(unidade);
    }

    @Test
    void deveFalharAoDeletarUnidadeInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    private UnidadeMaoDeObraRequest request(String nome, Boolean ativo) {
        UnidadeMaoDeObraRequest request = new UnidadeMaoDeObraRequest();
        request.setNome(nome);
        request.setAtivo(ativo);
        return request;
    }

    private UnidadeMaoDeObra unidade(Long id, String nome, boolean ativo) {
        return UnidadeMaoDeObra.builder()
                .id(id)
                .nome(nome)
                .ativo(ativo)
                .criadoEm(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
    }
}
