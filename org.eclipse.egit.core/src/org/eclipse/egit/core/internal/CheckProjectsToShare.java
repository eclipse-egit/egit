package org.eclipse.egit.core.internal;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.project.RepositoryFinder;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.util.FS;
import org.eclipse.team.core.RepositoryProvider;

class CheckProjectsToShare extends Job {
	private Object lock = new Object();

	private Set<IProject> projectCandidates;

	public CheckProjectsToShare() {
		super(CoreText.Activator_AutoShareJobName);
		this.projectCandidates = new LinkedHashSet<>();
		setUser(false);
		setSystem(true);
	}

	public void addProjectsToCheck(Set<IProject> projects) {
		synchronized (lock) {
			this.projectCandidates.addAll(projects);
			if (!projectCandidates.isEmpty()) {
				schedule(100);
			}
		}
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Set<IProject> projectsToCheck;
		synchronized (lock) {
			projectsToCheck = projectCandidates;
			projectCandidates = new LinkedHashSet<>();
		}
		if (projectsToCheck.isEmpty()) {
			return Status.OK_STATUS;
		}

		final Map<IProject, File> projects = new HashMap<>();
		for (IProject project : projectsToCheck) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			if (project.isAccessible()) {
				try {
					visitConnect(project, projects);
				} catch (CoreException e) {
					Activator.logError(e.getMessage(), e);
				}
			}
		}
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}
		if (projects.size() > 0) {
			ConnectProviderOperation op = new ConnectProviderOperation(
					projects);
			op.setRefreshResources(false);
			JobUtil.scheduleUserJob(op,
					CoreText.Activator_AutoShareJobName,
					JobFamilies.AUTO_SHARE);
		}
		return Status.OK_STATUS;
	}

	private void visitConnect(IProject project,
			final Map<IProject, File> projects) throws CoreException {

		if (RepositoryMapping.getMapping(project) != null) {
			return;
		}
		RepositoryProvider provider = RepositoryProvider
				.getProvider(project);
		// respect if project is already shared with another
		// team provider
		if (provider != null) {
			return;
		}
		RepositoryFinder f = new RepositoryFinder(project);
		f.setFindInChildren(false);
		List<RepositoryMapping> mappings = f
				.find(new NullProgressMonitor());
		if (mappings.isEmpty()) {
			return;
		}
		RepositoryMapping m = mappings.get(0);
		IPath gitDirPath = m.getGitDirAbsolutePath();
		if (gitDirPath == null || !isValidRepositoryPath(gitDirPath)) {
			return;
		}

		// connect
		File repositoryDir = gitDirPath.toFile();
		projects.put(project, repositoryDir);

		Set<String> configured = Activator.getDefault().getRepositoryUtil()
				.getRepositories();
		if (configured.contains(gitDirPath.toString())) {
			return;
		}
		int nofMappings = mappings.size();
		if (nofMappings > 1) {
			// We don't want to add submodules, that would only lead to
			// problems when a configured repository is deleted. Walk up the
			// hierarchy of nested repositories found. If we hit an already
			// configured repository, we're done anyway. Otherwise add the
			// topmost not yet configured repository that has a valid path.
			IPath lastPath = gitDirPath;
			for (int i = 1; i < nofMappings; i++) {
				IPath nextPath = mappings.get(i).getGitDirAbsolutePath();
				if (nextPath == null) {
					continue;
				}
				if (configured.contains(nextPath.toString())) {
					return;
				} else if (!isValidRepositoryPath(nextPath)) {
					break;
				}
				lastPath = nextPath;
			}
			repositoryDir = lastPath.toFile();
		}
		try {
			Activator.getDefault().getRepositoryUtil()
					.addConfiguredRepository(repositoryDir);
		} catch (IllegalArgumentException e) {
			Activator.logError(CoreText.Activator_AutoSharingFailed, e);
		}
	}

	static boolean isValidRepositoryPath(@NonNull IPath gitDirPath) {
		if (gitDirPath.segmentCount() == 0) {
			return false;
		}
		IPath workingDir = gitDirPath.removeLastSegments(1);
		// Don't connect "/" or "C:\"
		if (workingDir.isRoot()) {
			return false;
		}
		File userHome = FS.DETECTED.userHome();
		if (userHome != null) {
			Path userHomePath = new Path(userHome.getAbsolutePath());
			// Don't connect "/home" or "/home/username"
			if (workingDir.isPrefixOf(userHomePath)) {
				return false;
			}
		}
		return true;
	}
}