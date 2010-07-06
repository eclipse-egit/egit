/*******************************************************************************
 * Copyright (C) 2008, Tomi Pakarinen <tomi.pakarinen@iki.fi>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * Property page to be shown in project properties, if project is shared using
 * git provider. Currently there aren't any modifiable element.
 */
public class GitProjectPropertyPage extends PropertyPage {

	private Text gitDir;

	private Text branch;

	private Text id;

	private Text state;

	private Text workDir;

	@Override
	protected Control createContents(Composite parent) {
		// this page just shows read-only information to the user, no
		// default/apply buttons needed
		noDefaultAndApplyButton();

		final Composite composite = new Composite(parent, SWT.NULL);

		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		composite.setLayout(layout);

		gitDir = createLabeledReadOnlyText(composite, UIText.GitProjectPropertyPage_LabelGitDir);
		workDir = createLabeledReadOnlyText(composite, UIText.GitProjectPropertyPage_LabelWorkdir);
		branch = createLabeledReadOnlyText(composite, UIText.GitProjectPropertyPage_LabelBranch);
		id = createLabeledReadOnlyText(composite, UIText.GitProjectPropertyPage_LabelId);
		state = createLabeledReadOnlyText(composite, UIText.GitProjectPropertyPage_LabelState);

		// Get the project that is the source of this property page
		IProject project = null;
		final IAdaptable element = getElement();
		if (element instanceof IProject) {
			project = (IProject) element;
		} else {
			Object adapter = element.getAdapter(IProject.class);
			if (adapter instanceof IProject) {
				project = (IProject) adapter;
			}
		}

		Repository repository = RepositoryMapping.getMapping(project)
				.getRepository();

		if (repository != null) {
			try {
				fillValues(repository);
			} catch (IOException e) {
				if (GitTraceLocation.UI.isActive())
					GitTraceLocation.getTrace().trace(GitTraceLocation.UI.getLocation(), e.getMessage(), e);
			}
		}

		return composite;
	}

	private void fillValues(Repository repository) throws IOException {
		gitDir.setText(repository.getDirectory().getAbsolutePath());
		branch.setText(repository.getBranch());
		workDir.setText(repository.getWorkTree().getAbsolutePath());

		state.setText(repository.getRepositoryState().getDescription());

		final ObjectId objectId = repository
				.resolve(repository.getFullBranch());
		if (objectId == null) {
			if (repository.getAllRefs().size() == 0)
				id.setText(UIText.GitProjectPropertyPage_ValueEmptyRepository);
			else
				id.setText(UIText.GitProjectPropertyPage_ValueUnbornBranch);
		} else
			id.setText(objectId.name());
	}

	/**
	 * Create a read only text field with a label
	 *
	 * @param parent
	 *            the parent composite for new widgets
	 * @param labelText
	 *            text for label
	 * @return the new read only text field
	 */
	protected Text createLabeledReadOnlyText(Composite parent,
			final String labelText) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(labelText);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);

		Text text = new Text(parent, SWT.LEFT | SWT.READ_ONLY);
		text.setBackground(Display.getDefault().getSystemColor(
				SWT.COLOR_WIDGET_BACKGROUND));
		data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		text.setLayoutData(data);
		return text;
	}

}
