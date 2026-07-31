package br.com.nucleodasreformas.nucleoerp.cliente.service;

import br.com.nucleodasreformas.nucleoerp.cliente.dto.ClienteRequest;
import br.com.nucleodasreformas.nucleoerp.cliente.dto.ClienteResponse;
import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.mapper.ClienteMapper;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteResponse salvar(ClienteRequest request) {

        validarCpfCnpj(request);
        validarTelefoneCelularEmail(request);

        Cliente cliente = ClienteMapper.toEntity(request);

        cliente = repository.save(cliente);

        return ClienteMapper.toResponse(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {

        Cliente cliente = buscarClienteId(id);

        return ClienteMapper.toResponse(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {

        return repository.findAll()
                .stream()
                .map(ClienteMapper::toResponse)
                .toList();
    }

    public ClienteResponse atualizar(Long id, ClienteRequest request) {

        Cliente cliente = buscarClienteId(id);

        ClienteMapper.updateEntity(cliente, request);

        validarCpfCnpj(request);
        validarTelefoneCelularEmail(request);

        cliente = repository.save(cliente);

        return ClienteMapper.toResponse(cliente);
    }

    public void deletar(Long id) {

        Cliente cliente = buscarClienteId(id);

        cliente.setAtivo(false);
        repository.save(cliente);
    }


    private void validarCpfCnpj(ClienteRequest request) {

        if (request.getCpf() != null &&
                repository.existsByCpf(request.getCpf())) {

            throw new BusinessException("Já existe um cliente com este CPF.");
        }

        if (request.getCnpj() != null &&
                repository.existsByCnpj(request.getCnpj())) {

            throw new BusinessException("Já existe um cliente com este CNPJ.");
        }

    }

    private void validarTelefoneCelularEmail(ClienteRequest request) {
        if (request.getTelefone() != null && repository.existsByTelefone(request.getTelefone())) {
            throw new BusinessException("Já existe um cliente com esse Telefone");
        }
        if (request.getCelular() != null && repository.existsByCelular(request.getCelular())) {
            throw new BusinessException("Já existe um cliente com esse celular");
        }
        if (request.getEmail() != null && repository.existsByEmail(request.getEmail())){
            throw new BusinessException("Já existe um cliente com esse Email");
        }
    }

    private Cliente buscarClienteId(Long id) {

        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cliente não encontrado. Id: " + id));
    }

}