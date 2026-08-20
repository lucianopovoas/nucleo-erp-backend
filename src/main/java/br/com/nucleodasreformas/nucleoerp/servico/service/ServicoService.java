package br.com.nucleodasreformas.nucleoerp.servico.service;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.repository.CategoriaServicoRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.servico.dto.ServicoRequest;
import br.com.nucleodasreformas.nucleoerp.servico.dto.ServicoResponse;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import br.com.nucleodasreformas.nucleoerp.servico.mapper.ServicoMapper;
import br.com.nucleodasreformas.nucleoerp.servico.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ServicoService {

    private static final String MENSAGEM_DUPLICIDADE =
            "Já existe um serviço com esse nome nesta categoria.";
    private static final String MENSAGEM_CATEGORIA_INATIVA =
            "Não é possível vincular um serviço a uma categoria inativa.";
    private static final String INDICE_UNICIDADE = "uk_servico_categoria_nome_normalizado";

    private final ServicoRepository repository;
    private final CategoriaServicoRepository categoriaServicoRepository;

    public ServicoResponse salvar(ServicoRequest request) {
        normalizarNome(request);
        CategoriaServico categoriaServico = buscarCategoriaAtiva(request.getCategoriaServicoId());
        validarDuplicidade(categoriaServico.getId(), request.getNome());

        Servico servico = ServicoMapper.toEntity(request, categoriaServico);
        servico.setAtivo(true);

        return ServicoMapper.toResponse(salvarComTratamentoDeConflito(servico));
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscarPorId(Long id) {
        return ServicoMapper.toResponse(buscarServico(id));
    }

    @Transactional(readOnly = true)
    public List<ServicoResponse> listar() {
        return repository.findByAtivoTrue()
                .stream()
                .map(ServicoMapper::toResponse)
                .toList();
    }

    public ServicoResponse atualizar(Long id, ServicoRequest request) {
        Servico servico = buscarServico(id);
        normalizarNome(request);

        Long categoriaAtualId = servico.getCategoriaServico().getId();
        boolean categoriaAlterada = !Objects.equals(categoriaAtualId, request.getCategoriaServicoId());
        CategoriaServico categoriaDestino = categoriaAlterada
                ? buscarCategoriaAtiva(request.getCategoriaServicoId())
                : servico.getCategoriaServico();

        boolean ativoFinal = request.getAtivo() != null ? request.getAtivo() : Boolean.TRUE.equals(servico.getAtivo());
        if (ativoFinal && !Boolean.TRUE.equals(categoriaDestino.getAtivo())) {
            throw new BusinessException(MENSAGEM_CATEGORIA_INATIVA);
        }

        validarDuplicidadeNaAtualizacao(categoriaDestino.getId(), request.getNome(), id);
        ServicoMapper.updateEntity(servico, request, categoriaDestino);

        return ServicoMapper.toResponse(salvarComTratamentoDeConflito(servico));
    }

    public void deletar(Long id) {
        Servico servico = buscarServico(id);
        servico.setAtivo(false);
        repository.save(servico);
    }

    private void normalizarNome(ServicoRequest request) {
        if (request.getNome() != null) {
            request.setNome(request.getNome().trim());
        }
    }

    private CategoriaServico buscarCategoriaAtiva(Long id) {
        CategoriaServico categoriaServico = categoriaServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoria de serviço não encontrada. Id: " + id));

        if (!Boolean.TRUE.equals(categoriaServico.getAtivo())) {
            throw new BusinessException(MENSAGEM_CATEGORIA_INATIVA);
        }
        return categoriaServico;
    }

    private void validarDuplicidade(Long categoriaServicoId, String nome) {
        if (repository.existsByCategoriaENomeNormalizado(categoriaServicoId, nome)) {
            throw new BusinessException(MENSAGEM_DUPLICIDADE);
        }
    }

    private void validarDuplicidadeNaAtualizacao(Long categoriaServicoId, String nome, Long id) {
        if (repository.existsByCategoriaENomeNormalizadoAndIdNot(categoriaServicoId, nome, id)) {
            throw new BusinessException(MENSAGEM_DUPLICIDADE);
        }
    }

    private Servico salvarComTratamentoDeConflito(Servico servico) {
        try {
            return repository.saveAndFlush(servico);
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

    private Servico buscarServico(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado. Id: " + id));
    }
}
