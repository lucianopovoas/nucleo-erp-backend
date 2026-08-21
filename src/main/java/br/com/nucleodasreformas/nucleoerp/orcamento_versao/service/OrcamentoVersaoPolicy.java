package br.com.nucleodasreformas.nucleoerp.orcamento_versao.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class OrcamentoVersaoPolicy {

    public static final String RASCUNHO = "RASCUNHO";
    public static final String ENVIADO = "ENVIADO";
    public static final String APROVADO = "APROVADO";
    public static final String RECUSADO = "RECUSADO";
    public static final String CANCELADO = "CANCELADO";

    private static final Map<String, Set<String>> TRANSICOES = Map.of(
            RASCUNHO, Set.of(ENVIADO, CANCELADO),
            ENVIADO, Set.of(APROVADO, RECUSADO, CANCELADO),
            APROVADO, Set.of(),
            RECUSADO, Set.of(),
            CANCELADO, Set.of());

    public void garantirAtual(Orcamento orcamento, OrcamentoVersao versao) {
        if (orcamento.getVersaoAtual() == null
                || !orcamento.getVersaoAtual().getId().equals(versao.getId())) {
            throw new BusinessException("A versão informada é histórica e não pode ser alterada.");
        }
    }

    public void garantirEditavel(Orcamento orcamento, OrcamentoVersao versao) {
        garantirAtual(orcamento, versao);
        if (!RASCUNHO.equals(versao.getStatusOrcamento().getCodigo())) {
            throw new BusinessException("Somente uma versão em RASCUNHO pode ter seu conteúdo alterado.");
        }
    }

    public void validarTransicao(String origem, String destino) {
        Set<String> destinos = TRANSICOES.get(origem);
        if (destinos == null || !TRANSICOES.containsKey(destino) || !destinos.contains(destino)) {
            throw new BusinessException(
                    "Transição de status não permitida: " + origem + " -> " + destino + ".");
        }
    }

    public void garantirPodeOriginarNovaVersao(OrcamentoVersao origem) {
        String codigo = origem.getStatusOrcamento().getCodigo();
        if (!ENVIADO.equals(codigo) && !RECUSADO.equals(codigo)) {
            throw new BusinessException(
                    "Somente uma versão ENVIADA ou RECUSADA pode originar nova versão.");
        }
    }
}
