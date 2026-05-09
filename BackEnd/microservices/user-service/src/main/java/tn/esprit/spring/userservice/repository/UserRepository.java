package tn.esprit.spring.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tn.esprit.spring.userservice.entity.Role;
import tn.esprit.spring.userservice.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    boolean existsByUsernameAndDeletedFalse(String username);
    boolean existsByEmailAndDeletedFalse(String email);
    boolean existsByCinAndDeletedFalse(String cin);
    boolean existsByPhoneAndDeletedFalse(String phone);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByPhoneAndIdNot(String phone, Long id);

    Optional<User> findByUsernameAndDeletedFalse(String username);
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByCinAndDeletedFalse(String cin);
    Optional<User> findByIdAndDeletedFalse(Long id);
    Optional<User> findByKeycloakIdAndDeletedFalse(String keycloakId);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.deleted = false
          AND (lower(u.username) = lower(:identifier)
           OR lower(u.email) = lower(:identifier)
          )
    """)
    Optional<User> findByIdentifier(String identifier);

    List<User> findByRoleAndDeletedFalse(Role role);

    @Query("""
        SELECT count(u)
        FROM User u
        WHERE u.deleted = false
          AND u.accountStatus = tn.esprit.spring.userservice.entity.AccountStatus.PENDING_CONTRACT
          AND u.createdAt < :beforeDate
    """)
    long countPendingContractOlderThan(LocalDateTime beforeDate);
}
