/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dariusz Luksza - add getFileCachedRevisionTypedElement(String, Repository)
 *    Stefan Lay (SAP AG) - initial implementation
 *    Yann Simon <yann.simon.fr@gmail.com> - implementation of getHeadTypedElement
 *    Robin Stocker <robin@nibor.org>
 *    Laurent Goubet <laurent.goubet@obeo.fr>
 *    Gunnar Wagenknecht <gunnar@wagenknecht.org>
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.RevUtils;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.internal.storage.WorkingTreeFileRevision;
import org.eclipse.egit.core.internal.storage.WorkspaceFileRevision;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput.EmptyTypedElement;
import org.eclipse.egit.ui.internal.actions.CompareWithCommitActionHandler;
import org.eclipse.egit.ui.internal.merge.GitCompareEditorInput;
import org.eclipse.egit.ui.internal.synchronize.compare.LocalNonWorkspaceTypedElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig.AutoCRLF;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.io.EolCanonicalizingInputStream;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
 * A collection of helper methods useful for comparing content
 */
public class CompareUtils {
	/**
	 * A copy of the non-accessible preference constant
	 * IPreferenceIds.REUSE_OPEN_COMPARE_EDITOR from the team ui plug in
	 */
	private static final String REUSE_COMPARE_EDITOR_PREFID = "org.eclipse.team.ui.reuse_open_compare_editors"; //$NON-NLS-1$

	/** The team ui plugin ID which is not accessible */
	private static final String TEAM_UI_PLUGIN = "org.eclipse.team.ui"; //$NON-NLS-1$

	/**
	 *
	 * @param gitPath
	 *            path within the commit's tree of the file.
	 * @param commit
	 *            the commit the blob was identified to be within.
	 * @param db
	 *            the repository this commit was loaded out of.
	 * @return an instance of {@link ITypedElement} which can be used in
	 *         {@link CompareEditorInput}
	 */
	public static ITypedElement getFileRevisionTypedElement(
			final String gitPath, final RevCommit commit, final Repository db) {
		return getFileRevisionTypedElement(gitPath, commit, db, null);
	}

	/**
	 * @param gitPath
	 *            path within the commit's tree of the file.
	 * @param commit
	 *            the commit the blob was identified to be within.
	 * @param db
	 *            the repository this commit was loaded out of, and that this
	 *            file's blob should also be reachable through.
	 * @param blobId
	 *            unique name of the content.
	 * @return an instance of {@link ITypedElement} which can be used in
	 *         {@link CompareEditorInput}
	 */
	public static ITypedElement getFileRevisionTypedElement(
			final String gitPath, final RevCommit commit, final Repository db,
			ObjectId blobId) {
		ITypedElement right = new GitCompareFileRevisionEditorInput.EmptyTypedElement(
				NLS.bind(UIText.GitHistoryPage_FileNotInCommit,
						getName(gitPath), commit));

		try {
			IFileRevision nextFile = getFileRevision(gitPath, commit, db,
							blobId);
				if (nextFile != null) {
					String encoding = CompareCoreUtils.getResourceEncoding(db, gitPath);
					right = new FileRevisionTypedElement(nextFile, encoding);
				}
		} catch (IOException e) {
			Activator.error(NLS.bind(UIText.GitHistoryPage_errorLookingUpPath,
					gitPath, commit.getId()), e);
		}
		return right;
	}

	private static String getName(String gitPath) {
		final int last = gitPath.lastIndexOf('/');
		return last >= 0 ? gitPath.substring(last + 1) : gitPath;
	}

