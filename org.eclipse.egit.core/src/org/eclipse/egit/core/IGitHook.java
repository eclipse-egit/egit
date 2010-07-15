package org.eclipse.egit.core;

import org.eclipse.core.resources.IProject;

/**
 * @author alex
 *
 */
public interface IGitHook {

	/**
	 * @param project
	 */
	public void postCommit(IProject project);

}
