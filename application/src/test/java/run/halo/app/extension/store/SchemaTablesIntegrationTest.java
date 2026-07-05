package run.halo.app.extension.store;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.test.StepVerifier;

/**
 * Integration test to verify that the schema tables defined in schema-h2.sql are created correctly at application
 * startup.
 *
 * @author halo
 * @since 2.21.0
 */
@SpringBootTest
class SchemaTablesIntegrationTest {

    @Autowired
    R2dbcEntityTemplate entityTemplate;

    @Test
    void shouldContainUsersTable() {
        assertTableExists("USERS");
    }

    @Test
    void shouldContainExtensionLabelsTable() {
        assertTableExists("EXTENSION_LABELS");
    }

    @Test
    void shouldContainUserRolesTable() {
        assertTableExists("USER_ROLES");
    }

    @Test
    void shouldContainExtensionsTable() {
        assertTableExists("EXTENSIONS");
    }

    @Test
    void shouldBeAbleToInsertAndQueryUsersTable() {
        entityTemplate
                .getDatabaseClient()
                .sql("INSERT INTO users (name, display_name, email, email_verified, disabled, "
                        + "creation_timestamp) VALUES ('test-user', 'Test User', 'test@example.com', "
                        + "FALSE, FALSE, CURRENT_TIMESTAMP)")
                .fetch()
                .rowsUpdated()
                .as(StepVerifier::create)
                .expectNext(1L)
                .verifyComplete();

        entityTemplate
                .getDatabaseClient()
                .sql("SELECT name, display_name, email FROM users WHERE name = 'test-user'")
                .map((row, metadata) -> row.get("name", String.class))
                .one()
                .as(StepVerifier::create)
                .expectNext("test-user")
                .verifyComplete();
    }

    @Test
    void shouldBeAbleToInsertAndQueryExtensionLabelsTable() {
        entityTemplate
                .getDatabaseClient()
                .sql("INSERT INTO extension_labels (extension_name, label_key, label_value) "
                        + "VALUES ('/registry/v1alpha1/users/admin', 'role', 'admin')")
                .fetch()
                .rowsUpdated()
                .as(StepVerifier::create)
                .expectNext(1L)
                .verifyComplete();

        entityTemplate
                .getDatabaseClient()
                .sql("SELECT label_value FROM extension_labels "
                        + "WHERE extension_name = '/registry/v1alpha1/users/admin' "
                        + "AND label_key = 'role'")
                .map((row, metadata) -> row.get("label_value", String.class))
                .one()
                .as(StepVerifier::create)
                .expectNext("admin")
                .verifyComplete();
    }

    @Test
    void shouldBeAbleToInsertAndQueryUserRolesTable() {
        entityTemplate
                .getDatabaseClient()
                .sql("INSERT INTO user_roles (store_name, role_name) "
                        + "VALUES ('/registry/v1alpha1/users/admin', 'super-role')")
                .fetch()
                .rowsUpdated()
                .as(StepVerifier::create)
                .expectNext(1L)
                .verifyComplete();

        entityTemplate
                .getDatabaseClient()
                .sql("SELECT role_name FROM user_roles " + "WHERE store_name = '/registry/v1alpha1/users/admin'")
                .map((row, metadata) -> row.get("role_name", String.class))
                .one()
                .as(StepVerifier::create)
                .expectNext("super-role")
                .verifyComplete();
    }

    private void assertTableExists(String tableName) {
        entityTemplate
                .getDatabaseClient()
                .sql("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = :tableName")
                .bind("tableName", tableName)
                .map((row, metadata) -> row.get("TABLE_NAME", String.class))
                .one()
                .as(StepVerifier::create)
                .expectNext(tableName)
                .verifyComplete();
    }
}
