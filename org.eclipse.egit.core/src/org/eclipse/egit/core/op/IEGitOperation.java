package org.eclipse.egit.core.op;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * interface for EGit operations
 *
 */
public interface IEGitOperation {
	/**
	 * Executes the operation
	 * @param monitor
	 * @throws CoreException
	 */
	void execute(IProgressMonitor monitor) throws CoreException;
}
