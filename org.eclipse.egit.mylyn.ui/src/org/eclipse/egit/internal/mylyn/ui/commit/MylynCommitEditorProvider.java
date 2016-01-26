/*******************************************************************************
 * Copyright (C) 2012, Robert Pofuk <rpofuk@gmail.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.commit;

import org.eclipse.egit.ui.ICommitEditorProvider;
import org.eclipse.egit.ui.ICommitMessageEditor;
import org.eclipse.egit.ui.internal.commit.CommitProposalProcessor;
import org.eclipse.swt.widgets.Composite;

/**
 * Provider for Mylyn commit editor  
 *
 */
public class MylynCommitEditorProvider implements ICommitEditorProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.egit.ui.ICommitEditorProvider#getEditor(org.eclipse.swt.widgets.Composite, java.lang.String, int, org.eclipse.egit.ui.internal.commit.CommitProposalProcessor)
	 */
	@Override
	public ICommitMessageEditor getEditor(Composite parent, String initialText,
			int styles, final CommitProposalProcessor commitProposalProcessor) {
		return new MylynTaskCommitMessageEditor(parent, styles);
	}
}
