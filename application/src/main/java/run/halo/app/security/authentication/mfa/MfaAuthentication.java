package run.halo.app.security.authentication.mfa;

import static run.halo.app.security.authorization.AuthorityUtils.ANONYMOUS_ROLE_NAME;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class MfaAuthentication extends AbstractAuthenticationToken {

    private final Authentication previous;

    /**
     * Creates a token with the supplied array of authorities.
     *
     * @param previous the previous authentication
     */
    public MfaAuthentication(Authentication previous) {
        super(List.of(new SimpleGrantedAuthority(ANONYMOUS_ROLE_NAME)));
        this.previous = previous;
    }

    @Override
    public Object getCredentials() {
        return previous.getCredentials();
    }

    @Override
    public Object getPrincipal() {
        return previous.getPrincipal();
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    public Authentication getPrevious() {
        return previous;
    }
}
