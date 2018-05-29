/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.List;

import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.FileTreeContentProvider.Mode;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * UI to show a tree with files within a Repository
 */
public class NonDeletedFilesTree extends TreeViewer {

	private final List<String> filePaths;

	/**
	 * @param parent
	 * @param repository
	 * @param pathList
	 */
	public NonDeletedFilesTree(Composite parent, Repository repository,
			List<String> pathList) {
		super(createComposite(parent), SWT.BORDER);
		this.filePaths = pathList;

		Composite main = getTree().getParent();

		GridDataFactory.fillDefaults().grab(true, true).applyTo(getTree());

		final FileTreeContentProvider cp = new FileTreeContentProvider(
				repository);

		setContentProvider(cp);
		setLabelProvider(new FileTreeLabelProvider());
		setInput(this.filePaths);
		expandAll();

		final ToolBar dropDownBar = new ToolBar(main, SWT.FLAT | SWT.RIGHT);
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.BEGINNING)
				.grab(false, false).applyTo(dropDownBar);
		final ToolItem dropDownItem = new ToolItem(dropDownBar, SWT.DROP_DOWN);
		Image dropDownImage = UIIcons.HIERARCHY.createImage();
		UIUtils.hookDisposal(dropDownItem, dropDownImage);
		dropDownItem.setImage(dropDownImage);
		final Menu menu = new Menu(dropDownBar);
		dropDownItem.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				menu.dispose();
			}
		});
		dropDownItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Rectangle b = dropDownItem.getBounds();
				Point p = dropDownItem.getParent().toDisplay(
						new Point(b.x, b.y + b.height));
				menu.setLocation(p.x, p.y);
				menu.setVisible(true);
			}

		});

		final MenuItem showRepoRelative = new MenuItem(menu, SWT.RADIO);
		showRepoRelative
				.setText(UIText.NonDeletedFilesTree_RepoRelativePathsButton);
		showRepoRelative.setSelection(true);
		showRepoRelative.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (showRepoRelative.getSelection()) {
					cp.setMode(Mode.REPO_RELATIVE_PATHS);
					setInput(getInput());
					expandAll();
				}
			}
		});

		final MenuItem showFull = new MenuItem(menu, SWT.RADIO);
		showFull.setText(UIText.NonDeletedFilesTree_FileSystemPathsButton);
		showFull.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (showFull.getSelection()) {
					cp.setMode(Mode.FULL_PATHS);
					setInput(getInput());
					expandAll();
				}
			}
		});

		final MenuItem showResource = new MenuItem(menu, SWT.RADIO);
		showResource.setText(UIText.NonDeletedFilesTree_ResourcePathsButton);
		showResource.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (showResource.getSelection()) {
					cp.setMode(Mode.RESOURCE_PATHS);
					setInput(getInput());
					expandAll();
				}
			}
		});
	}

	private static Composite createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(0, 0).grab(true, true)
				.applyTo(main);
		GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(2)
				.applyTo(main);
		return main;
	}
}
