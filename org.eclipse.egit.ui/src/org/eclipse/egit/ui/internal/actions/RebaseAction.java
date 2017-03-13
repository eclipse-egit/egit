/*******************************************************************************
 * Copyright (C) 2011, 2013 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commands.shared.AbortRebaseCommand;
import org.eclipse.egit.ui.internal.commands.shared.AbstractRebaseCommandHandler;
import org.eclipse.egit.ui.internal.commands.shared.AbstractSharedCommandHandler;
import org.eclipse.egit.ui.internal.commands.shared.ContinueRebaseCommand;
import org.eclipse.egit.ui.internal.commands.shared.SkipRebaseCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;

/**
 * A pulldown action to rebase the current branch, or continue, skip or abort an
 * active rebase.
 *
 * @see RebaseActionHandler
 */
public class RebaseAction extends RepositoryAction implements
		IWorkbenchWindowPulldownDelegate {

	private final Image rebaseSkip;

	private final Image rebaseAbort;

	private final Image rebaseContinue;

	/**
	 *
	 */
	public RebaseAction() {
		super(ActionCommands.REBASE_ACTION, new RebaseActionHandler());

		rebaseSkip = UIIcons.REBASE_SKIP.createImage();
		rebaseAbort = UIIcons.REBASE_ABORT.createImage();
		rebaseContinue = UIIcons.REBASE_CONTINUE.createImage();
	}

	@Override
	public Menu getMenu(Control parent) {
		Menu menu = new Menu(parent);
		Repository repo = getRepository();

		boolean rebaseing = isInRebasingState(repo);
		boolean canContinue = rebaseing && canContinue(repo);

		addMenuItem(menu, UIText.RebasePulldownAction_Continue, rebaseContinue,
				new ContinueRebaseCommand(), canContinue);
		addMenuItem(menu, UIText.RebasePulldownAction_Skip, rebaseSkip,
				new SkipRebaseCommand(), rebaseing);
		addMenuItem(menu, UIText.RebasePulldownAction_Abort, rebaseAbort,
				new AbortRebaseCommand(), rebaseing);
		return menu;
	}

	@Override
	public void dispose() {
		rebaseSkip.dispose();
		rebaseAbort.dispose();
		rebaseContinue.dispose();
		super.dispose();
	}

	@Override
	protected boolean shouldRunAction() {
		Repository repo = getRepository();
		return !isInRebasingState(repo);
	}

	private void addMenuItem(Menu parent, String itemName, Image image,
			AbstractRebaseCommandHandler action, boolean isEnabled) {
		MenuItem item = new MenuItem(parent, SWT.PUSH);
		item.setImage(image);
		item.setText(itemName);
		item.setEnabled(isEnabled);
		ExecutionEvent event = createExecutionEvent();
		ItemSelectionListener selectionListener = new ItemSelectionListener(
				action, event);
		item.addSelectionListener(selectionListener);
	}

	private Repository getRepository() {
		ExecutionEvent event = createExecutionEvent();
		return AbstractSharedCommandHandler.getRepository(event);
	}

	private boolean isInRebasingState(Repository repo) {
		if (repo == null)
			return false;

		RepositoryState state = repo.getRepositoryState();
		return state.isRebasing();
	}

	private boolean canContinue(Repository repo) {
		IndexDiffCache diffCache = org.eclipse.egit.core.Activator.getDefault()
				.getIndexDiffCache();
		if (diffCache != null) {
			IndexDiffCacheEntry entry = diffCache.getIndexDiffCacheEntry(repo);
			return entry != null
					&& entry.getIndexDiff().getConflicting().isEmpty();
		}
		return false;
	}

	private static class ItemSelectionListener implements SelectionListener {

		private final ExecutionEvent event;

		private final AbstractRebaseCommandHandler action;

		private ItemSelectionListener(AbstractRebaseCommandHandler action,
				ExecutionEvent event) {
			this.event = event;
			this.action = action;
		}

		@Override
		public void widgetSelected(SelectionEvent selectionEvent) {
			try {
				action.execute(event);
			} catch (ExecutionException e) {
				Activator.logError(e.getMessage(), e);
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// not used
		}

	}

}
