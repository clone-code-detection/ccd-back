package github.clone_code_detection.repo;

import github.clone_code_detection.entity.authenication.UserImpl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nonnull;
import java.util.UUID;

@Repository
@Validated
public interface RepoUser extends JpaRepository<UserImpl, UUID> {
    // language=PostgresSQL
    String saveUserWithNameAndPassword = """
            insert into authen."user"(username, password)
            values (:username, :password)
            """;

    // language=PostgresSQL
    String addRoleToUserWithName = """
            insert into authen.relation_user_role(user_id, role_id)
            select "user".id, "role".id
            from authen."user",
                 authen."role"
            where username = (:username)
            and role.name = (:role);""";

    UserImpl findUserImplByUsername(@Nonnull String name);

    @Modifying
    @Query(value = saveUserWithNameAndPassword, nativeQuery = true)
    void insertUser(@Param("username") String username, @Param("password") String password);

    @Modifying
    @Query(value = addRoleToUserWithName, nativeQuery = true)
    void addRole(@Param("username") String username, @Param("role") String role);

    private UserImpl createUserWithRole(@Nonnull UserImpl user, String role) {
        insertUser(user.getUsername(), user.getPassword());
        addRole(user.getUsername(), role);
        return this.findUserImplByUsername(user.getUsername());
    }

    @Nonnull
    default UserImpl createOrgUser(@Nonnull UserImpl user) {

        return this.createUserWithRole(user, "admin");
    }

    @Nonnull
    default UserImpl createStandaloneUser(@Nonnull UserImpl user) {
        return this.createUserWithRole(user, "standalone");
    }


}