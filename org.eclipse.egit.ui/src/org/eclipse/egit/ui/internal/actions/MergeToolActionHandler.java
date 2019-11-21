/*******************************************************************************
 * Copyright (c) 2010, 2019, 2020 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Christian W. Damus - bug 544395
 *    Andre Bossert <andre.bossert@siemens.com> - external merge and diff tools
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.DiffContainerJob;
import org.eclipse.egit.ui.internal.ToolsUtils;
import org.eclipse.egit.ui.internal.diffmerge.DiffMergeSettings;
import org.eclipse.egit.ui.internal.merge.GitMergeEditorInput;
import org.eclipse.egit.ui.internal.merge.MergeInputMode;
import org.eclipse.egit.ui.internal.merge.MergeModeDialog;
import org.eclipse.egit.ui.internal.preferences.GitPreferenceRoot;
import org.eclipse.egit.ui.internal.revision.FileRevisionTypedElement;
import org.eclipse.egit.ui.internal.revision.ResourceEditableRevision;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.diffmergetool.FileElement;
import org.eclipse.jgit.internal.diffmergetool.MergeTools;
import org.eclipse.jgit.internal.diffmergetool.PromptContinueHandler;
import org.eclipse.jgit.internal.diffmergetool.ToolException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.internal.BooleanTriState;
import org.eclipse.swt.SWT;

/**
 * Action for selecting a commit and merging it with the current branch.
 */
