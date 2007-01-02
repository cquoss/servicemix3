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
package org.eclipse.jst.jbi.ui.project.facet;

import org.eclipse.jst.j2ee.internal.plugin.J2EEUIMessages;
import org.eclipse.jst.j2ee.internal.wizard.J2EEModuleFacetInstallPage;
import org.eclipse.jst.jbi.internal.project.operations.IJbiFacetInstallDataModelProperties;
import org.eclipse.jst.jbi.ui.internal.util.JBIUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class JbiFacetInstallPage extends J2EEModuleFacetInstallPage implements
		IJbiFacetInstallDataModelProperties {

	private static final String MODULE_NAME_UI = J2EEUIMessages
			.getResourceString(J2EEUIMessages.NAME_LABEL); //$NON-NLS-1$

	private Text configFolder;

	private Label configFolderLabel;

	private Label sourceFolderLabel;

	private Text sourceFolder;

	public JbiFacetInstallPage() {
		super("jbi.facet.install.page");
		setTitle(JBIUIMessages.JBI_INSTALL_TITLE);

		setDescription(JBIUIMessages.JBI_INSTALL_DESCRIPTION);
	}

	protected String[] getValidationPropertyNames() {
		return new String[] { EAR_PROJECT_NAME };
	}

	protected Composite createTopLevelComposite(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		this.configFolderLabel = new Label(composite, SWT.NONE);
		this.configFolderLabel.setText("Resources Folder");
		this.configFolderLabel.setLayoutData(gdhfill());

		this.configFolder = new Text(composite, SWT.BORDER);
		this.configFolder.setLayoutData(gdhfill());
		this.configFolder.setData("label", this.configFolderLabel);

		this.sourceFolderLabel = new Label(composite, SWT.NONE);
		this.sourceFolderLabel.setText("Source Folder");
		this.sourceFolderLabel.setLayoutData(gdhfill());

		this.sourceFolder = new Text(composite, SWT.BORDER);
		this.sourceFolder.setLayoutData(gdhfill());
		this.sourceFolder.setData("label", this.configFolderLabel);

		//$NON-NLS-1$
		synchHelper.synchText(configFolder, CONFIG_FOLDER, null);
		synchHelper.synchText(sourceFolder, JAVA_SOURCE_FOLDER, null);

		return composite;
	}
}
