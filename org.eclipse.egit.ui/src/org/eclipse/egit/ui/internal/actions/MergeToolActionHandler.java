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
			openInCompareExternal(input);
		} else {
			openInCompareInternal(input);
		}
		return null;
	}

	private static void openInCompareInternal(CompareEditorInput input) {
		CompareUI.openCompareEditor(input);
	}

	private static void openInCompareExternal(CompareEditorInput input) {
		System.out.println(
				"---------------- openInCompare with external tool ------------------"); //$NON-NLS-1$

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
					String mergedCompareFilePath = null;
					String mergedCompareFileName = null;
					String localCompareFilePath = null;
					String remoteCompareFilePath = null;
					String baseCompareFilePath = null;
					String mergeCmd = null;
					boolean prompt = false;
					boolean trustExitCode = true;
					// boolean keepBackup = false; // TODO
					boolean writeToTemp = false;
					boolean keepTemporaries = false;
					File baseDir = null;
					File tempDir = null;
					if (leftResource != null) {
						mergedCompareFilePath = leftResource.getRawLocation()
								.toOSString();
						mergedCompareFileName = leftResource.getName();
						baseDir = leftResource.getRawLocation().toFile()
								.getParentFile();
						System.out.println("mergedCompareFilePath: " //$NON-NLS-1$
								+ mergedCompareFilePath);
					}
					if (mergedCompareFilePath != null && leftRevision != null
							&& rightRevision != null) {
						// get the tool
						ITool tool = GitPreferenceRoot.getExternalMergeTool();
						if (tool != null) {
							// get the command
							mergeCmd = tool.getCommand();
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
												+ mergedCompareFilePath
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
								tempDir = ToolsUtils
										.createDirectoryForTempFiles();
								baseDir = tempDir;
							}
							localCompareFilePath = ToolsUtils.loadToTempFile(baseDir,
									mergedCompareFileName, "LOCAL", //$NON-NLS-1$
									leftRevision, writeToTemp);
							remoteCompareFilePath = ToolsUtils.loadToTempFile(baseDir,
									mergedCompareFileName, "REMOTE", //$NON-NLS-1$
									rightRevision, writeToTemp);
							baseCompareFilePath = ToolsUtils.loadToTempFile(baseDir,
									mergedCompareFileName, "BASE", //$NON-NLS-1$
									baseRevision, writeToTemp);
						}
					}
					// execute
					int exitCode = ToolsUtils.executeTool(mergedCompareFilePath,
							localCompareFilePath,
							remoteCompareFilePath, baseCompareFilePath,
							mergeCmd, tempDir);
					// delete temp
					if (tempDir != null && !keepTemporaries) {
						ToolsUtils.deleteDirectoryForTempFiles(tempDir);
					}
					// add file to stage if successfully merged
					if (exitCode == 0) {
						if (!trustExitCode) {
							int response = ToolsUtils.askUserAboutToolExecution(
									"mergetool", //$NON-NLS-1$
									"Merging file: " //$NON-NLS-1$
											+ mergedCompareFilePath
											+ "\n\nWas the merge successful?"); //$NON-NLS-1$
							if (response == SWT.YES) {
								/*
								 * TODO: implement add
								AddCommand addCommand = new Git(repo).add();
								boolean fileAdded = false;
								for (String path : notTracked)
									if (commitFileList.contains(path)) {
										addCommand.addFilepattern(path);
										fileAdded = true;
									}
								if (fileAdded)
									try {
										addCommand.call();
									} catch (Exception e) {
										throw new CoreException(Activator
												.error(e.getMessage(), e));
									}
								*/
							} else if (response == SWT.CANCEL) {
								return;
							}
						}
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
