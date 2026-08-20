package br.com.nucleodasreformas.nucleoerp.categoria_servico.service;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.dto.CategoriaServicoRequest;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.dto.CategoriaServicoResponse;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.repository.CategoriaServicoRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.servico.repository.ServicoRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServicoServiceTest {

    @Mock
    private CategoriaServicoRepository repository;

    @Mock
    private ServicoRepository servicoRepository;

    @InjectMocks
    private CategoriaServicoService service;

    @Test
    void deveSalvarCategoriaAtivaComNomeNormalizado() {
        CategoriaServicoRequest request = request("  Pintura  ", false);
        when(repository.existsByNomeNormalizado("Pintura")).thenReturn(false);
        when(repository.saveAndFlush(any(CategoriaServico.class))).thenAnswer(invocation -> {
            CategoriaServico categoria = invocation.getArgument(0);
            categoria.setId(1L);
            return categoria;
        });

        CategoriaServicoResponse response = service.salvar(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNome()).isEqualTo("Pintura");
        assertThat(response.getAtivo()).isTrue();
        verify(repository).existsByNomeNormalizado("Pintura");
    }

    @Test
    void deveRejeitarNomeDuplicadoIndependentementeDoEstadoDoRegistroExistente() {
        CategoriaServicoRequest request = request("pintura", null);
        when(repository.existsByNomeNormalizado("pintura")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe uma categoria de serviço com esse nome.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveConverterViolacaoConcorrenteDoIndiceUnicoEmErroDeNegocio() {
        CategoriaServicoRequest request = request("Pintura", null);
        ConstraintViolationException constraint = new ConstraintViolationException(
                "duplicidade", new SQLException(), "uk_categoria_servico_nome_normalizado");
        when(repository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("indice", constraint));

        assertThatThrownBy(() -> service.salvar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe uma categoria de serviço com esse nome.");
    }

    @Test
    void naoDeveMascararViolacaoDeIntegridadeNaoRelacionadaAoIndiceDeNome() {
        CategoriaServicoRequest request = request("Pintura", null);
        DataIntegrityViolationException erro = new DataIntegrityViolationException("outra violação");
        when(repository.saveAndFlush(any())).thenThrow(erro);

        assertThatThrownBy(() -> service.salvar(request)).isSameAs(erro);
    }

    @Test
    void deveBuscarCategoriaExistenteInclusiveInativa() {
        when(repository.findById(1L)).thenReturn(Optional.of(categoria(1L, "Pintura", false)));

        CategoriaServicoResponse response = service.buscarPorId(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAtivo()).isFalse();
    }

    @Test
    void deveFalharAoBuscarCategoriaInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Categoria de serviço não encontrada. Id: 99");
    }

    @Test
    void deveListarSomenteCategoriasAtivas() {
        when(repository.findByAtivoTrue()).thenReturn(List.of(categoria(1L, "Pintura", true)));

        List<CategoriaServicoResponse> responses = service.listar();

        assertThat(responses).singleElement().extracting(CategoriaServicoResponse::getAtivo).isEqualTo(true);
        verify(repository).findByAtivoTrue();
    }

    @Test
    void deveAtualizarEReativarCategoriaInativa() {
        CategoriaServico categoria = categoria(1L, "Pintura antiga", false);
        CategoriaServicoRequest request = request("  Pintura nova  ", true);
        when(repository.findById(1L)).thenReturn(Optional.of(categoria));
        when(repository.existsByNomeNormalizadoAndIdNot("Pintura nova", 1L)).thenReturn(false);
        when(repository.saveAndFlush(categoria)).thenReturn(categoria);

        CategoriaServicoResponse response = service.atualizar(1L, request);

        assertThat(response.getNome()).isEqualTo("Pintura nova");
        assertThat(response.getAtivo()).isTrue();
        verify(repository).existsByNomeNormalizadoAndIdNot("Pintura nova", 1L);
        verifyNoInteractions(servicoRepository);
    }

    @Test
    void deveInativarServicosAoInativarCategoriaPorPut() {
        CategoriaServico categoria = categoria(1L, "Pintura", true);
        CategoriaServicoRequest request = request("Pintura", false);
        when(repository.findById(1L)).thenReturn(Optional.of(categoria));
        when(repository.saveAndFlush(categoria)).thenReturn(categoria);

        CategoriaServicoResponse response = service.atualizar(1L, request);

        assertThat(response.getAtivo()).isFalse();
        verify(servicoRepository).inativarAtivosPorCategoriaId(1L);
    }

    @Test
    void devePreservarAtivoQuandoOmitidoNaAtualizacao() {
        CategoriaServico categoria = categoria(1L, "Pintura", false);
        CategoriaServicoRequest request = request("Pintura atualizada", null);
        when(repository.findById(1L)).thenReturn(Optional.of(categoria));
        when(repository.saveAndFlush(categoria)).thenReturn(categoria);

        CategoriaServicoResponse response = service.atualizar(1L, request);

        assertThat(response.getAtivo()).isFalse();
        verify(servicoRepository).inativarAtivosPorCategoriaId(1L);
    }

    @Test
    void deveRejeitarNomeDeOutraCategoriaNaAtualizacao() {
        CategoriaServico categoria = categoria(1L, "Pintura", true);
        CategoriaServicoRequest request = request("Elétrica", null);
        when(repository.findById(1L)).thenReturn(Optional.of(categoria));
        when(repository.existsByNomeNormalizadoAndIdNot("Elétrica", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, request))
                .isInstanceOf(BusinessException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveFalharAoAtualizarCategoriaInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, request("Pintura", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveDeletarCategoriaLogicamente() {
        CategoriaServico categoria = categoria(1L, "Pintura", true);
        when(repository.findById(1L)).thenReturn(Optional.of(categoria));

        service.deletar(1L);

        assertThat(categoria.getAtivo()).isFalse();
        verify(repository).save(categoria);
        verify(servicoRepository).inativarAtivosPorCategoriaId(1L);
    }

    @Test
    void deveFalharAoDeletarCategoriaInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(99L)).isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    private CategoriaServicoRequest request(String nome, Boolean ativo) {
        CategoriaServicoRequest request = new CategoriaServicoRequest();
        request.setNome(nome);
        request.setAtivo(ativo);
        return request;
    }

    private CategoriaServico categoria(Long id, String nome, boolean ativo) {
        return CategoriaServico.builder()
                .id(id)
                .nome(nome)
                .ativo(ativo)
                .criadoEm(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
    }
}
