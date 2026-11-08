package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * P10 / 2.3: index for the log context view ({@code GET /logs/{id}/context}): the entries just
 * before and after one entry of the same source and pod, ordered by (timestamp, id).
 *
 * <p>Built without blocking ingest. {@code CREATE INDEX CONCURRENTLY} is not supported on a
 * partitioned table, so (as the PostgreSQL docs describe): the parent index is created
 * {@code ON ONLY} log_entries (invalid, metadata only), each partition's index is built
 * {@code CONCURRENTLY} and attached; the parent becomes valid when every partition is attached.
 * Partitions created meanwhile (LogPartitionMaintenance) get the index from the parent. Not in a
 * transaction (CONCURRENTLY cannot run in one), and re-runnable: an invalid leftover of an
 * interrupted build is dropped and rebuilt.
 *
 * <p>Rollback: {@code DROP INDEX idx_log_entries_context;} (drops the partitions' indexes too).
 */
public class V26__log_entries_context_index extends BaseJavaMigration {
    static final String PARENT = "idx_log_entries_context";
    static final String COLUMNS = "(source_id, pod_name, timestamp, id)";

    @Override
    public boolean canExecuteInTransaction() {
        return false;
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX IF NOT EXISTS " + PARENT + " ON ONLY log_entries " + COLUMNS);
            for (String partition : partitions(statement)) {
                String index = indexName(partition);
                Boolean valid = validity(statement, index);
                if (Boolean.FALSE.equals(valid)) {
                    statement.execute("DROP INDEX CONCURRENTLY IF EXISTS " + index);
                }
                if (!Boolean.TRUE.equals(valid)) {
                    statement.execute("CREATE INDEX CONCURRENTLY IF NOT EXISTS " + index + " ON " + partition + " " + COLUMNS);
                }
                if (!attached(statement, index)) {
                    statement.execute("ALTER INDEX " + PARENT + " ATTACH PARTITION " + index);
                }
            }
            // Statistics for the new index columns (pod_name was not indexed before).
            statement.execute("ANALYZE log_entries");
        }
    }

    /** log_entries_20261109 -> log_entries_20261109_context_idx (within the 63-byte limit). */
    static String indexName(String partition) {
        return partition + "_context_idx";
    }

    private static List<String> partitions(Statement statement) throws Exception {
        List<String> names = new ArrayList<>();
        try (ResultSet rs = statement.executeQuery("""
                SELECT c.relname FROM pg_inherits i JOIN pg_class c ON c.oid = i.inhrelid
                 WHERE i.inhparent = 'log_entries'::regclass ORDER BY c.relname
                """)) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        return names;
    }

    /** True / false when the index exists (valid or not), null when it does not. */
    private static Boolean validity(Statement statement, String index) throws Exception {
        try (ResultSet rs = statement.executeQuery("""
                SELECT x.indisvalid FROM pg_index x JOIN pg_class c ON c.oid = x.indexrelid
                 WHERE c.relname = '%s'
                """.formatted(index))) {
            return rs.next() ? rs.getBoolean(1) : null;
        }
    }

    private static boolean attached(Statement statement, String index) throws Exception {
        try (ResultSet rs = statement.executeQuery("""
                SELECT 1 FROM pg_inherits i JOIN pg_class c ON c.oid = i.inhrelid
                 WHERE c.relname = '%s' AND i.inhparent = '%s'::regclass
                """.formatted(index, PARENT))) {
            return rs.next();
        }
    }
}
