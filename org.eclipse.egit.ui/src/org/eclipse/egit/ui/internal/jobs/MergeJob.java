package org.eclipse.egit.ui.internal.jobs;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.op.MergingOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.dialogs.PreferredStrategyDialog;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.swt.widgets.Display;

/**
 * This is a standard WorkspaceJob intended to be used for all operations that
 * involve merges. It checks for the presence of specific MergeStrategies
 * registered through the mergeStrategy extension point and displays a dialog
 * (according to preference
 * {@link UIPreferences#PREFERRED_MERGE_STRATEGY_HIDE_DIALOG}) to ask users the
 * MergeStrategy they want to use.
 *
 * @since 4.1
 */
public class MergeJob extends WorkspaceJob {

	private final MergingOperation operation;

	/**
	 * @param name
	 *            The name of the job
	 * @param op
	 *            The operation to perform in the job
	 */
	public MergeJob(String name, @NonNull MergingOperation op) {
		super(name);
		this.operation = op;
	}

	/**
	 * Checks whether it's necessary to ask the user for their preferred merge
	 * strategy and then perform the operation. This cannot be overridden,
	 * override doRunInWorkspace instead.
	 *
	 * @param monitor
	 */
	@Override
	public final IStatus runInWorkspace(IProgressMonitor monitor)
			throws CoreException {
		try {
			operation.setMergeStrategy(obtainMergeStrategy());
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		}
		return doRunInWorkspace(monitor);
	}

	/**
	 * This displays the merge strategy seletion dialog according to the
	 * associated preference.
	 *
	 * @return The selected merge strategy, or null if the default implemented
	 *         by JGit for the operation must be used.
	 * @throws OperationCanceledException
	 */
	protected MergeStrategy obtainMergeStrategy()
			throws OperationCanceledException {
		IEclipsePreferences uiPrefs = InstanceScope.INSTANCE.getNode(Activator
				.getPluginId());
		if (!uiPrefs.getBoolean(
				UIPreferences.PREFERRED_MERGE_STRATEGY_HIDE_DIALOG, false)) {
			SelectMergeStrategy select = new SelectMergeStrategy();
			select.displaySelectionDialog();
			return select.getStrategy();
		}
		return org.eclipse.egit.core.Activator.getDefault()
				.getPreferredMergeStrategy();
	}

	/**
	 * The default behavior implemented here is to run the job's operation and
	 * to return OK_STATUS unless a CoreException occurs, in which case its
	 * status is returned. This can be overridden if required.
	 *
	 * @param monitor
	 * @return OK_STATUS unless the job's operation throws a CoreException, in
	 *         which case its status is returned.
	 */
	protected IStatus doRunInWorkspace(IProgressMonitor monitor) {
		try {
			operation.execute(monitor);
		} catch (final CoreException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	private static class SelectMergeStrategy {

		private boolean canceled;

		private MergeStrategy strategy;

		private MergeStrategy getStrategy() {
			return strategy;
		}

		/**
		 * Display the dialog for the preferred strategy.
		 *
		 * @throws OperationCanceledException
		 */
		protected void displaySelectionDialog()
				throws OperationCanceledException {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					PreferredStrategyDialog dialog = new PreferredStrategyDialog(
							Display.getDefault().getActiveShell());
					if (dialog.open() == Window.CANCEL) {
						cancelOperation();
					} else {
						setStrategy(dialog.getSelectedStrategy());
					}
				}
			});
			if (canceled) {
				throw new OperationCanceledException();
			}
		}

		private void cancelOperation() {
			this.canceled = true;
		}

		private void setStrategy(MergeStrategy strategy) {
			this.strategy = strategy;
		}
	}

}
