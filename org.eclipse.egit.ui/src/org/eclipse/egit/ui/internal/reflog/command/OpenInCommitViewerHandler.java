package org.eclipse.egit.ui.internal.reflog.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.PartInitException;

/**
 * Handler to open commit in commit viewer
 */
public class OpenInCommitViewerHandler extends AbstractReflogCommandHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(event);
		RevCommit commit = getSelectedCommit(event, repository);
		if (commit != null) {
			try {
				CommitEditor.open(new RepositoryCommit(repository, commit));
			} catch (PartInitException e) {
				Activator.showError("Error opening commit viewer", e); //$NON-NLS-1$
			}
		}
		return null;
	}

}
