package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Bike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BikeRepository extends JpaRepository<Bike, Long> {

    boolean existsByCodigo(String codigo);

    List<Bike> findByStatus(Bike.Status status);
}
