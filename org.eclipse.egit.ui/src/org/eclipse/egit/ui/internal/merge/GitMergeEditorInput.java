/*******************************************************************************
 * Copyright (C) 2010, 2019 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.revision.EditableRevision;
import org.eclipse.egit.ui.internal.revision.FileRevisionTypedElement;
import org.eclipse.egit.ui.internal.revision.GitCompareFileRevisionEditorInput.EmptyTypedElement;
import org.eclipse.egit.ui.internal.revision.LocationEditableRevision;
import org.eclipse.egit.ui.internal.revision.ResourceEditableRevision;
import org.eclipse.egit.ui.internal.synchronize.compare.LocalNonWorkspaceTypedElement;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.NotIgnoredFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.synchronize.EditableSharedDocumentAdapter.ISharedDocumentAdapterListener;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

/**
 * A Git-specific {@link CompareEditorInput}
 */
@SuppressWarnings("restriction")
public class GitMergeEditorInput extends CompareEditorInput {
	private static final String LABELPATTERN = "{0} - {1}"; //$NON-NLS-1$

	private static final Image FOLDER_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

	private static final Image PROJECT_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(SharedImages.IMG_OBJ_PROJECT);

	private final boolean useWorkspace;

	private final IPath[] locations;

	/**
	 * @param useWorkspace
	 *            if <code>true</code>, use the workspace content (i.e. the
	 *            Git-merged version) as "left" content, otherwise use HEAD
	 *            (i.e. the previous, non-merged version)
	 * @param locations
	 *            as selected by the user
	 */
	public GitMergeEditorInput(boolean useWorkspace, IPath... locations) {
		super(new CompareConfiguration());
		this.useWorkspace = useWorkspace;
		this.locations = locations;
		CompareConfiguration config = getCompareConfiguration();
		config.setLeftEditable(true);
	}

	@Override
	public Object getAdapter(Class adapter) {
		if ((adapter == IFile.class || adapter == IResource.class)
				&& isUIThread()) {
			Object selectedEdition = getSelectedEdition();
			if (selectedEdition instanceof DiffNode) {
				DiffNode diffNode = (DiffNode) selectedEdition;
				ITypedElement element = diffNode.getLeft();
				if (element instanceof IResourceProvider) {
					IResource resource = ((IResourceProvider) element)
							.getResource();
					if (adapter.isInstance(resource)) {
						return resource;
					}
				}
			}
		}
		return super.getAdapter(adapter);
	}

	private static boolean isUIThread() {
		return Display.getCurrent() != null;
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		// make sure all resources belong to the same repository
		RevWalk rw = null;
		try {
			monitor.beginTask(
					UIText.GitMergeEditorInput_CheckingResourcesTaskName,
					IProgressMonitor.UNKNOWN);

			Map<Repository, Collection<String>> pathsByRepository = ResourceUtil
					.splitPathsByRepository(Arrays.asList(locations));
			if (pathsByRepository.size() != 1) {
				throw new InvocationTargetException(
						new IllegalStateException(
								UIText.RepositoryAction_multiRepoSelection));
			}
			Entry<Repository, Collection<String>> entry = pathsByRepository
					.entrySet().iterator().next();
			Repository repo = entry.getKey();
			List<String> filterPaths = new ArrayList<>(entry.getValue());

			if (monitor.isCanceled())
				throw new InterruptedException();

			rw = new RevWalk(repo);

			// get the "right" side (MERGE_HEAD for merge, ORIG_HEAD for rebase)
			final RevCommit rightCommit;
			try {
				String target;
				if (repo.getRepositoryState().equals(RepositoryState.MERGING))
					target = Constants.MERGE_HEAD;
				else if (repo.getRepositoryState().equals(RepositoryState.CHERRY_PICKING))
					target = Constants.CHERRY_PICK_HEAD;
				else if (repo.getRepositoryState().equals(
						RepositoryState.REBASING_INTERACTIVE))
					target = readFile(repo.getDirectory(),
							RebaseCommand.REBASE_MERGE + File.separatorChar
									+ RebaseCommand.STOPPED_SHA);
				else
					target = Constants.ORIG_HEAD;
				ObjectId mergeHead = repo.resolve(target);
				if (mergeHead == null)
					throw new IOException(NLS.bind(
							CoreText.ValidationUtils_CanNotResolveRefMessage,
							target));
				rightCommit = rw.parseCommit(mergeHead);
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}

			// we need the HEAD, also to determine the common
			// ancestor
			final RevCommit headCommit;
			try {
				ObjectId head = repo.resolve(Constants.HEAD);
				if (head == null)
					throw new IOException(NLS.bind(
							CoreText.ValidationUtils_CanNotResolveRefMessage,
							Constants.HEAD));
				headCommit = rw.parseCommit(head);
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}

			final String fullBranch;
			try {
				fullBranch = repo.getFullBranch();
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}

			// try to obtain the common ancestor
			List<RevCommit> startPoints = new ArrayList<>();
			rw.setRevFilter(RevFilter.MERGE_BASE);
			startPoints.add(rightCommit);
			startPoints.add(headCommit);
			RevCommit ancestorCommit;
			try {
				rw.markStart(startPoints);
				ancestorCommit = rw.next();
			} catch (Exception e) {
				ancestorCommit = null;
			}

			if (monitor.isCanceled())
				throw new InterruptedException();

			// set the labels
			CompareConfiguration config = getCompareConfiguration();
			config.setRightLabel(NLS.bind(LABELPATTERN, rightCommit
					.getShortMessage(), CompareUtils.truncatedRevision(rightCommit.name())));

			if (!useWorkspace)
				config.setLeftLabel(NLS.bind(LABELPATTERN, headCommit
						.getShortMessage(), CompareUtils.truncatedRevision(headCommit.name())));
			else
				config.setLeftLabel(UIText.GitMergeEditorInput_WorkspaceHeader);

			if (ancestorCommit != null)
				config.setAncestorLabel(NLS.bind(LABELPATTERN, ancestorCommit
						.getShortMessage(), CompareUtils.truncatedRevision(ancestorCommit.name())));

			// set title and icon
			setTitle(NLS.bind(UIText.GitMergeEditorInput_MergeEditorTitle,
					new Object[] {
							Activator.getDefault().getRepositoryUtil()
									.getRepositoryName(repo),
							rightCommit.getShortMessage(), fullBranch }));

			// build the nodes
			try {
				return buildDiffContainer(repo, headCommit,
						ancestorCommit, filterPaths, rw, monitor);
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}
		} finally {
			if (rw != null)
				rw.dispose();
			monitor.done();
		}
	}

