/*******************************************************************************
 * Copyright (c) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.jobs.RepositoryJobResultAction;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
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
	 */
	public ShowPushResultAction(@NonNull Repository repository,
			PushOperationResult result, String destination,
			boolean showConfigureButton) {
		super(repository, UIText.ShowPushResultAction_name);
		this.operationResult = result;
		this.destination = destination;
		this.showConfigure = showConfigureButton;
	}

	@Override
	protected void showResult(Repository repository) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell();
		PushResultDialog dialog = new PushResultDialog(shell, repository,
				operationResult, destination, false);
		dialog.showConfigureButton(showConfigure);
		dialog.open();
	}
}
