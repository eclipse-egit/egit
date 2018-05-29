/*******************************************************************************
 * Copyright (C) 2012, Mathias Kinzler <mathias.kinzler@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Allows to select a single commit from a list of {@link RevCommit}s. The
 * commits are shown in the order in which they are passed.
 */
public class CommitSelectDialog extends TitleAreaDialog {
	private final List<RevCommit> commits = new ArrayList<>();

	private final String message;

	private RevCommit selected;

	/**
	 * @param parent
	 * @param commits
	 **/
	public CommitSelectDialog(Shell parent, List<RevCommit> commits) {
		this(parent, commits, null);
	}

	/**
	 * Creates a new {@link CommitSelectDialog} instance.
	 *
	 * @param parent
	 *            {@link Shell} to use as parent for the dialog
	 * @param commits
	 *            to display
	 * @param message
	 *            to display
	 */
	public CommitSelectDialog(Shell parent, List<RevCommit> commits,
			String message) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.commits.addAll(commits);
		setHelpAvailable(false);
		this.message = message;
	}

	/**
	 * @return the selected commit
	 */
	public RevCommit getSelectedCommit() {
		return selected;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		final TableViewer tv = new TableViewer(main,
				SWT.SINGLE | SWT.BORDER
				| SWT.FULL_SELECTION);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(tv.getControl());
		tv.setContentProvider(ArrayContentProvider.getInstance());
		CommitLabelProvider labelProvider = new CommitLabelProvider();
		labelProvider.addListener(new ILabelProviderListener() {
			@Override
			public void labelProviderChanged(LabelProviderChangedEvent event) {
				tv.refresh();
			}
		});
		tv.setLabelProvider(labelProvider);
		Table table = tv.getTable();
		TableColumn c0 = new TableColumn(table, SWT.NONE);
		c0.setWidth(70);
		c0.setText(UIText.CommitSelectDialog_IdColumn);
		TableColumn c1 = new TableColumn(table, SWT.NONE);
		c1.setWidth(200);
		c1.setText(UIText.CommitSelectDialog_MessageColumn);
		TableColumn c2 = new TableColumn(table, SWT.NONE);
		c2.setWidth(200);
		c2.setText(UIText.CommitSelectDialog_AuthoColumn);
		TableColumn c3 = new TableColumn(table, SWT.NONE);
		c3.setWidth(150);
		c3.setText(UIText.CommitSelectDialog_DateColumn);
		tv.setInput(commits);
		table.setHeaderVisible(true);
		tv.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!event.getSelection().isEmpty())
					selected = (RevCommit) ((IStructuredSelection) event
							.getSelection()).getFirstElement();
				else
					selected = null;
				getButton(OK).setEnabled(selected != null);
			}
		});
		tv.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
		if (message != null) {
			Label label = new Label(main, SWT.LEFT | SWT.WRAP);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
			label.setText(message);
		}
		return main;
	}

	@Override
	public void create() {
		super.create();
		setTitle(UIText.CommitSelectDialog_Title);
		setMessage(UIText.CommitSelectDialog_Message);
		getButton(OK).setEnabled(false);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.CommitSelectDialog_WindowTitle);
	}
}
