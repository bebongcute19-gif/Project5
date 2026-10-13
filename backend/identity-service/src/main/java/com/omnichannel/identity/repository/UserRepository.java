package com.omnichannel.identity.repository;

import com.omnichannel.identity.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:identifier) OR LOWER(u.username) = LOWER(:identifier)")
    Optional<User> findByEmailOrUsername(@Param("identifier") String identifier);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE " +
            "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
            "(:status IS NULL OR u.status = :status)")
    Page<User> findAllWithFilters(
            @Param("email") String email,
            @Param("username") String username,
            @Param("status") String status,
            Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE (r.name = 'ADMIN' OR r.name = 'ROLE_ADMIN') AND u.enabled = true AND u.status = 'ACTIVE'")
    long countActiveAdmins();
}
