package github.clone_code_detection.repo;

import github.clone_code_detection.entity.authenication.UserImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class RepoUser {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public RepoUser(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public UserImpl findUserByName(String name) {
        try {
            List<UserImpl> users = namedParameterJdbcTemplate.query(
                    "select * from authen.\"user\" where username = (:username)",
                    Map.of("username", name),
                    BeanPropertyRowMapper.newInstance(UserImpl.class));
            assert !CollectionUtils.isEmpty(users);
            return users.get(0);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public boolean create(UserImpl user) {
        return true;
    }
}