package org.eclipse.egit.core;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * A {@link RevWalk} that will create {@link UtilCommit} objects, instead of {@link RevCommit} objects.
 */
public class UtilWalk extends RevWalk {
	/**
	 * Default constructor
	 * @param repo
	 *            the repository the walker will obtain data from. An
	 *            ObjectReader will be created by the walker, and must be
	 *            released by the caller.
	 */
	public UtilWalk(Repository repo) {
		super(repo);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected RevCommit createCommit(AnyObjectId id) {
		return new UtilCommit(id);
	}
}
