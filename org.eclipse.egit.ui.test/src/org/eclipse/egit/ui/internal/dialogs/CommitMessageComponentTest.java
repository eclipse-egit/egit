package org.eclipse.egit.ui.internal.dialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.common.StagingViewTester;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.test.commitmessageprovider.CommitMessageProviderFactory;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CommitMessageComponentTest extends LocalRepositoryTestCase {

	private static final GitRepositoriesViewTestUtils repoViewUtil = new GitRepositoriesViewTestUtils();

	private File repositoryFile;

	private Repository repository;

	@Before
	public void before() throws Exception {
		CommitMessageProviderFactory.activate();

		// TODO this code is copied from StagingViewTest. Remove redundancy
		repositoryFile = createProjectAndCommitToRepository();
		repository = lookupRepository(repositoryFile);
		TestUtil.configureTestCommitterAsUser(repository);
		Activator.getDefault().getRepositoryUtil()
				.addConfiguredRepository(repositoryFile);

		selectRepositoryNode();
	}

	@After
	public void after() {
		CommitMessageProviderFactory.deactivate();

		// TODO this code is copied from StagingViewTest. Remove redundancy
		TestUtil.hideView(RepositoriesView.VIEW_ID);
		TestUtil.hideView(StagingView.VIEW_ID);
		Activator.getDefault().getRepositoryUtil().removeDir(repositoryFile);
	}

	@Test
	public void testCaretPosition() {
		try {
			StagingViewTester stagingView = StagingViewTester.openStagingView();
			assertEquals(
					CommitMessageProviderFactory.getProvidedCaretPosition(),
					stagingView.getCaretPosition());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	// TODO this code is copied from StagingViewTest. Remove redundancy
	private void selectRepositoryNode() throws Exception {
		SWTBotView repositoriesView = TestUtil
				.showView(RepositoriesView.VIEW_ID);
		SWTBotTree tree = repositoriesView.bot().tree();

		SWTBotTreeItem repoNode = repoViewUtil.getRootItem(tree,
				repositoryFile);
		repoNode.select();
	}

}
