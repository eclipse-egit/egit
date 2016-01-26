/*******************************************************************************
 * Copyright (C) 2012, Robert Pofuk <rpofuk@gmail.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.ICommitEditorProvider;
import org.eclipse.egit.ui.ICommitMessageEditor;
import org.eclipse.egit.ui.internal.commit.CommitProposalProcessor;
import org.eclipse.swt.widgets.Composite;

/**
 * Default commit editor provider
 *
 */
public class DefaultCommitEditorProvider implements ICommitEditorProvider {

	@Override
	public ICommitMessageEditor getEditor(Composite parent, String initialText,
			int styles, final CommitProposalProcessor commitProposalProcessor) {
		return new CommitMessageArea(parent, initialText, styles) {

			@Override
			protected CommitProposalProcessor getCommitProposalProcessor() {
				return commitProposalProcessor;
			}

		};
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
