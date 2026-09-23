/*******************************************************************************
 * Copyright (c) 2026 Vector Informatik GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.io.File;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests whether an existing compare editor can or cannot be reused
 */
public class CompareEditorReuseTest extends LocalRepositoryTestCase {

	private TestRepository testRepository;

	@Before
	public void createRepositories() throws Exception {
		File repositoryFile = createProjectAndCommitToRepository();
		testRepository = new TestRepository<>(lookupRepository(repositoryFile));
		RepositoryUtil.INSTANCE.addConfiguredRepository(
				testRepository.getRepository().getDirectory());
	}

	@Test
	public void testEqualInput_editorReuse() throws Exception {
		IPath conflictPath = createRebaseConflict();

		GitMergeEditorInput input1 = new GitMergeEditorInput(
				MergeInputMode.STAGE_2, conflictPath);
		CompareUI.openCompareEditor(input1);
		IEditorPart activeEditor1 = getActiveEditor();

		GitMergeEditorInput input2 = new GitMergeEditorInput(
				MergeInputMode.STAGE_2, conflictPath);
		CompareUI.openCompareEditor(input2);
		IEditorPart activeEditor2 = getActiveEditor();

		// The input is equal, editor is reused
		assertSame(activeEditor1, activeEditor2);
	}

	@Test
	public void testDifferentMode_noEditorReuse() throws Exception {
		IPath conflictPath = createRebaseConflict();

		GitMergeEditorInput input1 = new GitMergeEditorInput(
				MergeInputMode.STAGE_2, conflictPath);
		CompareUI.openCompareEditor(input1);
		IEditorPart activeEditor1 = getActiveEditor();

		GitMergeEditorInput input2 = new GitMergeEditorInput(
				MergeInputMode.MERGED_OURS, conflictPath);
		CompareUI.openCompareEditor(input2);
		IEditorPart activeEditor2 = getActiveEditor();

		// Merge mode changed, editor can't be reused
		assertNotSame(activeEditor1, activeEditor2);
	}

	@Test
	public void testDifferentHead_noEditorReuse() throws Exception {
		IPath conflictPath = createRebaseConflict();

		GitMergeEditorInput input1 = new GitMergeEditorInput(
				MergeInputMode.STAGE_2, conflictPath);
		CompareUI.openCompareEditor(input1);
		IEditorPart activeEditor1 = getActiveEditor();

		// Continue until next conflict
		testRepository.git().add().addFilepattern(".").call();
		RebaseResult rebaseResult = testRepository.git().rebase()
				.setOperation(Operation.CONTINUE).call();
		assertThat(rebaseResult.getStatus(), is(RebaseResult.Status.STOPPED));

		GitMergeEditorInput input2 = new GitMergeEditorInput(
				MergeInputMode.STAGE_2, conflictPath);
		CompareUI.openCompareEditor(input2);
		IEditorPart activeEditor2 = getActiveEditor();

		// Head changed, editor can't be reused
		assertNotSame(activeEditor1, activeEditor2);
	}

	private static IEditorPart getActiveEditor() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getActiveEditor();
	}

	private IPath createRebaseConflict() throws Exception {
		IPath path = new Path(PROJ1).append(FOLDER).append(FILE1);
		testRepository.branch("main").commit().add(path.toString(), "main")
				.create();
		testRepository.branch("topic").commit().add(path.toString(), "topic")
				.create();
		touchAndSubmit("change 1", "change 1");
		touchAndSubmit("change 2", "change 2");
		RebaseResult rebaseResult = testRepository.git().rebase()
				.setUpstream("main").call();
		assertThat(rebaseResult.getStatus(), is(RebaseResult.Status.STOPPED));

		IPath repoWorkdirPath = new Path(
				testRepository.getRepository().getWorkTree().getPath());
		return repoWorkdirPath.append(path);
	}
}
