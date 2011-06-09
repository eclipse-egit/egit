package org.eclipse.egit.ui.test.stagview;

import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTestUtils;
import org.junit.BeforeClass;

public class StagingViewTest extends
LocalRepositoryTestCase {

	private static final GitRepositoriesViewTestUtils repoViewUtil = new GitRepositoriesViewTestUtils();

	@BeforeClass
	public static void setup() {
		repoViewUtil.openRepositoriesView(bot)
	}
	
}
