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
import java.util.Locale;
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

	private class CommitFile implements Comparable<CommitFile> {

		private final String display;

		private final String full;

		public CommitFile(String display, String full) {
			this.display = display;
			this.full = full;
		}

		public boolean matches(String prefix) {
			return display.toLowerCase(Locale.US).startsWith(prefix);
		}

		public int compareTo(CommitFile other) {
			return display.compareTo(other.display);
		}

		public Image getImage() {
			return (Image) resourceManager.get(UIUtils.getEditorImage(full));
		}

		public ICompletionProposal createProposal(int offset, int length) {
			return new CompletionProposal(display, offset, length,
					display.length(), getImage(), display, null, null);
		}
	}

	private Set<CommitFile> files = new TreeSet<CommitFile>();

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
			files.add(new CommitFile(name, name));
			int lastDot = name.lastIndexOf('.');
			if (lastDot > 0)
				files.add(new CommitFile(name.substring(0, lastDot), name));
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

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int offset) {
		String prefix;
		try {
			prefix = getPrefix(viewer, offset);
		} catch (BadLocationException e) {
			return NO_PROPOSALS;
		}

		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		if (prefix != null && prefix.length() > 0) {
			int replacementLength = prefix.length();
			int replacementOffset = offset - replacementLength;
			prefix = prefix.toLowerCase(Locale.US);
			for (CommitFile file : files)
				if (file.matches(prefix))
					proposals.add(file.createProposal(replacementOffset,
							replacementLength));
		} else
			for (CommitFile file : files)
				proposals.add(file.createProposal(offset, 0));
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
