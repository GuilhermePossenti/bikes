package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Bike;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BikeRepository extends JpaRepository<Bike, Long> {

    boolean existsByCodigo(String codigo);

    Optional<Bike> findByCodigo(String codigo);

    List<Bike> findByStatus(Bike.Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bike b where b.id = :id")
    Optional<Bike> findByIdForUpdate(Long id);
}
