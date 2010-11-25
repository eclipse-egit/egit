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
package org.eclipse.egit.ui.internal.rebase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.RebaseResult.Status;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Display the result of a rebase.
 */
public class RebaseResultDialog extends Dialog {
	private final Repository repo;

	private final RebaseResult result;

	/**
	 * @param shell
	 * @param repo
	 * @param result
	 */
	public RebaseResultDialog(Shell shell, Repository repo, RebaseResult result) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.repo = repo;
		this.result = result;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().indent(0, 0).grab(true, true).applyTo(
				main);

		Label resultLabel = new Label(main, SWT.NONE);
		resultLabel.setText(UIText.RebaseResultDialog_StatusLabel);
		Text resultText = new Text(main, SWT.BORDER);
		resultText.setText(result.getStatus().toString());
		resultText.setEditable(false);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(resultText);

		if (result.getStatus() == Status.STOPPED) {
			String diff;
			Group commitGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
			GridDataFactory.fillDefaults().span(2, 1).grab(true, true).applyTo(
					commitGroup);
			commitGroup.setText(UIText.RebaseResultDialog_DetailsGroup);
			commitGroup.setLayout(new GridLayout(2, false));
			RevWalk rw = new RevWalk(repo);

			Label commitIdLabel = new Label(commitGroup, SWT.NONE);
			commitIdLabel.setText(UIText.RebaseResultDialog_CommitIdLabel);
			Text commitId = new Text(commitGroup, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(commitId);

			Label commitMessageLabel = new Label(commitGroup, SWT.NONE);
			commitMessageLabel
					.setText(UIText.RebaseResultDialog_CommitMessageLabel);
			Text commitMessage = new Text(commitGroup, SWT.READ_ONLY
					| SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(
					commitMessage);
			try {
				// the commits might not have been fully loaded
				RevCommit commit = rw.parseCommit(result.getCurrentCommit());
				RevCommit parentCommit = rw.parseCommit(commit.getParent(0));
				commitMessage.setText(commit.getShortMessage());
				commitId.setText(commit.name());

				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				DiffFormatter df = new DiffFormatter(bos);
				df.setRepository(repo);
				try {
					df.format(commit.getTree(), parentCommit.getTree());
					diff = bos.toString("UTF-8"); //$NON-NLS-1$
				} catch (IOException e) {
					diff = null;
				}
			} catch (Exception e) {
				Activator.handleError(
						UIText.RebaseResultDialog_DiffCalculationErrorMessage,
						e, false);
				diff = UIText.RebaseResultDialog_DiffCalculationErrorDisplay;
			}
			if (diff != null) {
				Label diffLabel = new Label(commitGroup, SWT.NONE);
				diffLabel.setText(UIText.RebaseResultDialog_DiffDetailsLabel);
				GridDataFactory.fillDefaults().span(2, 1).applyTo(diffLabel);
				Text diffArea = new Text(commitGroup, SWT.MULTI | SWT.READ_ONLY
						| SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
				Point size = diffArea.getSize();
				int minHeight = diffArea.getLineHeight() * 10;
				GridDataFactory.fillDefaults().span(2, 1).grab(true, true)
						.hint(size).minSize(size.x, minHeight)
						.applyTo(diffArea);
				diffArea.setText(diff);
			}
			commitGroup.pack();
			applyDialogFont(main);
		}
		return main;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.RebaseResultDialog_DialogTitle);
	}
}
