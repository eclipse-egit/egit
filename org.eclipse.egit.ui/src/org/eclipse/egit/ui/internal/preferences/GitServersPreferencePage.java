/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.internal.hosts.GitHosts;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.fetch.GitServer;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * A preference page where a user can define extra {@link Pattern}s for matching
 * git server host names.
 */
public class GitServersPreferencePage extends PreferencePage
		implements IWorkbenchPreferencePage {

	private List<HostSpec> hosts = new ArrayList<>();

	private TableViewer table;

	/**
	 * Creates a new {@link GitServersPreferencePage}.
	 */
	public GitServersPreferencePage() {
		setDescription(UIText.GitServersPreferencePage_Description);
		noDefaultButton();
	}

	@Override
	public void init(IWorkbench workbench) {
		// Nothing to do
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, true).applyTo(main);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false)
				.applyTo(main);

		Composite tableContainer = new Composite(main, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, true).applyTo(tableContainer);
		TableColumnLayout layout = new TableColumnLayout();
		tableContainer.setLayout(layout);

		table = new TableViewer(tableContainer,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);

		Composite buttonContainer = new Composite(main, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BEGINNING)
				.grab(false, true).applyTo(buttonContainer);
		GridLayoutFactory.fillDefaults().applyTo(buttonContainer);

		Button add = new Button(buttonContainer, SWT.PUSH);
		add.setText(UIText.GitServersPreferencePage_AddLabel);
		add.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				addNew();
			}
		});
		setButtonLayoutData(add);

		Button remove = new Button(buttonContainer, SWT.PUSH);
		remove.setText(UIText.GitServersPreferencePage_RemoveLabel);
		remove.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				removeSelected();
			}
		});
		setButtonLayoutData(remove);

		table.addSelectionChangedListener(event -> {
			remove.setEnabled(!table.getSelection().isEmpty());
		});
		table.getTable().addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent event) {
				if (event.keyCode == SWT.DEL && remove.isEnabled()) {
					removeSelected();
				}
			}
		});

		table.setContentProvider(ArrayContentProvider.getInstance());
		TableViewerColumn serverColumn = new TableViewerColumn(table, SWT.NONE);
		serverColumn.getColumn().setMoveable(false);
		serverColumn.getColumn().setResizable(false);
		serverColumn.getColumn().setText(UIText.GitServersPreferencePage_ServerTitle);
		serverColumn.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof HostSpec) {
					GitServer server = ((HostSpec) element).serverType;
					return server == null ? UIText.GitServersPreferencePage_ServerUndefined : server.getName();
				}
				return null;
			}

		});
		serverColumn.setEditingSupport(new ServerEditingSupport(table));
		TableViewerColumn patternColumn = new TableViewerColumn(table,
				SWT.NONE);
		patternColumn.getColumn().setMoveable(false);
		patternColumn.getColumn().setResizable(false);
		patternColumn.getColumn().setText(UIText.GitServersPreferencePage_PatternTitle);
		patternColumn.getColumn()
				.setToolTipText(UIText.GitServersPreferencePage_PatternTooltip);
		patternColumn.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof HostSpec) {
					return ((HostSpec) element).hostPattern;
				}
				return null;
			}

		});
		patternColumn.setEditingSupport(
				new PatternEditingSupport(table, msg -> {
					setErrorMessage(msg);
					setValid(msg == null);
				}));
		table.getTable().setHeaderVisible(true);
		table.getTable().setLinesVisible(true);

		// Load and set the input
		loadHosts();

		table.setInput(hosts);
		serverColumn.getColumn().pack();
		patternColumn.getColumn().pack();
		// Make the pattern column use all available space.
		layout.setColumnData(patternColumn.getColumn(), new ColumnWeightData(
				100, patternColumn.getColumn().getWidth(), false));
		// Give the server column a little more minimum width for the
		// ComboBoxCellEditor's drop-down arrow widget.
		layout.setColumnData(serverColumn.getColumn(), new ColumnWeightData(0,
				serverColumn.getColumn().getWidth() + 20, false));
		return main;
	}

	private void removeSelected() {
		table.applyEditorValue();
		hosts.removeAll(table.getStructuredSelection().toList());
		table.refresh();
	}

	private void addNew() {
		table.applyEditorValue();
		hosts.add(new HostSpec());
		table.refresh();
	}

	private void loadHosts() {
		String rawData = getPreferenceStore()
				.getString(GitCorePreferences.core_gitServers);
		GitHosts.loadFromPreferences(rawData, (s, p) -> {
			hosts.add(new HostSpec(GitServer.valueOf(s), p));
		});
	}

	private void saveHosts() {
		StringBuilder builder = new StringBuilder();
		for (HostSpec host : hosts) {
			if (host.serverType == null
					|| StringUtils.isEmptyOrNull(host.hostPattern)) {
				continue;
			}
			builder.append(host.serverType.name())
					.append('\t')
					.append(host.hostPattern)
					.append('\n');
		}
		getPreferenceStore().putValue(GitCorePreferences.core_gitServers,
				builder.toString());
	}

	@Override
	public IPreferenceStore doGetPreferenceStore() {
		return new ScopedPreferenceStore(InstanceScope.INSTANCE,
				org.eclipse.egit.core.Activator.PLUGIN_ID);
	}

	@Override
	public boolean performOk() {
		table.applyEditorValue();
		saveHosts();
		return super.performOk();
	}

	/**
	 * Provides a {@link ComboBoxCellEditor} for selecting the {@link GitServer}
	 * server kind.
	 */
	private static class ServerEditingSupport extends EditingSupport {

		private final Map<String, GitServer> servers = new HashMap<>();

		private final String[] items;

		private final ComboBoxCellEditor editor;

		ServerEditingSupport(TableViewer viewer) {
			super(viewer);
			for (GitServer s : GitServer.values()) {
				servers.put(s.getName(), s);
			}
			items = servers.keySet().stream().sorted()
					.collect(Collectors.toList()).toArray(new String[0]);
			editor = new ComboBoxCellEditor(viewer.getTable(), items,
					SWT.READ_ONLY);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return editor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			if (((HostSpec) element).serverType == null) {
				return Integer.valueOf(0);
			}
			String name = ((HostSpec) element).serverType.getName();
			for (int i = 0; i < items.length; i++) {
				if (name.equals(items[i])) {
					return Integer.valueOf(i);
				}
			}
			return null;
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (value instanceof Integer) {
				int index = ((Integer) value).intValue();
				if (index >= 0) {
					((HostSpec) element).serverType = servers.get(items[index]);
					getViewer().update(element, null);
				}
			}

		}
	}

	/**
	 * Provides a {@link TextCellEditor} with validation and content assist for
	 * entering a regular expression pattern.
	 */
	private static class PatternEditingSupport extends EditingSupport {

		private final TextCellEditor editor;

		public PatternEditingSupport(TableViewer viewer,
				Consumer<String> reportError) {
			super(viewer);
			editor = new TextCellEditor(viewer.getTable()) {

				private boolean assisting = false;

				@Override
				protected Control createControl(Composite parent) {
					Control control = super.createControl(parent);
					// Prevent pasting multi-line text into a single-line
					// control. See bug 273470.
					text.addVerifyListener(
							event -> event.text = Utils.firstLine(event.text));
					addListener(new ICellEditorListener() {

						@Override
						public void editorValueChanged(boolean oldValidState,
								boolean newValidState) {
							reportError.accept(
									newValidState ? null : getErrorMessage());
						}

						@Override
						public void cancelEditor() {
							reportError.accept(null);
						}

						@Override
						public void applyEditorValue() {
							reportError.accept(null);
						}
					});
					// Add content assist for entering regular expressions.
					TextContentAdapter contentAdapter = new TextContentAdapter();
					FindReplaceDocumentAdapterContentProposalProvider proposalProvider =
							new FindReplaceDocumentAdapterContentProposalProvider(true);
					ContentAssistCommandAdapter proposer = new ContentAssistCommandAdapter(
							text, contentAdapter, proposalProvider,
							IWorkbenchCommandConstants.EDIT_CONTENT_ASSIST,
							new char[0], true);
					// Track the content assist pop-up: the editor must not
					// deactivate when the pop-up gets the focus.
					proposer.addContentProposalListener(
							new IContentProposalListener2() {

								@Override
								public void proposalPopupClosed(
										ContentProposalAdapter adapter) {
									assisting = false;
								}

								@Override
								public void proposalPopupOpened(
										ContentProposalAdapter adapter) {
									assisting = true;
								}
							});
					proposer.setEnabled(true);
					return control;
				}

				@Override
				protected void focusLost() {
					if (!assisting) {
						// Don't deactivate if content assist popup gets the
						// focus.
						super.focusLost();
					}
				}

				@Override
				public void deactivate() {
					super.deactivate();
					reportError.accept(null);
				}

				@Override
				protected boolean dependsOnExternalFocusListener() {
					return false;
				}
			};
			editor.setValidator(object -> {
				try {
					Pattern.compile(object.toString());
				} catch (PatternSyntaxException e) {
					return e.getLocalizedMessage();
				}
				return ""; //$NON-NLS-1$
			});
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return editor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			return ((HostSpec) element).hostPattern;
		}

		@Override
		protected void setValue(Object element, Object value) {
			((HostSpec) element).hostPattern = value.toString();
			getViewer().update(element, null);
		}
	}

	private static class HostSpec {

		GitServer serverType;

		String hostPattern;

		HostSpec() {
			this(null, ""); //$NON-NLS-1$
		}

		HostSpec(GitServer server, String pattern) {
			serverType = server;
			hostPattern = pattern;
		}

		@Override
		public String toString() {
			return "<" + serverType + ' ' + hostPattern + '>'; //$NON-NLS-1$
		}
	}
}
