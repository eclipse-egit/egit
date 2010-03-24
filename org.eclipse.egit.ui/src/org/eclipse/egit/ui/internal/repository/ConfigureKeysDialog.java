/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Configures the Git configuration keys to display in the Properties view
 */
class ConfigureKeysDialog extends Dialog {

	private static final class ContentProvider implements
			IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			return ((List) inputElement).toArray();
		}

		public void dispose() {
			// nothing
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// nothing
		}

	}

	private static final class LabelProvider extends BaseLabelProvider
			implements ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			return (String) element;
		}

	}

	/**
	 * The standard keys
	 */
	public static final List<String> standardKeys = new ArrayList<String>();

	static {
		standardKeys.add("core.logallrefupdates"); //$NON-NLS-1$
		standardKeys.add("core.compression"); //$NON-NLS-1$

		standardKeys.add("remote.?.url"); //$NON-NLS-1$
		standardKeys.add("remote.?.fetch"); //$NON-NLS-1$
		standardKeys.add("remote.?.push"); //$NON-NLS-1$

		standardKeys.add("user.name"); //$NON-NLS-1$
		standardKeys.add("user.email"); //$NON-NLS-1$

		Collections.sort(standardKeys);
	}

	private final List<String> activeKeys = new ArrayList<String>();

	/**
	 * @param parentShell
	 * @param activeKeys
	 */
	ConfigureKeysDialog(Shell parentShell, List<String> activeKeys) {
		super(parentShell);
		this.activeKeys.addAll(activeKeys);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
	}

	/**
	 * @return the strings
	 */
	public List<String> getActiveKeys() {
		return activeKeys;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.ConfigureKeysDialog_DialogTitle);
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		final CheckboxTableViewer tv = CheckboxTableViewer.newCheckList(main,
				SWT.NONE);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(tv.getTable());

		ToolBar tb = new ToolBar(main, SWT.HORIZONTAL);
		final ToolItem del = new ToolItem(tb, SWT.PUSH);
		del.setEnabled(false);
		del.setText(UIText.ConfigureKeysDialog_DeleteButton);
		del.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Object ob : tv.getCheckedElements()) {
					activeKeys.remove(ob);
					tv.setInput(activeKeys);
				}
			}

		});

		final ToolItem addStandard = new ToolItem(tb, SWT.PUSH);
		addStandard.setText(UIText.ConfigureKeysDialog_AddStandardButton);
		addStandard.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				for (String key : standardKeys) {
					if (!activeKeys.contains(key)) {
						activeKeys.add(key);
					}
					tv.setInput(activeKeys);
				}
				Collections.sort(activeKeys);
			}

		});

		ToolItem add = new ToolItem(tb, SWT.PUSH);
		add.setText(UIText.ConfigureKeysDialog_NewButton);
		add.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				IInputValidator validator = new IInputValidator() {

					public String isValid(String newText) {
						if (activeKeys.contains(newText))
							return NLS
									.bind(
											UIText.ConfigureKeysDialog_AlreadyThere_Message,
											newText);
						return null;
					}
				};
				InputDialog id = new InputDialog(getShell(),
						UIText.ConfigureKeysDialog_NewKeyLabel,
						UIText.ConfigureKeysDialog_NewKeyLabel, null, validator);
				if (id.open() == Window.OK) {
					activeKeys.add(id.getValue());
					Collections.sort(activeKeys);
					tv.setInput(activeKeys);
				}
			}

		});

		tv.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				boolean anyChecked = tv.getCheckedElements().length > 0;
				del.setEnabled(anyChecked);

			}
		});

		tv.setLabelProvider(new LabelProvider());
		tv.setContentProvider(new ContentProvider());
		tv.setInput(this.activeKeys);

		return main;
	}
}
