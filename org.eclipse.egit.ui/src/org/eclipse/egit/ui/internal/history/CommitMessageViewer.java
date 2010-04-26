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

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.MyersDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

class CommitMessageViewer extends TextViewer implements ISelectionChangedListener{
	private final ListenerList navListeners = new ListenerList();

	private final DateFormat fmt;

	private PlotCommit<?> commit;

	private Color sys_linkColor;

	private Color sys_darkgray;

	private Cursor sys_linkCursor;

	private Cursor sys_normalCursor;

	private boolean fill;

	private Repository db;

	private TreeWalk walker;

	private DiffFormatter diffFmt = new DiffFormatter();

	private static final String SPACE = " "; //$NON-NLS-1$

	private static final String LF = "\n"; //$NON-NLS-1$

	private static final String EMPTY = ""; //$NON-NLS-1$

	CommitMessageViewer(final Composite parent) {
		super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$

		final StyledText t = getTextWidget();
		t.setFont(UIUtils.getFont(UIPreferences.THEME_CommitMessageFont));

		sys_linkColor = t.getDisplay().getSystemColor(SWT.COLOR_BLUE);
		sys_darkgray = t.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
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
			setDocument(new Document());
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
			d.append(": ");  //$NON-NLS-1$
			d.append(author.getName());
			d.append(" <"); //$NON-NLS-1$
			d.append(author.getEmailAddress());
			d.append("> "); //$NON-NLS-1$
			d.append(fmt.format(author.getWhen()));
			d.append(LF);
		}

		if (committer != null) {
			d.append(UIText.CommitMessageViewer_committer);
			d.append(": ");  //$NON-NLS-1$
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
			d.append(": ");  //$NON-NLS-1$
			addLink(d, styles, p);
			d.append(" ("); //$NON-NLS-1$
			d.append(p.getShortMessage());
			d.append(")"); //$NON-NLS-1$
			d.append(LF);
		}

		for (int i = 0; i < commit.getChildCount(); i++) {
			final RevCommit p = commit.getChild(i);
			d.append(UIText.CommitMessageViewer_child);
			d.append(":  ");  //$NON-NLS-1$
			addLink(d, styles, p);
			d.append(" ("); //$NON-NLS-1$
			d.append(p.getShortMessage());
			d.append(")"); //$NON-NLS-1$
			d.append(LF);
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

		addDiff(d);


		Matcher matcher = p.matcher(msg);
		while (matcher.find()) {
			styles.add(new StyleRange(h0 + matcher.start(), matcher.end()-matcher.start(), null,  null, SWT.ITALIC));
		}

		final StyleRange[] arr = new StyleRange[styles.size()];
		styles.toArray(arr);
		setDocument(new Document(d.toString()));
		getTextWidget().setStyleRanges(arr);
	}

	private void makeGrayText(StringBuilder d,
			ArrayList<StyleRange> styles) {
		int p0 = 0;
		for (int i = 0; i<styles.size(); ++i) {
			StyleRange r = styles.get(i);
			if (p0 < r.start) {
				StyleRange nr = new StyleRange(p0, r.start  - p0, sys_darkgray, null);
				styles.add(i, nr);
				p0 = r.start;
			} else {
				if (r.foreground == null)
					r.foreground = sys_darkgray;
				p0 = r.start + r.length;
			}
		}
		if (d.length() - 1 > p0) {
			StyleRange nr = new StyleRange(p0, d.length() - p0, sys_darkgray, null);
			styles.add(nr);
		}
	}

	private void addLink(final StringBuilder d,
			final ArrayList<StyleRange> styles, final RevCommit to) {
		final ObjectLink sr = new ObjectLink();
		sr.targetCommit = to;
		sr.foreground = sys_linkColor;
		sr.underline = true;
		sr.start = d.length();
		d.append(to.getId().name());
		sr.length = d.length() - sr.start;
		styles.add(sr);
	}

	private void addDiff(final StringBuilder d) {
		if (!(commit.getParentCount() == 1))
			return;
		try {
			FileDiff[] diffs = FileDiff.compute(walker, commit);

			for (FileDiff diff : diffs) {
				if (diff.blobs.length == 2)
					outputDiff(d, diff);
			}
		} catch (IOException e) {
			// TODO throw an exception or log this?
			Activator.error(NLS.bind(
					UIText.CommitMessageViewer_errorGettingFileDifference,
					commit.getId()), e);
		}
	}

	private void outputDiff(final StringBuilder d, FileDiff fileDiff) throws IOException {
		String path = fileDiff.path;
		ObjectId id1 = fileDiff.blobs[0];
		ObjectId id2 = fileDiff.blobs[1];
		FileMode mode1 = fileDiff.modes[0];
		FileMode mode2 = fileDiff.modes[1];

		d.append(formatPathLine(path)).append(LF);
		if (id1.equals(ObjectId.zeroId())) {
			d.append(UIText.CommitMessageViewer_newFileMode
					+ SPACE
					+ mode2).append(LF);
		} else if (id2.equals(ObjectId.zeroId())) {
			d.append(UIText.CommitMessageViewer_deletedFileMode + SPACE + mode1).append(LF);
		} else if (!mode1.equals(mode2)) {
			d.append(UIText.CommitMessageViewer_oldMode + SPACE + mode1);
			d.append(UIText.CommitMessageViewer_newMode + SPACE + mode2).append(LF);
		}
		d.append(UIText.CommitMessageViewer_index).append(SPACE).append(id1.abbreviate(db, 7).name()).
			append("..").append(id2.abbreviate(db, 7).name()). //$NON-NLS-1$
			append (mode1.equals(mode2) ? SPACE + mode1 : EMPTY). append(LF);

		RawText a = getRawText(id1);
		RawText b = getRawText(id2);
		MyersDiff diff = new MyersDiff(a, b);
		diffFmt.formatEdits(new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				d.append((char) b);

			}
		 } , a, b, diff.getEdits());
		d.append(LF);
	}

	private String formatPathLine(String path) {
		int n = 80 - path.length() - 2;
		if (n < 0 )
			return path;
		final StringBuilder d = new StringBuilder();
		int i = 0;
		for (; i < n/2; i++)
			d.append("-"); //$NON-NLS-1$
		d.append(SPACE).append(path).append(SPACE);
		for (; i < n - 1; i++)
			d.append("-"); //$NON-NLS-1$
		return d.toString();
	}


	private RawText getRawText(ObjectId id) throws IOException {
		if (id.equals(ObjectId.zeroId()))
			return new RawText(new byte[] { });
		return new RawText(db.openBlob(id).getCachedBytes());
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
	}

	void setWrap(boolean wrap) {
		format();
		getTextWidget().setWordWrap(wrap);
	}

	void setFill(boolean fill) {
		this.fill = fill;
		format();
	}

	public void setDb(Repository db) {
		this.db = db;
	}

	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection)selection;
			Object obj = sel.getFirstElement();
			if (obj instanceof FileDiff) {
				String path = ((FileDiff)obj).path;
				findAndSelect(0, formatPathLine(path), true, true, false, false);
			}
		}

	}

}
