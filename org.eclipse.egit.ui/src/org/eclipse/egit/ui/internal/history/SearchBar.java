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

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * A reusable SearchBar component that may be used in combination with
 * {@link CommitGraphTable} to search in a given list of commits.
 *
 * @see GitHistoryPage
 * @see CommitSelectionDialog
 */
abstract class SearchBar extends ControlContribution {

	protected FindToolbar toolbar;

	protected Object searchContext;

	protected String lastText;

	private ObjectId lastObjectId;

	protected Object lastSearchContext;

	private ICommitsProvider provider;

	protected final CommitGraphTable graph;

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
	 * Creates the SearchBar.
	 *
	 * @param id
	 *            the contribution item id
	 * @param graph
	 *            the UI element displaying the commits
	 */
	public SearchBar(String id, CommitGraphTable graph) {
		super(id);
		this.graph = graph;
	}

	@Override
	protected FindToolbar createControl(Composite parent) {
		toolbar = new FindToolbar(parent);
		toolbar.setBackground(null);
		toolbar.addListener(SWT.Modify, (e) -> lastText = toolbar.getText());
		toolbar.addSelectionListener(selectionListener);
		toolbar.addStatusListener(this::showStatus);
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

		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolbar);
		return toolbar;
	}

	/**
	 * Set the {@link ICommitsProvider} implementation that provides a search
	 * context for the SearchBar. The list of commits and the {@link RevFlag}
	 * are passed on to the {@link FindToolbar}.
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

	/**
	 * The {@link FindToolbar} notifies clients of status changes (e.g. number
	 * of matching commits) by calling this method. A sub-class can provide a
	 * way to display these status messages to the user.
	 *
	 * @param originator
	 *            the toolbar object that produced the new message
	 * @param text
	 *            the new status message to be displayed
	 */
	abstract protected void showStatus(FindToolbar originator, String text);

}
