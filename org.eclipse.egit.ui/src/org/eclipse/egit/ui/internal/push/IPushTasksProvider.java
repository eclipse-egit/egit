package org.eclipse.egit.ui.internal.push;

import org.eclipse.egit.ui.internal.commit.RepositoryCommit;

/**
 * This interface must be implemented to be a push action provider.
 */
public interface IPushTasksProvider {

	/**
	 * Allows to perform tasks after a successful push
	 *
	 * @param commits array of commits that was pushed
	 */
	public void performTasksAfterPush(RepositoryCommit[] commits);

}
