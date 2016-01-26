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
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;

/**
 * Interface for editor provider. Implement this interface to use extension point
 *
 */
public interface ICommitEditorProvider {

	/**
	 * Method should return instance of ICommitMessageEditor.
	 *
	 * The style value is either one of the style constants defined in class
	 * <code>SWT</code> which is applicable to instances of this class, or must
	 * be built by <em>bitwise OR</em>'ing together (that is, using the
	 * <code>int</code> "|" operator) two or more of those <code>SWT</code>
	 * style constant
	 *
	 *
	 * @param parent
	 *            on which editor components will be added
	 * @param initialText
	 *            of message/comment component if it exists
	 * @param styles
	 *            to apply on sub component. If more than on component. Than
	 *            applied to component containing all others as subcomponents
	 * @param commitProposalProcessor
	 * @return commit message dialog
	 * @see Widget#getStyle
	 */
	ICommitMessageEditor getEditor(Composite parent,
			@NonNull String initialText,
			int styles, CommitProposalProcessor commitProposalProcessor);

	/**
	 *
	 * Return is editor enabled
	 *
	 * @return true if editor is enabled, false otherwise
	 */
	boolean isEnabled();
}