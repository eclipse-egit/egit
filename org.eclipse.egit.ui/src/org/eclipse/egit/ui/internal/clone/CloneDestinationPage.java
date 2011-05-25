/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page that allows the user entering the location of a repository to be
 * cloned.
 */
class CloneDestinationPage extends WizardPage {

	private final List<Ref> availableRefs = new ArrayList<Ref>();

	private RepositorySelection validatedRepoSelection;

	private List<Ref> validatedSelectedBranches;

	private Ref validatedHEAD;

	private ComboViewer initialBranch;

	private Text directoryText;

	private Text remoteText;

	private String helpContext = null;

	CloneDestinationPage() {
		super(CloneDestinationPage.class.getName());
		setTitle(UIText.CloneDestinationPage_title);
	}

	public void createControl(final Composite parent) {
		final Composite panel = new Composite(parent, SWT.NULL);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		panel.setLayout(layout);

		createDestinationGroup(panel);
		createConfigGroup(panel);
		Dialog.applyDialogFont(panel);
		setControl(panel);
		checkPage();
	}

	@Override
	public void setVisible(final boolean visible) {
		if (visible) {
			if (this.availableRefs.isEmpty()) {
				initialBranch.getCombo().setEnabled(false);
			}
		}
		super.setVisible(visible);
		if (visible)
			directoryText.setFocus();
	}

	public void setSelection(RepositorySelection repositorySelection, List<Ref> availableRefs, List<Ref> branches, Ref head){
		this.availableRefs.clear();
		this.availableRefs.addAll(availableRefs);
		checkPreviousPagesSelections(repositorySelection, branches, head);
		revalidate(repositorySelection,branches, head);
	}

	private void checkPreviousPagesSelections(
			RepositorySelection repositorySelection, List<Ref> branches,
			Ref head) {
		if (!repositorySelection.equals(validatedRepoSelection)
				|| !branches.equals(validatedSelectedBranches)
				|| !head.equals(validatedHEAD))
			setPageComplete(false);
		else
			checkPage();
	}

