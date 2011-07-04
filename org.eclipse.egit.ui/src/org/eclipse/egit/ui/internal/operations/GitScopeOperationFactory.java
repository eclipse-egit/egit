package org.eclipse.egit.ui.internal.operations;

import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.ui.IWorkbenchPart;

/**
 * @author bmuskalla
 *
 */
public class GitScopeOperationFactory {

	private static GitScopeOperationFactory instance;

	/**
	 * @param part
	 * @param manager
	 * @return a
	 */
	public GitScopeOperation createGitScopeOperation(
			IWorkbenchPart part, SubscriberScopeManager manager) {
		GitScopeOperation buildScopeOperation = new GitScopeOperation(part,
				manager);
		return buildScopeOperation;
	}

	/**
	 * @return the current factory
	 */
	public static synchronized GitScopeOperationFactory getFactory() {
		if(instance == null) {
			instance = new GitScopeOperationFactory();
		}
		return instance;
	}

	/**
	 * @param newInstance
	 */
	public static synchronized void setFactory(GitScopeOperationFactory newInstance) {
		instance = newInstance;
	}
}
