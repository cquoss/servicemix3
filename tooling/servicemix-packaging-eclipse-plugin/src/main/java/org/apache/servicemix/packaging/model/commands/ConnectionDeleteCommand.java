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
package org.apache.servicemix.packaging.model.commands;

import org.apache.servicemix.packaging.model.ComponentConnection;
import org.eclipse.gef.commands.Command;

/**
 * The GEF command for deleting a connection
 * 
 * @author <a href="mailto:philip.dodds@gmail.com">Philip Dodds </a>
 * 
 */
public class ConnectionDeleteCommand extends Command {

	private final ComponentConnection connection;

	public ConnectionDeleteCommand(ComponentConnection conn) {
		if (conn == null) {
			throw new IllegalArgumentException();
		}
		setLabel("connection deletion");
		this.connection = conn;
	}

	public void execute() {
		connection.disconnect();
	}

	public void undo() {
		connection.reconnect();
	}
}
