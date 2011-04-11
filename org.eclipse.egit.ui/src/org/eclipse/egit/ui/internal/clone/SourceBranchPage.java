/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (c) 2010, Benjamin Muskalla <bmuskalla@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CachedCheckboxTreeViewer;
import org.eclipse.egit.ui.internal.FilteredCheckboxTree;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;

class SourceBranchPage extends WizardPage {

	private RepositorySelection validatedRepoSelection;

	private Ref head;

	private final List<Ref> availableRefs = new ArrayList<Ref>();

	private Label label;

	private String transportError;

	private Button selectB;

	private Button unselectB;

	private CachedCheckboxTreeViewer refsViewer;

	private UserPasswordCredentials credentials;

	private String helpContext = null;

	SourceBranchPage() {
		super(SourceBranchPage.class.getName());
		setTitle(UIText.SourceBranchPage_title);
		setDescription(UIText.SourceBranchPage_description);
	}

	List<Ref> getSelectedBranches() {
		Object[] checkedElements = refsViewer.getCheckedElements();
		Ref[] checkedRefs = new Ref[checkedElements.length];
		System.arraycopy(checkedElements, 0, checkedRefs, 0, checkedElements.length);
		return Arrays.asList(checkedRefs);
	}

	List<Ref> getAvailableBranches() {
		return availableRefs;
	}

	Ref getHEAD() {
		return head;
	}

	boolean isSourceRepoEmpty() {
		return availableRefs.isEmpty();
	}

	boolean isAllSelected() {
		return availableRefs.size() == refsViewer.getCheckedElements().length;
	}

	public void createControl(final Composite parent) {
		final Composite panel = new Composite(parent, SWT.NULL);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		panel.setLayout(layout);

		label = new Label(panel, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		PatternFilter filter = new PatternFilter() {
			@Override
			public boolean isElementVisible(Viewer viewer, Object element) {
				if (getSelectedBranches().contains(element))
					return true;
				return super.isElementVisible(viewer, element);
			}
		};

		FilteredCheckboxTree fTree = new FilteredCheckboxTree(panel, null, SWT.NONE,
				filter);
		refsViewer = fTree.getCheckboxTreeViewer();

		ITreeContentProvider provider = new ITreeContentProvider() {

			public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
				// nothing
			}

			public void dispose() {
				// nothing
			}

			public Object[] getElements(Object input) {
				return ((List) input).toArray();
			}

			public boolean hasChildren(Object element) {
				return false;
			}

			public Object getParent(Object element) {
				return null;
			}

			public Object[] getChildren(Object parentElement) {
				return null;
			}
		};
		refsViewer.setContentProvider(provider);
		refsViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (((Ref)element).getName().startsWith(Constants.R_HEADS))
					return ((Ref)element).getName().substring(Constants.R_HEADS.length());
				return ((Ref)element).getName();
			}

