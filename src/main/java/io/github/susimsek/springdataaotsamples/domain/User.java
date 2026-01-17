package io.github.susimsek.springdataaotsamples.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "user_identity")
@NamedEntityGraph(
        name = "User.withAuthorities",
        attributeNodes = @NamedAttributeNode("authorities"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class User extends AuditableEntity {

    @Id
    @SequenceGenerator(
            name = "user_identity_seq",
            sequenceName = "user_identity_seq",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_identity_seq")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;

    @ManyToMany
    @JoinTable(
            name = "user_authority",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Authority> authorities = new HashSet<>();

    @PrePersist
    @PreUpdate
    void normalize() {
        username = username.trim().toLowerCase(Locale.ROOT);
        email = email.trim().toLowerCase(Locale.ROOT);
    }
}
