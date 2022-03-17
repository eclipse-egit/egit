/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.jgit.lib.ObjectId;

/**
 * State of a {@link CommitMessageComponent}
 *
 */
public class CommitMessageComponentState {

	static final int CARET_DEFAULT_POSITION = 0;

	private String commitMessage;

	private int caretPosition;

	private String committer;
	private String author;
	private boolean amend;
	private ObjectId headCommit;

	private boolean sign;

	private char autoCommentChar;

	/**
	 * @return commit message
	 */
	public String getCommitMessage() {
		return commitMessage;
	}

	/**
	 * @param commitMessage
	 */
	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}

	/**
	 * @return caretPosition
	 */
	public int getCaretPosition() {
		return caretPosition;
	}

	/**
	 * @param caretPosition
	 */
	public void setCaretPosition(int caretPosition) {
		this.caretPosition = caretPosition;
	}

	/**
	 * @return committer
	 */
	public String getCommitter() {
		return committer;
	}

	/**
	 * @param committer
	 */
	public void setCommitter(String committer) {
		this.committer = committer;
	}

	/**
	 * @return author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * @param author
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * @return amend
	 */
	public boolean getAmend() {
		return amend;
	}

	/**
	 * @param amend
	 */
	public void setAmend(boolean amend) {
		this.amend = amend;
	}

	/**
	 * @return sign
	 */
	public boolean getSign() {
		return sign;
	}

	/**
	 * @param sign
	 */
	public void setSign(boolean sign) {
		this.sign = sign;
	}

	/**
	 * Retrieves the comment character to use for git config
	 * {@code core.commentChar=auto}.
	 *
	 * @return the character, or {@code '\000'} of none stored
	 */
	public char getAutoCommentChar() {
		return autoCommentChar;
	}

	/**
	 * Sets the comment character determined by git config
	 * {@code core.commentChar=auto}.
	 *
	 * @param commentChar
	 *            the comment character, or {@code '\000'} if not
	 *            auto-determined
	 */
	public void setAutoCommentChar(char commentChar) {
		this.autoCommentChar = commentChar;
	}

	/**
	 * @param headCommit
	 */
	public void setHeadCommit(ObjectId headCommit) {
		this.headCommit = headCommit;
	}

	/**
	 * @return head commit
	 */
	public ObjectId getHeadCommit() {
		return headCommit;
	}

}
