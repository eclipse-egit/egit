/******************************************************************************
 *  Copyright (c) 2011, 2013 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.test.commit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.test.ContextMenuHelper;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotMultiPageEditor;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of {@link CommitEditor}
 */
public class CommitEditorTest extends LocalRepositoryTestCase {

	private static final TestUtil testUtil = new TestUtil();

	private Repository repository;

	private RevCommit commit;

	@Before
	public void setup() throws Exception {
		File repoFile = createProjectAndCommitToRepository();
		assertNotNull(repoFile);
		repository = RepositoryCache.INSTANCE.lookupRepository(repoFile);
		assertNotNull(repository);

		try (RevWalk walk = new RevWalk(repository)) {
			commit = walk.parseCommit(repository.resolve(Constants.HEAD));
			assertNotNull(commit);
			walk.parseBody(commit.getParent(0));
		}
	}

	@Test
	public void openAllEditorPagesOnValidCommit() throws Exception {
		final AtomicReference<IEditorPart> editorRef = new AtomicReference<>();
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				RepositoryCommit repoCommit = new RepositoryCommit(repository,
						commit);
				editorRef.set(CommitEditor.openQuiet(repoCommit));
			}
		});
		assertNotNull(editorRef.get());
		IEditorPart editor = editorRef.get();
		assertTrue(editor instanceof CommitEditor);
		RepositoryCommit adaptedCommit = Adapters.adapt(editor,
				RepositoryCommit.class);
		assertNotNull(adaptedCommit);
		assertEquals(commit, adaptedCommit.getRevCommit());
		assertEquals(repository.getDirectory(), adaptedCommit.getRepository()
				.getDirectory());
		IEditorInput input = editor.getEditorInput();
		assertNotNull(input);
		SWTBotMultiPageEditor botEditor = bot.multipageEditorByTitle(input
				.getName());
		assertNotNull(botEditor);
		assertTrue(botEditor.getPageCount() > 1);
		for (String name : botEditor.getPagesTitles())
			botEditor.activatePage(name);
		botEditor.close();
	}

	@Test
	public void showAnnotations() throws Exception {
		final AtomicReference<IEditorPart> editorRef = new AtomicReference<>();
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				RepositoryCommit repoCommit = new RepositoryCommit(repository,
						commit);
				editorRef.set(CommitEditor.openQuiet(repoCommit));
			}
		});
		assertNotNull(editorRef.get());
		SWTBotEditor commitEditor = bot.activeEditor();
		SWTBotTable table = commitEditor.bot().table(1);
		assertTrue(table.rowCount() > 0);
		table.select(0);
		ContextMenuHelper.clickContextMenuSync(table,
				testUtil.getPluginLocalizedValue("FileDiffBlame.label"));
		TestUtil.joinJobs(JobFamilies.BLAME);
		assertFalse(commitEditor.getReference().equals(
				bot.activeEditor().getReference()));

		final String content = getTestFileContent();
		// Change working directory content to validate blame opens HEAD
		setTestFileContent("updated content" + System.nanoTime());
		assertEquals(content, bot.activeEditor().toTextEditor().getText());
	}
}
