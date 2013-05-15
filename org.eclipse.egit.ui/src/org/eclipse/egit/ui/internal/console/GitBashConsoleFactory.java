package org.eclipse.egit.ui.internal.console;

import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;

/**
 * Factory for a Git Bash console.
 *
 * @since 3.1
 */
public class GitBashConsoleFactory implements IConsoleFactory {

	static final String NAME = "Git Bash Console"; //$NON-NLS-1$

	private final IConsoleManager fConsoleManager;

	private IOConsole fConsole = null;

	/**
	 * The default constructor
	 */
	public GitBashConsoleFactory() {
		fConsoleManager = ConsolePlugin.getDefault().getConsoleManager();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.console.IConsoleFactory#openConsole()
	 */
	public void openConsole() {
		IOConsole console = getConsole();

		IConsole[] existing = fConsoleManager.getConsoles();
		boolean exists = false;
		for (int i = 0; i < existing.length; i++) {
			if (console == existing[i])
				exists = true;
		}
		if (!exists)
			fConsoleManager.addConsoles(new IConsole[] { console });
		fConsoleManager.showConsoleView(console);
	}

	private synchronized IOConsole getConsole() {
		if (fConsole != null)
			return fConsole;
		fConsole = new GitBashConsole(NAME, UIIcons.GIT_BASH);
		return fConsole;
	}

	void closeConsole(IOConsole console) {
		synchronized (this) {
			if (console != fConsole)
				throw new IllegalArgumentException("Wrong console instance!"); //$NON-NLS-1$
			fConsole = null;
		}
		fConsoleManager.removeConsoles(new IConsole[] { console });
	}

}
