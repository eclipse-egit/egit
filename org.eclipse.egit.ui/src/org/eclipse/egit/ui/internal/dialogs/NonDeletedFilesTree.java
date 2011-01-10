/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.List;

import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.FileTreeContentProvider.Mode;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * UI to show a tree with files within a Repository
 */
public class NonDeletedFilesTree extends TreeViewer {

	private final List<String> filePaths;

	private Button showRepoRelative;

	private Button showFull;

	private Button showResource;

	/**
	 * @param parent
	 * @param repository
	 * @param pathList
	 */
	public NonDeletedFilesTree(Composite parent, Repository repository,
			List<String> pathList) {
		super(createComposite(parent, repository), SWT.BORDER);
		this.filePaths = pathList;

		Composite main = getTree().getParent();

		GridDataFactory.fillDefaults().span(2, 1).grab(true, true).applyTo(
				getTree());

		final FileTreeContentProvider cp = new FileTreeContentProvider(
				repository);

		GridDataFactory.fillDefaults().span(2, 1).grab(true, true).applyTo(
				getTree());

		setContentProvider(cp);
		setLabelProvider(new FileTreeLabelProvider());
		setInput(this.filePaths);
		expandAll();

		showRepoRelative = new Button(main, SWT.RADIO);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(
				showRepoRelative);
		showRepoRelative
				.setText(UIText.NonDeletedFilesTree_RepoRelativePathsButton);
		showRepoRelative.setSelection(true);
		showRepoRelative.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cp.setMode(Mode.REPO_RELATIVE_PATHS);
				setInput(getInput());
				expandAll();
			}
		});

		showFull = new Button(main, SWT.RADIO);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(
				showFull);
		showFull.setText(UIText.NonDeletedFilesTree_FileSystemPathsButton);
		showFull.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cp.setMode(Mode.FULL_PATHS);
				setInput(getInput());
				expandAll();
			}
		});

		showResource = new Button(main, SWT.RADIO);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(
				showResource);
		showResource.setText(UIText.NonDeletedFilesTree_ResourcePathsButton);
		showResource.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cp.setMode(Mode.RESOURCE_PATHS);
				setInput(getInput());
				expandAll();
			}
		});
	}

	private static Composite createComposite(Composite parent,
			Repository repository) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(0, 0).grab(true, true).applyTo(
				main);
		main.setLayout(new GridLayout(2, false));
		Label repoLabel = new Label(main, SWT.NONE);
		repoLabel.setText(UIText.NonDeletedFilesTree_RepositoryLabel);
		Text repoPath = new Text(main, SWT.BORDER | SWT.READ_ONLY);
		repoPath.setText(repository.getWorkTree().getPath());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(repoPath);

		return main;
	}
}
