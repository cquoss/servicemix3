/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

package org.apache.servicemix.beanflow;

/**
 * Represents a flow instance which is a bean based workflow written using Java code.
 * A flow monitors various {@link State} objects and takes action when things change.
 * Flows are designed to be thread safe and are intended for use in highly concurrent
 * or distributed applications so that the state can be changed from any thread.
 * <br>
 * The Processing of notifications of state changes should generally be quick; if lots of work
 * is required when some state changes it is advisable to use a thread pool to do the work.
 * 
 * @version $Revision: $
 */
public interface Flow {

    /**
     * The core transitions of a flow
     * 
     * @version $Revision: 1.1 $
     */
    public enum Transitions {
        Initialised, Starting, Started, Stopping, Stopped,
    };

    /**
     * Starts the flow
     */
    public void start();

    /**
     * Stops the flow
     */
    public void stop();

    /**
     * Returns the current running state of this flow
     */
    public State<Transitions> getState();

    /**
     * Returns true if the flow has stopped running (though it might have
     * failed)
     */
    public boolean isStopped();

    /**
     * Returns true if the flow has failed to complete succesfully
     */
    public boolean isFailed();
}