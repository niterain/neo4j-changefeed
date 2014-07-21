/*
 * Copyright (c) 2014 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.module.changefeed;

import com.graphaware.module.changefeed.domain.ChangeSet;
import com.graphaware.module.changefeed.io.ChangeReader;
import com.graphaware.module.changefeed.io.GraphChangeReader;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.GraphAwareRuntimeFactory;
import com.graphaware.runtime.metadata.EmptyContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public class ChangeFeedPruningTest {

    private GraphDatabaseService database;
    private ChangeReader changeReader;
    private ChangeFeedModule module;

    @Before
    public void setUp() {
        database = new TestGraphDatabaseFactory().newImpermanentDatabase();
        GraphAwareRuntime runtime = GraphAwareRuntimeFactory.createRuntime(database);
        module = new ChangeFeedModule("CFM", new ChangeFeedConfiguration(10), database);
        runtime.registerModule(module);
        runtime.start();

        changeReader = new GraphChangeReader(database, "CFM");
    }

    @After
    public void tearDown() {
        database.shutdown();
    }

    @Test
    public void fetchingChangesFromASequenceThatHasBeenPrunedShouldReturnEverything() {
        //Create 20 changes
        for (int i = 1; i <= 20; i++) {
            try (Transaction tx = database.beginTx()) {
                Node node = database.createNode();
                node.setProperty("age", i);
                tx.success();
            }
        }
        Collection<ChangeSet> changes = changeReader.getAllChanges();
        assertEquals(10, changes.size());

        changes = changeReader.getChangesSince(5);
        Iterator<ChangeSet> it = changes.iterator();

        assertEquals(10, changes.size());
        assertEquals(20, it.next().getSequence());
        module.doSomeWork(new EmptyContext(), database);
        assertEquals(10, changeReader.getAllChanges().size());
        changes = changeReader.getChangesSince(5);
        it = changes.iterator();
        assertEquals(10, changes.size());
        assertEquals(20, it.next().getSequence());

    }

    @Test
    public void feedShouldBePruned() throws InterruptedException {
        //Create 10 changes
        for (int i = 1; i <= 10; i++) {
            try (Transaction tx = database.beginTx()) {
                Node node = database.createNode();
                node.setProperty("age", i);
                tx.success();
            }
        }
        //Feed should not be pruned because it has not exceeded the maxChanges by 10
        Collection<ChangeSet> changes = changeReader.getAllChanges();
        assertEquals(10, changes.size());
        Thread.sleep(6000);  //Wait for pruning to kick in
        assertEquals(10, changes.size());

        //Add 10 more changes
        for (int i = 1; i <= 10; i++) {
            try (Transaction tx = database.beginTx()) {
                Node node = database.createNode();
                node.setProperty("age", i);
                tx.success();
            }
        }
        Thread.sleep(6000);  //Wait for pruning to kick in
        changes = changeReader.getAllChanges();
        assertEquals(10, changes.size());
    }
}
