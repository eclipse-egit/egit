package org.eclipse.egit.core.op;

import org.eclipse.jgit.merge.MergeStrategy;

/**
 * Operation that involves a merge, and that can consequently receive a specific
 * merge strategy to override the preferred merge strategy.
 *
 * @since 4.1
 */
public interface MergingOperation extends IEGitOperation {

	/**
	 * @param strategy
	 */
	void setMergeStrategy(MergeStrategy strategy);
}
