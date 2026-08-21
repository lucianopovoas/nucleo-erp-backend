package br.com.nucleodasreformas.nucleoerp.orcamento_versao.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrcamentoVersaoPolicyTest {

    private final OrcamentoVersaoPolicy policy = new OrcamentoVersaoPolicy();

    @Test
    void deveAceitarSomenteTransicoesDaMatrizCanonica() {
        assertThatCode(() -> policy.validarTransicao("RASCUNHO", "ENVIADO"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validarTransicao("RASCUNHO", "CANCELADO"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validarTransicao("ENVIADO", "APROVADO"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validarTransicao("ENVIADO", "RECUSADO"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validarTransicao("ENVIADO", "CANCELADO"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> policy.validarTransicao("ENVIADO", "RASCUNHO"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.validarTransicao("APROVADO", "ENVIADO"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.validarTransicao("CUSTOMIZADO", "ENVIADO"))
                .isInstanceOf(BusinessException.class);
    }
}
