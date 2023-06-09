package github.clone_code_detection.entity.authenication;

import github.clone_code_detection.entity.moodle.UserReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "user", schema = "authen")
public class UserImpl implements UserDetails {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @ManyToMany
    @JoinTable(name = "relation_user_role", schema = "authen", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Collection<Role> roles;

    @OneToOne(mappedBy = "internalUser", cascade = {CascadeType.ALL})
    private UserReference reference;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<String> authorities = new ArrayList<>();
        for (Role role : roles) {
            String roleName = "ROLE_" + role.getRole();
            authorities.add(roleName);
            for (Authority authority : role.getAuthorities()) {
                String authorityName = authority.getAuthority();
                authorities.add(authorityName);
            }
        }
        return authorities.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
