/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.internal.commit.CommitProposalProcessor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;

/**
 * Commit message area shared between the commit dialog and the staging view.
 */
public abstract class CommitMessageArea extends SpellcheckableMessageArea {

	/**
	 * @see SpellcheckableMessageArea#SpellcheckableMessageArea(Composite,
	 *      String, int)
	 * @param parent
	 * @param initialText
	 * @param styles
	 */
	public CommitMessageArea(Composite parent, String initialText, int styles) {
		super(parent, initialText, styles);
	}

	@Override
	protected IContentAssistant createContentAssistant(ISourceViewer viewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant.enableAutoInsert(true);
		final CommitProposalProcessor processor = getCommitProposalProcessor();
		getTextWidget().addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				processor.dispose();
			}
		});
		assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		return assistant;
	}

	/**
	 * @return the commit proposal processor that should be used for content assist
	 */
	protected abstract CommitProposalProcessor getCommitProposalProcessor();
}
