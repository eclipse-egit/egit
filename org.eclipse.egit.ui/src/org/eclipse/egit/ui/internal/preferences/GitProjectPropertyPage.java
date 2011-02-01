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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Property page to be shown in project properties, if project is shared using
 * git provider.
 */
public class GitProjectPropertyPage extends PropertyPage {

	private static final String GERRIT = "gerrit"; //$NON-NLS-1$
	private static final String ADD_CHANGE_ID = "addChangeId"; //$NON-NLS-1$

	private Text gitDir;

	private Text branch;

	private Text id;

	private Text state;

	private Text workDir;

	private Button changeIdButton;

	@Override
	protected Control createContents(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).create());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite repoInfo = new Composite(composite, SWT.NULL);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		repoInfo.setLayout(layout);

		gitDir = createLabeledReadOnlyText(repoInfo, UIText.GitProjectPropertyPage_LabelGitDir);
		workDir = createLabeledReadOnlyText(repoInfo, UIText.GitProjectPropertyPage_LabelWorkdir);
		branch = createLabeledReadOnlyText(repoInfo, UIText.GitProjectPropertyPage_LabelBranch);
		id = createLabeledReadOnlyText(repoInfo, UIText.GitProjectPropertyPage_LabelId);
		state = createLabeledReadOnlyText(repoInfo, UIText.GitProjectPropertyPage_LabelState);

		Repository repository = getRepository();

		if (repository != null) {
			try {
				fillValues(repository);
			} catch (IOException e) {
				if (GitTraceLocation.UI.isActive())
					GitTraceLocation.getTrace().trace(GitTraceLocation.UI.getLocation(), e.getMessage(), e);
			}
		}

		Group repoConfigGroup = new Group(composite, SWT.NONE);
		repoConfigGroup.setText(UIText.GitProjectPropertyPage_RepositoryConfigurationGroup);
		repoConfigGroup.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		repoConfigGroup.setLayout(new GridLayout());

		changeIdButton = new Button(repoConfigGroup, SWT.CHECK);
		changeIdButton.setText(UIText.GitProjectPropertyPage_AddChangeIdLabel);
		changeIdButton.setToolTipText(UIText.GitProjectPropertyPage_AddChangeIdTooltip);
		if (repository != null) {
			StoredConfig config = repository.getConfig();
			boolean addChangeId = config.getBoolean(GERRIT, ADD_CHANGE_ID, false);
			changeIdButton.setSelection(addChangeId);
		}

		return composite;
	}

	@Override
	public boolean performOk() {
		Repository repository = getRepository();
		if (repository != null) {
			StoredConfig config = repository.getConfig();
			boolean addChangeId = changeIdButton.getSelection();
			if (addChangeId) {
				config.setBoolean(GERRIT, null, ADD_CHANGE_ID, true);
			} else {
				config.unset(GERRIT, null, ADD_CHANGE_ID);
				if (config.getNames(GERRIT).isEmpty()) {
					config.unsetSection(GERRIT, null);
				}
			}
			try {
				config.save();
			} catch (IOException e) {
				if (GitTraceLocation.UI.isActive()) {
					GitTraceLocation.getTrace().trace(GitTraceLocation.UI.getLocation(), e.getMessage(), e);
				}
			}
		}
		return true;
	}

	/**
	 * Get the project that is the source of this property page.
	 *
	 * @return the repository
	 */
	private Repository getRepository() {
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
		return repository;
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
