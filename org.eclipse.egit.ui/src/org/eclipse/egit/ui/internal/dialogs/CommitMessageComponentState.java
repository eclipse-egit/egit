package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.jgit.lib.ObjectId;

/**
 * State of a {@link CommitMessageComponent}
 *
 */
public class CommitMessageComponentState {

	private String commitMessage;
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
