/*******************************************************************************
 * Copyright (C) 2016, 2017 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.jobs.RepositoryJobResultAction;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Action to display a {@link PushOperationResult} in a
 * {@link PushResultDialog}.
 */
public class ShowPushResultAction extends RepositoryJobResultAction {

	private final boolean showConfigure;

	private final PushOperationResult operationResult;

	private final String destination;

	private final @NonNull PushMode pushMode;

	/**
	 * Creates a new {@link ShowPushResultAction}.
	 *
	 * @param repository
	 *            the result belongs to
	 * @param result
	 *            to show
	 * @param destination
	 *            describing where the push went to
	 * @param showConfigureButton
	 *            {@code true} to show a configure button in the
	 *            {@link PushResultDialog}
	 * @param pushMode
	 *            the push was for
	 */
	public ShowPushResultAction(@NonNull Repository repository,
			PushOperationResult result, String destination,
			boolean showConfigureButton, @NonNull PushMode pushMode) {
		super(repository, UIText.ShowPushResultAction_name);
		this.operationResult = result;
		this.destination = destination;
		this.showConfigure = showConfigureButton;
		this.pushMode = pushMode;
	}

	private boolean isModal(Shell shell) {
		return (shell.getStyle() & (SWT.APPLICATION_MODAL | SWT.PRIMARY_MODAL
				| SWT.SYSTEM_MODAL)) != 0;
	}

	@Override
	protected void showResult(@NonNull Repository repository) {
		Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider()
				.getShell();
		PushResultDialog dialog = new PushResultDialog(shell, repository,
				operationResult, destination, isModal(shell), pushMode);
		dialog.showConfigureButton(showConfigure);
		dialog.open();
	}
}
