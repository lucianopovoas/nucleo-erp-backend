package br.com.nucleodasreformas.nucleoerp.fornecedor.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorRequest;
import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.fornecedor.entity.Fornecedor;
import br.com.nucleodasreformas.nucleoerp.fornecedor.mapper.FornecedorMapper;
import br.com.nucleodasreformas.nucleoerp.fornecedor.repository.FornecedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FornecedorService {

    private final FornecedorRepository repository;

    public FornecedorResponse salvar(FornecedorRequest request){

        Fornecedor fornecedor = FornecedorMapper.toEntity(request);

        validarNome(request);

        fornecedor = repository.save(fornecedor);

        return FornecedorMapper.toResponse(fornecedor);
    }

    @Transactional(readOnly = true)
    public FornecedorResponse buscarPorId(Long id){

        Fornecedor fornecedor = buscarFornecedorId(id);

        return FornecedorMapper.toResponse(fornecedor);
    }

    @Transactional(readOnly = true)
    public List<FornecedorResponse> listar(){

        return repository.findAll()
                .stream()
                .map(FornecedorMapper::toResponse)
                .toList();
    }

    public FornecedorResponse atualizar(Long id, FornecedorRequest request) {

        Fornecedor fornecedor = buscarFornecedorId(id);

        validarNomeAtualizar(request, id);

        FornecedorMapper.updateEntity(fornecedor, request);

        fornecedor = repository.save(fornecedor);

        return FornecedorMapper.toResponse(fornecedor);
    }

    public void deletar(Long id){

        Fornecedor fornecedor = buscarFornecedorId(id);
        fornecedor.setAtivo(false);
        repository.save(fornecedor);
    }

    private void validarNome(FornecedorRequest request){

        if (request.getNome() != null && repository.existsByNome(request.getNome())){
            throw new BusinessException("Já existe um Fornecedor com esse nome");
        }
    }

    private Fornecedor buscarFornecedorId(Long id){

        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Fornecedor não encontrado Id:" + id));
    }

    private void validarNomeAtualizar(FornecedorRequest request, Long id) {

        if (repository.existsByNomeAndIdNot(request.getNome(), id)) {
            throw new BusinessException("Já existe um fornecedor com esse nome.");
        }
    }

}
