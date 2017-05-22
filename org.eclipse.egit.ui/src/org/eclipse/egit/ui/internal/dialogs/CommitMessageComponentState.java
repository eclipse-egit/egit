/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.jgit.lib.ObjectId;

/**
 * State of a {@link CommitMessageComponent}
 *
 */
public class CommitMessageComponentState {

	private String commitMessage;

	private int caretPosition;

	private String committer;
	private String author;
	private boolean amend;
	private ObjectId headCommit;

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
