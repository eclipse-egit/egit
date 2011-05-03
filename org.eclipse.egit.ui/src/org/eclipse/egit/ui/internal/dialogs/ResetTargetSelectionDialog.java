/*******************************************************************************
3 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - use the abstract super class
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog for selecting a reset target.
 */
public class ResetTargetSelectionDialog extends AbstractBranchSelectionDialog {

	private static final int SWT_NONE = 0;
	private ResetType resetType = ResetType.MIXED;
	private Text anySha1;
	private String parsedCommitish;

	private Label subject;

	private Label author;

	private Label sha1;

	private Label committer;

	/**
	 * Construct a dialog to select a branch to reset to
	 *
	 * @param parentShell
	 * @param repo
	 */
	public ResetTargetSelectionDialog(Shell parentShell, Repository repo) {
		super(parentShell, repo, SHOW_LOCAL_BRANCHES | SHOW_REMOTE_BRANCHES
				| SHOW_TAGS | SHOW_REFERENCES | EXPAND_LOCAL_BRANCHES_NODE
				| SELECT_CURRENT_REF);
		super.setHelpAvailable(false);
	}

	@Override
	protected void createCustomArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(main);

		Group g2 = new Group(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(g2);
		g2.setLayout(new GridLayout(2, false));
		Label label = new Label(g2, SWT.NONE);
		label.setText("Reset to (expression):"); //$NON-NLS-1$
		anySha1 = new Text(g2, SWT.BORDER);
		anySha1.setToolTipText("Any git expression evaluating to a commit-ish"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().grab(true, false).applyTo(anySha1);

		Group g3 = new Group(g2, SWT_NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(g3);
		g3.setLayout(new GridLayout(2, false));
		new Label(g3, SWT.NONE).setText("SHA-1:"); //$NON-NLS-1$
		sha1 = new Label(g3, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sha1);
		new Label(g3, SWT.NONE).setText("Subject:"); //$NON-NLS-1$
		subject = new Label(g3, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(subject);
		new Label(g3, SWT.NONE).setText("Author:"); //$NON-NLS-1$
		author = new Label(g3, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(author);
		new Label(g3, SWT.NONE).setText("Committer:"); //$NON-NLS-1$
		committer = new Label(g3, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(committer);

		Group g = new Group(main, SWT.NONE);
		g.setText(UIText.ResetTargetSelectionDialog_ResetTypeGroup);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(g);
		g.setLayout(new GridLayout(1, false));

		anySha1.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				// Do nothing
			}
			public void focusGained(FocusEvent e) {
				branchTree.setSelection(null);
			}
		});
		anySha1.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String text = anySha1.getText();
				if (text.length() == 0) {
					parsedCommitish = null;
					setMessage(""); //$NON-NLS-1$
					return;
				}
				try {
					ObjectId resolved = repo.resolve(text+"^{commit}"); //$NON-NLS-1$
					if (resolved == null) {
						setMessage(
								"Unresolvable expression ", IMessageProvider.ERROR); //$NON-NLS-1$
						getButton(OK).setEnabled(false);
						parsedCommitish = null;
						sha1.setText(""); //$NON-NLS-1$
						subject.setText(""); //$NON-NLS-1$
						author.setText(""); //$NON-NLS-1$
						committer.setText(""); //$NON-NLS-1$
						return;
					} else {
						setMessage(""); //$NON-NLS-1$
						parsedCommitish = text;
						getButton(OK).setEnabled(true);
						RevWalk rw = new RevWalk(repo);
						RevCommit commit = rw.parseCommit(resolved);
						sha1.setText(AbbreviatedObjectId.fromObjectId(commit)
								.name());
						subject.setText(commit.getShortMessage());
						author.setText(commit.getAuthorIdent().getName()
								+ " <" //$NON-NLS-1$
								+ commit.getAuthorIdent().getEmailAddress()
								+ "> " + commit.getAuthorIdent().getWhen()); //$NON-NLS-1$
						committer.setText(commit.getCommitterIdent().getName()
								+ " <" //$NON-NLS-1$
								+ commit.getCommitterIdent().getEmailAddress()
								+ " >" + commit.getCommitterIdent().getWhen()); //$NON-NLS-1$
						rw.dispose();
					}
				} catch (IOException e1) {
					setMessage(e1.getMessage(), IMessageProvider.ERROR);
					getButton(OK).setEnabled(false);
					parsedCommitish = null;
				}
			}
		});
		branchTree.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				if (!event.getSelection().isEmpty()) {
					anySha1.setText(refNameFromDialog());
					anySha1.selectAll();
				}
			}
		});
		Button soft = new Button(g, SWT.RADIO);
		soft.setText(UIText.ResetTargetSelectionDialog_ResetTypeSoftButton);
		soft.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection())
					resetType = ResetType.SOFT;
			}
		});

		Button medium = new Button(g, SWT.RADIO);
		medium.setSelection(true);
		medium.setText(UIText.ResetTargetSelectionDialog_ResetTypeMixedButton);
		medium.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection())
					resetType = ResetType.MIXED;
			}
		});

		Button hard = new Button(g, SWT.RADIO);
		hard.setText(UIText.ResetTargetSelectionDialog_ResetTypeHardButton);
		hard.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection())
					resetType = ResetType.HARD;
			}
		});
	}

	@Override
	protected void refNameSelected(String refName) {
		getButton(Window.OK).setEnabled(refName != null);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(Window.OK).setText(
				UIText.ResetTargetSelectionDialog_ResetButton);
	}

	@Override
	protected String getTitle() {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repo);
		return NLS.bind(UIText.ResetTargetSelectionDialog_ResetTitle, repoName);
	}

	@Override
	protected String getWindowTitle() {
		return UIText.ResetTargetSelectionDialog_WindowTitle;
	}

	/**
	 * @return Type of Reset
	 */
	public ResetType getResetType() {
		return resetType;
	}

	@Override
	protected void okPressed() {
		if (resetType == ResetType.HARD) {
			if (!MessageDialog.openQuestion(getShell(),
					UIText.ResetTargetSelectionDialog_ResetQuestion,
					UIText.ResetTargetSelectionDialog_ResetConfirmQuestion)) {
				return;
			}
		}
		super.okPressed();
	}

	@Override
	protected String getMessageText() {
		return UIText.ResetTargetSelectionDialog_SelectBranchForResetMessage;
	}

	@Override
	public String getRefName() {
		String selected = super.getRefName();
		if (selected != null)
			return selected;
		return parsedCommitish;
	}
}