/*******************************************************************************
 * Copyright (c) 2010, SAP AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dariusz Luksza - add getFileCachedRevisionTypedElement(String, Repository)
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.File;
import java.io.IOException;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.internal.storage.WorkingTreeFileRevision;
import org.eclipse.egit.core.internal.storage.WorkspaceFileRevision;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.merge.GitCompareEditorInput;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
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
		IEclipsePreferences node = new InstanceScope().getNode(TEAM_UI_PLUGIN);

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
		boolean defaultReuse = new DefaultScope().getNode(TEAM_UI_PLUGIN)
				.getBoolean(REUSE_COMPARE_EDITOR_PREFID, false);
		return new InstanceScope().getNode(TEAM_UI_PLUGIN).getBoolean(
				REUSE_COMPARE_EDITOR_PREFID, defaultReuse);
	}

	private static void setReuseOpenEditor(boolean value) {
		new InstanceScope().getNode(TEAM_UI_PLUGIN).putBoolean(
				REUSE_COMPARE_EDITOR_PREFID, value);
	}

	/**
	 * Creates {@link ITypedElement} of file that was cached
	 *
	 * @param gitPath
	 * @param db
	 * @return {@link ITypedElement} instance for given cached file or
	 *         {@code null} if file isn't cached
	 */
	public static ITypedElement getFileCachedRevisionTypedElement(String gitPath,
			Repository db) {
		try {
			DirCache dc = db.lockDirCache();
			DirCacheEntry entry = dc.getEntry(gitPath);
			dc.unlock();

			// check if file is staged
			if (entry != null) {
				GitFileRevision nextFile = GitFileRevision.inIndex(db, gitPath);
				String encoding = CompareCoreUtils.getResourceEncoding(db, gitPath);
				return new FileRevisionTypedElement(nextFile, encoding);
			}
		} catch (IOException e) {
			Activator.error(NLS.bind(UIText.GitHistoryPage_errorLookingUpPath,
					gitPath), e);
		}

		return new GitCompareFileRevisionEditorInput.EmptyTypedElement(NLS
				.bind(UIText.CompareWithIndexAction_FileNotInIndex,
						gitPath.substring(gitPath.lastIndexOf("/") + 1))); //$NON-NLS-1$
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
		RevCommit headCommit = getHeadCommit(repository);
		if (headCommit == null)
			return;
		String path = RepositoryMapping.getMapping(file).getRepoRelativePath(
				file);
		ITypedElement base = CompareUtils.getFileRevisionTypedElement(path,
				headCommit, repository);
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
	 * Opens a compare editor. The working tree version of the given file is
	 * compared with the version in the HEAD commit. Use this method if the
	 * given file is outide the workspace.
	 *
	 * @param repository
	 * @param path
	 */
	public static void compareHeadWithWorkingTree(Repository repository,
			String path) {
		RevCommit headCommit = getHeadCommit(repository);
		if (headCommit == null)
			return;
		ITypedElement base = CompareUtils.getFileRevisionTypedElement(path,
				headCommit, repository);
		IFileRevision nextFile;
		nextFile = new WorkingTreeFileRevision(new File(
				repository.getWorkTree(), path));
		String encoding = ResourcesPlugin.getEncoding();
		ITypedElement next = new FileRevisionTypedElement(nextFile, encoding);
		GitCompareFileRevisionEditorInput input = new GitCompareFileRevisionEditorInput(
				next, base, null);
		CompareUI.openCompareDialog(input);
	}

	private static RevCommit getHeadCommit(Repository repository) {
		RevCommit headCommit;
		try {
			ObjectId objectId = repository.resolve(Constants.HEAD);
			if (objectId == null) {
				Activator.handleError(
						UIText.CompareUtils_errorGettingHeadCommit, null, true);
				return null;
			}
			headCommit = new RevWalk(repository).parseCommit(objectId);
		} catch (IOException e) {
			Activator.handleError(UIText.CompareUtils_errorGettingHeadCommit,
					e, true);
			return null;
		}
		return headCommit;
	}

}