public class MergeToolActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		int mergeMode = Activator.getDefault().getPreferenceStore().getInt(
				UIPreferences.MERGE_MODE);
		IPath[] locations = getSelectedLocations(event);
		CompareEditorInput input;
		if (mergeMode == 0) {
			MergeModeDialog dlg = new MergeModeDialog(getShell(event));
			if (dlg.open() != Window.OK)
				return null;
			input = new GitMergeEditorInput(dlg.getMergeMode(), locations);
		} else {
			MergeInputMode mode = MergeInputMode.fromInteger(mergeMode);
			input = new GitMergeEditorInput(mode, locations);
		}
		if (DiffMergeSettings.useInternalMergeTool()) {
			openMergeToolInternal(input);
		} else {
			openMergeToolExternal(input);
		}
		return null;
	}

	private static void openMergeToolInternal(CompareEditorInput input) {
		CompareUI.openCompareEditor(input);
	}

	private static void openMergeToolExternal(CompareEditorInput input) {
		final GitMergeEditorInput gitMergeInput = (GitMergeEditorInput) input;
		DiffContainerJob job = new DiffContainerJob(
				"Prepare filelist for external merge tools", gitMergeInput); //$NON-NLS-1$
		job.schedule();
		try {
			job.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		IDiffContainer diffCont = job.getDiffContainer();
		executeExternalToolForChildren(diffCont);
	}

	private static void executeExternalToolForChildren(
			IDiffContainer diffCont) {
		if (diffCont != null && diffCont.hasChildren()) {
			IDiffElement[] difContChilds = diffCont.getChildren();
			for (IDiffElement diffElement : difContChilds) {
				switch (diffElement.getKind()) {
				case Differencer.NO_CHANGE:
					executeExternalToolForChildren(
							(IDiffContainer) diffElement);
					break;
				case Differencer.PSEUDO_CONFLICT:
					break;
				case Differencer.CONFLICTING:
					try {
						mergeModified((DiffNode) diffElement);
					} catch (IOException | CoreException e) {
						e.printStackTrace();
						return;
					}
					break;
				}
			}
		}
	}

	private static void mergeModified(DiffNode node)
			throws IOException, CoreException {
		// get the left resource and revisions
		FileRevisionTypedElement leftRevision = (ResourceEditableRevision)node.getLeft();
		IResource leftResource = ((ResourceEditableRevision)node.getLeft()).getResource();
		FileRevisionTypedElement rightRevision = (FileRevisionTypedElement)node.getRight();
		FileRevisionTypedElement baseRevision = (FileRevisionTypedElement)node.getAncestor();
		// get the relative project path from right revision here
		String mergedFilePath = null;
		String mergedAbsoluteFilePath = null;
		if (leftResource != null) {
			mergedFilePath = leftResource.getName();
			mergedAbsoluteFilePath = leftResource.getRawLocation().toOSString();
		} else if (leftRevision != null) {
			mergedFilePath = leftRevision.getPath();
			mergedAbsoluteFilePath = mergedFilePath;
		}
		Repository repository = null;
		// get repo
		IPath[] paths = new Path[1];
		paths[0] = new Path(mergedAbsoluteFilePath);
		Map<Repository, Collection<String>> pathsByRepository = ResourceUtil
				.splitPathsByRepository(Arrays.asList(paths));
		Set<Repository> repos = pathsByRepository.keySet();
		if (repos.size() >= 1) {
			repository = repos.iterator().next();
		}
		if (repository == null) {
			return;
		}
		boolean isMergeSuccessful = true;
		FileElement merged = new FileElement(mergedFilePath,
				FileElement.Type.MERGED, repository.getWorkTree());
		long modifiedBefore = merged.getFile().lastModified();
		try {
			// create the merge tool manager
			MergeTools mergeTools = new MergeTools(repository);
			// get the selected tool name
			Optional<String> toolNameToUse = Optional.ofNullable(DiffMergeSettings.getMergeToolName());
			BooleanTriState prompt = BooleanTriState.FALSE;

			PromptContinueHandler promptContinueHandler = new FileNamePromptContinueHandler(
					mergedFilePath);

			// the parent directory for temp files (can be same as tempDir or just
			// the worktree dir)
			File tempDir = mergeTools.createTempDirectory();
			File tempFilesParent = tempDir != null ? tempDir : repository.getWorkTree();
			// create local, remote and optionally base file
			// elements
			FileElement local = createFileElement(leftRevision, mergedFilePath,
					FileElement.Type.LOCAL, repository, tempFilesParent, true);
			FileElement remote = createFileElement(rightRevision,
					mergedFilePath, FileElement.Type.REMOTE, repository,
					tempFilesParent, false);
			FileElement base = createFileElement(baseRevision, mergedFilePath,
					FileElement.Type.BASE, repository, tempFilesParent, false);
			/* ExecutionResult executionResult = */
			mergeTools.merge(local, remote, merged, base, tempDir,
					toolNameToUse, prompt, false, promptContinueHandler,
					tools -> {
						ToolsUtils.informUser("No tool configured.", //$NON-NLS-1$
								"No mergetool is set. Will try a preconfigured one now. To configure one open the git config settings."); //$NON-NLS-1$
					});
		} catch (ToolException e) {
			isMergeSuccessful = false;
			e.printStackTrace();
			if (e.isCommandExecutionError()) {
				ToolsUtils.informUserAboutError("mergetool - error", //$NON-NLS-1$
						e.getMessage() + "\n\nMerge aborted!"); //$NON-NLS-1$
				return; // abort the merge process
			}
		}
		// if merge was successful check file modified
		if (isMergeSuccessful) {
			long modifiedAfter = merged.getFile().lastModified();
			if (modifiedBefore == modifiedAfter) {
				int response = ToolsUtils.askUserAboutToolExecution(
						"mergetool - trustExitCode: false", //$NON-NLS-1$
						mergedFilePath
								+ " seems unchanged.\n\nWas the merge successful?"); //$NON-NLS-1$
				if (response == SWT.NO) {
					isMergeSuccessful = false;
				} else if (response == SWT.CANCEL) {
					return; // abort the merge process
				}
			}
		}
		// if automatically or manually successful
		// -> add the file to the index
		if (isMergeSuccessful && GitPreferenceRoot.autoAddToIndex()) {
			Git git = new Git(repository);
			try {
				git.add().addFilepattern(mergedFilePath).call();
			} catch (GitAPIException e) {
				e.printStackTrace();
			}
			git.close();
			repository.close();
		}
	}

	private static class FileNamePromptContinueHandler
			implements PromptContinueHandler {
		private final String fileName;

		public FileNamePromptContinueHandler(String fileName) {
			this.fileName = fileName;
		}

		@Override
		public boolean prompt(String toolName) {
			int response = ToolsUtils.askUserAboutToolExecution("mergetool", //$NON-NLS-1$
					"Merging file: " //$NON-NLS-1$
							+ fileName + "\n\nLaunch '" //$NON-NLS-1$
							+ toolName + "' ?"); //$NON-NLS-1$

			return response == SWT.YES;
		}
	}

	private static FileElement createFileElement(
			FileRevisionTypedElement revision, String path,
			FileElement.Type type, Repository repository, File tempFilesParent,
			boolean createWorktreeElement)
			throws CoreException, IOException {
		FileElement element = null;
		if (revision != null) {
			element = new FileElement(path, type,
					null, revision.getContents());
			element.createTempFile(tempFilesParent);
		} else if (createWorktreeElement) {
			element = new FileElement(path, type,
					repository.getWorkTree());
		}
		return element;
	}

	@Override
	public boolean isEnabled() {
		IPath[] paths = getSelectedLocations();
		Map<Repository, Collection<String>> pathsByRepository = ResourceUtil
				.splitPathsByRepository(Arrays.asList(paths));

		if (pathsByRepository.size() != 1)
			return false;

		Entry<Repository, Collection<String>> pathsEntry = pathsByRepository
				.entrySet().iterator().next();
		Repository repo = pathsEntry.getKey();
		Collection<String> selectedRepoPaths = pathsEntry.getValue();
		if (selectedRepoPaths.isEmpty())
			return false;

		IndexDiffCacheEntry entry = IndexDiffCache.INSTANCE
				.getIndexDiffCacheEntry(repo);
		if (entry == null || entry.getIndexDiff() == null)
			return false;

		Set<String> conflictingFiles = entry.getIndexDiff().getConflicting();
		if (conflictingFiles.isEmpty())
			return false;

		for (String selectedRepoPath : selectedRepoPaths) {
			Path selectedPath = new Path(selectedRepoPath);

			for (String conflictingFile : conflictingFiles)
				if (selectedPath.isPrefixOf(new Path(conflictingFile)))
					return true;
		}

		return false;
	}

	/**
	 * Git status of a selected resource can change independently of the
	 * workbench selection, so always check enabled.
	 *
	 * @return {@code true}, always
	 */
	@Override
	protected boolean alwaysCheckEnabled() {
		return true;
	}
}
