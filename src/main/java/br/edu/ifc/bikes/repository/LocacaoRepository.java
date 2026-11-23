package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Locacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LocacaoRepository extends JpaRepository<Locacao, Long> {

    @Query("select l from Locacao l join fetch l.cliente join fetch l.bike order by l.dataRetirada desc, l.id desc")
    List<Locacao> findAllComClienteEBike();
}
