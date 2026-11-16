package br.edu.ifc.bikes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@ToString

@Entity
@Table(name = "bikes")
@EntityListeners(AuditingEntityListener.class)
public class Bike implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "modelo", nullable = false, length = 100)
    private String modelo;

    @Column(name = "marca", nullable = false, length = 50)
    private String marca;

    @Column(name = "cor", nullable = false, length = 30)
    private String cor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.DISPONIVEL;

    @CreatedDate
    private LocalDateTime dataCriacao;
    @LastModifiedDate
    private LocalDateTime dataModificacao;
    @CreatedBy
    private String criadoPor;
    @LastModifiedBy
    private String modificadoPor;

    public enum Status {
        DISPONIVEL, ALUGADA, MANUTENCAO
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Bike bike)) return false;
        return Objects.equals(id, bike.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
