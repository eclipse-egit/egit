/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;

/**
 * "Adds" repositories
 *
 */
public class PasteCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {

	public Object execute(ExecutionEvent event) throws ExecutionException {

		// we check if the pasted content is a directory
		// repository location and try to add this
		String errorMessage = null;

		Clipboard clip = null;
		try {
			clip = new Clipboard(getView(event).getSite().getShell()
					.getDisplay());
			String content = (String) clip.getContents(TextTransfer
					.getInstance());
			if (content == null) {
				errorMessage = UIText.RepositoriesView_NothingToPasteMessage;
				return null;
			}

			File file = new File(content);
			if (!file.exists() || !file.isDirectory()) {
				errorMessage = UIText.RepositoriesView_ClipboardContentNotDirectoryMessage;
				return null;
			}

			if (!RepositoryCache.FileKey.isGitRepository(file)) {
				errorMessage = NLS
						.bind(
								UIText.RepositoriesView_ClipboardContentNoGitRepoMessage,
								content);
				return null;
			}

			if (util.addConfiguredRepository(file)) {
				// let's do the auto-refresh the rest
			} else
				errorMessage = NLS.bind(
						UIText.RepositoriesView_PasteRepoAlreadyThere, content);

			return null;
		} finally {
			if (clip != null)
				// we must dispose ourselves
				clip.dispose();
			if (errorMessage != null)
				Activator.handleError(errorMessage, null, true);
		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}

}
