/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com.dewire.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, IBM Corporation (Markus Keller <markus_keller@ch.ibm.com>)
 * Copyright (C) 2013, François Rey <eclipse.org_@_francois_._rey_._name>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitHelper;
import org.eclipse.egit.ui.internal.commit.CommitMessageHistory;
import org.eclipse.egit.ui.internal.commit.CommitProposalProcessor;
import org.eclipse.egit.ui.internal.components.CachedCheckboxTreeViewer;
import org.eclipse.egit.ui.internal.components.FilteredCheckboxTree;
import org.eclipse.egit.ui.internal.decorators.IProblemDecoratable;
import org.eclipse.egit.ui.internal.decorators.ProblemLabelDecorator;
import org.eclipse.egit.ui.internal.dialogs.CommitItem.Status;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponent.CommitStatus;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Dialog is shown to user when they request to commit files. Changes in the
 * selected portion of the tree are shown.
 */
public class CommitDialog extends TitleAreaDialog {

	private static IPreferenceStore getPreferenceStore() {
		return org.eclipse.egit.ui.Activator.getDefault().getPreferenceStore();
	}

	static class CommitFileContentProvider extends BaseWorkbenchContentProvider {
		@Override
		public Object[] getElements(Object element) {
			if (element instanceof Object[])
				return (Object[]) element;
			if (element instanceof Collection)
				return ((Collection) element).toArray();
			return new Object[0];
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
	}

	static class CommitStatusLabelProvider extends BaseLabelProvider implements
			IStyledLabelProvider {

		private Image DEFAULT = PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJ_FILE);

		private ResourceManager resourceManager = new LocalResourceManager(
				JFaceResources.getResources());

		private final Image SUBMODULE = UIIcons.REPOSITORY.createImage();

		private Image getEditorImage(CommitItem item) {
			if (!item.submodule) {
				Image image = DEFAULT;
				String name = new Path(item.path).lastSegment();
				if (name != null) {
					ImageDescriptor descriptor = PlatformUI.getWorkbench()
							.getEditorRegistry().getImageDescriptor(name);
					image = (Image) this.resourceManager.get(descriptor);
				}
				return image;
			} else
				return SUBMODULE;
		}

		private Image getDecoratedImage(Image base, ImageDescriptor decorator) {
			DecorationOverlayIcon decorated = new DecorationOverlayIcon(base,
					decorator, IDecoration.BOTTOM_RIGHT);
			return (Image) this.resourceManager.get(decorated);
		}

		@Override
		public StyledString getStyledText(Object element) {
			return new StyledString();
		}

		@Override
		public Image getImage(Object element) {
			CommitItem item = (CommitItem) element;
			ImageDescriptor decorator = null;
			switch (item.status) {
			case UNTRACKED:
				decorator = UIIcons.OVR_UNTRACKED;
				break;
			case ADDED:
			case ADDED_INDEX_DIFF:
				decorator = UIIcons.OVR_STAGED_ADD;
				break;
			case REMOVED:
			case REMOVED_NOT_STAGED:
			case REMOVED_UNTRACKED:
				decorator = UIIcons.OVR_STAGED_REMOVE;
				break;
			default:
				break;
			}
			return decorator != null ? getDecoratedImage(getEditorImage(item),
					decorator) : getEditorImage(item);
		}

		@Override
		public void dispose() {
			SUBMODULE.dispose();
			resourceManager.dispose();
			super.dispose();
		}
	}

	static class CommitPathLabelProvider extends ColumnLabelProvider {

		@Override
		public String getText(Object obj) {
			return ((CommitItem) obj).path;
		}

		@Override
		public String getToolTipText(Object element) {
			return ((CommitItem) element).status.getText();
		}

	}

	class HeaderSelectionListener extends SelectionAdapter {

		private CommitItem.Order order;

		private Boolean reversed;

		public HeaderSelectionListener(CommitItem.Order order) {
			this.order = order;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			TreeColumn column = (TreeColumn) e.widget;
			Tree tree = column.getParent();

			if (column == tree.getSortColumn()) {
				int currentDirection = tree.getSortDirection();
				switch (currentDirection) {
				case SWT.NONE:
					reversed = Boolean.FALSE;
					break;
				case SWT.UP:
					reversed = Boolean.TRUE;
					break;
				case SWT.DOWN:
					// fall through
				default:
					reversed = null;
					break;
				}
			} else
				reversed = Boolean.FALSE;

			if (reversed == null) {
				tree.setSortColumn(null);
				tree.setSortDirection(SWT.NONE);
				filesViewer.setComparator(null);
				return;
			}
			tree.setSortColumn(column);

			Comparator<CommitItem> comparator;
			if (reversed.booleanValue()) {
				comparator = order.descending();
				tree.setSortDirection(SWT.DOWN);
			} else {
				comparator = order;
				tree.setSortDirection(SWT.UP);
			}

			filesViewer.setComparator(new CommitViewerComparator(comparator));
		}

	}

