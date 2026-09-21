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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.MessageFormat;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.repository.tree.FolderNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

/**
 * Base class for creating a new file or folder below the selected working
 * tree or folder node in the repositories view.
 */
abstract class NewPathCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode<?>> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode<?> node = getFirstOrNull(getSelectedNodes(event));
		Path parent = getDirectory(node);
		if (parent == null) {
			return null;
		}
		InputDialog dialog = new InputDialog(getShell(event), getTitle(),
				getMessage(), null, name -> validate(parent, name));
		if (dialog.open() != Window.OK) {
			return null;
		}
		Path target = parent.resolve(dialog.getValue().trim()).normalize();
		try {
			create(target);
		} catch (IOException e) {
			Activator.handleError(MessageFormat.format(
					UIText.NewPathCommand_CreateFailed, target), e, true);
			return null;
		}
		refreshWorkspace(parent, target);
		RepositoriesView view = getView(event);
		view.refreshAndShow(IPath.fromFile(target.toFile()));
		afterCreate(target, view);
		return null;
	}

	@Override
	public boolean isEnabled() {
		return getSelectedNodes().size() == 1 && isWorkingDirSelection();
	}

	abstract String getTitle();

	abstract String getMessage();

	abstract void create(Path target) throws IOException;

	void afterCreate(Path target, RepositoriesView view) {
		// nothing by default
	}

	private static Path getDirectory(RepositoryTreeNode<?> node) {
		if (node instanceof FolderNode) {
			return ((FolderNode) node).getObject().toPath().toAbsolutePath();
		}
		Repository repository = node == null ? null : node.getRepository();
		if (repository == null || repository.isBare()) {
			return null;
		}
		return repository.getWorkTree().toPath().toAbsolutePath();
	}

	private static String validate(Path parent, String name) {
		String trimmed = name.trim();
		if (trimmed.isEmpty()) {
			return UIText.NewPathCommand_EmptyName;
		}
		Path target;
		try {
			target = parent.resolve(trimmed).normalize();
		} catch (InvalidPathException e) {
			return MessageFormat.format(UIText.NewPathCommand_InvalidName,
					trimmed);
		}
		if (!target.startsWith(parent) || target.equals(parent)) {
			return MessageFormat.format(UIText.NewPathCommand_InvalidName,
					trimmed);
		}
		for (Path p = target; !p.equals(parent); p = p.getParent()) {
			if (Constants.DOT_GIT.equals(p.getFileName().toString())) {
				return MessageFormat.format(UIText.NewPathCommand_InvalidName,
						trimmed);
			}
		}
		if (Files.exists(target)) {
			return MessageFormat.format(UIText.NewPathCommand_AlreadyExists,
					trimmed);
		}
		return null;
	}

	// Refresh the topmost created path so enclosing projects see it
	private static void refreshWorkspace(Path parent, Path target) {
		IContainer container = ResourceUtil.getContainerForLocation(
				IPath.fromFile(parent.toFile()), true);
		if (container == null) {
			return;
		}
		Path topmost = parent.relativize(target).getName(0);
		IPath child = IPath.fromOSString(topmost.toString());
		IResource resource = Files.isDirectory(parent.resolve(topmost))
				? container.getFolder(child)
				: container.getFile(child);
		try {
			resource.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			Activator.logError(e.getMessage(), e);
		}
	}
}