	@Override
	protected void contentsCreated() {
		super.contentsCreated();
		// select the first conflict
		getNavigator().selectChange(true);
	}

	@Override
	protected void handleDispose() {
		super.handleDispose();
		// we do NOT dispose the images, as these are shared
	}

	@SuppressWarnings("unused")
	private IDiffContainer buildDiffContainer(Repository repository,
			RevCommit headCommit, RevCommit ancestorCommit,
			List<String> filterPaths, RevWalk rw, IProgressMonitor monitor)
			throws IOException, InterruptedException {

		monitor.setTaskName(UIText.GitMergeEditorInput_CalculatingDiffTaskName);
		IDiffContainer result = new DiffNode(Differencer.CONFLICTING);

		try (TreeWalk tw = new TreeWalk(repository)) {
			int dirCacheIndex = tw.addTree(new DirCacheIterator(repository
					.readDirCache()));
			int fileTreeIndex = tw.addTree(new FileTreeIterator(repository));
			int repositoryTreeIndex = tw.addTree(rw.parseTree(repository
					.resolve(Constants.HEAD)));

			// skip ignored resources
			NotIgnoredFilter notIgnoredFilter = new NotIgnoredFilter(
					fileTreeIndex);
			// filter by selected resources
			if (filterPaths.size() > 1) {
				tw.setFilter(AndTreeFilter.create(
						PathFilterGroup.createFromStrings(filterPaths),
						notIgnoredFilter));
			} else if (filterPaths.size() > 0) {
				String path = filterPaths.get(0);
				if (path.isEmpty()) {
					tw.setFilter(notIgnoredFilter);
				} else {
					tw.setFilter(AndTreeFilter.create(
							PathFilterGroup.createFromStrings(path),
							notIgnoredFilter));
				}
			} else {
				tw.setFilter(notIgnoredFilter);
			}
			tw.setRecursive(true);

			while (tw.next()) {
				if (monitor.isCanceled())
					throw new InterruptedException();
				String gitPath = tw.getPathString();
				monitor.setTaskName(gitPath);

				FileTreeIterator fit = tw.getTree(fileTreeIndex,
						FileTreeIterator.class);
				if (fit == null)
					continue;

				DirCacheIterator dit = tw.getTree(dirCacheIndex,
						DirCacheIterator.class);

				final DirCacheEntry dirCacheEntry = dit == null ? null : dit
						.getDirCacheEntry();

				boolean conflicting = dirCacheEntry != null
						&& dirCacheEntry.getStage() > 0;

				AbstractTreeIterator rt = tw.getTree(repositoryTreeIndex,
						AbstractTreeIterator.class);

				// compare local file against HEAD to see if it was modified
				boolean modified = rt != null
						&& !fit.getEntryObjectId()
								.equals(rt.getEntryObjectId());

				// if this is neither conflicting nor changed, we skip it
				if (!conflicting && !modified)
					continue;

				ITypedElement right;
				if (conflicting) {
					GitFileRevision revision = GitFileRevision.inIndex(
							repository, gitPath, DirCacheEntry.STAGE_3);
					String encoding = CompareCoreUtils.getResourceEncoding(
							repository, gitPath);
					right = new FileRevisionTypedElement(revision, encoding);
				} else
					right = CompareUtils.getFileRevisionTypedElement(gitPath,
							headCommit, repository);

				// can this really happen?
				if (right instanceof EmptyTypedElement)
					continue;

				ITypedElement left;
				IFileRevision rev;
				// if the file is not conflicting (as it was auto-merged)
				// we will show the auto-merged (local) version

				Path repositoryPath = new Path(repository.getWorkTree()
						.getAbsolutePath());
				IPath location = repositoryPath
						.append(fit.getEntryPathString());
				assert location != null;
				IFile file = ResourceUtil.getFileForLocation(location, false);
				if (!conflicting || useWorkspace) {
					if (file != null) {
						left = SaveableCompareEditorInput
								.createFileElement(file);
					} else {
						left = new LocalNonWorkspaceTypedElement(repository,
								location);
					}
					if (left instanceof LocalResourceTypedElement) {
						((LocalResourceTypedElement) left)
								.setSharedDocumentListener(
										new LocalResourceSaver(
												(LocalResourceTypedElement) left));
					}
				} else {
					rev = GitFileRevision.inIndex(repository, gitPath,
							DirCacheEntry.STAGE_2);
					IRunnableContext runnableContext = getContainer();
					if (runnableContext == null) {
						runnableContext = PlatformUI.getWorkbench()
								.getProgressService();
						assert runnableContext != null;
					}
					if (file != null) {
						left = new ResourceEditableRevision(rev, file,
								runnableContext);
					} else {
						left = new LocationEditableRevision(rev, location,
								runnableContext);
					}
					// make sure we don't need a round trip later
					try {
						((EditableRevision) left).cacheContents(monitor);
					} catch (CoreException e) {
						throw new IOException(e.getMessage());
					}
				}

				int kind = Differencer.NO_CHANGE;
				if (conflicting)
					kind = Differencer.CONFLICTING;
				else if (modified)
					kind = Differencer.PSEUDO_CONFLICT;

				IDiffContainer fileParent = getFileParent(result,
						repositoryPath, file, location);

				ITypedElement ancestor = null;
				if (ancestorCommit != null) {
					ancestor = CompareUtils.getFileRevisionTypedElement(gitPath,
							ancestorCommit, repository);
					// we get an ugly black icon if we have an EmptyTypedElement
					// instead of null
					if (ancestor instanceof EmptyTypedElement) {
						ancestor = null;
					}
				}
				// create the node as child
				new DiffNode(fileParent, kind, ancestor, left, right);
			}
			return result;
		}
	}

