package org.eclipse.egit.ui.internal.actions;

import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class PullWithOptionsAction extends RepositoryAction {

	protected PullWithOptionsAction() {
		super(ActionCommands.PULL_WITH_OPTIONS,
				new PullWithOptionsActionHandler());
	}

}
