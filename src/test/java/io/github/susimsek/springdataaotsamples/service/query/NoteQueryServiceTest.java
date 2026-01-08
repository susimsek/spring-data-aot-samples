package io.github.susimsek.springdataaotsamples.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.domain.AuditableEntity_;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.Note_;
import io.github.susimsek.springdataaotsamples.repository.NoteRepository;
import io.github.susimsek.springdataaotsamples.service.NoteAuthorizationService;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCriteria;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.exception.NoteNotFoundException;
import io.github.susimsek.springdataaotsamples.service.mapper.NoteMapper;
import jakarta.persistence.metamodel.SingularAttribute;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class NoteQueryServiceTest {

    private static final Instant DEFAULT_TIMESTAMP = Instant.parse("2024-01-01T10:15:30Z");

    @Mock private NoteRepository noteRepository;

    @Mock private NoteMapper noteMapper;

    @Mock private NoteAuthorizationService noteAuthorizationService;

    @InjectMocks private NoteQueryService noteQueryService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void ensureJpaMetamodelInitialized() {
        if (Note_.pinned == null) {
            Note_.pinned = namedSingularAttribute("pinned");
        }
        if (AuditableEntity_.createdDate == null) {
            AuditableEntity_.createdDate = namedSingularAttribute("createdDate");
        }
    }

    private static <X, Y> SingularAttribute<X, Y> namedSingularAttribute(String name) {
        @SuppressWarnings("unchecked")
        SingularAttribute<X, Y> attribute =
                (SingularAttribute<X, Y>)
                        Proxy.newProxyInstance(
                                NoteQueryServiceTest.class.getClassLoader(),
                                new Class<?>[] {SingularAttribute.class},
                                (proxy, method, args) -> {
                                    if ("getName".equals(method.getName())) {
                                        return name;
                                    }
                                    return null;
                                });
        return attribute;
    }

    @Test
    void findAllShouldPrioritizePinnedInSortAndMapResults() {
        ensureJpaMetamodelInitialized();
        Pageable pageable = PageRequest.of(0, 10);
        Note note = org.mockito.Mockito.mock(Note.class);
        NoteDTO dto = sampleDto("t", "c", false, "#123456", Set.of("tag"));

        when(noteRepository.findAllWithTags(any(), any()))
                .thenAnswer(
                        invocation -> new PageImpl<>(List.of(note), invocation.getArgument(1), 1));
        when(noteMapper.toDto(note)).thenReturn(dto);

        Page<NoteDTO> result = noteQueryService.findAll(pageable, null, Set.of(), null, null);

        assertThat(result.getContent()).containsExactly(dto);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(noteRepository).findAllWithTags(any(), pageableCaptor.capture());

        Sort sort = pageableCaptor.getValue().getSort();
        List<Sort.Order> orders = sort.stream().toList();
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0)).isEqualTo(Sort.Order.desc("pinned"));
        assertThat(orders.get(1)).isEqualTo(Sort.Order.desc("createdDate"));
    }

    @Test
    void findByCriteriaShouldAcceptDeletedQueryTagsColorPinnedOwnerAndKeepExistingSort() {
        ensureJpaMetamodelInitialized();
        Pageable pageable = PageRequest.of(2, 20, Sort.by(Sort.Order.asc("title")));
        NoteCriteria criteria =
                new NoteCriteria("hello", true, Set.of("  "), "#abcdef", true, "ALICE");

        when(noteRepository.findAllWithTags(any(), any())).thenReturn(Page.empty());

        Page<NoteDTO> result = noteQueryService.findByCriteria(criteria, pageable);

        assertThat(result).isEmpty();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(noteRepository).findAllWithTags(any(), pageableCaptor.capture());
        List<Sort.Order> orders = pageableCaptor.getValue().getSort().stream().toList();
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0)).isEqualTo(Sort.Order.desc("pinned"));
        assertThat(orders.get(1)).isEqualTo(Sort.Order.asc("title"));
    }

    @Test
    void findAllForCurrentUserShouldThrowWhenCurrentUserMissing() {
        ensureJpaMetamodelInitialized();
        ThrowingCallable call =
                () -> noteQueryService.findAllForCurrentUser(PageRequest.of(0, 10), null, null, null, null);
        assertThrows(
                UsernameNotFoundException.class,
                call);
        verifyNoInteractions(noteRepository);
    }

    @Test
    void findAllForCurrentUserShouldCallRepositoryWhenAuthenticated() {
        ensureJpaMetamodelInitialized();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("alice", "n/a"));
        when(noteRepository.findAllWithTags(any(), any())).thenReturn(Page.empty());

        Page<NoteDTO> result =
                noteQueryService.findAllForCurrentUser(
                        PageRequest.of(0, 10), null, null, null, null);

        assertThat(result).isEmpty();
        verify(noteRepository).findAllWithTags(any(), any());
    }

    @Test
    void findByIdShouldMapWhenFound() {
        Note note = org.mockito.Mockito.mock(Note.class);
        NoteDTO dto = sampleDto("t", "c", false, null, Set.of());

        when(noteRepository.findOne(org.mockito.ArgumentMatchers.<Specification<Note>>any()))
                .thenReturn(java.util.Optional.of(note));
        when(noteMapper.toDto(note)).thenReturn(dto);

        NoteDTO result = noteQueryService.findById(42L);

        assertThat(result).isEqualTo(dto);
        verify(noteAuthorizationService, org.mockito.Mockito.never()).ensureReadAccess(any());
    }

    @Test
    void findByIdForCurrentUserShouldAuthorizeAndMap() {
        Note note = org.mockito.Mockito.mock(Note.class);
        NoteDTO dto = sampleDto("t", "c", false, null, Set.of());

        when(noteRepository.findOne(org.mockito.ArgumentMatchers.<Specification<Note>>any()))
                .thenReturn(java.util.Optional.of(note));
        when(noteMapper.toDto(note)).thenReturn(dto);

        NoteDTO result = noteQueryService.findByIdForCurrentUser(42L);

        assertThat(result).isEqualTo(dto);
        verify(noteAuthorizationService).ensureReadAccess(note);
    }

    @Test
    void findByIdShouldThrowWhenNoteMissing() {
        when(noteRepository.findOne(org.mockito.ArgumentMatchers.<Specification<Note>>any()))
                .thenReturn(java.util.Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> noteQueryService.findById(1L));
    }

    @Test
    void findByIdForCurrentUserShouldThrowWhenNoteMissing() {
        when(noteRepository.findOne(org.mockito.ArgumentMatchers.<Specification<Note>>any()))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                NoteNotFoundException.class, () -> noteQueryService.findByIdForCurrentUser(1L));
    }

    private NoteDTO sampleDto(
            String title, String content, boolean pinned, String color, Set<String> tags) {
        return new NoteDTO(
                1L,
                title,
                content,
                pinned,
                color,
                "alice",
                tags,
                0L,
                "alice",
                DEFAULT_TIMESTAMP,
                "alice",
                DEFAULT_TIMESTAMP,
                false,
                null,
                null);
    }
}
