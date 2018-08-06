/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
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
public abstract class CommitProposalProcessor implements IContentAssistProcessor {

	/**
	 * Replace all non single space whitespace characters with a single space
	 *
	 * @param value
	 * @return replaced string
	 */
	private static final String escapeWhitespace(String value) {
		final StringBuilder escaped = new StringBuilder(value);
		final int length = escaped.length();
		for (int i = 0; i < length; i++) {
			char c = escaped.charAt(i);
			if (c != ' ' && Character.isWhitespace(c))
				escaped.setCharAt(i, ' ');
		}
		return escaped.toString();
	}

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

		@Override
		public int compareTo(CommitFile other) {
			return display.compareTo(other.display);
		}

		@Override
		public int hashCode() {
			return display.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof CommitFile))
				return false;
			return (this.compareTo((CommitFile) other) == 0);
		}

		public Image getImage() {
			return (Image) resourceManager.get(UIUtils.getEditorImage(full));
		}

		public ICompletionProposal createProposal(int offset, int length) {
			return new CompletionProposal(display, offset, length,
					display.length(), getImage(), display, null, null);
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

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int offset) {
		String prefix;
		try {
			prefix = getPrefix(viewer, offset);
		} catch (BadLocationException e) {
			return NO_PROPOSALS;
		}

		Collection<String> messages = computeMessageProposals();
		Set<CommitFile> files = computeFileProposals();

		List<ICompletionProposal> proposals = new ArrayList<>();
		if (prefix != null && prefix.length() > 0) {
			int replacementLength = prefix.length();
			int replacementOffset = offset - replacementLength;
			prefix = prefix.toLowerCase(Locale.US);
			for (CommitFile file : files)
				if (file.matches(prefix))
					proposals.add(file.createProposal(replacementOffset,
							replacementLength));
			for (String message : messages)
				if (message.startsWith(prefix))
					proposals.add(new CompletionProposal(message,
							replacementOffset, replacementLength, message
									.length(), (Image) resourceManager
									.get(UIIcons.ELCL16_COMMENTS),
							escapeWhitespace(message), null, null));
		} else {
			for (String message : messages)
				proposals.add(new CompletionProposal(message, offset, 0,
						message.length(), (Image) resourceManager
								.get(UIIcons.ELCL16_COMMENTS),
						escapeWhitespace(message), null, null));
			for (CommitFile file : files)
				proposals.add(file.createProposal(offset, 0));
		}
		return proposals.toArray(new ICompletionProposal[0]);
	}

	/**
	 * @return the file names which will be made available through content assist
	 */
	protected abstract Collection<String> computeFileNameProposals();

	/**
	 * @return the commit messages which will be made available through content assist
	 */
	protected abstract Collection<String> computeMessageProposals();

	private Set<CommitFile> computeFileProposals() {
		Collection<String> paths = computeFileNameProposals();
		Set<CommitFile> files = new TreeSet<>();
		for (String path : paths) {
			String name = new Path(path).lastSegment();
			if (name == null)
				continue;
			files.add(new CommitFile(name, name));
			int lastDot = name.lastIndexOf('.');
			if (lastDot > 0)
				files.add(new CommitFile(name.substring(0, lastDot), name));
		}
		return files;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}
}
