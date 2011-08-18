/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;

/**
 * Commit proposal processor
 */
public class CommitProposalProcessor implements IContentAssistProcessor {

	private static final ICompletionProposal[] NO_PROPOSALS = new ICompletionProposal[0];

	private final LocalResourceManager resourceManager = new LocalResourceManager(
			JFaceResources.getResources());

	private Set<String> names = new TreeSet<String>();

	/**
	 * Create process with path proposals
	 *
	 * @param paths
	 */
	public CommitProposalProcessor(String[] paths) {
		for (String path : paths) {
			String name = new Path(path).lastSegment();
			if (name == null)
				continue;
			names.add(name);
			int lastDot = name.lastIndexOf('.');
			if (lastDot > 0)
				names.add(name.substring(0, lastDot));
		}
	}

	/**
	 * Dispose of processor
	 */
	public void dispose() {
		resourceManager.dispose();
	}

	private String getPrefix(ITextViewer viewer, int offset)
			throws BadLocationException {
		IDocument doc = viewer.getDocument();
		if (doc == null || offset > doc.getLength())
			return null;

		int length = 0;
		int start = offset - 1;
		while (start >= 0 && !Character.isWhitespace(doc.getChar(start))) {
			start--;
			length++;
		}
		return doc.get(start + 1, length);
	}

	private Image getImage(String name) {
		return (Image) resourceManager.get(UIUtils.getEditorImage(name));
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int offset) {
		String prefix;
		try {
			prefix = getPrefix(viewer, offset);
		} catch (BadLocationException e) {
			return NO_PROPOSALS;
		}

		int replacementLength;
		int replacementOffset;
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		if (prefix != null && prefix.length() > 0) {
			replacementLength = prefix.length();
			replacementOffset = offset - prefix.length();
			for (String name : names)
				if (name.startsWith(prefix))
					proposals.add(new CompletionProposal(name,
							replacementOffset, replacementLength,
							name.length(), getImage(name), name, null, null));
		} else {
			replacementLength = 0;
			replacementOffset = offset;
			for (String name : names)
				proposals.add(new CompletionProposal(name, replacementOffset,
						replacementLength, name.length(), getImage(name), name,
						null, null));
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
		return null;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	public String getErrorMessage() {
		return null;
	}

	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}
}
