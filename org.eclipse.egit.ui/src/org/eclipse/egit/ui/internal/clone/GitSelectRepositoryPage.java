/*******************************************************************************
 * Copyright (C) 2011, 2012 SAP AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Benjamin Muskalla <bmuskalla@eclipsesource.com>
 *    Daniel Megert <daniel_megert@ch.ibm.com> - remove unnecessary @SuppressWarnings
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewContentProvider;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.RepositorySearchWizard;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * Select a repository, add or clone
 */
public class GitSelectRepositoryPage extends WizardPage {
	private final static String LAST_SELECTED_REPO_PREF = "GitSelectRepositoryPage.lastRepository"; //$NON-NLS-1$

	private final RepositoryUtil util;

	private TreeViewer tv;

	private Button addRepo;

	private IPreferenceChangeListener configChangeListener;

	/**
	 *
	 */
	public GitSelectRepositoryPage() {
		super(GitSelectRepositoryPage.class.getName());
		setTitle(UIText.GitSelectRepositoryPage_PageTitle);
		setMessage(UIText.GitSelectRepositoryPage_PageMessage);
		util = Activator.getDefault().getRepositoryUtil();
	}

	/**
	 * @return the repository
	 */
	public Repository getRepository() {
		Object obj = ((IStructuredSelection) tv.getSelection())
				.getFirstElement();
		if (obj == null)
			return null;
		return ((RepositoryTreeNode) obj).getRepository();
	}

	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0).applyTo(
				main);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		// use a filtered tree
		FilteredTree tree = new FilteredTree(main, SWT.SINGLE | SWT.BORDER
				| SWT.H_SCROLL | SWT.V_SCROLL, new PatternFilter(), true);

		tv = tree.getViewer();
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);
		tv.setContentProvider(new RepositoriesViewContentProvider() {
			// we never show children, only the Repository nodes
			@Override
			public Object[] getChildren(Object parentElement) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}
		});
		tv.setLabelProvider(new RepositoriesViewLabelProvider());

		Composite tb = new Composite(main, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tb);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(tb);

		addRepo = new Button(tb, SWT.PUSH);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL,
				SWT.BEGINNING).applyTo(addRepo);
		addRepo.setText(UIText.GitSelectRepositoryPage_AddButton);
		addRepo.setToolTipText(UIText.GitSelectRepositoryPage_AddTooltip);
		addRepo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				List<String> configuredDirs = util.getConfiguredRepositories();
				RepositorySearchWizard wizard = new RepositorySearchWizard(
						configuredDirs);
				WizardDialog dlg = new WizardDialog(getShell(), wizard);
				if (dlg.open() == Window.OK
						&& !wizard.getDirectories().isEmpty()) {
					Set<String> dirs = wizard.getDirectories();
					for (String dir : dirs)
						util.addConfiguredRepository(new File(dir));
					checkPage();
				}
			}

		});

		tv.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				checkPage();
			}
		});

		tv.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				checkPage();
				if (isPageComplete())
					getContainer().showPage(getNextPage());
			}
		});

		tv.setInput(util.getConfiguredRepositories());

		configChangeListener = new IPreferenceChangeListener() {
			public void preferenceChange(PreferenceChangeEvent event) {
				Display display = tv.getControl().getDisplay();
				display.asyncExec(new Runnable() {
					public void run() {
						refreshRepositoryList();
						checkPage();
					}
				});
			}
		};
		util.getPreferences().addPreferenceChangeListener(configChangeListener);

		// we need to select at least a repository to become complete
		setPageComplete(false);
		Dialog.applyDialogFont(main);
		setControl(main);

	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		if (visible && tv.getSelection().isEmpty()) {
			// check in the dialog settings if a repository was selected before
			// and select it if nothing else is selected
			String repoDir = settings.get(LAST_SELECTED_REPO_PREF);
			if (repoDir != null)
				for (TreeItem item : tv.getTree().getItems()) {
					RepositoryNode node = (RepositoryNode) item.getData();
					if (node.getRepository().getDirectory().getPath().equals(
							repoDir))
						tv.setSelection(new StructuredSelection(node));
				}
		} else {
			// save selection in dialog settings
			Object element = ((IStructuredSelection) tv.getSelection())
					.getFirstElement();
			if (element instanceof RepositoryNode)
				settings.put(LAST_SELECTED_REPO_PREF,
						((RepositoryNode) element).getRepository()
								.getDirectory().getPath());
		}
	}

	private void refreshRepositoryList() {
		List<String> dirsBefore = (List<String>) tv.getInput();
		List<String> dirsAfter = util.getConfiguredRepositories();
		if (!dirsBefore.containsAll(dirsAfter)) {
			tv.setInput(dirsAfter);
			for (String dir : dirsAfter)
				if (!dirsBefore.contains(dir))
					try {
						RepositoryNode node = new RepositoryNode(
								null, new FileRepository(new File(dir)));
						tv.setSelection(new StructuredSelection(
								node));
					} catch (IOException e1) {
						Activator.handleError(e1.getMessage(), e1,
								false);
					}
		}
	}

	private void checkPage() {
		setErrorMessage(null);
		try {
			if (((List) tv.getInput()).isEmpty()) {
				setErrorMessage(UIText.GitSelectRepositoryPage_NoRepoFoundMessage);
				return;
			}

			if (tv.getSelection().isEmpty()) {
				setErrorMessage(UIText.GitSelectRepositoryPage_PleaseSelectMessage);
				return;
			}
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		util.getPreferences().removePreferenceChangeListener(
				configChangeListener);
	}
}
