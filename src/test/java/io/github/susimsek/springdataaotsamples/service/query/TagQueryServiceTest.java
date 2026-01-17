package io.github.susimsek.springdataaotsamples.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.domain.Tag_;
import io.github.susimsek.springdataaotsamples.repository.TagRepository;
import io.github.susimsek.springdataaotsamples.service.dto.TagDTO;
import io.github.susimsek.springdataaotsamples.service.mapper.TagMapper;
import jakarta.persistence.metamodel.SingularAttribute;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TagQueryServiceTest {

    @Mock private TagRepository tagRepository;

    @Mock private TagMapper tagMapper;

    @InjectMocks private TagQueryService tagQueryService;

    @Test
    void suggestPrefixPageShouldApplyDefaultSortWhenUnsorted() {
        ensureJpaMetamodelInitialized();
        Tag tag = new Tag(1L, "java");
        Page<Tag> page = new PageImpl<>(List.of(tag));
        when(tagRepository.findAll(ArgumentMatchers.<Specification<Tag>>any(), any(Pageable.class)))
                .thenReturn(page);
        when(tagMapper.toDto(tag)).thenReturn(new TagDTO(1L, "java"));

        Page<TagDTO> result =
                tagQueryService.suggestPrefixPage("ja", PageRequest.of(0, 5, Sort.unsorted()));

        assertThat(result.getContent()).singleElement().extracting(TagDTO::name).isEqualTo("java");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(tagRepository)
                .findAll(ArgumentMatchers.<Specification<Tag>>any(), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getSort().getOrderFor("name")).isNotNull();
        assertThat(used.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void suggestPrefixPageShouldHonorExistingSort() {
        ensureJpaMetamodelInitialized();
        Tag tag = new Tag(2L, "spring");
        Page<Tag> page = new PageImpl<>(List.of(tag));
        when(tagRepository.findAll(ArgumentMatchers.<Specification<Tag>>any(), any(Pageable.class)))
                .thenReturn(page);
        when(tagMapper.toDto(tag)).thenReturn(new TagDTO(2L, "spring"));

        Pageable custom = PageRequest.of(1, 10, Sort.by("createdDate").descending());
        Page<TagDTO> result = tagQueryService.suggestPrefixPage("sp", custom);

        assertThat(result.getContent()).singleElement().extracting(TagDTO::id).isEqualTo(2L);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(tagRepository)
                .findAll(ArgumentMatchers.<Specification<Tag>>any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdDate")).isNotNull();
    }

    private static void ensureJpaMetamodelInitialized() {
        if (Tag_.name == null) {
            Tag_.name = namedSingularAttribute("name");
        }
    }

    private static <X, Y> SingularAttribute<X, Y> namedSingularAttribute(String name) {
        @SuppressWarnings("unchecked")
        SingularAttribute<X, Y> attribute =
                (SingularAttribute<X, Y>)
                        Proxy.newProxyInstance(
                                TagQueryServiceTest.class.getClassLoader(),
                                new Class<?>[] {SingularAttribute.class},
                                (proxy, method, args) -> {
                                    if ("getName".equals(method.getName())) {
                                        return name;
                                    }
                                    return null;
                                });
        return attribute;
    }
}
