/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.egit.ui.internal.repository.tree.FileNode;

/**
 * Implements "Open in Editor"
 */
public class OpenInEditorCommand extends
		RepositoriesViewCommandHandler<FileNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		FileNode node = getSelectedNodes(event).get(0);
		File file = node.getObject();
		EgitUiEditorUtils.openEditor(file, getView(event).getSite().getPage());
		return null;
	}
}
