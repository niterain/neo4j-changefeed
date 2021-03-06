package com.graphaware.module.changefeed.cache;

import com.graphaware.module.changefeed.domain.ChangeSet;
import com.graphaware.module.changefeed.io.GraphChangeWriter;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * {@link GraphChangeWriter} that also pushes all written changes to {@link ChangeSetCache}.
 */
public class CachingGraphChangeWriter extends GraphChangeWriter {

    private final ChangeSetCache cache;

    /**
     * Construct a new writer.
     *
     * @param database in which to store the changes.
     * @param moduleId ID of the module storing changes.
     * @param cache    for caching changes.
     */
    public CachingGraphChangeWriter(GraphDatabaseService database, String moduleId, ChangeSetCache cache) {
        super(database, moduleId);
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void recordChanges(ChangeSet changeSet) {
        super.recordChanges(changeSet);
        cache.push(changeSet);
    }
}
