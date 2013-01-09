/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com.dewire.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2012, IBM Corporation (Markus Keller <markus_keller@ch.ibm.com>)
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.File;
import java.io.IOException;
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
import org.eclipse.egit.core.RevUtils;
import org.eclipse.egit.ui.ICommitMessageProvider;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.UIUtils.IPreviousValueProposalHandler;
import org.eclipse.egit.ui.internal.commit.CommitHelper;
import org.eclipse.egit.ui.internal.commit.CommitHelper.CommitInfo;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.ChangeIdUtil;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * This class provides a reusable UI component for the UI logic around commit
 * message, author, committer, signed off toggle, amend toggle and change id
 * toggle. Controls for commit message, author and committer are created by the
 * component host and attached via method <code>attachControls</code>. The
 * toggles (signed off, amend, change id) are provided by the host and can be of
 * any widget type (check box, tool bar item etc.). The host must notify the
 * commit message component when a toggle state changes by calling the methods
 * <code>setSignedOffButtonSelection</code>,
 * <code>setChangeIdButtonSelection</code> and
 * <code>setAmendingButtonSelection</code>. The component notifies the host via
 * interface {@link ICommitMessageComponentNotifications} about required changes
 * of the toggle selections.
 */
public class CommitMessageComponent {

	/**
	 * Status provider for whether a commit operation should be enabled or not
	 */
	public static class CommitStatus implements IMessageProvider {

		private static final CommitStatus OK = new CommitStatus();

		private final String message;

		private final int type;

		private CommitStatus() {
			message = null;
			type = NONE;
		}

		private CommitStatus(String message, int type) {
			this.message = message;
			this.type = type;
		}

		public String getMessage() {
			return message;
		}

		public int getMessageType() {
			return type;
		}
	}


	private static final String EMPTY_STRING = "";  //$NON-NLS-1$

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

	ObjectId originalChangeId;

	private String commitMessage = null;

	private String commitMessageBeforeAmending = EMPTY_STRING;

	private String previousCommitMessage = EMPTY_STRING;

	private String author = null;

	private String previousAuthor = null;

	private String committer = null;

	private boolean signedOff = false;

	private boolean amending = false;

	private boolean commitAllowed = true;

	private String cannotCommitMessage = null;

	private boolean amendAllowed = false;

	private boolean amendingCommitInRemoteBranch = false;

	private boolean createChangeId = false;

	private IPreviousValueProposalHandler authorHandler;

	private IPreviousValueProposalHandler committerHandler;

	private Repository repository;

	private Collection<String> filesToCommit = new ArrayList<String>();

	private ObjectId headCommitId;

	private boolean listersEnabled;

	/**
	 * @param repository
	 * @param listener
	 */
	public CommitMessageComponent(Repository repository,
			ICommitMessageComponentNotifications listener) {
		this.repository = repository;
		this.listener = listener;
	}

	/**
	 * @param listener
	 */
	public CommitMessageComponent(ICommitMessageComponentNotifications listener) {
		this.listener = listener;
	}

	/**
	 * Resets all state
	 */
	public void resetState() {
		originalChangeId = null;
		commitMessage = null;
		commitMessageBeforeAmending = EMPTY_STRING;
		previousCommitMessage = EMPTY_STRING;
		author = null;
		previousAuthor = null;
		committer = null;
		signedOff = false;
		amending = false;
		amendAllowed = false;
		createChangeId = false;
		filesToCommit = new ArrayList<String>();
		headCommitId = null;
		listersEnabled = false;
	}

	/**
	 * Returns the commit message, converting platform-specific line endings to
	 * '\n' and hard-wrapping lines if necessary.
	 *
	 * @return the message
	 */
	public String getCommitMessage() {
		commitMessage = commitText.getCommitMessage();
		return commitMessage;
	}

	/**
	 * Preset a commit message. This might be for amending a commit.
	 *
	 * @param s
	 *            the commit message
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
	 * @param signedOff
	 */
	public void setSignedOff(boolean signedOff) {
		this.signedOff = signedOff;
	}

