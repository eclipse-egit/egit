/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.DeleteTagOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.jgit.lib.Repository;

/**
 * Command to delete a tag
 */
public class DeleteTagCommand extends RepositoriesViewCommandHandler<TagNode> {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final List<TagNode> tags = getSelectedNodes(event);
		Job job = new Job(UIText.DeleteTagCommand_taskName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(UIText.DeleteTagCommand_taskName, tags.size());
				for (TagNode tag : tags) {
					final Repository repo = tag.getRepository();
					final String tagName = tag.getObject().getName();
					final DeleteTagOperation op = new DeleteTagOperation(repo,
							tagName);
					monitor.subTask(tagName);
					try {
						op.execute(monitor);
					} catch (CoreException e) {
						Activator.logError(e.getLocalizedMessage(), e);
					}
					monitor.worked(1);
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		return null;
	}
}
