package br.com.nucleodasreformas.nucleoerp.item_orcamento.entity;

import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "item_orcamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemOrcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorUnitario;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal desconto;

    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
