package io.github.susimsek.springdataaotsamples.service.exception;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

@NullMarked
public class RevisionNotFoundException extends ApiException {

  public RevisionNotFoundException(Long noteId, Long revision) {
    super(
        HttpStatus.NOT_FOUND,
        "Revision not found",
        "Revision " + revision + " for note " + noteId + " was not found",
        noteId,
        revision);
  }
}
