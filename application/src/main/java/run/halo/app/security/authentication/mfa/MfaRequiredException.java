package run.halo.app.security.authentication.mfa;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MfaRequiredException extends ResponseStatusException {

    private static final URI type = URI.create("https://halo.run/probs/mfa-required");

    public MfaRequiredException(URI redirectURI) {
        super(HttpStatus.UNAUTHORIZED, "MFA required");
        setType(type);
        getBody().setProperty("redirectURI", redirectURI);
    }

}
