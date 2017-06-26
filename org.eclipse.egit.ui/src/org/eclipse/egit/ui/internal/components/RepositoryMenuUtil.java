/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
 * Provides a way to populate a menu with a list of repositories.
 */
public final class RepositoryMenuUtil {

	private RepositoryMenuUtil() {
		// Utility class shall not be instantiated
	}

	/**
	 * Populates the given {@link IMenuManager} with a list of repositories.
	 * Each currently known configured repository is shown with its repository
	 * name and the path to the .git directory as tooltip; when a menu item is
	 * selected, the given {@code action} is invoked. Bare repositories can be
	 * excluded from the list. Menu items are sorted by repository name and .git
	 * directory paths.
	 *
	 * @param menuManager
	 *            to populate with the list of repositories
	 * @param includeBare
	 *            {@code true} if bare repositories should be included in the
	 *            list, {@code false} otherwise
	 * @param action
	 *            to perform on the chosen repository
	 */
	public static void fillRepositories(IMenuManager menuManager,
			boolean includeBare, Consumer<Repository> action) {
		for (IAction item : getRepositoryActions(includeBare, action)) {
			menuManager.add(item);
		}
	}

	/**
	 * Creates for each configured repository an {@link IAction} that will
	 * perform the given {@code action} when invoked.
	 *
	 * @param includeBare
	 *            {@code true} if bare repositories should be included in the
	 *            list, {@code false} otherwise
	 * @param action
	 *            to perform on the chosen repository
	 * @return the (possibly empty) list of actions
	 */
	public static Collection<IAction> getRepositoryActions(boolean includeBare,
			Consumer<Repository> action) {
		RepositoryUtil util = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryUtil();
		RepositoryCache cache = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache();
		Set<String> repositories = util.getRepositories();
		Map<String, Set<File>> repos = new HashMap<>();
		for (String repo : repositories) {
			File gitDir = new File(repo);
			String name = null;
			try {
				Repository r = cache.lookupRepository(gitDir);
				if (!includeBare && r.isBare()) {
					continue;
				}
				name = util.getRepositoryName(r);
			} catch (IOException e) {
				continue;
			}
			Set<File> files = repos.get(name);
			if (files == null) {
				files = new HashSet<>();
				files.add(gitDir);
				repos.put(name, files);
			} else {
				files.add(gitDir);
			}
		}
		String[] repoNames = repos.keySet().toArray(new String[repos.size()]);
		Arrays.sort(repoNames, CommonUtils.STRING_ASCENDING_COMPARATOR);
		List<IAction> result = new ArrayList<>();
		for (String repoName : repoNames) {
			Set<File> files = repos.get(repoName);
			File[] gitDirs = files.toArray(new File[files.size()]);
			Arrays.sort(gitDirs);
			for (File f : gitDirs) {
				IAction menuItem = new Action(repoName) {
					@Override
					public void run() {
						try {
							Repository r = cache.lookupRepository(f);
							action.accept(r);
						} catch (IOException e) {
							Activator.showError(e.getLocalizedMessage(), e);
						}
					}
				};
				menuItem.setToolTipText(f.getPath());
				result.add(menuItem);
			}
		}
		return result;
	}

	/**
	 * Utility class facilitating creating toolbar actions that show a drop-down
	 * menu of all registered repositories, performing a given action on a
	 * selected repository.
	 */
	public static class RepositoryToolbarAction extends Action
			implements IWorkbenchAction, IMenuCreator {

		private final RepositoryUtil util = org.eclipse.egit.core.Activator
				.getDefault().getRepositoryUtil();

		private final IEclipsePreferences preferences = util.getPreferences();

		private final IPreferenceChangeListener listener;

		private final Consumer<Repository> action;

		private final boolean includeBare;

		private Menu menu;

		private boolean showMenu;

		/**
		 * Creates a new {@link RepositoryToolbarAction} with the given
		 * {@code action} and default text, image, and tooltip.
		 *
		 * @param action
		 *            to run when a repository is selected from the drop-down
		 *            menu
		 * @param includeBare
		 *            {@code true} if bare repositories shall be included,
		 *            {@code false} otherwise
		 */
		public RepositoryToolbarAction(boolean includeBare,
				Consumer<Repository> action) {
			this(UIText.RepositoryToolbarAction_label, UIIcons.REPOSITORY,
					UIText.RepositoryToolbarAction_tooltip, includeBare,
					action);
		}

		/**
		 * Creates a new {@link RepositoryToolbarAction} with the given text and
		 * the given {@code action}.
		 *
		 * @param text
		 *            for the action
		 * @param image
		 *            for the action
		 * @param tooltip
		 *            for the action
		 * @param includeBare
		 *            {@code true} if bare repositories shall be included,
		 *            {@code false} otherwise
		 * @param action
		 *            to run when a repository is selected from the drop-down
		 *            menu
		 */
		public RepositoryToolbarAction(String text, ImageDescriptor image,
				String tooltip, boolean includeBare,
				Consumer<Repository> action) {
			super(text, IAction.AS_DROP_DOWN_MENU);
			setImageDescriptor(image);
			setToolTipText(tooltip);
			this.includeBare = includeBare;
			this.action = action;
			this.listener = event -> {
				if (RepositoryUtil.PREFS_DIRECTORIES_REL
						.equals(event.getKey())) {
					setEnabled(!util.getRepositories().isEmpty());
				}
			};
			setEnabled(!util.getRepositories().isEmpty());
			preferences.addPreferenceChangeListener(listener);
		}

		@Override
		public void run() {
			showMenu = true;
		}

		@Override
		public void runWithEvent(Event event) {
			if (!isEnabled()) {
				return;
			}
			// Show the menu also when the button is clicked, unless run() is
			// overridden (and not called via super).
			showMenu = false;
			run();
			Widget widget = event.widget;
			if (showMenu && (widget instanceof ToolItem)) {
				ToolItem item = (ToolItem) widget;
				Rectangle bounds = item.getBounds();
				event.detail = SWT.ARROW;
				event.x = bounds.x;
				event.y = bounds.y + bounds.height;
				item.notifyListeners(SWT.Selection, event);
			}
		}

		@Override
		public IMenuCreator getMenuCreator() {
			return this;
		}

		@Override
		public Menu getMenu(Control parent) {
			if (menu != null) {
				menu.dispose();
				menu = null;
			}
			if (isEnabled()) {
				Collection<IAction> actions = RepositoryMenuUtil
						.getRepositoryActions(includeBare, action);
				menu = new Menu(parent);
				for (IAction a : actions) {
					ActionContributionItem item = new ActionContributionItem(a);
					item.fill(menu, -1);
				}
			}
			return menu;
		}

		@Override
		public Menu getMenu(Menu parent) {
			// Not used
			return null;
		}

		@Override
		public void dispose() {
			if (menu != null) {
				menu.dispose();
				menu = null;
			}
			preferences.removePreferenceChangeListener(listener);
		}
	}
}
