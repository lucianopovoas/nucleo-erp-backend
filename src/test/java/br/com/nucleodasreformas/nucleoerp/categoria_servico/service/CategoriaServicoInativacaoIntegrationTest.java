package br.com.nucleodasreformas.nucleoerp.categoria_servico.service;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.dto.CategoriaServicoRequest;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.repository.CategoriaServicoRepository;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import br.com.nucleodasreformas.nucleoerp.servico.repository.ServicoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CategoriaServicoInativacaoIntegrationTest {

    @Autowired
    private CategoriaServicoService service;

    @Autowired
    private CategoriaServicoRepository categoriaServicoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Test
    void deveInativarServicosNoPutSemReativaLosAoReativarCategoria() {
        CategoriaServico categoria = salvarCategoria("Categoria PUT");
        Servico servico = salvarServico("Serviço PUT", categoria);

        service.atualizar(categoria.getId(), request(categoria.getNome(), false));

        assertThat(servicoRepository.findById(servico.getId()).orElseThrow().getAtivo()).isFalse();

        service.atualizar(categoria.getId(), request(categoria.getNome(), true));

        assertThat(servicoRepository.findById(servico.getId()).orElseThrow().getAtivo()).isFalse();
    }

    @Test
    void deveInativarServicosNoDeleteLogicoDaCategoria() {
        CategoriaServico categoria = salvarCategoria("Categoria DELETE");
        Servico servico = salvarServico("Serviço DELETE", categoria);

        service.deletar(categoria.getId());

        assertThat(categoriaServicoRepository.findById(categoria.getId()).orElseThrow().getAtivo()).isFalse();
        assertThat(servicoRepository.findById(servico.getId()).orElseThrow().getAtivo()).isFalse();
    }

    private CategoriaServico salvarCategoria(String prefixo) {
        return categoriaServicoRepository.saveAndFlush(CategoriaServico.builder()
                .nome(prefixo + " " + UUID.randomUUID())
                .build());
    }

    private Servico salvarServico(String prefixo, CategoriaServico categoria) {
        return servicoRepository.saveAndFlush(Servico.builder()
                .nome(prefixo + " " + UUID.randomUUID())
                .categoriaServico(categoria)
                .build());
    }

    private CategoriaServicoRequest request(String nome, boolean ativo) {
        CategoriaServicoRequest request = new CategoriaServicoRequest();
        request.setNome(nome);
        request.setAtivo(ativo);
        return request;
    }
}
