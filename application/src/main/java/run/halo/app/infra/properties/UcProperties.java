package run.halo.app.infra.properties;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class UcProperties {

    private String location = "classpath:/templates/uc/";

    @Valid
    private ProxyProperties proxy = new ProxyProperties();

}
