/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.CreatePatchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.history.command.HistoryViewCommands;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
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

	private final Color infoBackgroundColor;

	private SWTCommitList allCommits;

	// used for resolving PlotCommit objects by ids
	private HashMap<String, PlotCommit> commitsMap = null;

	private RevFlag highlight;

	private HistoryPageInput input;

	IAction copy;

	private Shell hoverShell;

	MenuListener menuListener;

	private RevCommit commitToShow;

	private GraphLabelProvider graphLabelProvider;

	CommitGraphTable(Composite parent) {
		nFont = UIUtils.getFont(UIPreferences.THEME_CommitGraphNormalFont);
		hFont = highlightFont();
		infoBackgroundColor = parent.getDisplay().getSystemColor(
				SWT.COLOR_INFO_BACKGROUND);

		Table rawTable = new Table(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
		rawTable.setHeaderVisible(true);
		rawTable.setLinesVisible(false);
		rawTable.setFont(nFont);

		final TableLayout layout = new TableLayout();
		rawTable.setLayout(layout);

		createColumns(rawTable, layout);
		createPaintListener(rawTable);

		table = new TableViewer(rawTable) {
			protected Widget doFindItem(final Object element) {
				return element != null ? ((SWTCommit) element).widget : null;
			}

			protected void mapElement(final Object element, final Widget item) {
				((SWTCommit) element).widget = item;
			}
		};

		graphLabelProvider = new GraphLabelProvider();

		table.setLabelProvider(graphLabelProvider);
		table.setContentProvider(new GraphContentProvider());
		renderer = new SWTPlotRenderer(rawTable.getDisplay());

		clipboard = new Clipboard(rawTable.getDisplay());
		rawTable.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				clipboard.dispose();
			}
		});

		copy = createStandardAction(ActionFactory.COPY);

		table.setUseHashlookup(true);

		table.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection s = event.getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection))
					return;
				final IStructuredSelection iss = (IStructuredSelection) s;
				commitToShow = (PlotCommit<?>) iss.getFirstElement();

				copy.setEnabled(canDoCopy());
			}
		});

		table.getTable().addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseHover(MouseEvent e) {
				synchronized (this) {
					if (hoverShell != null) {
						hoverShell.setVisible(false);
						hoverShell.dispose();
						hoverShell = null;
					}

					TableItem item = table.getTable().getItem(
							new Point(e.x, e.y));
					if (item == null)
						return;
					SWTCommit commit = (SWTCommit) item.getData();
					if (commit == null || commit.getRefCount() == 0)
						return;

					int relativeX = e.x - item.getBounds().x;
					for (int i = 0; i < commit.getRefCount(); i++) {
						Point textSpan = renderer.getRefHSpan(commit.getRef(i));
						if ((relativeX >= textSpan.x && relativeX <= textSpan.y)) {
							hoverShell = new Shell(getTableView().getTable()
									.getShell(), SWT.ON_TOP | SWT.NO_FOCUS
									| SWT.TOOL);
							hoverShell.setLayout(new FillLayout());
							Point tableLocation = getTableView().getTable()
									.toControl(0, 0);
							hoverShell.setLocation(
									-tableLocation.x + e.x,
									-tableLocation.y + e.y
											- renderer.getTextHeight());
							Label label = new Label(hoverShell, SWT.NONE);
							label.setText(getHooverText(commit.getRef(i)));
							label.setBackground(infoBackgroundColor);
							hoverShell.pack();
							hoverShell.setVisible(true);
						}
					}
				}
			}

			private String getHooverText(Ref r) {
				String name = r.getName();
				if (r.isSymbolic())
					name += ": " + r.getLeaf().getName(); //$NON-NLS-1$
				return name;
			}

		});

		table.getTable().addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				synchronized (this) {
					if (hoverShell == null || hoverShell.isDisposed())
						return;
					hoverShell.setVisible(false);
					hoverShell.dispose();
					hoverShell = null;
				}
			}
		});

		table.getTable().addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				if (allCommits != null)
					allCommits.dispose();
				if (renderer != null)
					renderer.dispose();
			}
		});

		Transfer[] transferTypes = new Transfer[] {TextTransfer.getInstance(), FileTransfer.getInstance()};
		table.addDragSupport(DND.DROP_DEFAULT | DND.DROP_COPY, transferTypes,
				new CommitDragSourceListener());
	}

	CommitGraphTable(final Composite parent, final IPageSite site,
			final MenuManager menuMgr) {
		this(parent);

		final IAction selectAll = createStandardAction(ActionFactory.SELECT_ALL);
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

		getTableView().addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				if (input == null || !input.isSingleFile()) {
					return;
				}

				ICommandService srv = (ICommandService) site
						.getService(ICommandService.class);
				IHandlerService hsrv = (IHandlerService) site
						.getService(IHandlerService.class);
				Command cmd = srv.getCommand(HistoryViewCommands.SHOWVERSIONS);
				Parameterization[] parms;
				if (Activator.getDefault().getPreferenceStore().getBoolean(
						UIPreferences.RESOURCEHISTORY_COMPARE_MODE)) {
					try {
						IParameter parm = cmd
								.getParameter(HistoryViewCommands.COMPARE_MODE_PARAM);
						parms = new Parameterization[] { new Parameterization(
								parm, Boolean.TRUE.toString()) };
					} catch (NotDefinedException e) {
						Activator.handleError(e.getMessage(), e, true);
						parms = null;
					}
				} else
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

		Control c = getControl();
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		c.setMenu(menuMgr.createContextMenu(c));
		c.addMenuDetectListener(menuListener = new MenuListener(menuMgr,
				getTableView(), site, copy));
	}

	Control getControl() {
		return table.getControl();
	}

	void selectCommitStored(final RevCommit c) {
		commitToShow = c;
		selectCommit(c);
	}

	void selectCommit(final RevCommit c) {
		if (c instanceof PlotCommit) {
			table.setSelection(new StructuredSelection(c), true);
		} else if (commitsMap != null) {
			PlotCommit swtCommit = commitsMap.get(c.getId().name());
			if (swtCommit != null) {
				table.setSelection(new StructuredSelection(swtCommit), true);
			}
		}
	}

	void addSelectionChangedListener(final ISelectionChangedListener l) {
		table.addPostSelectionChangedListener(l);
	}

	void removeSelectionChangedListener(final ISelectionChangedListener l) {
		table.removePostSelectionChangedListener(l);
	}

	boolean setRelativeDate(boolean booleanValue) {
		return graphLabelProvider.setRelativeDate(booleanValue);
	}

	private boolean canDoCopy() {
		return !table.getSelection().isEmpty();
	}

	@SuppressWarnings("unchecked")
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
			final SWTCommit[] asArray, HistoryPageInput input) {
		setHistoryPageInput(input);
		final SWTCommitList oldList = allCommits;
		highlight = hFlag;
		allCommits = list;
		table.setInput(asArray);
		if (asArray != null && asArray.length > 0) {
			if (oldList != list) {
				initCommitsMap();
			}
		} else {
			table.getTable().deselectAll();
		}
		if (commitToShow != null)
			selectCommit(commitToShow);
	}

	void setHistoryPageInput(HistoryPageInput input) {
		this.input = input;
		if (menuListener != null)
			menuListener.setInput(input);
	}

	private void initCommitsMap() {
		commitsMap = new HashMap<String, PlotCommit>();
		// ensure that filling (GenerateHistoryJob) and reading (here)
		// the commit list is thread safe
		synchronized (allCommits) {
			for (PlotCommit commit : allCommits)
				if (commit != null)
					commitsMap.put(commit.getId().name(), commit);
		}
	}

	private void createColumns(final Table rawTable, final TableLayout layout) {
		final TableColumn graph = new TableColumn(rawTable, SWT.NONE);
		graph.setResizable(true);
		graph.setText(""); //$NON-NLS-1$
		graph.setWidth(250);
		layout.addColumnData(new ColumnWeightData(20, true));

		final TableColumn author = new TableColumn(rawTable, SWT.NONE);
		author.setResizable(true);
		author.setText(UIText.HistoryPage_authorColumn);
		author.setWidth(250);
		layout.addColumnData(new ColumnWeightData(10, true));

		final TableColumn date = new TableColumn(rawTable, SWT.NONE);
		date.setResizable(true);
		date.setText(UIText.HistoryPage_dateColumn);
		date.setWidth(250);
		layout.addColumnData(new ColumnWeightData(5, true));

		final TableColumn commitId = new TableColumn(rawTable, SWT.NONE);
		commitId.setResizable(true);
		commitId.setText(UIText.CommitGraphTable_CommitId);
		commitId.setWidth(50);
		layout.addColumnData(new ColumnWeightData(3, true));

		final TableColumn committer = new TableColumn(rawTable, SWT.NONE);
		committer.setResizable(true);
		committer.setText(UIText.CommitGraphTable_Committer);
		committer.setWidth(100);
		layout.addColumnData(new ColumnWeightData(5, true));
	}

	private void createPaintListener(final Table rawTable) {
		// Tell SWT we will completely handle painting for some columns.
		//
		rawTable.addListener(SWT.EraseItem, new Listener() {
			public void handleEvent(final Event event) {
				if (0 <= event.index && event.index <= 4)
					event.detail &= ~SWT.FOREGROUND;
			}
		});

		rawTable.addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(final Event event) {
				doPaint(event);
			}
		});
	}

	void doPaint(final Event event) {
		final RevCommit c = (RevCommit) ((TableItem) event.item).getData();
		if (highlight != null && c.has(highlight))
			event.gc.setFont(hFont);
		else
			event.gc.setFont(nFont);

		if (event.index == 0) {
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

	private IAction createStandardAction(final ActionFactory af) {
		final String text = af.create(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getText();
		IAction action = new Action() {

			@Override
			public String getActionDefinitionId() {
				return af.getCommandId();
			}

			@Override
			public String getId() {
				return af.getId();
			}

			@Override
			public String getText() {
				return text;
			}

			@Override
			public void run() {
				if (af == ActionFactory.SELECT_ALL) {
					table.getTable().selectAll();
				}
				if (af == ActionFactory.COPY) {
					doCopy();
				}
			}
		};
		return action;
	}

	private final class CommitDragSourceListener extends DragSourceAdapter {
		@Override
		public void dragStart(DragSourceEvent event) {
			RevCommit commit = getSelectedCommit();
			event.doit = commit.getParentCount() == 1;
		}

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
						if (patchFile != null) {
							patchFile.deleteOnExit();
						}
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
			operation.useGitFormat(true);
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
			return commit;
		}

		private void writeToFile(final String fileName, String content)
				throws IOException {
			Writer output = new BufferedWriter(new FileWriter(fileName));
			try {
				output.write(content);
			} finally {
				output.close();
			}
		}
	}

	private final static class MenuListener implements MenuDetectListener {

		private final MenuManager popupMgr;

		private final ISelectionProvider selectionProvider;

		private final IPageSite site;

		private final IAction copyAction;

		private HistoryPageInput input;

		MenuListener(MenuManager menuManager,
				ISelectionProvider selectionProvider, IPageSite site,
				IAction copyAction) {
			this.popupMgr = menuManager;
			this.selectionProvider = selectionProvider;
			this.site = site;
			this.copyAction = copyAction;
		}

		public void setInput(HistoryPageInput input) {
			this.input = input;
		}

		public void menuDetected(MenuDetectEvent e) {
			popupMgr.removeAll();

			int selectionSize = ((IStructuredSelection) selectionProvider
					.getSelection()).size();

			if (input.isSingleFile()) {
				if (selectionSize == 1)
					if (input.getSingleFile() instanceof IResource)
						popupMgr
								.add(getCommandContributionItem(
										HistoryViewCommands.COMPARE_WITH_TREE,
										UIText.GitHistoryPage_CompareWithWorkingTreeMenuMenuLabel));
					else
						popupMgr
								.add(getCommandContributionItem(
										HistoryViewCommands.COMPARE_WITH_TREE,
										UIText.GitHistoryPage_CompareWithCurrentHeadMenu));
				if (selectionSize > 0) {
					popupMgr.add(getCommandContributionItem(
							HistoryViewCommands.OPEN,
							UIText.GitHistoryPage_OpenMenuLabel));
					popupMgr.add(getCommandContributionItem(
							HistoryViewCommands.OPEN_IN_TEXT_EDITOR,
							UIText.GitHistoryPage_OpenInTextEditorLabel));
				}
			}

			if (selectionSize == 1) {
				popupMgr.add(new Separator());
				popupMgr.add(getCommandContributionItem(
						HistoryViewCommands.CHECKOUT,
						UIText.GitHistoryPage_CheckoutMenuLabel));
				popupMgr.add(getCommandContributionItem(
						HistoryViewCommands.CREATE_BRANCH,
						UIText.GitHistoryPage_CreateBranchMenuLabel));
				popupMgr.add(getCommandContributionItem(
						HistoryViewCommands.CREATE_TAG,
						UIText.GitHistoryPage_CreateTagMenuLabel));
				popupMgr.add(getCommandContributionItem(
						HistoryViewCommands.CREATE_PATCH,
						UIText.GitHistoryPage_CreatePatchMenuLabel));
				popupMgr.add(getCommandContributionItem(
						HistoryViewCommands.CHERRYPICK,
						UIText.GitHistoryPage_cherryPickMenuItem));
				popupMgr.add(getCommandContributionItem(
						HistoryViewCommands.REVERT,
						UIText.GitHistoryPage_revertMenuItem));
				popupMgr.add(getCommandContributionItem(
						HistoryViewCommands.MERGE,
						UIText.GitHistoryPage_mergeMenuItem));
				popupMgr.add(getCommandContributionItem(
						HistoryViewCommands.REBASECURRENT,
						UIText.GitHistoryPage_rebaseMenuItem));
				popupMgr.add(new Separator());

				MenuManager resetManager = new MenuManager(
						UIText.GitHistoryPage_ResetMenuLabel, UIIcons.RESET,
						"Reset"); //$NON-NLS-1$

				popupMgr.add(resetManager);

				Map<String, String> parameters = new HashMap<String, String>();
				parameters.put(HistoryViewCommands.RESET_MODE, "Soft"); //$NON-NLS-1$
				resetManager.add(getCommandContributionItem(
						HistoryViewCommands.RESET,
						UIText.GitHistoryPage_ResetSoftMenuLabel, parameters));
				parameters = new HashMap<String, String>();
				parameters.put(HistoryViewCommands.RESET_MODE, "Mixed"); //$NON-NLS-1$
				resetManager.add(getCommandContributionItem(
						HistoryViewCommands.RESET,
						UIText.GitHistoryPage_ResetMixedMenuLabel, parameters));
				parameters = new HashMap<String, String>();
				parameters.put(HistoryViewCommands.RESET_MODE, "Hard"); //$NON-NLS-1$
				resetManager.add(getCommandContributionItem(
						HistoryViewCommands.RESET,
						UIText.GitHistoryPage_ResetHardMenuLabel, parameters));
			} else if (selectionSize == 2) {
				popupMgr.add(getCommandContributionItem(
						HistoryViewCommands.COMPARE_VERSIONS,
						UIText.GitHistoryPage_CompareWithEachOtherMenuLabel));
				if (!input.isSingleFile())
					popupMgr
							.add(getCommandContributionItem(
									HistoryViewCommands.COMPARE_VERSIONS_IN_TREE,
									UIText.CommitGraphTable_CompareWithEachOtherInTreeMenuLabel));
			}
			popupMgr.add(new Separator());

			MenuManager quickDiffManager = new MenuManager(
					UIText.GitHistoryPage_QuickdiffMenuLabel, null, "Quickdiff"); //$NON-NLS-1$

			popupMgr.add(quickDiffManager);

			quickDiffManager.add(getCommandContributionItem(
					HistoryViewCommands.SET_QUICKDIFF_BASELINE,
					UIText.GitHistoryPage_SetAsBaselineMenuLabel));

			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(HistoryViewCommands.BASELINE_TARGET, "HEAD"); //$NON-NLS-1$
			quickDiffManager.add(getCommandContributionItem(
					HistoryViewCommands.RESET_QUICKDIFF_BASELINE,
					UIText.GitHistoryPage_ResetBaselineToHeadMenuLabel,
					parameters));

			parameters = new HashMap<String, String>();
			parameters.put(HistoryViewCommands.BASELINE_TARGET, "HEAD^1"); //$NON-NLS-1$
			quickDiffManager.add(getCommandContributionItem(
					HistoryViewCommands.RESET_QUICKDIFF_BASELINE,
					UIText.GitHistoryPage_ResetBaselineToParentOfHeadMenuLabel,
					parameters));

			// copy and such after additions
			popupMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			popupMgr.add(copyAction);
			popupMgr.add(getCommandContributionItem(
					HistoryViewCommands.OPEN_IN_COMMIT_VIEWER,
					UIText.CommitGraphTable_OpenCommitLabel));
			popupMgr.add(new Separator());
		}

		private CommandContributionItem getCommandContributionItem(
				String commandId, String menuLabel) {
			CommandContributionItemParameter parameter = new CommandContributionItemParameter(
					site, commandId, commandId,
					CommandContributionItem.STYLE_PUSH);
			parameter.label = menuLabel;
			return new CommandContributionItem(parameter);
		}

		private CommandContributionItem getCommandContributionItem(
				String commandId, String menuLabel,
				Map<String, String> parameters) {
			CommandContributionItemParameter parameter = new CommandContributionItemParameter(
					site, commandId, commandId,
					CommandContributionItem.STYLE_PUSH);
			parameter.label = menuLabel;
			parameter.parameters = parameters;
			return new CommandContributionItem(parameter);
		}
	}
}
