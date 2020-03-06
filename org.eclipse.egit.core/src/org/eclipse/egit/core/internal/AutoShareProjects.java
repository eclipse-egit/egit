package org.eclipse.egit.core.internal;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.JobFamilies;

/**
 * @author alex WIP
 *
 */
public class AutoShareProjects implements IResourceChangeListener {

	private static int INTERESTING_CHANGES = IResourceDelta.ADDED
			| IResourceDelta.OPEN;

	private CheckProjectsToShare checkProjectsJob;

	private IJobManager jobManager;

	/**
	 * @param workspace
	 *            WIP
	 */
	public void setWorkspace(IWorkspace workspace) {
		workspace.addResourceChangeListener(this,
				IResourceChangeEvent.POST_CHANGE);
	}

	/**
	 * @param workspace
	 *            WIP
	 */
	public void unsetWorkspace(IWorkspace workspace) {
		workspace.removeResourceChangeListener(this);
	}

	/**
	 * @param jobManager
	 *            WIP
	 */
	public void setJobManager(IJobManager jobManager) {
		this.jobManager = jobManager;
	}

	/**
	 * @param jobManager
	 *            WIP
	 */
	public void unsetJobManager(IJobManager jobManager) {
		jobManager.cancel(JobFamilies.AUTO_SHARE);
		this.jobManager = null;
	}

	/**
	 * WIP
	 */
	public void start() {
		checkProjectsJob = new CheckProjectsToShare();
	}

	private boolean doAutoShare() {
		IEclipsePreferences d = DefaultScope.INSTANCE
				.getNode(Activator.getPluginId());
		IEclipsePreferences p = InstanceScope.INSTANCE
				.getNode(Activator.getPluginId());
		return p.getBoolean(GitCorePreferences.core_autoShareProjects,
				d.getBoolean(GitCorePreferences.core_autoShareProjects, true));
	}

	/**
	 * WIP
	 */
	public void stop() {
		boolean isRunning = !checkProjectsJob.cancel();
		final IJobManager jobs = jobManager;
		if (jobs != null) {
			jobs.cancel(JobFamilies.AUTO_SHARE);
			try {
				if (isRunning) {
					checkProjectsJob.join();
				}
				jobs.join(JobFamilies.AUTO_SHARE, new NullProgressMonitor());
			} catch (OperationCanceledException e) {
				// Ignore
			} catch (InterruptedException e) {
				Activator.logError(e.getLocalizedMessage(), e);
			}
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (!doAutoShare()) {
			return;
		}
		try {
			final Set<IProject> projectCandidates = new LinkedHashSet<>();
			event.getDelta().accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta)
						throws CoreException {
					return collectOpenedProjects(delta, projectCandidates);
				}
			});
			if (!projectCandidates.isEmpty()) {
				checkProjectsJob.addProjectsToCheck(projectCandidates);
			}
		} catch (CoreException e) {
			Activator.logError(e.getMessage(), e);
			return;
		}
	}

	/*
	 * This method should not use RepositoryMapping.getMapping(project) or
	 * RepositoryProvider.getProvider(project) which can trigger
	 * RepositoryProvider.map(project) and deadlock current thread. See
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=468270
	 */
	private boolean collectOpenedProjects(IResourceDelta delta,
			Set<IProject> projects) {
		if (delta.getKind() == IResourceDelta.CHANGED
				&& (delta.getFlags() & INTERESTING_CHANGES) == 0) {
			return true;
		}
		final IResource resource = delta.getResource();
		if (resource.getType() == IResource.ROOT) {
			return true;
		}
		if (resource.getType() != IResource.PROJECT) {
			return false;
		}
		if (!resource.isAccessible() || resource.getLocation() == null) {
			return false;
		}
		projects.add((IProject) resource);
		return false;
	}

}