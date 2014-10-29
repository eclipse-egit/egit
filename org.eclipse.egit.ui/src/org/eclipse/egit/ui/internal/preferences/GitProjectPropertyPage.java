/*******************************************************************************
 * Copyright (C) 2008, 2011 Tomi Pakarinen <tomi.pakarinen@iki.fi> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Benjamin Muskalla (Tasktop Technologies Inc) - Hyperlinking of HEAD
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.attributes.Attribute;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.TreeWalk.OperationType;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

/**
 * Property page to be shown in project properties, if project is shared using
 * git provider. Currently there aren't any modifiable element.
 */
public class GitProjectPropertyPage extends PropertyPage {

	private Text gitDir;

	private Text branch;

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
		layout.verticalSpacing = 5;
		composite.setLayout(layout);

		gitDir = createLabeledReadOnlyText(composite,
				UIText.GitProjectPropertyPage_LabelGitDir);
		workDir = createLabeledReadOnlyText(composite,
				UIText.GitProjectPropertyPage_LabelWorkdir);
		branch = createLabeledReadOnlyText(composite,
				UIText.GitProjectPropertyPage_LabelBranch);
		state = createLabeledReadOnlyText(composite,
				UIText.GitProjectPropertyPage_LabelState);

		// Get the project that is the source of this property page
		IProject project = null;
		IResource resource = null;
		final IAdaptable element = getElement();
		if (element instanceof IResource) {
			resource = ((IResource) element);
			project = resource.getProject();
		} else {
			Object adapter = element.getAdapter(IResource.class);
			if (adapter instanceof IResource) {
				resource = ((IResource) element);
				project = resource.getProject();
			}
		}
		RepositoryMapping repoMapping = RepositoryMapping.getMapping(project);
		Repository repository = repoMapping.getRepository();

		if (repository != null) {
			try {
				createHeadLink(repository, composite);
				fillValues(repository);
				if (resource != null) {
					String path = repoMapping.getRepoRelativePath(resource
							.getLocation());
					if (path != null)
						createAttributesTables(composite, repository, path);

				}
			} catch (IOException e) {
				if (GitTraceLocation.UI.isActive())
					GitTraceLocation.getTrace().trace(GitTraceLocation.UI.getLocation(), e.getMessage(), e);
			}
		}

