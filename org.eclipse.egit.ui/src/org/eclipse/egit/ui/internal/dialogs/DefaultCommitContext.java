package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.ui.commit.CommitContext;

/**
 * A base implementation of a {@link CommitContext} that uses a
 * {@link SpellcheckableMessageArea} for the commit message.
 */
public abstract class DefaultCommitContext implements CommitContext {

	private final SpellcheckableMessageArea text;

	/**
	 * Creates a new instance.
	 *
	 * @param text
	 *            {@link SpellcheckableMessageArea} containing the commit
	 *            message
	 */
	public DefaultCommitContext(SpellcheckableMessageArea text) {
		this.text = text;
	}

	@Override
	public String getCommitMessage() {
		return Utils.normalizeLineEndings(text.getText());
	}

	@Override
	public char getCommentChar() {
		return text.getCommentChar();
	}

	@Override
	public void setCommitMessage(String msg) {
		text.setText(msg == null ? "" : msg); //$NON-NLS-1$
	}

}
