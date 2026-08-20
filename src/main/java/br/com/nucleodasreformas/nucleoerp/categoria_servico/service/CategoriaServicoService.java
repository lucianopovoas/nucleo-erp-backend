package br.com.nucleodasreformas.nucleoerp.categoria_servico.service;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.dto.CategoriaServicoRequest;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.dto.CategoriaServicoResponse;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.mapper.CategoriaServicoMapper;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.repository.CategoriaServicoRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.servico.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoriaServicoService {

    private static final String MENSAGEM_DUPLICIDADE = "Já existe uma categoria de serviço com esse nome.";
    private static final String INDICE_UNICIDADE = "uk_categoria_servico_nome_normalizado";

    private final CategoriaServicoRepository repository;
    private final ServicoRepository servicoRepository;

    public CategoriaServicoResponse salvar(CategoriaServicoRequest request) {
        normalizarNome(request);
        validarNomeDuplicado(request.getNome());

        CategoriaServico categoriaServico = CategoriaServicoMapper.toEntity(request);
        categoriaServico.setAtivo(true);

        return CategoriaServicoMapper.toResponse(salvarComTratamentoDeConflito(categoriaServico));
    }

    @Transactional(readOnly = true)
    public CategoriaServicoResponse buscarPorId(Long id) {
        return CategoriaServicoMapper.toResponse(buscarCategoriaServico(id));
    }

    @Transactional(readOnly = true)
    public List<CategoriaServicoResponse> listar() {
        return repository.findByAtivoTrue()
                .stream()
                .map(CategoriaServicoMapper::toResponse)
                .toList();
    }

    public CategoriaServicoResponse atualizar(Long id, CategoriaServicoRequest request) {
        CategoriaServico categoriaServico = buscarCategoriaServico(id);

        normalizarNome(request);
        validarNomeDuplicadoNaAtualizacao(request.getNome(), id);
        CategoriaServicoMapper.updateEntity(categoriaServico, request);

        CategoriaServico categoriaSalva = salvarComTratamentoDeConflito(categoriaServico);
        if (!Boolean.TRUE.equals(categoriaSalva.getAtivo())) {
            servicoRepository.inativarAtivosPorCategoriaId(categoriaSalva.getId());
        }

        return CategoriaServicoMapper.toResponse(categoriaSalva);
    }

    public void deletar(Long id) {
        CategoriaServico categoriaServico = buscarCategoriaServico(id);
        categoriaServico.setAtivo(false);
        repository.save(categoriaServico);
        servicoRepository.inativarAtivosPorCategoriaId(id);
    }

    private void normalizarNome(CategoriaServicoRequest request) {
        if (request.getNome() != null) {
            request.setNome(request.getNome().trim());
        }
    }

    private void validarNomeDuplicado(String nome) {
        if (repository.existsByNomeNormalizado(nome)) {
            throw new BusinessException(MENSAGEM_DUPLICIDADE);
        }
    }

    private void validarNomeDuplicadoNaAtualizacao(String nome, Long id) {
        if (repository.existsByNomeNormalizadoAndIdNot(nome, id)) {
            throw new BusinessException(MENSAGEM_DUPLICIDADE);
        }
    }

    private CategoriaServico salvarComTratamentoDeConflito(CategoriaServico categoriaServico) {
        try {
            return repository.saveAndFlush(categoriaServico);
        } catch (DataIntegrityViolationException ex) {
            if (causadoPeloIndiceUnico(ex)) {
                throw new BusinessException(MENSAGEM_DUPLICIDADE);
            }
            throw ex;
        }
    }

    private boolean causadoPeloIndiceUnico(Throwable throwable) {
        Throwable causa = throwable;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violacao
                    && INDICE_UNICIDADE.equals(violacao.getConstraintName())) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }

    private CategoriaServico buscarCategoriaServico(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoria de serviço não encontrada. Id: " + id));
    }
}
