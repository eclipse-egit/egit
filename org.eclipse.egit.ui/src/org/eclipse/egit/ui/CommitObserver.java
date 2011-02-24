package org.eclipse.egit.ui;

/**
 * @author Adrian Staudt
 *
 */
public interface CommitObserver {

	/**
	 * @param commitOperation
	 * @return ture if the commit has been accepted, false otherwise
	 */
	boolean finaliceCommit(org.eclipse.egit.core.op.CommitOperation commitOperation);

	/**
	 * @return The reason why the commit has been refused.
	 */
	String getRefuseReason();
}
