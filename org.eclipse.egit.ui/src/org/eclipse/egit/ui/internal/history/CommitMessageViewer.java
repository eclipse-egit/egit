/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2011, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.BooleanPrefAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

class CommitMessageViewer extends SourceViewer implements
		ISelectionChangedListener {

	private static final Color SYS_LINKCOLOR = PlatformUI.getWorkbench()
			.getDisplay().getSystemColor(SWT.COLOR_BLUE);

	private static final Color SYS_DARKGRAY = PlatformUI.getWorkbench()
			.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);

	private static final Color SYS_HUNKHEADER_COLOR = PlatformUI.getWorkbench()
			.getDisplay().getSystemColor(SWT.COLOR_BLUE);

	private static final Color SYS_LINES_ADDED_COLOR = PlatformUI
			.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);

	private static final Color SYS_LINES_REMOVED_COLOR = PlatformUI
			.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_RED);

	private static final Cursor SYS_LINK_CURSOR = PlatformUI.getWorkbench()
			.getDisplay().getSystemCursor(SWT.CURSOR_HAND);

	private final Cursor sys_normalCursor;

	// notified when clicking on a link in the message (branch, commit...)
	private final ListenerList navListeners = new ListenerList();

	// set by selecting files in the file list
	private final List<FileDiff> currentDiffs = new ArrayList<FileDiff>();

	// listener to detect changes in the wrap and fill preferences
	private final IPropertyChangeListener listener;

	// the current repository
	private Repository db;

	// the "input" (set by setInput())
	private PlotCommit<?> commit;

	// formatting option to fill the lines
	private boolean fill;

	private FormatJob formatJob;

	private final IWorkbenchPartSite partSite;

	private List<Ref> allRefs;

	private ListenerHandle refsChangedListener;

	private StyleRange[] styleRanges;

	CommitMessageViewer(final Composite parent, final IPageSite site, IWorkbenchPartSite partSite) {
		super(parent, null, SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		this.partSite = partSite;

		final StyledText t = getTextWidget();
		t.setFont(UIUtils.getFont(UIPreferences.THEME_CommitMessageFont));

		sys_normalCursor = t.getCursor();

		// set the cursor when hovering over a link
		t.addListener(SWT.MouseMove, new Listener() {
			public void handleEvent(final Event e) {
				StyleRange styleRange = getStyleRange(e.x, e.y);
				if (styleRange != null && styleRange.underline)
					t.setCursor(SYS_LINK_CURSOR);
				else
					t.setCursor(sys_normalCursor);
				for (StyleRange sr : styleRanges) {
					getTextWidget().setStyleRange(sr);
				}
			}
		});
		// react on link click
		t.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				// only process the hyper link if it was a primary mouse click
				if (e.button != 1)
					return;

				final StyleRange r = getStyleRange(e.x, e.y);
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

		// react on changes in the fill and wrap preferences
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
				if (event.getProperty().equals(UIPreferences.HISTORY_SHOW_TAG_SEQUENCE)) {
					format();
					return;
				}
			}
		};

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.addPropertyChangeListener(listener);
		fill = store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_FILL);
		setWrap(store
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP));

		// global action handlers for select all and copy
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
		// register and unregister the global actions upon focus events
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

		final MenuManager mgr = new MenuManager();
		Control c = getControl();
		c.setMenu(mgr.createContextMenu(c));

		IPersistentPreferenceStore pstore = (IPersistentPreferenceStore) store;

		Action showTagSequence = new BooleanPrefAction(pstore, UIPreferences.HISTORY_SHOW_TAG_SEQUENCE, UIText.ResourceHistory_ShowTagSequence) {
			@Override
			protected void apply(boolean value) {
				// nothing, just toggle
			}
		};
		mgr.add(showTagSequence);

		Action wrapComments = new BooleanPrefAction(pstore, UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP, UIText.ResourceHistory_toggleCommentWrap) {
			@Override
			protected void apply(boolean value) {
				// nothing, just toggle
			}
		};
		mgr.add(wrapComments);

		Action fillParagraphs = new BooleanPrefAction(pstore, UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_FILL, UIText.ResourceHistory_toggleCommentFill) {
			@Override
			protected void apply(boolean value) {
				// nothing, just toggle
			}
		};
		mgr.add(fillParagraphs);

	}

	void addDoneListenerToFormatJob() {
		formatJob.addJobChangeListener(new JobChangeAdapter() {

			public void done(IJobChangeEvent event) {
				if (!event.getResult().isOK())
					return;
				final StyledText text = getTextWidget();
				if (text == null || text.isDisposed())
					return;
				final FormatJob job = (FormatJob) event.getJob();
				text.getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (text.isDisposed())
							return;

						setDocument(new Document(job.getFormatResult()
								.getCommitInfo()));

						// Combine the style ranges from the format job and the
						// style ranges
						// for hyperlinks found by registered hyperlink
						// detectors.

						List<StyleRange> styleRangeList = new ArrayList<StyleRange>();

						for (StyleRange styleRange : job.getFormatResult()
								.getStyleRange())
							styleRangeList.add(styleRange);

						StyleRange[] hyperlinkDetectorStyleRanges = getHyperlinkDetectorStyleRanges();

						for (StyleRange styleRange : hyperlinkDetectorStyleRanges)
							styleRangeList.add(styleRange);

						styleRanges = new StyleRange[styleRangeList
								.size()];
						styleRangeList.toArray(styleRanges);

						// Style ranges must be in order.
						Arrays.sort(styleRanges, new Comparator<StyleRange>() {
							public int compare(StyleRange o1, StyleRange o2) {
								if (o2.start > o1.start)
									return -1;
								if (o1.start > o2.start)
									return 1;
								return 0;
							}
						});

						text.setStyleRanges(new StyleRange[0]);
						for (StyleRange sr : styleRanges) {
							text.setStyleRange(sr);
						}
					}
				});
			}
		});
	}

	private StyleRange[] getHyperlinkDetectorStyleRanges() {
		List<StyleRange> styleRangeList = new ArrayList<StyleRange>();
		if (fHyperlinkDetectors != null && fHyperlinkDetectors.length > 0) {
			for (int i = 0; i < getTextWidget().getText().length(); i++) {
				IRegion region = new Region(i, 0);
				for (IHyperlinkDetector hyperLinkDetector : fHyperlinkDetectors) {
					IHyperlink[] hyperlinks = hyperLinkDetector
							.detectHyperlinks(this, region, true);
					if (hyperlinks != null) {
						for (IHyperlink hyperlink : hyperlinks) {
							StyleRange hyperlinkStyleRange = new StyleRange(
									hyperlink.getHyperlinkRegion().getOffset(),
									hyperlink.getHyperlinkRegion().getLength(),
									Display.getDefault().getSystemColor(
											SWT.COLOR_BLUE), Display
											.getDefault().getSystemColor(
													SWT.COLOR_WHITE));
							hyperlinkStyleRange.underline = true;
							styleRangeList.add(hyperlinkStyleRange);
						}
					}
				}
			}
		}
		StyleRange[] styleRangeArray = new StyleRange[styleRangeList.size()];
		styleRangeList.toArray(styleRangeArray);
		return styleRangeArray;
	}

	@Override
	protected void handleDispose() {
		if (formatJob != null) {
			formatJob.cancel();
			formatJob = null;
		}
		Activator.getDefault().getPreferenceStore()
				.removePropertyChangeListener(listener);
		if (refsChangedListener != null)
			refsChangedListener.remove();
		refsChangedListener = null;
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
		// right-clicking on a commit will fire selection change events,
		// so we only rebuild this when the commit did in fact change
		if (input == commit)
			return;
		currentDiffs.clear();
		commit = (PlotCommit<?>) input;
		allRefs = getBranches();
		if (refsChangedListener != null)
			refsChangedListener.remove();
		refsChangedListener = db.getListenerList().addRefsChangedListener(new RefsChangedListener() {

			public void onRefsChanged(RefsChangedEvent event) {
				allRefs = getBranches();
			}
		});
		format();
	}

	public Object getInput() {
		return commit;
	}

	void setRepository(final Repository repository) {
		this.db = repository;
	}

	private List<Ref> getBranches()  {
		List<Ref> ref = new ArrayList<Ref>();
		try {
			ref.addAll(db.getRefDatabase().getRefs(Constants.R_HEADS).values());
			ref.addAll(db.getRefDatabase().getRefs(Constants.R_REMOTES).values());
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}
		return ref;
	}

	private Repository getRepository() {
		if (db == null)
			throw new IllegalStateException("Repository has not been set"); //$NON-NLS-1$
		return db;
	}

	private void format() {
		if (commit == null) {
			setDocument(new Document("")); //$NON-NLS-1$
			return;
		}
		if (formatJob != null && formatJob.getState() != Job.NONE)
			formatJob.cancel();
		scheduleFormatJob();
	}

	private void scheduleFormatJob() {
		IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) partSite
				.getAdapter(IWorkbenchSiteProgressService.class);
		if (siteService == null)
			return;
		FormatJob.FormatRequest formatRequest = new FormatJob.FormatRequest(getRepository(),
				commit, fill, currentDiffs, SYS_LINKCOLOR, SYS_DARKGRAY,
				SYS_HUNKHEADER_COLOR, SYS_LINES_ADDED_COLOR,
				SYS_LINES_REMOVED_COLOR,
				allRefs);
		formatJob = new FormatJob(formatRequest);
		addDoneListenerToFormatJob();
		siteService.schedule(formatJob, 0 /* now */, true /*
														 * use the half-busy
														 * cursor in the part
														 */);
	}

	static final class ObjectLink extends StyleRange {
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
		getTextWidget().setWordWrap(wrap);
	}

	private void setFill(boolean fill) {
		this.fill = fill;
		format();
	}

	public void selectionChanged(SelectionChangedEvent event) {
		currentDiffs.clear();
		ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			for (Object obj : sel.toList())
				if (obj instanceof FileDiff)
					currentDiffs.add((FileDiff) obj);
		}
		format();
	}

	/**
	 * Get style range at x/y coordinates
	 *
	 * @param x
	 * @param y
	 * @return style range, will be null when no style range exists at given
	 *         coordinates
	 */
	private StyleRange getStyleRange(final int x, final int y) {
		final StyledText t = getTextWidget();
		final int offset;
		try {
			offset = t.getOffsetAtLocation(new Point(x, y));
		} catch (IllegalArgumentException e) {
			return null;
		}
		if (offset < t.getCharCount())
			return t.getStyleRangeAtOffset(offset);
		else
			return null;
	}
}