	/**
	 * @param createChangeId
	 */
	public void setCreateChangeId(boolean createChangeId) {
		this.createChangeId = createChangeId;
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
	 * @param filesToCommit
	 */
	public void setFilesToCommit(Collection<String> filesToCommit) {
		this.filesToCommit = filesToCommit;
	}

	/**
	 * @return whether to auto-add a signed-off line to the message
	 */
	public boolean isSignedOff() {
		return signedOff;
	}

	/**
	 * @return whether the last commit is to be amended
	 */
	public boolean isAmending() {
		return amending;
	}

	/**
	 * Set whether the last commit is going to be amended
	 *
	 * @param amending
	 */
	public void setAmending(boolean amending) {
		this.amending = amending;
	}


	/**
	 * Set whether commit is allowed at the moment.
	 *
	 * @param commitAllowed
	 */
	public void setCommitAllowed(boolean commitAllowed) {
		this.commitAllowed = commitAllowed;
	}

	/**
	 * Set the message to be shown about why the commit is not allowed.
	 *
	 * @param cannotCommitMessage
	 */
	public void setCannotCommitMessage(String cannotCommitMessage) {
		this.cannotCommitMessage = cannotCommitMessage;
	}

	/**
	 * Set whether the previous commit may be amended
	 *
	 * @param amendAllowed
	 */
	public void setAmendAllowed(boolean amendAllowed) {
		this.amendAllowed = amendAllowed;
		commitMessageBeforeAmending = EMPTY_STRING;
	}

	/**
	 * @param selection
	 */
	public void setAmendingButtonSelection(boolean selection) {
		amending = selection;
		if (!selection) {
			originalChangeId = null;
			authorText.setText(author);
			commitText.setText(commitMessageBeforeAmending);
			commitMessageBeforeAmending = EMPTY_STRING;
		} else {
			getHeadCommitInfo();
			saveOriginalChangeId();
			commitMessageBeforeAmending = commitText.getText();
			commitText.setText(previousCommitMessage);
			if (previousAuthor != null)
				authorText.setText(previousAuthor);
		}
		refreshChangeIdText();
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
	public void updateStateFromUI() {
		commitMessage = commitText.getText();
		author = authorText.getText().trim();
		committer = committerText.getText().trim();
	}

	/**
	 *
	 */
	public void updateUIFromState() {
		commitText.setText(commitMessage);
		authorText.setText(author);
		committerText.setText(committer);
	}

	/**
	 * @return state
	 */
	public CommitMessageComponentState getState() {
		updateStateFromUI();
		CommitMessageComponentState state = new CommitMessageComponentState();
		state.setAmend(isAmending());
		state.setAuthor(getAuthor());
		// store text with platform specific line endings
		state.setCommitMessage(commitText.getText());
		state.setCommitter(getCommitter());
		state.setHeadCommit(getHeadCommit());
		return state;
	}

	/**
	 * Disable listeners on commit message editor and committer text
	 * to change data programmatically.
	 * @param enable
	 */
	public void enableListers(boolean enable) {
		this.listersEnabled = enable;
	}

	/**
	 * Get the status of whether the commit operation should be enabled or
	 * disabled.
	 * <p>
	 * This method checks the current state of the widgets and must always be
	 * called from the UI-thread.
	 * <p>
	 * The returned status includes a message and type denoting why committing
	 * cannot be completed.
	 *
	 * @return non-null commit status
	 */
	public CommitStatus getStatus() {
		if (!commitAllowed)
			return new CommitStatus(cannotCommitMessage, IMessageProvider.ERROR);

		String authorValue = authorText.getText();
		if (authorValue.length() == 0
				|| RawParseUtils.parsePersonIdent(authorValue) == null)
			return new CommitStatus(
					UIText.CommitMessageComponent_MessageInvalidAuthor,
					IMessageProvider.ERROR);

		String committerValue = committerText.getText();
		if (committerValue.length() == 0
				|| RawParseUtils.parsePersonIdent(committerValue) == null) {
			return new CommitStatus(
					UIText.CommitMessageComponent_MessageInvalidCommitter,
					IMessageProvider.ERROR);
		}

		if (amending && amendingCommitInRemoteBranch)
			return new CommitStatus(
					UIText.CommitMessageComponent_AmendingCommitInRemoteBranch,
					IMessageProvider.WARNING);

		return CommitStatus.OK;
	}

	/**
	 * @return true if commit info is ok
	 */
	public boolean checkCommitInfo() {
		updateStateFromUI();

		if (commitMessage.trim().length() == 0) {
			MessageDialog.openWarning(getShell(),
					UIText.CommitDialog_ErrorNoMessage,
					UIText.CommitDialog_ErrorMustEnterCommitMessage);
			return false;
		}

		boolean authorValid = false;
		if (author.length() > 0)
			authorValid = RawParseUtils.parsePersonIdent(author) != null;
		if (!authorValid) {
			MessageDialog.openWarning(getShell(),
					UIText.CommitDialog_ErrorInvalidAuthor,
					UIText.CommitDialog_ErrorInvalidAuthorSpecified);
			return false;
		}

		boolean committerValid = false;
		if (committer.length() > 0)
			committerValid = RawParseUtils.parsePersonIdent(committer) != null;
		if (!committerValid) {
			MessageDialog.openWarning(getShell(),
					UIText.CommitDialog_ErrorInvalidAuthor,
					UIText.CommitDialog_ErrorInvalidCommitterSpecified);
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
	public void attachControls(SpellcheckableMessageArea commitText,
			Text authorText, Text committerText) {
		this.commitText = commitText;
		this.authorText = authorText;
		this.committerText = committerText;
		addListeners();
	}

	private void addListeners() {
		authorHandler = UIUtils.addPreviousValuesContentProposalToText(
				authorText, AUTHOR_VALUES_PREF);
		committerText.addModifyListener(new ModifyListener() {
			String oldCommitter = committerText.getText();

			public void modifyText(ModifyEvent e) {
				if (!listersEnabled)
					return;
				if (signedOff) {
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
		committerHandler = UIUtils.addPreviousValuesContentProposalToText(
				committerText, COMMITTER_VALUES_PREF);
		commitText.getTextWidget().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!listersEnabled)
					return;
				updateSignedOffButton();
				updateChangeIdButton();
			}
		});
	}

	/**
	 * Sets the defaults for change id and signed off
	 */
	public void setDefaults() {
		createChangeId = repository.getConfig().getBoolean(
				ConfigConstants.CONFIG_GERRIT_SECTION,
				ConfigConstants.CONFIG_KEY_CREATECHANGEID, false);
		signedOff = org.eclipse.egit.ui.Activator.getDefault()
		.getPreferenceStore()
		.getBoolean(UIPreferences.COMMIT_DIALOG_SIGNED_OFF_BY);
	}

	/**
	 * Initial UI update
	 */
	public void updateUI() {
		if (amending)
			getHeadCommitInfo();

		commitText.setText(calculateCommitMessage(filesToCommit));
		authorText.setText(getSafeString(author));
		committerText.setText(getSafeString(committer));
		if (amending) {
			authorText.setText(previousAuthor);
			saveOriginalChangeId();
		} else {
			if (!amendAllowed) {
				originalChangeId = null;
			}
			refreshSignedOffBy();
			refreshChangeIdText();
		}
		updateSignedOffButton();
		updateChangeIdButton();
	}

	/**
	 * update signed off and change id button from the
	 * commit message
	 */
	public void updateSignedOffAndChangeIdButton() {
		updateSignedOffButton();
		updateChangeIdButton();
	}

	private void getHeadCommitInfo() {
		CommitInfo headCommitInfo = CommitHelper.getHeadCommitInfo(repository);
		RevCommit previousCommit = headCommitInfo.getCommit();

		amendingCommitInRemoteBranch = isContainedInAnyRemoteBranch(previousCommit);
		previousCommitMessage = headCommitInfo.getCommitMessage();
		previousAuthor = headCommitInfo.getAuthor();
	}

	private boolean isContainedInAnyRemoteBranch(RevCommit commit) {
		try {
			Collection<Ref> refs = repository.getRefDatabase().getRefs(
					Constants.R_REMOTES).values();
			return RevUtils.isContainedInAnyRef(repository, commit, refs);
		} catch (IOException e) {
			// The result only affects a warning, so pretend there was no
			// problem.
			return false;
		}
	}

	private String getSafeString(String string) {
		if (string == null)
			return EMPTY_STRING;
		return string;
	}

	private Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	/**
	 * @param paths
	 * @return the calculated commit message
	 */
	private String calculateCommitMessage(Collection<String> paths) {
		if (commitMessage != null) {
			// special case for merge
			return commitMessage;
		}

		if (amending)
			return previousCommitMessage;
		String calculatedCommitMessage = null;

		Set<IResource> resources = new HashSet<IResource>();
		for (String path : paths) {
			IFile file = findFile(path);
			if (file != null)
				resources.add(file.getProject());
		}
		try {
			ICommitMessageProvider messageProvider = getCommitMessageProvider();
			if (messageProvider != null) {
				IResource[] resourcesArray = resources
						.toArray(new IResource[0]);
				calculatedCommitMessage = messageProvider
						.getMessage(resourcesArray);
			}
		} catch (CoreException coreException) {
			Activator.error(coreException.getLocalizedMessage(), coreException);
		}
		if (calculatedCommitMessage != null)
			return calculatedCommitMessage;
		else
			return EMPTY_STRING;
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
				Activator.logError(
						UIText.CommitDialog_WrongTypeOfCommitMessageProvider,
						null);
			}
		}
		return null;
	}

	private void saveOriginalChangeId() {
		int changeIdOffset = findOffsetOfChangeIdLine(previousCommitMessage);
		if (changeIdOffset > 0) {
			int endOfChangeId = findNextEOL(changeIdOffset,
					previousCommitMessage);
			if (endOfChangeId < 0)
				endOfChangeId = previousCommitMessage.length();
			int sha1Offset = changeIdOffset + "Change-Id: I".length(); //$NON-NLS-1$
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
		return message.indexOf(Text.DELIMITER, oldPos + 1);
	}

	private int findOffsetOfChangeIdLine(String message) {
		return ChangeIdUtil.indexOfChangeId(message, Text.DELIMITER);
	}

	private void updateChangeIdButton() {
		String curText = commitText.getText();
		if (!curText.endsWith(Text.DELIMITER))
			curText += Text.DELIMITER;

		createChangeId = curText.indexOf(Text.DELIMITER + "Change-Id: ") != -1; //$NON-NLS-1$
		listener.updateChangeIdToggleSelection(createChangeId);
	}

	private void refreshChangeIdText() {
		if (createChangeId) {
			// ChangeIdUtil uses \n line endings
			String text = commitText.getText().replaceAll(Text.DELIMITER, "\n"); //$NON-NLS-1$
			String changedText = ChangeIdUtil.insertId(
					text,
					originalChangeId != null ? originalChangeId : ObjectId
							.zeroId(), true);
			if (!text.equals(changedText)) {
				changedText = changedText.replaceAll("\n", Text.DELIMITER); //$NON-NLS-1$
				commitText.setText(changedText);
			}
		} else {
			String text = commitText.getText();
			int changeIdOffset = findOffsetOfChangeIdLine(text);
			if (changeIdOffset > 0) {
				String cleanedText;
				int endOfChangeId = findNextEOL(changeIdOffset, text);
				if (endOfChangeId == -1)
					cleanedText = text.substring(0, changeIdOffset);
				else
					cleanedText = text.substring(0, changeIdOffset)
							+ text.substring(endOfChangeId);
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
		if (lastIndexOfLineBreak != -1
				&& lastIndexOfLineBreak == output.length() - breakLength)
			output = output.substring(0, output.length() - breakLength);

		// get the last line
		lastIndexOfLineBreak = output.lastIndexOf(Text.DELIMITER);
		return lastIndexOfLineBreak == -1 ? output : output.substring(
				lastIndexOfLineBreak + breakLength, output.length());
	}

	private void updateSignedOffButton() {
		String curText = commitText.getText();
		if (!curText.endsWith(Text.DELIMITER))
			curText += Text.DELIMITER;
		signedOff = curText.indexOf(getSignedOff() + Text.DELIMITER) != -1;
		listener.updateSignedOffToggleSelection(signedOff);
	}

	private void refreshSignedOffBy() {
		String curText = commitText.getText();
		if (signedOff)
			// add signed off line
			commitText.setText(signOff(curText));
		else {
			// remove signed off line
			String s = getSignedOff();
			if (s != null) {
				curText = replaceSignOff(curText, s, EMPTY_STRING);
				if (curText.endsWith(Text.DELIMITER + Text.DELIMITER))
					curText = curText.substring(0, curText.length()
							- Text.DELIMITER.length());
				commitText.setText(curText);
			}
		}
	}

	private String replaceSignOff(String input, String oldSignOff,
			String newSignOff) {
		assert input != null;
		assert oldSignOff != null;
		assert newSignOff != null;

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
		signedOff = signedOffButtonSelection;
		refreshSignedOffBy();
	}

	/**
	 * @param selection
	 *
	 */
	public void setChangeIdButtonSelection(boolean selection) {
		createChangeId = selection;
		refreshChangeIdText();
	}

	/**
	 * @param repository
	 */
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	/**
	 * @return repository
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @param id the id of the current head commit
	 */
	public void setHeadCommit(ObjectId id) {
		headCommitId = id;
	}

	/**
	 * @return head commit
	 */
	public ObjectId getHeadCommit() {
		return headCommitId;
	}
}
