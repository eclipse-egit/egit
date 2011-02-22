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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

class CommitMessageViewer extends TextViewer implements
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

	CommitMessageViewer(final Composite parent, final IPageSite site, IWorkbenchPartSite partSite) {
		super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		this.partSite = partSite;

		final StyledText t = getTextWidget();
		t.setFont(UIUtils.getFont(UIPreferences.THEME_CommitMessageFont));

		sys_normalCursor = t.getCursor();

		// set the cursor when hovering over a link
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
			}
		};
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(
				listener);

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
	}

	void addDoneListenerToFormatJob() {
		formatJob.addJobChangeListener(new IJobChangeListener() {

			public void sleeping(IJobChangeEvent event) {
				// empty
			}

			public void scheduled(IJobChangeEvent event) {
				// empty
			}

			public void running(IJobChangeEvent event) {
				// empty
			}

			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					if (getTextWidget().isDisposed())
						return;
					final FormatJob job = (FormatJob) event.getJob();
					getTextWidget().getDisplay().asyncExec(new Runnable() {
						public void run() {
							setDocument(new Document(job.getFormatResult().getCommitInfo()));
							getTextWidget().setStyleRanges(job.getFormatResult().getStyleRange());
						}
					});
				}
			}

			public void awake(IJobChangeEvent event) {
				// empty
			}

			public void aboutToRun(IJobChangeEvent event) {
				// empty
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
		format();
	}

	public Object getInput() {
		return commit;
	}

	void setRepository(final Repository repository) {
		this.db = repository;
	}

	private void format() {
		if (commit == null) {
			setDocument(new Document(
					UIText.CommitMessageViewer_SelectOneCommitMessage));
			return;
		}
		if (formatJob != null && formatJob.getState() != Job.NONE) {
			formatJob.cancel();
		}
		scheduleFormatJob();
	}

	private void scheduleFormatJob() {
		IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) partSite
				.getAdapter(IWorkbenchSiteProgressService.class);
		if (siteService == null)
			return;
		FormatJob.FormatRequest formatRequest = new FormatJob.FormatRequest(db,
				commit, fill, currentDiffs, SYS_LINKCOLOR, SYS_DARKGRAY,
				SYS_HUNKHEADER_COLOR, SYS_LINES_ADDED_COLOR,
				SYS_LINES_REMOVED_COLOR);
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



}
