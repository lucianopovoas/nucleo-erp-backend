package br.com.nucleodasreformas.nucleoerp.material.service;

import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialRequest;
import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialResponse;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private MaterialRepository repository;

    @InjectMocks
    private MaterialService service;

    @Test
    void deveSalvarMaterialValidoAtivoPorPadrao() {
        when(repository.save(any(Material.class))).thenAnswer(invocation -> {
            Material material = invocation.getArgument(0);
            material.setId(1L);
            return material;
        });

        MaterialResponse response = service.salvar(request("Lona"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNome()).isEqualTo("Lona");
        assertThat(response.getDescricao()).isEqualTo("Descrição");
        assertThat(response.getUnidade()).isEqualTo("M2");
        assertThat(response.getLargura()).isEqualByComparingTo("1.50");
        assertThat(response.getAtivo()).isTrue();
    }

    @Test
    void deveBuscarMaterialExistente() {
        when(repository.findById(1L)).thenReturn(Optional.of(material(1L, "Lona", true)));

        MaterialResponse response = service.buscarPorId(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNome()).isEqualTo("Lona");
    }

    @Test
    void deveFalharAoBuscarMaterialInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveListarMateriaisConformeComportamentoAtualInclusiveInativos() {
        when(repository.findAll()).thenReturn(List.of(
                material(1L, "Ativo", true), material(2L, "Inativo", false)));

        List<MaterialResponse> responses = service.listar();

        assertThat(responses).extracting(MaterialResponse::getAtivo).containsExactly(true, false);
    }

    @Test
    void deveAtualizarMaterialExistente() {
        Material material = material(1L, "Antigo", true);
        when(repository.findById(1L)).thenReturn(Optional.of(material));
        when(repository.save(material)).thenReturn(material);

        MaterialResponse response = service.atualizar(1L, request("Atualizado"));

        assertThat(response.getNome()).isEqualTo("Atualizado");
        assertThat(response.getLargura()).isEqualByComparingTo("1.50");
        verify(repository).save(material);
    }

    @Test
    void deveFalharAoAtualizarMaterialInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, request("Material")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveDeletarMaterialLogicamente() {
        Material material = material(1L, "Lona", true);
        when(repository.findById(1L)).thenReturn(Optional.of(material));

        service.deletar(1L);

        assertThat(material.getAtivo()).isFalse();
        verify(repository).save(material);
    }

    @Test
    void deveFalharAoDeletarMaterialInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(99L)).isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    private MaterialRequest request(String nome) {
        MaterialRequest request = new MaterialRequest();
        request.setNome(nome);
        request.setDescricao("Descrição");
        request.setUnidade("M2");
        request.setLargura(new BigDecimal("1.50"));
        return request;
    }

    private Material material(Long id, String nome, boolean ativo) {
        return Material.builder().id(id).nome(nome).unidade("M2").ativo(ativo).build();
    }
}