	class CommitItemSelectionListener extends SelectionAdapter {

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			IStructuredSelection selection = (IStructuredSelection) filesViewer.getSelection();
			CommitItem commitItem = (CommitItem) selection.getFirstElement();
			if (commitItem == null) {
				return;
			}
			compare(commitItem);
		}

	}

	private final class CommitItemFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			boolean result = true;
			if (!showUntracked || !allowToChangeSelection) {
				if (element instanceof CommitItem) {
					CommitItem item = (CommitItem) element;
					if (item.status == Status.UNTRACKED)
						result = false;
				}
			}
			return result;
		}
	}

	private static final String SHOW_UNTRACKED_PREF = "CommitDialog.showUntracked"; //$NON-NLS-1$

	private static final String DIALOG_SETTINGS_SECTION_NAME = Activator
			.getPluginId() + ".COMMIT_DIALOG_SECTION"; //$NON-NLS-1$

	/**
	 * A constant used for the 'commit and push button' button
	 */
	public static final int COMMIT_AND_PUSH_ID = 30;

	FormToolkit toolkit;

	CommitMessageComponent commitMessageComponent;

	SpellcheckableMessageArea commitText;

	Text authorText;

	Text committerText;

	ToolItem amendingItem;

	ToolItem signedOffItem;

	ToolItem signCommitItem;

	ToolItem changeIdItem;

	ToolItem showUntrackedItem;

	CachedCheckboxTreeViewer filesViewer;

	Section filesSection;

	Button commitButton;

	Button commitAndPushButton;

	Button ignoreErrors;

	ArrayList<CommitItem> items = new ArrayList<>();

	private String commitMessage = null;

	private String author = null;

	private String committer = null;

	/**
	 * A collection of files that should be already checked in the table.
	 */
	private Set<String> preselectedFiles = Collections.emptySet();

	private boolean preselectAll = false;

	private ArrayList<String> selectedFiles = new ArrayList<>();

	private boolean amending = false;

	private boolean amendAllowed = true;

	private boolean showUntracked = true;

	private boolean createChangeId = false;

	private boolean allowToChangeSelection = true;

	private Repository repository;

	private boolean isPushRequested = false;

	private boolean signCommit = false;

	/**
	 * @param parentShell
	 */
	public CommitDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * @return The message the user entered
	 */
	public String getCommitMessage() {
		return commitMessage;
	}

	/**
	 * Preset a commit message. This might be for amending a commit.
	 * @param s the commit message
	 */
	public void setCommitMessage(String s) {
		this.commitMessage = s;
	}

	/**
	 * @return the files selected by the user to commit.
	 */
	public Collection<String> getSelectedFiles() {
		return selectedFiles;
	}

	/**
	 * Sets the files that should be checked in this table.
	 *
	 * @param preselectedFiles
	 *            the files to be checked in the dialog's table, must not be
	 *            <code>null</code>
	 */
	public void setPreselectedFiles(Set<String> preselectedFiles) {
		Assert.isNotNull(preselectedFiles);
		this.preselectedFiles = preselectedFiles;
	}

	/**
	 * Preselect all changed files in the commit dialog.
	 * Untracked files are not preselected.
	 * @param preselectAll
	 */
	public void setPreselectAll(boolean preselectAll) {
		this.preselectAll = preselectAll;
	}

	/**
	 * Set the total set of changed files, including additions and
	 * removals
	 * @param repository
	 * @param paths paths of files potentially affected by a new commit
	 * @param indexDiff IndexDiff of the related repository
	 */
	public void setFiles(Repository repository, Set<String> paths,
			IndexDiff indexDiff) {
		this.repository = repository;
		items.clear();
		for (String path : paths) {
			CommitItem item = new CommitItem();
			item.status = getFileStatus(path, indexDiff);
			item.submodule = FileMode.GITLINK == indexDiff.getIndexMode(path);
			item.path = path;
			item.problemSeverity = getProblemSeverity(repository, path);
			items.add(item);
		}

		// initially, we sort by status plus path
		Collections.sort(items, new Comparator<CommitItem>() {
			@Override
			public int compare(CommitItem o1, CommitItem o2) {
				int diff = o1.status.ordinal() - o2.status.ordinal();
				if (diff != 0)
					return diff;
				return o1.path.compareToIgnoreCase(o2.path);
			}
		});
	}

	/**
	 * @return The author to set for the commit
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Pre-set author for the commit
	 *
	 * @param author
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * @return The committer to set for the commit
	 */
	public String getCommitter() {
		return committer;
	}

	/**
	 * Pre-set committer for the commit
	 *
	 * @param committer
	 */
	public void setCommitter(String committer) {
		this.committer = committer;
	}

	/**
	 * @return whether the last commit is to be amended
	 */
	public boolean isAmending() {
		return amending;
	}

	/**
	 * Pre-set whether the last commit is going to be amended
	 *
	 * @param amending
	 */
	public void setAmending(boolean amending) {
		this.amending = amending;
	}

	/**
	 * Set whether the previous commit may be amended
	 *
	 * @param amendAllowed
	 */
	public void setAmendAllowed(boolean amendAllowed) {
		this.amendAllowed = amendAllowed;
	}

	/**
	 * Set whether is is allowed to change the set of selected files
	 * @param allowToChangeSelection
	 */
	public void setAllowToChangeSelection(boolean allowToChangeSelection) {
		this.allowToChangeSelection = allowToChangeSelection;
	}

	/**
	 * @return signCommit
	 */
	public boolean isSignCommit() {
		return signCommit;
	}

	/**
	 * @return true if a Change-Id line for Gerrit should be created
	 */
	public boolean getCreateChangeId() {
		return createChangeId;
	}

	/**
	 * @return true if push shall be executed
	 */
	public boolean isPushRequested() {
		return isPushRequested;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		toolkit.adapt(parent, false, false);
		commitAndPushButton = createButton(parent, COMMIT_AND_PUSH_ID,
				UIText.CommitDialog_CommitAndPush, false);
		commitButton = createButton(parent, IDialogConstants.OK_ID,
				UIText.CommitDialog_Commit, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		updateMessage();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId)
			okPressed();
		else if (COMMIT_AND_PUSH_ID == buttonId) {
			isPushRequested = true;
			okPressed();
		} else if (IDialogConstants.CANCEL_ID == buttonId)
			cancelPressed();
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		toolkit.adapt(parent, false, false);
		return super.createButtonBar(parent);
	}

	@Override
	protected Control createHelpControl(Composite parent) {
		toolkit.adapt(parent, false, false);
		Link link = new Link(parent, SWT.WRAP | SWT.NO_FOCUS);
		((GridLayout) parent.getLayout()).numColumns++;
		link.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		link.setText(UIText.CommitDialog_OpenStagingViewLink);
		link.setToolTipText(UIText.CommitDialog_OpenStagingViewToolTip);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openStagingViewLinkClicked();
			}
		});

		toolkit.adapt(link, false, false);
		return link;
	}

	private void openStagingViewLinkClicked() {
		IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
		try {
			StagingView view = (StagingView) workbenchPage
					.showView(StagingView.VIEW_ID);
			view.reload(repository);
			String message = commitMessageComponent.getCommitMessage();
			if (message != null && message.length() > 0)
				view.setCommitMessage(message);
			setReturnCode(CANCEL);
			close();
		} catch (PartInitException e) {
			Activator.handleError(UIText.CommitDialog_OpenStagingViewError, e,
					true);
		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = settings
				.getSection(DIALOG_SETTINGS_SECTION_NAME);
		if (section == null)
			section = settings.addNewSection(DIALOG_SETTINGS_SECTION_NAME);
		return section;
	}

	/**
	 * Add message drop down toolbar item
	 *
	 * @param parent
	 * @return toolbar
	 */
	protected ToolBar addMessageDropDown(Composite parent) {
		final ToolBar dropDownBar = new ToolBar(parent, SWT.FLAT | SWT.RIGHT);
		final ToolItem dropDownItem = new ToolItem(dropDownBar, SWT.PUSH);
		dropDownItem.setImage(PlatformUI.getWorkbench().getSharedImages()
				.getImage("IMG_LCL_RENDERED_VIEW_MENU")); //$NON-NLS-1$
		final Menu menu = new Menu(dropDownBar);
		dropDownItem.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				menu.dispose();
			}
		});
		MenuItem preferencesItem = new MenuItem(menu, SWT.PUSH);
		preferencesItem.setText(UIText.CommitDialog_ConfigureLink);
		preferencesItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String[] pages = new String[] { UIPreferences.PAGE_COMMIT_PREFERENCES };
				PreferencesUtil.createPreferenceDialogOn(getShell(), pages[0],
						pages, null).open();
			}

		});
		dropDownItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Rectangle b = dropDownItem.getBounds();
				Point p = dropDownItem.getParent().toDisplay(
						new Point(b.x, b.y + b.height));
				menu.setLocation(p.x, p.y);
				menu.setVisible(true);
			}

		});
		return dropDownBar;
	}

	@Override
	protected Control createContents(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});
		return super.createContents(parent);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		parent.getShell().setText(UIText.CommitDialog_CommitChanges);

		container = toolkit.createComposite(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		toolkit.paintBordersFor(container);
		GridLayoutFactory.swtDefaults().applyTo(container);

		final SashForm sashForm= new SashForm(container, SWT.VERTICAL
				| SWT.FILL);
		toolkit.adapt(sashForm, true, true);
		sashForm.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.create());
		createMessageAndPersonArea(sashForm);
		filesSection = createFileSection(sashForm);
		sashForm.setWeights(new int[] { 50, 50 });

		applyDialogFont(container);
		container.pack();
		commitText.setFocus();
		Image titleImage = UIIcons.WIZBAN_COMMIT.createImage();
		UIUtils.hookDisposal(parent, titleImage);
		setTitleImage(titleImage);
		setTitle(UIText.CommitDialog_Title);
		if (ignoreErrors != null) {
			setMessage(UIText.CommitDialog_MessageErrors,
					IMessageProvider.WARNING);
		} else {
			setMessage(UIText.CommitDialog_Message,
					IMessageProvider.INFORMATION);
		}

		filesViewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateMessage();
			}
		});

		updateFileSectionText();
		return container;
	}

	private Section createFileSection(Composite container) {
		Section filesSection = toolkit.createSection(container,
				ExpandableComposite.TITLE_BAR
						| ExpandableComposite.CLIENT_INDENT);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(filesSection);
		Composite filesArea = toolkit.createComposite(filesSection);
		filesSection.setClient(filesArea);
		toolkit.paintBordersFor(filesArea);
		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2)
				.applyTo(filesArea);

		ToolBar filesToolbar = new ToolBar(filesSection, SWT.FLAT);

		filesSection.setTextClient(filesToolbar);

		PatternFilter patternFilter = new PatternFilter() {
			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				if(element instanceof CommitItem) {
					CommitItem commitItem = (CommitItem) element;
					return wordMatches(commitItem.path);
				}
				return super.isLeafMatch(viewer, element);
			}
		};
		patternFilter.setIncludeLeadingWildcard(true);
		FilteredCheckboxTree resourcesTreeComposite = new FilteredCheckboxTree(
				filesArea, toolkit, SWT.FULL_SELECTION, patternFilter) {
			@Override
			protected WorkbenchJob doCreateRefreshJob() {
				// workaround for file filter not having an explicit change
				// listener
				WorkbenchJob filterJob = super.doCreateRefreshJob();
				filterJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							getDisplay().asyncExec(new Runnable() {
								@Override
								public void run() {
									updateFileSectionText();
								}
							});
						}
					}
				});
				return filterJob;
			}
		};
		Tree resourcesTree = resourcesTreeComposite.getViewer().getTree();
		resourcesTree.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TREE_BORDER);
		resourcesTreeComposite.setLayoutData(GridDataFactory.fillDefaults()
				.hint(600, 200).grab(true, true).create());

		resourcesTree.addSelectionListener(new CommitItemSelectionListener());
		resourcesTree.setHeaderVisible(true);

		if (getPreferenceStore()
				.getBoolean(UIPreferences.WARN_BEFORE_COMMITTING)
				&& getPreferenceStore()
						.getBoolean(UIPreferences.BLOCK_COMMIT)) {
			ignoreErrors = new Button(resourcesTreeComposite, SWT.CHECK);
			ignoreErrors.setText(UIText.CommitDialog_IgnoreErrors);

			ignoreErrors.setSelection(false);
			ignoreErrors.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					Button button = (Button) e.getSource();
					boolean enable = button.getSelection();
					updateMessage();
					updateCommitButtons(enable);
				}
			});
		}

		TreeColumn statCol = new TreeColumn(resourcesTree, SWT.LEFT);
		statCol.setText(UIText.CommitDialog_Status);
		statCol.setWidth(150);
		statCol.addSelectionListener(new HeaderSelectionListener(
				CommitItem.Order.ByStatus));

		TreeColumn resourceCol = new TreeColumn(resourcesTree, SWT.LEFT);
		resourceCol.setText(UIText.CommitDialog_Path);
		resourceCol.setWidth(415);
		resourceCol.addSelectionListener(new HeaderSelectionListener(
				CommitItem.Order.ByFile));

		filesViewer = resourcesTreeComposite.getCheckboxTreeViewer();
		new TreeViewerColumn(filesViewer, statCol)
				.setLabelProvider(createStatusLabelProvider());
		new TreeViewerColumn(filesViewer, resourceCol)
				.setLabelProvider(new CommitPathLabelProvider());
		ColumnViewerToolTipSupport.enableFor(filesViewer);
		filesViewer.setContentProvider(new CommitFileContentProvider());
		filesViewer.setUseHashlookup(true);
		IDialogSettings settings = org.eclipse.egit.ui.Activator.getDefault()
				.getDialogSettings();
		if (settings.get(SHOW_UNTRACKED_PREF) != null) {
			// note, no matter how the dialog settings are, if
			// the preferences force us to include untracked files
			// we must show them
			showUntracked = Boolean.valueOf(settings.get(SHOW_UNTRACKED_PREF))
					.booleanValue()
					|| getPreferenceStore().getBoolean(
							UIPreferences.COMMIT_DIALOG_INCLUDE_UNTRACKED);
		}

		filesViewer.addFilter(new CommitItemFilter());
		filesViewer.setInput(items.toArray());
		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(createContextMenuListener());
		filesViewer.getTree().setMenu(
				menuManager.createContextMenu(filesViewer.getTree()));
		filesViewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateFileSectionText();
			}
		});

		showUntrackedItem = new ToolItem(filesToolbar, SWT.CHECK);
		Image showUntrackedImage = UIIcons.UNTRACKED_FILE.createImage();
		UIUtils.hookDisposal(showUntrackedItem, showUntrackedImage);
		showUntrackedItem.setImage(showUntrackedImage);
		showUntrackedItem
				.setToolTipText(UIText.CommitDialog_ShowUntrackedFiles);
		showUntrackedItem.setSelection(showUntracked);
		showUntrackedItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				showUntracked = showUntrackedItem.getSelection();
				filesViewer.refresh(true);
				updateFileSectionText();
				updateMessage();
			}

		});

		ToolItem checkAllItem = new ToolItem(filesToolbar, SWT.PUSH);
		Image checkImage = UIIcons.CHECK_ALL.createImage();
		UIUtils.hookDisposal(checkAllItem, checkImage);
		checkAllItem.setImage(checkImage);
		checkAllItem.setToolTipText(UIText.CommitDialog_SelectAll);
		checkAllItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				filesViewer.setAllChecked(true);
				updateFileSectionText();
				updateMessage();
			}

		});

		ToolItem uncheckAllItem = new ToolItem(filesToolbar, SWT.PUSH);
		Image uncheckImage = UIIcons.UNCHECK_ALL.createImage();
		UIUtils.hookDisposal(uncheckAllItem, uncheckImage);
		uncheckAllItem.setImage(uncheckImage);
		uncheckAllItem.setToolTipText(UIText.CommitDialog_DeselectAll);
		uncheckAllItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				filesViewer.setAllChecked(false);
				updateFileSectionText();
				updateMessage();
			}

		});

		if (!allowToChangeSelection) {
			amendingItem.setSelection(false);
			amendingItem.setEnabled(false);
			showUntrackedItem.setSelection(false);
			showUntrackedItem.setEnabled(false);
			checkAllItem.setEnabled(false);
			uncheckAllItem.setEnabled(false);

			filesViewer.addCheckStateListener(new ICheckStateListener() {

				@Override
				public void checkStateChanged(CheckStateChangedEvent event) {
					if (!event.getChecked())
						filesViewer.setAllChecked(true);
					updateFileSectionText();
				}
			});
			filesViewer.setAllChecked(true);
		} else {
			final boolean includeUntracked = getPreferenceStore().getBoolean(
					UIPreferences.COMMIT_DIALOG_INCLUDE_UNTRACKED);
			for (CommitItem item : items) {
				if (!preselectAll && !preselectedFiles.contains(item.path))
					continue;
				if (item.status == Status.ASSUME_UNCHANGED)
					continue;
				if (!includeUntracked && item.status == Status.UNTRACKED)
					continue;
				filesViewer.setChecked(item, true);
			}
		}
		statCol.pack();
		resourceCol.pack();
		return filesSection;
	}

	private void updateCommitButtons(boolean enable) {
		commitAndPushButton.setEnabled(enable);
		commitButton.setEnabled(enable);
	}

	private Composite createMessageAndPersonArea(Composite container) {

		Composite messageAndPersonArea = toolkit.createComposite(container);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(messageAndPersonArea);
		GridLayoutFactory.swtDefaults().margins(0, 0).spacing(0, 0)
				.applyTo(messageAndPersonArea);

		Section messageSection = toolkit.createSection(messageAndPersonArea,
				ExpandableComposite.TITLE_BAR
						| ExpandableComposite.CLIENT_INDENT);
		messageSection.setText(UIText.CommitDialog_CommitMessage);
		Composite messageArea = toolkit.createComposite(messageSection);
		GridLayoutFactory.fillDefaults().spacing(0, 0)
				.extendedMargins(2, 2, 2, 2).applyTo(messageArea);
		toolkit.paintBordersFor(messageArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(messageSection);
		GridLayoutFactory.swtDefaults().applyTo(messageSection);

		Composite headerArea = new Composite(messageSection, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(2)
				.applyTo(headerArea);

		ToolBar messageToolbar = new ToolBar(headerArea, SWT.FLAT
				| SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL)
				.grab(true, false).applyTo(messageToolbar);

		addMessageDropDown(headerArea);

		messageSection.setTextClient(headerArea);

		final CommitProposalProcessor commitProposalProcessor = new CommitProposalProcessor() {
			@Override
			protected Collection<String> computeFileNameProposals() {
				return getFileList();
			}

			@Override
			protected Collection<String> computeMessageProposals() {
				return CommitMessageHistory.getCommitHistory();
			}
		};
		commitText = new CommitMessageArea(messageArea, commitMessage, SWT.NONE) {
			@Override
			protected CommitProposalProcessor getCommitProposalProcessor() {
				return commitProposalProcessor;
			}
		};
		commitText
				.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		messageSection.setClient(messageArea);
		Point size = commitText.getTextWidget().getSize();
		int minHeight = commitText.getTextWidget().getLineHeight() * 3;
		commitText.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, true).hint(size).minSize(size.x, minHeight)
				.align(SWT.FILL, SWT.FILL).create());

		UIUtils.addBulbDecorator(commitText.getTextWidget(),
				UIText.CommitDialog_ContentAssist);

		Composite personArea = toolkit.createComposite(messageAndPersonArea);
		toolkit.paintBordersFor(personArea);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(personArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(personArea);

		toolkit.createLabel(personArea, UIText.CommitDialog_Author)
				.setForeground(
						toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
		authorText = toolkit.createText(personArea, null);
		authorText
				.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		authorText.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());
		if (repository != null
				&& repository.getRepositoryState().equals(
						RepositoryState.CHERRY_PICKING_RESOLVED))
			authorText.setEnabled(false);

		toolkit.createLabel(personArea, UIText.CommitDialog_Committer)
				.setForeground(
						toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
		committerText = toolkit.createText(personArea, null);
		committerText.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());
		if (committer != null)
			committerText.setText(committer);

		amendingItem = new ToolItem(messageToolbar, SWT.CHECK);
		amendingItem.setSelection(amending);
		if (amending)
			amendingItem.setEnabled(false); // if already set, don't allow any
											// changes
		else if (!amendAllowed)
			amendingItem.setEnabled(false);
		amendingItem.setToolTipText(UIText.CommitDialog_AmendPreviousCommit);
		Image amendImage = UIIcons.AMEND_COMMIT.createImage();
		UIUtils.hookDisposal(amendingItem, amendImage);
		amendingItem.setImage(amendImage);

		signedOffItem = new ToolItem(messageToolbar, SWT.CHECK);

		signedOffItem.setToolTipText(UIText.CommitDialog_AddSOB);
		Image signedOffImage = UIIcons.SIGNED_OFF.createImage();
		UIUtils.hookDisposal(signedOffItem, signedOffImage);
		signedOffItem.setImage(signedOffImage);

		signCommitItem = new ToolItem(messageToolbar, SWT.CHECK);

		signCommitItem.setToolTipText(UIText.CommitDialog_SignCommit);
		Image signCommitImage = UIIcons.SIGN_COMMIT.createImage();
		UIUtils.hookDisposal(signCommitItem, signCommitImage);
		signCommitItem.setImage(signCommitImage);

		changeIdItem = new ToolItem(messageToolbar, SWT.CHECK);
		Image changeIdImage = UIIcons.GERRIT.createImage();
		UIUtils.hookDisposal(changeIdItem, changeIdImage);
		changeIdItem.setImage(changeIdImage);
		changeIdItem.setToolTipText(UIText.CommitDialog_AddChangeIdLabel);

		final ICommitMessageComponentNotifications listener = new ICommitMessageComponentNotifications() {

			@Override
			public void updateSignedOffToggleSelection(boolean selection) {
				signedOffItem.setSelection(selection);
			}

			@Override
			public void updateChangeIdToggleSelection(boolean selection) {
				changeIdItem.setSelection(selection);
			}

			@Override
			public void updateSignCommitToggleSelection(boolean selection) {
				signCommitItem.setSelection(selection);
			}

			@Override
			public void statusUpdated() {
				updateMessage();
			}
		};

		commitMessageComponent = new CommitMessageComponent(repository,
				listener);
		commitMessageComponent.enableListeners(false);
		commitMessageComponent.setDefaults();
		commitMessageComponent.attachControls(commitText, authorText,
				committerText);
		commitMessageComponent.setCommitMessage(commitMessage);
		commitMessageComponent.setAuthor(author);
		commitMessageComponent.setCommitter(committer);
		commitMessageComponent.setAmending(amending);
		commitMessageComponent.setFilesToCommit(getFileList());

		amendingItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				commitMessageComponent.setAmendingButtonSelection(amendingItem
						.getSelection());
			}
		});

		changeIdItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				commitMessageComponent.setChangeIdButtonSelection(changeIdItem
						.getSelection());
			}
		});

		signedOffItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				commitMessageComponent
						.setSignedOffButtonSelection(signedOffItem
								.getSelection());
			}
		});

		signCommitItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				commitMessageComponent.setSignCommitButtonSelection(
						signCommitItem.getSelection());
			}
		});

		commitMessageComponent.updateUI();
		commitMessageComponent.enableListeners(true);

		return messageAndPersonArea;
	}

	private static CellLabelProvider createStatusLabelProvider() {
		CommitStatusLabelProvider baseProvider = new CommitStatusLabelProvider();
		ProblemLabelDecorator decorator = new ProblemLabelDecorator(null);
		return new DecoratingStyledCellLabelProvider(baseProvider, decorator, null) {
			@Override
			public String getToolTipText(Object element) {
				return ((CommitItem) element).status.getText();
			}
		};
	}

	private void updateMessage() {
		if (commitButton == null)
			// Not yet fully initialized.
			return;

		String message = null;
		int type = IMessageProvider.NONE;

		boolean ignoreErrorsValue = ignoreErrors == null ? true
				: !ignoreErrors.getSelection();
		boolean hasErrorsOrWarnings = getPreferenceStore()
				.getBoolean(UIPreferences.WARN_BEFORE_COMMITTING)
						? (getProblemsSeverity() >= Integer
								.parseInt(getPreferenceStore().getString(
										UIPreferences.WARN_BEFORE_COMMITTING_LEVEL))
								&& ignoreErrorsValue)
						: false;
		if (hasErrorsOrWarnings) {
			message = UIText.CommitDialog_MessageErrors;
			type = IMessageProvider.WARNING;
		} else {
			String commitMsg = commitMessageComponent.getCommitMessage();
			if (commitMsg == null || commitMsg.trim().length() == 0) {
				message = UIText.CommitDialog_Message;
				type = IMessageProvider.INFORMATION;
			} else if (!isCommitWithoutFilesAllowed()) {
				message = UIText.CommitDialog_MessageNoFilesSelected;
				type = IMessageProvider.INFORMATION;
			} else {
				CommitStatus status = commitMessageComponent.getStatus();
				message = status.getMessage();
				type = status.getMessageType();
			}
		}
		setMessage(message, type);
		boolean commitBlocked = getPreferenceStore()
				.getBoolean(UIPreferences.WARN_BEFORE_COMMITTING)
				&& getPreferenceStore().getBoolean(UIPreferences.BLOCK_COMMIT)
						? (getProblemsSeverity() >= Integer
								.parseInt(getPreferenceStore().getString(
										UIPreferences.BLOCK_COMMIT_LEVEL))
								&& ignoreErrorsValue)
						: false;
		boolean commitEnabled = (type == IMessageProvider.WARNING
				|| type == IMessageProvider.NONE) && !commitBlocked;
		updateCommitButtons(commitEnabled);
	}

	private boolean isCommitWithoutFilesAllowed() {
		if (filesViewer.getCheckedElements().length > 0)
			return true;

		if (amendingItem.getSelection())
			return true;

		return CommitHelper.isCommitWithoutFilesAllowed(repository);
	}

	private Collection<String> getFileList() {
		Collection<String> result = new ArrayList<>();
		for (CommitItem item : items) {
			result.add(item.path);
		}
		return result;
	}

	private void updateFileSectionText() {
		filesSection.setText(MessageFormat.format(UIText.CommitDialog_Files,
				Integer.valueOf(filesViewer.getCheckedElements().length),
				Integer.valueOf(filesViewer.getTree().getItemCount())));
	}

	private IMenuListener createContextMenuListener() {
		return new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				if (!allowToChangeSelection)
					return;

				final IStructuredSelection selection = (IStructuredSelection) filesViewer
						.getSelection();
				if (selection.isEmpty())
					return;

				if (selection.size() == 1
						&& selection.getFirstElement() instanceof CommitItem) {
					CommitItem commitItem = (CommitItem) selection
							.getFirstElement();
					manager.add(createCompareAction(commitItem));
				}

				boolean hasUnselected = false;
				for (Object element : selection.toList()) {
					if (!filesViewer.getChecked(element)) {
						hasUnselected = true;
						break;
					}
				}

				if (hasUnselected)
					manager.add(createSelectAction(selection));
				manager.add(createAddAction(selection));
			}
		};
	}

	private Action createCompareAction(final CommitItem commitItem) {
		return new Action(UIText.CommitDialog_CompareWithHeadRevision) {
			@Override
			public void run() {
				compare(commitItem);
			}
		};
	}

	private Action createAddAction(final IStructuredSelection selection) {
		return new Action(UIText.CommitDialog_AddFileOnDiskToIndex) {
			@Override
			public void run() {
				try (Git git = new Git(repository)) {
					AddCommand addCommand = git.add();
					for (Iterator<?> it = selection.iterator(); it.hasNext();) {
						CommitItem commitItem = (CommitItem) it.next();
						addCommand.addFilepattern(commitItem.path);
					}
					try {
						addCommand.call();
					} catch (Exception e) {
						Activator.logError(UIText.CommitDialog_ErrorAddingFiles,
								e);
					}
				}
				for (Iterator<?> it = selection.iterator(); it.hasNext();) {
					CommitItem commitItem = (CommitItem) it.next();
					try {
						commitItem.status = getFileStatus(commitItem.path);
					} catch (IOException e) {
						Activator.logError(
								UIText.CommitDialog_ErrorAddingFiles, e);
					}
				}
				filesViewer.refresh(true);
			}
		};
	}

	private IAction createSelectAction(final IStructuredSelection selection) {
		return new Action(UIText.CommitDialog_SelectForCommit) {
			@Override
			public void run() {
				for (Object item : selection.toList())
					filesViewer.setChecked(item, true);
			}
		};
	}

	/** Retrieve file status
	 * @param path
	 * @return file status
	 * @throws IOException
	 */
	private Status getFileStatus(String path) throws IOException {
		FileTreeIterator fileTreeIterator = new FileTreeIterator(repository);
		IndexDiff indexDiff = new IndexDiff(repository, Constants.HEAD, fileTreeIterator);
		Set<String> repositoryPaths = Collections.singleton(path);
		indexDiff.setFilter(PathFilterGroup.createFromStrings(repositoryPaths));
		indexDiff.diff(null, 0, 0, ""); //$NON-NLS-1$
		return getFileStatus(path, indexDiff);
	}

	/** Retrieve file status from an already calculated IndexDiff
	 * @param path
	 * @param indexDiff
	 * @return file status
	 */
	private static Status getFileStatus(String path, IndexDiff indexDiff) {
		if (indexDiff.getAssumeUnchanged().contains(path)) {
			return Status.ASSUME_UNCHANGED;
		} else if (indexDiff.getAdded().contains(path)) {
			// added
			if (indexDiff.getModified().contains(path))
				return Status.ADDED_INDEX_DIFF;
			else
				return Status.ADDED;
		} else if (indexDiff.getChanged().contains(path)) {
			// changed
			if (indexDiff.getModified().contains(path))
				return Status.MODIFIED_INDEX_DIFF;
			else
				return Status.MODIFIED;
		} else if (indexDiff.getUntracked().contains(path)) {
			// untracked
			if (indexDiff.getRemoved().contains(path))
				return Status.REMOVED_UNTRACKED;
			else
				return Status.UNTRACKED;
		} else if (indexDiff.getRemoved().contains(path)) {
			// removed
			return Status.REMOVED;
		} else if (indexDiff.getMissing().contains(path)) {
			// missing
			return Status.REMOVED_NOT_STAGED;
		} else if (indexDiff.getModified().contains(path)) {
			// modified (and not changed!)
			return Status.MODIFIED_NOT_STAGED;
		}
		return Status.UNKNOWN;
	}

	private static int getProblemSeverity(Repository repository, String path) {
		IFile file = ResourceUtil.getFileForLocation(repository, path, false);
		if (file != null) {
			try {
				int severity = file.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
				return severity;
			} catch (CoreException e) {
				// Fall back to below
			}
		}
		return IProblemDecoratable.SEVERITY_NONE;
	}

	@Override
	protected void okPressed() {
		if (!isCommitWithoutFilesAllowed()) {
			MessageDialog.openWarning(getShell(), UIText.CommitDialog_ErrorNoItemsSelected, UIText.CommitDialog_ErrorNoItemsSelectedToBeCommitted);
			return;
		}

		if (!commitMessageComponent.checkCommitInfo())
			return;

		Object[] checkedElements = filesViewer.getCheckedElements();
		selectedFiles.clear();
		for (Object obj : checkedElements)
			selectedFiles.add(((CommitItem) obj).path);

		amending = commitMessageComponent.isAmending();
		commitMessage = commitMessageComponent.getCommitMessage();
		author = commitMessageComponent.getAuthor();
		committer = commitMessageComponent.getCommitter();
		createChangeId = changeIdItem.getSelection();
		signCommit = commitMessageComponent.isSignCommit();

		IDialogSettings settings = org.eclipse.egit.ui.Activator
			.getDefault().getDialogSettings();
		settings.put(SHOW_UNTRACKED_PREF, showUntracked);
		CommitMessageHistory.saveCommitHistory(getCommitMessage());
		super.okPressed();
	}

	private int getProblemsSeverity() {
		int result = IProblemDecoratable.SEVERITY_NONE;
		Object[] checkedElements = filesViewer.getCheckedElements();
		ArrayList<String> selectedFiles = new ArrayList<>();
		for (Object obj : checkedElements)
			selectedFiles.add(((CommitItem) obj).path);
		for (final CommitItem item : items) {
			if (item.getProblemSeverity() >= IMarker.SEVERITY_WARNING
					&& selectedFiles.contains(item.path)) {
				if (result < item.getProblemSeverity()) {
					result = item.getProblemSeverity();
				}
			}
		}
		return result;
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	private void compare(CommitItem commitItem) {
		IFile file = findFile(commitItem.path);
		if (file == null || !ResourceUtil.isSharedWithGit(file.getProject())) {
			CompareUtils.compareHeadWithWorkingTree(repository,
					commitItem.path);
		} else {
			CompareUtils.compareHeadWithWorkspace(repository, file);
		}
	}

	private IFile findFile(String path) {
		return ResourceUtil.getFileForLocation(repository, path, false);
	}
}

