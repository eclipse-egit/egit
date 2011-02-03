/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPageSite;

class CommitMessageViewer extends TextViewer implements
		ISelectionChangedListener {
	private final ListenerList navListeners = new ListenerList();

	private final DateFormat fmt;

	private PlotCommit<?> commit;

	private Color sys_linkColor;

	private Color sys_darkgray;

	private Color sys_hunkHeaderColor;

	private Color sys_linesAddedColor;

	private Color sys_linesRemovedColor;

	private Cursor sys_linkCursor;

	private Cursor sys_normalCursor;

	private boolean fill;

	private Repository db;

	private TreeWalk walker;

	private IPropertyChangeListener listener;

	// the encoding for the currently processed file
	private String currentEncoding = null;

	private static final String SPACE = " "; //$NON-NLS-1$

	private static final String LF = "\n"; //$NON-NLS-1$

	CommitMessageViewer(final Composite parent, final IPageSite site) {
		super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$

		final StyledText t = getTextWidget();
		t.setFont(UIUtils.getFont(UIPreferences.THEME_CommitMessageFont));

		sys_linkColor = t.getDisplay().getSystemColor(SWT.COLOR_BLUE);
		sys_darkgray = t.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
		sys_hunkHeaderColor = t.getDisplay().getSystemColor(SWT.COLOR_BLUE);
		sys_linesAddedColor = t.getDisplay().getSystemColor(
				SWT.COLOR_DARK_GREEN);
		sys_linesRemovedColor = t.getDisplay().getSystemColor(
				SWT.COLOR_DARK_RED);

		sys_linkCursor = t.getDisplay().getSystemCursor(SWT.CURSOR_HAND);

		sys_normalCursor = t.getCursor();

		t.addListener(SWT.MouseMove, new Listener() {
			public void handleEvent(final Event e) {
				final int o;
				try {
					o = t.getOffsetAtLocation(new Point(e.x, e.y));
				} catch (IllegalArgumentException err) {
					t.setCursor(sys_normalCursor);
					return;
				}

				final StyleRange r = t.getStyleRangeAtOffset(o);
				if (r instanceof ObjectLink)
					t.setCursor(sys_linkCursor);
				else
					t.setCursor(sys_normalCursor);
			}
		});
		t.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				// only process the hyperlink if it was a primary mouse click
				if (e.button != 1) {
					return;
				}

				final int o;
				try {
					o = t.getOffsetAtLocation(new Point(e.x, e.y));
				} catch (IllegalArgumentException err) {
					return;
				}

				final StyleRange r = t.getStyleRangeAtOffset(o);
				if (r instanceof ObjectLink) {
					final RevCommit c = ((ObjectLink) r).targetCommit;
					for (final Object l : navListeners.getListeners())
						((CommitNavigationListener) l).showCommit(c);
				}
			}
		});
		setTextDoubleClickStrategy(new DefaultTextDoubleClickStrategy(),
				IDocument.DEFAULT_CONTENT_TYPE);
		activatePlugins();
		listener = new IPropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(
						UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP)) {
					setWrap(((Boolean) event.getNewValue()).booleanValue());
					return;
				}
				if (event.getProperty().equals(
						UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_FILL)) {
					setFill(((Boolean) event.getNewValue()).booleanValue());
					return;
				}
			}
		};

		final IAction selectAll = new Action() {
			@Override
			public void run() {
				doOperation(ITextOperationTarget.SELECT_ALL);
			}

			@Override
			public boolean isEnabled() {
				return canDoOperation(ITextOperationTarget.SELECT_ALL);
			}
		};

		final IAction copy = new Action() {
			@Override
			public void run() {
				doOperation(ITextOperationTarget.COPY);
			}

			@Override
			public boolean isEnabled() {
				return canDoOperation(ITextOperationTarget.COPY);
			}
		};
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(
				listener);
		getControl().addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				site.getActionBars().setGlobalActionHandler(
						ActionFactory.SELECT_ALL.getId(), null);
				site.getActionBars().setGlobalActionHandler(
						ActionFactory.COPY.getId(), null);
				site.getActionBars().updateActionBars();
			}

			public void focusGained(FocusEvent e) {
				site.getActionBars().setGlobalActionHandler(
						ActionFactory.SELECT_ALL.getId(), selectAll);
				site.getActionBars().setGlobalActionHandler(
						ActionFactory.COPY.getId(), copy);
				site.getActionBars().updateActionBars();
			}
		});
	}

	@Override
	protected void handleDispose() {
		Activator.getDefault().getPreferenceStore()
				.removePropertyChangeListener(listener);
		super.handleDispose();
	}

	void addCommitNavigationListener(final CommitNavigationListener l) {
		navListeners.add(l);
	}

	void removeCommitNavigationListener(final CommitNavigationListener l) {
		navListeners.remove(l);
	}

	@Override
	public void setInput(final Object input) {
		commit = (PlotCommit<?>) input;
		format();
	}

	public Object getInput() {
		return commit;
	}

	void setTreeWalk(final TreeWalk walk) {
		walker = walk;
	}

	private void format() {
		if (commit == null) {
			setDocument(new Document(
					UIText.CommitMessageViewer_SelectOneCommitMessage));
			return;
		}

		final PersonIdent author = commit.getAuthorIdent();
		final PersonIdent committer = commit.getCommitterIdent();
		final StringBuilder d = new StringBuilder();
		final ArrayList<StyleRange> styles = new ArrayList<StyleRange>();

		d.append(UIText.CommitMessageViewer_commit);
		d.append(SPACE);
		d.append(commit.getId().name());
		d.append(LF);

		if (author != null) {
			d.append(UIText.CommitMessageViewer_author);
			d.append(": "); //$NON-NLS-1$
			d.append(author.getName());
			d.append(" <"); //$NON-NLS-1$
			d.append(author.getEmailAddress());
			d.append("> "); //$NON-NLS-1$
			d.append(fmt.format(author.getWhen()));
			d.append(LF);
		}

		if (committer != null) {
			d.append(UIText.CommitMessageViewer_committer);
			d.append(": "); //$NON-NLS-1$
			d.append(committer.getName());
			d.append(" <"); //$NON-NLS-1$
			d.append(committer.getEmailAddress());
			d.append("> "); //$NON-NLS-1$
			d.append(fmt.format(committer.getWhen()));
			d.append(LF);
		}

		for (int i = 0; i < commit.getParentCount(); i++) {
			final RevCommit p = commit.getParent(i);
			d.append(UIText.CommitMessageViewer_parent);
			d.append(": "); //$NON-NLS-1$
			addLink(d, styles, p);
			d.append(" ("); //$NON-NLS-1$
			d.append(p.getShortMessage());
			d.append(")"); //$NON-NLS-1$
			d.append(LF);
		}

		for (int i = 0; i < commit.getChildCount(); i++) {
			final RevCommit p = commit.getChild(i);
			d.append(UIText.CommitMessageViewer_child);
			d.append(": "); //$NON-NLS-1$
			addLink(d, styles, p);
			d.append(" ("); //$NON-NLS-1$
			d.append(p.getShortMessage());
			d.append(")"); //$NON-NLS-1$
			d.append(LF);
		}

		List<Ref> branches = getBranches();
		if (!branches.isEmpty()) {
			d.append(UIText.CommitMessageViewer_branches);
			d.append(": "); //$NON-NLS-1$
			for (Iterator<Ref> i = branches.iterator(); i.hasNext(); ) {
				Ref head = i.next();
				RevCommit p;
				try {
					p = new RevWalk(db).parseCommit(head.getObjectId());
					addLink(d, formatHeadRef(head), styles, p);
					if (i.hasNext())
						d.append(", "); //$NON-NLS-1$
				} catch (MissingObjectException e) {
					Activator.logError(e.getMessage(), e);
				} catch (IncorrectObjectTypeException e) {
					Activator.logError(e.getMessage(), e);
				} catch (IOException e) {
					Activator.logError(e.getMessage(), e);
				}
			}
			d.append(LF);
		}

		String tagsString = getTagsString();
		if (tagsString.length() > 0) {
			d.append(UIText.CommitMessageViewer_tags);
			d.append(": "); //$NON-NLS-1$
			d.append(tagsString);
			d.append(LF);
		}

		try {
			Ref followingTag = getNextTag(false);
			if (followingTag != null) {
				d.append(UIText.CommitMessageViewer_follows);
				d.append(": "); //$NON-NLS-1$
				RevCommit p = new RevWalk(db).parseCommit(followingTag.getObjectId());
				addLink(d, formatTagRef(followingTag), styles, p);
				d.append(LF);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		try {
			Ref precedingTag = getNextTag(true);
			if (precedingTag != null) {
				d.append(UIText.CommitMessageViewer_precedes);
				d.append(": "); //$NON-NLS-1$
				RevCommit p = new RevWalk(db).parseCommit(precedingTag.getObjectId());
				addLink(d, formatTagRef(precedingTag), styles, p);
				d.append(LF);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		makeGrayText(d, styles);
		d.append(LF);
		String msg = commit.getFullMessage();
		Pattern p = Pattern.compile("\n([A-Z](?:[A-Za-z]+-)+by: [^\n]+)"); //$NON-NLS-1$
		if (fill) {
			Matcher spm = p.matcher(msg);
			if (spm.find()) {
				String subMsg = msg.substring(0, spm.end());
				msg = subMsg.replaceAll("([\\w.,; \t])\n(\\w)", "$1 $2") //$NON-NLS-1$ //$NON-NLS-2$
						+ msg.substring(spm.end());
			}
		}
		int h0 = d.length();
		d.append(msg);

		d.append(LF);

		addDiff(d, styles);

		Matcher matcher = p.matcher(msg);
		while (matcher.find()) {
			styles.add(new StyleRange(h0 + matcher.start(), matcher.end()
					- matcher.start(), null, null, SWT.ITALIC));
		}

		final StyleRange[] arr = new StyleRange[styles.size()];
		styles.toArray(arr);
		Arrays.sort(arr, new Comparator<StyleRange>() {
			public int compare(StyleRange o1, StyleRange o2) {
				return o1.start - o2.start;
			}
		});
		setDocument(new Document(d.toString()));
		getTextWidget().setStyleRanges(arr);
	}

	private String getTagsString() {
		StringBuilder sb = new StringBuilder();
		Map<String, Ref> tagsMap = db.getTags();
		for (String tagName : tagsMap.keySet()) {
			ObjectId peeledId = tagsMap.get(tagName).getPeeledObjectId();
			if (peeledId != null && peeledId.equals(commit)) {
				if (sb.length() > 0)
					sb.append(", "); //$NON-NLS-1$
				sb.append(tagName);
			}
		}

		return sb.toString();
	}

	private String formatHeadRef(Ref ref) {
		final String name = ref.getName();
		if (name.startsWith(Constants.R_HEADS))
			return name.substring(Constants.R_HEADS.length());
		else if (name.startsWith(Constants.R_REMOTES))
			return name.substring(Constants.R_REMOTES.length());
		return name;
	}

	private String formatTagRef(Ref ref) {
		final String name = ref.getName();
		if (name.startsWith(Constants.R_TAGS))
			return name.substring(Constants.R_TAGS.length());
		return name;
	}

	private void makeGrayText(StringBuilder d, ArrayList<StyleRange> styles) {
		int p0 = 0;
		for (int i = 0; i < styles.size(); ++i) {
			StyleRange r = styles.get(i);
			if (p0 < r.start) {
				StyleRange nr = new StyleRange(p0, r.start - p0, sys_darkgray,
						null);
				styles.add(i, nr);
				p0 = r.start;
			} else {
				if (r.foreground == null)
					r.foreground = sys_darkgray;
				p0 = r.start + r.length;
			}
		}
		if (d.length() - 1 > p0) {
			StyleRange nr = new StyleRange(p0, d.length() - p0, sys_darkgray,
					null);
			styles.add(nr);
		}
	}

	private void addLink(final StringBuilder d, String linkLabel,
			final ArrayList<StyleRange> styles, final RevCommit to) {
		final ObjectLink sr = new ObjectLink();
		sr.targetCommit = to;
		sr.foreground = sys_linkColor;
		sr.underline = true;
		sr.start = d.length();
		d.append(linkLabel);
		sr.length = d.length() - sr.start;
		styles.add(sr);
	}

	private void addLink(final StringBuilder d,
			final ArrayList<StyleRange> styles, final RevCommit to) {
		addLink(d, to.getId().name(), styles, to);
	}

	private void addDiff(final StringBuilder d,
			final ArrayList<StyleRange> styles) {
		final DiffFormatter diffFmt = new DiffFormatter(
				new BufferedOutputStream(new ByteArrayOutputStream() {

					@Override
					public synchronized void write(byte[] b, int off, int len) {
						super.write(b, off, len);
						if (currentEncoding == null)
							d.append(toString());

						else
							try {
								d.append(toString(currentEncoding));
							} catch (UnsupportedEncodingException e) {
								d.append(toString());
							}
						reset();
					}

				})) {
			@Override
			protected void writeHunkHeader(int aCur, int aEnd, int bCur,
					int bEnd) throws IOException {
				flush();
				int start = d.length();
				super.writeHunkHeader(aCur, aEnd, bCur, bEnd);
				flush();
				int end = d.length();
				styles.add(new StyleRange(start, end - start,
						sys_hunkHeaderColor, null));
			}

			@Override
			protected void writeAddedLine(RawText b, int bCur)
					throws IOException {
				flush();
				int start = d.length();
				super.writeAddedLine(b, bCur);
				flush();
				int end = d.length();
				styles.add(new StyleRange(start, end - start,
						sys_linesAddedColor, null));
			}

			@Override
			protected void writeRemovedLine(RawText b, int bCur)
					throws IOException {
				flush();
				int start = d.length();
				super.writeRemovedLine(b, bCur);
				flush();
				int end = d.length();
				styles.add(new StyleRange(start, end - start,
						sys_linesRemovedColor, null));
			}
		};

		if (commit.getParentCount() > 1)
			return;
		try {
			FileDiff[] diffs = FileDiff.compute(walker, commit);

			for (FileDiff diff : diffs) {
				if (diff.getBlobs().length == 2) {
					String path = diff.getPath();
					currentEncoding = CompareUtils
							.getResourceEncoding(db, path);
					d.append(formatPathLine(path)).append("\n"); //$NON-NLS-1$
					diff.outputDiff(d, db, diffFmt, true);
					diffFmt.flush();
				}
			}
		} catch (IOException e) {
			Activator.handleError(NLS.bind(
					UIText.CommitMessageViewer_errorGettingFileDifference,
					commit.getId()), e, false);
		}
	}

	private String formatPathLine(String path) {
		int n = 80 - path.length() - 2;
		if (n < 0)
			return path;
		final StringBuilder d = new StringBuilder();
		int i = 0;
		for (; i < n / 2; i++)
			d.append("-"); //$NON-NLS-1$
		d.append(SPACE).append(path).append(SPACE);
		for (; i < n - 1; i++)
			d.append("-"); //$NON-NLS-1$
		return d.toString();
	}

	static class ObjectLink extends StyleRange {
		RevCommit targetCommit;

		public boolean similarTo(final StyleRange style) {
			if (!(style instanceof ObjectLink))
				return false;
			if (targetCommit != ((ObjectLink) style).targetCommit)
				return false;
			return super.similarTo(style);
		}

		@Override
		public boolean equals(Object object) {
			return super.equals(object)
					&& targetCommit.equals(((ObjectLink) object).targetCommit);
		}

		@Override
		public int hashCode() {
			return super.hashCode() ^ targetCommit.hashCode();
		}
	}

	private void setWrap(boolean wrap) {
		format();
		getTextWidget().setWordWrap(wrap);
	}

	private void setFill(boolean fill) {
		this.fill = fill;
		format();
	}

	public void setDb(Repository db) {
		this.db = db;
	}

	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			Object obj = sel.getFirstElement();
			if (obj instanceof FileDiff) {
				String path = ((FileDiff) obj).getPath();
				findAndSelect(0, formatPathLine(path), true, true, false, false);
			}
		}

	}

	/**
	 * Finds nextdoor tagged revition. Searches forwards (in descendants) or backwards (in ancestors)
	 * @param searchDescendant if <code>false</code>, will search for tagged revision in ancestors
	 * @return {@link Ref} or <code>null</code> if no tag found
	 * @throws IOException
	 */
	private Ref getNextTag(boolean searchDescendant) throws IOException {
		RevWalk revWalk = new RevWalk(db);

		Map<String, Ref> tagsMap = db.getTags();
		Ref tagRef = null;

		for (String tagName : tagsMap.keySet()) {
			// both RevCommits must be allocated using same RevWalk instance,
			// otherwise isMergedInto returns wrong result!
			RevCommit current = revWalk.parseCommit(commit);
			RevCommit newTag = revWalk.parseCommit(tagsMap.get(tagName).getObjectId());

			if (newTag.getId().equals(commit))
				continue;

			// check if newTag matches our criteria
			if (isMergedInto(revWalk, newTag, current, searchDescendant)) {
				if (tagRef != null) {
					RevCommit oldTag = revWalk.parseCommit(tagRef.getObjectId());

					// both oldTag and newTag satisfy search criteria, so taking the closest one
					if (isMergedInto(revWalk, oldTag, newTag, searchDescendant))
						tagRef = tagsMap.get(tagName);
				} else
					tagRef = tagsMap.get(tagName);
			}
		}

		return tagRef;
	}

	/**
	 * @param rw
	 * @param base
	 * @param tip
	 * @param swap if <code>true</code>, base and tip arguments are swapped
	 * @return <code>true</code> if there is a path directly from tip to base (and thus base is fully merged into tip); <code>false</code> otherwise.
	 * @throws IOException
	 */
	private boolean isMergedInto(final RevWalk rw, final RevCommit base, final RevCommit tip, boolean swap)
			throws IOException {
		return !swap ? rw.isMergedInto(base, tip) : rw.isMergedInto(tip, base);
	}

	/**
	 * @return List of heads from those current commit is reachable
	 */
	private List<Ref> getBranches() {
		RevWalk revWalk = new RevWalk(db);
		List<Ref> result = new ArrayList<Ref>();

		try {
			Map<String, Ref> refsMap = new HashMap<String, Ref>();
			refsMap.putAll(db.getRefDatabase().getRefs(Constants.R_HEADS));
			// add remote heads to search
			refsMap.putAll(db.getRefDatabase().getRefs(Constants.R_REMOTES));

			for (String headName : refsMap.keySet()) {
				RevCommit headCommit = revWalk.parseCommit(refsMap.get(headName).getObjectId());
				// the base RevCommit also must be allocated using same RevWalk instance,
				// otherwise isMergedInto returns wrong result!
				RevCommit base = revWalk.parseCommit(commit);

				if (revWalk.isMergedInto(base, headCommit))
					result.add(refsMap.get(headName));	// commit is reachable from this head
			}
		} catch (IOException e) {
			// skip exception
		}

		return result;
	}

}
