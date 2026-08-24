package br.com.nucleodasreformas.nucleoerp.orcamento_versao.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

@Component
public class OrcamentoVersaoPolicy {

    public static final String RASCUNHO = "RASCUNHO";
    public static final String ENVIADO = "ENVIADO";
    public static final String APROVADO = "APROVADO";
    public static final String RECUSADO = "RECUSADO";
    public static final String CANCELADO = "CANCELADO";

    private static final Map<String, List<String>> TRANSICOES = Map.of(
            RASCUNHO, List.of(ENVIADO, CANCELADO),
            ENVIADO, List.of(APROVADO, RECUSADO, CANCELADO),
            APROVADO, List.of(),
            RECUSADO, List.of(),
            CANCELADO, List.of());

    public void garantirAtual(Orcamento orcamento, OrcamentoVersao versao) {
        if (!ehAtual(orcamento, versao)) {
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
        if (!destinosPermitidos(origem).contains(destino)) {
            throw new BusinessException(
                    "Transição de status não permitida: " + origem + " -> " + destino + ".");
        }
    }

    public void garantirPodeOriginarNovaVersao(OrcamentoVersao origem) {
        if (!podeOriginarNovaVersao(origem)) {
            throw new BusinessException(
                    "Somente uma versão ENVIADA ou RECUSADA pode originar nova versão.");
        }
    }

    public boolean ehAtual(Orcamento orcamento, OrcamentoVersao versao) {
        return orcamento.getVersaoAtual() != null
                && orcamento.getVersaoAtual().getId().equals(versao.getId());
    }

    public boolean podeEditarConteudo(Orcamento orcamento, OrcamentoVersao versao) {
        return ehAtual(orcamento, versao)
                && RASCUNHO.equals(versao.getStatusOrcamento().getCodigo());
    }

    public boolean podeOriginarNovaVersao(OrcamentoVersao origem) {
        String codigo = origem.getStatusOrcamento().getCodigo();
        return ENVIADO.equals(codigo) || RECUSADO.equals(codigo);
    }

    public List<String> destinosPermitidos(String origem) {
        return TRANSICOES.getOrDefault(origem, List.of());
    }
}
