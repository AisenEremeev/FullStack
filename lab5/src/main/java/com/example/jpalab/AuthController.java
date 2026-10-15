package com.example.jpalab;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    public AuthController(UserRepository users, PasswordEncoder passwords,
            AuthenticationManager authenticationManager, JwtEncoder jwtEncoder) {
        this.users = users;
        this.passwords = passwords;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
    }

    public record Credentials(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_]{3,40}") String username,
        @NotBlank @Size(min = 8, max = 64) String password) { }
    public record RegisteredUser(Long id, String username) { }
    public record TokenResponse(String accessToken, String tokenType, long expiresIn) { }
    public record CurrentUser(String username) { }

    @PostMapping("/register")
    public ResponseEntity<RegisteredUser> register(@Valid @RequestBody Credentials request) {
        checkPasswordLength(request.password());
        String username = request.username().toLowerCase(Locale.ROOT);
        if (users.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }
        try {
            AppUser user = users.saveAndFlush(new AppUser(username, passwords.encode(request.password())));
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisteredUser(user.getId(), user.getUsername()));
        } catch (DataIntegrityViolationException ex) {
            // The unique constraint also protects concurrent registrations.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody Credentials request) {
        checkPasswordLength(request.password());
        try {
            var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                    request.username().toLowerCase(Locale.ROOT), request.password()));
            Instant now = Instant.now();
            JwtClaimsSet claims = JwtClaimsSet.builder().issuer(SecurityConfig.ISSUER)
                .subject(authentication.getName()).issuedAt(now).expiresAt(now.plusSeconds(900)).build();
            String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new TokenResponse(token, "Bearer", 900));
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    @GetMapping("/me")
    public CurrentUser me(@AuthenticationPrincipal Jwt jwt) { return new CurrentUser(jwt.getSubject()); }

    private void checkPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password exceeds 72 UTF-8 bytes");
        }
    }
}
