/*******************************************************************************
 *  Copyright (c) 2011, 2013 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    François Rey - gracefully ignore linked resources
 *    Laurent Goubet <laurent.goubet@obeo.fr>
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CommitSelectDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Compare with previous revision action handler.
 */
public class CompareWithPreviousActionHandler extends RepositoryActionHandler {

	/**
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(true, event);
		if (repository == null) {
			return null;
		}

		final IResource[] resources = getSelectedResources(event);
		if (resources.length != 1) {
			return null;
		}
		try {
			IWorkbenchPage workBenchPage = HandlerUtil
					.getActiveWorkbenchWindowChecked(event).getActivePage();
			final PreviousCommit previous = getPreviousRevision(event,
					resources[0]);
			if (previous != null) {
				CompareUtils.compare(resources, repository, Constants.HEAD,
						previous.commit.getName(), true, workBenchPage);
			}
		} catch (Exception e) {
			Activator.handleError(
					UIText.CompareWithRefAction_errorOnSynchronize, e, true);
		}

		return null;
	}

	@Override
	public boolean isEnabled() {
		IResource[] selectedResources = getSelectedResources();
		return super.isEnabled() && selectedResources.length == 1 &&
				selectionMapsToSingleRepository();
	}

	private PreviousCommit getPreviousRevision(final ExecutionEvent event,
			final IResource resource) throws IOException {

		final List<PreviousCommit> previousList = findPreviousCommits();

		final AtomicReference<PreviousCommit> previous = new AtomicReference<>();
		if (previousList.size() == 0)
			showNotFoundDialog(event, resource);
		else if (previousList.size() == 1)
			previous.set(previousList.get(0));
		else {
			final List<RevCommit> commits = new ArrayList<>();
			for (PreviousCommit pc : previousList)
				commits.add(pc.commit);
			HandlerUtil.getActiveShell(event).getDisplay()
					.syncExec(new Runnable() {
						@Override
						public void run() {
							CommitSelectDialog dlg = new CommitSelectDialog(
									HandlerUtil.getActiveShell(event), commits);
							if (dlg.open() == Window.OK)
								for (PreviousCommit pc : previousList)
									if (pc.commit.equals(dlg
											.getSelectedCommit())) {
										previous.set(pc);
										break;
									}
						}
					});
		}

		return previous.get();
	}

	private void showNotFoundDialog(final ExecutionEvent event,
			IResource resource) {
		final String message = MessageFormat
				.format(UIText.CompareWithPreviousActionHandler_MessageRevisionNotFound,
						resource.getName());
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				MessageDialog
						.openWarning(
						HandlerUtil.getActiveShell(event),
								UIText.CompareWithPreviousActionHandler_TitleRevisionNotFound,
								message);
			}
		});
	}
}
