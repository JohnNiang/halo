package run.halo.app.infra;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import run.halo.app.infra.properties.HaloProperties;
import run.halo.app.infra.properties.SecurityProperties.KeyStoreProperties;

@Slf4j
@Component
public class KeystoreInitializer implements InitializingBean {

    public static final String BEAN_NAME = "keystoreInitializer";

    private final Path keyStorePath;

    private final KeyStoreProperties keyStoreProperties;

    public KeystoreInitializer(HaloProperties haloProperties) {
        this.keyStorePath = haloProperties.getWorkDir().resolve("keys/halo2.keystore");
        this.keyStoreProperties = haloProperties.getSecurity().getKeyStore();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!Files.exists(keyStorePath)) {
            log.info("Initializing key store at {}", keyStorePath);
            Files.createDirectories(keyStorePath.getParent());
            Files.createFile(keyStorePath);
            var password = keyStoreProperties.getPassword().toCharArray();
            var keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null, password);
            try (var os = Files.newOutputStream(keyStorePath, CREATE_NEW)) {
                keystore.store(os, password);
            }
            log.info("Initialized key store at {}", keyStorePath);
        }
    }

    public KeyStore computeKeyStore(Consumer<KeyStore> keystoreWriter) throws Exception {
        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        var password = keyStoreProperties.getPassword().toCharArray();
        try (var is = Files.newInputStream(keyStorePath, READ)) {
            keyStore.load(is, password);
        }
        keystoreWriter.accept(keyStore);
        try (var os = Files.newOutputStream(keyStorePath, WRITE, TRUNCATE_EXISTING)) {
            keyStore.store(os, password);
        }
        return keyStore;
    }

}
