/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tomasz Zarna <Tomasz.Zarna@pl.ibm.com> - Allow to save patches in Workspace
 *    Daniel Megert <daniel_megert@ch.ibm.com> - Create Patch... should remember previously chosen location
 *    Daniel Megert <daniel_megert@ch.ibm.com> - Create Patch wizard must validate initial input
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.op.CreatePatchOperation;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * A wizard page to choose the target location.
 * <p>
 * Extracted from {@link GitCreatePatchWizard}
 */
public class LocationPage extends WizardPage {

	private static final String PATH_KEY = "GitCreatePatchWizard.LocationPage.path"; //$NON-NLS-1$
	private static final String LOCATION_KEY = "GitCreatePatchWizard.LocationPage.location"; //$NON-NLS-1$
	private static final String LOCATION_VALUE_CLIPBOARD = "clipboard"; //$NON-NLS-1$
	private static final String LOCATION_VALUE_FILE_SYSTEM = "filesystem"; //$NON-NLS-1$
	private static final String LOCATION_VALUE_WORKSPACE = "workspace"; //$NON-NLS-1$


	private Button cpRadio;

	Button fsRadio;

	private Text fsPathText;

	private Button fsBrowseButton;

	private Button wsRadio;

	private Text wsPathText;

	private Button wsBrowseButton;

	private boolean wsBrowsed = false;

	private boolean pageValid;

	private IContainer wsSelectedContainer;

	private static class LocationPageContentProvider extends
			BaseWorkbenchContentProvider {
		//Never show closed projects
		boolean showClosedProjects = false;

		@Override
		public Object[] getChildren(Object element) {
			if (element instanceof IWorkspace) {
				// check if closed projects should be shown
				IProject[] allProjects = ((IWorkspace) element).getRoot().getProjects();
				if (showClosedProjects)
					return allProjects;

				ArrayList<IProject> accessibleProjects = new ArrayList<>();
				for (IProject project : allProjects)
					if (project.isOpen())
						accessibleProjects.add(project);
				return accessibleProjects.toArray();
			}
			return super.getChildren(element);
		}
	}

	private class WorkspaceDialog extends TitleAreaDialog {

		protected TreeViewer wsTreeViewer;
		protected Text wsFilenameText;
		protected Image dlgTitleImage;

		private boolean modified = false;

		public WorkspaceDialog(Shell shell) {
			super(shell);
		}

		@Override
		protected Control createContents(Composite parent) {
			Control control = super.createContents(parent);
			setTitle(UIText.GitCreatePatchWizard_WorkspacePatchDialogTitle);
			setMessage(UIText.GitCreatePatchWizard_WorkspacePatchDialogDescription);
			//create title image
			dlgTitleImage = UIIcons.WIZBAN_CREATE_PATCH.createImage();
			setTitleImage(dlgTitleImage);

			return control;
		}

		@Override
		protected Control createDialogArea(Composite parent){
			Composite parentComposite = (Composite) super.createDialogArea(parent);

			// create a composite with standard margins and spacing
			Composite composite = new Composite(parentComposite, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
			layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
			layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
			layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
			composite.setLayout(layout);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));
			composite.setFont(parentComposite.getFont());

			getShell().setText(UIText.GitCreatePatchWizard_WorkspacePatchDialogSavePatch);

			wsTreeViewer = new TreeViewer(composite, SWT.BORDER);
			final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.widthHint= 550;
			gd.heightHint= 250;
			wsTreeViewer.getTree().setLayoutData(gd);

			wsTreeViewer.setContentProvider(new LocationPageContentProvider());
			wsTreeViewer.setComparator(new ResourceComparator(ResourceComparator.NAME));
			wsTreeViewer.setLabelProvider(new WorkbenchLabelProvider());
			wsTreeViewer.setInput(ResourcesPlugin.getWorkspace());

			//Open to whatever is selected in the workspace field
			IPath existingWorkspacePath = new Path(wsPathText.getText());
			//Ensure that this workspace path is valid
			IResource selectedResource = ResourcesPlugin.getWorkspace().getRoot().findMember(existingWorkspacePath);
			if (selectedResource != null) {
				wsTreeViewer.expandToLevel(selectedResource, 0);
				wsTreeViewer.setSelection(new StructuredSelection(selectedResource));
			}

			final Composite group = new Composite(composite, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			group.setLayout(layout);
			group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

			final Label label = new Label(group, SWT.NONE);
			label.setLayoutData(new GridData());
			label.setText(UIText.GitCreatePatchWizard_WorkspacePatchDialogFileName);

			wsFilenameText = new Text(group,SWT.BORDER);
			wsFilenameText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

			setupListeners();

			return parent;
		}