class CommitItem implements IProblemDecoratable {

	Status status;

	String path;

	boolean submodule;

	int problemSeverity;

	@Override
	public int getProblemSeverity() {
		return problemSeverity;
	}

	/** The ordinal of this {@link Enum} is used to provide the "native" sorting of the list */
	public static enum Status {
		/** */
		ADDED(UIText.CommitDialog_StatusAdded),
		/** */
		MODIFIED(UIText.CommitDialog_StatusModified),
		/** */
		REMOVED(UIText.CommitDialog_StatusRemoved),
		/** */
		ADDED_INDEX_DIFF(UIText.CommitDialog_StatusAddedIndexDiff),
		/** */
		MODIFIED_INDEX_DIFF(UIText.CommitDialog_StatusModifiedIndexDiff),
		/** */
		MODIFIED_NOT_STAGED(UIText.CommitDialog_StatusModifiedNotStaged),
		/** */
		REMOVED_NOT_STAGED(UIText.CommitDialog_StatusRemovedNotStaged),
		/** */
		UNTRACKED(UIText.CommitDialog_StatusUntracked),
		/** */
		REMOVED_UNTRACKED(UIText.CommitDialog_StatusRemovedUntracked),
		/** */
		ASSUME_UNCHANGED(UIText.CommitDialog_StatusAssumeUnchaged),
		/** */
		UNKNOWN(UIText.CommitDialog_StatusUnknown);

		public String getText() {
			return myText;
		}

		private final String myText;

		private Status(String text) {
			myText = text;
		}
	}

	public static enum Order implements Comparator<CommitItem> {
		ByStatus() {

			@Override
			public int compare(CommitItem o1, CommitItem o2) {
				return o1.status.compareTo(o2.status);
			}

		},

		ByFile() {

			@Override
			public int compare(CommitItem o1, CommitItem o2) {
				return o1.path.compareToIgnoreCase(
						o2.path);
			}

		};

		public Comparator<CommitItem> ascending() {
			return this;
		}

		public Comparator<CommitItem> descending() {
			return Collections.reverseOrder(this);
		}
	}
}

class CommitViewerComparator extends ViewerComparator {

	private Comparator<CommitItem> itemComparator;

	public CommitViewerComparator(Comparator<CommitItem> comparator) {
		this.itemComparator = comparator;
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		return itemComparator.compare((CommitItem) e1, (CommitItem) e2);
	}

}
