/*******************************************************************************
 * Copyright (c) 2013, 2022 SAP AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Matthias Sohn (SAP AG) - initial implementation
 *    Simon Muschel <smuschel@gmx.de> - Bug 451817
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.util.regex.Pattern;

import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.internal.gerrit.GerritUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponent;
import org.eclipse.egit.ui.internal.dialogs.ICommitMessageComponentNotifications;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.egit.ui.internal.staging.CommitMessagePreviewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jgit.lib.CommitConfig.CleanupMode;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.UserConfig;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Dialog for editing a commit message
 */
public class CommitMessageEditorDialog extends TitleAreaDialog {

	private static final String DIALOG_SETTINGS_SECTION_NAME = Activator.PLUGIN_ID
			+ ".COMMIT_MESSAGE_EDITOR_DIALOG_SECTION"; //$NON-NLS-1$

	private static final Pattern CHANGE_ID = Pattern
			.compile("^Change-Id: I[0-9a-fA-F]{40}$", Pattern.MULTILINE); //$NON-NLS-1$

	private Repository repository;

	private SpellcheckableMessageArea messageArea;

	private Composite previewArea;

	private CommitMessagePreviewer previewer;

	private CommitMessageComponent commitComponent;

	private Composite commitMessageSection;

	private StackLayout previewLayout;

	private IAction addChangeIdAction;

	private String title;

	private String okButtonLabel;

	private String cancelButtonLabel;

	private String commitMessage;

	private boolean useChangeId;

	private CleanupMode mode;

	private char commentChar;

	/**
	 * @param parentShell
	 *            the parent SWT shell
	 * @param repository
	 *            to commit is or will be in
	 * @param commitMessage
	 *            the commit message to be edited
	 * @param mode
	 *            the {@link CleanupMode}
	 * @param commentChar
	 *            the comment character
	 */
	public CommitMessageEditorDialog(Shell parentShell, Repository repository,
			String commitMessage, CleanupMode mode, char commentChar) {
		this(parentShell, repository, commitMessage, mode, commentChar,
				UIText.CommitMessageEditorDialog_EditCommitMessageTitle);
	}

	/**
	 * @param parentShell
	 *            the parent SWT shell
	 * @param repository
	 *            to commit is or will be in
	 * @param commitMessage
	 *            the commit message to be edited
	 * @param mode
	 *            the {@link CleanupMode}
	 * @param commentChar
	 *            the comment character
	 * @param title
	 *            the dialog title
	 */
	public CommitMessageEditorDialog(Shell parentShell, Repository repository,
			String commitMessage, CleanupMode mode, char commentChar,
			String title) {
		this(parentShell, repository, commitMessage, mode, commentChar,
				UIText.CommitMessageEditorDialog_OkButton,
				IDialogConstants.CANCEL_LABEL);
		this.title = title;
	}

	/**
	 * @param parentShell
	 *            the parent SWT shell
	 * @param repository
	 *            to commit is or will be in
	 * @param commitMessage
	 *            the commit message to be edited
	 * @param mode
	 *            the {@link CleanupMode}
	 * @param commentChar
	 *            the comment character
	 * @param okButtonLabel
	 *            the label for the Ok button
	 * @param cancelButtonLabel
	 *            the label for the Cancel button
	 */
	public CommitMessageEditorDialog(Shell parentShell, Repository repository,
			String commitMessage, CleanupMode mode, char commentChar,
			String okButtonLabel, String cancelButtonLabel) {
		super(parentShell);
		this.repository = repository;
		this.commitMessage = commitMessage;
		this.title = UIText.CommitMessageEditorDialog_EditCommitMessageTitle;
		this.okButtonLabel = okButtonLabel;
		this.cancelButtonLabel = cancelButtonLabel;
		this.mode = mode;
		this.commentChar = commentChar;
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(newShellStyle | SWT.RESIZE | SWT.MAX);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		composite.setLayout(gridLayout);

		setTitle(UIText.RebaseInteractiveHandler_EditMessageDialogTitle);
		setMessage(UIText.RebaseInteractiveHandler_EditMessageDialogText);

		Config config = repository.getConfig();

		commitMessageSection = new Composite(composite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(commitMessageSection);
		GridLayoutFactory.fillDefaults().numColumns(1)
				.applyTo(commitMessageSection);

		Composite titleBar = new Composite(commitMessageSection, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(titleBar);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(titleBar);
		Label areaTitle = new Label(titleBar, SWT.NONE);
		areaTitle.setText(UIText.StagingView_CommitMessage);
		areaTitle.setFont(JFaceResources.getFontRegistry()
				.getBold(JFaceResources.DEFAULT_FONT));
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(areaTitle);

		Composite commitMessageToolbarComposite = new Composite(titleBar,
				SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false)
				.align(SWT.END, SWT.CENTER)
				.applyTo(commitMessageToolbarComposite);
		RowLayout layout = new RowLayout();
		layout.marginTop = 0;
		layout.marginBottom = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;
		commitMessageToolbarComposite.setLayout(layout);
		ToolBarManager commitMessageToolBarManager = new ToolBarManager(
				SWT.FLAT | SWT.HORIZONTAL);

		IAction previewAction = new Action(
				UIText.StagingView_Preview_Commit_Message,
				IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				if (isChecked()) {
					previewLayout.topControl = previewArea;
					areaTitle.setText(UIText.StagingView_CommitMessagePreview);
					previewer.setText(repository,
							messageArea.getCommitMessage());
				} else {
					previewLayout.topControl = messageArea;
					areaTitle.setText(UIText.StagingView_CommitMessage);
				}
				areaTitle.requestLayout();
				previewLayout.topControl.getParent().layout(true, true);
				commitMessageSection.redraw();
				if (!isChecked()) {
					messageArea.setFocus();
				}
			}
		};
		previewAction.setImageDescriptor(UIIcons.ELCL16_PREVIEW);
		commitMessageToolBarManager.add(previewAction);
		commitMessageToolBarManager.add(new Separator());

		IAction signOffAction = new Action(UIText.StagingView_Add_Signed_Off_By,
				IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				commitComponent.setSignedOffButtonSelection(isChecked());
			}
		};
		signOffAction.setImageDescriptor(UIIcons.SIGNED_OFF);
		commitMessageToolBarManager.add(signOffAction);

		addChangeIdAction = new Action(UIText.StagingView_Add_Change_ID,
				IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				commitComponent.setChangeIdButtonSelection(isChecked());
			}
		};
		addChangeIdAction.setImageDescriptor(UIIcons.GERRIT);
		boolean hasChangeId = hasChangeIdFooter(commitMessage);
		addChangeIdAction.setChecked(hasChangeId);
		addChangeIdAction.setEnabled(
				hasChangeId || GerritUtil.getCreateChangeId(config));
		commitMessageToolBarManager.add(addChangeIdAction);

