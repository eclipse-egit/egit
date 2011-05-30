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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
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

	private Set<String> filesToBeDeletedRet;

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
		this.setFilesToBeDeletedRet(filesToBeDeleted);
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
		Job job = new Job(UIText.CleanTreeDialog_GetTreeJobName) {
			@Override
			public boolean belongsTo(Object family) {
				if (family.equals(JobFamilies.FILL_TAG_LIST))
					return true;
				return super.belongsTo(family);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
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

		// set dialog headers
		setTitle(getTitle());
		setMessage(UIText.CleanTreeDialog_Message);

		// new composite for file selector
		Composite composite = (Composite) super.createDialogArea(parent);
		final SashForm mainForm = new SashForm(composite, SWT.HORIZONTAL
				| SWT.FILL);
		mainForm.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.create());

		createFileSelectorArea(mainForm);

		applyDialogFont(parent);
		return composite;
	}

	private void createFileSelectorArea(Composite parent) {
		Composite fileSelectorArea = new Composite(parent, SWT.NORMAL);
		fileSelectorArea.setLayout(GridLayoutFactory.swtDefaults().create());
		fileSelectorArea.setLayoutData(GridLayoutFactory.fillDefaults().create());

		// Label at the top of the fileSelectorArea
		new Label(fileSelectorArea, SWT.WRAP).setText(UIText.CleanTreeDialog_deleteFiles);

		// White space table
		//Table table = new Table(fileSelectorArea, SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE);
		Table table = new Table(fileSelectorArea, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE);
		table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(80, 100).create());

		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(100, 10));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayout(layout);

		TableColumn[] column = new TableColumn[2];
		column[0] = new TableColumn(table, SWT.NONE);
		column[0].setText("FileName"); //$NON-NLS-1$

		// generate actual file names
		deleteFileListViewer = new TableViewer(table);
		deleteFileListViewer.setLabelProvider(new CleanLabelProvider());
		deleteFileListViewer.setContentProvider(ArrayContentProvider.getInstance());
		deleteFileListViewer.setInput(new String[] { UIText.CleanTreeDialog_LoadingMessageText });
		deleteFileListViewer.getTable().setEnabled(false);

		// (un)check event handler
		table.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				String string = event.detail == SWT.CHECK ? "Checked" //$NON-NLS-1$
					: "Selected"; //$NON-NLS-1$
				System.out.println(event.item + " " + string); //$NON-NLS-1$
				//System.out.println("Remaining: " + filesToBeDeletedRet.toString()); //$NON-NLS-1$
				//filesToBeDeletedRet.remove(event.item);
				//System.out.println("Remaining: " + filesToBeDeletedRet.toString()); //$NON-NLS-1$
			}
		});

		applyDialogFont(parent);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	private String getTitle() {
		String title = ""; //$NON-NLS-1$
		if (branchName != null) {
			title = NLS.bind(UIText.CleanTreeDialog_Title_Branch,
					branchName);
		} else {
			title = NLS.bind(UIText.CleanTreeDialog_Title,
					branchName);
		}
		return title;
	}

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

	/**
	 * @param filesToBeDeletedRet the filesToBeDeletedRet to set
	 */
	public void setFilesToBeDeletedRet(Set<String> filesToBeDeletedRet) {
		this.filesToBeDeletedRet = filesToBeDeletedRet;
	}

	/**
	 * @return the filesToBeDeletedRet
	 */
	public Set<String> getFilesToBeDeletedRet() {
		return filesToBeDeletedRet;
	}

	private static class CleanLabelProvider extends WorkbenchLabelProvider implements
	ITableLabelProvider {
		private final Image IMG_TAG;

		private final Image IMG_LIGHTTAG;

		private final ResourceManager fImageCache;

		private CleanLabelProvider() {
			fImageCache = new LocalResourceManager(
					JFaceResources.getResources());
			IMG_TAG = fImageCache.createImage(UIIcons.TAG);
			IMG_LIGHTTAG = SWTUtils.getDecoratedImage(
					fImageCache.createImage(UIIcons.TAG), UIIcons.OVR_LIGHTTAG);
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
			if (element instanceof String)
				return element.toString();
			else
				return "Could not load"; //$NON-NLS-1$
		}

		public void dispose() {
			fImageCache.dispose();
			super.dispose();
		}
	}
}
