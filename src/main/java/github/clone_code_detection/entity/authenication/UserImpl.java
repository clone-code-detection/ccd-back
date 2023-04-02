package github.clone_code_detection.entity.authenication;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;


    // language=PostgreSQL
    @Formula("select " +
            "distinct (concat('ROLE_', role.name))\n" +
            "from authen.\"user\"\n" +
            "         join authen.relation_user_role rur on \"user\".id = rur.user_id\n" +
            "         join authen.relation_role_authority rra on rur.role_id = rra.role_id\n" +
            "         join authen.role on rra.role_id = role.id\n" +
            "         join authen.authority on rra.authority_id = authority.id\n" +
            "where authen.\"user\".id = :id\n" +
            "\n" +
            "UNION ALL\n" +
            "\n" +
            "select distinct authority.name as authorities\n" +
            "from authen.\"user\"\n" +
            "         join authen.relation_user_role rur on \"user\".id = rur.user_id\n" +
            "         join authen.relation_role_authority rra on rur.role_id = rra.role_id\n" +
            "         join authen.role on rra.role_id = role.id\n" +
            "         join authen.authority on rra.authority_id = authority.id\n" +
            "where authen.\"user\".id = :id;")
    @ElementCollection(targetClass = String.class)
    Collection<String> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> grantedAuthorities = new ArrayList<>();
        for (String authority : authorities) {
            grantedAuthorities.add(new SimpleGrantedAuthority(authority));
        }
        return grantedAuthorities;
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
