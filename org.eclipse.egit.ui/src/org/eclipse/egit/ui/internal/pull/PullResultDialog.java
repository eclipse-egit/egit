/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Markus Keller <markus_keller@ch.ibm.com> - Show the repository name in the title of the Pull Result dialog
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pull;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.fetch.FetchResultDialog;
import org.eclipse.egit.ui.internal.merge.MergeResultDialog;
import org.eclipse.egit.ui.internal.rebase.RebaseResultDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.RebaseResult.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Display the result of a pull.
 * <p>
 * Simply combines fetch and merge result dialogs into one dialog.
 */
public class PullResultDialog extends Dialog {
	private final Repository repo;

	private final PullResult result;

	private boolean persistSize;

	/**
	 * @param shell
	 * @param repo
	 * @param result
	 */
	public PullResultDialog(Shell shell, Repository repo, PullResult result) {
		super(shell);
		setShellStyle(getShellStyle() & ~SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
		setBlockOnOpen(false);
		this.repo = repo;
		this.result = result;
		persistSize = hasFetchResults() || hasMergeResults();
	}

	private boolean hasFetchResults() {
		final FetchResult fetchResult = result.getFetchResult();
		return fetchResult != null
				&& !fetchResult.getTrackingRefUpdates().isEmpty();
	}

	private boolean hasMergeResults() {
		final MergeResult mergeResult = result.getMergeResult();
		return mergeResult != null
				&& mergeResult.getMergeStatus() != MergeStatus.ALREADY_UP_TO_DATE;
	}

	private boolean hasRebaseResults() {
		final RebaseResult rebaseResult = result.getRebaseResult();
		return rebaseResult != null
				&& rebaseResult.getStatus() != Status.UP_TO_DATE;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(main);
		GridDataFactory.fillDefaults().indent(0, 0).grab(true, true).applyTo(
				main);
		Group fetchResultGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		fetchResultGroup
				.setText(UIText.PullResultDialog_FetchResultGroupHeader);
		GridLayoutFactory.fillDefaults().applyTo(fetchResultGroup);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(
				fetchResultGroup);
		FetchResult fRes = result.getFetchResult();
		if (hasFetchResults()) {
			GridLayoutFactory.fillDefaults().applyTo(fetchResultGroup);
			FetchResultDialog dlg = new FetchResultDialog(getParentShell(),
					repo, fRes, result.getFetchedFrom());
			Control fresult = dlg.createFetchResultTable(fetchResultGroup);
			Object layoutData = fresult.getLayoutData();
			if (layoutData instanceof GridData)
				GridDataFactory.createFrom((GridData) layoutData)
						.hint(SWT.DEFAULT, 130).applyTo(fresult);

		} else {
			GridLayoutFactory.swtDefaults().applyTo(fetchResultGroup);
			Label noResult = new Label(fetchResultGroup, SWT.NONE);
			if (result.getFetchedFrom().equals(".")) //$NON-NLS-1$
				noResult
						.setText(UIText.PullResultDialog_NothingToFetchFromLocal);
			else
				noResult.setText(NLS.bind(
						UIText.FetchResultDialog_labelEmptyResult, result
								.getFetchedFrom()));

		}
		Group mergeResultGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		mergeResultGroup
				.setText(UIText.PullResultDialog_MergeResultGroupHeader);
		if (hasMergeResults()) {
			GridDataFactory.fillDefaults().grab(true, true).applyTo(
					mergeResultGroup);
			GridLayoutFactory.fillDefaults().applyTo(mergeResultGroup);
			MergeResultDialog dlg = new MergeResultDialog(getParentShell(),
					repo, result.getMergeResult());
			dlg.createDialogArea(mergeResultGroup);
		} else if (hasRebaseResults()) {
			RebaseResultDialog.createFailedOrConflictsParts(mergeResultGroup,
					result.getRebaseResult());
			GridDataFactory.fillDefaults().grab(true, false).applyTo(
					mergeResultGroup);
		} else {
			GridDataFactory.fillDefaults().grab(true, false).applyTo(
					mergeResultGroup);
			GridLayoutFactory.swtDefaults().applyTo(mergeResultGroup);
			Label noResult = new Label(mergeResultGroup, SWT.NONE);
			noResult
					.setText(UIText.PullResultDialog_MergeAlreadyUpToDateMessage);
		}
		return main;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.CLOSE_LABEL,
				true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(NLS.bind(
				UIText.PullResultDialog_DialogTitle,
				Activator.getDefault().getRepositoryUtil()
						.getRepositoryName(repo)));
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return UIUtils.getDialogBoundSettings(getClass());
	}

	@Override
	protected int getDialogBoundsStrategy() {
		int strategy = DIALOG_PERSISTLOCATION;
		if (persistSize)
			strategy |= DIALOG_PERSISTSIZE;
		return strategy;
	}

	@Override
	protected Point getInitialSize() {
		if (!persistSize) {
			// For "small" dialogs with label-only results, use the default
			// height and the persisted width
			Point size = super.getInitialSize();
			size.x = getPersistedSize().x;
			return size;
		}
		return super.getInitialSize();
	}

	private Point getPersistedSize() {
		boolean oldPersistSize = persistSize;
		// This affects getDialogBoundsStrategy
		persistSize = true;
		try {
			Point persistedSize = super.getInitialSize();
			return persistedSize;
		} finally {
			persistSize = oldPersistSize;
		}
	}
}
