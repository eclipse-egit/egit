/*******************************************************************************
 * Copyright (c) 2013 Robin Stocker <robin@nibor.org> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo.SelectionType;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewContentProvider;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.repository.tree.TagsNode;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Tag to select a remote and one or more tags to push.
 */
public class PushTagsPage extends WizardPage {

	private final Repository repository;

	private final Set<String> tagRefNamesToSelect = new HashSet<String>();

	private RemoteConfig selectedRemoteConfig = null;

	private List<TagNode> selectedTags = new ArrayList<TagNode>();

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
			if (tagName.startsWith(Constants.R_TAGS)) {
				tagRefNamesToSelect.add(tagName);
			} else {
				tagRefNamesToSelect.add(Constants.R_TAGS + tagName);
			}
		}
	}

	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());

		Label remoteLabel = new Label(main, SWT.NONE);
		remoteLabel.setText(UIText.PushTagsPage_RemoteLabel);

		RemoteSelectionCombo remoteSelectionCombo = new RemoteSelectionCombo(
				main, SWT.NONE, SelectionType.PUSH);
		remoteSelectionCombo.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());
		selectedRemoteConfig = remoteSelectionCombo
				.setItems(getRemoteConfigs());
		remoteSelectionCombo
				.addRemoteSelectionListener(new RemoteSelectionCombo.IRemoteSelectionListener() {
					public void remoteSelected(RemoteConfig remoteConfig) {
						selectedRemoteConfig = remoteConfig;
					}
				});

		final CheckboxTableViewer tagsTable = CheckboxTableViewer.newCheckList(
				main, SWT.BORDER);
		tagsTable.getControl().setLayoutData(
				GridDataFactory.fillDefaults().grab(true, true).span(2, 1)
						.hint(400, 300).create());
		RepositoriesViewContentProvider contentProvider = new RepositoriesViewContentProvider();
		Object[] tagNodes = contentProvider.getChildren(new TagsNode(null,
				repository));
		tagsTable.setContentProvider(new ArrayContentProvider());
		tagsTable.setLabelProvider(new DelegatingStyledCellLabelProvider(
				new RepositoriesViewLabelProvider()));
		tagsTable.setComparator(new ViewerComparator(
				CommonUtils.STRING_ASCENDING_COMPARATOR));
		tagsTable.setInput(tagNodes);

		initiallySelectTags(tagNodes, tagsTable);

		tagsTable.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				setSelectedTags(tagsTable.getCheckedElements());
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

	private void initiallySelectTags(Object[] tagNodes,
			CheckboxTableViewer tagsTable) {
		List<TagNode> checkedTags = new ArrayList<TagNode>();
		for (Object node : tagNodes) {
			if (node instanceof TagNode) {
				TagNode tagNode = (TagNode) node;
				Ref ref = tagNode.getObject();
				if (tagRefNamesToSelect.contains(ref.getName()))
					checkedTags.add(tagNode);
			}
		}

		TagNode[] checkedTagsArray = checkedTags
				.toArray(new TagNode[checkedTags.size()]);
		tagsTable.setCheckedElements(checkedTagsArray);
		tagsTable.setSelection(StructuredSelection.EMPTY);
		if (checkedTagsArray.length > 0) {
			tagsTable.reveal(checkedTagsArray[0]);
		}
		setSelectedTags(checkedTagsArray);
	}

	private void setSelectedTags(Object[] tags) {
		selectedTags.clear();
		for (Object tag : tags) {
			if (tag instanceof TagNode)
				selectedTags.add((TagNode) tag);
		}
		setPageComplete(isPageComplete());
	}

	private List<RemoteConfig> getRemoteConfigs() {
		try {
			return RemoteConfig.getAllRemoteConfigs(repository.getConfig());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
