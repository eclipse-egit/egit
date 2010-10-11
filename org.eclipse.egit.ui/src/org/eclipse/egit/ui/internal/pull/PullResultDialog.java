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

import org.eclipse.egit.ui.internal.fetch.FetchResultDialog;
import org.eclipse.egit.ui.internal.merge.MergeResultDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
		this.repo = repo;
		this.result = result;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		if (result.getFetchResult() != null) {
			FetchResultDialog dlg = new FetchResultDialog(getParentShell(),
					repo, result.getFetchResult(), result.getFetchedFrom());
			dlg.createDialogArea(main);
		}
		if (result.getMergeResult() != null) {
			MergeResultDialog dlg = new MergeResultDialog(getParentShell(),
					repo, result.getMergeResult());
			dlg.createDialogArea(main);
		}
		return main;
	}
}
