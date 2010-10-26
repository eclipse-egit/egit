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
	private final boolean useWorkspace;

	private final IResource[] resources;

	private DiffNode compareResult;

	private Image FOLDER_IMAGE = PlatformUI.getWorkbench().getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_FOLDER);

	private Image PROJECT_IMAGE = PlatformUI.getWorkbench().getSharedImages()
			.getImage(SharedImages.IMG_OBJ_PROJECT);

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
		final Set<IFile> files = new HashSet<IFile>();
		List<IContainer> folders = new ArrayList<IContainer>();

		for (IResource res : resources) {
			if (res.getType() == IResource.FILE)
				files.add((IFile) res);
			else
				folders.add((IContainer) res);
		}

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
		this.compareResult = new DiffNode(Differencer.CONFLICTING);
		try {
			for (IFile file : files) {
				fileToDiffNode(file, this.compareResult, monitor);
			}
		} catch (IOException e) {
			throw new InvocationTargetException(e);
		}
		return compareResult;
	}

	@Override
	protected void handleDispose() {
		super.handleDispose();
		// we do NOT dispose the images, as these are shared
	}

	private void fileToDiffNode(final IFile file, IDiffContainer root,
			IProgressMonitor monitor) throws IOException {
		CompareConfiguration config = getCompareConfiguration();

		RepositoryMapping map = RepositoryMapping.getMapping(file);
		String gitPath = map.getRepoRelativePath(file);

		// ignore everything in .git
		if (gitPath.startsWith(Constants.DOT_GIT))
			return;

		Repository repo = map.getRepository();
		// analyze if there are changes or conflicts
		RevWalk rw = new RevWalk(repo);
		TreeWalk tw = new TreeWalk(repo);

		List<String> paths = new ArrayList<String>();
		paths.add(map.getRepoRelativePath(file));
		tw.setFilter(PathFilterGroup.createFromStrings(paths));

		boolean conflicting = false;
		boolean automerged = false;

		int dcindex = tw.addTree(new DirCacheIterator(repo.readDirCache()));
		int ftindex = tw.addTree(new FileTreeIterator(repo));
		int rtindex = tw.addTree(rw.parseTree(repo.resolve(Constants.HEAD)));

		tw.setRecursive(tw.getFilter().shouldBeRecursive());
		tw.next();

		DirCacheIterator dit = tw.getTree(dcindex, DirCacheIterator.class);

		final DirCacheEntry indexEntry = dit == null ? null : dit
				.getDirCacheEntry();

		conflicting = indexEntry != null && indexEntry.getStage() > 0;

		AbstractTreeIterator rt = tw.getTree(rtindex,
				AbstractTreeIterator.class);

		FileTreeIterator fit = tw.getTree(ftindex, FileTreeIterator.class);
		// compare local file against HEAD to see if it was auto-merged
		automerged = fit != null && rt != null
				&& !fit.getEntryObjectId().equals(rt.getEntryObjectId());

		if (!conflicting && !automerged)
			return;

		RevCommit rightCommit;
		try {
			rightCommit = rw.parseCommit(repo.resolve(Constants.MERGE_HEAD));
		} catch (Exception e) {
			rightCommit = null;
		}

		RevCommit headCommit;
		try {
			headCommit = rw.parseCommit(repo.resolve(Constants.HEAD));
		} catch (IOException e) {
			headCommit = null;
		}

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

		if (headCommit == null || rightCommit == null)
			return;

		ITypedElement right = CompareUtils.getFileRevisionTypedElement(gitPath,
				rightCommit, repo);

		if (right instanceof EmptyTypedElement)
			return;
		config.setRightLabel(rightCommit.getName()
				+ " " + rightCommit.getShortMessage()); //$NON-NLS-1$

		IFileRevision rev;
		if (!useWorkspace) {
			rev = GitFileRevision.inCommit(repo, headCommit, gitPath, null);
			config.setLeftLabel(NLS.bind(
					UIText.GitMergeEditorInput_LastHeadHeader, headCommit
							.getName(), headCommit.getShortMessage()));
		} else {
			rev = new LocalFileRevision(file);
			config.setLeftLabel(UIText.GitMergeEditorInput_WorkspaceHeader);
		}

		EditableRevision leftEditable = new EditableRevision(rev) {

			@Override
			public void setContent(final byte[] newContent) {
				try {
					run(false, false, new IRunnableWithProgress() {
						public void run(IProgressMonitor myMonitor)
								throws InvocationTargetException,
								InterruptedException {
							try {
								file.refreshLocal(IResource.DEPTH_INFINITE,
										myMonitor);
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
		try {
			leftEditable.cacheContents(monitor);
		} catch (CoreException e) {
			// ignore here
			return;
		}
		int kind = Differencer.NO_CHANGE;
		if (conflicting)
			kind = Differencer.CONFLICTING;
		else if (automerged)
			kind = Differencer.PSEUDO_CONFLICT;

		DiffNode fileParent = getFileParent(root, file);

		if (ancestorCommit != null) {
			config.setAncestorLabel(ancestorCommit.getShortMessage());

			ITypedElement anc = CompareUtils.getFileRevisionTypedElement(
					gitPath, ancestorCommit, repo);
			new DiffNode(fileParent, kind, anc, leftEditable, right);
		} else {
			new DiffNode(fileParent, kind, null, leftEditable, right);
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
}
