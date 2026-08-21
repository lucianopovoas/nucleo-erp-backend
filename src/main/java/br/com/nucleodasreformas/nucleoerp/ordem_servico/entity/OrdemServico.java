package br.com.nucleodasreformas.nucleoerp.ordem_servico.entity;

import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "ordem_servico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated(event = EventType.INSERT)
    @Column(nullable = false, insertable = false, updatable = false)
    private Long numero;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_versao_id", nullable = false)
    private OrcamentoVersao orcamentoVersao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_ordem_servico_id", nullable = false)
    private StatusOrdemServico statusOrdemServico;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
