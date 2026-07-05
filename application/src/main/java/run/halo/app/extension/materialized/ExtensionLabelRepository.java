package run.halo.app.extension.materialized;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * R2DBC repository for the {@code extension_labels} table.
 *
 * @author halo
 * @since 2.21.0
 */
@Repository
public interface ExtensionLabelRepository extends R2dbcRepository<ExtensionLabel, ExtensionLabelId>,
    ExtensionLabelRepositoryCustom {

    @Query("SELECT extension_name FROM extension_labels "
        + "WHERE label_key = :labelKey AND label_value = :labelValue")
    Flux<String> findExtensionNamesByLabelKeyAndLabelValue(
        @Param("labelKey") String labelKey,
        @Param("labelValue") String labelValue
    );

    @Query("SELECT extension_name FROM extension_labels WHERE label_key = :labelKey")
    Flux<String> findExtensionNamesByLabelKey(@Param("labelKey") String labelKey);

    @Query("DELETE FROM extension_labels WHERE extension_name = :extensionName")
    Mono<Void> deleteByExtensionName(@Param("extensionName") String extensionName);
}
