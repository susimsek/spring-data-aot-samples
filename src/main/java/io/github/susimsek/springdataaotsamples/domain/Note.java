package io.github.susimsek.springdataaotsamples.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "note")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Note extends SoftDeletableEntity {

    @Id
    @SequenceGenerator(name = "note_seq", sequenceName = "note_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "note_seq")
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1024)
    private String content;

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(length = 20)
    private String color;

    @Column(name = "owner", nullable = false, length = 100)
    private String owner;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @BatchSize(size = 10)
    @JoinTable(name = "note_tag",
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Tag> tags = new LinkedHashSet<>();
}
