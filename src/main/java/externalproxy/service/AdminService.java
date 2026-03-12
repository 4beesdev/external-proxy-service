package externalproxy.service;

import externalproxy.domain.Admin;
import externalproxy.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;

    public Admin loadUserByUsername(String username) {
        return adminRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + username));
    }

    public UserDetailsService adminDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) {
                return adminRepository.findByEmail(username)
                        .orElseThrow(() -> new RuntimeException("User not found: " + username + " " +
                                "@404"));
            }
        };
    }
}
