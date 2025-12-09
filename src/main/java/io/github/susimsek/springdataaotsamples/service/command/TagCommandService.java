package io.github.susimsek.springdataaotsamples.service.command;

import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.repository.TagRepository;
import io.github.susimsek.springdataaotsamples.service.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagCommandService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Transactional
    public Set<Tag> resolveTags(Set<String> names) {
        var normalized = normalizeNames(names);
        if (normalized.isEmpty()) {
            return new LinkedHashSet<>();
        }

        var existing = tagRepository.findByNameIn(normalized);
        Map<String, Tag> byName = existing.stream()
                .collect(Collectors.toMap(
                        Tag::getName,
                        Function.identity(),
                        (a, b) -> a,
                        LinkedHashMap::new));

        var missing = normalized.stream()
                .filter(name -> !byName.containsKey(name))
                .map(name -> {
                    var tag = new Tag();
                    tag.setName(name);
                    return tag;
                })
                .toList();

        if (!missing.isEmpty()) {
            var saved = tagRepository.saveAll(missing);
            saved.forEach(tag -> byName.put(tag.getName(), tag));
        }

        return new LinkedHashSet<>(byName.values());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupOrphanTagsAsync() {
        var orphanIds = tagRepository.findOrphanIds();
        if (orphanIds.isEmpty()) {
            return;
        }
        tagRepository.deleteAllByIdInBatch(orphanIds);
        log.debug("Deleted {} orphan tags", orphanIds.size());
    }

    private Set<String> normalizeNames(Set<String> names) {
        if (CollectionUtils.isEmpty(names)) {
            return Set.of();
        }

        return tagMapper.toTags(names).stream()
            .map(Tag::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
