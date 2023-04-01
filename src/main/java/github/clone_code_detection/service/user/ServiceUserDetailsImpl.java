package github.clone_code_detection.service.user;

import github.clone_code_detection.repo.RepoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service
public class ServiceUserDetailsImpl implements org.springframework.security.core.userdetails.UserDetailsService {
    private final RepoUser repoUserDetails;

    @Autowired
    public ServiceUserDetailsImpl(RepoUser repoUserDetails) {
        this.repoUserDetails = repoUserDetails;
    }

    @Override
    public UserDetails loadUserByUsername(@Nonnull String username) {
        return repoUserDetails.findUserByName(username);
    }
}
