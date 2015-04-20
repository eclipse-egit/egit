package org.eclipse.egit.core.op;

import java.io.IOException;
import java.util.Set;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.osgi.util.NLS;

/**
 * This class implements renaming of a remote
 */
public class RenameRemoteOperation implements IEGitOperation {
	private final Repository repository;

	private final String oldName;

	private final String newName;

	/**
	 * @param repository
	 * @param oldName
	 *            the remote to rename
	 * @param newName
	 *            the new name
	 */
	public RenameRemoteOperation(Repository repository, String oldName,
			String newName) {
		this.repository = repository;
		this.oldName = oldName;
		this.newName = newName;
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;

		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			public void run(IProgressMonitor actMonitor) throws CoreException {
				final String taskName = NLS.bind(
						CoreText.RenameRemoteOperation_TaskName,
						oldName, newName);
				actMonitor.beginTask(taskName, 1);
				try {
					final String REMOTE = "remote"; //$NON-NLS-1$ // TODO: what to do?
					final StoredConfig config = repository.getConfig();
					final Set<String> names = config.getNames(REMOTE, oldName);

					if (names.size() == 0)
						throw new IOException("Remote not found"); //$NON-NLS-1$ // TODO

					for (String name : names) {
						config.setString(REMOTE, newName, name,
								config.getString(REMOTE, oldName, name));
						config.unset(REMOTE, oldName, name);
					}

					config.unsetSection(REMOTE, oldName);
					config.save();
				} catch (IOException e) {
					throw new CoreException(Activator.error(e.getMessage(), e));
				}
				actMonitor.worked(1);
				actMonitor.done();
			}
		};
		// lock workspace to protect working tree changes
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, monitor);
	}

	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repository);
	}
}