/*******************************************************************************
 * Copyright (c) 2012 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitCloneSourceProviderExtension.CloneSourceProvider;
import org.eclipse.egit.ui.internal.provisional.wizards.RepositoryServerInfo;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * Displays the possible locations of git repositories
 */
public class RepositoryLocationPage extends WizardPage {

	private final List<CloneSourceProvider> repositoryImports;

	// local cache for contributed WizardPages
	private Map<CloneSourceProvider, WizardPage> resolvedWizardPages;

	private TreeViewer tv;

	/**
	 * @param cloneSourceProvider all contributed CloneSourceProviders
	 */
	public RepositoryLocationPage(List<CloneSourceProvider> cloneSourceProvider) {
		super(RepositoryLocationPage.class.getName());
		this.repositoryImports = cloneSourceProvider;
		resolvedWizardPages = new HashMap<>();
		setTitle(UIText.RepositoryLocationPage_title);
		setMessage(UIText.RepositoryLocationPage_info);
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0)
				.applyTo(main);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		// use a filtered tree
		FilteredTree tree = new FilteredTree(main, SWT.SINGLE | SWT.BORDER
				| SWT.H_SCROLL | SWT.V_SCROLL, new PatternFilter(), true);

		tv = tree.getViewer();
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);
		tv.setContentProvider(new RepositoryLocationContentProvider());

		tv.setLabelProvider(new RepositoryLocationLabelProvider());

		tv.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				checkPage();
			}
		});

		tv.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				checkPage();
				if (isPageComplete())
					getContainer().showPage(getNextPage());
			}
		});

		tv.setInput(repositoryImports);
		setControl(main);
	}

	private void checkPage() {
		setErrorMessage(null);
		boolean complete = false;
		IStructuredSelection selection = (IStructuredSelection) tv
				.getSelection();
		if (selection.size() == 1) {
			Object element = selection.getFirstElement();
			if (element instanceof CloneSourceProvider) {
				CloneSourceProvider repositoryImport = (CloneSourceProvider) element;
				if (repositoryImport.equals(CloneSourceProvider.LOCAL)
						|| repositoryImport.hasFixLocation())
					complete = true;
			} else if (element instanceof RepositoryServerInfo) {
				complete = true;
			}
		}

		setPageComplete(complete);
	}

	@Override
	public IWizardPage getNextPage() {
		IStructuredSelection selection = (IStructuredSelection) tv
				.getSelection();

		if (selection.size() == 1) {
			Object element = selection.getFirstElement();
			if (element instanceof CloneSourceProvider) {
				return getNextPage((CloneSourceProvider) element);
			} else if (element instanceof RepositoryServerInfo) {
				Object parent = ((ITreeContentProvider) tv.getContentProvider())
						.getParent(element);
				if (parent instanceof CloneSourceProvider)
					return getNextPage((CloneSourceProvider) parent);
			}
		}

		return null;

	}

	private IWizardPage getNextPage(CloneSourceProvider repositoryImport) {
		if (repositoryImport.equals(CloneSourceProvider.LOCAL))
			return getWizard().getNextPage(this);
		else
			return getWizardPage(repositoryImport);
	}

	private WizardPage getWizardPage(CloneSourceProvider repositoryImport) {
		WizardPage nextPage;
		nextPage = resolvedWizardPages.get(repositoryImport);
		if (nextPage == null) {
			try {
				nextPage = repositoryImport.getRepositorySearchPage();
			} catch (CoreException e) {
				Activator.error(e.getLocalizedMessage(), e);
			}
			if (nextPage != null) {
				nextPage.setWizard(getWizard());
				resolvedWizardPages.put(repositoryImport, nextPage);
			}
		}
		return nextPage;
	}
}
