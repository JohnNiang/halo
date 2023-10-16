package run.halo.app.security.authentication.mfa;

import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class TotpAuthenticationToken extends AbstractAuthenticationToken {

    private final String code;

    public TotpAuthenticationToken(String code) {
        super(List.of());
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public Object getCredentials() {
        return getCode();
    }

    @Override
    public Object getPrincipal() {
        return getCode();
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }
}
