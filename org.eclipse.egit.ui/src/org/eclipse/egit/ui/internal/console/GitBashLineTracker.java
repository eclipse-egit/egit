package org.eclipse.egit.ui.internal.console;

import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 *
 */
public class GitBashLineTracker implements IConsoleLineTracker {

	private GitBashConsole console;

	public void init(IConsole console) {
		throw new UnsupportedOperationException(
				"The constructor for org.eclipse.debug.ui.console.IConsole shouldn't be called."); //$NON-NLS-1$
	}

	/**
	 * @param console
	 */
	public void init(GitBashConsole console) {
		this.console = console;
	}

	public void lineAppended(IRegion line) {
		try {
			IDocument document = console.getDocument();
			String cmd = document.get(line.getOffset(), line.getLength());
			cmd = cmd.substring(cmd.lastIndexOf(GitBashConsole.PROMPT)
					+ GitBashConsole.PROMPT.length());
			console.exec(cmd);
		} catch (BadLocationException e) {
			// ignore
		}

	}

	public void dispose() {

	}

}