	/**
	 *
	 * @param gitPath
	 *            path within the commit's tree of the file.
	 * @param commit
	 *            the commit the blob was identified to be within.
	 * @param db
	 *            the repository this commit was loaded out of, and that this
	 *            file's blob should also be reachable through.
	 * @param blobId
	 *            unique name of the content.
	 * @return an instance of {@link IFileRevision} or null if the file is not
	 *         contained in {@code commit}
	 * @throws IOException
	 */
	public static IFileRevision getFileRevision(final String gitPath,
			final RevCommit commit, final Repository db, ObjectId blobId)
			throws IOException {

		TreeWalk w = TreeWalk.forPath(db, gitPath, commit.getTree());
		// check if file is contained in commit
		if (w != null) {
			final IFileRevision fileRevision = GitFileRevision.inCommit(db,
					commit, gitPath, blobId);
			return fileRevision;
		}
		return null;
	}


	/**
	 * Creates a {@link ITypedElement} for the commit which is the common ancestor of
	 * the provided commits.
	 * @param gitPath
	 *            path within the ancestor commit's tree of the file.
	 * @param commit1
	 * @param commit2
	 * @param db
	 *            the repository this commit was loaded out of.
	 * @return an instance of {@link ITypedElement} which can be used in
	 *         {@link CompareEditorInput}
	 */
	public static ITypedElement getFileRevisionTypedElementForCommonAncestor(
			final String gitPath, ObjectId commit1, ObjectId commit2,
			Repository db) {
		ITypedElement ancestor = null;
		RevCommit commonAncestor = null;
		try {
			commonAncestor = RevUtils.getCommonAncestor(db, commit1, commit2);
		} catch (IOException e) {
			Activator.logError(NLS.bind(UIText.CompareUtils_errorCommonAncestor,
					commit1.getName(), commit2.getName()), e);
		}
		if (commonAncestor != null)
			ancestor = CompareUtils
				.getFileRevisionTypedElement(gitPath, commonAncestor, db);
		return ancestor;
	}
/**
	 * @param element
	 * @param adapterType
	 * @return the adapted element, or null
	 */
	public static Object getAdapter(Object element, Class adapterType) {
		return getAdapter(element, adapterType, false);
	}

	/**
	 * @param ci
	 * @return a truncated revision identifier if it is long
	 */
	public static String truncatedRevision(String ci) {
		if (ci.length() > 10)
			return ci.substring(0, 7) + "..."; //$NON-NLS-1$
		else
			return ci;
	}

	/**
	 * @param element
	 * @param adapterType
	 * @param load
	 * @return the adapted element, or null
	 */
	private static Object getAdapter(Object element, Class adapterType,
			boolean load) {
		if (adapterType.isInstance(element))
			return element;
		if (element instanceof IAdaptable) {
			Object adapted = ((IAdaptable) element).getAdapter(adapterType);
			if (adapterType.isInstance(adapted))
				return adapted;
		}
		if (load) {
			Object adapted = Platform.getAdapterManager().loadAdapter(element,
					adapterType.getName());
			if (adapterType.isInstance(adapted))
				return adapted;
		} else {
			Object adapted = Platform.getAdapterManager().getAdapter(element,
					adapterType);
			if (adapterType.isInstance(adapted))
				return adapted;
		}
		return null;
	}

	/**
	 * @param workBenchPage
	 * @param input
	 */
	public static void openInCompare(IWorkbenchPage workBenchPage,
			CompareEditorInput input) {
		IEditorPart editor = findReusableCompareEditor(input, workBenchPage);
		if (editor != null) {
			IEditorInput otherInput = editor.getEditorInput();
			if (otherInput.equals(input)) {
				// simply provide focus to editor
				if (OpenStrategy.activateOnOpen())
					workBenchPage.activate(editor);
				else
					workBenchPage.bringToTop(editor);
			} else {
				// if editor is currently not open on that input either re-use
				// existing
				CompareUI.reuseCompareEditor(input, (IReusableEditor) editor);
				if (OpenStrategy.activateOnOpen())
					workBenchPage.activate(editor);
				else
					workBenchPage.bringToTop(editor);
			}
		} else {
			CompareUI.openCompareEditor(input);
		}
	}

