package br.com.nucleodasreformas.nucleoerp.orcamento_versao.service;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.entity.DespesaOrcamento;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository.DespesaOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.entity.ItemOrcamento;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.ItemOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.entity.MaoDeObraOrcamento;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository.MaoDeObraOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.entity.MaterialOrcamento;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.repository.MaterialOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoStatusRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.mapper.OrcamentoVersaoMapper;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.repository.OrcamentoVersaoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrcamentoVersaoService {

    private static final String CONSTRAINT_APROVACAO =
            "uk_orcamento_versao_aprovada_por_orcamento";
    private static final String CONSTRAINT_NUMERO = "uk_orcamento_versao_numero";

    private final OrcamentoVersaoRepository repository;
    private final OrcamentoRepository orcamentoRepository;
    private final StatusOrcamentoRepository statusRepository;
    private final ItemOrcamentoRepository itemRepository;
    private final MaterialOrcamentoRepository materialRepository;
    private final MaoDeObraOrcamentoRepository maoDeObraRepository;
    private final DespesaOrcamentoRepository despesaRepository;
    private final OrcamentoVersaoGuard guard;
    private final OrcamentoVersaoPolicy policy;
    private final OrcamentoVersaoTotaisService totaisService;

    @Transactional(readOnly = true)
    public OrcamentoVersaoResponse buscarPorId(Long orcamentoId, Long versaoId) {
        OrcamentoVersao versao = guard.buscar(orcamentoId, versaoId);
        return montarResponse(versao);
    }

    @Transactional(readOnly = true)
    public List<OrcamentoVersaoResponse> listar(Long orcamentoId) {
        if (!orcamentoRepository.existsById(orcamentoId)) {
            throw new ResourceNotFoundException("Orçamento não encontrado. Id: " + orcamentoId);
        }
        List<OrcamentoVersao> versoes = repository
                .findByOrcamento_IdOrderByNumeroVersaoAsc(orcamentoId);
        List<Long> ids = versoes.stream().map(OrcamentoVersao::getId).toList();
        Map<Long, TotaisOrcamentoVersao> totais = totaisService.buscarPorVersoes(ids);
        return versoes.stream()
                .map(versao -> OrcamentoVersaoMapper.toResponse(versao, totais.get(versao.getId())))
                .toList();
    }

    public OrcamentoVersaoResponse atualizar(
            Long orcamentoId,
            Long versaoId,
            OrcamentoVersaoUpdateRequest request) {
        OrcamentoVersao versao = guard.bloquearEditavel(orcamentoId, versaoId);
        if (request.isObservacaoInformada()) {
            versao.setObservacao(request.getObservacao());
        }
        return montarResponse(repository.saveAndFlush(versao));
    }

    public OrcamentoVersaoResponse alterarStatus(
            Long orcamentoId,
            Long versaoId,
            OrcamentoVersaoStatusRequest request) {
        ContextoOrcamentoVersao contexto = guard.bloquear(orcamentoId, versaoId);
        policy.garantirAtual(contexto.orcamento(), contexto.versao());

        StatusOrcamento atual = contexto.versao().getStatusOrcamento();
        if (atual.getId().equals(request.getStatusOrcamentoId())) {
            return montarResponse(contexto.versao());
        }

        StatusOrcamento destino = buscarStatusAtivo(request.getStatusOrcamentoId());
        policy.validarTransicao(atual.getCodigo(), destino.getCodigo());
        if (OrcamentoVersaoPolicy.APROVADO.equals(destino.getCodigo())
                && repository.existsByOrcamento_IdAndStatusOrcamento_Codigo(
                        orcamentoId, OrcamentoVersaoPolicy.APROVADO)) {
            throw new BusinessException("O orçamento já possui uma versão aprovada.");
        }

        contexto.versao().setStatusOrcamento(destino);
        return montarResponse(salvarComTratamentoDeConflito(contexto.versao()));
    }

    public OrcamentoVersaoResponse criarNovaVersao(Long orcamentoId, Long versaoOrigemId) {
        ContextoOrcamentoVersao contexto = guard.bloquear(orcamentoId, versaoOrigemId);
        policy.garantirAtual(contexto.orcamento(), contexto.versao());
        policy.garantirPodeOriginarNovaVersao(contexto.versao());

        if (repository.existsByOrcamento_IdAndStatusOrcamento_Codigo(
                orcamentoId, OrcamentoVersaoPolicy.APROVADO)) {
            throw new BusinessException(
                    "Não é possível criar nova versão após a aprovação do orçamento.");
        }

        StatusOrcamento rascunho = buscarStatusAtivoPorCodigo(OrcamentoVersaoPolicy.RASCUNHO);
        OrcamentoVersao nova = OrcamentoVersaoMapper.toEntity(
                contexto.orcamento(),
                contexto.versao().getNumeroVersao() + 1,
                rascunho,
                contexto.versao().getObservacao());
        nova = salvarComTratamentoDeConflito(nova);

        clonarLinhas(contexto.versao(), nova);
        contexto.orcamento().setVersaoAtual(nova);
        orcamentoRepository.saveAndFlush(contexto.orcamento());
        return montarResponse(nova);
    }

    private void clonarLinhas(OrcamentoVersao origem, OrcamentoVersao destino) {
        List<ItemOrcamento> itens = itemRepository.findByOrcamentoVersao_IdOrderByIdAsc(origem.getId())
                .stream().map(item -> ItemOrcamento.builder()
                        .orcamentoVersao(destino)
                        .servico(item.getServico())
                        .descricao(item.getDescricao())
                        .quantidade(item.getQuantidade())
                        .valorUnitario(item.getValorUnitario())
                        .desconto(item.getDesconto())
                        .valorTotal(item.getValorTotal())
                        .build()).toList();
        List<MaterialOrcamento> materiais = materialRepository
                .findByOrcamentoVersao_IdOrderByIdAsc(origem.getId()).stream()
                .map(material -> MaterialOrcamento.builder()
                        .orcamentoVersao(destino)
                        .material(material.getMaterial())
                        .descricao(material.getDescricao())
                        .unidade(material.getUnidade())
                        .quantidade(material.getQuantidade())
                        .custoUnitario(material.getCustoUnitario())
                        .custoTotal(material.getCustoTotal())
                        .build()).toList();
        List<MaoDeObraOrcamento> maoDeObra = maoDeObraRepository
                .findByOrcamentoVersao_IdOrderByIdAsc(origem.getId()).stream()
                .map(linha -> MaoDeObraOrcamento.builder()
                        .orcamentoVersao(destino)
                        .unidadeMaoDeObra(linha.getUnidadeMaoDeObra())
                        .descricao(linha.getDescricao())
                        .unidade(linha.getUnidade())
                        .quantidade(linha.getQuantidade())
                        .custoUnitario(linha.getCustoUnitario())
                        .custoTotal(linha.getCustoTotal())
                        .build()).toList();
        List<DespesaOrcamento> despesas = despesaRepository
                .findByOrcamentoVersao_IdOrderByIdAsc(origem.getId()).stream()
                .map(despesa -> DespesaOrcamento.builder()
                        .orcamentoVersao(destino)
                        .descricao(despesa.getDescricao())
                        .valor(despesa.getValor())
                        .build()).toList();

        itemRepository.saveAll(itens);
        materialRepository.saveAll(materiais);
        maoDeObraRepository.saveAll(maoDeObra);
        despesaRepository.saveAll(despesas);
        despesaRepository.flush();
    }

    private OrcamentoVersaoResponse montarResponse(OrcamentoVersao versao) {
        return OrcamentoVersaoMapper.toResponse(versao, totaisService.buscarPorVersao(versao.getId()));
    }

    private StatusOrcamento buscarStatusAtivo(Long id) {
        StatusOrcamento status = statusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status de orçamento não encontrado. Id: " + id));
        if (!Boolean.TRUE.equals(status.getAtivo())) {
            throw new BusinessException("Não é possível selecionar um status de orçamento inativo.");
        }
        return status;
    }

    private StatusOrcamento buscarStatusAtivoPorCodigo(String codigo) {
        StatusOrcamento status = statusRepository.findByCodigo(codigo)
                .orElseThrow(() -> new BusinessException(
                        "O status '" + codigo + "' não está cadastrado."));
        if (!Boolean.TRUE.equals(status.getAtivo())) {
            throw new BusinessException("O status '" + codigo + "' está inativo.");
        }
        return status;
    }

    private OrcamentoVersao salvarComTratamentoDeConflito(OrcamentoVersao versao) {
        try {
            return repository.saveAndFlush(versao);
        } catch (DataIntegrityViolationException ex) {
            String constraint = buscarConstraint(ex);
            if (CONSTRAINT_APROVACAO.equals(constraint)) {
                throw new BusinessException("O orçamento já possui uma versão aprovada.");
            }
            if (CONSTRAINT_NUMERO.equals(constraint)) {
                throw new BusinessException("Já existe uma versão com esse número no orçamento.");
            }
            throw ex;
        }
    }

    private String buscarConstraint(Throwable throwable) {
        Throwable causa = throwable;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violacao) {
                return violacao.getConstraintName();
            }
            causa = causa.getCause();
        }
        return null;
    }
}
