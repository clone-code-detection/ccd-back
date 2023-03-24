package github.clone_code_detection.repo;

import github.clone_code_detection.entity.authenication.UserImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class RepoUser {
    public static final String saveUserByNameAndPassword = "insert into authen.user(username, password) values (:username, :password)";
    public static final String findUserByName = "select * from authen.user where username = (:username)";
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public RepoUser(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Nullable
    public UserImpl findUserByName(@Nonnull String name) {
        List<UserImpl> users = namedParameterJdbcTemplate.query(findUserByName, Map.of("username", name),
                                                                BeanPropertyRowMapper.newInstance(UserImpl.class));
        if (CollectionUtils.isEmpty(users)) return null;
        return users.get(0);
    }

    public boolean create(@Nonnull UserImpl user) {
        Integer executeResult = namedParameterJdbcTemplate.execute(saveUserByNameAndPassword,
                                                                   Map.of("username", user.getUsername(), "password",
                                                                          user.getPassword()),
                                                                   PreparedStatement::executeUpdate);
        assert executeResult != null;
        return executeResult.equals(1);
    }
}