	private void createDestinationGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.CloneDestinationPage_groupDestination);

		Label dirLabel = new Label(g, SWT.NONE);
		dirLabel.setText(UIText.CloneDestinationPage_promptDirectory + ":"); //$NON-NLS-1$
		dirLabel
				.setToolTipText(UIText.CloneDestinationPage_DefaultRepoFolderTooltip);
		final Composite p = new Composite(g, SWT.NONE);
		final GridLayout grid = new GridLayout();
		grid.numColumns = 2;
		p.setLayout(grid);
		p.setLayoutData(createFieldGridData());
		directoryText = new Text(p, SWT.BORDER);
		directoryText.setLayoutData(createFieldGridData());
		directoryText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				checkPage();
			}
		});
		final Button b = new Button(p, SWT.PUSH);
		b.setText(UIText.CloneDestinationPage_browseButton);
		b.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				final FileDialog d;

				d = new FileDialog(getShell(), SWT.APPLICATION_MODAL | SWT.SAVE);
				if (directoryText.getText().length() > 0) {
					final File file = new File(directoryText.getText())
							.getAbsoluteFile();
					d.setFilterPath(file.getParent());
					d.setFileName(file.getName());
				}
				final String r = d.open();
				if (r != null)
					directoryText.setText(r);
			}
		});

		newLabel(g, UIText.CloneDestinationPage_promptInitialBranch + ":"); //$NON-NLS-1$
		initialBranch = new ComboViewer(g, SWT.DROP_DOWN | SWT.READ_ONLY);
		initialBranch.getCombo().setLayoutData(createFieldGridData());
		initialBranch.getCombo().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				checkPage();
			}
		});
		initialBranch.setContentProvider(ArrayContentProvider.getInstance());
		initialBranch.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				if (((Ref)element).getName().startsWith(Constants.R_HEADS))
					return ((Ref)element).getName().substring(Constants.R_HEADS.length());
				return ((Ref)element).getName();
			} });
	}

	private void createConfigGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.CloneDestinationPage_groupConfiguration);

		newLabel(g, UIText.CloneDestinationPage_promptRemoteName + ":"); //$NON-NLS-1$
		remoteText = new Text(g, SWT.BORDER);
		remoteText.setText(Constants.DEFAULT_REMOTE_NAME);
		remoteText.setLayoutData(createFieldGridData());
		remoteText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});
	}

	private static Group createGroup(final Composite parent, final String text) {
		final Group g = new Group(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		g.setLayout(layout);
		g.setText(text);
		final GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = SWT.FILL;
		g.setLayoutData(gd);
		return g;
	}

	private static void newLabel(final Group g, final String text) {
		new Label(g, SWT.NULL).setText(text);
	}

	private static GridData createFieldGridData() {
		return new GridData(SWT.FILL, SWT.DEFAULT, true, false);
	}

	/**
	 * @return location the user wants to store this repository.
	 */
	public File getDestinationFile() {
		return new File(directoryText.getText());
	}

	/**
	 * @return initial branch selected (includes refs/heads prefix).
	 */
	public Ref getInitialBranch() {
		IStructuredSelection selection =
			(IStructuredSelection)initialBranch.getSelection();
		return (Ref)selection.getFirstElement();
	}

	/**
	 * @return remote name
	 */
	public String getRemote() {
		return remoteText.getText();
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
	 * Check internal state for page completion status.
	 */
	private void checkPage() {
		final String dstpath = directoryText.getText();
		if (dstpath.length() == 0) {
			setErrorMessage(UIText.CloneDestinationPage_errorDirectoryRequired);
			setPageComplete(false);
			return;
		}
		final File absoluteFile = new File(dstpath).getAbsoluteFile();
		if (!isEmptyDir(absoluteFile)) {
			setErrorMessage(NLS.bind(
					UIText.CloneDestinationPage_errorNotEmptyDir, absoluteFile
							.getPath()));
			setPageComplete(false);
			return;
		}

		if (!canCreateSubdir(absoluteFile.getParentFile())) {
			setErrorMessage(NLS.bind(UIText.GitCloneWizard_errorCannotCreate,
					absoluteFile.getPath()));
			setPageComplete(false);
			return;
		}
		if (!availableRefs.isEmpty()
			&& initialBranch.getCombo().getSelectionIndex() < 0) {
			setErrorMessage(UIText.CloneDestinationPage_errorInitialBranchRequired);
			setPageComplete(false);
			return;
		}
		if (remoteText.getText().length() == 0) {
			setErrorMessage(UIText.CloneDestinationPage_errorRemoteNameRequired);
			setPageComplete(false);
			return;
		}

		setErrorMessage(null);
		setPageComplete(true);
	}

	private static boolean isEmptyDir(final File dir) {
		if (!dir.exists())
			return true;
		if (!dir.isDirectory())
			return false;
		return dir.listFiles().length == 0;
	}

	// this is actually just an optimistic heuristic - should be named
	// isThereHopeThatCanCreateSubdir() as probably there is no 100% reliable
	// way to check that in Java for Windows
	private static boolean canCreateSubdir(final File parent) {
		if (parent == null)
			return true;
		if (parent.exists())
			return parent.isDirectory() && parent.canWrite();
		return canCreateSubdir(parent.getParentFile());
	}

	private void revalidate(RepositorySelection repoSelection, List<Ref> branches, Ref head) {
		if (repoSelection.equals(validatedRepoSelection)
				&& branches.equals(validatedSelectedBranches)
				&& head.equals(validatedHEAD)) {
			checkPage();
			return;
		}

		if (!repoSelection.equals(validatedRepoSelection)) {
			validatedRepoSelection = repoSelection;
			// update repo-related selection only if it changed
			final String n = validatedRepoSelection.getURI().getHumanishName();
			setDescription(NLS.bind(UIText.CloneDestinationPage_description, n));
			String destinationDir = Activator.getDefault().getPreferenceStore()
					.getString(UIPreferences.DEFAULT_REPO_DIR);
			File parentDir = new File(destinationDir);
			directoryText.setText(new File(parentDir, n).getAbsolutePath());
		}

		validatedSelectedBranches = branches;
		validatedHEAD = head;

		initialBranch.setInput(branches);
		if (head != null && branches.contains(head))
			initialBranch.setSelection(new StructuredSelection(head));
		else if (branches.size() > 0)
			initialBranch
					.setSelection(new StructuredSelection(branches.get(0)));
		checkPage();
	}

}
