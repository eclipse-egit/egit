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
package org.eclipse.egit.ui.internal.dialogs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Display the result of a Branch operation.
 */
public class NonDeletedFilesDialog extends MessageDialog {
	private static final Image INFO = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);

	private static final int RETRY = 98;

	private final Repository repository;

	private final List<String> filePaths;

	private NonDeletedFilesTree tree;

	private Button retry;

	/**
	 * @param shell
	 * @param repository
	 * @param filePaths
	 */
	public NonDeletedFilesDialog(Shell shell, Repository repository,
			List<String> filePaths) {
		super(shell, UIText.NonDeletedFilesDialog_NonDeletedFilesTitle, INFO,
				UIText.NonDeletedFilesDialog_NonDeletedFilesMessage,
				MessageDialog.INFORMATION,
				new String[] { IDialogConstants.OK_LABEL }, 0);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.repository = repository;
		this.filePaths = filePaths;
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(0, 0).grab(true, true).applyTo(
				main);
		main.setLayout(new GridLayout(1, false));

		tree = new NonDeletedFilesTree(main, repository, filePaths);
		applyDialogFont(main);

		return main;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == RETRY) {
			boolean refresh = false;
			List<String> newPaths = new ArrayList<>();
			for (String filePath : filePaths) {
				File file = new File(repository.getWorkTree(), filePath);
				try {
					FileUtils.delete(file, FileUtils.SKIP_MISSING
							| FileUtils.RECURSIVE);
					refresh = true;
				} catch (IOException e) {
					newPaths.add(filePath);
				}
			}
			filePaths.clear();
			filePaths.addAll(newPaths);
			tree.setInput(filePaths);
			UIUtils.expandAll(tree);
			if (refresh) {
				try {
					ResourcesPlugin.getWorkspace().getRoot().refreshLocal(
							IResource.DEPTH_INFINITE, null);
				} catch (CoreException e) {
					Activator.handleError(e.getMessage(), e, false);
				}
			}
			retry.setEnabled(!filePaths.isEmpty());
			return;
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		retry = createButton(parent, RETRY,
				UIText.NonDeletedFilesDialog_RetryDeleteButton, false);
		super.createButtonsForButtonBar(parent);
	}
}