		return composite;
	}

	private void createAttributesTables(Composite parent, Repository repository,
			String string) throws NoWorkTreeException, IOException {
		Group holdingGroups = new Group(parent, SWT.TOP);
		holdingGroups.setText(UIText.GitProjectPropertyPage_GroupAttributes);
		GridData layoutData = new GridData(GridData.FILL,
				GridData.VERTICAL_ALIGN_BEGINNING, true, false);
		layoutData.horizontalSpan = 2;
		holdingGroups.setLayoutData(layoutData);
		GridLayout layout = new GridLayout(1, true);
		holdingGroups.setLayout(layout);

		// Looks for attributes
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(new FileTreeIterator(repository));
		treeWalk.addTree(new DirCacheIterator(repository.readDirCache()));
		treeWalk.setFilter(PathFilter.create(string));
		treeWalk.setRecursive(true);

		final Set<Attribute> checkinAttributes;
		final Set<Attribute> checkoutAttributes;
		if (treeWalk.next()) {
			checkinAttributes = new HashSet<Attribute>(
					treeWalk.getAttributes(OperationType.CHECKIN_OP));
			checkoutAttributes = new HashSet<Attribute>(
					treeWalk.getAttributes(OperationType.CHECKOUT_OP));
		} else {
			checkinAttributes = Collections.emptySet();
			checkoutAttributes = Collections.emptySet();
		}

		if (checkinAttributes.equals(checkoutAttributes)) {
			if (!checkinAttributes.isEmpty()) {
				// Creates only one table since the attributes are the same for
				// checkin or checkout
				createTable(holdingGroups, checkinAttributes, null);
			} else {
				// Does not create any table if there is no attributes to
				// display
				createLabeledReadOnlyText(holdingGroups,
						UIText.GitProjectPropertyPage_LabelNone);
			}
		} else {
			// Create two different table for checkin and checkout attributes
			createTable(holdingGroups, checkinAttributes,
					UIText.GitProjectPropertyPage_LabelCheckinAttribute);
			createTable(holdingGroups, checkoutAttributes,
					UIText.GitProjectPropertyPage_LabelCheckoutAttribute);
		}
	}

	private void createTable(Composite parent, final Set<Attribute> attrs,
			String labelValue) {
		if (labelValue != null) {
			Label label = new Label(parent, SWT.NONE);
			label.setText(labelValue);
			label.setLayoutData(new GridData(
					GridData.HORIZONTAL_ALIGN_BEGINNING,
					GridData.VERTICAL_ALIGN_BEGINNING, false, false));
		}
		final ListViewer attributeViewer = new ListViewer(parent, SWT.NO_SCROLL
				| SWT.HIDE_SELECTION | SWT.NO_FOCUS);
		attributeViewer.getList().setLayoutData(
				new GridData(GridData.FILL, GridData.VERTICAL_ALIGN_BEGINNING,
						true, true));
		attributeViewer.setContentProvider(new ArrayContentProvider());
		// Prevents selection in the viewer
		attributeViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						if (!event.getSelection().isEmpty()) {
							attributeViewer
									.setSelection(StructuredSelection.EMPTY);
						}
					}
				});

		attributeViewer.setInput(attrs);
	}

	private void createHeadLink(final Repository repository, Composite composite) throws IOException {
		final ObjectId objectId = repository
				.resolve(repository.getFullBranch());
		if (objectId == null) {
			Text headLabel = createLabeledReadOnlyText(composite, UIText.GitProjectPropertyPage_LabelId);
			if (repository.getAllRefs().size() == 0)
				headLabel.setText(UIText.GitProjectPropertyPage_ValueEmptyRepository);
			else
				headLabel.setText(UIText.GitProjectPropertyPage_ValueUnbornBranch);
		} else {
			Hyperlink headLink = createHeadHyperLink(composite, UIText.GitProjectPropertyPage_LabelId);
			headLink.setText(objectId.name());
			headLink.setUnderlined(true);
			headLink.setFont(JFaceResources.getDialogFont());
			headLink.setForeground(JFaceColors.getHyperlinkText(headLink
					.getDisplay()));
			headLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					RepositoryCommit commit = getCommit(repository, objectId);
					if(commit != null)
						CommitEditor.openQuiet(commit);
				}
			});
		}
	}

	private void fillValues(final Repository repository) throws IOException {
		gitDir.setText(repository.getDirectory().getAbsolutePath());
		branch.setText(repository.getBranch());
		workDir.setText(repository.getWorkTree().getAbsolutePath());

		state.setText(repository.getRepositoryState().getDescription());
	}

	private RepositoryCommit getCommit(Repository repository, ObjectId objectId) {
		RevWalk walk = new RevWalk(repository);
		try {
			RevCommit commit = walk.parseCommit(objectId);
			for (RevCommit parent : commit.getParents())
				walk.parseBody(parent);
			return new RepositoryCommit(repository, commit);
		} catch (IOException e) {
			Activator.showError(NLS.bind(
					UIText.GitProjectPropertyPage_UnableToGetCommit,
					objectId.name()), e);
		} finally {
			walk.release();
		}
		return null;
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
	private Text createLabeledReadOnlyText(Composite parent,
			final String labelText) {
		createLabel(parent, labelText);

		Text text = createText(parent);
		return text;
	}

	private Hyperlink createHeadHyperLink(Composite composite,
			String labelText) {
		createLabel(composite, labelText);

		Hyperlink hyperlink = new Hyperlink(composite, SWT.NONE);
		hyperlink.setLayoutData(GridDataFactory.fillDefaults().create());
		return hyperlink;
	}

	private Text createText(Composite parent) {
		GridData data = new GridData();
		Text text = UIUtils.createSelectableLabel(parent, SWT.LEFT);
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		text.setLayoutData(data);
		return text;
	}

	private void createLabel(Composite parent, final String labelText) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(labelText);
		GridData data = new GridData();
		data.horizontalSpan = 1;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
	}

}
