package github.clone_code_detection.repo;

import github.clone_code_detection.entity.authenication.UserImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Repository
public class RepoUser {
    // language=PostgreSQL
    public static final String saveUserWithNameAndPassword =
            "insert into authen.\"user\"(username, password)\n" + "values (:username, :password)\n";

    // language=PostgreSQL
    public static final String addRoleToUserWithName =
            "insert into authen.relation_user_role(user_id, role_id)\n" + "select \"user\".id, \"role\".id\n" +
                    "from authen.\"user\",\n" + "     authen.\"role\"\n" + "where username = (:username)\n" +
                    "and role.name = (:role);";

    // language=PostgreSQL
    public static final String findUserByName = "select \"user\".username,\n" + "       \"user\".password,\n" +
            "       array_agg(distinct (concat('ROLE_', role.name))) as roles,\n" +
            "       array_agg(distinct authority.name)               as authorities\n" + "from authen.\"user\"\n" +
            "         join authen.relation_user_role rur on \"user\".id = rur.user_id\n" +
            "         join authen.relation_role_authority rra on rur.role_id = rra.role_id\n" +
            "         join authen.role on rra.role_id = role.id\n" +
            "         join authen.authority on rra.authority_id = authority.id\n" + "where username = (:username) \n" +
            "group by username, password;";
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public RepoUser(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Nonnull
    public UserImpl findUserByName(@Nonnull String name) {
        try {
            return Objects.requireNonNull(
                    namedParameterJdbcTemplate.queryForObject(findUserByName, Map.of("username", name),
                                                              (rs, rowNum) -> UserImpl.builder()
                                                                                      .username(
                                                                                              rs.getString("username"))
                                                                                      .password(
                                                                                              rs.getString("password"))

                                                                                      .roles((String[]) rs.getArray(
                                                                                                                  "roles")
                                                                                                          .getArray())
                                                                                      .authorities(
                                                                                              (String[]) rs.getArray(
                                                                                                                   "authorities")
                                                                                                           .getArray())
                                                                                      .build()));
        } catch (EmptyResultDataAccessException ex) {
            throw new UsernameNotFoundException(MessageFormat.format("User {0} not found", name));
        }
    }

    private void createUserWithRole(@Nonnull UserImpl user, String role) {
        Integer executeResult = namedParameterJdbcTemplate.execute(saveUserWithNameAndPassword,
                                                                   Map.of("username", user.getUsername(), "password",
                                                                          user.getPassword(), "role", role),
                                                                   PreparedStatement::executeUpdate);
        assert executeResult != null;


        Integer addRoleResult = namedParameterJdbcTemplate.execute(addRoleToUserWithName,
                                                                   Map.of("username", user.getUsername(), "role", role),
                                                                   PreparedStatement::executeUpdate);
        assert addRoleResult != null;
    }

    public void createOrgUser(@Nonnull UserImpl user) {
        this.createUserWithRole(user, "admin");
    }

    public void createStandaloneUser(@Nonnull UserImpl user) {
        this.createUserWithRole(user, "standalone");
    }
}