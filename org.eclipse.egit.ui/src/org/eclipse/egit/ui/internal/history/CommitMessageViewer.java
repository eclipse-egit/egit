/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2011, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2014, Marc-Andre Laperle <marc-andre.laperle@ericsson.com>
 * Copyright (C) 2015, IBM Corporation (Dani Megert <daniel_megert@ch.ibm.com>)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.BooleanPrefAction;
import org.eclipse.egit.ui.internal.history.FormatJob.FormatResult;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

class CommitMessageViewer extends SourceViewer {

	private static final Color SYS_LINKCOLOR = PlatformUI.getWorkbench()
			.getDisplay().getSystemColor(SWT.COLOR_BLUE);

	private static final Color SYS_DARKGRAY = PlatformUI.getWorkbench()
			.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);

	private static final Cursor SYS_LINK_CURSOR = PlatformUI.getWorkbench()
			.getDisplay().getSystemCursor(SWT.CURSOR_HAND);

	private final Cursor sys_normalCursor;

	// notified when clicking on a link in the message (branch, commit...)
	private final ListenerList navListeners = new ListenerList();

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

	private BooleanPrefAction showTagSequencePrefAction;

	private BooleanPrefAction wrapCommentsPrefAction;

	private BooleanPrefAction fillParagraphsPrefAction;

	CommitMessageViewer(final Composite parent, final IPageSite site, IWorkbenchPartSite partSite) {
		super(parent, null, SWT.READ_ONLY);
		this.partSite = partSite;

		final StyledText t = getTextWidget();
		t.setFont(UIUtils.getFont(UIPreferences.THEME_CommitMessageFont));

		sys_normalCursor = t.getCursor();

		// set the cursor when hovering over a link
		t.addListener(SWT.MouseMove, new Listener() {
			@Override
			public void handleEvent(final Event e) {
				StyleRange styleRange = getStyleRange(e.x, e.y);
				if (styleRange != null && styleRange.underline)
					t.setCursor(SYS_LINK_CURSOR);
				else
					t.setCursor(sys_normalCursor);
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
			@Override
			public void propertyChange(PropertyChangeEvent event) {
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
			@Override
			public void focusLost(FocusEvent e) {
				site.getActionBars().setGlobalActionHandler(
						ActionFactory.SELECT_ALL.getId(), null);
				site.getActionBars().setGlobalActionHandler(
						ActionFactory.COPY.getId(), null);
				site.getActionBars().updateActionBars();
			}

			@Override
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

		showTagSequencePrefAction = new BooleanPrefAction(pstore,
				UIPreferences.HISTORY_SHOW_TAG_SEQUENCE,
				UIText.ResourceHistory_ShowTagSequence) {
			@Override
			protected void apply(boolean value) {
				// nothing, just toggle
			}
		};
		mgr.add(showTagSequencePrefAction);

		wrapCommentsPrefAction = new BooleanPrefAction(pstore,
				UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP,
				UIText.ResourceHistory_toggleCommentWrap) {
			@Override
			protected void apply(boolean value) {
				// nothing, just toggle
			}
		};
		mgr.add(wrapCommentsPrefAction);

		fillParagraphsPrefAction = new BooleanPrefAction(pstore,
				UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_FILL,
				UIText.ResourceHistory_toggleCommentFill) {
			@Override
			protected void apply(boolean value) {
				// nothing, just toggle
			}
		};
		mgr.add(fillParagraphsPrefAction);

	}

	void addDoneListenerToFormatJob() {
		formatJob.addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				if (!event.getResult().isOK())
					return;
				final StyledText text = getTextWidget();
				if (text == null || text.isDisposed())
					return;
				final FormatJob job = (FormatJob) event.getJob();
				text.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						applyFormatJobResultInUI(job.getFormatResult());
					}
				});
			}
		});
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
		showTagSequencePrefAction.dispose();
		wrapCommentsPrefAction.dispose();
		fillParagraphsPrefAction.dispose();

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
		commit = (PlotCommit<?>) input;
		if (refsChangedListener != null) {
			refsChangedListener.remove();
			refsChangedListener = null;
		}

		if (db != null) {
			allRefs = getBranches(db);
			refsChangedListener = db.getListenerList().addRefsChangedListener(
					new RefsChangedListener() {

						@Override
						public void onRefsChanged(RefsChangedEvent event) {
							allRefs = getBranches(db);
						}
					});
		}
		format();
	}

	@Override
	public Object getInput() {
		return commit;
	}

	void setRepository(final Repository repository) {
		this.db = repository;
	}

	private static List<Ref> getBranches(Repository repo)  {
		List<Ref> ref = new ArrayList<Ref>();
		try {
			RefDatabase refDb = repo.getRefDatabase();
			ref.addAll(refDb.getRefs(Constants.R_HEADS).values());
			ref.addAll(refDb.getRefs(Constants.R_REMOTES).values());
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
		if (db == null || commit == null) {
			setDocument(new Document("")); //$NON-NLS-1$
			return;
		}
		if (formatJob != null && formatJob.getState() != Job.NONE)
			formatJob.cancel();
		scheduleFormatJob();
	}

	private void scheduleFormatJob() {
		IWorkbenchSiteProgressService siteService = CommonUtils.getAdapter(partSite, IWorkbenchSiteProgressService.class);
		if (siteService == null)
			return;
		FormatJob.FormatRequest formatRequest = new FormatJob.FormatRequest(
				getRepository(), commit, fill, SYS_LINKCOLOR, SYS_DARKGRAY,
				allRefs);
		formatJob = new FormatJob(formatRequest);
		addDoneListenerToFormatJob();
		siteService.schedule(formatJob, 0 /* now */, true /*
														 * use the half-busy
														 * cursor in the part
														 */);
	}

	private void applyFormatJobResultInUI(FormatResult formatResult) {
		StyledText text = getTextWidget();
		if (!UIUtils.isUsable(text))
			return;

		setDocument(new Document(formatResult.getCommitInfo()));

		// Set style ranges from format job. We know that they are already
		// ordered and don't overlap.
		text.setStyleRanges(formatResult.getStyleRange());

		// Apply additional styles. If we combined them with the above style
		// ranges and set them all at once, we would have to manually remove
		// overlapping ones.
		UIUtils.applyHyperlinkDetectorStyleRanges(CommitMessageViewer.this,
				fHyperlinkDetectors);
	}

	static final class ObjectLink extends StyleRange {
		RevCommit targetCommit;

		@Override
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

	private void setFill(boolean fill) {
		this.fill = fill;
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