		@Override
		protected Button createButton(Composite parent, int id,
				String label, boolean defaultButton) {
			Button button = super.createButton(parent, id, label,
					defaultButton);
			if (id == IDialogConstants.OK_ID)
				button.setEnabled(false);
			return button;
		}

		private void validateDialog() {
			String fileName = wsFilenameText.getText();

			if (fileName.isEmpty())
				if (modified) {
					setErrorMessage(UIText.GitCreatePatchWizard_WorkspacePatchDialogEnterFileName);
					getButton(IDialogConstants.OK_ID).setEnabled(false);
					return;
				} else {
					setErrorMessage(null);
					getButton(IDialogConstants.OK_ID).setEnabled(false);
					return;
				}

			// make sure that the filename is valid
			if (!(ResourcesPlugin.getWorkspace().validateName(fileName,
					IResource.FILE)).isOK() && modified) {
				setErrorMessage(UIText.GitCreatePatchWizard_WorkspacePatchEnterValidFileName);
				getButton(IDialogConstants.OK_ID).setEnabled(false);
				return;
			}

			// Make sure that a container has been selected
			if (getSelectedContainer() == null) {
				setErrorMessage(UIText.GitCreatePatchWizard_WorkspacePatchDialogEnterValidLocation);
				getButton(IDialogConstants.OK_ID).setEnabled(false);
				return;
			} else {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IPath fullPath = wsSelectedContainer.getFullPath().append(
						fileName);
				if (workspace.getRoot().getFolder(fullPath).exists()) {
					setErrorMessage(UIText.GitCreatePatchWizard_WorkspacePatchFolderExists);
					getButton(IDialogConstants.OK_ID).setEnabled(false);
					return;
				}
			}

			setErrorMessage(null);
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}

		@Override
		protected void okPressed() {
			IFile file = wsSelectedContainer.getFile(new Path(
					wsFilenameText.getText()));
			if (file != null)
				wsPathText.setText(file.getFullPath().toString());

			validatePage();
			super.okPressed();
		}

		private IContainer getSelectedContainer() {
			Object obj = ((IStructuredSelection)wsTreeViewer.getSelection()).getFirstElement();
			if (obj instanceof IContainer)
				wsSelectedContainer = (IContainer) obj;
			else if (obj instanceof IFile)
				wsSelectedContainer = ((IFile) obj).getParent();
			return wsSelectedContainer;
		}

		@Override
		protected void cancelPressed() {
			validatePage();
			super.cancelPressed();
		}

		@Override
		public boolean close() {
			if (dlgTitleImage != null)
				dlgTitleImage.dispose();
			return super.close();
		}

