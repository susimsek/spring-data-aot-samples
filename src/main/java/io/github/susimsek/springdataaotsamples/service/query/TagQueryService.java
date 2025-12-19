package io.github.susimsek.springdataaotsamples.service.query;

import io.github.susimsek.springdataaotsamples.domain.Tag_;
import io.github.susimsek.springdataaotsamples.repository.TagRepository;
import io.github.susimsek.springdataaotsamples.service.dto.TagDTO;
import io.github.susimsek.springdataaotsamples.service.mapper.TagMapper;
import io.github.susimsek.springdataaotsamples.service.spec.TagSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagQueryService {

  private final TagRepository tagRepository;
  private final TagMapper tagMapper;

  @Transactional(readOnly = true)
  public Page<TagDTO> suggestPrefixPage(String prefix, Pageable pageable) {
    var spec = TagSpecifications.startsWith(prefix);
    var pageableWithSort = ensureSorted(pageable);
    return tagRepository.findAll(spec, pageableWithSort).map(tagMapper::toDto);
  }

  private Pageable ensureSorted(Pageable pageable) {
    if (pageable.getSort().isUnsorted()) {
      return PageRequest.of(
          pageable.getPageNumber(),
          pageable.getPageSize(),
          Sort.by(Tag_.name.getName()).ascending());
    }
    return pageable;
  }
}
