/*******************************************************************************
 * Copyright (C) 2019 Simon Muschel <smuschel@gmx.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Simon Muschel <smuschel@gmx.de> - Bug 345466
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.egit.ui.internal.history.FindToolbar.StatusListener;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.SubContributionItem;
import org.eclipse.jface.action.SubToolBarManager;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IActionBars;

class SearchBar extends ControlContribution {

	private IActionBars bars;

	private FindToolbar toolbar;

	private Object searchContext;

	private String lastText;

	private ObjectId lastObjectId;

	private Object lastSearchContext;

	private ICommitsProvider provider;

	private boolean wasVisible = false;

	private final CommitGraphTable graph;

	private final IAction openCloseToggle;

	/**
	 * "Go to next/previous" from the {@link FindToolbar} sends
	 * {@link SWT#Selection} events with the chosen {@link RevCommit} as data.
	 */
	private final Listener selectionListener = new Listener() {

		@Override
		public void handleEvent(Event evt) {
			final RevCommit commit = (RevCommit) evt.data;
			lastObjectId = commit.getId();
			graph.selectCommit(commit);
		}
	};

	/**
	 * Listener to close the search bar on ESC. (Ctrl/Cmd-F is already handled
	 * via global retarget action.)
	 */
	private final KeyListener keyListener = new KeyAdapter() {

		@Override
		public void keyPressed(KeyEvent e) {
			int key = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
			if (key == SWT.ESC) {
				setVisible(false);
				e.doit = false;
			}
		}
	};

	/**
	 * Listener to display status messages from the asynchronous find. (Is
	 * called in the UI thread.)
	 */
	private final StatusListener statusListener = new StatusListener() {

		@Override
		public void setMessage(FindToolbar originator, String text) {
			if (bars != null) {
				IStatusLineManager status = bars.getStatusLineManager();
				if (status != null) {
					status.setMessage(text);
				}
			}
		}
	};

	/**
	 * Listener to ensure that the history view is fully activated when the user
	 * clicks into the search bar's text widget. This makes sure our status
	 * manager gets activated and thus shows the status messages. We don't get a
	 * focus event when the user clicks in the field; and fiddling with the
	 * focus in a FocusListener could get hairy anyway.
	 */
	private final Listener mouseListener = new Listener() {

		private boolean hasFocus;

		private boolean hadFocusOnMouseDown;

		@Override
		public void handleEvent(Event e) {
			switch (e.type) {
			case SWT.FocusIn:
				toolbar.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						hasFocus = true;
					}
				});

				break;
			case SWT.FocusOut:
				hasFocus = false;
				break;
			case SWT.MouseDown:
				hadFocusOnMouseDown = hasFocus;
				break;
			case SWT.MouseUp:
				if (!hadFocusOnMouseDown) {
					graph.getControl().setFocus();
					toolbar.setFocus();
				}
				break;
			default:
				break;
			}
		}
	};

	/**
	 *
	 * @param id
	 * @param graph
	 * @param openCloseAction
	 * @param bars
	 */
	public SearchBar(String id, CommitGraphTable graph, IAction openCloseAction,
			IActionBars bars) {
		super(id);
		super.setVisible(false);
		this.graph = graph;
		this.openCloseToggle = openCloseAction;
		this.bars = bars;
	}

	private void beforeHide() {
		lastText = toolbar.getText();
		lastSearchContext = searchContext;
		statusListener.setMessage(toolbar, ""); //$NON-NLS-1$
		// It will be disposed by the IToolBarManager
		toolbar = null;
		openCloseToggle.setChecked(false);
		wasVisible = false;
	}

	private void workAroundBug551067(boolean visible) {
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=551067
		IContributionManager parent = getParent();
		if (parent instanceof SubToolBarManager) {
			SubToolBarManager subManager = (SubToolBarManager) parent;
			IContributionItem item = subManager.getParent().find(getId());
			if (item instanceof SubContributionItem) {
				item.setVisible(visible && subManager.isVisible());
			}
		}
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible != isVisible()) {
			if (!visible) {
				beforeHide();
			}
			super.setVisible(visible);
			workAroundBug551067(visible);
			// Update the toolbar. Will dispose our FindToolbar widget on
			// hide, and will create a new one (through createControl())
			// on show. It'll also reposition the toolbar, if needed.
			// Note: just doing bars.getToolBarManager().update(true);
			// messes up big time (doesn't resize or re-position).
			if (bars != null) {
				bars.updateActionBars();
			}
			if (visible && toolbar != null) {
				openCloseToggle.setChecked(true);
				// If the toolbar was moved below the tabs, we now have
				// the wrong background. It disappears when one clicks
				// elsewhere. Looks like an inactive selection... No
				// way found to fix this but this ugly focus juggling:
				graph.getControl().setFocus();
				toolbar.setFocus();
			} else if (!visible && !graph.getControl().isDisposed()) {
				graph.getControl().setFocus();
			}
		}
	}

	@Override
	public boolean isDynamic() {
		// We toggle our own visibility
		return true;
	}

	@Override
	protected Control createControl(Composite parent) {
		toolbar = new FindToolbar(parent);
		toolbar.setBackground(null);
		toolbar.addKeyListener(keyListener);
		toolbar.addListener(SWT.FocusIn, mouseListener);
		toolbar.addListener(SWT.FocusOut, mouseListener);
		toolbar.addListener(SWT.MouseDown, mouseListener);
		toolbar.addListener(SWT.MouseUp, mouseListener);
		toolbar.addListener(SWT.Modify, (e) -> lastText = toolbar.getText());
		toolbar.addStatusListener(statusListener);
		toolbar.addSelectionListener(selectionListener);
		boolean hasInput = provider != null;
		if (hasInput) {
			setInput(provider);
		}
		if (lastText != null) {
			if (lastSearchContext != null
					&& lastSearchContext.equals(searchContext)) {
				toolbar.setPreselect(lastObjectId);
			}
			toolbar.setText(lastText, hasInput);
		}
		lastSearchContext = null;
		lastObjectId = null;
		if (wasVisible) {
			return toolbar;
		}
		wasVisible = true;
		// This fixes the wrong background when Eclipse starts up with the
		// search bar visible.
		toolbar.getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				if (toolbar != null && !toolbar.isDisposed()) {
					// See setVisible() above. Somehow, we need this, too.
					graph.getControl().setFocus();
					toolbar.setFocus();
				}
			}
		});

		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolbar);
		return toolbar;
	}

	/**
	 *
	 * @param provider
	 */
	public void setInput(ICommitsProvider provider) {
		this.provider = provider;
		if (toolbar != null) {
			searchContext = provider.getSearchContext();
			toolbar.setInput(provider.getHighlight(),
					graph.getTableView().getTable(), provider.getCommits());
		}
	}

}
