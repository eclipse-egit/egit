/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.test.commit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotMultiPageEditor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests of {@link CommitEditor}
 */
public class CommitEditorTest extends LocalRepositoryTestCase {

	private static Repository repository;

	private static RevCommit commit;

	@BeforeClass
	public static void setup() throws Exception {
		closeWelcomePage();
		File repoFile = createProjectAndCommitToRepository();
		assertNotNull(repoFile);
		repository = Activator.getDefault().getRepositoryCache()
				.lookupRepository(repoFile);
		assertNotNull(repository);

		RevWalk walk = new RevWalk(repository);
		try {
			commit = walk.parseCommit(repository.resolve(Constants.HEAD));
			assertNotNull(commit);
			walk.parseBody(commit.getParent(0));
		} finally {
			walk.release();
		}
	}

	@Test
	public void openAllEditorPagesOnValidCommit() throws Exception {
		final AtomicReference<IEditorPart> editorRef = new AtomicReference<IEditorPart>();
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			public void run() {
				RepositoryCommit repoCommit = new RepositoryCommit(repository,
						commit);
				editorRef.set(CommitEditor.openQuiet(repoCommit));
			}
		});
		assertNotNull(editorRef.get());
		IEditorPart editor = editorRef.get();
		assertTrue(editor instanceof CommitEditor);
		RepositoryCommit adaptedCommit = (RepositoryCommit) editor
				.getAdapter(RepositoryCommit.class);
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
}