			@Override
			public Image getImage(Object element) {
				return RepositoryTreeNodeType.REF.getIcon();
			}
		});

		refsViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				checkPage();
			}
		});
		final Composite bPanel = new Composite(panel, SWT.NONE);
		bPanel.setLayout(new RowLayout());
		selectB = new Button(bPanel, SWT.PUSH);
		selectB.setText(UIText.SourceBranchPage_selectAll);
		selectB.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				refsViewer.setAllChecked(true);
				checkPage();
			}
		});
		unselectB = new Button(bPanel, SWT.PUSH);
		unselectB.setText(UIText.SourceBranchPage_selectNone);
		unselectB.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				refsViewer.setAllChecked(false);
				checkPage();
			}
		});
		bPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Dialog.applyDialogFont(panel);
		setControl(panel);
		checkPage();
	}

	public void setSelection(RepositorySelection selection){
		revalidate(selection);
	}

	public void setCredentials(UserPasswordCredentials credentials) {
		this.credentials = credentials;
	}

	/**
	 * Set the ID for context sensitive help
	 *
	 * @param id
	 *            help context
	 */
	public void setHelpContext(String id) {
		helpContext = id;
	}

	@Override
	public void performHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp(helpContext);
	}

	/**
	 * Check internal state for page completion status. This method should be
	 * called only when all necessary data from previous form is available.
	 */
	private void checkPage() {
		setMessage(null);
		int checkedElementCount = refsViewer.getCheckedElements().length;
		selectB.setEnabled(checkedElementCount != availableRefs.size());
		unselectB.setEnabled(checkedElementCount != 0);
		if (transportError != null) {
			setErrorMessage(transportError);
			setPageComplete(false);
			return;
		}

		if (getSelectedBranches().isEmpty()) {
			setErrorMessage(UIText.SourceBranchPage_errorBranchRequired);
			setPageComplete(false);
			return;
		}
		setErrorMessage(null);
		setPageComplete(true);
	}

	private void revalidate(final RepositorySelection newRepoSelection) {
		if (newRepoSelection.equals(validatedRepoSelection)) {
			// URI hasn't changed, no need to refill the page with new data
			checkPage();
			return;
		}

		label.setText(NLS.bind(UIText.SourceBranchPage_branchList,
				newRepoSelection.getURI().toString()));
		label.getParent().layout();

		validatedRepoSelection = null;
		transportError = null;
		head = null;
		availableRefs.clear();
		refsViewer.setInput(null);
		setPageComplete(false);
		setErrorMessage(null);
		setMessage(null);
		label.getDisplay().asyncExec(new Runnable() {
			public void run() {
				revalidateImpl(newRepoSelection);
			}
		});
	}

	private void revalidateImpl(final RepositorySelection newRepoSelection) {
		if (label.isDisposed() || !isCurrentPage())
			return;

		final ListRemoteOperation listRemoteOp;
		try {
			final URIish uri = newRepoSelection.getURI();
			final Repository db = new FileRepository(new File("/tmp")); //$NON-NLS-1$
			int timeout = Activator.getDefault().getPreferenceStore().getInt(
					UIPreferences.REMOTE_CONNECTION_TIMEOUT);
			listRemoteOp = new ListRemoteOperation(db, uri, timeout);
			if (credentials != null)
				listRemoteOp
						.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
								credentials.getUser(), credentials.getPassword()));
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					listRemoteOp.run(monitor);
				}
			});
		} catch (InvocationTargetException e) {
			Throwable why = e.getCause();
			transportError(why.getMessage());
			ErrorDialog.openError(getShell(),
					UIText.SourceBranchPage_transportError,
					UIText.SourceBranchPage_cannotListBranches, new Status(
							IStatus.ERROR, Activator.getPluginId(), 0, why
									.getMessage(), why.getCause()));
			return;
		} catch (IOException e) {
			transportError(UIText.SourceBranchPage_cannotCreateTemp);
			return;
		} catch (InterruptedException e) {
			transportError(UIText.SourceBranchPage_remoteListingCancelled);
			return;
		}

		final Ref idHEAD = listRemoteOp.getRemoteRef(Constants.HEAD);
		head = null;
		for (final Ref r : listRemoteOp.getRemoteRefs()) {
			final String n = r.getName();
			if (!n.startsWith(Constants.R_HEADS))
				continue;
			availableRefs.add(r);
			if (idHEAD == null || head != null)
				continue;
			if (r.getObjectId().equals(idHEAD.getObjectId()))
				head = r;
		}
		Collections.sort(availableRefs, new Comparator<Ref>() {
			public int compare(final Ref o1, final Ref o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		if (idHEAD != null && head == null) {
			head = idHEAD;
			availableRefs.add(0, idHEAD);
		}

		validatedRepoSelection = newRepoSelection;
		refsViewer.setInput(availableRefs);
		refsViewer.setAllChecked(true);
		checkPage();
	}

	private void transportError(final String msg) {
		transportError = msg;
		checkPage();
	}
}
