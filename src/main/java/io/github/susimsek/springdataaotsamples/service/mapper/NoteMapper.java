package io.github.susimsek.springdataaotsamples.service.mapper;

import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCreateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NotePatchRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.LinkedHashSet;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = TagMapper.class)
public interface NoteMapper extends EntityMapper<NoteDTO, Note> {

    @Mapping(target = "tags", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
    Note toEntity(NoteCreateRequest request);

    @Mapping(target = "tags", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
    void updateEntity(NoteUpdateRequest request, @MappingTarget Note note);

    @Mapping(target = "tags", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void patchEntity(NotePatchRequest request, @MappingTarget Note note);

    @Override
    @Mapping(target = "tags", ignore = true)
    Note toEntity(NoteDTO dto);

    @Override
    @Mapping(target = "tags", source = "tags")
    NoteDTO toDto(Note note);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tags", source = "tags", qualifiedByName = "cloneTags")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "deletedBy", expression = "java(null)")
    @Mapping(target = "deletedDate", expression = "java(null)")
    void applyRevision(Note source, @MappingTarget Note target);

    @Named("cloneTags")
    static Set<Tag> cloneTags(Set<Tag> tags) {
        return tags != null ? new LinkedHashSet<>(tags) : java.util.Set.of();
    }
}
