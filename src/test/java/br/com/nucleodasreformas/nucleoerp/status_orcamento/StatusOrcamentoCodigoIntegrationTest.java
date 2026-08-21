package br.com.nucleodasreformas.nucleoerp.status_orcamento;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.service.StatusOrcamentoService;
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
class StatusOrcamentoCodigoIntegrationTest {

    @Autowired private StatusOrcamentoService service;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void deveNormalizarCodigoEManterIdentidadeAoRenomear() {
        String sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        StatusOrcamentoRequest request = new StatusOrcamentoRequest();
        request.setCodigo(" em_analise_" + sufixo.toLowerCase());
        request.setNome("Em análise " + sufixo);

        StatusOrcamentoResponse salvo = service.salvar(request);
        assertThat(salvo.getCodigo()).isEqualTo("EM_ANALISE_" + sufixo);

        StatusOrcamentoUpdateRequest update = new StatusOrcamentoUpdateRequest();
        update.setNome("Em avaliação " + sufixo);
        StatusOrcamentoResponse atualizado = service.atualizar(salvo.getId(), update);
        assertThat(atualizado.getCodigo()).isEqualTo(salvo.getCodigo());
        assertThat(atualizado.getNome()).isEqualTo("Em avaliação " + sufixo);
    }

    @Test
    void deveRejeitarCodigoDuplicadoEAlteracaoDireta() {
        StatusOrcamentoRequest request = new StatusOrcamentoRequest();
        request.setCodigo("RASCUNHO");
        request.setNome("Outro rascunho");
        assertThatThrownBy(() -> service.salvar(request))
                .isInstanceOf(BusinessException.class);

        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM status_orcamento WHERE codigo = 'ENVIADO'", Long.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE status_orcamento SET codigo = 'ENVIADO_2' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("codigo do status de orcamento e imutavel");
    }
}
