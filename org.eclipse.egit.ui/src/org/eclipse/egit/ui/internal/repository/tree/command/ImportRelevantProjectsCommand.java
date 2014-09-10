package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

/**
 *
 */
public class ImportRelevantProjectsCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryTreeNode> selectedNodes = getSelectedNodes(event);
		if (selectedNodes == null || selectedNodes.isEmpty()) {
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					UIText.ImportProjectsWrongSelection,
					UIText.ImportProjectsSelectionInRepositoryRequired);
			return null;
		}

		for (Object node : selectedNodes) {
			List<File> files = null;
			if (node instanceof PlotCommit) {
				files = getFilesForPlotCommit((PlotCommit) node, event);
			} else if (node instanceof RefNode) {
				files = getFilesForRefNode((RefNode) node);
			}
			Set<File> dotProjectFiles = findDotProjectFiles(files);
			importProjects(dotProjectFiles);
		}

		return null;
	}

	private List<File> getFilesForPlotCommit(PlotCommit node,
			ExecutionEvent event) throws ExecutionException {
		try {
			IWorkbenchPart ap = HandlerUtil.getActivePartChecked(event);
			if (ap instanceof IHistoryView) {
				Object input = ((IHistoryView) ap).getHistoryPage().getInput();
				Repository repo = getRepository(input);
				List<File> files = getFiles(repo, node);
				return files;
			}
		} catch (MissingObjectException e) {
			Activator.error(e.getMessage(), e);
		} catch (IncorrectObjectTypeException e) {
			Activator.error(e.getMessage(), e);
		} catch (CorruptObjectException e) {
			Activator.error(e.getMessage(), e);
		} catch (IOException e) {
			Activator.error(e.getMessage(), e);
		}
		return null;
	}

	private Repository getRepository(Object input) throws ExecutionException {
		if (input == null)
			return null;
		if (input instanceof HistoryPageInput)
			return ((HistoryPageInput) input).getRepository();
		if (input instanceof RepositoryTreeNode)
			return ((RepositoryTreeNode) input).getRepository();
		if (input instanceof IResource) {
			IResource resource = (IResource) input;
			RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
			if (mapping != null)
				return mapping.getRepository();
			// for closed projects team framework doesn't allow to get mapping
			// so try again using a path based approach
			Repository repository = org.eclipse.egit.core.Activator
					.getDefault().getRepositoryCache().getRepository(resource);
			if (repository != null)
				return repository;
		}
		if (input instanceof IAdaptable) {
			IResource resource = (IResource) ((IAdaptable) input)
					.getAdapter(IResource.class);
			if (resource != null) {
				RepositoryMapping mapping = RepositoryMapping
						.getMapping(resource);
				if (mapping != null)
					return mapping.getRepository();
			}

		}

		Repository repo = AdapterUtils.adapt(input, Repository.class);
		if (repo != null)
			return repo;

		throw new ExecutionException(
				UIText.AbstractHistoryCommanndHandler_CouldNotGetRepositoryMessage);
	}

	private List<File> getFilesForRefNode(RefNode rn) {
		Ref ref = rn.getObject();
		Repository repo = rn.getRepository();
		List<File> files = new ArrayList<File>();
		try {
			PlotWalk walk = new PlotWalk(repo);
			RevCommit unfilteredCommit = walk.parseCommit(ref.getLeaf()
					.getObjectId());
			files = getFiles(repo, unfilteredCommit);

		} catch (MissingObjectException e) {
			Activator.error(e.getMessage(), e);
		} catch (IncorrectObjectTypeException e) {
			Activator.error(e.getMessage(), e);
		} catch (IOException e) {
			Activator.error(e.getMessage(), e);
		}
		return files;
	}

	private List<File> getFiles(Repository repo,
			final RevCommit unfilteredCommit) throws MissingObjectException,
			IOException, IncorrectObjectTypeException, CorruptObjectException {
		List<File> files = new ArrayList<File>();
		TreeWalk tw = new TreeWalk(repo);
		tw.setRecursive(true);
		final PlotWalk walk = new PlotWalk(repo);

		for (RevCommit parent : unfilteredCommit.getParents())
			walk.parseBody(parent);

		FileDiff[] diffs = FileDiff.compute(repo, tw, unfilteredCommit,
				TreeFilter.ALL);
		if (diffs != null && diffs.length > 0) {
			File repoDir = repo.getDirectory();
			String repoPath = repoDir.getParentFile().getAbsolutePath();
			for (FileDiff d : diffs) {
				String path = d.getPath();
				File f = new File(repoPath + File.separator + path);
				files.add(f);
			}
		}
		return files;
	}

	private Set<File> findDotProjectFiles(List<File> files) {
		Set<File> result = new HashSet<File>();
		for (File file : files) {
			if (file.isFile()) {
				// find project file going the file hierarchy upwards
				while (file != null) {
					File f = new File(file.getParent() + File.separator
							+ ".project"); //$NON-NLS-1$
					if (f.isFile()) {
						result.add(f);
						break;
					}
					file = file.getParentFile();
				}
			}
		}
		return result;
	}

	private void importProjects(final Set<File> dotProjectFiles) {
		WorkspaceJob job = new WorkspaceJob(
				UIText.ImportRelevantProjectsCommand_ImportingChangedProjects) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				for (File f : dotProjectFiles) {
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					String ap = f.getAbsolutePath();
					importProject(ap);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private void importProject(String path) {
		try {
			IProjectDescription description = IDEWorkbenchPlugin
					.getPluginWorkspace()
					.loadProjectDescription(new Path(path));
			if (description != null) {
				String projectName = description.getName();
				IProject project = ResourcesPlugin.getWorkspace().getRoot()
						.getProject(projectName);
				if (project.exists() == true) {
					if (project.isOpen() == false)
						project.open(IResource.BACKGROUND_REFRESH,
								new NullProgressMonitor());
				} else {
					project.create(description, new NullProgressMonitor());
					project.open(IResource.BACKGROUND_REFRESH,
							new NullProgressMonitor());
				}
			}
		} catch (Exception e) {
			Activator.error(e.getMessage(), e);
		}
	}
}
