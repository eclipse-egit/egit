/*******************************************************************************
 * Copyright (c) 2010, 2013, 2015 SAP AG and others.
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
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 477248
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;

import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.GitDateFormatter;
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
	private static final String RESET_TYPE_SETTING = "ResetTargetSelectionDialog.resetType"; //$NON-NLS-1$
	private static final int SWT_NONE = 0;
	private ResetType resetType = ResetType.MIXED;
	private Text anySha1;
	private String parsedCommitish;

	private Label subject;

	private Label author;

	private Label sha1;

	private Label committer;

	private final GitDateFormatter gitDateFormatter = PreferenceBasedDateFormatter
			.create();

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
		GridLayoutFactory.fillDefaults().applyTo(main);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(main);

		Group g2 = new Group(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(g2);
		g2.setLayout(new GridLayout(2, false));
		Label label = new Label(g2, SWT.NONE);
		label.setText(UIText.ResetTargetSelectionDialog_ExpressionLabel);
		anySha1 = new Text(g2, SWT.BORDER);
		anySha1.setToolTipText(UIText.ResetTargetSelectionDialog_ExpressionTooltip);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(anySha1);

		Group g3 = new Group(g2, SWT_NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(g3);
		g3.setLayout(new GridLayout(2, false));
		new Label(g3, SWT.NONE).setText(UIText.ResetTargetSelectionDialog_CommitLabel);
		sha1 = new Label(g3, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(sha1);
		new Label(g3, SWT.NONE).setText(UIText.ResetTargetSelectionDialog_SubjectLabel);
		subject = new Label(g3, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(subject);
		new Label(g3, SWT.NONE).setText(UIText.ResetTargetSelectionDialog_AuthorLabel);
		author = new Label(g3, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(author);
		new Label(g3, SWT.NONE).setText(UIText.ResetTargetSelectionDialog_CommitterLabel);
		committer = new Label(g3, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(committer);

		Group g = new Group(main, SWT.NONE);
		g.setText(UIText.ResetTargetSelectionDialog_ResetTypeGroup);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(g);
		g.setLayout(new GridLayout(1, false));

		anySha1.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				// Do nothing
			}
			@Override
			public void focusGained(FocusEvent e) {
				branchTree.setSelection(null);
			}
		});
		anySha1.addModifyListener(new ModifyListener() {
			@Override
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
								UIText.ResetTargetSelectionDialog_UnresolvableExpressionError,
								IMessageProvider.ERROR);
						getButton(OK).setEnabled(false);
						parsedCommitish = null;
						sha1.setText(""); //$NON-NLS-1$
						subject.setText(""); //$NON-NLS-1$
						author.setText(""); //$NON-NLS-1$
						committer.setText(""); //$NON-NLS-1$
						return;
					} else {
						if (RepositoryUtil.isDetachedHead(repo)) {
							setMessage(
									UIText.ResetTargetSelectionDialog_DetachedHeadState,
									IMessageProvider.INFORMATION);
						} else {
							setMessage(""); //$NON-NLS-1$
						}
						parsedCommitish = text;
						getButton(OK).setEnabled(true);
						try (RevWalk rw = new RevWalk(repo)) {
							RevCommit commit = rw.parseCommit(resolved);
							sha1.setText(AbbreviatedObjectId
									.fromObjectId(commit).name());
							subject.setText(commit.getShortMessage());
							author.setText(
									commit.getAuthorIdent().getName() + " <" //$NON-NLS-1$
											+ commit.getAuthorIdent()
													.getEmailAddress()
											+ "> " //$NON-NLS-1$
											+ gitDateFormatter.formatDate(
													commit.getAuthorIdent()));
							committer.setText(commit.getCommitterIdent()
									.getName()
									+ " <" //$NON-NLS-1$
									+ commit.getCommitterIdent()
											.getEmailAddress()
									+ "> " + gitDateFormatter.formatDate( //$NON-NLS-1$
											commit.getCommitterIdent()));
						}
					}
				} catch (IOException e1) {
					setMessage(e1.getMessage(), IMessageProvider.ERROR);
					getButton(OK).setEnabled(false);
					parsedCommitish = null;
				}
			}
		});
		branchTree.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!event.getSelection().isEmpty()) {
					String refName = refNameFromDialog();
					if (refName != null) {
						anySha1.setText(refName);
						anySha1.selectAll();
					}
				}
			}
		});
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		if (settings.get(RESET_TYPE_SETTING) != null) {
			resetType = ResetType.valueOf(settings.get(RESET_TYPE_SETTING));
		}
		createResetButton(g,
				UIText.ResetTargetSelectionDialog_ResetTypeSoftButton,
				ResetType.SOFT);
		createResetButton(g,
				UIText.ResetTargetSelectionDialog_ResetTypeMixedButton,
				ResetType.MIXED);
		createResetButton(g,
				UIText.ResetTargetSelectionDialog_ResetTypeHardButton,
				ResetType.HARD);
	}

	private Button createResetButton(Composite parent, String text,
			final ResetType type) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(text);
		button.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection())
					resetType = type;
			}
		});
		button.setSelection(type == resetType);
		return button;
	}

	@Override
	protected void refNameSelected(String refName) {
		boolean enabled = refName != null || parsedCommitish != null;
		getButton(Window.OK).setEnabled(enabled);
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
			if (!CommandConfirmation.confirmHardReset(getShell(), repo)) {
				return;
			}
		}
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		settings.put(RESET_TYPE_SETTING, resetType.name());
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

	@Override
	protected boolean markRef(String refName) {
		// preselect HEAD if in the detached HEAD state
		return super
				.markRef(RepositoryUtil.isDetachedHead(repo) ? Constants.HEAD
						: refName);
	}
}
