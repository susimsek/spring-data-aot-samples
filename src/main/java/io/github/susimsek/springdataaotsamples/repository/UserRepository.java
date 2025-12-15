package io.github.susimsek.springdataaotsamples.repository;

import io.github.susimsek.springdataaotsamples.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.cache.annotation.Cacheable;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    String USER_BY_USERNAME_CACHE = "userByUsername";

    @EntityGraph(value = "User.withAuthorities")
    @Cacheable(cacheNames = USER_BY_USERNAME_CACHE, key = "#username", unless = "#result == null")
    Optional<User> findOneWithAuthoritiesByUsername(String username);

    @EntityGraph(value = "User.withAuthorities")
    Optional<User> findOneWithAuthoritiesById(Long id);

    boolean existsByUsername(String username);
}
