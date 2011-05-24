/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com.dewire.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.AdaptableFileTreeIterator;
import org.eclipse.egit.ui.ICommitMessageProvider;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.commit.CommitMesageSection;
import org.eclipse.egit.ui.internal.dialogs.CommitItem.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Dialog is shown to user when they request to commit files. Changes in the
 * selected portion of the tree are shown.
 */
public class CommitDialog extends TitleAreaDialog {

	static class CommitStatusLabelProvider extends ColumnLabelProvider {

		private Image DEFAULT = PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJ_FILE);

		private ResourceManager resourceManager = new LocalResourceManager(
				JFaceResources.getResources());

		private Image getEditorImage(CommitItem item) {
			Image image = DEFAULT;
			String name = new Path(item.path).lastSegment();
			if (name != null) {
				ImageDescriptor descriptor = PlatformUI.getWorkbench()
						.getEditorRegistry().getImageDescriptor(name);
				image = (Image) this.resourceManager.get(descriptor);
			}
			return image;
		}

		private Image getDecoratedImage(Image base, ImageDescriptor decorator) {
			DecorationOverlayIcon decorated = new DecorationOverlayIcon(base,
					decorator, IDecoration.BOTTOM_RIGHT);
			return (Image) this.resourceManager.get(decorated);
		}

		public String getText(Object obj) {
			return ""; //$NON-NLS-1$
		}

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

		public String getToolTipText(Object element) {
			return ((CommitItem) element).status.getText();
		}

