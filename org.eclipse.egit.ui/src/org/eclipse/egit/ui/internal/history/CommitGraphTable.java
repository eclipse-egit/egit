/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011-2012, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2011-2012, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2012-2013, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.CreatePatchOperation;
import org.eclipse.egit.core.op.CreatePatchOperation.DiffHeaderFormat;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.ActionUtils;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.SWTCommitList.SWTLane;
import org.eclipse.egit.ui.internal.history.command.ShowVersionsHandler;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.IPageSite;

class CommitGraphTable {
	static Font highlightFont() {
		final Font n, h;

		n = UIUtils.getFont(UIPreferences.THEME_CommitGraphNormalFont);
		h = UIUtils.getFont(UIPreferences.THEME_CommitGraphHighlightFont);

		final FontData[] nData = n.getFontData();
		final FontData[] hData = h.getFontData();
		if (nData.length != hData.length)
			return h;
		for (int i = 0; i < nData.length; i++)
			if (!nData[i].equals(hData[i]))
				return h;

		return UIUtils.getBoldFont(UIPreferences.THEME_CommitGraphNormalFont);
	}

	private static final String LINESEP = System.getProperty("line.separator"); //$NON-NLS-1$

	private final TableViewer table;

	private Clipboard clipboard;

	private final SWTPlotRenderer renderer;

	private final Font nFont;

	private final Font hFont;

	private SWTCommitList allCommits;

	private int allCommitsLength = 0;

	// used for resolving PlotCommit objects by ids
	private HashMap<String, PlotCommit> commitsMap = null;

	private RevFlag highlight;

	private HistoryPageInput input;

	IAction copy;

	private RevCommit commitToShow;

	private final TableLoader tableLoader;

	private boolean trace = GitTraceLocation.HISTORYVIEW.isActive();

	private boolean enableAntialias = true;

	CommitGraphTable(Composite parent, final TableLoader loader,
			final ResourceManager resources) {
		this(parent, loader, resources, true);
	}

