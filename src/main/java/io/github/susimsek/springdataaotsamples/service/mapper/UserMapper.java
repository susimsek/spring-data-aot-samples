package io.github.susimsek.springdataaotsamples.service.mapper;

import io.github.susimsek.springdataaotsamples.domain.Authority;
import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.service.dto.UserDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.util.CollectionUtils;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "authorities", source = "authorities", qualifiedByName = "authorityNames")
    UserDTO toDto(User user);

    @Named("authorityNames")
    default Set<String> authorityNames(Set<Authority> authorities) {
        if (CollectionUtils.isEmpty(authorities)) {
            return Set.of();
        }
        return authorities.stream().map(Authority::getName).collect(Collectors.toSet());
    }
}