		public void dispose() {
			resourceManager.dispose();
			super.dispose();
		}

	}

	static class CommitPathLabelProvider extends ColumnLabelProvider {

		public String getText(Object obj) {
			return ((CommitItem) obj).path;
		}

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
			TableColumn column = (TableColumn) e.widget;
			Table table = column.getParent();

			if (column == table.getSortColumn()) {
				int currentDirection = table.getSortDirection();
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
				table.setSortColumn(null);
				table.setSortDirection(SWT.NONE);
				filesViewer.setComparator(null);
				return;
			}
			table.setSortColumn(column);

			Comparator<CommitItem> comparator;
			if (reversed.booleanValue()) {
				comparator = order.descending();
				table.setSortDirection(SWT.DOWN);
			} else {
				comparator = order;
				table.setSortDirection(SWT.UP);
			}

			filesViewer.setComparator(new CommitViewerComparator(comparator));
		}

	}

	class CommitItemSelectionListener extends SelectionAdapter {

		public void widgetDefaultSelected(SelectionEvent e) {
			IStructuredSelection selection = (IStructuredSelection) filesViewer
					.getSelection();
			CommitItem commitItem = (CommitItem) selection.getFirstElement();
			if (commitItem == null) {
				return;
			}
			IFile file = findFile(commitItem.path);
			if (file == null)
				CompareUtils.compareHeadWithWorkingTree(repository,
						commitItem.path);
			else
				CompareUtils.compareHeadWithWorkspace(repository, file);
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

	/**
	 * Constant for the extension point for the commit message provider
	 */
	private static final String COMMIT_MESSAGE_PROVIDER_ID = "org.eclipse.egit.ui.commitMessageProvider"; //$NON-NLS-1$

	private static final String COMMITTER_VALUES_PREF = "CommitDialog.committerValues"; //$NON-NLS-1$

	private static final String AUTHOR_VALUES_PREF = "CommitDialog.authorValues"; //$NON-NLS-1$

	private static final String SHOW_UNTRACKED_PREF = "CommitDialog.showUntracked"; //$NON-NLS-1$

	FormToolkit toolkit;

	CommitMesageSection messageSection = new CommitMesageSection();

	ToolItem showUntrackedItem;

	CheckboxTableViewer filesViewer;

	Section filesSection;

	ObjectId originalChangeId;

	ArrayList<CommitItem> items = new ArrayList<CommitItem>();

	private boolean signedOff = false;

	private boolean amending = false;

	private boolean createChangeId = false;

	private String commitMessage = null;

	private String author = null;

	private String committer = null;

	/**
	 * A collection of files that should be already checked in the table.
	 */
	private Set<String> preselectedFiles = Collections.emptySet();

	private boolean preselectAll = false;

	private ArrayList<String> selectedFiles = new ArrayList<String>();

	private boolean showUntracked = true;

	private boolean allowToChangeSelection = true;

	private Repository repository;

	/**
	 * @param parentShell
	 */
	public CommitDialog(Shell parentShell) {
		super(parentShell);
		setTitleImage(UIIcons.WIZBAN_CONNECT_REPO.createImage());
	}

	/**
	 * @return The message the user entered
	 */
	public String getCommitMessage() {
		return commitMessage;
	}

	/**
	 * Preset a commit message. This might be for amending a commit.
	 *
	 * @param s
	 *            the commit message
	 */
	public void setCommitMessage(String s) {
		messageSection.setMessage(s);
		commitMessage = s;
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
	 * Preselect all changed files in the commit dialog. Untracked files are not
	 * preselected.
	 *
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
			item.path = path;
			items.add(item);
		}
		messageSection.setCreateChangeIdDefault(repository.getConfig()
				.getBoolean(ConfigConstants.CONFIG_GERRIT_SECTION,
						ConfigConstants.CONFIG_KEY_CREATECHANGEID, false));

		// initially, we sort by status plus path
		Collections.sort(items, new Comparator<CommitItem>() {
			public int compare(CommitItem o1, CommitItem o2) {
				int diff = o1.status.ordinal() - o2.status.ordinal();
				if (diff != 0)
					return diff;
				return o1.path.compareTo(o2.path);
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
		messageSection.setAuthor(author);
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
		messageSection.setCommitter(committer);
	}

	/**
	 * Pre-set the previous author if amending the commit
	 *
	 * @param previousAuthor
	 */
	public void setPreviousAuthor(String previousAuthor) {
		messageSection.setPreviousAuthor(previousAuthor);
	}

	/**
	 * @return whether to auto-add a signed-off line to the message
	 */
	public boolean isSignedOff() {
		return signedOff;
	}

	/**
	 * Pre-set whether a signed-off line should be included in the commit
	 * message.
	 *
	 * @param signedOff
	 */
	public void setSignedOff(boolean signedOff) {
		messageSection.setSignedOff(signedOff);
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
		messageSection.setAmending(amending);
		this.amending = amending;
	}

	/**
	 * Set the message from the previous commit for amending.
	 *
	 * @param string
	 */
	public void setPreviousCommitMessage(String string) {
		messageSection.setPreviousCommitMessage(string);
	}

	/**
	 * Set whether the previous commit may be amended
	 *
	 * @param amendAllowed
	 */
	public void setAmendAllowed(boolean amendAllowed) {
		messageSection.setAmendAllowed(amendAllowed);
	}

	/**
	 * Set whether is is allowed to change the set of selected files
	 * @param allowToChangeSelection
	 */
	public void setAllowToChangeSelection(boolean allowToChangeSelection) {
		this.allowToChangeSelection = allowToChangeSelection;
	}

	/**
	 * @return true if a Change-Id line for Gerrit should be created
	 */
	public boolean getCreateChangeId() {
		return createChangeId;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		toolkit.adapt(parent, false, false);
		createButton(parent, IDialogConstants.OK_ID, UIText.CommitDialog_Commit, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		toolkit.adapt(parent, false, false);
		return super.createButtonBar(parent);
	}

	@Override
	protected Control createHelpControl(Composite parent) {
		toolkit.adapt(parent, false, false);
		Control help = super.createHelpControl(parent);
		toolkit.adapt(help, false, false);
		return help;
	}

	@Override
	protected Control createContents(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(new DisposeListener() {

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

		messageSection.setPreviousCommitterKey(COMMITTER_VALUES_PREF);
		messageSection.setPreviousAuthorKey(AUTHOR_VALUES_PREF);
		messageSection.createControl(container, toolkit, true);
		messageSection.getMessageArea().getTextWidget()
				.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent event) {
						if (event.keyCode == SWT.CR
								&& (event.stateMask & SWT.CONTROL) > 0) {
							okPressed();
						} else if (event.keyCode == SWT.TAB
								&& (event.stateMask & SWT.SHIFT) == 0) {
							event.doit = false;
							messageSection.getMessageArea().getTextWidget()
									.traverse(SWT.TRAVERSE_TAB_NEXT);
						}
					}
				});
		Set<IResource> resources = new HashSet<IResource>();
		for (CommitItem item : items) {
			IFile file = findFile(item.path);
			if (file != null)
				resources.add(file.getProject());
		}
		if (!amending && commitMessage == null)
			try {
				ICommitMessageProvider messageProvider = getCommitMessageProvider();
				if (messageProvider != null)
					messageSection.setMessage(messageProvider
							.getMessage(resources
									.toArray(new IResource[resources.size()])));
			} catch (CoreException coreException) {
				Activator.error(coreException.getLocalizedMessage(),
						coreException);
			}

		filesSection = toolkit.createSection(container,
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

		Table resourcesTable = toolkit.createTable(filesArea, SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK);
		resourcesTable.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TREE_BORDER);
		resourcesTable.setLayoutData(GridDataFactory.fillDefaults()
				.hint(600, 200).grab(true, true).create());

		resourcesTable.addSelectionListener(new CommitItemSelectionListener());

		resourcesTable.setHeaderVisible(true);
		TableColumn statCol = new TableColumn(resourcesTable, SWT.LEFT);
		statCol.setText(UIText.CommitDialog_Status);
		statCol.setWidth(150);
		statCol.addSelectionListener(new HeaderSelectionListener(
				CommitItem.Order.ByStatus));

		TableColumn resourceCol = new TableColumn(resourcesTable, SWT.LEFT);
		resourceCol.setText(UIText.CommitDialog_Path);
		resourceCol.setWidth(415);
		resourceCol.addSelectionListener(new HeaderSelectionListener(
				CommitItem.Order.ByFile));

		filesViewer = new CheckboxTableViewer(resourcesTable);
		new TableViewerColumn(filesViewer, statCol)
				.setLabelProvider(new CommitStatusLabelProvider());
		new TableViewerColumn(filesViewer, resourceCol)
				.setLabelProvider(new CommitPathLabelProvider());
		ColumnViewerToolTipSupport.enableFor(filesViewer);
		filesViewer.setContentProvider(ArrayContentProvider.getInstance());
		filesViewer.setUseHashlookup(true);
		filesViewer.addFilter(new CommitItemFilter());
		filesViewer.setInput(items.toArray());
		filesViewer.getTable().setMenu(getContextMenu());
		filesViewer.addCheckStateListener(new ICheckStateListener() {

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
		IDialogSettings settings = org.eclipse.egit.ui.Activator.getDefault()
				.getDialogSettings();
		if (settings.get(SHOW_UNTRACKED_PREF) != null) {
			showUntracked = Boolean.valueOf(settings.get(SHOW_UNTRACKED_PREF))
					.booleanValue();
		}
		showUntrackedItem.setSelection(showUntracked);
		showUntrackedItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				showUntracked = showUntrackedItem.getSelection();
				filesViewer.refresh(true);
				updateFileSectionText();
			}

		});

		ToolItem checkAllItem = new ToolItem(filesToolbar, SWT.PUSH);
		Image checkImage = UIIcons.CHECK_ALL.createImage();
		UIUtils.hookDisposal(checkAllItem, checkImage);
		checkAllItem.setImage(checkImage);
		checkAllItem.setToolTipText(UIText.CommitDialog_SelectAll);
		checkAllItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				filesViewer.setAllChecked(true);
				updateFileSectionText();
			}

		});

		ToolItem uncheckAllItem = new ToolItem(filesToolbar, SWT.PUSH);
		Image uncheckImage = UIIcons.UNCHECK_ALL.createImage();
		UIUtils.hookDisposal(uncheckAllItem, uncheckImage);
		uncheckAllItem.setImage(uncheckImage);
		uncheckAllItem.setToolTipText(UIText.CommitDialog_DeselectAll);
		uncheckAllItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				filesViewer.setAllChecked(false);
				updateFileSectionText();
			}

		});

		if (!allowToChangeSelection) {
			messageSection.setAmendEnabled(false);
			showUntrackedItem.setSelection(false);
			showUntrackedItem.setEnabled(false);

			filesViewer.addCheckStateListener(new ICheckStateListener() {

				public void checkStateChanged(CheckStateChangedEvent event) {
					if (!event.getChecked())
						filesViewer.setAllChecked(true);
				}
			});
			filesViewer.setAllGrayed(true);
			filesViewer.setAllChecked(true);
		} else {
			for (CommitItem item : items) {
				if ((preselectAll || preselectedFiles.contains(item.path))
						&& item.status != Status.UNTRACKED
						&& item.status != Status.ASSUME_UNCHANGED)
					filesViewer.setChecked(item, true);
			}
		}

		applyDialogFont(container);
		statCol.pack();
		resourceCol.pack();
		container.pack();
		messageSection.getMessageArea().setFocus();
		setTitle(UIText.CommitDialog_Title);
		setMessage(UIText.CommitDialog_Message, IMessageProvider.INFORMATION);
		updateFileSectionText();
		return container;
	}

	private void updateFileSectionText() {
		filesSection.setText(MessageFormat.format(UIText.CommitDialog_Files,
				Integer.valueOf(filesViewer.getCheckedElements().length),
				Integer.valueOf(filesViewer.getTable().getItemCount())));
	}

	private ICommitMessageProvider getCommitMessageProvider()
			throws CoreException {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] config = registry
				.getConfigurationElementsFor(COMMIT_MESSAGE_PROVIDER_ID);
		if (config.length > 0) {
			Object provider;
			provider = config[0].createExecutableExtension("class");//$NON-NLS-1$
			if (provider instanceof ICommitMessageProvider) {
				return (ICommitMessageProvider) provider;
			} else {
				Activator.logError(UIText.CommitDialog_WrongTypeOfCommitMessageProvider,
						null);
			}
		}
		return null;
	}

	private Menu getContextMenu() {
		if (!allowToChangeSelection)
			return null;
		Menu menu = new Menu(filesViewer.getTable());
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(UIText.CommitDialog_AddFileOnDiskToIndex);
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				IStructuredSelection sel = (IStructuredSelection) filesViewer
						.getSelection();
				if (sel.isEmpty()) {
					return;
				}
				AddCommand addCommand = new Git(repository).add();
				for (Iterator<?> it = sel.iterator(); it.hasNext();) {
					CommitItem commitItem = (CommitItem) it.next();
					addCommand.addFilepattern(commitItem.path);
				}
				try {
					addCommand.call();
				} catch (NoFilepatternException e) {
					Activator.logError(UIText.CommitDialog_ErrorAddingFiles, e);
				}
				for (Iterator<?> it = sel.iterator(); it.hasNext();) {
					CommitItem commitItem = (CommitItem) it.next();
					try {
						commitItem.status = getFileStatus(commitItem.path);
					} catch (IOException e) {
						Activator.logError(UIText.CommitDialog_ErrorAddingFiles, e);
					}
				}
				filesViewer.refresh(true);
			}
		});

		return menu;
	}

	/** Retrieve file status
	 * @param path
	 * @return file status
	 * @throws IOException
	 */
	private Status getFileStatus(String path) throws IOException {
		AdaptableFileTreeIterator fileTreeIterator = new AdaptableFileTreeIterator(
				repository, ResourcesPlugin.getWorkspace().getRoot());
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

	@Override
	protected void okPressed() {
		commitMessage = messageSection.getMessageArea().getCommitMessage();
		author = messageSection.getAuthor().trim();
		committer = messageSection.getCommitter().trim();
		signedOff = messageSection.isSignedOff();
		amending = messageSection.isAmending();

		Object[] checkedElements = filesViewer.getCheckedElements();
		selectedFiles.clear();
		for (Object obj : checkedElements)
			selectedFiles.add(((CommitItem) obj).path);

		if (commitMessage.trim().length() == 0) {
			MessageDialog.openWarning(getShell(),
					UIText.CommitDialog_ErrorNoMessage,
					UIText.CommitDialog_ErrorMustEnterCommitMessage);
			return;
		}

		boolean authorValid = false;
		if (author.length() > 0) {
			authorValid = RawParseUtils.parsePersonIdent(author) != null;
		}
		if (!authorValid) {
			MessageDialog.openWarning(getShell(),
					UIText.CommitDialog_ErrorInvalidAuthor,
					UIText.CommitDialog_ErrorInvalidAuthorSpecified);
			return;
		}

		boolean committerValid = false;
		if (committer.length() > 0) {
			committerValid = RawParseUtils.parsePersonIdent(committer) != null;
		}
		if (!committerValid) {
			MessageDialog.openWarning(getShell(),
					UIText.CommitDialog_ErrorInvalidAuthor,
					UIText.CommitDialog_ErrorInvalidCommitterSpecified);
			return;
		}

		if (selectedFiles.isEmpty() && !amending) {
			MessageDialog.openWarning(getShell(),
					UIText.CommitDialog_ErrorNoItemsSelected,
					UIText.CommitDialog_ErrorNoItemsSelectedToBeCommitted);
			return;
		}

		messageSection.updateProposals();

		IDialogSettings settings = org.eclipse.egit.ui.Activator.getDefault()
				.getDialogSettings();
		settings.put(SHOW_UNTRACKED_PREF, showUntracked);
		super.okPressed();
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	private IFile findFile(String path) {
		URI uri = new File(repository.getWorkTree(), path).toURI();
		IFile[] workspaceFiles = ResourcesPlugin.getWorkspace().getRoot()
				.findFilesForLocationURI(uri);
		if (workspaceFiles.length > 0)
			return workspaceFiles[0];
		else
			return null;
	}
}

class CommitItem {
	Status status;

	String path;

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

			public int compare(CommitItem o1, CommitItem o2) {
				return o1.status.compareTo(o2.status);
			}

		},

		ByFile() {

			public int compare(CommitItem o1, CommitItem o2) {
				return o1.path.compareTo(
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

	public CommitViewerComparator(Comparator comparator){
		super(comparator);
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		return getComparator().compare(e1, e2);
	}

}
