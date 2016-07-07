package org.eclipse.egit.internal.mylyn.ui.commit;

import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskActivationAdapter;
import org.eclipse.ui.PlatformUI;

/**
 * A listener that updates commit text in staging view when a task is activated
 */
public class CommitTextTaskActivationListener extends TaskActivationAdapter {

	@Override
	public void taskActivated(ITask task) {
		StagingView view = (StagingView) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.findView(StagingView.VIEW_ID);
		if (view != null) {
			view.resetCommitMessageComponent();
		}
	}

}
