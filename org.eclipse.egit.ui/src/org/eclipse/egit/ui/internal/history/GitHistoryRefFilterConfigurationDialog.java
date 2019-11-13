/*******************************************************************************
 * Copyright (C) 2019, Tim Neumann <Tim.Neumann@advantest.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.util.List;
import java.util.Set;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.AbstractBranchSelectionDialog;
import org.eclipse.egit.ui.internal.history.RefFilterHelper.RefFilter;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ViewSettingsDialog;

/**
 * The dialog for creating ref filters for the git history view.
 */
public class GitHistoryRefFilterConfigurationDialog
		extends ViewSettingsDialog {

	private static final String FILTER_COLUMN_NAME = "filter"; //$NON-NLS-1$

	private static final String NEW_FILTER_INITIAL_STRING = "refs/*"; //$NON-NLS-1$

	private final Repository repo;

	private final RefFilterHelper helper;

	private Set<RefFilter> filters;

	private CheckboxTableViewer configsTable;
	private Button removeButton;
	private Button editButton;
	private TextCellEditor editor;

	private volatile boolean editingMode = false;

	private boolean defaultsPerformed = false;

	private Point minimumSize;

	/**
	 * Create a new instance of the receiver.
	 *
	 * @param parentShell
	 * @param repo
	 * @param helper
	 */
	public GitHistoryRefFilterConfigurationDialog(Shell parentShell,
			Repository repo, RefFilterHelper helper) {
		super(parentShell);
		this.repo = repo;
		this.helper = helper;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.GitHistoryPage_filterRefDialog_dialogTitle);
		newShell.addShellListener(new ShellAdapter() {

			@Override
			public void shellActivated(ShellEvent e) {
				// Prevent making the dialog too small
				newShell.removeShellListener(this); // Only the first time
				if (minimumSize != null) {
					newShell.setMinimumSize(minimumSize);
				}
			}
		});
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = createUi(parent);
		init();
		return container;
	}

	private Composite createUi(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());

		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setFont(parent.getFont());

		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setBackground(container.getBackground());

		Group filtersComposite = new Group(composite, SWT.NONE);
		filtersComposite
				.setText(
						UIText.GitHistoryPage_filterRefDialog_filtersCompositLabel);

		filtersComposite.setLayout(new GridLayout(2, false));
		filtersComposite
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		filtersComposite.setBackground(composite.getBackground());

		fillFiltersComposite(filtersComposite);

		Composite actionsComposite = new Composite(composite, SWT.NONE);

		actionsComposite.setLayout(new GridLayout());
		actionsComposite
				.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		actionsComposite.setBackground(composite.getBackground());

		createActionCompositeButtons(actionsComposite);

		composite.pack();
		minimumSize = composite.getSize();

		// Line above OK and cancel buttons
		Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator
				.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return container;
	}

	private void fillFiltersComposite(Group filtersComposite) {
		Composite tableComposite = new Composite(filtersComposite, SWT.NONE);
		tableComposite.setLayout(new GridLayout());
		tableComposite
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableComposite.setBackground(filtersComposite.getBackground());

		createTable(tableComposite);

		tableComposite.pack();

		createFilterCompositeButtons(filtersComposite);

		Label patternExplanation = new Label(filtersComposite, SWT.WRAP);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = tableComposite.getSize().x;
		patternExplanation.setLayoutData(data);
		patternExplanation.setText(
				UIText.GitHistoryPage_filterRefDialog_patternExplanation);
	}

	private void createTable(Composite parent) {
		configsTable = CheckboxTableViewer
				.newCheckList(parent,
						SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tableData.widthHint = convertHorizontalDLUsToPixels(120);
		configsTable.getControl().setLayoutData(tableData);

		configsTable.setContentProvider(ArrayContentProvider.getInstance());

		configsTable.setLabelProvider(new RefLableProvider());

		configsTable.setComparator(new ViewerComparator() {
			@Override
			public int category(Object element) {
				RefFilter filter = ((RefFilter) element);
				if (filter.isPreconfigured()) {
					return 100;
				}
				return 1000;
			}
		});

		configsTable.addSelectionChangedListener(event -> {
			updateButtonEnablement();
		});

		configsTable.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				((RefFilter) event.getElement())
						.setSelected(event.getChecked());
			}
		});

		configsTable.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isGrayed(Object element) {
				return false;
			}

			@Override
			public boolean isChecked(Object element) {
				return ((RefFilter) element).isSelected();
			}
		});

		configsTable.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editCurrentRow();
			}
		});

		editor = new TextCellEditor(configsTable.getTable()) {
			@Override
			public void deactivate() {
				super.deactivate();
				if (editingMode) {
					editingMode = false;
					updateButtonEnablement();
				}
			}
		};
		CellEditor[] editors = new CellEditor[1];
		editors[0] = editor;
		configsTable.setColumnProperties(new String[] { FILTER_COLUMN_NAME });
		configsTable.setCellEditors(editors);
		configsTable.setCellModifier(new ICellModifier() {

			@Override
			public void modify(Object element, String property, Object value) {
				RefFilter filter = (RefFilter) ((TableItem) element).getData();
				// Remove the filter before changing the value; its hashCode
				// depends on it!
				filters.remove(filter);
				filter.setFilterString((String) value);
				filters.add(filter);
				configsTable.refresh();
				configsTable.reveal(filter);
			}

			@Override
			public Object getValue(Object element, String property) {
				RefFilter filter = (RefFilter) element;
				return filter.getFilterString();
			}

			@Override
			public boolean canModify(Object element, String property) {
				if (!editingMode) {
					return false;
				}
				RefFilter filter = (RefFilter) element;
				return !filter.isPreconfigured();
			}
		});
	}

	private void createFilterCompositeButtons(Composite parent) {
		Composite buttonComposite = new Composite(parent, SWT.NONE);
		buttonComposite.setLayout(new GridLayout());
		buttonComposite.setLayoutData(
				new GridData(SWT.CENTER, SWT.BEGINNING, false, false));

		Button addNew = new Button(buttonComposite, SWT.PUSH);
		addNew.setText(UIText.GitHistoryPage_filterRefDialog_button_add);
		addNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RefFilter newFilter = helper.new RefFilter(
						NEW_FILTER_INITIAL_STRING);
				filters.add(newFilter);
				configsTable.refresh();
				editFilter(newFilter);
			}
		});
		setButtonLayoutData(addNew);

		Button addRefButton = new Button(buttonComposite, SWT.PUSH);
		addRefButton
				.setText(UIText.GitHistoryPage_filterRefDialog_button_addRef);
		addRefButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateButtonEnablement();
				RefSelectionDialog dialog = new RefSelectionDialog(getShell(),
						repo);
				if (dialog.open() == Window.OK) {
					RefFilter newFilter = helper.new RefFilter(
							dialog.getRefName());
					filters.add(newFilter);
					configsTable.refresh();
					configsTable.setSelection(
							new StructuredSelection(newFilter), true);
				}
			}
		});
		setButtonLayoutData(addRefButton);

		removeButton = new Button(buttonComposite, SWT.PUSH);
		removeButton
				.setText(UIText.GitHistoryPage_filterRefDialog_button_remove);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeSelectedFilters();
			}
		});
		removeButton.setEnabled(false);
		setButtonLayoutData(removeButton);

		editButton = new Button(buttonComposite, SWT.PUSH);
		editButton.setText(UIText.GitHistoryPage_filterRefDialog_button_edit);
		editButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editCurrentRow();
				updateButtonEnablement();
			}
		});
		editButton.setEnabled(false);
		setButtonLayoutData(editButton);
	}

	private void createActionCompositeButtons(Composite parent) {
		Composite buttonComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(
				new GridData(SWT.CENTER, SWT.BEGINNING, false, false));

		Button setHeadOnly = new Button(buttonComposite, SWT.PUSH);
		setHeadOnly.setText(
				UIText.GitHistoryPage_filterRefDialog_button_headOnly);
		setHeadOnly.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				helper.selectOnlyHEAD(filters);
				configsTable.refresh();
			}
		});
		setButtonLayoutData(setHeadOnly);

		Button setCurrentBranchOnly = new Button(buttonComposite, SWT.PUSH);
		setCurrentBranchOnly
				.setText(
						UIText.GitHistoryPage_filterRefDialog_button_currentBranchOnly);
		setCurrentBranchOnly.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				helper.selectOnlyCurrentBranch(filters);
				configsTable.refresh();
			}
		});
		setButtonLayoutData(setCurrentBranchOnly);

		Button setAllBranchesAndTags = new Button(buttonComposite, SWT.PUSH);
		setAllBranchesAndTags.setText(
				UIText.GitHistoryPage_filterRefDialog_button_allBranchesAndTags);
		setAllBranchesAndTags.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				helper.selectExactlyAllBranchesAndTags(filters);
				configsTable.refresh();
			}
		});
		setButtonLayoutData(setAllBranchesAndTags);
	}

	@Override
	protected void okPressed() {
		helper.setRefFilters(filters);
		if (defaultsPerformed) {
			helper.resetLastSelectionStateToDefault();
		}
		super.okPressed();
	}

	@Override
	protected void performDefaults() {
		filters = helper.getDefaults();
		configsTable.setInput(filters);
		defaultsPerformed = true;
		super.performDefaults();
	}

	private void init() {
		filters = helper.getRefFilters();
		if (helper.isOnlyHEADSelected(filters)) {
			helper.restoreLastSelectionState(filters);
		}
		configsTable.setInput(filters);
		configsTable.refresh();
	}

	private void editCurrentRow() {
		editFilter((RefFilter) configsTable.getStructuredSelection()
				.getFirstElement());
	}

	private void editFilter(RefFilter filter) {
		editingMode = true;
		configsTable.editElement(filter, 0);
		updateButtonEnablement();
	}

	private void removeSelectedFilters() {
		IStructuredSelection selection = configsTable.getStructuredSelection();
		if (selection.isEmpty()) {
			return;
		}
		filters.removeAll(selection.toList());
		configsTable.refresh();
	}

	private void updateButtonEnablement() {
		IStructuredSelection selection = configsTable.getStructuredSelection();
		boolean allWriteable = false;
		if (!selection.isEmpty()) {
			List<?> elements = selection.toList();
			allWriteable = elements.stream()
					.allMatch(x -> x instanceof RefFilter
							&& !((RefFilter) x).isPreconfigured());
		}
		removeButton.setEnabled(allWriteable);
		editButton.setEnabled(selection.size() == 1
				&& !((RefFilter) selection.getFirstElement())
						.isPreconfigured());
	}

	private static class RefLableProvider extends LabelProvider
			implements IColorProvider {

		@Override
		public String getText(Object element) {
			RefFilter filter = ((RefFilter) element);
			String result = filter.getFilterString();
			if (filter.isPreconfigured()) {
				result += UIText.GitHistoryPage_filterRefDialog_preconfiguredText;
			}
			return result;
		}

		@Override
		public Color getForeground(Object element) {
			RefFilter filter = ((RefFilter) element);
			if (filter.isPreconfigured()) {
				return PlatformUI.getWorkbench().getDisplay()
						.getSystemColor(SWT.COLOR_DARK_GRAY);
			}
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

	}

	private static class RefSelectionDialog
			extends AbstractBranchSelectionDialog {

		public RefSelectionDialog(Shell parentShell, Repository repository) {
			// Using empty string instead of null to select nothing instead of
			// the current branch.
			super(parentShell, repository, "", SHOW_LOCAL_BRANCHES //$NON-NLS-1$
					| SHOW_REMOTE_BRANCHES | SHOW_TAGS | SHOW_REFERENCES
					| SELECT_CURRENT_REF | EXPAND_LOCAL_BRANCHES_NODE
					| EXPAND_REMOTE_BRANCHES_NODE);
		}

		@Override
		protected void refNameSelected(String refName) {
			setOkButtonEnabled(refName != null);
		}

		@Override
		protected String getTitle() {
			return UIText.GitHistoryPage_filterRefDialog_selectRefDialog_dialogTitle;
		}

		@Override
		protected String getMessageText() {
			return UIText.GitHistoryPage_filterRefDialog_selectRefDialog_dialogMessage;
		}
	}
}
