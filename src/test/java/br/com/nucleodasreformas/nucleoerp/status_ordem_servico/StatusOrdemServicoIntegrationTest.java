package br.com.nucleodasreformas.nucleoerp.status_ordem_servico;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoRequest;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.repository.StatusOrdemServicoRepository;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.service.StatusOrdemServicoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class StatusOrdemServicoIntegrationTest {

    @Autowired private StatusOrdemServicoRepository repository;
    @Autowired private StatusOrdemServicoService service;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void deveConterSeedCanonicoAtivo() {
        assertThat(repository.findAll())
                .filteredOn(status -> Boolean.TRUE.equals(status.getAtivo()))
                .extracting(status -> status.getCodigo())
                .contains("COMPRAR_MATERIAL", "EM_EXECUCAO", "INSTALAR", "CONCLUIDO");
    }

    @Test
    void deveNormalizarCodigoPreservarIdentidadeERespeitarInativacao() {
        String sufixo = sufixo();
        StatusOrdemServicoRequest request = new StatusOrdemServicoRequest();
        request.setCodigo(" aguardando_" + sufixo.toLowerCase());
        request.setNome(" Aguardando " + sufixo + " ");

        var salvo = service.salvar(request);
        assertThat(salvo.getCodigo()).isEqualTo("AGUARDANDO_" + sufixo);
        assertThat(salvo.getNome()).isEqualTo("Aguardando " + sufixo);
        assertThat(salvo.getAtivo()).isTrue();

        StatusOrdemServicoUpdateRequest update = new StatusOrdemServicoUpdateRequest();
        update.setNome("Renomeado " + sufixo);
        update.setAtivo(false);
        var atualizado = service.atualizar(salvo.getId(), update);
        assertThat(atualizado.getCodigo()).isEqualTo(salvo.getCodigo());
        assertThat(atualizado.getAtivo()).isFalse();
        assertThat(service.listar()).noneMatch(status -> status.getId().equals(salvo.getId()));
        assertThat(service.buscarPorId(salvo.getId()).getAtivo()).isFalse();
    }

    @Test
    void deveRejeitarCodigoDuplicadoEAlteracaoDireta() {
        StatusOrdemServicoRequest request = new StatusOrdemServicoRequest();
        request.setCodigo("COMPRAR_MATERIAL");
        request.setNome("Nome único " + sufixo());
        assertThatThrownBy(() -> service.salvar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("código");

        Long id = repository.findByCodigo("EM_EXECUCAO").orElseThrow().getId();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE status_ordem_servico SET codigo='OUTRO' WHERE id=?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("codigo do status de ordem de servico e imutavel");
    }

    @Test
    void deveRejeitarNomeDuplicadoNormalizado() {
        StatusOrdemServicoRequest request = new StatusOrdemServicoRequest();
        request.setCodigo("OUTRO_" + sufixo());
        request.setNome(" comprar MATERIAL ");

        assertThatThrownBy(() -> service.salvar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nome");
    }

    private String sufixo() {
        return UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase();
    }
}
