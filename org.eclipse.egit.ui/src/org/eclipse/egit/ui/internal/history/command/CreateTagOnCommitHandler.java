/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.internal.op.TagOperation;
import org.eclipse.egit.ui.internal.dialogs.CreateTagDialog;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Create a tag based on a commit.
 */
public class CreateTagOnCommitHandler extends AbstractHistoryCommandHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		PlotCommit commit = (PlotCommit) getSelection(getPage()).getFirstElement();
		final Repository repo = getRepository(event);

		CreateTagDialog dialog = new CreateTagDialog(HandlerUtil
				.getActiveShellChecked(event), commit.getId(), repo);

		if (dialog.open() != Window.OK)
			return null;

		final TagBuilder tag = new TagBuilder();
		PersonIdent personIdent = new PersonIdent(repo);
		String tagName = dialog.getTagName();

		tag.setTag(tagName);
		tag.setTagger(personIdent);
		tag.setMessage(dialog.getTagMessage());
		tag.setObjectId(commit);

		try {
			new TagOperation(repo, tag, dialog.shouldOverWriteTag())
					.execute(new NullProgressMonitor());
		} catch (CoreException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		GitHistoryPage page = getPage();
		if (page == null)
			return false;
		IStructuredSelection sel = getSelection(page);
		return sel.size() == 1 && sel.getFirstElement() instanceof RevCommit;
	}
}
