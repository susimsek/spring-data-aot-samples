package io.github.susimsek.springdataaotsamples.service.mapper;

import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.service.dto.TagDTO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TagMapper extends EntityMapper<TagDTO, Tag> {

    default String normalizeName(String name) {
        return name == null ? null : name.trim().toLowerCase(Locale.ROOT);
    }

    @AfterMapping
    default void normalizeName(@MappingTarget Tag tag) {
        tag.setName(normalizeName(tag.getName()));
    }

    default Map<String, Tag> toTagMapByName(List<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return new LinkedHashMap<>();
        }
        return tags.stream()
                .collect(
                        Collectors.toMap(
                                Tag::getName,
                                Function.identity(),
                                (a, b) -> a,
                                LinkedHashMap::new));
    }

    default List<Tag> buildMissingTags(Set<String> normalizedNames, Map<String, Tag> byName) {
        if (CollectionUtils.isEmpty(normalizedNames)) {
            return List.of();
        }
        return normalizedNames.stream()
                .filter(name -> !byName.containsKey(name))
                .map(name -> new Tag(null, name))
                .toList();
    }

    default Set<Tag> toOrderedTags(Set<String> normalizedNames, Map<String, Tag> byName) {
        if (CollectionUtils.isEmpty(normalizedNames)) {
            return Set.of();
        }
        return normalizedNames.stream()
                .map(byName::get)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    default Set<Tag> toOrderedTags(Set<String> normalizedNames, List<Tag> tags) {
        return toOrderedTags(normalizedNames, toTagMapByName(tags));
    }

    default Set<String> toNames(Set<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return Set.of();
        }
        return tags.stream().map(Tag::getName).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
