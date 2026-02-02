package com.venus.kyc.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    public CustomUserDetailsService(UserRepository userRepository, PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<String> permissions = permissionRepository.findPermissionsByRole(user.role());
        String[] authorities = new String[permissions.size() + 1];
        // Ensure we don't double prefix if DB already has it, or just use bare role if
        // configured that way.
        // Spring Security usually wants ROLE_ for hasRole(), or any string for
        // hasAuthority().
        // Our SecurityConfig uses hasAuthority("MANAGE_USERS") etc.
        // It does NOT use hasRole().
        // However, standard is usually ROLE_ prefix for the role itself.
        authorities[0] = user.role().startsWith("ROLE_") ? user.role() : "ROLE_" + user.role();
        for (int i = 0; i < permissions.size(); i++) {
            authorities[i + 1] = permissions.get(i);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.username())
                .password(user.password())
                .authorities(authorities)
                .disabled(!user.enabled())
                .build();
    }
}
