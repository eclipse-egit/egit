/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.UIUtils.IPreviousValueProposalHandler;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.util.ChangeIdUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.internal.IWorkbenchGraphicConstants;
import org.eclipse.ui.internal.WorkbenchImages;

/**
 * Section containing commit message, author, and committer controls.
 */
public class CommitMesageSection {

	/**
	 * Signed off default
	 */
	protected boolean signedOff = org.eclipse.egit.ui.Activator.getDefault()
			.getPreferenceStore()
			.getBoolean(UIPreferences.COMMIT_DIALOG_SIGNED_OFF_BY);

	/**
	 * Amending commit default
	 */
	protected boolean amending = false;

	/**
	 * Amending allowed default
	 */
	protected boolean amendAllowed = true;

	/**
	 * Create change id default
	 */
	protected boolean createChangeIdDefault = false;

	/**
	 * Create change id
	 */
	protected boolean createChangeId = false;

	/**
	 * Commit message
	 */
	protected String commitMessage = ""; //$NON-NLS-1$

	/**
	 * Previous commit message
	 */
	protected String previousCommitMessage = ""; //$NON-NLS-1$

	/**
	 * Original change id
	 */
	protected ObjectId originalChangeId;

	/**
	 * Author
	 */
	protected String author = null;

	/**
	 * Previous author
	 */
	protected String previousAuthor = null;

	/**
	 * Committer
	 */
	protected String committer = null;

	/**
	 * Previous author preference key
	 */
	protected String previousAuthorKey = null;

	/**
	 * Previous committer preference key
	 */
	protected String previousCommitterKey = null;

	/**
	 * Message section
	 */
	protected Section section;

	/**
	 * Commit message text area
	 */
	protected SpellcheckableMessageArea commitText;

	/**
	 * Author text
	 */
	protected Text authorText;

	/**
	 * Committer text
	 */
	protected Text committerText;

	/**
	 * Amend previous commit toolbar item
	 */
	protected ToolItem amendingItem;

	/**
	 * Add signed off by line toolbar item
	 */
	protected ToolItem signedOffItem;

	/**
	 * Gerrit change id toolbar item
	 */
	protected ToolItem changeIdItem;

	/**
	 * Author content assist proposal handler
	 */
	protected IPreviousValueProposalHandler authorHandler;

	/**
	 * Committer content assist proposal handler
	 */
	protected IPreviousValueProposalHandler committerHandler;

	/**
	 * Create commit message section control as a child of the given parent.
	 *
	 * @param parent
	 * @param toolkit
	 */
	public void createControl(Composite parent, FormToolkit toolkit) {
		createControl(parent, toolkit, false);
	}

