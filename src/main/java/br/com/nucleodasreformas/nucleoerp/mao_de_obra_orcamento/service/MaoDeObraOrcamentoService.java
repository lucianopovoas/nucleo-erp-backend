package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.entity.MaoDeObraOrcamento;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.mapper.MaoDeObraOrcamentoMapper;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository.MaoDeObraOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoGuard;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.repository.UnidadeMaoDeObraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MaoDeObraOrcamentoService {

    private final MaoDeObraOrcamentoRepository repository;
    private final UnidadeMaoDeObraRepository unidadeRepository;
    private final OrcamentoVersaoGuard versaoGuard;

    public MaoDeObraOrcamentoResponse salvar(
            Long orcamentoId, Long versaoId, MaoDeObraOrcamentoRequest request) {
        OrcamentoVersao versao = versaoGuard.bloquearEditavel(orcamentoId, versaoId);
        UnidadeMaoDeObra unidade = buscarUnidadeAtiva(request.getUnidadeMaoDeObraId());
        String descricao = validarDescricao(request.getDescricao());
        BigDecimal total = calcularCustoTotal(request.getQuantidade(), request.getCustoUnitario());
        MaoDeObraOrcamento linha = MaoDeObraOrcamentoMapper.toEntity(
                versao, unidade, descricao, unidade.getNome(),
                request.getQuantidade(), request.getCustoUnitario(), total);
        return MaoDeObraOrcamentoMapper.toResponse(repository.saveAndFlush(linha));
    }

    @Transactional(readOnly = true)
    public MaoDeObraOrcamentoResponse buscarPorId(
            Long orcamentoId, Long versaoId, Long linhaId) {
        versaoGuard.buscar(orcamentoId, versaoId);
        return MaoDeObraOrcamentoMapper.toResponse(buscarLinha(versaoId, linhaId));
    }

    @Transactional(readOnly = true)
    public List<MaoDeObraOrcamentoResponse> listar(Long orcamentoId, Long versaoId) {
        versaoGuard.buscar(orcamentoId, versaoId);
        return repository.findByOrcamentoVersao_IdOrderByIdAsc(versaoId).stream()
                .map(MaoDeObraOrcamentoMapper::toResponse).toList();
    }

    public MaoDeObraOrcamentoResponse atualizar(
            Long orcamentoId, Long versaoId, Long linhaId,
            MaoDeObraOrcamentoUpdateRequest request) {
        versaoGuard.bloquearEditavel(orcamentoId, versaoId);
        MaoDeObraOrcamento linha = buscarLinhaParaAtualizar(versaoId, linhaId);
        UnidadeMaoDeObra atual = linha.getUnidadeMaoDeObra();
        boolean alterada = request.getUnidadeMaoDeObraId() != null
                && !request.getUnidadeMaoDeObraId().equals(atual.getId());
        UnidadeMaoDeObra unidade = alterada
                ? buscarUnidadeAtiva(request.getUnidadeMaoDeObraId()) : atual;
        String descricao = request.isDescricaoInformada()
                ? validarDescricao(request.getDescricao()) : linha.getDescricao();
        String unidadeSnapshot = alterada ? unidade.getNome() : linha.getUnidade();
        BigDecimal quantidade = request.getQuantidade() != null
                ? request.getQuantidade() : linha.getQuantidade();
        BigDecimal unitario = request.getCustoUnitario() != null
                ? request.getCustoUnitario() : linha.getCustoUnitario();
        BigDecimal total = calcularCustoTotal(quantidade, unitario);
        MaoDeObraOrcamentoMapper.updateEntity(
                linha, unidade, descricao, unidadeSnapshot, quantidade, unitario, total);
        return MaoDeObraOrcamentoMapper.toResponse(repository.saveAndFlush(linha));
    }

    public void deletar(Long orcamentoId, Long versaoId, Long linhaId) {
        versaoGuard.bloquearEditavel(orcamentoId, versaoId);
        repository.delete(buscarLinhaParaAtualizar(versaoId, linhaId));
    }

    private String validarDescricao(String descricao) {
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new BusinessException("A descrição informada não pode ser nula ou vazia.");
        }
        return descricao.trim();
    }

    private BigDecimal calcularCustoTotal(BigDecimal quantidade, BigDecimal unitario) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new BusinessException("A quantidade deve ser maior que zero.");
        }
        if (unitario == null || unitario.signum() < 0) {
            throw new BusinessException("O custo unitário não pode ser negativo.");
        }
        return quantidade.multiply(unitario).setScale(2, RoundingMode.HALF_UP);
    }

    private UnidadeMaoDeObra buscarUnidadeAtiva(Long id) {
        UnidadeMaoDeObra unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unidade de mão de obra não encontrada. Id: " + id));
        if (!Boolean.TRUE.equals(unidade.getAtivo())) {
            throw new BusinessException(
                    "Não é possível vincular uma unidade de mão de obra inativa ao orçamento.");
        }
        return unidade;
    }

    private MaoDeObraOrcamento buscarLinha(Long versaoId, Long linhaId) {
        return repository.findByIdAndOrcamentoVersao_Id(linhaId, versaoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mão de obra do orçamento não encontrada. Id: " + linhaId
                                + ", versão: " + versaoId));
    }

    private MaoDeObraOrcamento buscarLinhaParaAtualizar(Long versaoId, Long linhaId) {
        return repository.findByIdAndOrcamentoVersaoIdForUpdate(linhaId, versaoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mão de obra do orçamento não encontrada. Id: " + linhaId
                                + ", versão: " + versaoId));
    }
}
