package br.com.nucleodasreformas.nucleoerp.fornecedor.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorRequest;
import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.fornecedor.entity.Fornecedor;
import br.com.nucleodasreformas.nucleoerp.fornecedor.repository.FornecedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {

    @Mock
    private FornecedorRepository repository;

    @InjectMocks
    private FornecedorService service;

    @Test
    void deveSalvarFornecedorComEmail() {
        FornecedorRequest request = request("Fornecedor com email", "contato@fornecedor.com");
        when(repository.save(any(Fornecedor.class))).thenAnswer(invocation -> {
            Fornecedor fornecedor = invocation.getArgument(0);
            fornecedor.setId(1L);
            return fornecedor;
        });

        FornecedorResponse response = service.salvar(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("contato@fornecedor.com");
        verify(repository).save(any(Fornecedor.class));
    }

    @Test
    void deveSalvarFornecedorSemEmail() {
        FornecedorRequest request = request("Fornecedor sem email", null);
        when(repository.save(any(Fornecedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FornecedorResponse response = service.salvar(request);

        assertThat(response.getEmail()).isNull();
        assertThat(response.getAtivo()).isTrue();
    }

    @Test
    void deveRejeitarFornecedorComNomeDuplicado() {
        FornecedorRequest request = request("Fornecedor", null);
        when(repository.existsByNome("Fornecedor")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request)).isInstanceOf(BusinessException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deveBuscarFornecedorExistente() {
        when(repository.findById(1L)).thenReturn(Optional.of(fornecedor(1L, "Fornecedor", true)));

        FornecedorResponse response = service.buscarPorId(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNome()).isEqualTo("Fornecedor");
    }

    @Test
    void deveFalharAoBuscarFornecedorInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveListarFornecedoresConformeComportamentoAtualInclusiveInativos() {
        when(repository.findAll()).thenReturn(List.of(
                fornecedor(1L, "Ativo", true), fornecedor(2L, "Inativo", false)));

        List<FornecedorResponse> responses = service.listar();

        assertThat(responses).extracting(FornecedorResponse::getAtivo).containsExactly(true, false);
    }

    @Test
    void deveAtualizarEmailDoFornecedor() {
        Fornecedor fornecedor = Fornecedor.builder()
                .id(1L)
                .nome("Fornecedor")
                .email("antigo@fornecedor.com")
                .build();
        FornecedorRequest request = request("Fornecedor", "novo@fornecedor.com");

        when(repository.findById(1L)).thenReturn(Optional.of(fornecedor));
        when(repository.save(fornecedor)).thenReturn(fornecedor);

        FornecedorResponse response = service.atualizar(1L, request);

        assertThat(fornecedor.getEmail()).isEqualTo("novo@fornecedor.com");
        assertThat(response.getEmail()).isEqualTo("novo@fornecedor.com");
        verify(repository).existsByNomeAndIdNot("Fornecedor", 1L);
    }

    @Test
    void devePermitirRemoverEmailNaAtualizacao() {
        Fornecedor fornecedor = Fornecedor.builder()
                .id(1L)
                .nome("Fornecedor")
                .email("contato@fornecedor.com")
                .build();
        FornecedorRequest request = request("Fornecedor", null);

        when(repository.findById(1L)).thenReturn(Optional.of(fornecedor));
        when(repository.save(fornecedor)).thenReturn(fornecedor);

        FornecedorResponse response = service.atualizar(1L, request);

        assertThat(fornecedor.getEmail()).isNull();
        assertThat(response.getEmail()).isNull();
    }

    @Test
    void deveManterMesmoNomeNaAtualizacaoSemConflitarComProprioRegistro() {
        Fornecedor fornecedor = fornecedor(1L, "Fornecedor", true);
        FornecedorRequest request = request("Fornecedor", null);
        when(repository.findById(1L)).thenReturn(Optional.of(fornecedor));
        when(repository.existsByNomeAndIdNot("Fornecedor", 1L)).thenReturn(false);
        when(repository.save(fornecedor)).thenReturn(fornecedor);

        FornecedorResponse response = service.atualizar(1L, request);

        assertThat(response.getNome()).isEqualTo("Fornecedor");
        verify(repository).existsByNomeAndIdNot("Fornecedor", 1L);
    }

    @Test
    void deveRejeitarAtualizacaoParaNomePertencenteAOutroFornecedor() {
        Fornecedor fornecedor = fornecedor(1L, "Fornecedor A", true);
        FornecedorRequest request = request("Fornecedor B", null);
        when(repository.findById(1L)).thenReturn(Optional.of(fornecedor));
        when(repository.existsByNomeAndIdNot("Fornecedor B", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, request)).isInstanceOf(BusinessException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deveFalharAoAtualizarFornecedorInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, request("Fornecedor", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveDeletarFornecedorLogicamente() {
        Fornecedor fornecedor = fornecedor(1L, "Fornecedor", true);
        when(repository.findById(1L)).thenReturn(Optional.of(fornecedor));

        service.deletar(1L);

        assertThat(fornecedor.getAtivo()).isFalse();
        verify(repository).save(fornecedor);
    }

    @Test
    void deveFalharAoDeletarFornecedorInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(99L)).isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    private FornecedorRequest request(String nome, String email) {
        FornecedorRequest request = new FornecedorRequest();
        request.setNome(nome);
        request.setEmail(email);
        return request;
    }

    private Fornecedor fornecedor(Long id, String nome, boolean ativo) {
        return Fornecedor.builder().id(id).nome(nome).ativo(ativo).build();
    }
}
