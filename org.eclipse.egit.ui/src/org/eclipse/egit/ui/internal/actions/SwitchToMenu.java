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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

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

		Repository[] repositories = SelectionUtils
				.getRepositories(handlerService.getCurrentState());

		if (repositories.length > 0) {
			createDynamicMenu(menu, repositories);
		}
	}

	private void createDynamicMenu(Menu menu, final Repository[] repositories) {

		if (!isMultipleSelection(repositories)) {
			createNewBranchMenuItem(menu, repositories[0]);
			createSeparator(menu);
		}

		int itemCount = createMostActiveBranchesMenuItems(menu, repositories);

		if (!isMultipleSelection(repositories) && itemCount > 0) {
			createSeparator(menu);
			createOtherMenuItem(menu, repositories[0]);
		}

		if (itemCount == 0 && isMultipleSelection(repositories)) {
			// If the menu would be empty, add a disabled menuItem to inform the
			// user that no common branches among the selection were found
			createDisabledMenu(menu, UIText.SwitchToMenu_NoCommonBranchesFound);
		}
	}

	private void createNewBranchMenuItem(Menu menu, Repository repository) {
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

	}

	private int createMostActiveBranchesMenuItems(Menu menu,
			Repository[] repositories) {
		int itemCount = 0;
		try {
			List<Map<String, Ref>> activeBranches = new ArrayList<>();

			for (Repository repository : repositories) {
				Map<String, Ref> branchRefMapping = getMostActiveBranches(
						repository, MAX_NUM_MENU_ENTRIES);
				activeBranches.add(branchRefMapping);
			}

			Set<String> activeBranchIntersection = getBranchNameIntersection(
					activeBranches);
			for (String branchName : activeBranchIntersection) {
				itemCount++;
				createMenuItemMultiple(menu, repositories, branchName);
			}

			if (itemCount >= MAX_NUM_MENU_ENTRIES) {
				return itemCount;
			}

			List<Map<String, Ref>> localBranchMapping = new ArrayList<>();
			for (Repository repository : repositories) {
				Map<String, Ref> localBranches = repository.getRefDatabase()
						.getRefs(Constants.R_HEADS);
				localBranchMapping.add(localBranches);
			}

			// A separator between recently used branches and local branches is
			// nice but only if we have both recently used branches and other
			// local branches
			Set<String> localBranchNameIntersection = getBranchNameIntersection(
					localBranchMapping);
			localBranchNameIntersection.removeAll(activeBranchIntersection);
			if (itemCount > 0 && !localBranchNameIntersection.isEmpty()) {
				createSeparator(menu);
			}

			for (String localBranchName : localBranchNameIntersection) {
				itemCount++;
				if (itemCount > MAX_NUM_MENU_ENTRIES) {
					break;
				}

				createMenuItemMultiple(menu, repositories, localBranchName);
			}
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}

		return itemCount;
	}

	private Set<String> getBranchNameIntersection(
			List<Map<String, Ref>> refMapping) {
		Iterator<Map<String, Ref>> iterator = refMapping.iterator();
		if (!iterator.hasNext()) {
			return Collections.emptySet();
		}

		Set<String> intersection = new TreeSet<>(
				CommonUtils.STRING_ASCENDING_COMPARATOR);
		intersection.addAll(iterator.next().keySet());
		iterator.forEachRemaining(map -> intersection.retainAll(map.keySet()));
		return intersection;
	}

	private void createOtherMenuItem(Menu menu, Repository repository) {
		MenuItem others = new MenuItem(menu, SWT.PUSH);
		others.setText(UIText.SwitchToMenu_OtherMenuLabel);
		others.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CheckoutDialog dialog = new CheckoutDialog(
						e.display.getActiveShell(), repository);
				if (dialog.open() == Window.OK) {
					BranchOperationUI.checkout(repository, dialog.getRefName())
							.start();
				}

			}
		});
	}

	private void createDisabledMenu(Menu menu, String text) {
		MenuItem disabled = new MenuItem(menu, SWT.PUSH);
		disabled.setText(text);
		disabled.setImage(branchImage);
		disabled.setEnabled(false);
	}

	private static MenuItem createSeparator(Menu menu) {
		return new MenuItem(menu, SWT.SEPARATOR);
	}

	private void createMenuItemMultiple(Menu menu, Repository[] repositories,
			String shortName) {

		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(shortName);

		boolean allRepositoriesCheckedOut = Stream.of(repositories)
				.allMatch(r -> shortName.equals(getBranch(r)));

		if (allRepositoriesCheckedOut) {
			item.setImage(checkedOutImage);
		} else {
			item.setImage(branchImage);
		}
		item.setEnabled(!allRepositoriesCheckedOut);

		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				BranchOperationUI.checkout(repositories, shortName).start();
			}
		});
	}

	private String getBranch(Repository repo) {
		try {
			return repo.getBranch();
		} catch (IOException e) {
			return ""; //$NON-NLS-1$
		}
	}

	private Map<String, Ref> getMostActiveBranches(final Repository repository,
			int maximumBranchCount) throws IOException {
		Map<String, Ref> localBranches = repository.getRefDatabase()
				.getRefs(Constants.R_HEADS);
		Map<String, Ref> activeRefs = new HashMap<>();

		ReflogReader reflogReader = repository.getReflogReader(Constants.HEAD);
		List<ReflogEntry> reflogEntries;
		if (reflogReader == null) {
			return Collections.emptyMap();
		}

		reflogEntries = reflogReader.getReverseEntries();

		for (ReflogEntry entry : reflogEntries) {
			CheckoutEntry checkout = entry.parseCheckout();
			if (checkout != null) {
				Ref ref = localBranches.get(checkout.getFromBranch());
				if (ref != null && activeRefs.size() < maximumBranchCount) {
					activeRefs.put(checkout.getFromBranch(), ref);
				}
				ref = localBranches.get(checkout.getToBranch());
				if (ref != null && activeRefs.size() < maximumBranchCount) {
					activeRefs.put(checkout.getToBranch(), ref);
				}
			}
		}

		return activeRefs;
	}

	private boolean isMultipleSelection(Repository[] repositories) {
		return repositories.length > 1;
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
