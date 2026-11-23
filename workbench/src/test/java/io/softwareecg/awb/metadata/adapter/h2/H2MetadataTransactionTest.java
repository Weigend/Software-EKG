package io.softwareecg.awb.metadata.adapter.h2;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class H2MetadataTransactionTest {

    @Test
    void commits_all_connections_used_inside_the_transaction() throws Exception {
        H2MetadataDataSource dataSource = source();

        dataSource.inTransaction(() -> {
            execute(dataSource, "INSERT INTO transaction_probe VALUES (1)");
            execute(dataSource, "INSERT INTO transaction_probe VALUES (2)");
            return null;
        });

        assertEquals(2, count(dataSource));
    }

    @Test
    void rolls_back_all_connections_used_inside_the_transaction() throws Exception {
        H2MetadataDataSource dataSource = source();

        assertThrows(IllegalStateException.class, () -> dataSource.inTransaction(() -> {
            execute(dataSource, "INSERT INTO transaction_probe VALUES (1)");
            throw new IllegalStateException("abort");
        }));

        assertEquals(0, count(dataSource));
    }

    private static H2MetadataDataSource source() throws Exception {
        H2MetadataDataSource source = new H2MetadataDataSource(
                "jdbc:h2:mem:metadata-transaction-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        execute(source, "CREATE TABLE transaction_probe (id INT PRIMARY KEY)");
        return source;
    }

    private static void execute(H2MetadataDataSource source, String sql) {
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static int count(H2MetadataDataSource source) throws Exception {
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM transaction_probe")) {
            result.next();
            return result.getInt(1);
        }
    }
}
