package org.eclipse.egit.ui.internal.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.internal.console.IOConsolePage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.progress.UIJob;

/**
 *
 */
public class GitBashConsole extends MessageConsole {

	private static final String TYPE = "gitBashConsole"; //$NON-NLS-1$

	private String gitBinPath = "d:/apps/PortableGit/bin/"; //$NON-NLS-1$

	static final String DOLLAR = "$"; //$NON-NLS-1$

	static final String PROMPT = "$>"; //$NON-NLS-1$

	private static final String CHANGE_DIR_CMD = "cd "; //$NON-NLS-1$

	private final IPath workspaceLocation = ResourcesPlugin.getWorkspace()
			.getRoot().getRawLocation();

	File currentDir = ResourcesPlugin.getWorkspace().getRoot()
			.getRawLocation().toFile();

	private GitBashSelectionListener selectionChangedListener;

	/**
	 * @param name
	 * @param imageDescriptor
	 */
	public GitBashConsole(String name, ImageDescriptor imageDescriptor) {
		super(name, TYPE, imageDescriptor, true);
		addPatternMatchListener(new ConsoleLineNotifier());
		selectionChangedListener = new GitBashSelectionListener(this);
	}

	@Override
	protected void init() {
		super.init();
		readNativeGitBinFolder();
		exec("git"); //$NON-NLS-1$
	}

	private void readNativeGitBinFolder() {
		final IPreferenceStore preferenceStore = Activator.getDefault()
				.getPreferenceStore();
		gitBinPath = preferenceStore
				.getString(UIPreferences.GIT_CONSOLE_BIN_PATH) + "\\"; //$NON-NLS-1$
	}

	void exec(final String cmd) {
		MessageConsoleStream out = newMessageStream();
		String result = safeExecuteCGit(cmd);
		if (result != null)
			out.println(result);
		displayPrompt();
	}

	private String safeExecuteCGit(String cmd) {
		try {
			if (DOLLAR.equals(cmd))
				return displaySelection();
			if (cmd.contains(DOLLAR)) {
				cmd = resolveDollar(cmd);
			}
			if (cmd.startsWith(CHANGE_DIR_CMD)) {
				return executeCdCmd(cmd);
			}
			return executeInGitBin(cmd);
		} catch (IOException e) {
			// ignore
		}
		return "An error occured running the command"; //$NON-NLS-1$
	}

	private String resolveDollar(String cmd) {
		Assert.isLegal(cmd.indexOf(DOLLAR) != -1);
		File file = workspaceLocation.append(
				selectionChangedListener.getSelection().getFullPath()).toFile();
		return cmd.replace(DOLLAR, file.toString());
	}

	private String displaySelection() {
		if (selectionChangedListener.getSelection() == null)
			return "No valid input selected"; //$NON-NLS-1$
		return selectionChangedListener.getSelection().getFullPath().toString();
	}

	private String executeCdCmd(String cmd) throws IOException {
		Assert.isLegal(cmd.startsWith(CHANGE_DIR_CMD));
		cmd = cmd.substring(CHANGE_DIR_CMD.length());
		processPath(cmd);
		return null; // nothing to display
	}

	private void processPath(String cmd) throws IOException {
		Path path = new Path(cmd);
		for (int i = 0; i < path.segmentCount(); i++) {
			processSegment(path.segment(i));
		}
	}

	private void processSegment(String segment) throws IOException {
		if ("..".equals(segment)) { //$NON-NLS-1$
			currentDir = currentDir.getParentFile();
		} else if (Arrays.asList(currentDir.listFiles()).contains(
				new File(currentDir, segment))) {
			currentDir = new File(currentDir, segment);
		} else
			throw new IOException("Bad segment"); //$NON-NLS-1$
	}

	private String executeInGitBin(final String cmd) throws IOException {
		Process p = Runtime.getRuntime().exec(gitBinPath + cmd, null,
				currentDir);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		String result = toString(in);
		if (result.length() > 0)
			return result;
		BufferedReader err = new BufferedReader(new InputStreamReader(
				p.getErrorStream()));
		return toString(err);
	}

	private String toString(BufferedReader in) throws IOException {
		StringWriter sw = new StringWriter();
		char[] buffer = new char[1024 * 4];
		int n = 0;
		while ((n = in.read(buffer)) != -1) {
			sw.write(buffer, 0, n);
		}
		return sw.toString();
	}

	private void displayPrompt() {
		MessageConsoleStream out = newMessageStream();
		out.print(currentDir + PROMPT);
	}

	public IPageBookViewPage createPage(IConsoleView view) {
		addSelectionListener(view);
		return new IOConsolePage(this, view);
	}

	private void addSelectionListener(IConsoleView view) {
		IWorkbenchPartSite site = view.getSite();
		ISelectionService service = (ISelectionService) site
				.getService(ISelectionService.class);
		service.addPostSelectionListener(selectionChangedListener);
		ISelection selection = service.getSelection();
		if (selection != null && !selection.isEmpty()) {
			IWorkbenchPart part = site.getPage().getActivePart();
			if (part != null)
				selectionChangedListener.selectionChanged(part, selection);
		}
	}

	void resetName() {
		final String newName = computeName();
		String name = getName();
		if (!name.equals(newName)) {
			UIJob job = new UIJob("Update Git Bash console title") { //$NON-NLS-1$
				public IStatus runInUIThread(IProgressMonitor monitor) {
					GitBashConsole.this.setName(newName);
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();
		}
	}

	private String computeName() {
		return GitBashConsoleFactory.NAME + ", $=" + displaySelection(); //$NON-NLS-1$
	}
}
