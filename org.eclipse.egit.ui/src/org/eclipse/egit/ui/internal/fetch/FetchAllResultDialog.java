/*******************************************************************************
 * Copyright (C) 2026 EGit Contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import java.util.List;

import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.TitleAndImageDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog displaying results from fetching all configured remotes.
 */
public class FetchAllResultDialog extends TitleAndImageDialog {

	private final Repository repository;

	private final List<FetchResultEntry> results;

	private final List<String> errors;

	/**
	 * @param parentShell
	 *            parent shell
	 * @param repository
	 *            repository that was fetched
	 * @param results
	 *            successful fetch results
	 * @param errors
	 *            error messages from failed remotes
	 */
	public FetchAllResultDialog(Shell parentShell, Repository repository,
			List<FetchResultEntry> results, List<String> errors) {
		super(parentShell, UIIcons.WIZBAN_FETCH);
		setShellStyle(getShellStyle() & ~SWT.APPLICATION_MODAL | SWT.RESIZE);
		setBlockOnOpen(false);
		this.repository = repository;
		this.results = results;
		this.errors = errors;
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				UIText.FetchResultDialog_CloseButton, true);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		setTitle(NLS.bind(UIText.FetchResultDialog_title,
				RepositoryUtil.INSTANCE.getRepositoryName(repository)));

		Composite main = new Composite(composite, SWT.NONE);
		GridLayoutFactory.swtDefaults().margins(5, 8).spacing(0, 8)
				.applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		for (FetchResultEntry entry : results) {
			FetchResult result = entry.getResult();
			Group group = new Group(main, SWT.SHADOW_ETCHED_IN);
			group.setText(entry.getSourceString());
			GridLayoutFactory.swtDefaults().margins(6, 8).spacing(0, 6)
					.applyTo(group);
			GridDataFactory.fillDefaults()
					.grab(true, !result.getTrackingRefUpdates().isEmpty())
					.applyTo(group);
			if (result.getTrackingRefUpdates().isEmpty()) {
				Label label = new Label(group, SWT.NONE);
				label.setText(NLS.bind(UIText.FetchResultDialog_labelEmptyResult,
						entry.getSourceString()));
			} else {
				Label label = new Label(group, SWT.NONE);
				label.setText(NLS.bind(
						UIText.FetchResultDialog_labelNonEmptyResult,
						entry.getSourceString()));
				FetchResultTable table = new FetchResultTable(group);
				GridDataFactory.fillDefaults().grab(true, true).hint(600, 130)
						.applyTo(table.getControl());
				table.setData(repository, result);
			}
		}

		for (String error : errors) {
			Label label = new Label(main, SWT.WRAP);
			label.setText(error);
			GridDataFactory.fillDefaults().grab(true, false).hint(600,
					SWT.DEFAULT).applyTo(label);
		}

		applyDialogFont(composite);
		return composite;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(NLS.bind(UIText.FetchResultDialog_title,
				RepositoryUtil.INSTANCE.getRepositoryName(repository)));
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return UIUtils.getDialogBoundSettings(getClass());
	}

	@Override
	protected Point getInitialSize() {
		Point size = super.getInitialSize();
		size.x = Math.max(size.x, 650);
		size.y = Math.max(size.y, Math.min(520,
				180 + results.size() * 65 + errors.size() * 40));
		return size;
	}
}
