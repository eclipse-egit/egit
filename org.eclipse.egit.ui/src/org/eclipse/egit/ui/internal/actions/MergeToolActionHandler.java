/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.externaltools.DiffConteinerJob;
import org.eclipse.egit.ui.internal.externaltools.ITool;
import org.eclipse.egit.ui.internal.externaltools.ToolsUtils;
import org.eclipse.egit.ui.internal.merge.GitMergeEditorInput;
import org.eclipse.egit.ui.internal.merge.MergeModeDialog;
import org.eclipse.egit.ui.internal.preferences.GitPreferenceRoot;
import org.eclipse.egit.ui.internal.revision.FileRevisionTypedElement;
import org.eclipse.egit.ui.internal.revision.ResourceEditableRevision;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
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
			input = new GitMergeEditorInput(dlg.useWorkspace(), locations);
		} else {
			boolean useWorkspace = mergeMode == 1;
			input = new GitMergeEditorInput(useWorkspace, locations);
		}
		if (GitPreferenceRoot.useExternalMergeTool()) {
			openMergeToolExternal(input);
		} else {
			openMergeToolInternal(input);
		}
		return null;
	}

	private static void openMergeToolInternal(CompareEditorInput input) {
		CompareUI.openCompareEditor(input);
	}

	private static void openMergeToolExternal(CompareEditorInput input) {
		System.out.println(
				"---------------- openMergeToolExternal ----------------"); //$NON-NLS-1$

		final GitMergeEditorInput gitMergeInput = (GitMergeEditorInput) input;

		DiffConteinerJob job = new DiffConteinerJob(
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
			for(IDiffElement diffElement : difContChilds)
			{
				switch (diffElement.getKind()) {
				case Differencer.NO_CHANGE:
					executeExternalToolForChildren(
							(IDiffContainer) diffElement);
					break;
				case Differencer.PSEUDO_CONFLICT:
					break;
				case Differencer.CONFLICTING:
					DiffNode node = (DiffNode) diffElement;
					// get the left resource and revisions
					FileRevisionTypedElement leftRevision = (ResourceEditableRevision) node
							.getLeft();
					IFile leftResource = ((ResourceEditableRevision) node
							.getLeft()).getFile();
					FileRevisionTypedElement rightRevision = (FileRevisionTypedElement) node
							.getRight();
					FileRevisionTypedElement baseRevision = (FileRevisionTypedElement) node
							.getAncestor();
					// get file names
					String mergedAbsoluteFilePath = null;
					String mergedRelativeFilePath = null;
					String mergedFileName = null;
					String localAbsoluteFilePath = null;
					String remoteAbsoluteFilePath = null;
					String baseAbsoluteFilePath = null;
					String mergeCmd = null;
					String mergeBaseLessCmd = null;
					boolean prompt = false;
					boolean trustExitCode = true;
					// boolean keepBackup = false; // TODO
					boolean writeToTemp = false;
					boolean keepTemporaries = false;
					File mergedDirPath = null;
					File tempDirPath = null;
					File workDirPath = null;
					Repository repository = null;
					if (leftResource != null) {
						mergedAbsoluteFilePath = leftResource.getRawLocation()
								.toOSString();
						mergedFileName = leftResource.getName();
						mergedDirPath = leftResource.getRawLocation().toFile()
								.getParentFile();
						System.out.println("file: " //$NON-NLS-1$
								+ mergedAbsoluteFilePath);
					}
					if (mergedAbsoluteFilePath != null) {
						IPath[] paths = new Path[1];
						paths[0] = new Path(mergedAbsoluteFilePath);
						Map<Repository, Collection<String>> pathsByRepository = ResourceUtil
								.splitPathsByRepository(Arrays.asList(paths));
						Set<Repository> repos = pathsByRepository.keySet();
						if (repos.size() == 1) {
							repository = repos.iterator().next();
						}
						if (repository != null) {
							workDirPath = repository.getWorkTree();
						}
					}
					if (mergedAbsoluteFilePath != null && leftRevision != null
							&& rightRevision != null) {
						// get the relative project path from left revision here
						mergedRelativeFilePath = leftRevision.getPath();
						// get the tool
						ITool tool = GitPreferenceRoot.getExternalMergeTool();
						if (tool != null) {
							// get the command
							mergeCmd = tool.getCommand(); // empty or index = 0 is the default command (with $BASE support)
							mergeBaseLessCmd = tool.getCommand(1); // index = 1 is the alternative command (without $BASE support)
							// get other attribute values
							prompt = GitPreferenceRoot
									.getExternalMergeToolAttributeValueBoolean(
											tool.getName(), "prompt"); //$NON-NLS-1$
							trustExitCode = GitPreferenceRoot
									.getExternalMergeToolAttributeValueBoolean(
											tool.getName(), "trustExitCode"); //$NON-NLS-1$
							/*keepBackup = GitPreferenceRoot
									.getExternalMergeToolAttributeValueBoolean(
											tool.getName(), "keepBackup"); //$NON-NLS-1$*/
							writeToTemp = GitPreferenceRoot
									.getExternalMergeToolAttributeValueBoolean(
											tool.getName(), "writeToTemp"); //$NON-NLS-1$
							keepTemporaries = GitPreferenceRoot
									.getExternalMergeToolAttributeValueBoolean(
											tool.getName(), "keepTemporaries"); //$NON-NLS-1$
							// first check if we should ask user
							if (prompt) {
								int response = ToolsUtils.askUserAboutToolExecution(
										"mergetool", //$NON-NLS-1$
										"Merging file: " //$NON-NLS-1$
														+ mergedRelativeFilePath
												+ "\n\nLaunch '" //$NON-NLS-1$
												+ tool.getName()
												+ "' ?"); //$NON-NLS-1$
								if (response == SWT.NO) {
									break;
								} else if (response == SWT.CANCEL) {
									return;
								}
							}
							// check if temp dir should be created
							if (writeToTemp) {
								tempDirPath = ToolsUtils
										.createDirectoryForTempFiles();
								mergedDirPath = tempDirPath;
							}
							localAbsoluteFilePath = ToolsUtils.loadToTempFile(
									mergedDirPath,
									mergedFileName, "LOCAL", //$NON-NLS-1$
									leftRevision, writeToTemp);
							remoteAbsoluteFilePath = ToolsUtils.loadToTempFile(
									mergedDirPath,
									mergedFileName, "REMOTE", //$NON-NLS-1$
									rightRevision, writeToTemp);
							baseAbsoluteFilePath = ToolsUtils.loadToTempFile(
									mergedDirPath,
									mergedFileName, "BASE", //$NON-NLS-1$
									baseRevision, writeToTemp);
						}
					}
					// execute
					int exitCode = -1;
					try {
						exitCode = ToolsUtils.executeTool(workDirPath,
								mergedAbsoluteFilePath,
								localAbsoluteFilePath, remoteAbsoluteFilePath,
										baseAbsoluteFilePath,
										baseAbsoluteFilePath != null ? mergeCmd
												: mergeBaseLessCmd,
										tempDirPath);
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
						ToolsUtils.informUserAboutError("mergetool - error", //$NON-NLS-1$
								e.getMessage() + "\n\nMerge aborted!"); //$NON-NLS-1$
						return; // abort the merge process
					} finally {
						System.out.println("exitCode: " //$NON-NLS-1$
								+ Integer.toString(exitCode));
						// delete temp
						if (tempDirPath != null && !keepTemporaries) {
							ToolsUtils.deleteDirectoryForTempFiles(tempDirPath);
						}
					}
					// add file to stage if successfully merged
					boolean addFile = false;
					if (trustExitCode) {
						System.out.println("trustExitCode: true"); //$NON-NLS-1$
						if (exitCode == 0) {
							addFile = true;
						}
					} else {
						System.out.println("trustExitCode: false"); //$NON-NLS-1$
						int response = ToolsUtils.askUserAboutToolExecution(
								"mergetool - trustExitCode: false", //$NON-NLS-1$
								"Merging file: " //$NON-NLS-1$
										+ mergedRelativeFilePath
										+ "\n\nWas the merge successful?"); //$NON-NLS-1$
						if (response == SWT.YES) {
							addFile = true;
						} else if (response == SWT.CANCEL) {
							return; // abort the merge process
						}
					}
					if (repository != null) {
						if (addFile && GitPreferenceRoot.autoAddToIndex()
								&& mergedAbsoluteFilePath != null) {
							System.out.println("addFile: " //$NON-NLS-1$
									+ mergedFileName);
							Git git = new Git(repository);
							try {
								git.add().addFilepattern(mergedRelativeFilePath)
										.call();
							} catch (GitAPIException e) {
								e.printStackTrace();
							}
							git.close();
						}
						repository.close();
					}
					break;
				}
			}
		}
	}

	@Override
	public boolean isEnabled() {
		IPath[] paths = getSelectedLocations();
		Map<Repository, Collection<String>> pathsByRepository = ResourceUtil
				.splitPathsByRepository(Arrays.asList(paths));

		Set<Repository> repos = pathsByRepository.keySet();

		if (repos.size() != 1)
			return false;

		Repository repo = repos.iterator().next();
		Collection<String> selectedRepoPaths = pathsByRepository.get(repo);
		if (selectedRepoPaths.isEmpty())
			return false;

		IndexDiffCache cache = org.eclipse.egit.core.Activator.getDefault().getIndexDiffCache();
		if (cache == null)
			return false;

		IndexDiffCacheEntry entry = cache.getIndexDiffCacheEntry(repo);
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
}
