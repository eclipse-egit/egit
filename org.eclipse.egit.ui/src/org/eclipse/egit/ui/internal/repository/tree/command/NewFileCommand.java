/*******************************************************************************
 * Copyright (C) 2026, Lars Vogel <Lars.Vogel@vogella.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;

/**
 * Creates a new file in the working tree and opens it in an editor.
 */
public class NewFileCommand extends NewPathCommand {

	@Override
	String getTitle() {
		return UIText.NewPathCommand_NewFileTitle;
	}

	@Override
	String getMessage() {
		return UIText.NewPathCommand_NewFileMessage;
	}

	@Override
	void create(Path target) throws IOException {
		Files.createDirectories(target.getParent());
		Files.createFile(target);
	}

	@Override
	void afterCreate(Path target, RepositoriesView view) {
		EgitUiEditorUtils.openEditor(target.toFile(),
				view.getSite().getPage());
	}
}
