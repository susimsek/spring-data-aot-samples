package io.github.susimsek.springdataaotsamples.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.repository.TagRepository;
import io.github.susimsek.springdataaotsamples.service.mapper.TagMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagCommandServiceTest {

    @Mock private TagRepository tagRepository;

    @Mock private TagMapper tagMapper;

    @Mock private CacheProvider cacheProvider;

    @InjectMocks private TagCommandService tagCommandService;

    @Test
    void resolveTagsShouldReuseExistingAndCreateMissing() {
        Set<String> names = Set.of("Java", "spring");
        Tag java = new Tag(1L, "java");
        Tag spring = new Tag(2L, "spring");

        when(tagMapper.toTags(names))
                .thenAnswer(
                        inv ->
                                ((Set<String>) inv.getArgument(0))
                                        .stream()
                                                .map(String::toLowerCase)
                                                .map(n -> new Tag(null, n))
                                                .collect(
                                                        Collectors.toCollection(
                                                                LinkedHashSet::new)));
        when(tagRepository.findByNameIn(Set.of("java", "spring"))).thenReturn(List.of(java));
        when(tagRepository.saveAll(org.mockito.ArgumentMatchers.<Iterable<Tag>>any()))
                .thenReturn(List.of(spring));

        Set<Tag> result = tagCommandService.resolveTags(names);

        assertThat(result).containsExactly(java, spring);
        verify(tagRepository).findByNameIn(Set.of("java", "spring"));
        ArgumentCaptor<Iterable<Tag>> savedCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(tagRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue())
                .anySatisfy(t -> assertThat(t.getName()).isEqualTo("spring"));
    }

    @Test
    void resolveTagsShouldReturnEmptyWhenNamesEmpty() {
        Set<Tag> result = tagCommandService.resolveTags(Set.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(tagRepository, cacheProvider, tagMapper);
    }

    @Test
    void cleanupOrphanTagsAsyncShouldDeleteAndClearCacheWhenIdsExist() {
        List<Long> orphanIds = List.of(1L, 2L, 3L);
        when(tagRepository.findOrphanIds()).thenReturn(orphanIds);

        tagCommandService.cleanupOrphanTagsAsync();

        verify(tagRepository).deleteAllByIdInBatch(orphanIds);
        verify(cacheProvider).clearCache(Tag.class.getName());
    }

    @Test
    void cleanupOrphanTagsAsyncShouldDoNothingWhenNoOrphans() {
        when(tagRepository.findOrphanIds()).thenReturn(List.of());

        tagCommandService.cleanupOrphanTagsAsync();

        verify(tagRepository, never()).deleteAllByIdInBatch(any());
        verifyNoInteractions(cacheProvider);
    }
}
