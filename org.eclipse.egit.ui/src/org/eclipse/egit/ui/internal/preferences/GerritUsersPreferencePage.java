/*******************************************************************************
 * Copyright (c) 2012 Kamil Sobon <kam.sobon@gmail.com>.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *    Kamil Sobon - initial implementation
 *******************************************************************************/

package org.eclipse.egit.ui.internal.preferences;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.IPersonProvider.Person;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.XMLMemento;
import org.w3c.dom.DOMException;

/**
 * Preference page for managing list of known gerrit's users.
 */
public class GerritUsersPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	/**
	 * Key under which list of users is stored in {@link IPreferenceStore}.
	 */
	public static final String USERS_PREFERENCE_KEY = "USERS_PREFERENCE"; //$NON-NLS-1$

	/** Label provider of 'full name' column */
	private final ColumnLabelProvider nameColumnLabelProvider = new ColumnLabelProvider() {
		public String getText(Object element) {
			String name = ((PersonEntry) element).getName();
			return name != null ? name : ""; //$NON-NLS-1$
		}
	};

	/** Label provider of 'login' column */
	private final ColumnLabelProvider loginColumnLabelProvider = new ColumnLabelProvider() {
		public String getText(Object element) {
			return ((PersonEntry) element).getLogin();
		}
	};

	/** Selection listener of 'add' button */
	private final SelectionListener addButtonSelectionListener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			PersonEntry person = new PersonEntry();
			people.add(person);
			setInput();

			userTableViewer.editElement(person, 0);
		}
	};

	/** Selection listener of 'remove' button */
	private final SelectionListener removeButtonSelectionListener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			people.remove(userTableViewer.getTable().getSelectionIndex());
			setInput();
		}
	};

	/**
	 * Table viewer's selection change listener that enables or disables remove
	 * button.
	 */
	private ISelectionChangedListener selectionChangeListener = new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {
			removeButton.setEnabled(userTableViewer.getTable()
					.getSelectionIndex() >= 0);
		}
	};

	/** List of people displayed in table viewer. */
	private List<PersonEntry> people;

	/** Table viewer that stores list of users. */
	private TableViewer userTableViewer;

	/** Button for removing users */
	private Button removeButton;

	/**
	 * The default constructor.
	 */
	public GerritUsersPreferencePage() {
		noDefaultAndApplyButton();

		people = new ArrayList<PersonEntry>();
		for (Person person : PreferenceStorePersonProvider.getInstance()
				.getPeople()) {
			people.add(new PersonEntry(person));
		}
	}

	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(UIText.GerritUsersPreferencePage_PageDescription);
	}

	@Override
	public boolean performOk() {
		// Serialize users to XML format using XMLMemento
		try {
			// Create root element
			XMLMemento memento = XMLMemento
					.createWriteRoot(PreferenceStorePersonProvider.ROOT_ELEMENT);

			// Iterate over list of users and process each one
			for (PersonEntry person : people) {
				IMemento personMemento = memento
						.createChild(PreferenceStorePersonProvider.USER_ELEMENT);
				personMemento.putString(
						PreferenceStorePersonProvider.USER_LOGIN_ELEMENT,
						person.getLogin());
				personMemento.putString(
						PreferenceStorePersonProvider.USER_NAME_ELEMENT,
						person.getName());
			}

			StringWriter writer = new StringWriter();
			memento.save(writer);

			String content = writer.toString();
			getPreferenceStore().setValue(USERS_PREFERENCE_KEY, content);
		} catch (DOMException e) {
			Activator.handleError(
					UIText.GerritUsersPreferencePage_SavingExceptionError, e,
					true);
			return false;
		} catch (IOException e) {
			Activator.handleError(
					UIText.GerritUsersPreferencePage_SavingExceptionError, e,
					true);
			return false;

		}

		return true;
	}

	@Override
	protected Control createContents(Composite parent) {
		// Create main composite
		Composite main = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0)
				.applyTo(main);

		// Create table viewer that displays users
		createUserTableViewer(main);

		// Create buttons
		createButtons(main);

		return main;
	}

	private void createUserTableViewer(Composite main) {
		userTableViewer = new TableViewer(main, SWT.BORDER | SWT.V_SCROLL
				| SWT.FULL_SELECTION);

		// Create strategy of cell editors activation
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(
				userTableViewer) {
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};

		// Configure viewer's editors behavior
		TableViewerEditor.create(userTableViewer, actSupport,
				ColumnViewerEditor.TABBING_HORIZONTAL
						| ColumnViewerEditor.KEYBOARD_ACTIVATION
						| ColumnViewerEditor.TABBING_CYCLE_IN_ROW);

		// Configure SWT table (inside viewer)
		Table userTable = userTableViewer.getTable();
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200)
				.applyTo(userTable);
		userTable.setHeaderVisible(true);
		userTable.setLinesVisible(true);

		// Configure viewer
		userTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		userTableViewer.addSelectionChangedListener(selectionChangeListener);

		// Create columns
		createColumns();

		// Set input
		setInput();

	}

	private void createColumns() {
		// Create column 'full name'
		TableViewerColumn nameColumn = createTableViewerColumn(
				UIText.GerritUsersPreferencePage_FullNameColumn, 150);
		nameColumn.setLabelProvider(nameColumnLabelProvider);
		nameColumn.setEditingSupport(new NameEditingSupport(userTableViewer));

		// Create column 'login'
		TableViewerColumn loginColumn = createTableViewerColumn(
				UIText.GerritUsersPreferencePage_LoginColumn, 150);
		loginColumn.setLabelProvider(loginColumnLabelProvider);
		loginColumn.setEditingSupport(new LoginEditingSupport(userTableViewer));
	}

	private TableViewerColumn createTableViewerColumn(String title, int bound) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(
				userTableViewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();

		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);

		return viewerColumn;
	}

	private void createButtons(Composite main) {
		Composite buttonsComposite = new Composite(main, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0)
				.applyTo(buttonsComposite);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING)
				.applyTo(buttonsComposite);

		Button addButton = new Button(buttonsComposite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING)
				.hint(getHintSize(addButton), SWT.DEFAULT).applyTo(addButton);
		addButton.setText(UIText.GerritUsersPreferencePage_AddUserButton);
		addButton.addSelectionListener(addButtonSelectionListener);

		removeButton = new Button(buttonsComposite, SWT.NONE);
		GridDataFactory.createFrom((GridData) addButton.getLayoutData())
				.applyTo(removeButton);
		removeButton.setText(UIText.GerritUsersPreferencePage_RemoveUserButton);
		removeButton.addSelectionListener(removeButtonSelectionListener);
		removeButton.setEnabled(false);
	}

	private void setInput() {
		userTableViewer.setInput(people);
	}

	private static final int getHintSize(Control control) {
		PixelConverter converter = new PixelConverter(control);
		return converter
				.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
	}

	/**
	 * Editing support for 'full name' column.
	 */
	private final class NameEditingSupport extends EditingSupport {

		public NameEditingSupport(ColumnViewer viewer) {
			super(viewer);
		}

		@Override
		protected void setValue(Object element, Object value) {
			((PersonEntry) element).setName((String) value);
			getViewer().update(element, null);
		}

		@Override
		protected Object getValue(Object element) {
			return ((PersonEntry) element).getName();
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(userTableViewer.getTable());
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}

	/**
	 * Editing support for 'login' column.
	 */
	private final class LoginEditingSupport extends EditingSupport {

		public LoginEditingSupport(ColumnViewer viewer) {
			super(viewer);
		}

		@Override
		protected void setValue(Object element, Object value) {
			((PersonEntry) element).setLogin((String) value);
			getViewer().update(element, null);
		}

		@Override
		protected Object getValue(Object element) {
			return ((PersonEntry) element).getLogin();
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(userTableViewer.getTable());
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}

	/**
	 * Wrapper for {@link Person} class which is immutable.
	 */
	private final static class PersonEntry {
		private String name;

		private String login;

		public PersonEntry() {
			name = UIText.GerritUsersPreferencePage_InitialNameText;
			login = UIText.GerritUsersPreferencePage_InitialLoginText;
		}

		public PersonEntry(Person person) {
			name = person.getName();
			login = person.getLogin();
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getLogin() {
			return login;
		}

		public void setLogin(String login) {
			this.login = login;
		}

	}
}
