/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com.dewire.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.ICommitMessageProvider;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.UIUtils.IPreviousValueProposalHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.ChangeIdUtil;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog is shown to user when they request to commit files. Changes in the
 * selected portion of the tree are shown.
 */
public class CommitMessageComponent {

	/**
	* Constant for the extension point for the commit message provider
	*/
	private static final String COMMIT_MESSAGE_PROVIDER_ID = "org.eclipse.egit.ui.commitMessageProvider"; //$NON-NLS-1$

	private static final String COMMITTER_VALUES_PREF = "CommitDialog.committerValues"; //$NON-NLS-1$

	private static final String AUTHOR_VALUES_PREF = "CommitDialog.authorValues"; //$NON-NLS-1$

	ICommitMessageComponentNotifications listener;

	SpellcheckableMessageArea commitText;

	Text authorText;

	Text committerText;

	boolean signedOffButtonSelection;

	boolean changeIdButtonSelection;

	Button showUntrackedButton;

	ObjectId originalChangeId;

	private String commitMessage = null;

	private String previousCommitMessage = ""; //$NON-NLS-1$

	private String author = null;

	private String previousAuthor = null;

	private String committer = null;

	private boolean signedOff = org.eclipse.egit.ui.Activator.getDefault()
	.getPreferenceStore()
	.getBoolean(UIPreferences.COMMIT_DIALOG_SIGNED_OFF_BY);

	private boolean amending = false;

	private boolean createChangeId = false;

	private IPreviousValueProposalHandler authorHandler;

	private IPreviousValueProposalHandler committerHandler;

	private Repository repository;

	/**
	 * @param repository
	 * @param listener
	 */
	public CommitMessageComponent(Repository repository, ICommitMessageComponentNotifications listener) {
		this.repository = repository;
		this.listener = listener;
	}

	/**
	 * @return The message the user entered
	 */
	public String getCommitMessage() {
		commitMessage = commitText.getCommitMessage();
		return commitMessage;
	}

	/**
	 * Preset a commit message. This might be for amending a commit.
	 * @param s the commit message
	 */
	public void setCommitMessage(String s) {
		this.commitMessage = s;
	}

	/**
	 * @return The author to set for the commit
	 */
	public String getAuthor() {
		author = authorText.getText().trim();
		return author;
	}

	/**
	 * Pre-set author for the commit
	 *
	 * @param author
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * @return The committer to set for the commit
	 */
	public String getCommitter() {
		committer = committerText.getText().trim();
		return committer;
	}

	/**
	 * Pre-set committer for the commit
	 *
	 * @param committer
	 */
	public void setCommitter(String committer) {
		this.committer = committer;
	}

	/**
	 * Pre-set the previous author if amending the commit
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
		return signedOff;
	}

	/**
	 * Pre-set whether a signed-off line should be included in the commit
	 * message.
	 *
	 * @param signedOff
	 */
	public void setSignedOff(boolean signedOff) {
		this.signedOff = signedOff;
	}

	/**
	 * @return whether the last commit is to be amended
	 */
	public boolean isAmending() {
		return amending;
	}

