package com.pegacorn.rently.security;

import com.pegacorn.rently.entity.User;
import com.pegacorn.rently.repository.UserRepository;
import com.pegacorn.rently.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with phone: " + phone));

        user.setRoles(userRoleRepository.findRolesByUserId(user.getId()));
        return new UserPrincipal(user);
    }

    public UserDetails loadUserById(String userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        user.setRoles(userRoleRepository.findRolesByUserId(user.getId()));
        return new UserPrincipal(user);
    }
}
