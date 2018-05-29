/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP AG and others.
 * Copyright (C) 2012, Tomasz Zarna <Tomasz.Zarna@pl.ibm.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - use the abstract super class
 *    Tomasz Zarna (IBM) - merge squash, bug 382720
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeConfig;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for selecting a merge target.
 *
 */
public class MergeTargetSelectionDialog extends AbstractBranchSelectionDialog {

	private boolean mergeSquash;

	private FastForwardMode fastForwardMode;

	private boolean mergeCommit;

	/**
	 * @param parentShell
	 * @param repo
	 */
	public MergeTargetSelectionDialog(Shell parentShell, Repository repo) {
		super(parentShell, repo, getMergeTarget(repo), SHOW_LOCAL_BRANCHES
				| SHOW_REMOTE_BRANCHES | SHOW_TAGS | EXPAND_LOCAL_BRANCHES_NODE
				| getSelectSetting(repo));
		MergeConfig config = MergeConfig.getConfigForCurrentBranch(repo);
		fastForwardMode = config.getFastForwardMode();
		mergeSquash = config.isSquash();
		if (mergeSquash)
			mergeCommit = false;
		else
			mergeCommit = config.isCommit();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(Window.OK).setText(
				UIText.MergeTargetSelectionDialog_ButtonMerge);
	}

	@Override
	protected String getMessageText() {
		String branch = getCurrentBranch();
		if (branch != null)
			return MessageFormat.format(
					UIText.MergeTargetSelectionDialog_SelectRefWithBranch,
					branch);
		else
			return UIText.MergeTargetSelectionDialog_SelectRef;
	}

	@Override
	protected String getTitle() {
		String branch = getCurrentBranch();
		if (branch != null)
			return MessageFormat.format(
					UIText.MergeTargetSelectionDialog_TitleMergeWithBranch,
					branch);
		else
			return UIText.MergeTargetSelectionDialog_TitleMerge;
	}

	@Override
	protected void refNameSelected(String refName) {
		boolean tagSelected = refName != null
				&& refName.startsWith(Constants.R_TAGS);

		boolean branchSelected = refName != null
				&& (refName.startsWith(Constants.R_HEADS) || refName
						.startsWith(Constants.R_REMOTES));

		boolean currentSelected;
		try {
			currentSelected = refName != null
					&& refName.equals(repo.getFullBranch());
		} catch (IOException e) {
			currentSelected = false;
		}

		getButton(Window.OK).setEnabled(
				!currentSelected && (branchSelected || tagSelected));
	}

	@Override
	protected void createCustomArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(main);
		Group mergeTypeGroup = new Group(main, SWT.NONE);
		mergeTypeGroup
				.setText(UIText.MergeTargetSelectionDialog_MergeTypeGroup);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(mergeTypeGroup);
		mergeTypeGroup.setLayout(new GridLayout(1, false));

		Button commit = new Button(mergeTypeGroup, SWT.RADIO);
		if (mergeCommit)
			commit.setSelection(true);
		commit.setText(UIText.MergeTargetSelectionDialog_MergeTypeCommitButton);
		commit.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection()) {
					mergeSquash = false;
					mergeCommit = true;
				}
			}
		});

		Button noCommit = new Button(mergeTypeGroup, SWT.RADIO);
		if (!mergeCommit && !mergeSquash)
			noCommit.setSelection(true);
		noCommit.setText(UIText.MergeTargetSelectionDialog_MergeTypeNoCommitButton);
		noCommit.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection()) {
					mergeSquash = false;
					mergeCommit = false;
				}
			}
		});

		Button squash = new Button(mergeTypeGroup, SWT.RADIO);
		if (mergeSquash)
			squash.setSelection(true);
		squash.setText(UIText.MergeTargetSelectionDialog_MergeTypeSquashButton);
		squash.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection()) {
					mergeSquash = true;
					mergeCommit = false;
				}
			}
		});

		Group fastForwardGroup = new Group(main, SWT.NONE);
		fastForwardGroup
				.setText(UIText.MergeTargetSelectionDialog_FastForwardGroup);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(fastForwardGroup);
		fastForwardGroup.setLayout(new GridLayout(1, false));

		createFastForwardButton(fastForwardGroup,
				UIText.MergeTargetSelectionDialog_FastForwardButton,
				FastForwardMode.FF);
		createFastForwardButton(fastForwardGroup,
				UIText.MergeTargetSelectionDialog_NoFastForwardButton,
				FastForwardMode.NO_FF);
		createFastForwardButton(fastForwardGroup,
				UIText.MergeTargetSelectionDialog_OnlyFastForwardButton,
				FastForwardMode.FF_ONLY);
	}

	private void createFastForwardButton(Group grp, String text,
			final FastForwardMode ffMode) {
		Button btn = new Button(grp, SWT.RADIO);
		btn.setText(text);
		btn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection())
					fastForwardMode = ffMode;
			}
		});
		btn.setSelection(fastForwardMode == ffMode);
	}

	/**
	 * @return whether the merge is to be squashed
	 */
	public boolean isMergeSquash() {
		return mergeSquash;
	}

	/**
	 * @return selected fast forward mode
	 */
	public FastForwardMode getFastForwardMode() {
		return fastForwardMode;
	}

	/**
	 * @return whether the merge is to be committed
	 */
	public boolean isCommit() {
		return mergeCommit;
	}
}
