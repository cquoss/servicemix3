/** 
 * 
 * Copyright 2005 Datasul B2B, Inc. http://www.neogrid.com.br
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.servicemix.components.varscheduler;

import java.util.Date;

/**
 * Iterator for scheduling. 
 * @author george
 *
 */
public interface ScheduleIterator {
	/**
	 * Next execution date of associated task. 
	 * Implementations should return null to cancel running task.
	 *  
	 * @return next date of Execution
	 */
	Date nextExecution();
}