	/**
	 * Pre-set whether the last commit is going to be amended
	 *
	 * @param amending
	 */
	public void setAmending(boolean amending) {
		this.amending = amending;
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
	 * @return true if a Change-Id line for Gerrit should be created
	 */
	public boolean getCreateChangeId() {
		return createChangeId;
	}

	/**
	 *
	 */
	public void okPressed() {
		commitMessage = commitText.getCommitMessage();
		author = authorText.getText().trim();
		committer = committerText.getText().trim();
		signedOff = signedOffButtonSelection;
		// amending = amendingButton.getSelection();

		if (commitMessage.trim().length() == 0) {
			MessageDialog.openWarning(getShell(), UIText.CommitDialog_ErrorNoMessage, UIText.CommitDialog_ErrorMustEnterCommitMessage);
			return;
		}

		boolean authorValid = false;
		if (author.length() > 0) {
			authorValid = RawParseUtils.parsePersonIdent(author) != null;
		}
		if (!authorValid) {
			MessageDialog.openWarning(getShell(), UIText.CommitDialog_ErrorInvalidAuthor, UIText.CommitDialog_ErrorInvalidAuthorSpecified);
			return;
		}

		boolean committerValid = false;
		if (committer.length() > 0) {
			committerValid = RawParseUtils.parsePersonIdent(committer)!=null;
		}
		if (!committerValid) {
			MessageDialog.openWarning(getShell(), UIText.CommitDialog_ErrorInvalidAuthor, UIText.CommitDialog_ErrorInvalidCommitterSpecified);
			return;
		}

		authorHandler.updateProposals();
		committerHandler.updateProposals();
	}

	/**
	 * @return true if commit info is ok
	 */
	public boolean checkCommitInfo() {
		commitMessage = commitText.getCommitMessage();
		author = authorText.getText().trim();
		committer = committerText.getText().trim();
		signedOff = signedOffButtonSelection;

		if (commitMessage.trim().length() == 0) {
			MessageDialog.openWarning(getShell(), UIText.CommitDialog_ErrorNoMessage, UIText.CommitDialog_ErrorMustEnterCommitMessage);
			return false;
		}

		boolean authorValid = false;
		if (author.length() > 0) {
			authorValid = RawParseUtils.parsePersonIdent(author) != null;
		}
		if (!authorValid) {
			MessageDialog.openWarning(getShell(), UIText.CommitDialog_ErrorInvalidAuthor, UIText.CommitDialog_ErrorInvalidAuthorSpecified);
			return false;
		}

		boolean committerValid = false;
		if (committer.length() > 0) {
			committerValid = RawParseUtils.parsePersonIdent(committer)!=null;
		}
		if (!committerValid) {
			MessageDialog.openWarning(getShell(), UIText.CommitDialog_ErrorInvalidAuthor, UIText.CommitDialog_ErrorInvalidCommitterSpecified);
			return false;
		}

		authorHandler.updateProposals();
		committerHandler.updateProposals();
		return true;
	}


	/**
	 * @param commitText
	 * @param authorText
	 * @param committerText
	 */
	public void attachControls(SpellcheckableMessageArea commitText, Text authorText, Text committerText) {

		this.commitText = commitText;
		this.authorText = authorText;
		this.committerText = committerText;
		addListeners();
	}

	private void addListeners() {
		commitText.setText(calculateCommitMessage(new ArrayList<String>())); // TODO extract mylyn stuff to class?

		// allow to commit with ctrl-enter
		commitText.getTextWidget().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.keyCode == SWT.CR
						&& (event.stateMask & SWT.CONTROL) > 0) {
					// okPressed(); TODO implement callback
				} else if (event.keyCode == SWT.TAB
						&& (event.stateMask & SWT.SHIFT) == 0) {
					event.doit = false;
					commitText.traverse(SWT.TRAVERSE_TAB_NEXT);
				}
			}
		});

		if (author != null)
			authorText.setText(author);

		authorHandler = UIUtils.addPreviousValuesContentProposalToText(authorText, AUTHOR_VALUES_PREF);
		if (committer != null)
			committerText.setText(committer);
		committerText.addModifyListener(new ModifyListener() {
			String oldCommitter = committerText.getText();
			public void modifyText(ModifyEvent e) {
				if (signedOffButtonSelection) {
					// the commit message is signed
					// the signature must be updated
					String newCommitter = committerText.getText();
					String oldSignOff = getSignedOff(oldCommitter);
					String newSignOff = getSignedOff(newCommitter);
					commitText.setText(replaceSignOff(commitText.getText(), oldSignOff, newSignOff));
					oldCommitter = newCommitter;
				}
			}
		});

		committerHandler = UIUtils.addPreviousValuesContentProposalToText(committerText, COMMITTER_VALUES_PREF);


		if (amending) {
			authorText.setText(previousAuthor);
			saveOriginalChangeId();
		}

		if (!amending)
			refreshSignedOffBy();

		if (!amending)
			refreshChangeIdText();

		commitText.getTextWidget().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateSignedOffButton();
				updateChangeIdButton();
			}
		});

		updateSignedOffButton();
		updateChangeIdButton();

	}

	/**
	 *
	 */
	public void updateFields() {
		commitText.setText(commitMessage == null ? "": commitMessage);  // TODO mylyn stuff //$NON-NLS-1$
		authorText.setText(author);
		committerText.setText(committer);
	}

	private Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	/**
	 * @param paths
	 * @return the calculated commit message
	 */
	private String calculateCommitMessage(Collection<String> paths) {
		if(commitMessage != null) {
			// special case for merge
			return commitMessage;
		}

		if (amending)
			return previousCommitMessage;
		String calculatedCommitMessage = null;

		Set<IResource> resources = new HashSet<IResource>();
		for (String path:paths) {
			IFile file = findFile(path);
			if (file != null)
				resources.add(file.getProject());
		}
		try {
			ICommitMessageProvider messageProvider = getCommitMessageProvider();
			if(messageProvider != null) {
				IResource[] resourcesArray = resources.toArray(new IResource[0]);
				calculatedCommitMessage = messageProvider.getMessage(resourcesArray);
			}
		} catch (CoreException coreException) {
			Activator.error(coreException.getLocalizedMessage(),
					coreException);
		}
		if (calculatedCommitMessage != null)
			return calculatedCommitMessage;
		else
			return ""; //$NON-NLS-1$
	}

	private ICommitMessageProvider getCommitMessageProvider()
			throws CoreException {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] config = registry
				.getConfigurationElementsFor(COMMIT_MESSAGE_PROVIDER_ID);
		if (config.length > 0) {
			Object provider;
			provider = config[0].createExecutableExtension("class");//$NON-NLS-1$
			if (provider instanceof ICommitMessageProvider) {
				return (ICommitMessageProvider) provider;
			} else {
				Activator.logError(UIText.CommitDialog_WrongTypeOfCommitMessageProvider,
						null);
			}
		}
		return null;
	}

	private void saveOriginalChangeId() {
		int changeIdOffset = findOffsetOfChangeIdLine(previousCommitMessage);
		if (changeIdOffset > 0) {
			int endOfChangeId = findNextEOL(changeIdOffset, previousCommitMessage);
			if (endOfChangeId < 0)
				endOfChangeId = previousCommitMessage.length()-1;
			int sha1Offset = changeIdOffset + "\nChange-Id: I".length(); //$NON-NLS-1$
			try {
				originalChangeId = ObjectId.fromString(previousCommitMessage.substring(sha1Offset, endOfChangeId));
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

	private void updateChangeIdButton() {
		String curText = commitText.getText();
		if (!curText.endsWith(Text.DELIMITER))
			curText += Text.DELIMITER;

		boolean hasId = curText.indexOf(Text.DELIMITER + "Change-Id: ") != -1; //$NON-NLS-1$
		if (hasId) {
			listener.updateChangeIdToggleSelection(true);
			createChangeId = true;
		}
	}

	private void refreshChangeIdText() {
		createChangeId = changeIdButtonSelection;
		String text = commitText.getText().replaceAll(Text.DELIMITER, "\n"); //$NON-NLS-1$
		if (createChangeId) {
			String changedText = ChangeIdUtil.insertId(text,
					originalChangeId != null ? originalChangeId : ObjectId.zeroId(), true);
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

	private String getSignedOff() {
		return getSignedOff(committerText.getText());
	}

	private String getSignedOff(String signer) {
		return Constants.SIGNED_OFF_BY_TAG + signer;
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
		if (lastIndexOfLineBreak != -1 && lastIndexOfLineBreak == output.length() - breakLength)
			output = output.substring(0, output.length() - breakLength);

		// get the last line
		lastIndexOfLineBreak = output.lastIndexOf(Text.DELIMITER);
		return lastIndexOfLineBreak == -1 ? output : output.substring(lastIndexOfLineBreak + breakLength, output.length());
	}

	private void updateSignedOffButton() {
		String curText = commitText.getText();
		if (!curText.endsWith(Text.DELIMITER))
			curText += Text.DELIMITER;

		listener.updateSignedOffToggleSelection(curText.indexOf(getSignedOff() + Text.DELIMITER) != -1);
	}

	private void refreshSignedOffBy() {
		String curText = commitText.getText();
		if (signedOffButtonSelection) {
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

	private String replaceSignOff(String input, String oldSignOff, String newSignOff) {
		assert input != null;
		assert oldSignOff != null;
		assert newSignOff != null;

		String curText = input;
		if (!curText.endsWith(Text.DELIMITER))
			curText += Text.DELIMITER;

		int indexOfSignOff = curText.indexOf(oldSignOff + Text.DELIMITER);
		if (indexOfSignOff == -1)
			return input;

		return input.substring(0, indexOfSignOff) + newSignOff + input.substring(indexOfSignOff + oldSignOff.length(), input.length());
	}

	// TODO: move to utils
	private IFile findFile(String path) {
		URI uri = new File(repository.getWorkTree(), path).toURI();
		IFile[] workspaceFiles = ResourcesPlugin.getWorkspace().getRoot()
				.findFilesForLocationURI(uri);
		if (workspaceFiles.length > 0)
			return workspaceFiles[0];
		else
			return null;
	}

	/**
	 * @param signedOffButtonSelection
	 */
	public void setSignedOffButtonSelection(boolean signedOffButtonSelection) {
		this.signedOffButtonSelection = signedOffButtonSelection;
		refreshSignedOffBy();
	}

	/**
	 * @param selection
	 */
	public void setAmendingButtonSelection(boolean selection) {
		if (!selection) {
			originalChangeId = null;
			authorText.setText(author);
		}
		else {
			saveOriginalChangeId();
			commitText.setText(previousCommitMessage.replaceAll(
						"\n", Text.DELIMITER)); //$NON-NLS-1$
			if (previousAuthor != null)
				authorText.setText(previousAuthor);
		}
		refreshChangeIdText();
	}

	/**
	 * @param selection
	 *
	 */
	public void setChangeIdButtonSelection(boolean selection) {
		changeIdButtonSelection = selection;
		refreshChangeIdText();
	}

}
