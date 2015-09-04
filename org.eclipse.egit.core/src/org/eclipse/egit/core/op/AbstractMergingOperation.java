package org.eclipse.egit.core.op;

import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.merge.MergeStrategy;

/**
 * @since 4.1
 */
public abstract class AbstractMergingOperation implements MergingOperation {
	private MergeStrategy mergeStrategy;

	/**
	 * @return The merge strategy to use in this operation.
	 */
	protected MergeStrategy getApplicableMergeStrategy() {
		if (mergeStrategy == null) {
			return Activator.getDefault().getPreferredMergeStrategy();
		}
		return mergeStrategy;
	}

	@Override
	public void setMergeStrategy(MergeStrategy strategy) {
		this.mergeStrategy = strategy;
	}

}
