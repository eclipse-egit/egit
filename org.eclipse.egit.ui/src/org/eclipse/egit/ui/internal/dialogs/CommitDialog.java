/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com.dewire.com>
 * Copyright (C) 2007, Robin Rosenberg <me@lathund.dewire.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.AdaptableFileTreeIterator;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.internal.storage.GitFileHistoryProvider;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.UIUtils.IPreviousValueProposalHandler;
import org.eclipse.egit.ui.internal.FileRevisionTypedElement;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.util.ChangeIdUtil;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Dialog is shown to user when they request to commit files. Changes in the
 * selected portion of the tree are shown.
 */
public class CommitDialog extends Dialog {

	static class CommitLabelProvider extends WorkbenchLabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object obj, int columnIndex) {
			CommitItem item = (CommitItem) obj;

			switch (columnIndex) {
			case 0:
				return item.status;

			case 1:
				return item.file.getProject().getName() + ": " //$NON-NLS-1$
						+ item.file.getProjectRelativePath();

			default:
				return null;
			}
		}

		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0)
				return getImage(element);
			return null;
		}
	}

	private final class CommitItemFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			boolean result = true;
			if (!showUntracked || !allowToChangeSelection){
				if (element instanceof CommitItem) {
					CommitItem item = (CommitItem)element;
					if (item.status.equals(UIText.CommitDialog_StatusUntracked))
						result = false;
				}
			}
			return result;
		}
	}

	ArrayList<CommitItem> items = new ArrayList<CommitItem>();

	private static final String COMMITTER_VALUES_PREF = "CommitDialog.committerValues"; //$NON-NLS-1$

	private static final String AUTHOR_VALUES_PREF = "CommitDialog.authorValues"; //$NON-NLS-1$

	private static final String SHOW_UNTRACKED_PREF = "CommitDialog.showUntracked"; //$NON-NLS-1$


	/**
	 * @param parentShell
	 */
	public CommitDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.SELECT_ALL_ID, UIText.CommitDialog_SelectAll, false);
		createButton(parent, IDialogConstants.DESELECT_ALL_ID, UIText.CommitDialog_DeselectAll, false);

		createButton(parent, IDialogConstants.OK_ID, UIText.CommitDialog_Commit, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	CommitMessageArea commitText;
	Text authorText;
	Text committerText;
	Button amendingButton;
	Button signedOffButton;
	Button changeIdButton;
	Button showUntrackedButton;

	CheckboxTableViewer filesViewer;

	ObjectId originalChangeId;

	/**
	 * A collection of files that should be already checked in the table.
	 */
	private Collection<IFile> preselectedFiles = Collections.emptyList();

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		parent.getShell().setText(UIText.CommitDialog_CommitChanges);

		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);

		Label label = new Label(container, SWT.LEFT);
		label.setText(UIText.CommitDialog_CommitMessage);
		label.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).grab(true, false).create());

		commitText = new CommitMessageArea(container, commitMessage);
		Point size = commitText.getTextWidget().getSize();
		int minHeight = commitText.getTextWidget().getLineHeight() * 3;
		commitText.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).grab(true, true)
				.hint(size).minSize(size.x, minHeight).align(SWT.FILL, SWT.FILL).create());
		commitText.setText(commitMessage);

		// allow to commit with ctrl-enter
		commitText.getTextWidget().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.keyCode == SWT.CR
						&& (event.stateMask & SWT.CONTROL) > 0) {
					okPressed();
				} else if (event.keyCode == SWT.TAB
						&& (event.stateMask & SWT.SHIFT) == 0) {
					event.doit = false;
					commitText.traverse(SWT.TRAVERSE_TAB_NEXT);
				}
			}
		});

		new Label(container, SWT.LEFT).setText(UIText.CommitDialog_Author);
		authorText = new Text(container, SWT.BORDER);
		authorText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		if (author != null)
			authorText.setText(author);

		authorHandler = UIUtils.addPreviousValuesContentProposalToText(authorText, AUTHOR_VALUES_PREF);
		new Label(container, SWT.LEFT).setText(UIText.CommitDialog_Committer);
		committerText = new Text(container, SWT.BORDER);
		committerText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		if (committer != null)
			committerText.setText(committer);
		committerText.addModifyListener(new ModifyListener() {
			String oldCommitter = committerText.getText();
			public void modifyText(ModifyEvent e) {
				if (signedOffButton.getSelection()) {
					// the commit message is signed
					// the signature must be updated
					String newCommitter = committerText.getText();
					String oldSignOff = getSignedOff(oldCommitter);
					String newSignOff = getSignedOff(newCommitter);
					commitText.setText(replaceSignOff(commitText.getText(), oldSignOff, newSignOff));
					oldCommitter = newCommitter;
				}
			}
		});

		committerHandler = UIUtils.addPreviousValuesContentProposalToText(committerText, COMMITTER_VALUES_PREF);

		Link preferencesLink = new Link(container, SWT.NONE);
		preferencesLink.setText(UIText.CommitDialog_ConfigureLink);
		preferencesLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String preferencePageId = "org.eclipse.egit.ui.internal.preferences.CommitDialogPreferencePage"; //$NON-NLS-1$
				PreferenceDialog dialog = PreferencesUtil
						.createPreferenceDialogOn(null, preferencePageId,
								new String[] { preferencePageId }, null);
				dialog.open();
				commitText.reconfigure();
			}
		});

		amendingButton = new Button(container, SWT.CHECK);
		if (amending) {
			amendingButton.setSelection(amending);
			amendingButton.setEnabled(false); // if already set, don't allow any changes
			commitText.setText(previousCommitMessage);
			authorText.setText(previousAuthor);
			saveOriginalChangeId();
		} else if (!amendAllowed) {
			amendingButton.setEnabled(false);
			originalChangeId = null;
		}
		amendingButton.addSelectionListener(new SelectionListener() {
			boolean alreadyAdded = false;
			public void widgetSelected(SelectionEvent arg0) {
				if (!amendingButton.getSelection()) {
					originalChangeId = null;
				}
				else {
					saveOriginalChangeId();
					if (!alreadyAdded) {
						alreadyAdded = true;
						String curText = commitText.getText();
						if (curText.length() > 0)
							curText += Text.DELIMITER;
						commitText.setText(curText
								+ previousCommitMessage.replaceAll(
										"\n", Text.DELIMITER)); //$NON-NLS-1$
						authorText.setText(previousAuthor);
					}
				}
				refreshChangeIdText();
			}

			public void widgetDefaultSelected(SelectionEvent arg0) {
				// Empty
			}
		});

		amendingButton.setText(UIText.CommitDialog_AmendPreviousCommit);
		amendingButton.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());

		signedOffButton = new Button(container, SWT.CHECK);
		signedOffButton.setSelection(signedOff);
		signedOffButton.setText(UIText.CommitDialog_AddSOB);
		signedOffButton.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());

		signedOffButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent arg0) {
				String curText = commitText.getText();
				if (signedOffButton.getSelection()) {
					// add signed off line
					commitText.setText(signOff(curText));
				} else {
					// remove signed off line
					curText = replaceSignOff(curText, getSignedOff(), ""); //$NON-NLS-1$
					if (curText.endsWith(Text.DELIMITER + Text.DELIMITER))
						curText = curText.substring(0, curText.length() - Text.DELIMITER.length());
					commitText.setText(curText);
				}
			}

			public void widgetDefaultSelected(SelectionEvent arg0) {
				// Empty
			}
		});

		changeIdButton = new Button(container, SWT.CHECK);
		changeIdButton.setText(UIText.CommitDialog_AddChangeIdLabel);
		changeIdButton.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
		changeIdButton.setToolTipText(UIText.CommitDialog_AddChangeIdTooltip);
		changeIdButton.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				refreshChangeIdText();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				// empty
			}
		});

		showUntrackedButton = new Button(container, SWT.CHECK);
		showUntrackedButton.setText(UIText.CommitDialog_ShowUntrackedFiles);
		showUntrackedButton.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());

		IDialogSettings settings = org.eclipse.egit.ui.Activator.getDefault()
				.getDialogSettings();
		if (settings.get(SHOW_UNTRACKED_PREF) != null) {
			showUntracked = Boolean.valueOf(settings.get(SHOW_UNTRACKED_PREF))
					.booleanValue();
		}

		showUntrackedButton.setSelection(showUntracked);

		showUntrackedButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				showUntracked = showUntrackedButton.getSelection();
				filesViewer.refresh(true);
			}

		});

		commitText.getTextWidget().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateSignedOffButton();
				updateChangeIdButton();
			}
		});
		updateSignedOffButton();
		updateChangeIdButton();

		Table resourcesTable = new Table(container, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK | SWT.BORDER);
		resourcesTable.setLayoutData(GridDataFactory.fillDefaults().hint(600,
				200).span(2,1).grab(true, true).create());

		resourcesTable.addSelectionListener(new CommitItemSelectionListener());

		resourcesTable.setHeaderVisible(true);
		TableColumn statCol = new TableColumn(resourcesTable, SWT.LEFT);
		statCol.setText(UIText.CommitDialog_Status);
		statCol.setWidth(150);
		statCol.addSelectionListener(new HeaderSelectionListener(CommitItem.Order.ByStatus));

		TableColumn resourceCol = new TableColumn(resourcesTable, SWT.LEFT);
		resourceCol.setText(UIText.CommitDialog_File);
		resourceCol.setWidth(415);
		resourceCol.addSelectionListener(new HeaderSelectionListener(CommitItem.Order.ByFile));

		filesViewer = new CheckboxTableViewer(resourcesTable);
		filesViewer.setContentProvider(ArrayContentProvider.getInstance());
		filesViewer.setLabelProvider(new CommitLabelProvider());
		filesViewer.addFilter(new CommitItemFilter());
		filesViewer.setInput(items);
		filesViewer.getTable().setMenu(getContextMenu());
		if (!allowToChangeSelection) {
			amendingButton.setSelection(false);
			amendingButton.setEnabled(false);
			showUntrackedButton.setSelection(false);
			showUntrackedButton.setEnabled(false);

			filesViewer.addCheckStateListener(new ICheckStateListener() {

				public void checkStateChanged(CheckStateChangedEvent event) {
				       if( !event.getChecked() ) {
				    	   filesViewer.setAllChecked(true);
				       }
				}
			});
			filesViewer.setAllGrayed(true);
			filesViewer.setAllChecked(true);
		}
		else {
			// pre-emptively check any preselected files
			for (IFile selectedFile : preselectedFiles) {
				for (CommitItem item : items) {
					if (item.file.equals(selectedFile)) {
						filesViewer.setChecked(item, true);
						break;
					}
				}
			}
		}

		applyDialogFont(container);
		container.pack();
		return container;
	}

	private void saveOriginalChangeId() {
		int changeIdOffset = findOffsetOfChangeIdLine(previousCommitMessage);
		if (changeIdOffset > 0) {
			int endOfChangeId = findNextEOL(changeIdOffset, previousCommitMessage);
			int sha1Offset = changeIdOffset + "\nChange-Id: I".length(); //$NON-NLS-1$
			try {
				originalChangeId = ObjectId.fromString(previousCommitMessage.substring(sha1Offset, endOfChangeId));
			} catch (IllegalArgumentException e) {
				originalChangeId = null;
			}
		} else
			originalChangeId = null;
	}

	private int findNextEOL(int oldPos, String message) {
		return message.indexOf("\n", oldPos + 1); //$NON-NLS-1$
	}

	private int findOffsetOfChangeIdLine(String message) {
		return message.indexOf("\nChange-Id: I"); //$NON-NLS-1$
	}

	private void updateSignedOffButton() {
		String curText = commitText.getText();
		if (!curText.endsWith(Text.DELIMITER))
			curText += Text.DELIMITER;

		signedOffButton.setSelection(curText.indexOf(getSignedOff() + Text.DELIMITER) != -1);
	}

	private void updateChangeIdButton() {
		String curText = commitText.getText();
		if (!curText.endsWith(Text.DELIMITER))
			curText += Text.DELIMITER;

		boolean hasId = curText.indexOf(Text.DELIMITER + "Change-Id: ") != -1; //$NON-NLS-1$
		if (hasId) {
			changeIdButton.setSelection(true);
			createChangeId = true;
		}
	}

	private String getSignedOff() {
		return getSignedOff(committerText.getText());
	}

	private String getSignedOff(String signer) {
		return Constants.SIGNED_OFF_BY_TAG + signer;
	}

	private String signOff(String input) {
		String output = input;
		if (!output.endsWith(Text.DELIMITER))
			output += Text.DELIMITER;

		// if the last line is not footer line, add a line break
		if (!getLastLine(output).matches("[A-Za-z\\-]+:.*")) //$NON-NLS-1$
			output += Text.DELIMITER;
		output += getSignedOff();
		return output;
	}

	private String getLastLine(String input) {
		String output = input;
		int breakLength = Text.DELIMITER.length();

		// remove last line break if exist
		int lastIndexOfLineBreak = output.lastIndexOf(Text.DELIMITER);
		if (lastIndexOfLineBreak != -1 && lastIndexOfLineBreak == output.length() - breakLength)
			output = output.substring(0, output.length() - breakLength);

		// get the last line
		lastIndexOfLineBreak = output.lastIndexOf(Text.DELIMITER);
		return lastIndexOfLineBreak == -1 ? output : output.substring(lastIndexOfLineBreak + breakLength, output.length());
	}

	private String replaceSignOff(String input, String oldSignOff, String newSignOff) {
		assert input != null;
		assert oldSignOff != null;
		assert newSignOff != null;

		String curText = input;
		if (!curText.endsWith(Text.DELIMITER))
			curText += Text.DELIMITER;

		int indexOfSignOff = curText.indexOf(oldSignOff + Text.DELIMITER);
		if (indexOfSignOff == -1)
			return input;

		return input.substring(0, indexOfSignOff) + newSignOff + input.substring(indexOfSignOff + oldSignOff.length(), input.length());
	}

	private Menu getContextMenu() {
		if (!allowToChangeSelection)
			return null;
		Menu menu = new Menu(filesViewer.getTable());
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(UIText.CommitDialog_AddFileOnDiskToIndex);
		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				IStructuredSelection sel = (IStructuredSelection) filesViewer.getSelection();
				if (sel.isEmpty()) {
					return;
				}
				try {
					List<IResource> filesToAdd = new ArrayList<IResource>();
					for (Iterator<?> it = sel.iterator(); it.hasNext();) {
						CommitItem commitItem = (CommitItem) it.next();
						filesToAdd.add(commitItem.file);
					}
					AddToIndexOperation op = new AddToIndexOperation(filesToAdd);
					op.execute(new NullProgressMonitor());
					for (Iterator<?> it = sel.iterator(); it.hasNext();) {
						CommitItem commitItem = (CommitItem) it.next();
						commitItem.status = getFileStatus(commitItem.file);
					}
					filesViewer.refresh(true);
				} catch (CoreException e) {
					Activator.logError(UIText.CommitDialog_ErrorAddingFiles, e);
					return;
				} catch (IOException e) {
					Activator.logError(UIText.CommitDialog_ErrorAddingFiles, e);
					return;
				}
			}
		});

		return menu;
	}

	/** Retrieve file status from an already calculated IndexDiff
	 * @param path
	 * @param indexDiff
	 * @return file status
	 */
	private static String getFileStatus(String path, IndexDiff indexDiff) {
		String prefix = UIText.CommitDialog_StatusUnknown;
		if (indexDiff.getAdded().contains(path)) {
			// added
			if (indexDiff.getModified().contains(path))
				prefix = UIText.CommitDialog_StatusAddedIndexDiff;
			else
				prefix = UIText.CommitDialog_StatusAdded;
		} else if (indexDiff.getChanged().contains(path)) {
			// changed
			if (indexDiff.getModified().contains(path))
				prefix = UIText.CommitDialog_StatusModifiedIndexDiff;
			else
				prefix = UIText.CommitDialog_StatusModified;
		} else if (indexDiff.getUntracked().contains(path)) {
			// untracked
			if (indexDiff.getRemoved().contains(path))
				prefix = UIText.CommitDialog_StatusRemovedUntracked;
			else
				prefix = UIText.CommitDialog_StatusUntracked;
		} else if (indexDiff.getRemoved().contains(path)) {
			// removed
			prefix = UIText.CommitDialog_StatusRemoved;
		} else if (indexDiff.getMissing().contains(path)) {
			// missing
			prefix = UIText.CommitDialog_StatusRemovedNotStaged;
		} else if (indexDiff.getModified().contains(path)) {
			// modified (and not changed!)
			prefix = UIText.CommitDialog_StatusModifiedNotStaged;
		}
		return prefix;
	}

	/** Retrieve file status
	 * @param file
	 * @return file status
	 * @throws IOException
	 */
	private static String getFileStatus(IFile file) throws IOException {
		RepositoryMapping mapping = RepositoryMapping.getMapping(file);
		String path = mapping.getRepoRelativePath(file);
		Repository repo = mapping.getRepository();
		AdaptableFileTreeIterator fileTreeIterator =
			new AdaptableFileTreeIterator(repo.getWorkTree(),
					ResourcesPlugin.getWorkspace().getRoot());
		IndexDiff indexDiff = new IndexDiff(repo, Constants.HEAD, fileTreeIterator);
		Set<String> repositoryPaths = Collections.singleton(path);
		indexDiff.setFilter(PathFilterGroup.createFromStrings(repositoryPaths));
		indexDiff.diff();
		return getFileStatus(path, indexDiff);
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

	private String commitMessage = ""; //$NON-NLS-1$
	private String author = null;
	private String committer = null;
	private String previousAuthor = null;
	private boolean signedOff = false;
	private boolean amending = false;
	private boolean amendAllowed = true;
	private boolean showUntracked = true;
	private boolean createChangeId = false;
	private boolean allowToChangeSelection = true;

	private ArrayList<IFile> selectedFiles = new ArrayList<IFile>();
	private String previousCommitMessage = ""; //$NON-NLS-1$
	private IPreviousValueProposalHandler authorHandler;
	private IPreviousValueProposalHandler committerHandler;


	/**
	 * Pre-select suggested set of resources to commit
	 *
	 * @param items
	 */
	public void setSelectedFiles(IFile[] items) {
		Collections.addAll(selectedFiles, items);
	}

	/**
	 * @return the resources selected by the user to commit.
	 */
	public IFile[] getSelectedFiles() {
		return selectedFiles.toArray(new IFile[0]);
	}

	/**
	 * Sets the files that should be checked in this table.
	 *
	 * @param preselectedFiles
	 *            the files to be checked in the dialog's table, must not be
	 *            <code>null</code>
	 */
	public void setPreselectedFiles(Collection<IFile> preselectedFiles) {
		Assert.isNotNull(preselectedFiles);
		this.preselectedFiles = preselectedFiles;
	}

	class HeaderSelectionListener extends SelectionAdapter {

		private CommitItem.Order order;

		private boolean reversed;

		public HeaderSelectionListener(CommitItem.Order order) {
			this.order = order;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			TableColumn column = (TableColumn)e.widget;
			Table table = column.getParent();

			if (column == table.getSortColumn()) {
				reversed = !reversed;
			} else {
				reversed = false;
			}
			table.setSortColumn(column);

			Comparator<CommitItem> comparator;
			if (reversed) {
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
			IStructuredSelection selection = (IStructuredSelection) filesViewer.getSelection();

			CommitItem commitItem = (CommitItem) selection.getFirstElement();
			if (commitItem == null) {
				return;
			}
			if (commitItem.status.equals(UIText.CommitDialog_StatusUntracked)) {
				return;
			}

			IProject project = commitItem.file.getProject();
			RepositoryMapping mapping = RepositoryMapping.getMapping(project);
			if (mapping == null) {
				return;
			}
			Repository repository = mapping.getRepository();

			try {
				ObjectId id = repository.resolve(Constants.HEAD);
				if (id == null
						|| repository.open(id, Constants.OBJ_COMMIT).getType() != Constants.OBJ_COMMIT) {
					return;
				}
			} catch (IOException e1) {
				return;
			}

			GitProvider provider = (GitProvider) RepositoryProvider.getProvider(project);
			GitFileHistoryProvider fileHistoryProvider = (GitFileHistoryProvider) provider.getFileHistoryProvider();

			IFileHistory fileHistory = fileHistoryProvider.getFileHistoryFor(commitItem.file, IFileHistoryProvider.SINGLE_REVISION, null);

			IFileRevision baseFile = fileHistory.getFileRevisions()[0];
			IFileRevision nextFile = fileHistoryProvider.getWorkspaceFileRevision(commitItem.file);

			ITypedElement base = new FileRevisionTypedElement(baseFile);
			ITypedElement next = new FileRevisionTypedElement(nextFile);

			GitCompareFileRevisionEditorInput input = new GitCompareFileRevisionEditorInput(next, base, null);
			CompareUI.openCompareDialog(input);
		}

	}

	@Override
	protected void okPressed() {
		commitMessage = commitText.getCommitMessage();
		author = authorText.getText().trim();
		committer = committerText.getText().trim();
		signedOff = signedOffButton.getSelection();
		amending = amendingButton.getSelection();

		Object[] checkedElements = filesViewer.getCheckedElements();
		selectedFiles.clear();
		for (Object obj : checkedElements)
			selectedFiles.add(((CommitItem) obj).file);

		if (commitMessage.trim().length() == 0) {
			MessageDialog.openWarning(getShell(), UIText.CommitDialog_ErrorNoMessage, UIText.CommitDialog_ErrorMustEnterCommitMessage);
			return;
		}

		boolean authorValid = false;
		if (author.length() > 0) {
			authorValid = RawParseUtils.parsePersonIdent(author) != null;
		}
		if (!authorValid) {
			MessageDialog.openWarning(getShell(), UIText.CommitDialog_ErrorInvalidAuthor, UIText.CommitDialog_ErrorInvalidAuthorSpecified);
			return;
		}

		boolean committerValid = false;
		if (committer.length() > 0) {
			committerValid = RawParseUtils.parsePersonIdent(committer)!=null;
		}
		if (!committerValid) {
			MessageDialog.openWarning(getShell(), UIText.CommitDialog_ErrorInvalidAuthor, UIText.CommitDialog_ErrorInvalidCommitterSpecified);
			return;
		}

		if (selectedFiles.isEmpty() && !amending) {
			MessageDialog.openWarning(getShell(), UIText.CommitDialog_ErrorNoItemsSelected, UIText.CommitDialog_ErrorNoItemsSelectedToBeCommitted);
			return;
		}

		authorHandler.updateProposals();
		committerHandler.updateProposals();

		IDialogSettings settings = org.eclipse.egit.ui.Activator
			.getDefault().getDialogSettings();
		settings.put(SHOW_UNTRACKED_PREF, showUntracked);
		super.okPressed();
	}

	/**
	 * Set the total list of changed resources, including additions and
	 * removals
	 *
	 * @param files potentially affected by a new commit
	 * @param indexDiffs IndexDiffs of the related repositories
	 */
	public void setFileList(ArrayList<IFile> files, Map<Repository, IndexDiff> indexDiffs) {
		items.clear();
		for (IFile file : files) {
			RepositoryMapping repositoryMapping = RepositoryMapping.getMapping(file.getProject());
			Repository repo = repositoryMapping.getRepository();
			String path = repositoryMapping.getRepoRelativePath(file);
			CommitItem item = new CommitItem();
			item.status = getFileStatus(path, indexDiffs.get(repo));
			item.file = file;
			items.add(item);
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.SELECT_ALL_ID == buttonId) {
			filesViewer.setAllChecked(true);
		}
		if (IDialogConstants.DESELECT_ALL_ID == buttonId) {
			filesViewer.setAllChecked(false);
		}
		super.buttonPressed(buttonId);
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
	 * Pre-set the previous author if amending the commit
	 *
	 * @param previousAuthor
	 */
	public void setPreviousAuthor(String previousAuthor) {
		this.previousAuthor = previousAuthor;
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
		this.signedOff = signedOff;
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
	 * Set the message from the previous commit for amending.
	 *
	 * @param string
	 */
	public void setPreviousCommitMessage(String string) {
		this.previousCommitMessage = string;
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

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	/**
	 * @return true if a Change-Id line for Gerrit should be created
	 */
	public boolean getCreateChangeId() {
		return createChangeId;
	}

	private void refreshChangeIdText() {
		createChangeId = changeIdButton.getSelection();
		String text = commitText.getText().replaceAll(Text.DELIMITER, "\n"); //$NON-NLS-1$
		if (createChangeId) {
			String changedText = ChangeIdUtil.insertId(text,
					originalChangeId != null ? originalChangeId : ObjectId.zeroId(), true);
			if (!text.equals(changedText)) {
				changedText = changedText.replaceAll("\n", Text.DELIMITER); //$NON-NLS-1$
				commitText.setText(changedText);
			}
		} else {
			int changeIdOffset = findOffsetOfChangeIdLine(text);
			if (changeIdOffset > 0) {
				int endOfChangeId = findNextEOL(changeIdOffset, text);
				String cleanedText = text.substring(0, changeIdOffset)
						+ text.substring(endOfChangeId);
				cleanedText = cleanedText.replaceAll("\n", Text.DELIMITER); //$NON-NLS-1$
				commitText.setText(cleanedText);
			}
		}
	}

}

class CommitItem {
	String status;

	IFile file;

	public static enum Order implements Comparator<CommitItem> {
		ByStatus() {

			public int compare(CommitItem o1, CommitItem o2) {
				return o1.status.compareTo(o2.status);
			}

		},

		ByFile() {

			public int compare(CommitItem o1, CommitItem o2) {
				return o1.file.getProjectRelativePath().toString().
					compareTo(o2.file.getProjectRelativePath().toString());
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
