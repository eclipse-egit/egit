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
import org.eclipse.egit.ui.internal.UIText;
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
import org.eclipse.osgi.util.NLS;
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
		boolean useInternalMergeTool = DiffMergeSettings.useInternalMergeTool();
		CompareEditorInput input;
		if (useInternalMergeTool) {
			if (mergeMode == 0) {
				MergeModeDialog dlg = new MergeModeDialog(getShell(event));
				if (dlg.open() != Window.OK)
					return null;
				input = new GitMergeEditorInput(dlg.getMergeMode(), locations);
			} else {
				MergeInputMode mode = MergeInputMode.fromInteger(mergeMode);
				input = new GitMergeEditorInput(mode, locations);
			}
		} else {
			// TODO: for external merge we don't support yet other merge modes,
			// mergeModified() below need to be improved to distinguish
			// better between different new diff node types like
			// LocalResourceTypedElement or HiddenResourceTypedElement
			input = new GitMergeEditorInput(MergeInputMode.STAGE_2, locations);
		}
		if (useInternalMergeTool) {
			openMergeToolInternal(input);
		} else {
			openMergeToolExternal(input);
		}
		return null;
	}

	private static void openMergeToolInternal(CompareEditorInput input) {
		CompareUI.openCompareEditor(input);
	}

	private static void openMergeToolExternal(CompareEditorInput input)
			throws ExecutionException {
		final GitMergeEditorInput gitMergeInput = (GitMergeEditorInput) input;
		DiffContainerJob job = new DiffContainerJob(
				UIText.MergeToolActionHandler_openExternalMergeToolJobName,
				gitMergeInput);
		job.schedule();
		try {
			job.join();
		} catch (InterruptedException e) {
			Thread.interrupted();
			throw new ExecutionException(
					UIText.MergeToolActionHandler_openExternalMergeToolWaitInterrupted,
					e);
		}
		IDiffContainer diffCont = job.getDiffContainer();
		executeExternalToolForChildren(diffCont, job.getRepository());
	}

	private static void executeExternalToolForChildren(
			IDiffContainer diffCont, Repository repo)
			throws ExecutionException {
		if (diffCont != null && diffCont.hasChildren()) {
			IDiffElement[] difContChilds = diffCont.getChildren();
			for (IDiffElement diffElement : difContChilds) {
				int diffKind = diffElement.getKind();
				if (diffKind == Differencer.NO_CHANGE) {
					executeExternalToolForChildren(
							(IDiffContainer) diffElement, repo);
				} else if ((diffKind & Differencer.CONFLICTING) != 0) {
					try {
						mergeModified((DiffNode) diffElement, repo);
					} catch (IOException | CoreException e) {
						throw new ExecutionException(
								UIText.MergeToolActionHandler_externalMergeToolRunFailed,
								e);
					}
				}
			}
		}
	}

	private static void mergeModified(DiffNode node, Repository repo)
			throws IOException, CoreException {
		// get the left resource and revisions
		FileRevisionTypedElement leftRevision = (ResourceEditableRevision)node.getLeft();
		IResource leftResource = ((ResourceEditableRevision)node.getLeft()).getResource();
		FileRevisionTypedElement rightRevision = (FileRevisionTypedElement)node.getRight();
		FileRevisionTypedElement baseRevision = (FileRevisionTypedElement)node.getAncestor();
		// get the relative project path from right revision here
		String mergedFilePath = null;
		Repository repository = repo;
		if (leftResource != null) {
			IPath relativePath = ResourceUtil.getRepositoryRelativePath(
					leftResource.getRawLocation(), repository);
			mergedFilePath = relativePath == null ? leftResource.getName()
					: relativePath.toOSString();
		} else if (leftRevision != null) {
			mergedFilePath = leftRevision.getPath();
		}
		boolean isMergeSuccessful = true;
		FileElement merged = new FileElement(mergedFilePath,
				FileElement.Type.MERGED, repository.getWorkTree());
		long modifiedBefore = merged.getFile().lastModified();

		try {
			// create the merge tool manager
			MergeTools mergeTools = new MergeTools(repository);
			// get the selected tool name
			Optional<String> toolNameToUse = DiffMergeSettings
					.getMergeToolName(repository, mergedFilePath);
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
						ToolsUtils.informUser(
								UIText.MergeToolActionHandler_noToolConfiguredDialogTitle,
								UIText.MergeToolActionHandler_noToolConfiguredDialogContent);
					});
		} catch (ToolException e) {
			isMergeSuccessful = false;
			if (e.isCommandExecutionError()) {
				Activator.handleError(
						UIText.MergeToolActionHandler_mergeToolErrorDialogContent,
						e, true);
				return; // abort the merge process
			} else {
				Activator.logWarning("Failed to run external merge tool.", e); //$NON-NLS-1$
			}
		}
		// if merge was successful check file modified
		if (isMergeSuccessful) {
			long modifiedAfter = merged.getFile().lastModified();
			if (modifiedBefore == modifiedAfter) {
				int response = ToolsUtils.askUserAboutToolExecution(
						UIText.MergeToolActionHandler_mergeToolNoChangeDialogTitle,
						NLS.bind(
								UIText.MergeToolActionHandler_mergeToolNoChangeDialogContent,
								mergedFilePath));
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
			try (Git git = new Git(repository)) {
				git.add().addFilepattern(mergedFilePath).call();
				if (leftResource != null) {
					leftResource.getParent().refreshLocal(IResource.DEPTH_ONE,
							null);
				}
			} catch (GitAPIException e) {
				Activator.handleError(
						UIText.MergeToolActionHandler_mergeToolFailedAddMergedToGit,
						e, true);
			}
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
			int response = ToolsUtils.askUserAboutToolExecution(
					UIText.MergeToolActionHandler_mergeToolPromptDialogTitle,
					NLS.bind(
							UIText.MergeToolActionHandler_mergeToolPromptDialogContent,
							fileName, toolName));

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
