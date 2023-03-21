package github.clone_code_detection.service;

import github.clone_code_detection.repo.RepoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements org.springframework.security.core.userdetails.UserDetailsService {
    private final RepoUser repoUserDetails;

    @Autowired
    public UserDetailsServiceImpl(RepoUser repoUserDetails) {
        this.repoUserDetails = repoUserDetails;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        assert username != null : "Username must not be empty";
        return repoUserDetails.findUserByName(username);
    }
}
