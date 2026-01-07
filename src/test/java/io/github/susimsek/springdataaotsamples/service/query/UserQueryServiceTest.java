package io.github.susimsek.springdataaotsamples.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import io.github.susimsek.springdataaotsamples.service.dto.UserSearchDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserQueryService userQueryService;

    @Test
    void searchUsernamesShouldMapUsersToSearchDto() {
        User user = new User();
        user.setId(5L);
        user.setUsername("alice");
        user.setEnabled(true);
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(
                        org.mockito.ArgumentMatchers.<Specification<User>>any(),
                        any(Pageable.class)))
                .thenReturn(page);

        Page<UserSearchDTO> result = userQueryService.searchUsernames("ali", PageRequest.of(0, 5));

        assertThat(result.getContent())
                .singleElement()
                .satisfies(
                        dto -> {
                            assertThat(dto.id()).isEqualTo(5L);
                            assertThat(dto.username()).isEqualTo("alice");
                            assertThat(dto.enabled()).isTrue();
                        });

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository)
                .findAll(
                        org.mockito.ArgumentMatchers.<Specification<User>>any(),
                        pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
    }
}
