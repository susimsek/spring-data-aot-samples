package io.github.susimsek.springdataaotsamples.service.mapper;

import io.github.susimsek.springdataaotsamples.config.audit.RevisionInfo;
import io.github.susimsek.springdataaotsamples.domain.Note;
import io.github.susimsek.springdataaotsamples.service.dto.NoteRevisionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.data.history.Revision;

import java.time.Instant;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = NoteMapper.class)
public interface NoteRevisionMapper {

    @Mapping(target = "revision", source = "revision", qualifiedByName = "revisionNumber")
    @Mapping(target = "versionNumber", ignore = true)
    @Mapping(target = "revisionType", source = "revision", qualifiedByName = "revisionType")
    @Mapping(target = "revisionDate", source = "revision", qualifiedByName = "revisionInstant")
    @Mapping(target = "auditor", source = "revision", qualifiedByName = "revisionAuditor")
    @Mapping(target = "note", source = "entity")
    NoteRevisionDTO toRevisionDto(Revision<Long, Note> revision);

    @Mapping(target = "revision", source = "revision", qualifiedByName = "revisionNumber")
    @Mapping(target = "versionNumber", source = "versionNumber")
    @Mapping(target = "revisionType", source = "revision", qualifiedByName = "revisionType")
    @Mapping(target = "revisionDate", source = "revision", qualifiedByName = "revisionInstant")
    @Mapping(target = "auditor", source = "revision", qualifiedByName = "revisionAuditor")
    @Mapping(target = "note", source = "revision.entity")
    NoteRevisionDTO toRevisionDto(Revision<Long, Note> revision, String versionNumber);

    @Named("revisionNumber")
    default Long mapRevisionNumber(Revision<Long, ?> revision) {
        return revision.getMetadata().getRevisionNumber().orElse(null);
    }

    @Named("revisionType")
    default String mapRevisionType(Revision<Long, ?> revision) {
        var type = revision.getMetadata().getRevisionType();
        return type.name();
    }

    @Named("revisionInstant")
    default Instant mapRevisionInstant(Revision<Long, ?> revision) {
        return revision.getMetadata().getRevisionInstant().orElse(null);
    }

    @Named("revisionAuditor")
    default String extractAuditor(Revision<Long, ?> revision) {
        var delegate = revision.getMetadata().getDelegate();
        if (delegate instanceof RevisionInfo info) {
            return info.getUsername();
        }
        return null;
    }

    default Page<NoteRevisionDTO> toRevisionDtoPage(Page<Revision<Long, Note>> page) {
        var pageable = page.getPageable();
        var content = page.getContent();
        var total = page.getTotalElements();
        var pageOffset = pageable.getOffset();
        var mapped = IntStream.range(0, content.size())
                .mapToObj(i -> {
                    var revision = content.get(i);
                    var versionNumber = String.valueOf(Math.max(1L, total - pageOffset - i));
                    return toRevisionDto(revision, versionNumber);
                })
                .toList();
        return new PageImpl<>(mapped, pageable, total);
    }
}