	private IDiffContainer getFileParent(IDiffContainer root,
			IPath repositoryPath, IFile file, IPath location) {
		int projectSegment = -1;
		String projectName = null;
		if (file != null) {
			IProject project = file.getProject();
			IPath projectLocation = project.getLocation();
			if (projectLocation != null) {
				IPath projectPath = project.getLocation().makeRelativeTo(
						repositoryPath);
				projectSegment = projectPath.segmentCount() - 1;
				projectName = project.getName();
			}
		}

		IPath path = location.makeRelativeTo(repositoryPath);
		IDiffContainer child = root;
		for (int i = 0; i < path.segmentCount() - 1; i++) {
			if (i == projectSegment)
				child = getOrCreateChild(child, projectName, true);
			else
				child = getOrCreateChild(child, path.segment(i), false);
		}
		return child;
	}

	private DiffNode getOrCreateChild(IDiffContainer parent, final String name,
			final boolean projectMode) {
		for (IDiffElement child : parent.getChildren()) {
			if (child.getName().equals(name)) {
				return ((DiffNode) child);
			}
		}
		DiffNode child = new DiffNode(parent, Differencer.NO_CHANGE) {

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Image getImage() {
				if (projectMode)
					return PROJECT_IMAGE;
				else
					return FOLDER_IMAGE;
			}
		};
		return child;
	}

	private String readFile(File directory, String fileName) throws IOException {
		byte[] content = IO.readFully(new File(directory, fileName));
		// strip off the last LF
		int end = content.length;
		while (0 < end && content[end - 1] == '\n')
			end--;
		return RawParseUtils.decode(content, 0, end);
	}

	@Override
	public boolean canRunAsJob() {
		return true;
	}

	private static class LocalResourceSaver
			implements ISharedDocumentAdapterListener {

		LocalResourceTypedElement element;

		public LocalResourceSaver(LocalResourceTypedElement element) {
			this.element = element;
		}

		@Override
		public void handleDocumentConnected() {
			// Nothing
		}

		@Override
		public void handleDocumentDisconnected() {
			// Nothing
		}

		@Override
		public void handleDocumentFlushed() {
			try {
				element.saveDocument(true, null);
			} catch (CoreException e) {
				Activator.handleStatus(e.getStatus(), true);
			}
		}

		@Override
		public void handleDocumentDeleted() {
			// Nothing
		}

		@Override
		public void handleDocumentSaved() {
			// Nothing
		}

	}
}
