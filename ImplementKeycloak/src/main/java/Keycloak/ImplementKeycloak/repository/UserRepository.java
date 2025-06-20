package Keycloak.ImplementKeycloak.repository;

import Keycloak.ImplementKeycloak.Model.ThinkUser;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface UserRepository extends JpaRepository<ThinkUser, Integer> {

    Optional<ThinkUser> findByKeycloakId(String keycloakId);

    List<ThinkUser> findByStatusOrderByUserNameAsc(Integer status);

    @Query("""
    SELECT tu FROM ThinkUser tu
    WHERE (:firstName IS NULL OR tu.firstName LIKE %:firstName%)
      AND (:lastName IS NULL OR tu.lastName LIKE %:lastName%)
      AND (:userName IS NULL OR tu.userName LIKE %:userName%)
      AND (:email IS NULL OR tu.email LIKE %:email%)
      AND (:city IS NULL OR tu.city LIKE %:city%)
      AND (:status IS NULL OR tu.status = :status)""")
    List<ThinkUser> searchFilter(@Param("firstName") String firstName, @Param("lastName") String lastName, @Param("userName") String userName,
            @Param("email") String email, @Param("city") String city, @Param("status") Integer status); //""" - multi-line string (java 15+)

}
