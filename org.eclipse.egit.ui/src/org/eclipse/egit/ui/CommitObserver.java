package org.eclipse.egit.ui;

import org.eclipse.egit.core.op.CommitOperation;

/**
 * Interface for hooking into the commit process. This interface is
 * associated with the extension point "org.eclipse.egit.ui.commitObserver".
 * A commit observer can still change the resources that have to be committed,
 * after the user has confirmed the commit dialog.
 *
 * @author Adrian Staudt
 *
 */
public interface CommitObserver {

	/**
	 * This method is called after the user has confirmed which resources
	 * should be committed. The resources that should be committed can be requested
	 * from the object given by the parameter commitOperation.
	 * If the state of the resources that have to be committed do not fit to your
	 * application need, the commit process can be canceled by returning false.
	 *
	 * @param commitOperation The operation that hold the resources that will be committed.
	 * @return true if the commit has been accepted, false otherwise
	 */
	boolean finalizeCommit(CommitOperation commitOperation);

	/**
	 * As the commit can be canceled, it is expected that each CommitObserver provides
	 * a reason why the commit has been refused. This reason will be shown to the user.
	 * If the method finalizeCommit does return true, this method will not be evaluated.
	 *
	 * @return The reason why the commit has been refused.
	 */
	String getRefuseReason();
}
