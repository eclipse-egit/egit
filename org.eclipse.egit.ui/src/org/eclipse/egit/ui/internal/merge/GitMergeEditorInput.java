/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.EditableRevision;
import org.eclipse.egit.ui.internal.LocalFileRevision;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput.EmptyTypedElement;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

/**
 * A Git-specific {@link CompareEditorInput}
 */
public class GitMergeEditorInput extends CompareEditorInput {
	private static final String LABELPATTERN = "{0} - {1}"; //$NON-NLS-1$

	private static final Image FOLDER_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

	private static final Image PROJECT_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(SharedImages.IMG_OBJ_PROJECT);

	private final boolean useWorkspace;

	private final IResource[] resources;

	/**
	 * @param useWorkspace
	 *            if <code>true</code>, use the workspace content (i.e. the
	 *            Git-merged version) as "left" content, otherwise use HEAD
	 *            (i.e. the previous, non-merged version)
	 * @param resources
	 *            as selected by the user
	 */
	public GitMergeEditorInput(boolean useWorkspace, IResource... resources) {
		super(new CompareConfiguration());
		this.useWorkspace = useWorkspace;
		this.resources = resources;
		CompareConfiguration config = getCompareConfiguration();
		config.setLeftEditable(true);
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
			List<String> filterPaths = new ArrayList<String>();
			Repository repo = null;
			for (IResource resource : resources) {
				RepositoryMapping map = RepositoryMapping.getMapping(resource
						.getProject());
				if (repo != null && repo != map.getRepository())
					throw new InvocationTargetException(
							new IllegalStateException(
									UIText.AbstractHistoryCommanndHandler_NoUniqueRepository));
				filterPaths.add(map.getRepoRelativePath(resource));
				repo = map.getRepository();
			}

			if (repo == null)
				throw new InvocationTargetException(
						new IllegalStateException(
								UIText.AbstractHistoryCommanndHandler_NoUniqueRepository));

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
							UIText.ValidationUtils_CanNotResolveRefMessage,
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
							UIText.ValidationUtils_CanNotResolveRefMessage,
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
			List<RevCommit> startPoints = new ArrayList<RevCommit>();
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
					.getShortMessage(), rightCommit.name()));

			if (!useWorkspace)
				config.setLeftLabel(NLS.bind(LABELPATTERN, headCommit
						.getShortMessage(), headCommit.name()));
			else
				config.setLeftLabel(UIText.GitMergeEditorInput_WorkspaceHeader);

			if (ancestorCommit != null)
				config.setAncestorLabel(NLS.bind(LABELPATTERN, ancestorCommit
						.getShortMessage(), ancestorCommit.name()));

			// set title and icon
			setTitle(NLS.bind(UIText.GitMergeEditorInput_MergeEditorTitle,
					new Object[] {
							Activator.getDefault().getRepositoryUtil()
									.getRepositoryName(repo),
							rightCommit.getShortMessage(), fullBranch }));

			// build the nodes
			try {
				return buildDiffContainer(repo, rightCommit, headCommit,
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

	private IDiffContainer buildDiffContainer(Repository repository,
			RevCommit rightCommit, RevCommit headCommit,
			RevCommit ancestorCommit, List<String> filterPaths, RevWalk rw,
			IProgressMonitor monitor) throws IOException, InterruptedException {

		monitor.setTaskName(UIText.GitMergeEditorInput_CalculatingDiffTaskName);
		IDiffContainer result = new DiffNode(Differencer.CONFLICTING);

		TreeWalk tw = new TreeWalk(repository);
		try {
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
				List<TreeFilter> suffixFilters = new ArrayList<TreeFilter>();
				for (String filterPath : filterPaths)
					suffixFilters.add(PathFilter.create(filterPath, tw.getPathEncoding()));
				TreeFilter otf = OrTreeFilter.create(suffixFilters);
				tw.setFilter(AndTreeFilter.create(otf, notIgnoredFilter));
			} else if (filterPaths.size() > 0)
				tw.setFilter(AndTreeFilter.create(PathFilter.create(filterPaths
						.get(0), tw.getPathEncoding()), notIgnoredFilter));
			else
				tw.setFilter(notIgnoredFilter);

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
				if (conflicting)
					right = CompareUtils.getFileRevisionTypedElement(gitPath,
							rightCommit, repository);
				else
					right = CompareUtils.getFileRevisionTypedElement(gitPath,
							headCommit, repository);

				// can this really happen?
				if (right instanceof EmptyTypedElement)
					continue;

				IFileRevision rev;
				// if the file is not conflicting (as it was auto-merged)
				// we will show the auto-merged (local) version

				IPath locationPath = new Path(fit.getEntryFile().getPath());
				final IFile file = ResourcesPlugin.getWorkspace().getRoot()
						.getFileForLocation(locationPath);
				if (file == null)
					// TODO in the future, we should be able to show a version
					// for a non-workspace file as well
					continue;
				if (!conflicting || useWorkspace)
					rev = new LocalFileRevision(file);
				else
					rev = GitFileRevision.inCommit(repository, headCommit,
							gitPath, null);

				EditableRevision leftEditable = new EditableRevision(rev) {
					@Override
					public void setContent(final byte[] newContent) {
						try {
							run(false, false, new IRunnableWithProgress() {
								public void run(IProgressMonitor myMonitor)
										throws InvocationTargetException,
										InterruptedException {
									try {
										file.setContents(
												new ByteArrayInputStream(
														newContent), false,
												true, myMonitor);
									} catch (CoreException e) {
										throw new InvocationTargetException(e);
									}
								}
							});
						} catch (InvocationTargetException e) {
							Activator
									.handleError(e.getTargetException()
											.getMessage(), e
											.getTargetException(), true);
						} catch (InterruptedException e) {
							// ignore here
						}
					}
				};
				// make sure we don't need a round trip later
				try {
					leftEditable.cacheContents(monitor);
				} catch (CoreException e) {
					throw new IOException(e.getMessage());
				}

				int kind = Differencer.NO_CHANGE;
				if (conflicting)
					kind = Differencer.CONFLICTING;
				else if (modified)
					kind = Differencer.PSEUDO_CONFLICT;

				DiffNode fileParent = getFileParent(result, file);

				ITypedElement anc;
				if (ancestorCommit != null)
					anc = CompareUtils.getFileRevisionTypedElement(gitPath,
							ancestorCommit, repository);
				else
					anc = null;
				// we get an ugly black icon if we have an EmptyTypedElement
				// instead of null
				if (anc instanceof EmptyTypedElement)
					anc = null;
				// create the node as child
				new DiffNode(fileParent, kind, anc, leftEditable, right);
			}
			return result;
		} finally {
			tw.release();
		}
	}

	private DiffNode getFileParent(IDiffContainer root, IFile file) {
		String projectName = file.getProject().getName();
		DiffNode child = getOrCreateChild(root, projectName, true);
		IPath path = file.getProjectRelativePath();
		for (int i = 0; i < path.segmentCount() - 1; i++)
			child = getOrCreateChild(child, path.segment(i), false);
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
}
