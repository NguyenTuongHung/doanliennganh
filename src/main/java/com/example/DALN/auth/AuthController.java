package com.example.DALN.auth;

import com.example.DALN.security.DalnPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.Profile register(@Valid @RequestBody RegisterRequest request) {
        try {
            if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu không được vượt quá 72 byte UTF-8");
            }
            return authService.register(request.email(), request.fullName(), request.password());
        } catch (AuthService.EmailAlreadyRegisteredException duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được đăng ký");
        }
    }

    @GetMapping("/me")
    public AuthService.Profile me(@org.springframework.security.core.annotation.AuthenticationPrincipal DalnPrincipal principal) {
        return authService.profile(principal.getId());
    }

    @PutMapping("/me")
    public AuthService.Profile updateMe(@org.springframework.security.core.annotation.AuthenticationPrincipal DalnPrincipal principal,
                                        @Valid @RequestBody UpdateProfileRequest request) {
        try {
            return authService.updateProfile(principal.getId(), request.fullName(), request.phone());
        } catch (org.springframework.dao.DuplicateKeyException duplicatePhone) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng");
        }
    }

    public record RegisterRequest(@NotBlank @Email @Size(max = 190) String email,
                                 @NotBlank @Size(max = 160) String fullName,
                                 @NotBlank @Size(min = 8, max = 72) String password) {}
    public record UpdateProfileRequest(@NotBlank @Size(max = 160) String fullName,
                                       @Size(max = 30) String phone) {}
}
