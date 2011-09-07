package org.eclipse.egit.core.test;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffChangedListener;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

public class IndexDiffCacheTest {

	@Test
	public void testLinuxRepo() throws Exception {
		File gitDir = new File("C:\\git\\linux\\.git");
		Repository repository = Activator.getDefault().getRepositoryCache().lookupRepository(gitDir);
		assertNotNull(repository);
		IndexDiffCacheEntry cacheEntry = Activator.getDefault().getIndexDiffCache().getIndexDiffCacheEntry(repository);
		cacheEntry.addIndexDiffChangedListener(new IndexDiffChangedListener() {

			public void indexDiffChanged(Repository repo,
					IndexDiffData indexDiffData) {
				System.out.println("IndexDiff changed for repo " + repo.getDirectory().toString());
			}
		});
		while (1==1)
			Thread.sleep(10000);
	}

}