		ToolBar tb = commitMessageToolBarManager
				.createControl(commitMessageToolbarComposite);
		tb.setBackground(null);

		Composite commitMessageTextComposite = new Composite(
				commitMessageSection, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(commitMessageTextComposite);

		previewLayout = new StackLayout();
		commitMessageTextComposite.setLayout(previewLayout);

		messageArea = new SpellcheckableMessageArea(commitMessageTextComposite,
				"", SWT.NONE); //$NON-NLS-1$
		messageArea.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TEXT_BORDER);
		CleanupMode cleanup = mode;
		if (cleanup == null || CleanupMode.DEFAULT.equals(cleanup)) {
			cleanup = CleanupMode.STRIP;
		}
		messageArea.setCleanupMode(cleanup, commentChar);
		// CommitMessageComponent expects the text to use Text.DELIMITER as line
		// terminator.
		String msg = Utils.normalizeLineEndings(commitMessage).replaceAll("\n", //$NON-NLS-1$
				Text.DELIMITER);
		messageArea.setText(msg);
		Point size = messageArea.getTextWidget().getSize();
		int minHeight = messageArea.getTextWidget().getLineHeight() * 3;
		GridDataFactory.fillDefaults().grab(true, true)
				.hint(size).minSize(size.x, minHeight)
				.align(SWT.FILL, SWT.FILL).applyTo(messageArea);

		previewArea = new Composite(commitMessageTextComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(previewArea);
		previewArea.setLayout(new FillLayout());
		previewArea.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TEXT_BORDER);
		previewer = new CommitMessagePreviewer();
		previewer.createControl(previewArea);

		previewLayout.topControl = messageArea;
		messageArea.setFocus();

		// Create two hidden text fields for author and committer, set to the
		// current user, so that we can use the CommitMessageComponent. The
		// committer is needed for the sign-off button; the author won't be
		// used.

		Text hiddenAuthor = new Text(composite, SWT.NONE);
		GridDataFactory.fillDefaults().exclude(true).applyTo(hiddenAuthor);
		hiddenAuthor.setVisible(false);
		Text hiddenCommitter = new Text(composite, SWT.NONE);
		GridDataFactory.fillDefaults().exclude(true).applyTo(hiddenCommitter);
		hiddenCommitter.setVisible(false);

		UserConfig cfg = config.get(UserConfig.KEY);
		String person = cfg.getCommitterName() + " <" + cfg.getCommitterEmail() //$NON-NLS-1$
				+ '>';
		hiddenAuthor.setText(person);
		hiddenCommitter.setText(person);

		commitComponent = new CommitMessageComponent(repository,
				new ICommitMessageComponentNotifications() {

					@Override
					public void updateSignedOffToggleSelection(
							boolean selection) {
						signOffAction.setChecked(selection);
					}

					@Override
					public void updateSignCommitToggleSelection(
							boolean selection) {
						// Ignore
					}

					@Override
					public void updateChangeIdToggleSelection(
							boolean selection) {
						addChangeIdAction.setChecked(selection);
					}

					@Override
					public void statusUpdated() {
						// Ignore
					}
				});
		commitComponent.attachControls(messageArea, hiddenAuthor,
				hiddenCommitter);
		commitComponent.updateSignedOffAndChangeIdButton();

		return composite;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = settings
				.getSection(DIALOG_SETTINGS_SECTION_NAME);
		if (section == null)
			section = settings.addNewSection(DIALOG_SETTINGS_SECTION_NAME);
		return section;
	}

	@Override
	protected void okPressed() {
		commitMessage = messageArea.getCommitMessage();
		useChangeId = addChangeIdAction.isChecked();
		super.okPressed();
	}

	/**
	 * @return the commit message
	 */
	public String getCommitMessage() {
		return commitMessage;
	}

	/**
	 * Tells whether a Gerrit Change-Id should be computed.
	 *
	 * @return {@code true} if a Change-Id should be computed; {@code false} otherwise
	 */
	public boolean isWithChangeId() {
		return useChangeId;
	}

	private boolean hasChangeIdFooter(String message) {
		int footer = CommonUtils.getFooterOffset(message);
		return footer > 0 && CHANGE_ID.matcher(message).find(footer);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				okButtonLabel, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				cancelButtonLabel, false);
	}
}
