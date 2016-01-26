/*******************************************************************************
 * Copyright (C) 2012, Robert Pofuk <rpofuk@gmail.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui;

import org.eclipse.egit.ui.internal.commit.CommitProposalProcessor;
import org.eclipse.swt.widgets.Composite;

/**
 * Interface for editor provider. Implement this interface to use extension point
 *
 */
public interface ICommitEditorProvider {

	/**
	 * @param parent
	 * @param initialText
	 * @param styles
	 * @param commitProposalProcessor
	 * @return commit message dialog
	 */
	ICommitMessageEditor getEditor(Composite parent, String initialText,
			int styles, CommitProposalProcessor commitProposalProcessor);

}