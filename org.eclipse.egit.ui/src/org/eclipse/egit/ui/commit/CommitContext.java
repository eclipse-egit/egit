package org.eclipse.egit.ui.commit;

import org.eclipse.jgit.lib.Repository;

/**
 * Provides access to the context of a commit in progress: which repository, and
 * the commit message.
 * <p>
 * This can be used by custom commands contributed via plugin.xml to the
 * {@link #COMMIT_MSG_TOOLBAR_ID} to access and modify the commit message.
 * </p>
 * <p>
 * In <code>plugin.xml</code>, define a command, handler, and menu contribution
 * to the toolbar:
 *
 * <pre>
 * &lt;extension point="org.eclipse.ui.commands">
 *    &lt;command
 *          categoryId="my.commandCategory"
 *          id="my.CommandId"
 *          name="My Command" />
 * &lt;/extension>
 * &lt;extension point="org.eclipse.ui.commandImages">
 *    &lt;image commandId="my.CommandId" icon="some icon path" />
 * &lt;/extension>
 * &lt;extension point="org.eclipse.ui.handlers">
 *    &lt;handler commandId="my.commandId">
 *       &lt;class class="my.package.MyCommandHandler" />
 *    &lt;/handler>
 * &lt;/extension>
 * &lt;extension point="org.eclipse.ui.menus">
 *    &lt;menuContribution
 *          locationURI="toolbar:org.eclipse.egit.ui.commitMsgToolBar?after=additions">
 *       &lt;command
 *            commandId="my.CommandId"
 *            label="My Command"
 *            style="push" />
 *    &lt;/menuContribution>
 * &lt;/extension>
 * </pre>
 *
 * and implement your command handler:
 *
 * <pre>
 * package my.package;
 *
 * import org.eclipse.core.commands.AbstractHandler;
 * import org.eclipse.core.commands.ExecutionEvent;
 * import org.eclipse.core.commands.ExecutionException;
 * import org.eclipse.egit.ui.commit.CommitContext;
 * import org.eclipse.egit.ui.commit.CommitContextUtils;
 *
 * public class MyCommandHandler extends AbstractHandler {
 *
 *   public Object execute(ExecutionEvent event) throws ExecutionException {
 *     CommitContext context = CommitContextUtils.getCommitContext(event);
 *     if (context != null) {
 *       // Do something
 *     }
 *     return null;
 *   }
 *
 * }
 * </pre>
 *
 * @since 7.4
 */
public interface CommitContext {

	/**
	 * The ID of the commit message toolbar. This is the toolbar containing the
	 * "Amend", "Sign-Off", "Sign", and "Gerrit Change-Id" buttons. This ID can
	 * be used in plugin.xml to contribute additional commands to that toolbar,
	 * in both the staging view or in the commit dialog.
	 */
	static final String COMMIT_MSG_TOOLBAR_ID = "org.eclipse.egit.ui.commitMsgToolBar"; //$NON-NLS-1$

	/**
	 * Retrieves the {@link Repository} this {@link CommitContext} relates to.
	 * Use this to obtain additional information about the commit being made,
	 * such as staged/unstaged changes, or commit message settings like the
	 * clean-up mode or the comment character. (Note that the current comment
	 * character is also available directly via {@link #getCommentChar()} as a
	 * convenience.)
	 *
	 * @return the {@link Repository}; may be {@code null}.
	 */
	Repository getRepository();

	/**
	 * Retrieves the <em>raw</em> commit message text. The text uses plain LF as
	 * line delimiter and contains the raw message with no whitespace removed,
	 * no line-wrapping applied, and including all comment lines.
	 *
	 * @return the current commit message
	 */
	String getCommitMessage();

	/**
	 * Retrieves the current comment character used in the raw commit message.
	 *
	 * @return the comment character
	 */
	char getCommentChar();

	/**
	 * Sets the (raw) commit message.
	 *
	 * @param text
	 *            the commit message to set
	 */
	void setCommitMessage(String text);
}
