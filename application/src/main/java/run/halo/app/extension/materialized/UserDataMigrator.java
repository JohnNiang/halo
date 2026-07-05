package run.halo.app.extension.materialized;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.User;
import run.halo.app.extension.ExtensionConverter;
import run.halo.app.extension.ExtensionStoreUtil;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.store.ReactiveExtensionStoreClient;
import run.halo.app.infra.InitializationPhase;

/**
 * Migrates User extension data from the {@code extensions} table to the materialized
 * {@code users}, {@code extension_labels}, and {@code user_roles} tables on startup.
 *
 * @author halo
 * @since 2.21.0
 */
@Slf4j
@Component
public class UserDataMigrator implements SmartLifecycle {

    private final ReactiveExtensionStoreClient extensionStoreClient;
    private final ExtensionConverter converter;
    private final MaterializedUserStoreWriter writer;
    private final SchemeManager schemeManager;
    private final UserRepository userRepository;

    private volatile boolean running;

    public UserDataMigrator(ReactiveExtensionStoreClient extensionStoreClient,
                            ExtensionConverter converter,
                            MaterializedUserStoreWriter writer,
                            SchemeManager schemeManager,
                            UserRepository userRepository) {
        this.extensionStoreClient = extensionStoreClient;
        this.converter = converter;
        this.writer = writer;
        this.schemeManager = schemeManager;
        this.userRepository = userRepository;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;

        try {
            var scheme = schemeManager.get(User.class);
            var storeNamePrefix = ExtensionStoreUtil.buildStoreNamePrefix(scheme) + "/";

            // Count rows in materialized users table
            long materializedCount = userRepository.count().blockOptional().orElse(0L);

            // Count User entries in extensions table
            long extensionCount = extensionStoreClient.countByNamePrefix(storeNamePrefix)
                .blockOptional().orElse(0L);

            // If materialized count >= extension count, skip migration
            if (materializedCount >= extensionCount) {
                log.info("User data migration skipped: materialized table has {} rows, "
                    + "extensions table has {} User entries", materializedCount, extensionCount);
                return;
            }

            log.info("Starting user data migration: {} User entries to migrate",
                extensionCount - materializedCount);

            // Migrate in batches using cursor-based pagination
            String cursor = null;
            int migratedCount = 0;
            int batchSize = 100;

            while (true) {
                var batch = extensionStoreClient.listBy(storeNamePrefix, cursor, batchSize)
                    .collectList()
                    .blockOptional()
                    .orElse(java.util.List.of());

                if (batch.isEmpty()) {
                    break;
                }

                for (var extensionStore : batch) {
                    try {
                        var user = converter.convertFrom(User.class, extensionStore);
                        writer.syncUser(user).block();
                        migratedCount++;
                    } catch (Exception e) {
                        log.error("Failed to migrate user from store: {}",
                            extensionStore.getName(), e);
                    }
                }

                // Update cursor to the last item's name for next batch
                cursor = batch.get(batch.size() - 1).getName();

                log.info("Migrated {} users so far", migratedCount);

                // If we got fewer than batchSize, we've reached the end
                if (batch.size() < batchSize) {
                    break;
                }
            }

            log.info("User data migration completed: {} users migrated", migratedCount);
        } catch (Exception e) {
            log.error("Failed to complete user data migration", e);
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // Run after SchemeInitializer (which uses InitializationPhase.SCHEME = Integer.MIN_VALUE + 100)
        // Use EXTENSION_RESOURCES phase which is next in sequence
        return InitializationPhase.EXTENSION_RESOURCES.getPhase();
    }
}