	/**
	 * Create commit message section control as a child of the given parent.
	 *
	 * @param parent
	 * @param toolkit
	 * @param enforceMinMessageSize
	 *            true to ensure that the message box has a minimum size and
	 *            hint
	 */
	public void createControl(Composite parent, FormToolkit toolkit,
			boolean enforceMinMessageSize) {
		toolkit.paintBordersFor(parent);
		Section messageSection = toolkit.createSection(parent,
				ExpandableComposite.TITLE_BAR
						| ExpandableComposite.CLIENT_INDENT);
		messageSection.setText(UIText.CommitDialog_CommitMessage);
		Composite messageArea = toolkit.createComposite(messageSection);
		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2)
				.applyTo(messageArea);
		toolkit.paintBordersFor(messageArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(messageSection);

		Composite headerArea = new Composite(messageSection, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(2)
				.applyTo(headerArea);

		ToolBar messageToolbar = new ToolBar(headerArea, SWT.FLAT
				| SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL)
				.grab(true, false).applyTo(messageToolbar);

		addMessageDropDown(headerArea);

		messageSection.setTextClient(headerArea);

		commitText = new SpellcheckableMessageArea(messageArea, "", SWT.NONE); //$NON-NLS-1$
		commitText
				.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		messageSection.setClient(messageArea);
		GridDataFactory commitTextFactory = GridDataFactory.fillDefaults()
				.grab(true, true);
		if (enforceMinMessageSize) {
			Point size = commitText.getTextWidget().getSize();
			int minHeight = commitText.getTextWidget().getLineHeight() * 3;
			commitTextFactory.hint(size).minSize(size.x, minHeight);
		}
		commitTextFactory.applyTo(commitText);

		Composite personArea = toolkit.createComposite(messageArea);
		toolkit.paintBordersFor(personArea);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(personArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(personArea);

		toolkit.createLabel(personArea, UIText.CommitDialog_Author)
				.setForeground(
						toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
		authorText = toolkit.createText(personArea, null);
		authorText
				.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		authorText.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());

		toolkit.createLabel(personArea, UIText.CommitDialog_Committer)
				.setForeground(
						toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
		committerText = toolkit.createText(personArea, null);
		committerText.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());

		amendingItem = new ToolItem(messageToolbar, SWT.CHECK);
		amendingItem.setToolTipText(UIText.CommitDialog_AmendPreviousCommit);
		Image amendImage = UIIcons.AMEND_COMMIT.createImage();
		UIUtils.hookDisposal(amendingItem, amendImage);
		amendingItem.setImage(amendImage);

		signedOffItem = new ToolItem(messageToolbar, SWT.CHECK);
		signedOffItem.setToolTipText(UIText.CommitDialog_AddSOB);
		Image signedOffImage = UIIcons.SIGNED_OFF.createImage();
		UIUtils.hookDisposal(signedOffItem, signedOffImage);
		signedOffItem.setImage(signedOffImage);

		changeIdItem = new ToolItem(messageToolbar, SWT.CHECK);
		Image changeIdImage = UIIcons.GERRIT.createImage();
		UIUtils.hookDisposal(changeIdItem, changeIdImage);
		changeIdItem.setImage(changeIdImage);
		changeIdItem.setToolTipText(UIText.CommitDialog_AddChangeIdLabel);

		initialize();
		addListeners();
	}

	private void addListeners() {
		committerText.addModifyListener(new ModifyListener() {
			String oldCommitter = committerText.getText();

			public void modifyText(ModifyEvent e) {
				if (signedOffItem.getSelection()) {
					// the commit message is signed
					// the signature must be updated
					String newCommitter = committerText.getText();
					String oldSignOff = getSignedOff(oldCommitter);
					String newSignOff = getSignedOff(newCommitter);
					commitText.setText(replaceSignOff(commitText.getText(),
							oldSignOff, newSignOff));
					oldCommitter = newCommitter;
				}
			}
		});
		commitText.getTextWidget().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateSignedOffButton();
				updateChangeIdButton();
			}
		});
		signedOffItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent arg0) {
				refreshSignedOffBy();
			}

		});
		changeIdItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				refreshChangeIdText();
			}

		});
		amendingItem.addSelectionListener(new SelectionAdapter() {
			boolean alreadyAdded = false;

			public void widgetSelected(SelectionEvent arg0) {
				if (!amendingItem.getSelection()) {
					originalChangeId = null;
					authorText.setText(author);
				} else {
					saveOriginalChangeId();
					if (!alreadyAdded) {
						alreadyAdded = true;
						commitText.setText(previousCommitMessage.replaceAll(
								"\n", Text.DELIMITER)); //$NON-NLS-1$
					}
					if (previousAuthor != null)
						authorText.setText(previousAuthor);
				}
				refreshChangeIdText();
			}
		});
	}

	private String replaceSignOff(String input, String oldSignOff,
			String newSignOff) {
		String curText = input;
		if (!curText.endsWith(Text.DELIMITER))
			curText += Text.DELIMITER;

		int indexOfSignOff = curText.indexOf(oldSignOff + Text.DELIMITER);
		if (indexOfSignOff == -1)
			return input;

		return input.substring(0, indexOfSignOff)
				+ newSignOff
				+ input.substring(indexOfSignOff + oldSignOff.length(),
						input.length());
	}

	private String getSignedOff() {
		return getSignedOff(committerText.getText());
	}

	private String getSignedOff(String signer) {
		return Constants.SIGNED_OFF_BY_TAG + signer;
	}

	private void updateSignedOffButton() {
		String curText = commitText.getText();
		if (!curText.endsWith(Text.DELIMITER))
			curText += Text.DELIMITER;

		signedOffItem.setSelection(curText.indexOf(getSignedOff()
				+ Text.DELIMITER) != -1);
	}

	private void updateChangeIdButton() {
		String curText = commitText.getText();
		if (!curText.endsWith(Text.DELIMITER))
			curText += Text.DELIMITER;

		boolean hasId = curText.indexOf(Text.DELIMITER + "Change-Id: ") != -1; //$NON-NLS-1$
		if (hasId) {
			changeIdItem.setSelection(true);
			createChangeId = true;
		}
	}

	private void refreshChangeIdText() {
		createChangeId = changeIdItem.getSelection();
		String text = commitText.getText().replaceAll(Text.DELIMITER, "\n"); //$NON-NLS-1$
		if (createChangeId) {
			String changedText = ChangeIdUtil.insertId(
					text,
					originalChangeId != null ? originalChangeId : ObjectId
							.zeroId(), true);
			if (!text.equals(changedText)) {
				changedText = changedText.replaceAll("\n", Text.DELIMITER); //$NON-NLS-1$
				commitText.setText(changedText);
			}
		} else {
			int changeIdOffset = findOffsetOfChangeIdLine(text);
			if (changeIdOffset > 0) {
				int endOfChangeId = findNextEOL(changeIdOffset, text);
				String cleanedText = text.substring(0, changeIdOffset)
						+ text.substring(endOfChangeId);
				cleanedText = cleanedText.replaceAll("\n", Text.DELIMITER); //$NON-NLS-1$
				commitText.setText(cleanedText);
			}
		}
	}

	private void refreshSignedOffBy() {
		String curText = commitText.getText();
		if (signedOffItem.getSelection()) {
			// add signed off line
			commitText.setText(signOff(curText));
		} else {
			// remove signed off line
			String s = getSignedOff();
			if (s != null) {
				curText = replaceSignOff(curText, s, ""); //$NON-NLS-1$
				if (curText.endsWith(Text.DELIMITER + Text.DELIMITER))
					curText = curText.substring(0, curText.length()
							- Text.DELIMITER.length());
				commitText.setText(curText);
			}
		}
	}

	private String signOff(String input) {
		String output = input;
		if (!output.endsWith(Text.DELIMITER))
			output += Text.DELIMITER;

		// if the last line is not footer line, add a line break
		if (!getLastLine(output).matches("[A-Za-z\\-]+:.*")) //$NON-NLS-1$
			output += Text.DELIMITER;
		output += getSignedOff();
		return output;
	}

	private String getLastLine(String input) {
		String output = input;
		int breakLength = Text.DELIMITER.length();

		// remove last line break if exist
		int lastIndexOfLineBreak = output.lastIndexOf(Text.DELIMITER);
		if (lastIndexOfLineBreak != -1
				&& lastIndexOfLineBreak == output.length() - breakLength)
			output = output.substring(0, output.length() - breakLength);

		// get the last line
		lastIndexOfLineBreak = output.lastIndexOf(Text.DELIMITER);
		return lastIndexOfLineBreak == -1 ? output : output.substring(
				lastIndexOfLineBreak + breakLength, output.length());
	}

	/**
	 * Initialize section with the defaults set
	 */
	public void initialize() {
		if (author != null)
			authorText.setText(author);
		if (committer != null)
			committerText.setText(committer);

		if (amending) {
			amendingItem.setSelection(amending);
			// if already set, don't allow any changes
			amendingItem.setEnabled(false);
			authorText.setText(previousAuthor);
			saveOriginalChangeId();
		} else if (!amendAllowed) {
			amendingItem.setEnabled(false);
			originalChangeId = null;
		}

		changeIdItem.setSelection(createChangeIdDefault);

		if (!amending)
			refreshChangeIdText();

		updateSignedOffButton();
		updateChangeIdButton();

		if (previousCommitterKey != null)
			committerHandler = UIUtils.addPreviousValuesContentProposalToText(
					committerText, previousCommitterKey);

		if (previousAuthorKey != null)
			authorHandler = UIUtils.addPreviousValuesContentProposalToText(
					authorText, previousAuthorKey);

		commitText.setText(calculateCommitMessage());
	}

	/**
	 * @return the calculated commit message
	 */
	private String calculateCommitMessage() {
		if (amending)
			return previousCommitMessage;
		else
			return commitMessage;
	}

	private void saveOriginalChangeId() {
		int changeIdOffset = findOffsetOfChangeIdLine(previousCommitMessage);
		if (changeIdOffset > 0) {
			int endOfChangeId = findNextEOL(changeIdOffset,
					previousCommitMessage);
			if (endOfChangeId < 0)
				endOfChangeId = previousCommitMessage.length() - 1;
			int sha1Offset = changeIdOffset + "\nChange-Id: I".length(); //$NON-NLS-1$
			try {
				originalChangeId = ObjectId.fromString(previousCommitMessage
						.substring(sha1Offset, endOfChangeId));
			} catch (IllegalArgumentException e) {
				originalChangeId = null;
			}
		} else
			originalChangeId = null;
	}

	private int findNextEOL(int oldPos, String message) {
		return message.indexOf("\n", oldPos + 1); //$NON-NLS-1$
	}

	private int findOffsetOfChangeIdLine(String message) {
		return message.indexOf("\nChange-Id: I"); //$NON-NLS-1$
	}

	private String nonNull(String string) {
		return string != null ? string : ""; //$NON-NLS-1$
	}

	/**
	 * Add message drop down toolbar item
	 *
	 * @param parent
	 * @return toolbar
	 */
	protected ToolBar addMessageDropDown(Composite parent) {
		final ToolBar dropDownBar = new ToolBar(parent, SWT.FLAT | SWT.RIGHT);
		final ToolItem dropDownItem = new ToolItem(dropDownBar, SWT.PUSH);
		dropDownItem
				.setImage(WorkbenchImages
						.getImage(IWorkbenchGraphicConstants.IMG_LCL_RENDERED_VIEW_MENU));
		dropDownItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				Menu menu = new Menu(dropDownBar);
				MenuItem preferencesItem = new MenuItem(menu, SWT.PUSH);
				preferencesItem.setText(UIText.CommitDialog_ConfigureLink);
				preferencesItem.addSelectionListener(new SelectionAdapter() {

					public void widgetSelected(SelectionEvent e1) {
						PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
								section.getShell(),
								UIPreferences.PAGE_COMMIT_PREFERENCES,
								new String[] { UIPreferences.PAGE_COMMIT_PREFERENCES },
								null);
						if (Window.OK == dialog.open())
							commitText.reconfigure();
					}

				});

				Rectangle b = dropDownItem.getBounds();
				Point p = dropDownItem.getParent().toDisplay(
						new Point(b.x, b.y + b.height));
				menu.setLocation(p.x, p.y);
				menu.setVisible(true);
			}

		});
		return dropDownBar;
	}

	/**
	 * @return The author to set for the commit
	 */
	public String getAuthor() {
		return authorText.getText().trim();
	}

	/**
	 * Pre-set author for the commit
	 *
	 * @param author
	 */
	public void setAuthor(String author) {
		if (authorText != null)
			authorText.setText(nonNull(author));
		else
			this.author = author;
	}

	/**
	 * @return The committer to set for the commit
	 */
	public String getCommitter() {
		return committerText.getText().trim();
	}

	/**
	 * Set committer for the commit
	 *
	 * @param committer
	 */
	public void setCommitter(String committer) {
		if (committerText != null)
			committerText.setText(nonNull(committer));
		else
			this.committer = committer;
	}

	/**
	 * Set the previous author if amending the commit
	 *
	 * @param previousAuthor
	 */
	public void setPreviousAuthor(String previousAuthor) {
		this.previousAuthor = previousAuthor;
	}

	/**
	 * @return whether to auto-add a signed-off line to the message
	 */
	public boolean isSignedOff() {
		return signedOffItem.getSelection();
	}

	/**
	 * Set whether a signed-off line should be included in the commit message.
	 *
	 * @param signedOff
	 */
	public void setSignedOff(boolean signedOff) {
		if (signedOffItem != null)
			signedOffItem.setSelection(signedOff);
		else
			this.signedOff = signedOff;
	}

	/**
	 * @return whether the last commit is to be amended
	 */
	public boolean isAmending() {
		return amendingItem.getSelection();
	}

	/**
	 * Set whether the last commit is going to be amended
	 *
	 * @param amending
	 */
	public void setAmending(boolean amending) {
		if (amendingItem != null)
			amendingItem.setSelection(amending);
		else
			this.amending = amending;
	}

	/**
	 * Set commit message
	 *
	 * @param message
	 */
	public void setMessage(String message) {
		if (commitText != null)
			commitText.setText(nonNull(message));
		else
			commitMessage = nonNull(message);
	}

	/**
	 * Set the message from the previous commit for amending.
	 *
	 * @param string
	 */
	public void setPreviousCommitMessage(String string) {
		this.previousCommitMessage = string;
	}

	/**
	 * Set whether the previous commit may be amended
	 *
	 * @param amendAllowed
	 */
	public void setAmendAllowed(boolean amendAllowed) {
		this.amendAllowed = amendAllowed;
	}

	/**
	 * @param previousAuthorKey
	 */
	public void setPreviousAuthorKey(String previousAuthorKey) {
		this.previousAuthorKey = previousAuthorKey;
	}

	/**
	 * @param previousCommitterKey
	 */
	public void setPreviousCommitterKey(String previousCommitterKey) {
		this.previousCommitterKey = previousCommitterKey;
	}

	/**
	 * @param create
	 */
	public void setCreateChangeIdDefault(boolean create) {
		this.createChangeIdDefault = create;
	}

	/**
	 * Get commit message area widget
	 *
	 * @return message area
	 */
	public SpellcheckableMessageArea getMessageArea() {
		return this.commitText;
	}

	/**
	 * Set whether or not the amend tool item is enabled. Must be called after
	 * {@link #createControl(Composite, FormToolkit)} is called.
	 *
	 * @param enabled
	 */
	public void setAmendEnabled(boolean enabled) {
		amendingItem.setEnabled(enabled);
		if (!enabled)
			amendingItem.setSelection(false);
	}

	/**
	 * Updates author and committer proposals
	 */
	public void updateProposals() {
		if (authorHandler != null)
			authorHandler.updateProposals();
		if (committerHandler != null)
			committerHandler.updateProposals();
	}

}
