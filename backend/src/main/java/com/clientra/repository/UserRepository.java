package com.clientra.repository;

import com.clientra.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRoleIdIn(Collection<Integer> roleIds);
    long countByRoleIdAndActiveTrue(Integer roleId);
}
