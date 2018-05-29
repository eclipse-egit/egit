/*******************************************************************************
 * Copyright (C) 2011, 2016 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 486594
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.egit.ui.internal.dialogs.CheckoutDialog;
import org.eclipse.egit.ui.internal.repository.CreateBranchWizard;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.CheckoutEntry;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Dynamically builds the "Switch to..." sub-menu
 */
public class SwitchToMenu extends ContributionItem implements
		IWorkbenchContribution {
	/** the maximum number of branches to show in the sub-menu */
	static final int MAX_NUM_MENU_ENTRIES = 20;

	private IHandlerService handlerService;

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
		ResourceManager pluginResources = Activator.getDefault()
				.getResourceManager();
		branchImage = UIIcons.getImage(pluginResources, UIIcons.BRANCH);
		newBranchImage = UIIcons.getImage(pluginResources,
				UIIcons.CREATE_BRANCH);
		checkedOutImage = UIIcons.getImage(pluginResources,
				UIIcons.CHECKED_OUT_BRANCH);
	}

	@Override
	public void fill(Menu menu, int index) {
		if (handlerService == null)
			return;

		Repository repository = SelectionUtils
				.getRepository(handlerService.getCurrentState());
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
				String sourceRef = repository.getConfig().getString(
						ConfigConstants.CONFIG_WORKFLOW_SECTION, null,
						ConfigConstants.CONFIG_KEY_DEFBRANCHSTARTPOINT);
				CreateBranchWizard wiz = null;
				try {
					Ref ref = null;
					if (sourceRef != null) {
						ref = repository.findRef(sourceRef);
					}
					if (ref != null) {
						wiz = new CreateBranchWizard(repository, ref.getName());
					} else {
						wiz = new CreateBranchWizard(repository,
								repository.getFullBranch());
					}
				} catch (IOException e1) {
					// Ignore
				}
				if (wiz == null) {
					wiz = new CreateBranchWizard(repository);
				}
				WizardDialog dlg = new WizardDialog(e.display.getActiveShell(),
						wiz);
				dlg.setHelpAvailable(false);
				dlg.open();
			}
		});
		createSeparator(menu);
		try {
			String currentBranch = repository.getFullBranch();
			Map<String, Ref> localBranches = repository.getRefDatabase().getRefs(
					Constants.R_HEADS);
			TreeMap<String, Ref> sortedRefs = new TreeMap<>(
					CommonUtils.STRING_ASCENDING_COMPARATOR);

			// Add the MAX_NUM_MENU_ENTRIES most recently used branches first
			ReflogReader reflogReader = repository.getReflogReader(
					Constants.HEAD);
			List<ReflogEntry> reflogEntries;
			if (reflogReader == null) {
				reflogEntries = Collections.emptyList();
			} else {
				reflogEntries = reflogReader.getReverseEntries();
			}
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
					createSeparator(menu);

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
				createSeparator(menu);
			MenuItem others = new MenuItem(menu, SWT.PUSH);
			others.setText(UIText.SwitchToMenu_OtherMenuLabel);
			others.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					CheckoutDialog dialog = new CheckoutDialog(
							e.display.getActiveShell(), repository);
					if (dialog.open() == Window.OK) {
						BranchOperationUI
								.checkout(repository, dialog.getRefName())
								.start();
					}

				}
			});
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
	}

	private static MenuItem createSeparator(Menu menu) {
		return new MenuItem(menu, SWT.SEPARATOR);
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

	@Override
	public void initialize(IServiceLocator serviceLocator) {
		handlerService = CommonUtils.getService(serviceLocator, IHandlerService.class);
	}

}
