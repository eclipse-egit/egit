/*******************************************************************************
 * Copyright (C) 2011, Abhishek Bhatnagar <abhishekbh@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.SWTUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * @author abhishek
 *
 */
public class CleanTreeDialog extends TitleAreaDialog {

	/**
	 * Button id for a "Clear" button (value 22).
	 */
	public static final int CLEAR_ID = 22;

	private String branchName;

	private Repository repo;

	private TableViewer deleteFileListViewer;

	private Set<String> filesToBeDeleted;

	/**
	 * Construct dialog to creating or editing tag.
	 *
	 * @param parent
	 * @param branchName
	 * @param repo
	 * @param filesToBeDeleted
	 */
	public CleanTreeDialog(Shell parent, String branchName, Repository repo, Set<String> filesToBeDeleted) {
		super(parent);
		this.setBranchName(branchName);
		this.setRepo(repo);
		this.setFilesToBeDeleted(filesToBeDeleted);
		setHelpAvailable(false);
	}

	/**
	 * Construct dialog to creating or editing tag.
	 *
	 * @param parent
	 * @param commitId
	 * @param repo
	 */
	public CleanTreeDialog(Shell parent, ObjectId commitId, Repository repo) {
		super(parent);
		this.setBranchName(null);
		this.setRepo(repo);
		setHelpAvailable(false);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.CreateTagDialog_NewTag);
		newShell.setMinimumSize(600, 400);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		parent.setLayout(GridLayoutFactory.swtDefaults().create());
		parent.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.create());

		Composite margin = new Composite(parent, SWT.NONE);
		margin.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.create());

		super.createButtonsForButtonBar(parent);
	}

	@Override
	public void create() {
		super.create();
		// start a job that fills the tag list lazily
		Job job = new Job(UIText.CreateTagDialog_GetTagJobName) {
			@Override
			public boolean belongsTo(Object family) {
				if (family.equals(JobFamilies.FILL_TAG_LIST))
					return true;
				return super.belongsTo(family);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				// This is most likely the file list loading place
				//final List<Object> filesDelete = getCleanFileList();
				PlatformUI.getWorkbench().getDisplay()
						.asyncExec(new Runnable() {
							public void run() {
								if (!deleteFileListViewer.getTable().isDisposed()) {
									deleteFileListViewer.setInput(getFilesToBeDeleted());
									deleteFileListViewer.getTable().setEnabled(true);
								}
							}
						});
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	@Override
	public boolean close() {
		return super.close();
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		initializeDialogUnits(parent);

		setTitle(getTitle());
		setMessage(UIText.CreateTagDialog_Message);

		Composite composite = (Composite) super.createDialogArea(parent);

		final SashForm mainForm = new SashForm(composite, SWT.HORIZONTAL
				| SWT.FILL);
		mainForm.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.create());

		createLeftSection(mainForm);
		createExistingTagsSection(mainForm);

		mainForm.setWeights(new int[] { 70, 30 });

		applyDialogFont(parent);
		return composite;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case CLEAR_ID:

			break;
		case IDialogConstants.OK_ID:
			// read and store data from widgets
			//$FALL-THROUGH$ continue propagating OK button action
		default:
			super.buttonPressed(buttonId);
		}
	}

	private void createLeftSection(SashForm mainForm) {
		Composite left = new Composite(mainForm, SWT.RESIZE);
		left.setLayout(GridLayoutFactory.swtDefaults().margins(10, 5).create());
		left.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.create());

		Label label = new Label(left, SWT.WRAP);
		label.setText(UIText.CreateTagDialog_tagName);
		GridData data = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_CENTER);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH / 2);
		label.setLayoutData(data);
		label.setFont(left.getFont());

		new Label(left, SWT.WRAP).setText(UIText.CreateTagDialog_tagMessage);
	}

	private void createExistingTagsSection(Composite parent) {
		Composite right = new Composite(parent, SWT.NORMAL);
		right.setLayout(GridLayoutFactory.swtDefaults().create());
		right.setLayoutData(GridLayoutFactory.fillDefaults().create());

		new Label(right, SWT.WRAP).setText(UIText.CreateTagDialog_existingTags);

		Table table = new Table(right, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER
				| SWT.SINGLE);
		table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.hint(80, 100).create());

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(100, 20));
		table.setLayout(layout);

		deleteFileListViewer = new TableViewer(table);
		deleteFileListViewer.setLabelProvider(new TagLabelProvider());
		deleteFileListViewer.setContentProvider(ArrayContentProvider.getInstance());

		// let's set the table inactive initially and display a "Loading..."
		// message and fill the list asynchronously during create() in order to
		// improve UI responsiveness
		deleteFileListViewer
				.setInput(new String[] { UIText.CreateTagDialog_LoadingMessageText });
		deleteFileListViewer.getTable().setEnabled(false);
		applyDialogFont(parent);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	private String getTitle() {
		String title = ""; //$NON-NLS-1$
		if (branchName != null) {
			title = NLS.bind(UIText.CreateTagDialog_questionNewTagTitle,
					branchName);
		}
		return title;
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @param branchName the branchName to set
	 */
	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

	/**
	 * @return the branchName
	 */
	public String getBranchName() {
		return branchName;
	}

	/**
	 * @param repo the repo to set
	 */
	public void setRepo(Repository repo) {
		this.repo = repo;
	}

	/**
	 * @return the repo
	 */
	public Repository getRepo() {
		return repo;
	}

	/**
	 * @param filesToBeDeleted the filesToBeDeleted to set
	 */
	public void setFilesToBeDeleted(Set<String> filesToBeDeleted) {
		this.filesToBeDeleted = filesToBeDeleted;
	}

	/**
	 * @return the filesToBeDeleted
	 */
	public Set<String> getFilesToBeDeleted() {
		return filesToBeDeleted;
	}

	private static class TagLabelProvider extends WorkbenchLabelProvider implements
	ITableLabelProvider {
		private final Image IMG_TAG;

		private final Image IMG_LIGHTTAG;

		private final ResourceManager fImageCache;

		private TagLabelProvider() {
			fImageCache = new LocalResourceManager(
					JFaceResources.getResources());
			IMG_TAG = fImageCache.createImage(UIIcons.TAG);
			IMG_LIGHTTAG = SWTUtils.getDecoratedImage(
					fImageCache.createImage(UIIcons.TAG), UIIcons.OVR_UNTRACKED);
		}

		public Image getColumnImage(Object element, int columnIndex) {
			// initially, we just display a single String ("Loading...")
			if (element instanceof String)
				return null;
			else if (element instanceof Ref)
				return IMG_LIGHTTAG;
			else
				return IMG_TAG;
		}

		public String getColumnText(Object element, int columnIndex) {
			// initially, we just display a single String ("Loading...")
			if (element instanceof String)
				return (String) element;
			else if (element instanceof Ref)
				return ((Ref) element).getName().substring(10);
			else
				return ((RevTag) element).getTagName();
		}

		public void dispose() {
			fImageCache.dispose();
			super.dispose();
		}
	}
}
