package io.github.susimsek.springdataaotsamples.service.query;

import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import io.github.susimsek.springdataaotsamples.service.dto.UserSearchDTO;
import io.github.susimsek.springdataaotsamples.service.spec.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserSearchDTO> searchUsernames(String query, Pageable pageable) {
        var spec = UserSpecifications.usernameContains(query);
        return userRepository
                .findAll(spec, pageable)
                .map(user -> new UserSearchDTO(user.getId(), user.getUsername(), user.isEnabled()));
    }
}
