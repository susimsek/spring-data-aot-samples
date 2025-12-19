package io.github.susimsek.springdataaotsamples.service.mapper;

import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.service.dto.TagDTO;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TagMapper extends EntityMapper<TagDTO, Tag> {

  default Set<Tag> toTags(Set<String> names) {
    if (CollectionUtils.isEmpty(names)) {
      return Set.of();
    }
    return names.stream()
        .filter(StringUtils::hasText)
        .map(name -> name.trim().toLowerCase(Locale.ROOT))
        .distinct()
        .map(normalized -> new Tag(null, normalized))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  default Set<String> toNames(Set<Tag> tags) {
    if (CollectionUtils.isEmpty(tags)) {
      return Set.of();
    }
    return tags.stream().map(Tag::getName).collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
