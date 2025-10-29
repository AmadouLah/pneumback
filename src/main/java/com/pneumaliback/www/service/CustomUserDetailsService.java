package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email == null ? "" : email.trim();
        User user = userRepository.findByEmailIgnoreCase(normalized)
                .orElseThrow(() -> {
                    log.warn("Tentative de connexion avec un email inexistant: {}", normalized);
                    return new UsernameNotFoundException("Utilisateur non trouvé: " + normalized);
                });
        if (!user.isAccountNonLocked()) {
            log.warn("Tentative de connexion avec un compte verrouillé: {}", normalized);
            throw new UsernameNotFoundException("Compte verrouillé: " + normalized);
        }
        
        log.debug("Utilisateur chargé: {}", email);
        return user;
    }
}
