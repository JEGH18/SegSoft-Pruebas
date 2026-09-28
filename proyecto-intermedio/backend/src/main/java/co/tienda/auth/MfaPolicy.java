package co.tienda.auth;

import co.tienda.user.User;
import dev.samstevens.totp.code.CodeVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class MfaPolicy {

    private final Set<String> rolesRequiringMfa;
    private final CodeVerifier codeVerifier;

    public MfaPolicy(@Value("${mfa.required.roles}") Set<String> rolesRequiringMfa, CodeVerifier codeVerifier) {
        this.rolesRequiringMfa = rolesRequiringMfa;
        this.codeVerifier = codeVerifier;
    }

    public boolean requiresMfa(User user) {
        return rolesRequiringMfa.contains(user.getRole());
    }

    public boolean isValidCode(User user, String code) {
        return code != null && codeVerifier.isValidCode(user.getTotpSecret(), code);
    }
}
