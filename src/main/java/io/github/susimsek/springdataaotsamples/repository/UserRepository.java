package io.github.susimsek.springdataaotsamples.repository;

import io.github.susimsek.springdataaotsamples.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(value = "User.withAuthorities")
    Optional<User> findOneWithAuthoritiesByUsername(String username);

    @EntityGraph(value = "User.withAuthorities")
    Optional<User> findOneWithAuthoritiesById(Long id);
}
