package com.bookzzang.api.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/auth")
class PublicAuthController {
    private final CredentialAuthService credentials;
    PublicAuthController(CredentialAuthService credentials) { this.credentials = credentials; }

    @PostMapping("/email-availability")
    Map<String, Boolean> emailAvailability(@Valid @RequestBody EmailRequest request) { return Map.of("available", credentials.emailAvailable(request.email())); }
    @PostMapping("/signup") @ResponseStatus(HttpStatus.CREATED)
    Map<String, String> signUp(@Valid @RequestBody SignUpRequest request) { credentials.signUp(request.email(), request.password(), request.nickname(), request.gender(), request.ageGroup()); return Map.of("message", "signup complete"); }
    @PostMapping("/login")
    Map<String, Object> login(@Valid @RequestBody LoginRequest request) { return sessionResponse(credentials.login(request.email(), request.password())); }
    @PostMapping("/token/refresh")
    Map<String, Object> refresh(@Valid @RequestBody RefreshRequest request) { return sessionResponse(credentials.refresh(request.refreshToken())); }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshRequest request) { credentials.logout(request.refreshToken()); }
    private Map<String, Object> sessionResponse(CredentialAuthService.AuthSession session) { return Map.of("accessToken", session.accessToken(), "refreshToken", session.refreshToken(), "expiresIn", session.expiresIn()); }

    record EmailRequest(@NotBlank @Email String email) { }
    record SignUpRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 72) String password,
            @NotBlank @Size(min = 2, max = 20) String nickname,
            @Pattern(regexp = "FEMALE|MALE") String gender,
            @Min(10) @Max(90) Integer ageGroup
    ) {
        @AssertTrue(message = "ageGroup must be a multiple of 10")
        boolean isValidAgeGroup() { return ageGroup == null || ageGroup % 10 == 0; }
    }
    record LoginRequest(@NotBlank @Email String email, @NotBlank @Size(max = 72) String password) { }
    record RefreshRequest(@NotBlank String refreshToken) { }
}
