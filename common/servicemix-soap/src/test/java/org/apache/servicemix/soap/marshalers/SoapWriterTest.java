/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.soap.marshalers;

import javax.activation.DataHandler;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;

import junit.framework.TestCase;

public class SoapWriterTest extends TestCase {
			
	public void testGetContentTypeSimpleMessage() throws Exception {
		SoapWriter writer = new SoapWriter(new SoapMarshaler(), new SoapMessage());
		assertEquals("text/xml;charset=UTF-8", writer.getContentType());
	}
	
	public void testGetContentTypeSimpleMessageDefaultCharsetChanged() throws Exception {
		SourceTransformer.defaultCharset = "ISO-8859-1";
		SoapWriter writer = new SoapWriter(new SoapMarshaler(), new SoapMessage());
		assertEquals("text/xml;charset=ISO-8859-1", writer.getContentType());
		SourceTransformer.defaultCharset = "UTF-8";
	}
	
	public void testGetContentTypeComplexMessage() throws Exception {
		SoapMessage message = new SoapMessage();
		DataHandler handler = new DataHandler(new Object(), "mime/type");
		message.addAttachment("attachment", handler);
		
		SoapWriter writer = new SoapWriter(new SoapMarshaler(), message);
		assertTrue(writer.getContentType().startsWith("multipart/related; type=\"text/xml\""));		
	}
}
