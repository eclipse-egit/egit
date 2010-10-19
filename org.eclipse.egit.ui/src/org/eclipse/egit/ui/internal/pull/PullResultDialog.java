/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pull;

import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.fetch.FetchResultDialog;
import org.eclipse.egit.ui.internal.merge.MergeResultDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
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

	/**
	 * @param shell
	 * @param repo
	 * @param result
	 */
	public PullResultDialog(Shell shell, Repository repo, PullResult result) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.repo = repo;
		this.result = result;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().indent(0, 0).grab(true, true).applyTo(
				main);
		Group fetchResultGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		fetchResultGroup
				.setText(UIText.PullResultDialog_FetchResultGroupHeader);
		fetchResultGroup.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(
				fetchResultGroup);
		FetchResult fRes = result.getFetchResult();
		if (fRes != null && !fRes.getTrackingRefUpdates().isEmpty()) {
			FetchResultDialog dlg = new FetchResultDialog(getParentShell(),
					repo, fRes, result.getFetchedFrom());
			Control fresult = dlg.createDialogArea(fetchResultGroup);
			GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT,
					130).applyTo(fresult);
		} else {
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
		mergeResultGroup.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(
				mergeResultGroup);
		MergeResult mRes = result.getMergeResult();
		if (mRes != null
				&& mRes.getMergeStatus() != MergeStatus.ALREADY_UP_TO_DATE) {
			MergeResultDialog dlg = new MergeResultDialog(getParentShell(),
					repo, result.getMergeResult());
			dlg.createDialogArea(mergeResultGroup);
		} else {
			Label noResult = new Label(mergeResultGroup, SWT.NONE);
			noResult
					.setText(UIText.PullResultDialog_MergeAlreadyUpToDateMessage);
		}
		return main;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.PullResultDialog_DialogTitle);
	}
}
