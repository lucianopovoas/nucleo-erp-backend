package br.com.nucleodasreformas.nucleoerp.servico.service;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.repository.CategoriaServicoRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.servico.dto.ServicoRequest;
import br.com.nucleodasreformas.nucleoerp.servico.dto.ServicoResponse;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
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
class ServicoServiceTest {

    @Mock
    private ServicoRepository repository;

    @Mock
    private CategoriaServicoRepository categoriaServicoRepository;

    @InjectMocks
    private ServicoService service;

    @Test
    void deveSalvarServicoAtivoComNomeNormalizado() {
        CategoriaServico categoria = categoria(3L, "Toldos", true);
        when(categoriaServicoRepository.findById(3L)).thenReturn(Optional.of(categoria));
        when(repository.existsByCategoriaENomeNormalizado(3L, "Instalação")).thenReturn(false);
        when(repository.saveAndFlush(any(Servico.class))).thenAnswer(invocation -> {
            Servico servico = invocation.getArgument(0);
            servico.setId(10L);
            return servico;
        });

        ServicoResponse response = service.salvar(request("  Instalação  ", 3L, false));

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getNome()).isEqualTo("Instalação");
        assertThat(response.getCategoriaServico().getId()).isEqualTo(3L);
        assertThat(response.getAtivo()).isTrue();
    }

    @Test
    void deveRejeitarCategoriaInexistenteAoSalvar() {
        when(categoriaServicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(request("Instalação", 99L, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Categoria de serviço não encontrada. Id: 99");

        verifyNoInteractions(repository);
    }

    @Test
    void deveRejeitarCategoriaInativaAoSalvar() {
        when(categoriaServicoRepository.findById(3L)).thenReturn(Optional.of(categoria(3L, "Toldos", false)));

        assertThatThrownBy(() -> service.salvar(request("Instalação", 3L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular um serviço a uma categoria inativa.");

        verifyNoInteractions(repository);
    }

    @Test
    void deveRejeitarNomeDuplicadoNaMesmaCategoriaInclusiveInativo() {
        when(categoriaServicoRepository.findById(3L)).thenReturn(Optional.of(categoria(3L, "Toldos", true)));
        when(repository.existsByCategoriaENomeNormalizado(3L, "instalação")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request("instalação", 3L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe um serviço com esse nome nesta categoria.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void devePermitirMesmoNomeEmCategoriaDiferente() {
        CategoriaServico categoria = categoria(4L, "Comunicação visual", true);
        when(categoriaServicoRepository.findById(4L)).thenReturn(Optional.of(categoria));
        when(repository.existsByCategoriaENomeNormalizado(4L, "Instalação")).thenReturn(false);
        when(repository.saveAndFlush(any(Servico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServicoResponse response = service.salvar(request("Instalação", 4L, null));

        assertThat(response.getCategoriaServico().getId()).isEqualTo(4L);
    }

    @Test
    void deveConverterConflitoConcorrenteDoIndiceEmErroDeNegocio() {
        when(categoriaServicoRepository.findById(3L)).thenReturn(Optional.of(categoria(3L, "Toldos", true)));
        ConstraintViolationException constraint = new ConstraintViolationException(
                "duplicidade", new SQLException(), "uk_servico_categoria_nome_normalizado");
        when(repository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("indice", constraint));

        assertThatThrownBy(() -> service.salvar(request("Instalação", 3L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe um serviço com esse nome nesta categoria.");
    }

    @Test
    void naoDeveMascararViolacaoDeIntegridadeNaoRelacionadaAoIndiceDeNome() {
        when(categoriaServicoRepository.findById(3L)).thenReturn(Optional.of(categoria(3L, "Toldos", true)));
        DataIntegrityViolationException erro = new DataIntegrityViolationException("outra violação");
        when(repository.saveAndFlush(any())).thenThrow(erro);

        assertThatThrownBy(() -> service.salvar(request("Instalação", 3L, null))).isSameAs(erro);
    }

    @Test
    void deveBuscarServicoExistenteInclusiveInativo() {
        when(repository.findById(10L)).thenReturn(Optional.of(servico(10L, "Instalação", false,
                categoria(3L, "Toldos", true))));

        ServicoResponse response = service.buscarPorId(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getAtivo()).isFalse();
    }

    @Test
    void deveFalharAoBuscarServicoInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Serviço não encontrado. Id: 99");
    }

    @Test
    void deveListarSomenteResultadoDaConsultaDeAtivos() {
        when(repository.findByAtivoTrue()).thenReturn(List.of(servico(
                10L, "Instalação", true, categoria(3L, "Toldos", true))));

        List<ServicoResponse> responses = service.listar();

        assertThat(responses).singleElement().extracting(ServicoResponse::getAtivo).isEqualTo(true);
        verify(repository).findByAtivoTrue();
    }

    @Test
    void deveAtualizarNomeMantendoCategoriaAtiva() {
        CategoriaServico categoria = categoria(3L, "Toldos", true);
        Servico servico = servico(10L, "Instalação", true, categoria);
        when(repository.findById(10L)).thenReturn(Optional.of(servico));
        when(repository.saveAndFlush(servico)).thenReturn(servico);

        ServicoResponse response = service.atualizar(10L, request("  Reforma  ", 3L, null));

        assertThat(response.getNome()).isEqualTo("Reforma");
        assertThat(response.getCategoriaServico().getId()).isEqualTo(3L);
        assertThat(response.getAtivo()).isTrue();
        verifyNoInteractions(categoriaServicoRepository);
    }

    @Test
    void deveMudarParaOutraCategoriaAtiva() {
        Servico servico = servico(10L, "Instalação", true, categoria(3L, "Toldos", true));
        CategoriaServico novaCategoria = categoria(4L, "Comunicação visual", true);
        when(repository.findById(10L)).thenReturn(Optional.of(servico));
        when(categoriaServicoRepository.findById(4L)).thenReturn(Optional.of(novaCategoria));
        when(repository.saveAndFlush(servico)).thenReturn(servico);

        ServicoResponse response = service.atualizar(10L, request("Instalação", 4L, null));

        assertThat(response.getCategoriaServico().getId()).isEqualTo(4L);
    }

    @Test
    void deveRejeitarMudancaParaCategoriaInativaMesmoPermanecendoInativo() {
        Servico servico = servico(10L, "Instalação", false, categoria(3L, "Toldos", true));
        when(repository.findById(10L)).thenReturn(Optional.of(servico));
        when(categoriaServicoRepository.findById(4L))
                .thenReturn(Optional.of(categoria(4L, "Inativa", false)));

        assertThatThrownBy(() -> service.atualizar(10L, request("Instalação", 4L, false)))
                .isInstanceOf(BusinessException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void devePermitirAtualizarServicoInativoPreservandoCategoriaInativa() {
        CategoriaServico categoriaInativa = categoria(3L, "Toldos", false);
        Servico servico = servico(10L, "Antigo", false, categoriaInativa);
        when(repository.findById(10L)).thenReturn(Optional.of(servico));
        when(repository.saveAndFlush(servico)).thenReturn(servico);

        ServicoResponse response = service.atualizar(10L, request("Atualizado", 3L, null));

        assertThat(response.getNome()).isEqualTo("Atualizado");
        assertThat(response.getAtivo()).isFalse();
        verifyNoInteractions(categoriaServicoRepository);
    }

    @Test
    void deveRejeitarAtivacaoQuandoCategoriaAtualEstaInativa() {
        Servico servico = servico(10L, "Instalação", false, categoria(3L, "Toldos", false));
        when(repository.findById(10L)).thenReturn(Optional.of(servico));

        assertThatThrownBy(() -> service.atualizar(10L, request("Instalação", 3L, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular um serviço a uma categoria inativa.");
    }

    @Test
    void deveReativarServicoQuandoCategoriaAtualEstaAtiva() {
        Servico servico = servico(10L, "Instalação", false, categoria(3L, "Toldos", true));
        when(repository.findById(10L)).thenReturn(Optional.of(servico));
        when(repository.saveAndFlush(servico)).thenReturn(servico);

        ServicoResponse response = service.atualizar(10L, request("Instalação", 3L, true));

        assertThat(response.getAtivo()).isTrue();
    }

    @Test
    void deveInativarServicoPorPut() {
        Servico servico = servico(10L, "Instalação", true, categoria(3L, "Toldos", true));
        when(repository.findById(10L)).thenReturn(Optional.of(servico));
        when(repository.saveAndFlush(servico)).thenReturn(servico);

        ServicoResponse response = service.atualizar(10L, request("Instalação", 3L, false));

        assertThat(response.getAtivo()).isFalse();
    }

    @Test
    void deveRejeitarConflitoNaAtualizacaoExcluindoOProprioId() {
        Servico servico = servico(10L, "Instalação", true, categoria(3L, "Toldos", true));
        when(repository.findById(10L)).thenReturn(Optional.of(servico));
        when(repository.existsByCategoriaENomeNormalizadoAndIdNot(3L, "Reforma", 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(10L, request("Reforma", 3L, null)))
                .isInstanceOf(BusinessException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveFalharAoAtualizarServicoInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, request("Instalação", 3L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveDeletarServicoLogicamente() {
        Servico servico = servico(10L, "Instalação", true, categoria(3L, "Toldos", true));
        when(repository.findById(10L)).thenReturn(Optional.of(servico));

        service.deletar(10L);

        assertThat(servico.getAtivo()).isFalse();
        verify(repository).save(servico);
    }

    private ServicoRequest request(String nome, Long categoriaServicoId, Boolean ativo) {
        ServicoRequest request = new ServicoRequest();
        request.setNome(nome);
        request.setCategoriaServicoId(categoriaServicoId);
        request.setAtivo(ativo);
        return request;
    }

    private CategoriaServico categoria(Long id, String nome, boolean ativo) {
        return CategoriaServico.builder().id(id).nome(nome).ativo(ativo).build();
    }

    private Servico servico(Long id, String nome, boolean ativo, CategoriaServico categoriaServico) {
        return Servico.builder()
                .id(id)
                .nome(nome)
                .categoriaServico(categoriaServico)
                .ativo(ativo)
                .criadoEm(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
    }
}
