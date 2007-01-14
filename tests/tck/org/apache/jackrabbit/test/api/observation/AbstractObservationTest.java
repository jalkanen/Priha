/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.api.observation;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.observation.EventListenerIterator;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * This class implements the basic {@link #setUp} and {@link #tearDown()}
 * methods for the observation test cases.
 */
public abstract class AbstractObservationTest extends AbstractJCRTest {

    /**
     * Default wait timeout for events: 5000 ms
     */
    protected static final long DEFAULT_WAIT_TIMEOUT = 5000;

    /**
     * The <code>ObservationManager</code>
     */
    protected ObservationManager obsMgr;

    protected void setUp() throws Exception {
        super.setUp();
        obsMgr = superuser.getWorkspace().getObservationManager();
    }

    /**
     * Registers an <code>EventListener</code> for all events.
     *
     * @param listener the <code>EventListener</code>.
     * @throws RepositoryException if registration fails.
     */
    protected void addEventListener(EventListener listener) throws RepositoryException {
        addEventListener(listener,
                Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED);
    }

    /**
     * Registers an <code>EventListener</code> for events of the specified
     * type(s).
     *
     * @param listener  the <code>EventListener</code>.
     * @param eventType the event types
     * @throws RepositoryException if registration fails.
     */
    protected void addEventListener(EventListener listener, int eventType)
            throws RepositoryException {
        if (obsMgr != null) {
            obsMgr.addEventListener(listener,
                    eventType,
                    superuser.getRootNode().getPath(),
                    true,
                    null,
                    null,
                    false);
        } else {
            throw new IllegalStateException("ObservationManager not available.");
        }
    }

    /**
     * Removes the <code>EventListener</code> from the ObservationManager.
     *
     * @param listener the <code>EventListener</code> to unregister.
     * @throws RepositoryException if unregister fails.
     */
    protected void removeEventListener(EventListener listener) throws RepositoryException {
        if (obsMgr != null) {
            obsMgr.removeEventListener(listener);
        } else {
            throw new IllegalStateException("ObservationManager not available.");
        }
    }

    /**
     * Consumes the <code>EventListenerIterator</code> and returns the
     * <code>EventListener</code> as an array.
     * @param it the iterator.
     * @return array of <code>EventListeners</code>.
     */
    protected EventListener[] toArray(EventListenerIterator it) {
        List listeners = new ArrayList();
        while (it.hasNext()) {
            listeners.add(it.nextEventListener());
        }
        return (EventListener[]) listeners.toArray(new EventListener[listeners.size()]);
    }

    //--------------------< check methods >-------------------------------------

    /**
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #testRoot}.
     *
     * @param events   the <code>Event</code>s.
     * @param relPaths paths to child nodes added relative to {@link
     *                 #testRoot}.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    protected void checkNodeAdded(Event[] events, String[] relPaths)
            throws RepositoryException {
        checkNodes(events, relPaths, Event.NODE_ADDED);
    }

    /**
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #testRoot}.
     *
     * @param events   the <code>Event</code>s.
     * @param relPaths paths to child nodes added relative to {@link
     *                 #testRoot}.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    protected void checkNodeRemoved(Event[] events, String[] relPaths)
            throws RepositoryException {
        checkNodes(events, relPaths, Event.NODE_REMOVED);
    }

    /**
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #testRoot}.
     *
     * @param events   the <code>Event</code>s.
     * @param relPaths paths to added properties relative to {@link
     *                 #testRoot}.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    protected void checkPropertyAdded(Event[] events, String[] relPaths)
            throws RepositoryException {
        checkNodes(events, relPaths, Event.PROPERTY_ADDED);
    }

    /**
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #testRoot}.
     *
     * @param events   the <code>Event</code>s.
     * @param relPaths paths to changed properties relative to {@link
     *                 #testRoot}.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    protected void checkPropertyChanged(Event[] events, String[] relPaths)
            throws RepositoryException {
        checkNodes(events, relPaths, Event.PROPERTY_CHANGED);
    }

    /**
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #testRoot}.
     *
     * @param events   the <code>Event</code>s.
     * @param relPaths paths to removed properties relative to {@link
     *                 #testRoot}.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    protected void checkPropertyRemoved(Event[] events, String[] relPaths)
            throws RepositoryException {
        checkNodes(events, relPaths, Event.PROPERTY_REMOVED);
    }

    /**
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #testRoot}.
     *
     * @param events    the <code>Event</code>s.
     * @param relPaths  paths to item events relative to {@link #testRoot}.
     * @param eventType the type of event to check.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    private void checkNodes(Event[] events, String[] relPaths, long eventType)
            throws RepositoryException {
        assertEquals("Number of events wrong", relPaths.length, events.length);
        Set paths = new HashSet();
        for (int i = 0; i < events.length; i++) {
            assertEquals("Wrong event type", eventType, events[i].getType());
            String path = events[i].getPath();
            paths.add(path);
        }
        for (int i = 0; i < relPaths.length; i++) {
            String expected = testRoot + "/" + relPaths[i];
            assertTrue("Path " + expected + " not found in events.",
                    paths.contains(expected));
        }
    }
}
