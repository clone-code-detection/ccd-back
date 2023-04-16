package github.clone_code_detection.service.user;

import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.repo.RepoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.text.MessageFormat;

@Service
public class ServiceUserDetailsImpl implements org.springframework.security.core.userdetails.UserDetailsService {
    private final RepoUser repoUserDetails;

    @Autowired
    public ServiceUserDetailsImpl(RepoUser repoUserDetails) {
        this.repoUserDetails = repoUserDetails;
    }

    @Override
    public UserDetails loadUserByUsername(@Nonnull String username) {
        UserImpl user = repoUserDetails.findUserImplByUsername(username);
        if (user == null)
            throw new UsernameNotFoundException(MessageFormat.format("User with name {0} not found", username));
        return user;
    }
}