		void setupListeners(){
			wsTreeViewer.addSelectionChangedListener(
					new ISelectionChangedListener() {
						@Override
						public void selectionChanged(SelectionChangedEvent event) {
							IStructuredSelection s = (IStructuredSelection)event.getSelection();
							Object obj=s.getFirstElement();
							if (obj instanceof IContainer)
								wsSelectedContainer = (IContainer) obj;
							else if (obj instanceof IFile){
								IFile tempFile = (IFile) obj;
								wsSelectedContainer = tempFile.getParent();
								wsFilenameText.setText(tempFile.getName());
							}
							validateDialog();
						}
					});

			wsTreeViewer.addDoubleClickListener(
					new IDoubleClickListener() {
						@Override
						public void doubleClick(DoubleClickEvent event) {
							ISelection s= event.getSelection();
							if (s instanceof IStructuredSelection) {
								Object item = ((IStructuredSelection)s).getFirstElement();
								if (wsTreeViewer.getExpandedState(item))
									wsTreeViewer.collapseToLevel(item, 1);
								else
									wsTreeViewer.expandToLevel(item, 1);
							}
							validateDialog();
						}
					});

			wsFilenameText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					modified = true;
					validateDialog();
				}
			});
		}
	}

	/**
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	protected LocationPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	@Override
	public void createControl(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NULL);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		composite.setLayout(gridLayout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		initializeDialogUnits(composite);

		String selectedOption= getDialogSettings().get(LOCATION_KEY);
		if (selectedOption == null)
			selectedOption = LOCATION_VALUE_CLIPBOARD;

		// clipboard
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 3;
		cpRadio = new Button(composite, SWT.RADIO);
		cpRadio.setText(UIText.GitCreatePatchWizard_Clipboard);
		cpRadio.setLayoutData(gd);
		cpRadio.setSelection(LOCATION_VALUE_CLIPBOARD.equals(selectedOption));

		// filesystem
		boolean isFileSystemSelected = LOCATION_VALUE_FILE_SYSTEM.equals(selectedOption);
		fsRadio = new Button(composite, SWT.RADIO);
		fsRadio.setText(UIText.GitCreatePatchWizard_File);
		fsRadio.setSelection(isFileSystemSelected);

		fsPathText = new Text(composite, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fsPathText.setLayoutData(gd);
		fsPathText.setText(createFileName());
		fsPathText.setEnabled(isFileSystemSelected);

		fsBrowseButton = new Button(composite, SWT.PUSH);
		fsBrowseButton.setText(UIText.GitCreatePatchWizard_Browse);
		UIUtils.setButtonLayoutData(fsBrowseButton);
		fsBrowseButton.setEnabled(isFileSystemSelected);

		// workspace
		boolean isWorkspaceSelected = LOCATION_VALUE_WORKSPACE.equals(selectedOption);
		wsRadio = new Button(composite, SWT.RADIO);
		wsRadio.setText(UIText.GitCreatePatchWizard_Workspace);
		wsRadio.setSelection(isWorkspaceSelected);

		wsPathText = new Text(composite, SWT.BORDER);
		wsPathText.setLayoutData(gd);
		wsPathText.setEnabled(isWorkspaceSelected);

		wsBrowseButton = new Button(composite, SWT.PUSH);
		wsBrowseButton.setText(UIText.GitCreatePatchWizard_Browse);
		UIUtils.setButtonLayoutData(wsBrowseButton);
		wsBrowseButton.setEnabled(isWorkspaceSelected);

		cpRadio.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				// disable other input controls
				if (((Button) event.widget).getSelection()) {
					fsPathText.setEnabled(false);
					fsBrowseButton.setEnabled(false);
					wsPathText.setEnabled(false);
					wsBrowseButton.setEnabled(false);
					getDialogSettings().put(LOCATION_KEY, LOCATION_VALUE_CLIPBOARD);
					validatePage();
				}
			}
		});

		fsRadio.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection()) {
					// enable filesystem input controls
					fsPathText.setEnabled(true);
					fsBrowseButton.setEnabled(true);
					wsPathText.setEnabled(false);
					wsBrowseButton.setEnabled(false);
					// set focus to filesystem input text control
					fsPathText.setFocus();
					getDialogSettings().put(LOCATION_KEY, LOCATION_VALUE_FILE_SYSTEM);
					validatePage();
				}
			}
		});

		fsPathText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (validatePage()) {
					IPath filePath= Path.fromOSString(fsPathText.getText()).removeLastSegments(1);
					getDialogSettings().put(PATH_KEY, filePath.toPortableString());
				}
			}
		});

		fsBrowseButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				final FileDialog dialog = new FileDialog(getShell(),
						SWT.PRIMARY_MODAL | SWT.SAVE);
				if (pageValid) {
					final File file = new File(fsPathText.getText());
					dialog.setFilterPath(file.getParent());
					dialog.setFileName(file.getName());
				} else
					dialog.setFileName(""); //$NON-NLS-1$
				dialog.setText(""); //$NON-NLS-1$
				final String path = dialog.open();
				if (path != null)
					fsPathText.setText(new Path(path).toOSString());
				validatePage();
			}
		});

		wsRadio.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (((Button) event.widget).getSelection()) {
					fsPathText.setEnabled(false);
					fsBrowseButton.setEnabled(false);
					// enable workspace input controls
					wsPathText.setEnabled(true);
					wsBrowseButton.setEnabled(true);
					// set focus to workspace input text control
					wsPathText.setFocus();
					getDialogSettings().put(LOCATION_KEY, LOCATION_VALUE_WORKSPACE);
					validatePage();
				}
			}
		});

		wsPathText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (validatePage()) {
					IPath filePath= Path.fromOSString(wsPathText.getText()).removeLastSegments(1);
					getDialogSettings().put(PATH_KEY, filePath.toPortableString());
				}
			}
		});

		wsBrowseButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				final WorkspaceDialog dialog = new WorkspaceDialog(getShell());
				wsBrowsed = true;
				dialog.open();
				validatePage();
			}
		});

		Dialog.applyDialogFont(composite);
		setControl(composite);

		validatePage();
	}

	private String createFileName() {
		String suggestedFileName = getCommit() != null ? CreatePatchOperation
				.suggestFileName(getCommit()) : getRepository().getWorkTree()
				.getName().concat(".patch"); //$NON-NLS-1$
		String path = getDialogSettings().get(PATH_KEY);
		if (path != null)
			return Path.fromPortableString(path).append(suggestedFileName).toOSString();

		return (new File(System.getProperty("user.dir", ""), suggestedFileName)).getPath(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private RevCommit getCommit() {
		return ((GitCreatePatchWizard)getWizard()).getCommit();
	}

	private Repository getRepository() {
		return ((GitCreatePatchWizard)getWizard()).getRepository();
	}

	/**
	 * Allow the user to finish if a valid file has been entered.
	 *
	 * @return page is valid
	 */
	protected boolean validatePage() {
		if (wsRadio.getSelection())
			pageValid = validateWorkspaceLocation();
		else if (fsRadio.getSelection())
			pageValid = validateFilesystemLocation();
		else if (cpRadio.getSelection())
			pageValid = true;

		/**
		 * Avoid draw flicker by clearing error message if all is valid.
		 */
		if (pageValid) {
			setMessage(null);
			setErrorMessage(null);
		}
		setPageComplete(pageValid);
		return pageValid;
	}

	/**
	 * The following conditions must hold for the file system location to be
	 * valid:
	 * - the path must be valid and non-empty
	 * - the path must be absolute
	 * - the specified file must be of type file
	 * - the parent must exist (new folders can be created via the browse button)
	 *
	 * @return if the location is valid
	 */
	private boolean validateFilesystemLocation() {
		final String pathString = fsPathText.getText().trim();
		if (pathString.length() == 0
				|| !new Path("").isValidPath(pathString)) { //$NON-NLS-1$
			setErrorMessage(UIText.GitCreatePatchWizard_FilesystemError);
			return false;
		}

		final File file = new File(pathString);
		if (!file.isAbsolute()) {
			setErrorMessage(UIText.GitCreatePatchWizard_FilesystemInvalidError);
			return false;
		}

		if (file.isDirectory()) {
			setErrorMessage(UIText.GitCreatePatchWizard_FilesystemDirectoryError);
			return false;
		}

		if (pathString.endsWith("/") || pathString.endsWith("\\")) { //$NON-NLS-1$//$NON-NLS-2$
			setErrorMessage(UIText.GitCreatePatchWizard_FilesystemDirectoryNotExistsError);
			return false;
		}

		final File parent = file.getParentFile();
		if (!(parent.exists() && parent.isDirectory())) {
			setErrorMessage(UIText.GitCreatePatchWizard_FilesystemDirectoryNotExistsError);
			return false;
		}
		return true;
	}

	/**
	 * The following conditions must hold for the workspace location to be valid:
	 * - a parent must be selected in the workspace tree view
	 * - the resource name must be valid
	 *
	 * @return if the location is valid
	 */
	private boolean validateWorkspaceLocation() {
		//make sure that the field actually has a filename in it - making
		//sure that the user has had a chance to browse the workspace first
		if (wsPathText.getText().equals("")){ //$NON-NLS-1$
			if (wsRadio.getSelection() && wsBrowsed)
				setErrorMessage(UIText.GitCreatePatchWizard_WorkspacePatchEnterValidFileName);
			else
				setErrorMessage(UIText.GitCreatePatchWizard_WorkspacePatchSelectByBrowsing);
			return false;
		}

		//Make sure that all the segments but the last one (i.e. project + all
		//folders) exist - file doesn't have to exist. It may have happened that
		//some folder refactoring has been done since this path was last saved.
		//
		// The path will always be in format project/{folders}*/file - this
		// is controlled by the workspace location dialog and by
		// validatePath method when path has been entered manually.

		IPath pathToWorkspaceFile = new Path(wsPathText.getText());
		IStatus status = ResourcesPlugin.getWorkspace().validatePath(wsPathText.getText(), IResource.FILE);
		if (status.isOK()) {
			//Trim file name from path
			IPath containerPath = pathToWorkspaceFile.removeLastSegments(1);
			IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath);
			if (container == null) {
				if (wsRadio.getSelection())
					setErrorMessage(UIText.GitCreatePatchWizard_WorkspacePatchSelectByBrowsing);
				return false;
			} else if (!container.isAccessible()) {
				if (wsRadio.getSelection())
					setErrorMessage(UIText.GitCreatePatchWizard_WorkspacePatchProjectClosed);
				return false;
			} else if (ResourcesPlugin.getWorkspace().getRoot().getFolder(
					pathToWorkspaceFile).exists()) {
				setErrorMessage(UIText.GitCreatePatchWizard_WorkspacePatchFolderExists);
				return false;
			}
		} else {
			setErrorMessage(status.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * @return answers a full path to a file system file or
	 *         <code>null</code> if the user selected to save the patch in
	 *         the clipboard.
	 */
	public File getFile() {
		if (pageValid && fsRadio.getSelection())
			return new File(fsPathText.getText().trim());
		if (pageValid && wsRadio.getSelection()) {
			final String filename= wsPathText.getText().trim();
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			final IFile file = root.getFile(new Path(filename));
			return file.getLocation().toFile();
		}
		return null;
	}
}
