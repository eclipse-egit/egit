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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.EditableRevision;
import org.eclipse.egit.ui.internal.LocalFileRevision;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput.EmptyTypedElement;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
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

	private DiffNode compareResult;

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
	public boolean isEditionSelectionDialog() {
		return true;
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		final Set<IFile> files = new HashSet<IFile>();
		List<IContainer> folders = new ArrayList<IContainer>();
		Set<IProject> projects = new HashSet<IProject>();

		// collect all projects and sort the selected
		// resources into files and folders
		for (IResource res : resources) {
			projects.add(res.getProject());
			if (res.getType() == IResource.FILE)
				files.add((IFile) res);
			else
				folders.add((IContainer) res);
		}

		if (monitor.isCanceled())
			throw new InterruptedException();

		// make sure all resources belong to the same repository
		Repository repo = null;
		for (IProject project : projects) {
			RepositoryMapping map = RepositoryMapping.getMapping(project);
			if (repo != null && repo != map.getRepository())
				throw new InvocationTargetException(
						new IllegalStateException(
								UIText.AbstractHistoryCommanndHandler_NoUniqueRepository));
			repo = map.getRepository();
		}

		if (repo == null)
			throw new InvocationTargetException(new IllegalStateException(
					UIText.AbstractHistoryCommanndHandler_NoUniqueRepository));

		if (monitor.isCanceled())
			throw new InterruptedException();

		// collect all file children of the selected folders
		IResourceVisitor fileCollector = new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				if (resource.getType() == IResource.FILE)
					files.add((IFile) resource);
				return true;
			}
		};

		for (IContainer cont : folders) {
			try {
				cont.accept(fileCollector);
			} catch (CoreException e) {
				// ignore here
			}
		}

		if (monitor.isCanceled())
			throw new InterruptedException();

		// our root node
		this.compareResult = new DiffNode(Differencer.CONFLICTING);

		final RevWalk rw = new RevWalk(repo);

		// get the "right" side (MERGE_HEAD)
		final RevCommit rightCommit;
		try {
			ObjectId mergeHead = repo.resolve(Constants.MERGE_HEAD);
			if (mergeHead == null)
				throw new IOException(NLS.bind(
						UIText.ValidationUtils_CanNotResolveRefMessage,
						Constants.MERGE_HEAD));
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

		// now we calculate the nodes containing the compare information
		try {
			for (IFile file : files) {
				if (monitor.isCanceled())
					throw new InterruptedException();

				monitor.setTaskName(file.getFullPath().toString());

				RepositoryMapping map = RepositoryMapping.getMapping(file);
				String gitPath = map.getRepoRelativePath(file);

				// ignore everything in .git
				if (gitPath.startsWith(Constants.DOT_GIT))
					continue;

				fileToDiffNode(file, gitPath, map, this.compareResult,
						rightCommit, headCommit, ancestorCommit, rw, monitor);
			}
		} catch (IOException e) {
			throw new InvocationTargetException(e);
		}
		return compareResult;
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

	private void fileToDiffNode(final IFile file, String gitPath,
			RepositoryMapping map, IDiffContainer root, RevCommit rightCommit,
			RevCommit headCommit, RevCommit ancestorCommit, RevWalk rw,
			IProgressMonitor monitor) throws IOException, InterruptedException {

		if (monitor.isCanceled())
			throw new InterruptedException();

		TreeWalk tw = new TreeWalk(map.getRepository());

		List<String> paths = new ArrayList<String>();
		paths.add(map.getRepoRelativePath(file));
		tw.setFilter(PathFilterGroup.createFromStrings(paths));

		int dcindex = tw.addTree(new DirCacheIterator(map.getRepository()
				.readDirCache()));
		int ftindex = tw.addTree(new FileTreeIterator(map.getRepository()));
		int rtindex = tw.addTree(rw.parseTree(map.getRepository().resolve(
				Constants.HEAD)));

		tw.setRecursive(tw.getFilter().shouldBeRecursive());
		tw.next();

		DirCacheIterator dit = tw.getTree(dcindex, DirCacheIterator.class);

		final DirCacheEntry indexEntry = dit == null ? null : dit
				.getDirCacheEntry();

		boolean conflicting = indexEntry != null && indexEntry.getStage() > 0;

		AbstractTreeIterator rt = tw.getTree(rtindex,
				AbstractTreeIterator.class);

		FileTreeIterator fit = tw.getTree(ftindex, FileTreeIterator.class);
		// compare local file against HEAD to see if it was modified
		boolean modified = fit != null && rt != null
				&& !fit.getEntryObjectId().equals(rt.getEntryObjectId());

		// if this is neither conflicting nor changed, we skip it
		if (!conflicting && !modified)
			return;

		ITypedElement right = CompareUtils.getFileRevisionTypedElement(gitPath,
				rightCommit, map.getRepository());

		// can this really happen?
		if (right instanceof EmptyTypedElement)
			return;

		IFileRevision rev;
		// if the file is not conflicting (as it was auto-merged)
		// we will show the auto-merged (local) version
		if (!conflicting || useWorkspace)
			rev = new LocalFileRevision(file);
		else
			rev = GitFileRevision.inCommit(map.getRepository(), headCommit,
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
								file.setContents(new ByteArrayInputStream(
										newContent), false, true, myMonitor);
							} catch (CoreException e) {
								throw new InvocationTargetException(e);
							}
						}
					});
				} catch (InvocationTargetException e) {
					Activator.handleError(e.getTargetException().getMessage(),
							e.getTargetException(), true);
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

		DiffNode fileParent = getFileParent(root, file);

		ITypedElement anc;
		if (ancestorCommit != null)
			anc = CompareUtils.getFileRevisionTypedElement(gitPath,
					ancestorCommit, map.getRepository());
		else
			anc = null;
		// create the node as child
		new DiffNode(fileParent, kind, anc, leftEditable, right);
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
}
