package io.github.susimsek.springdataaotsamples.service.mapper;

import io.github.susimsek.springdataaotsamples.config.audit.RevisionInfo;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.domain.Tag;
import io.github.susimsek.springdataaotsamples.service.dto.NoteCreateRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NotePatchRequest;
import io.github.susimsek.springdataaotsamples.service.dto.NoteRevisionDTO;
import io.github.susimsek.springdataaotsamples.service.dto.NoteUpdateRequest;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.data.history.Revision;

import java.util.Set;

@Mapper(componentModel = "spring", uses = TagMapper.class)
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
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedDate", ignore = true)
    void applyRevision(Note source, @MappingTarget Note target,
                       @Context Set<Tag> resolvedTags);

    @AfterMapping
    default void resolveRevisionTags(@MappingTarget Note target,
                                     @Context Set<Tag> resolvedTags) {
        target.setTags(resolvedTags != null ? resolvedTags : Set.of());
        target.setDeleted(false);
        target.setDeletedBy(null);
        target.setDeletedDate(null);
    }

    default NoteRevisionDTO toRevisionDto(Revision<Long, Note> revision) {
        if (revision == null) {
            return null;
        }
        var meta = revision.getMetadata();
        var type = meta.getRevisionType();
        var auditor = meta.getDelegate() instanceof RevisionInfo info ? info.getUsername() : null;
        return new NoteRevisionDTO(
                meta.getRevisionNumber().orElse(null),
                type.name(),
                meta.getRevisionInstant().orElse(null),
                auditor,
                toDto(revision.getEntity())
        );
    }
}
