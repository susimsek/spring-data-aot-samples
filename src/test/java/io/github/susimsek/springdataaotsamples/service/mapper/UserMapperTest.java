package io.github.susimsek.springdataaotsamples.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.domain.Authority;
import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.service.dto.UserDTO;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toDtoShouldMapUserAndAuthorities() {
        User user = new User();
        user.setId(42L);
        user.setUsername("alice");
        user.setAuthorities(
                Set.of(new Authority(null, "ROLE_USER"), new Authority(null, "ROLE_ADMIN")));

        UserDTO dto = mapper.toDto(user);

        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.username()).isEqualTo("alice");
        assertThat(dto.authorities()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void authorityNamesShouldHandleNullOrEmpty() {
        assertThat(mapper.authorityNames(null)).isEmpty();
        assertThat(mapper.authorityNames(Set.of())).isEmpty();
    }
}
