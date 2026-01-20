package io.github.susimsek.springdataaotsamples.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.repository.TagRepository;
import io.github.susimsek.springdataaotsamples.service.mapper.TagMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TagCommandServiceTest {

    @Mock private TagRepository tagRepository;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TagMapper tagMapper;

    @InjectMocks private TagCommandService tagCommandService;

    @Test
    void resolveTagsShouldReuseExistingAndCreateMissing() {
        Set<String> names = Set.of(" Java ", "spring");
        Tag java = new Tag(1L, "java");
        Tag spring = new Tag(2L, "spring");

        when(tagRepository.findByNameIn(ArgumentMatchers.<Set<String>>any()))
                .thenReturn(List.of(java));
        when(tagRepository.saveAllAndFlush(ArgumentMatchers.<Iterable<Tag>>any()))
                .thenReturn(List.of(spring));

        Set<Tag> result = tagCommandService.resolveTags(names);

        assertThat(result).containsExactlyInAnyOrder(java, spring);
        ArgumentCaptor<Set<String>> namesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(tagRepository).findByNameIn(namesCaptor.capture());
        assertThat(namesCaptor.getValue()).containsExactlyInAnyOrder("java", "spring");
        ArgumentCaptor<Iterable<Tag>> savedCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(tagRepository).saveAllAndFlush(savedCaptor.capture());
        assertThat(savedCaptor.getValue())
                .anySatisfy(t -> assertThat(t.getName()).isEqualTo("spring"));
    }

    @Test
    void resolveTagsShouldRecoverFromDuplicateTagCreationRace() {
        Set<String> names = Set.of("updated");
        Tag existing = new Tag(1L, "updated");

        when(tagRepository.findByNameIn(ArgumentMatchers.<Set<String>>any()))
                .thenReturn(List.of())
                .thenReturn(List.of(existing));
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(tagRepository)
                .saveAllAndFlush(ArgumentMatchers.<Iterable<Tag>>any());

        Set<Tag> result = tagCommandService.resolveTags(names);

        assertThat(result).containsExactly(existing);
    }

    @Test
    void resolveTagsShouldReturnEmptyWhenNamesEmpty() {
        Set<Tag> result = tagCommandService.resolveTags(Set.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(tagRepository, tagMapper);
    }

    @Test
    void cleanupOrphanTagsAsyncShouldDeleteOrphansWhenAnyExist() {
        when(tagRepository.deleteOrphans()).thenReturn(3);
        tagCommandService.cleanupOrphanTagsAsync();
        verify(tagRepository).deleteOrphans();
        verifyNoMoreInteractions(tagRepository);
    }

    @Test
    void cleanupOrphanTagsAsyncShouldStillCallDeleteOrphansWhenNoneExist() {
        tagCommandService.cleanupOrphanTagsAsync();
        verify(tagRepository).deleteOrphans();
        verifyNoMoreInteractions(tagRepository);
    }
}
