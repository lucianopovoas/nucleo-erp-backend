package br.com.nucleodasreformas.nucleoerp;

import br.com.nucleodasreformas.nucleoerp.cliente.dto.ClienteRequest;
import br.com.nucleodasreformas.nucleoerp.cliente.dto.ClienteResponse;
import br.com.nucleodasreformas.nucleoerp.cliente.service.ClienteService;
import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorRequest;
import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.fornecedor.service.FornecedorService;
import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialRequest;
import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialResponse;
import br.com.nucleodasreformas.nucleoerp.material.service.MaterialService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CadastroDefaultsIntegrationTests {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private FornecedorService fornecedorService;

    @Autowired
    private MaterialService materialService;

    @Test
    void deveCriarCadastrosAtivosComDataDeCriacaoSemAtivoNoRequest() {
        String sufixo = UUID.randomUUID().toString();

        ClienteRequest clienteRequest = new ClienteRequest();
        clienteRequest.setNome("Cliente " + sufixo);

        FornecedorRequest fornecedorRequest = new FornecedorRequest();
        fornecedorRequest.setNome("Fornecedor " + sufixo);

        MaterialRequest materialRequest = new MaterialRequest();
        materialRequest.setNome("Material " + sufixo);
        materialRequest.setUnidade("UN");

        ClienteResponse cliente = clienteService.salvar(clienteRequest);
        FornecedorResponse fornecedor = fornecedorService.salvar(fornecedorRequest);
        MaterialResponse material = materialService.salvar(materialRequest);

        assertThat(cliente.getId()).isNotNull();
        assertThat(cliente.getCriadoEm()).isNotNull();
        assertThat(cliente.getAtivo()).isTrue();

        assertThat(fornecedor.getId()).isNotNull();
        assertThat(fornecedor.getCriadoEm()).isNotNull();
        assertThat(fornecedor.getAtivo()).isTrue();

        assertThat(material.getId()).isNotNull();
        assertThat(material.getCriadoEm()).isNotNull();
        assertThat(material.getAtivo()).isTrue();
    }
}
