/*******************************************************************************
 * Copyright (C) 2022 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A simple default previewer for commit messages.
 */
public class CommitMessagePreviewer {

	private SpellcheckableMessageArea viewer;

	/**
	 * Creates the {@link Control} necessary for the preview.
	 *
	 * @param parent
	 *            a {@link Composite} provided to contain the previewer
	 * @return the top control of the previewer
	 */
	public Control createControl(Composite parent) {
		viewer = new SpellcheckableMessageArea(parent, "", true, SWT.NONE) { //$NON-NLS-1$

			@Override
			protected int getViewerStyles() {
				return SWT.V_SCROLL | SWT.H_SCROLL;
			}

			@Override
			protected void configureHardWrap() {
				// Disabled
			}

			@Override
			protected void createMarginPainter() {
				// Disabled
			}
		};
		GridDataFactory.fillDefaults().grab(true, true).applyTo(viewer);
		return viewer;
	}

	/**
	 * Sets the text the previewer shall display.
	 *
	 * @param repository
	 *            {@link Repository} into which the commit would be made
	 * @param text
	 *            to show
	 */
	public void setText(Repository repository, String text) {
		viewer.setText(text);
	}
}
