package com.musicdreamer.identity.web;

import com.musicdreamer.identity.application.ActivationApplicationService;
import com.musicdreamer.identity.application.RegistrationApplicationService;
import com.musicdreamer.identity.application.RegistrationCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity")
public final class IdentityRegistrationController {

    private final RegistrationApplicationService registration;
    private final ActivationApplicationService activation;

    public IdentityRegistrationController(
            RegistrationApplicationService registration,
            ActivationApplicationService activation) {
        this.registration = Objects.requireNonNull(registration, "registration");
        this.activation = Objects.requireNonNull(activation, "activation");
    }

    @PostMapping("/registrations")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest request) {
        var result = registration.register(new RegistrationCommand(
                request.username(), request.email(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegistrationResponse(result.userId()));
    }

    @PostMapping("/activations")
    public ResponseEntity<Void> activate(@Valid @RequestBody ActivationRequest request) {
        activation.activate(request.token());
        return ResponseEntity.noContent().build();
    }

    public record RegistrationRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank String password) {
    }

    public record RegistrationResponse(long userId) {
    }

    public record ActivationRequest(@NotBlank String token) {
    }
}
