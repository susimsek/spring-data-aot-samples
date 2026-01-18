package io.github.susimsek.springdataaotsamples.service.command;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.repository.TagRepository;
import io.github.susimsek.springdataaotsamples.service.mapper.TagMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagCommandService {

    private final TagRepository tagRepository;
    private final CacheProvider cacheProvider;
    private final TagMapper tagMapper;

    @Transactional
    public Set<Tag> resolveTags(@Nullable Set<String> names) {
        if (CollectionUtils.isEmpty(names)) {
            return new LinkedHashSet<>();
        }
        Set<String> normalized =
                names.stream()
                        .map(this::normalizeTagName)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Tag> existing = tagRepository.findByNameIn(normalized);
        Map<String, Tag> byName = tagMapper.toTagMapByName(existing);

        List<Tag> missing = tagMapper.buildMissingTags(normalized, byName);

        if (!CollectionUtils.isEmpty(missing)) {
            try {
                tagRepository
                        .saveAllAndFlush(missing)
                        .forEach(tag -> byName.put(tag.getName(), tag));
            } catch (DataIntegrityViolationException _) {
                var refreshed = tagRepository.findByNameIn(normalized);
                return tagMapper.toOrderedTags(normalized, refreshed);
            }
        }

        return tagMapper.toOrderedTags(normalized, byName);
    }

    private String normalizeTagName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
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
        cacheProvider.clearCache(Tag.class.getName(), orphanIds);
    }
}
