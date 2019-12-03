/*******************************************************************************
 * Copyright (c) 2013 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.CachedCheckboxTreeViewer;
import org.eclipse.egit.ui.internal.components.FilteredCheckboxTree;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo.SelectionType;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewContentProvider;
import org.eclipse.egit.ui.internal.repository.RepositoryTreeNodeLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.repository.tree.TagsNode;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Tag to select a remote and one or more tags to push.
 */
public class PushTagsPage extends WizardPage {

	private final Repository repository;

	private final Set<String> tagRefNamesToSelect = new HashSet<>();

	private RemoteConfig selectedRemoteConfig = null;

	private List<TagNode> selectedTags = new ArrayList<>();

	private boolean forceUpdateSelected = false;

	private Label tagsLabel;

	/**
	 * @param repository
	 * @param tagNamesToSelect
	 */
	public PushTagsPage(Repository repository,
			Collection<String> tagNamesToSelect) {
		super(UIText.PushTagsPage_PageName);
		setTitle(UIText.PushTagsPage_PageTitle);
		setMessage(UIText.PushTagsPage_PageMessage);

		this.repository = repository;
		for (String tagName : tagNamesToSelect) {
			if (tagName.startsWith(Constants.R_TAGS))
				tagRefNamesToSelect.add(tagName);
			else
				tagRefNamesToSelect.add(Constants.R_TAGS + tagName);
		}
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(GridLayoutFactory.swtDefaults()
				.spacing(LayoutConstants.getSpacing()).numColumns(2).create());

		Label remoteLabel = new Label(main, SWT.NONE);
		remoteLabel.setText(UIText.PushTagsPage_RemoteLabel);

		RemoteSelectionCombo remoteSelectionCombo = new RemoteSelectionCombo(
				main, SWT.NONE, SelectionType.PUSH);
		remoteSelectionCombo.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());
		selectedRemoteConfig = remoteSelectionCombo
				.setItems(getRemoteConfigs());
		remoteSelectionCombo.addRemoteSelectionListener(rc -> {
			selectedRemoteConfig = rc;
		});

		tagsLabel = new Label(main, SWT.NONE);
		tagsLabel.setText(UIText.PushTagsPage_TagsLabelNoneSelected);
		tagsLabel.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).span(2, 1).create());

		FilteredCheckboxTree tree = new FilteredCheckboxTree(main, null,
				SWT.BORDER);
		tree.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.span(2, 1).hint(400, 300).create());

		final Button forceUpdateButton = new Button(main, SWT.CHECK);
		forceUpdateButton
				.setText(UIText.PushTagsPage_ForceUpdateButton);
		forceUpdateButton.setSelection(false);
		forceUpdateButton.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).span(2, 1).create());
		forceUpdateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				forceUpdateSelected = forceUpdateButton.getSelection();
			}
		});

		final CachedCheckboxTreeViewer treeViewer = tree
				.getCheckboxTreeViewer();
		TagsNode tagsNode = new TagsNode(null, repository);
		ContentProvider contentProvider = new ContentProvider(tagsNode);
		treeViewer.setContentProvider(contentProvider);
		treeViewer.setLabelProvider(new RepositoryTreeNodeLabelProvider(true));
		treeViewer.setComparator(new ViewerComparator(
				CommonUtils.STRING_ASCENDING_COMPARATOR));
		treeViewer.setInput(tagsNode);

		final Object[] tagNodes = contentProvider.getElements(tagsNode);
		initiallySelectTags(tagNodes, treeViewer);

		treeViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				setSelectedTags(treeViewer.getCheckedElements());
			}
		});

		setControl(main);
	}

	@Override
	public boolean isPageComplete() {
		return selectedRemoteConfig != null && !selectedTags.isEmpty();
	}

	RemoteConfig getSelectedRemoteConfig() {
		return selectedRemoteConfig;
	}

	List<TagNode> getSelectedTags() {
		return selectedTags;
	}

	boolean isForceUpdateSelected() {
		return forceUpdateSelected;
	}

	private void initiallySelectTags(Object[] tagNodes,
			CheckboxTreeViewer viewer) {
		List<TagNode> checkedTags = new ArrayList<>();
		for (Object node : tagNodes) {
			if (node instanceof TagNode) {
				TagNode tagNode = (TagNode) node;
				Ref ref = tagNode.getObject();
				if (tagRefNamesToSelect.contains(ref.getName()))
					checkedTags.add(tagNode);
			}
		}

		TagNode[] checkedTagsArray = checkedTags
				.toArray(new TagNode[0]);
		viewer.setCheckedElements(checkedTagsArray);
		if (checkedTagsArray.length > 0) {
			// Reveal tags (just using reveal does not work on some platforms)
			viewer.setSelection(new StructuredSelection(checkedTagsArray), true);
			// Clear selection, we don't want to highlight the rows that much
			viewer.setSelection(StructuredSelection.EMPTY);
		}
		setSelectedTags(checkedTagsArray);
	}

	private void setSelectedTags(Object[] tags) {
		selectedTags.clear();
		for (Object tag : tags) {
			if (tag instanceof TagNode)
				selectedTags.add((TagNode) tag);
		}
		int number = selectedTags.size();
		if (number == 0)
			tagsLabel.setText(UIText.PushTagsPage_TagsLabelNoneSelected);
		else
			tagsLabel.setText(MessageFormat.format(UIText.PushTagsPage_TagsLabelSelected,
					Integer.valueOf(selectedTags.size())));
		setPageComplete(isPageComplete());
	}

	private List<RemoteConfig> getRemoteConfigs() {
		try {
			return RemoteConfig.getAllRemoteConfigs(repository.getConfig());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private static class ContentProvider extends
			RepositoriesViewContentProvider {
		private final Object[] children;

		private ContentProvider(TagsNode tagsNode) {
			this.children = getChildren(tagsNode);
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return children;
		}
	}
}
