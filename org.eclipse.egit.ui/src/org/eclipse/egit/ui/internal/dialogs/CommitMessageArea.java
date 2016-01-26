/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.ICommitMessageEditor;
import org.eclipse.egit.ui.internal.commit.CommitProposalProcessor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Commit message area shared between the commit dialog and the staging view.
 */
public abstract class CommitMessageArea extends SpellcheckableMessageArea
		implements ICommitMessageEditor {

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
	 * @return the commit proposal processor that should be used for content
	 *         assist
	 */
	protected abstract CommitProposalProcessor getCommitProposalProcessor();

	/**
	 * @param iCommitMessageChangeListener
	 */
	@Override
	public void addValueChangeListener(
			final CommitMessageChangeListener iCommitMessageChangeListener) {

		this.getDocument().addDocumentListener(new IDocumentListener() {
			@Override
			public void documentChanged(DocumentEvent event) {
				iCommitMessageChangeListener.commitMessageChanged();
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				// nothing to do
			}
		});
	}

	@Override
	public int getMinHeight() {
		return getTextWidget().getLineHeight() * 3;
	}

	@Override
	public void addVerifyKeyListener(VerifyKeyListener listener) {
		getTextWidget().addVerifyKeyListener(listener);
	}

	@Override
	public Point getSize() {
		return super.getSize();
	}

	@Override
	public Control getCommentWidget() {
		return getTextWidget();
	}

	@Override
	public Control getEditorWidget() {
		return this;
	}

}
