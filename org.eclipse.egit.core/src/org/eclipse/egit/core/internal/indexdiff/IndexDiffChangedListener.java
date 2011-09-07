package org.eclipse.egit.core.internal.indexdiff;

import org.eclipse.jgit.lib.Repository;

/**
 * @author d020964
 *
 */
public interface IndexDiffChangedListener {

	/**
	 * @param repository
	 * @param indexDiffData
	 */
	void indexDiffChanged(Repository repository, IndexDiffData indexDiffData);
}