	private static IEditorPart findReusableCompareEditor(
			CompareEditorInput input, IWorkbenchPage page) {
		IEditorReference[] editorRefs = page.getEditorReferences();
		// first loop looking for an editor with the same input
		for (int i = 0; i < editorRefs.length; i++) {
			IEditorPart part = editorRefs[i].getEditor(false);
			if (part != null
					&& (part.getEditorInput() instanceof GitCompareFileRevisionEditorInput || part.getEditorInput() instanceof GitCompareEditorInput)
					&& part instanceof IReusableEditor
					&& part.getEditorInput().equals(input)) {
				return part;
			}
		}
		// if none found and "Reuse open compare editors" preference is on use
		// a non-dirty editor
		if (isReuseOpenEditor()) {
			for (int i = 0; i < editorRefs.length; i++) {
				IEditorPart part = editorRefs[i].getEditor(false);
				if (part != null
						&& (part.getEditorInput() instanceof SaveableCompareEditorInput)
						&& part instanceof IReusableEditor && !part.isDirty()) {
					return part;
				}
			}
		}
		// no re-usable editor found
		return null;
	}

	/**
	 * Action to toggle the team 'reuse compare editor' preference
	 */
	public static class ReuseCompareEditorAction extends Action implements
			IPreferenceChangeListener, IWorkbenchAction {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(TEAM_UI_PLUGIN);

		/**
		 * Default constructor
		 */
		public ReuseCompareEditorAction() {
			node.addPreferenceChangeListener(this);
			setText(UIText.GitHistoryPage_ReuseCompareEditorMenuLabel);
			setChecked(CompareUtils.isReuseOpenEditor());
		}

		public void run() {
			CompareUtils.setReuseOpenEditor(isChecked());
		}

		public void dispose() {
			// stop listening
			node.removePreferenceChangeListener(this);
		}

		public void preferenceChange(PreferenceChangeEvent event) {
			setChecked(isReuseOpenEditor());

		}
	}

	private static boolean isReuseOpenEditor() {
		boolean defaultReuse = DefaultScope.INSTANCE.getNode(TEAM_UI_PLUGIN)
				.getBoolean(REUSE_COMPARE_EDITOR_PREFID, false);
		return InstanceScope.INSTANCE.getNode(TEAM_UI_PLUGIN).getBoolean(
				REUSE_COMPARE_EDITOR_PREFID, defaultReuse);
	}

	private static void setReuseOpenEditor(boolean value) {
		InstanceScope.INSTANCE.getNode(TEAM_UI_PLUGIN).putBoolean(
				REUSE_COMPARE_EDITOR_PREFID, value);
	}

	/**
	 * Opens a compare editor. The workspace version of the given file is
	 * compared with the version in the HEAD commit.
	 *
	 * @param repository
	 * @param file
	 */
	public static void compareHeadWithWorkspace(Repository repository,
			IFile file) {
		String path = RepositoryMapping.getMapping(file).getRepoRelativePath(
				file);
		ITypedElement base = getHeadTypedElement(repository, path);
		if (base == null)
			return;

		IFileRevision nextFile = new WorkspaceFileRevision(file);
		String encoding = null;
		try {
			encoding = file.getCharset();
		} catch (CoreException e) {
			Activator.handleError(UIText.CompareUtils_errorGettingEncoding, e, true);
		}
		ITypedElement next = new FileRevisionTypedElement(nextFile, encoding);
		GitCompareFileRevisionEditorInput input = new GitCompareFileRevisionEditorInput(
				next, base, null);
		CompareUI.openCompareDialog(input);
	}

	/**
	 * Opens a compare editor comparing the working directory version of the
	 * given IFile with the version of that file corresponding to
	 * {@code refName}.
	 *
	 * @param repository
	 *            The repository to load file revisions from.
	 * @param file
	 *            File to compare revisions for.
	 * @param refName
	 *            Reference to compare with the workspace version of
	 *            {@code file}. Can be either a commit ID, a reference or a
	 *            branch name.
	 * @param page
	 *            If not {@null} try to re-use a compare editor on this
	 *            page if any is available. Otherwise open a new one.
	 * @throws IOException
	 *             If HEAD or {@code refName} can't be resolved in the given
	 *             repository.
	 */
	public static void compareWorkspaceWithRef(Repository repository,
			IFile file, String refName, IWorkbenchPage page) throws IOException {
		final RepositoryMapping mapping = RepositoryMapping.getMapping(file);
		final String gitPath = mapping.getRepoRelativePath(file);
		final ITypedElement base = SaveableCompareEditorInput
				.createFileElement(file);

		CompareEditorInput in = prepareCompareInput(repository, gitPath, base,
				refName);

		if (page != null)
			openInCompare(page, in);
		else
			CompareUI.openCompareEditor(in);
	}