	CommitGraphTable(Composite parent, final TableLoader loader,
			final ResourceManager resources, boolean canShowEmailAddresses) {
		nFont = UIUtils.getFont(UIPreferences.THEME_CommitGraphNormalFont);
		hFont = highlightFont();
		tableLoader = loader;

		Composite tableContainer = new Composite(parent, SWT.NONE);
		final Table rawTable = new Table(tableContainer,
				SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
		rawTable.setHeaderVisible(true);
		rawTable.setLinesVisible(false);
		rawTable.setFont(nFont);
		rawTable.addListener(SWT.SetData, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (tableLoader != null) {
					TableItem item = (TableItem) event.item;
					int index = rawTable.indexOf(item);
					if (trace)
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.HISTORYVIEW.getLocation(),
								"Item " + index); //$NON-NLS-1$
					tableLoader.loadItem(index);
				}
			}
		});

		TableColumnLayout layout = new TableColumnLayout();
		tableContainer.setLayout(layout);

		createColumns(rawTable, layout);
		createPaintListener(rawTable);

		rawTable.addListener(SWT.Resize, event -> layout(rawTable, layout));

		table = new TableViewer(rawTable) {
			@Override
			protected Widget doFindItem(final Object element) {
				return element != null ? ((SWTCommit) element).widget : null;
			}

			@Override
			protected void mapElement(final Object element, final Widget item) {
				if (element == null) {
					return;
				}
				((SWTCommit) element).widget = item;
			}
		};

		GraphLabelProvider graphLabelProvider = new GraphLabelProvider(
				canShowEmailAddresses);
		graphLabelProvider.addListener(new ILabelProviderListener() {
			@Override
			public void labelProviderChanged(LabelProviderChangedEvent event) {
				table.refresh();
			}
		});
		table.setLabelProvider(graphLabelProvider);
		table.setContentProvider(new GraphContentProvider());
		renderer = new SWTPlotRenderer(rawTable.getDisplay(), resources);

		clipboard = new Clipboard(rawTable.getDisplay());
		rawTable.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				clipboard.dispose();
			}
		});

		copy = ActionUtils.createGlobalAction(ActionFactory.COPY,
				() -> doCopy());
		copy.setText(UIText.CommitGraphTable_CopyCommitIdLabel);
		copy.setImageDescriptor(UIIcons.ELCL16_ID);
		table.setUseHashlookup(true);

		table.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection s = event.getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection))
					return;
				final IStructuredSelection iss = (IStructuredSelection) s;
				commitToShow = (PlotCommit<?>) iss.getFirstElement();

				copy.setEnabled(canDoCopy());
			}
		});

		final CommitGraphTableHoverManager hoverManager = new CommitGraphTableHoverManager(
				table, renderer);
		hoverManager.install(table.getTable());

		table.getTable().addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (allCommits != null)
					allCommits.dispose();
				hoverManager.dispose();
			}
		});

		Transfer[] transferTypes = new Transfer[] {TextTransfer.getInstance(), FileTransfer.getInstance()};
		table.addDragSupport(DND.DROP_DEFAULT | DND.DROP_COPY, transferTypes,
				new CommitDragSourceListener());
	}

	CommitGraphTable(final Composite parent, final IPageSite site,
			final MenuManager menuMgr, final TableLoader loader,
			final ResourceManager resources) {
		this(parent, loader, resources);

		final IAction selectAll = ActionUtils.createGlobalAction(
				ActionFactory.SELECT_ALL,
				() -> getTableView().getTable().selectAll());
		ActionUtils.setGlobalActions(getControl(), copy, selectAll);

		getTableView().addOpenListener(new IOpenListener() {
			@Override
			public void open(OpenEvent event) {
				if (input == null || !input.isSingleFile())
					return;

				ICommandService srv = CommonUtils.getService(site, ICommandService.class);
				IHandlerService hsrv = CommonUtils.getService(site, IHandlerService.class);
				Command cmd = srv.getCommand(ShowVersionsHandler.COMMAND_ID);
				Parameterization[] parms;
				if (Activator.getDefault().getPreferenceStore().getBoolean(
						UIPreferences.RESOURCEHISTORY_COMPARE_MODE))
					try {
						IParameter parm = cmd
								.getParameter(
										ShowVersionsHandler.COMPARE_MODE_PARAM);
						parms = new Parameterization[] { new Parameterization(
								parm, Boolean.TRUE.toString()) };
					} catch (NotDefinedException e) {
						Activator.handleError(e.getMessage(), e, true);
						parms = null;
					}
				else
					parms = null;
				ParameterizedCommand pcmd = new ParameterizedCommand(cmd, parms);
				try {
					hsrv.executeCommandInContext(pcmd, null, hsrv
							.getCurrentState());
				} catch (Exception e) {
					Activator.handleError(e.getMessage(), e, true);
				}
			}
		});

		menuMgr.add(new Separator(IWorkbenchActionConstants.HISTORY_GROUP));
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menuMgr.add(new Separator());
		menuMgr.add(copy);
		Control c = getControl();
		menuMgr.addMenuListener(manager -> c.setFocus());
		c.setMenu(menuMgr.createContextMenu(c));
	}

	Control getControl() {
		return table.getControl();
	}

	void selectCommitStored(final RevCommit c) {
		commitToShow = c;
		selectCommit(c);
	}

	void selectCommit(final RevCommit c) {
		if (c instanceof PlotCommit)
			table.setSelection(new StructuredSelection(c), true);
		else if (commitsMap != null) {
			PlotCommit swtCommit = commitsMap.get(c.getId().name());
			if (swtCommit == null && tableLoader != null)
				tableLoader.loadCommit(c);
			if (swtCommit != null)
				table.setSelection(new StructuredSelection(swtCommit), true);
		}
	}

	void addSelectionChangedListener(final ISelectionChangedListener l) {
		table.addPostSelectionChangedListener(l);
	}

	void removeSelectionChangedListener(final ISelectionChangedListener l) {
		table.removePostSelectionChangedListener(l);
	}

	private boolean canDoCopy() {
		return !table.getSelection().isEmpty();
	}

	private void doCopy() {
		final ISelection s = table.getSelection();
		if (s.isEmpty() || !(s instanceof IStructuredSelection))
			return;
		final IStructuredSelection iss = (IStructuredSelection) s;
		final Iterator<PlotCommit> itr = iss.iterator();
		final StringBuilder r = new StringBuilder();
		while (itr.hasNext()) {
			final PlotCommit d = itr.next();
			if (r.length() > 0)
				r.append(LINESEP);
			r.append(d.getId().name());
		}

		if (clipboard == null || clipboard.isDisposed())
			return;
		clipboard.setContents(new Object[] { r.toString() },
				new Transfer[] { TextTransfer.getInstance() }, DND.CLIPBOARD);
	}

	void setInput(final RevFlag hFlag, final SWTCommitList list,
			final SWTCommit[] asArray, HistoryPageInput input, boolean keepPosition) {
		int topIndex = -1;
		if (keepPosition) {
			topIndex = table.getTable().getTopIndex();
		}
		setHistoryPageInput(input);
		final SWTCommitList oldList = allCommits;
		if (oldList != null && oldList != list) {
			oldList.dispose();
		}
		highlight = hFlag;
		allCommits = list;
		int newAllCommitsLength = asArray == null ? 0 : asArray.length;
		table.setInput(asArray);
		if (newAllCommitsLength > 0) {
			if (oldList != list || allCommitsLength < newAllCommitsLength) {
				initCommitsMap(asArray);
			}
		} else {
			table.getTable().deselectAll();
			// Fire an event
			table.setSelection(table.getSelection());
		}
		allCommitsLength = newAllCommitsLength;
		if (commitToShow != null) {
			selectCommit(commitToShow);
		}
		if (keepPosition) {
			table.getTable().setTopIndex(topIndex);
		}
	}

	void setHistoryPageInput(HistoryPageInput input) {
		this.input = input;
	}

	private void initCommitsMap(SWTCommit[] asArray) {
		commitsMap = new HashMap<>();
		for (SWTCommit commit : asArray) {
			if (commit != null) {
				commitsMap.put(commit.getId().name(), commit);
			}
		}
	}

	private void createColumns(Table rawTable,
			TableColumnLayout layout) {
		final TableColumn commitId = new TableColumn(rawTable, SWT.NONE);
		commitId.setResizable(true);
		commitId.setText(UIText.CommitGraphTable_CommitId);
		int minWidth;
		GC gc = new GC(rawTable.getDisplay());
		try {
			gc.setFont(rawTable.getFont());
			minWidth = gc.stringExtent("0000000").x + 5; //$NON-NLS-1$
		} finally {
			gc.dispose();
		}
		layout.setColumnData(commitId, new ColumnPixelData(minWidth, false));

		final TableColumn graph = new TableColumn(rawTable, SWT.NONE);
		graph.setResizable(true);
		graph.setText(UIText.CommitGraphTable_messageColumn);
		graph.setWidth(400);
		layout.setColumnData(graph, new ColumnWeightData(20, 200, true));

		final TableColumn author = new TableColumn(rawTable, SWT.NONE);
		author.setResizable(true);
		author.setText(UIText.HistoryPage_authorColumn);
		author.setWidth(100);
		layout.setColumnData(author, new ColumnWeightData(5, 80, true));

		final TableColumn date = new TableColumn(rawTable, SWT.NONE);
		date.setResizable(true);
		date.setText(UIText.HistoryPage_authorDateColumn);
		date.setWidth(100);
		layout.setColumnData(date, new ColumnWeightData(5, 80, true));

		final TableColumn committer = new TableColumn(rawTable, SWT.NONE);
		committer.setResizable(true);
		committer.setText(UIText.CommitGraphTable_Committer);
		committer.setWidth(100);
		layout.setColumnData(committer, new ColumnWeightData(5, 80, true));

		final TableColumn committerDate = new TableColumn(rawTable, SWT.NONE);
		committerDate.setResizable(true);
		committerDate.setText(UIText.CommitGraphTable_committerDataColumn);
		committerDate.setWidth(100);
		layout.setColumnData(committerDate, new ColumnWeightData(5, 80, true));
	}

	private void createPaintListener(final Table rawTable) {
		// Tell SWT we will completely handle painting for some columns.
		//
		rawTable.addListener(SWT.EraseItem, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				if (0 <= event.index && event.index <= 5)
					event.detail &= ~SWT.FOREGROUND;
			}
		});

		rawTable.addListener(SWT.PaintItem, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				doPaint(event);
			}
		});
	}

	void doPaint(final Event event) {
		// enable antialiasing early to avoid different font extent in
		// PlotRenderer
		if (this.enableAntialias)
			try {
				event.gc.setAntialias(SWT.ON);
			} catch (SWTException e) {
				this.enableAntialias = false;
			}

		final RevCommit c = (RevCommit) ((TableItem) event.item).getData();
		if (c instanceof SWTCommit) {
			final SWTLane lane = ((SWTCommit) c).getLane();
			if (lane != null && lane.color.isDisposed())
				return;
		}
		if (highlight != null && c != null && c.has(highlight)) {
			event.gc.setFont(hFont);
		} else {
			event.gc.setFont(nFont);
		}

		if (event.index == 1) {
			renderer.paint(event, input == null ? null : input.getHead());
			return;
		}

		final ITableLabelProvider lbl;
		final String txt;

		lbl = (ITableLabelProvider) table.getLabelProvider();
		txt = lbl.getColumnText(c, event.index);

		final Point textsz = event.gc.textExtent(txt);
		final int texty = (event.height - textsz.y) / 2;
		event.gc.drawString(txt, event.x, event.y + texty, true);
	}

	/**
	 * Returns the SWT TableView of this CommitGraphTable.
	 *
	 * @return Table the SWT Table
	 */
	public TableViewer getTableView() {
		return table;
	}

	private void layout(Table rawTable, TableColumnLayout layout) {
		rawTable.getParent().layout();
		// Check that the table now fits
		int tableWidth = rawTable.getSize().x;
		int columnsWidth = 0;
		TableColumn[] columns = rawTable.getColumns();
		for (TableColumn col : columns) {
			columnsWidth += col.getWidth();
		}
		if (columnsWidth > tableWidth) {
			// Re-distribute the space again
			layout.setColumnData(columns[1],
					new ColumnWeightData(20, 200, true));
			for (int i = 2; i < columns.length; i++) {
				layout.setColumnData(columns[i],
						new ColumnWeightData(5, 80, true));
			}
			rawTable.getParent().layout();
		}
		// Sometimes observed cheese on Cocoa without this
		rawTable.redraw();
	}

	private final class CommitDragSourceListener extends DragSourceAdapter {
		@Override
		public void dragStart(DragSourceEvent event) {
			RevCommit commit = getSelectedCommit();
			event.doit = commit.getParentCount() == 1;
		}

		@Override
		public void dragSetData(DragSourceEvent event) {
			boolean isFileTransfer = FileTransfer.getInstance()
					.isSupportedType(event.dataType);
			boolean isTextTransfer = TextTransfer.getInstance()
					.isSupportedType(event.dataType);
			if (isFileTransfer || isTextTransfer) {
				RevCommit commit = getSelectedCommit();
				String patchContent = createPatch(commit);
				if (isTextTransfer) {
					event.data = patchContent;
					return;
				} else {
					File patchFile = null;
					try {
						patchFile = createTempFile(commit);
						writeToFile(patchFile.getAbsolutePath(), patchContent);
						event.data = new String[] { patchFile.getAbsolutePath() };
					} catch (IOException e) {
						Activator.logError(NLS.bind(
								UIText.CommitGraphTable_UnableToWritePatch,
								commit.getId().name()), e);
					} finally {
						if (patchFile != null)
							patchFile.deleteOnExit();
					}
				}
			}
		}

		private File createTempFile(RevCommit commit) throws IOException {
			String tmpDir = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
			String patchName = "egit-patch" + commit.getId().name(); //$NON-NLS-1$
			File patchDir = new File(tmpDir, patchName);
			int counter = 1;
			while(patchDir.exists()) {
				patchDir = new File(tmpDir, patchName + "_" + counter); //$NON-NLS-1$
				counter++;
			}
			FileUtils.mkdir(patchDir);
			patchDir.deleteOnExit();
			File patchFile;
			String suggestedFileName = CreatePatchOperation
					.suggestFileName(commit);
			patchFile = new File(patchDir, suggestedFileName);
			return patchFile;
		}

		private String createPatch(RevCommit commit) {
			Repository repository = input.getRepository();
			CreatePatchOperation operation = new CreatePatchOperation(
					repository, commit);
			operation.setHeaderFormat(DiffHeaderFormat.EMAIL);
			operation.setContextLines(CreatePatchOperation.DEFAULT_CONTEXT_LINES);
			try {
				operation.execute(null);
			} catch (CoreException e) {
				Activator.logError(NLS.bind(
						UIText.CommitGraphTable_UnableToCreatePatch, commit
								.getId().name()), e);
			}
			String patchContent = operation.getPatchContent();
			return patchContent;
		}

		private RevCommit getSelectedCommit() {
			IStructuredSelection selection = (IStructuredSelection) table
					.getSelection();
			RevCommit commit = (RevCommit) selection.getFirstElement();
			try (RevWalk walk = new org.eclipse.jgit.revwalk.RevWalk(
					input.getRepository())) {
				return walk.parseCommit(commit.getId());
			} catch (IOException e) {
				throw new RuntimeException(
						"Could not parse commit " + commit.getId(), e); //$NON-NLS-1$
			}
		}

		private void writeToFile(final String fileName, String content)
				throws IOException {
			Writer output = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(fileName), RawParseUtils.UTF8_CHARSET));
			try {
				output.write(content);
			} finally {
				output.close();
			}
		}
	}

}
