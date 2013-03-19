/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.SWTUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.egit.ui.internal.repository.tree.BranchesNode;
import org.eclipse.egit.ui.internal.repository.tree.LocalNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.internal.storage.file.CheckoutEntry;
import org.eclipse.jgit.internal.storage.file.ReflogEntry;
import org.eclipse.jgit.internal.storage.file.ReflogReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Dynamically builds the "Switch to..." sub-menu
 */
public class SwitchToMenu extends ContributionItem implements
		IWorkbenchContribution {
	/** the maximum number of branches to show in the sub-menu */
	private static final int MAX_NUM_MENU_ENTRIES = 20;

	private ISelectionService srv;

	private final Image branchImage;

	private final Image newBranchImage;

	private final Image checkedOutImage;

	/**
	 */
	public SwitchToMenu() {
		this(null);
	}

	/**
	 * @param id
	 */
	public SwitchToMenu(String id) {
		super(id);
		branchImage = UIIcons.BRANCH.createImage();
		newBranchImage = UIIcons.CREATE_BRANCH.createImage();
		// create the "checked out" image
		checkedOutImage = SWTUtils.getDecoratedImage(branchImage,
				UIIcons.OVR_CHECKEDOUT);
	}

	@Override
	public void fill(Menu menu, int index) {
		if (srv == null)
			return;
		ISelection sel = srv.getSelection();
		if (!(sel instanceof IStructuredSelection))
			return;
		Object selected = ((IStructuredSelection) sel).getFirstElement();
		if (selected instanceof IAdaptable) {
			Object adapter = ((IAdaptable) selected).getAdapter(IProject.class);
			if (adapter != null)
				selected = adapter;
		}

		Repository repository = null;
		if (selected instanceof RepositoryNode)
			repository = ((RepositoryNode) selected).getRepository();
		else if (selected instanceof BranchesNode)
			repository = ((BranchesNode) selected).getRepository();
		else if (selected instanceof LocalNode)
			repository = ((LocalNode) selected).getRepository();
		else if ((selected instanceof IProject)) {
			RepositoryMapping mapping = RepositoryMapping
					.getMapping((IProject) selected);
			if (mapping != null)
				repository = mapping.getRepository();
		}

		if (repository != null)
			createDynamicMenu(menu, repository);
	}

	private void createDynamicMenu(Menu menu, final Repository repository) {
		MenuItem newBranch = new MenuItem(menu, SWT.PUSH);
		newBranch.setText(UIText.SwitchToMenu_NewBranchMenuLabel);
		newBranch.setImage(newBranchImage);
		newBranch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				BranchOperationUI.create(repository).start();
			}
		});
		new MenuItem(menu, SWT.SEPARATOR);
		try {
			String currentBranch = repository.getFullBranch();
			Map<String, Ref> localBranches = repository.getRefDatabase().getRefs(
					Constants.R_HEADS);
			TreeMap<String, Ref> sortedRefs = new TreeMap<String, Ref>();

			// Add the MAX_NUM_MENU_ENTRIES most recently used branches first
			List<ReflogEntry> reflogEntries = new ReflogReader(
					repository, Constants.HEAD)
					.getReverseEntries();
			for (ReflogEntry entry : reflogEntries) {
				CheckoutEntry checkout = entry.parseCheckout();
				if (checkout != null) {
					Ref ref = localBranches.get(checkout.getFromBranch());
					if (ref != null)
						if (sortedRefs.size() < MAX_NUM_MENU_ENTRIES)
							sortedRefs.put(checkout.getFromBranch(), ref);
					ref = localBranches.get(checkout.getToBranch());
					if (ref != null)
						if (sortedRefs.size() < MAX_NUM_MENU_ENTRIES)
							sortedRefs.put(checkout.getToBranch(), ref);
				}
			}

			// Add the recently used branches to the menu, in alphabetical order
			int itemCount = 0;
			for (final Entry<String, Ref> entry : sortedRefs.entrySet()) {
				itemCount++;
				final String shortName = entry.getKey();
				final String fullName = entry.getValue().getName();
				createMenuItem(menu, repository, currentBranch, fullName, shortName);
				// Do not duplicate branch names
				localBranches.remove(shortName);
			}

			if (itemCount < MAX_NUM_MENU_ENTRIES) {
				// A separator between recently used branches and local branches is
				// nice but only if we have both recently used branches and other
				// local branches
				if (itemCount > 0 && localBranches.size() > 0)
					new MenuItem(menu, SWT.SEPARATOR);

				// Now add more other branches if we have only a few branch switches
				// Sort the remaining local branches
				sortedRefs.clear();
				sortedRefs.putAll(localBranches);
				for (final Entry<String, Ref> entry : sortedRefs.entrySet()) {
					itemCount++;
					// protect ourselves against a huge sub-menu
					if (itemCount > MAX_NUM_MENU_ENTRIES)
						break;
					final String fullName = entry.getValue().getName();
					final String shortName = entry.getKey();
					createMenuItem(menu, repository, currentBranch, fullName, shortName);
				}
			}
			if (itemCount > 0)
				new MenuItem(menu, SWT.SEPARATOR);
			MenuItem others = new MenuItem(menu, SWT.PUSH);
			others.setText(UIText.SwitchToMenu_OtherMenuLabel);
			others.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					BranchOperationUI.checkout(repository).start();
				}
			});
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
	}

	private void createMenuItem(Menu menu, final Repository repository,
			String currentBranch, final String fullName, String shortName) {
		final MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(shortName);
		boolean checkedOut = currentBranch.equals(fullName);
		if (checkedOut)
			item.setImage(checkedOutImage);
		else
			item.setImage(branchImage);
		item.setEnabled(!checkedOut);
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				BranchOperationUI.checkout(repository, fullName)
						.start();
			}
		});
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	public void initialize(IServiceLocator serviceLocator) {
		srv = (ISelectionService) serviceLocator
				.getService(ISelectionService.class);
	}

	@Override
	public void dispose() {
		branchImage.dispose();
		newBranchImage.dispose();
		checkedOutImage.dispose();
	}
}