	/**
	 * Opens a compare editor comparing the working directory version of the
	 * given File with the version corresponding to {@code refName} of the same
	 * file.
	 *
	 * @param repository
	 *            The repository to load file revisions from.
	 * @param file
	 *            File to compare revisions for.
	 * @param refName
	 *            Reference to compare with the workspace version of
	 *            {@code file}. Can be either a commit ID, a reference or a
	 *            branch name.
	 * @param page
	 *            If not {@null} try to re-use a compare editor on this
	 *            page if any is available. Otherwise open a new one.
	 * @throws IOException
	 *             If HEAD or {@code refName} can't be resolved in the given
	 *             repository.
	 */
	public static void compareLocalWithRef(Repository repository, File file,
			String refName, IWorkbenchPage page) throws IOException {
		final String gitPath = getRepoRelativePath(repository, file);
		final ITypedElement base = new LocalNonWorkspaceTypedElement(new Path(
				file.getAbsolutePath()));

		CompareEditorInput in = prepareCompareInput(repository, gitPath, base,
				refName);

		if (page != null)
			openInCompare(page, in);
		else
			CompareUI.openCompareEditor(in);
	}

	/*
	 * Creates a compare input that can be used to compare a given local file
	 * with another reference. The given "base" element should always reflect a
	 * local file, either in the workspace (IFile) or on the file system
	 * (java.io.File) since we'll use "HEAD" to find a common ancestor of this
	 * base and the reference we compare it with.
	 */
	private static CompareEditorInput prepareCompareInput(
			Repository repository, String gitPath, ITypedElement base,
			String refName) throws IOException {
		final ObjectId destCommitId = repository.resolve(refName);
		RevWalk rw = new RevWalk(repository);
		RevCommit commit = rw.parseCommit(destCommitId);
		rw.release();
		final ITypedElement destCommit = getFileRevisionTypedElement(gitPath,
				commit, repository);

		final ITypedElement commonAncestor;
		if (base != null && commit != null) {
			final ObjectId headCommitId = repository.resolve(Constants.HEAD);
			commonAncestor = getFileRevisionTypedElementForCommonAncestor(
					gitPath, headCommitId, destCommitId, repository);
		} else {
			commonAncestor = null;
		}

		final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
				base, destCommit, commonAncestor, null);
		in.getCompareConfiguration().setRightLabel(refName);
		return in;
	}

	private static String getRepoRelativePath(Repository repository, File file) {
		IPath workdirPath = new Path(repository.getWorkTree().getPath());
		IPath filePath = new Path(file.getPath()).setDevice(null);
		return filePath.removeFirstSegments(workdirPath.segmentCount())
				.toString();
	}

	/**
	 * Opens a compare editor. The working tree version of the given file is
	 * compared with the version in the HEAD commit. Use this method if the
	 * given file is outide the workspace.
	 *
	 * @param repository
	 * @param path
	 */
	public static void compareHeadWithWorkingTree(Repository repository,
			String path) {
		ITypedElement base = getHeadTypedElement(repository, path);
		if (base == null)
			return;
		IFileRevision nextFile;
		nextFile = new WorkingTreeFileRevision(new File(
				repository.getWorkTree(), path));
		String encoding = ResourcesPlugin.getEncoding();
		ITypedElement next = new FileRevisionTypedElement(nextFile, encoding);
		GitCompareFileRevisionEditorInput input = new GitCompareFileRevisionEditorInput(
				next, base, null);
		CompareUI.openCompareDialog(input);
	}

	/**
	 * Get a typed element for the file as contained in HEAD. Tries to return
	 * the last commit that modified the file in order to have more useful
	 * author information.
	 * <p>
	 * Returns an empty typed element if there is not yet a head (initial import
	 * case).
	 * <p>
	 * If there is an error getting the HEAD commit, it is handled and null
	 * returned.
	 *
	 * @param repository
	 * @param repoRelativePath
	 * @return typed element, or null if there was an error getting the HEAD
	 *         commit
	 */
	public static ITypedElement getHeadTypedElement(Repository repository, String repoRelativePath) {
		try {
			Ref head = repository.getRef(Constants.HEAD);
			if (head == null || head.getObjectId() == null)
				// Initial import, not yet a HEAD commit
				return new EmptyTypedElement(""); //$NON-NLS-1$

			RevCommit latestFileCommit;
			RevWalk rw = new RevWalk(repository);
			try {
				RevCommit headCommit = rw.parseCommit(head.getObjectId());
				rw.markStart(headCommit);
				rw.setTreeFilter(AndTreeFilter.create(
						PathFilter.create(repoRelativePath),
						TreeFilter.ANY_DIFF));
				latestFileCommit = rw.next();
				// Fall back to HEAD
				if (latestFileCommit == null)
					latestFileCommit = headCommit;
			} finally {
				rw.release();
			}

			return CompareUtils.getFileRevisionTypedElement(repoRelativePath, latestFileCommit, repository);
		} catch (IOException e) {
			Activator.handleError(UIText.CompareUtils_errorGettingHeadCommit,
					e, true);
			return null;
		}
	}

	/**
	 * Get a typed element for the file in the index.
	 *
	 * @param baseFile
	 * @return typed element
	 * @throws IOException
	 */
	public static ITypedElement getIndexTypedElement(final IFile baseFile)
			throws IOException {
		final RepositoryMapping mapping = RepositoryMapping.getMapping(baseFile);
		final Repository repository = mapping.getRepository();
		final String gitPath = mapping.getRepoRelativePath(baseFile);
		final String encoding = CompareCoreUtils.getResourceEncoding(baseFile);
		return getIndexTypedElement(repository, gitPath, encoding);
	}

	/**
	 * Get a typed element for the repository and repository-relative path in the index.
	 *
	 * @param repository
	 * @param repoRelativePath
	 * @return typed element
	 * @throws IOException
	 */
	public static ITypedElement getIndexTypedElement(
			final Repository repository, final String repoRelativePath)
			throws IOException {
		String encoding = CompareCoreUtils.getResourceEncoding(repository, repoRelativePath);
		return getIndexTypedElement(repository, repoRelativePath, encoding);
	}

	private static ITypedElement getIndexTypedElement(
			final Repository repository, final String gitPath, String encoding) {
		IFileRevision nextFile = GitFileRevision.inIndex(repository, gitPath);
		final EditableRevision next = new EditableRevision(nextFile, encoding);

		IContentChangeListener listener = new IContentChangeListener() {
			public void contentChanged(IContentChangeNotifier source) {
				final byte[] newContent = next.getModifiedContent();
				setIndexEntryContents(repository, gitPath, newContent);
			}
		};

		next.addContentChangeListener(listener);
		return next;
	}


	/**
	 * Extracted from {@link CompareWithCommitActionHandler}
	 * @param actLeft
	 * @param actRight
	 * @return compare input
	 */
	public static DiffNode prepareGitCompare(ITypedElement actLeft, ITypedElement actRight) {
		if (actLeft.getType().equals(ITypedElement.FOLDER_TYPE)) {
			//			return new MyDiffContainer(null, left,right);
			DiffNode diffNode = new DiffNode(null,Differencer.CHANGE,null,actLeft,actRight);
			ITypedElement[] lc = (ITypedElement[])((IStructureComparator)actLeft).getChildren();
			ITypedElement[] rc = (ITypedElement[])((IStructureComparator)actRight).getChildren();
			int li=0;
			int ri=0;
			while (li<lc.length && ri<rc.length) {
				ITypedElement ln = lc[li];
				ITypedElement rn = rc[ri];
				int compareTo = ln.getName().compareTo(rn.getName());
				// TODO: Git ordering!
				if (compareTo == 0) {
					if (!ln.equals(rn))
						diffNode.add(prepareGitCompare(ln,rn));
					++li;
					++ri;
				} else if (compareTo < 0) {
					DiffNode childDiffNode = new DiffNode(Differencer.ADDITION, null, ln, null);
					diffNode.add(childDiffNode);
					if (ln.getType().equals(ITypedElement.FOLDER_TYPE)) {
						ITypedElement[] children = (ITypedElement[])((IStructureComparator)ln).getChildren();
						if(children != null && children.length > 0) {
							for (ITypedElement child : children) {
								childDiffNode.add(addDirectoryFiles(child, Differencer.ADDITION));
							}
						}
					}
					++li;
				} else {
					DiffNode childDiffNode = new DiffNode(Differencer.DELETION, null, null, rn);
					diffNode.add(childDiffNode);
					if (rn.getType().equals(ITypedElement.FOLDER_TYPE)) {
						ITypedElement[] children = (ITypedElement[])((IStructureComparator)rn).getChildren();
						if(children != null && children.length > 0) {
							for (ITypedElement child : children) {
								childDiffNode.add(addDirectoryFiles(child, Differencer.DELETION));
							}
						}
					}
					++ri;
				}
			}
			while (li<lc.length) {
				ITypedElement ln = lc[li];
				DiffNode childDiffNode = new DiffNode(Differencer.ADDITION, null, ln, null);
				diffNode.add(childDiffNode);
				if (ln.getType().equals(ITypedElement.FOLDER_TYPE)) {
					ITypedElement[] children = (ITypedElement[])((IStructureComparator)ln).getChildren();
					if(children != null && children.length > 0) {
						for (ITypedElement child : children) {
							childDiffNode.add(addDirectoryFiles(child, Differencer.ADDITION));
						}
					}
				}
				++li;
			}
			while (ri<rc.length) {
				ITypedElement rn = rc[ri];
				DiffNode childDiffNode = new DiffNode(Differencer.DELETION, null, null, rn);
				diffNode.add(childDiffNode);
				if (rn.getType().equals(ITypedElement.FOLDER_TYPE)) {
					ITypedElement[] children = (ITypedElement[])((IStructureComparator)rn).getChildren();
					if(children != null && children.length > 0) {
						for (ITypedElement child : children) {
							childDiffNode.add(addDirectoryFiles(child, Differencer.DELETION));
						}
					}
				}
				++ri;
			}
			return diffNode;
		} else {
			return new DiffNode(actLeft, actRight);
		}
	}

	/**
	 * Extracted from {@link CompareWithCommitActionHandler}
	 * @param elem
	 * @param diffType
	 * @return diffnode
	 */
	private static DiffNode addDirectoryFiles(ITypedElement elem, int diffType) {
		ITypedElement l = null;
		ITypedElement r = null;
		if (diffType == Differencer.DELETION) {
			r = elem;
		} else {
			l = elem;
		}

		if (elem.getType().equals(ITypedElement.FOLDER_TYPE)) {
			DiffNode diffNode = null;
			diffNode = new DiffNode(null,Differencer.CHANGE,null,l,r);
			ITypedElement[] children = (ITypedElement[])((IStructureComparator)elem).getChildren();
			for (ITypedElement child : children) {
				diffNode.add(addDirectoryFiles(child, diffType));
			}
			return diffNode;
		} else {
			return new DiffNode(diffType, null, l, r);
		}
	}

	/**
	 * Set contents on index entry of specified path. Line endings of contents
	 * are canonicalized if configured.
	 *
	 * @param repository
	 * @param gitPath
	 * @param newContent
	 *            content with working directory line endings
	 */
	private static void setIndexEntryContents(final Repository repository,
			final String gitPath, final byte[] newContent) {
		DirCache cache = null;
		try {
			cache = repository.lockDirCache();
			DirCacheEditor editor = cache.editor();
			if (newContent.length == 0) {
				editor.add(new DirCacheEditor.DeletePath(gitPath));
			} else {
				int length;
				byte[] content;
				WorkingTreeOptions workingTreeOptions = repository.getConfig()
						.get(WorkingTreeOptions.KEY);
				AutoCRLF autoCRLF = workingTreeOptions.getAutoCRLF();
				switch (autoCRLF) {
				case FALSE:
					content = newContent;
					length = newContent.length;
					break;
				case INPUT:
				case TRUE:
					EolCanonicalizingInputStream in = new EolCanonicalizingInputStream(
							new ByteArrayInputStream(newContent), true);
					// Canonicalization should lead to same or shorter length
					// (CRLF to LF), so we don't have to expand the byte[].
					content = new byte[newContent.length];
					length = IO.readFully(in, content, 0);
					break;
				default:
					throw new IllegalArgumentException(
							"Unknown autocrlf option " + autoCRLF); //$NON-NLS-1$
				}

				editor.add(new DirCacheEntryEditor(gitPath, repository,
						content, length));
			}
			try {
				editor.commit();
			} catch (RuntimeException e) {
				if (e.getCause() instanceof IOException)
					throw (IOException) e.getCause();
				else
					throw e;
			}

		} catch (IOException e) {
			Activator.handleError(
					UIText.CompareWithIndexAction_errorOnAddToIndex, e, true);
		} finally {
			if (cache != null)
				cache.unlock();
		}
	}

	private static class DirCacheEntryEditor extends DirCacheEditor.PathEdit {

		private final Repository repo;

		private final byte[] content;
		private final int contentLength;

		public DirCacheEntryEditor(String path, Repository repo,
				byte[] content, int contentLength) {
			super(path);
			this.repo = repo;
			this.content = content;
			this.contentLength = contentLength;
		}

		@Override
		public void apply(DirCacheEntry ent) {
			ObjectInserter inserter = repo.newObjectInserter();
			if (ent.getFileMode() != FileMode.REGULAR_FILE)
				ent.setFileMode(FileMode.REGULAR_FILE);

			ent.setLength(contentLength);
			ent.setLastModified(System.currentTimeMillis());
			try {
				ent.setObjectId(inserter.insert(Constants.OBJ_BLOB, content, 0,
						contentLength));
				inserter.flush();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	/**
	 * Indicates if it is OK to open the selected file directly in a compare
	 * editor.
	 * <p>
	 * It is not OK to show the single file if the file is part of a
	 * logical model element that spans multiple files.
	 * </p>
	 *
	 * @param file
	 *            file the user is trying to compare
	 * @return <code>true</code> if the file can be opened directly in a compare
	 *         editor, <code>false</code> if the synchronize view should be
	 *         opened instead.
	 */
	public static boolean canDirectlyOpenInCompare(IFile file) {
		/*
		 * Note : it would be better to use a remote context here in order to
		 * give the model provider a chance to resolve the remote logical model
		 * instead of only relying on the local one. However, this might be a
		 * long operation and would not really provide more context : we're
		 * trying to determine if the local file can be compared alone, this can
		 * be done by relying on the local model only.
		 */
		final ResourceMapping[] mappings = ResourceUtil.getResourceMappings(
				file, ResourceMappingContext.LOCAL_CONTEXT);

		for (ResourceMapping mapping : mappings) {
			try {
				final ResourceTraversal[] traversals = mapping.getTraversals(
						ResourceMappingContext.LOCAL_CONTEXT, null);
				for (ResourceTraversal traversal : traversals) {
					final IResource[] resources = traversal.getResources();
					for (IResource resource : resources) {
						if (!resource.equals(file))
							return false;
					}
				}
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		return true;
	}
}
