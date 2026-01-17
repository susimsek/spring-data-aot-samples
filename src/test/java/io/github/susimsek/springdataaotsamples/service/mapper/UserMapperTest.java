package io.github.susimsek.springdataaotsamples.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.domain.Authority;
import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.security.AuthoritiesConstants;
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
        user.setEmail("alice@example.com");
        user.setAuthorities(
                Set.of(
                        new Authority(null, AuthoritiesConstants.USER),
                        new Authority(null, AuthoritiesConstants.ADMIN)));

        UserDTO dto = mapper.toDto(user);

        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.username()).isEqualTo("alice");
        assertThat(dto.email()).isEqualTo("alice@example.com");
        assertThat(dto.authorities())
                .containsExactlyInAnyOrder(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);
    }

    @Test
    void authorityNamesShouldHandleNullOrEmpty() {
        assertThat(mapper.authorityNames(null)).isEmpty();
        assertThat(mapper.authorityNames(Set.of())).isEmpty();
    }
}
