/*******************************************************************************
 * Copyright (C) 2026, Eclipse EGit contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pullrequest;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.internal.bitbucket.BitbucketClient;
import org.eclipse.egit.core.internal.bitbucket.ChangedFile;
import org.eclipse.egit.core.internal.bitbucket.PullRequest;
import org.eclipse.egit.core.internal.bitbucket.PullRequestComment;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.ActionUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;

/**
 * View for displaying changed files in a pull request
 */
public class PullRequestChangedFilesView extends ViewPart {

	/**
	 * View ID
	 */
	public static final String VIEW_ID = "org.eclipse.egit.ui.PullRequestChangedFilesView"; //$NON-NLS-1$

	private FormToolkit toolkit;

	private Form form;

	private TreeViewer changedFilesViewer;

	private PullRequest selectedPullRequest;

	private List<PullRequestChangedFile> changedFiles = new ArrayList<>();

	private List<PullRequestComment> allComments = new ArrayList<>();

	private Repository gitRepository;

	private ISelectionListener prSelectionListener;

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(e -> toolkit.dispose());

		form = toolkit.createForm(parent);
		form.setText("Changed Files"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);
		toolkit.decorateFormHeading(form);
		GridLayoutFactory.fillDefaults().applyTo(form.getBody());

		TreeColumnLayout treeColumnLayout = new TreeColumnLayout();
		Composite layoutComposite = new Composite(form.getBody(), SWT.NONE);
		layoutComposite.setLayout(treeColumnLayout);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(layoutComposite);

		changedFilesViewer = new TreeViewer(layoutComposite,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		changedFilesViewer.getTree().setHeaderVisible(true);
		changedFilesViewer.getTree().setLinesVisible(true);

		setupColumns(treeColumnLayout);

		changedFilesViewer
				.setContentProvider(new PullRequestChangesContentProvider());
		changedFilesViewer.setInput(changedFiles);

		// Register as selection provider for view communication
		getSite().setSelectionProvider(changedFilesViewer);

		// Open compare editor on double-click
		changedFilesViewer
				.addDoubleClickListener(event -> {
					IStructuredSelection selection = (IStructuredSelection) event
							.getSelection();
					if (!selection.isEmpty()) {
						Object element = selection.getFirstElement();
						if (element instanceof PullRequestChangedFile) {
							openCompareEditor(
									(PullRequestChangedFile) element);
						}
					}
				});

		// Open compare editor on Enter key
		changedFilesViewer.getTree().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					IStructuredSelection selection = (IStructuredSelection) changedFilesViewer
							.getSelection();
					if (!selection.isEmpty()) {
						Object element = selection.getFirstElement();
						if (element instanceof PullRequestChangedFile) {
							openCompareEditor(
									(PullRequestChangedFile) element);
						}
					}
				}
			}
		});

		// Create context menu for changed files
		createChangedFilesPopupMenu(changedFilesViewer);

		// Listen for PR selection from PullRequestListView
		prSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (selection instanceof IStructuredSelection) {
					Object first = ((IStructuredSelection) selection)
							.getFirstElement();
					if (first instanceof PullRequest) {
						onPRSelected((PullRequest) first);
					}
				}
			}
		};
		getSite().getWorkbenchWindow().getSelectionService()
				.addSelectionListener(PullRequestListView.VIEW_ID,
						prSelectionListener);
	}

	private void setupColumns(TreeColumnLayout layout) {
		// File Column
		TreeViewerColumn fileColumn = createColumn(layout, "File", 60, //$NON-NLS-1$
				SWT.LEFT);
		fileColumn.setLabelProvider(new PullRequestChangesLabelProvider());

		// Change Type Column
		TreeViewerColumn changeColumn = createColumn(layout, "Change", 10, //$NON-NLS-1$
				SWT.LEFT);
		changeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequestChangedFile) {
					return ((PullRequestChangedFile) element).getChangeType()
							.toString();
				}
				return ""; //$NON-NLS-1$
			}
		});

		// Comments Column
		TreeViewerColumn commentsColumn = createColumn(layout, "Comments", 10, //$NON-NLS-1$
				SWT.CENTER);
		commentsColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequestChangedFile) {
					PullRequestChangedFile file = (PullRequestChangedFile) element;
					int count = getCommentCountForFile(file.getPath(),
							file.getSrcPath());
					return count > 0 ? String.valueOf(count) : ""; //$NON-NLS-1$
				} else if (element instanceof PullRequestFolderEntry) {
					PullRequestFolderEntry folder = (PullRequestFolderEntry) element;
					int count = getCommentCountForFolder(folder);
					return count > 0 ? String.valueOf(count) : ""; //$NON-NLS-1$
				}
				return ""; //$NON-NLS-1$
			}
		});

		// Path Column
		TreeViewerColumn pathColumn = createColumn(layout, "Path", 30, //$NON-NLS-1$
				SWT.LEFT);
		pathColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequestChangedFile) {
					return ((PullRequestChangedFile) element).getPath();
				} else if (element instanceof PullRequestFolderEntry) {
					return ((PullRequestFolderEntry) element).getPath()
							.toString();
				}
				return ""; //$NON-NLS-1$
			}
		});
	}

	private TreeViewerColumn createColumn(TreeColumnLayout layout, String text,
			int weight, int style) {
		TreeViewerColumn column = new TreeViewerColumn(changedFilesViewer,
				style);
		column.getColumn().setText(text);
		layout.setColumnData(column.getColumn(), new ColumnWeightData(weight));
		return column;
	}

	/**
	 * Attempts to resolve the local Git repository that matches the Bitbucket
	 * pull request.
	 *
	 * @param pr
	 *            the pull request
	 * @return the matching Git repository, or null if not found
	 */
	private Repository resolveGitRepository(PullRequest pr) {
		String serverUrl = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.BITBUCKET_SERVER_URL);
		String projectKey = pr.getToRef().getRepository().getProject()
				.getKey();
		String repoSlug = pr.getToRef().getRepository().getSlug();

		// Build expected path fragment: /scm/{projectKey}/{repoSlug}
		String pathFragment = "/scm/" + projectKey.toLowerCase() + "/" //$NON-NLS-1$ //$NON-NLS-2$
				+ repoSlug.toLowerCase();

		// Search all repositories in the workspace
		for (Repository repo : RepositoryCache.INSTANCE.getAllRepositories()) {
			try {
				for (RemoteConfig remote : RemoteConfig
						.getAllRemoteConfigs(repo.getConfig())) {
					for (URIish uri : remote.getURIs()) {
						String uriStr = uri.toString().toLowerCase();
						// Check if URI contains the server URL and path fragment
						if (uriStr.contains(
								serverUrl.toLowerCase().replaceAll("https?://", //$NON-NLS-1$
										"")) //$NON-NLS-1$
								&& uriStr.contains(pathFragment)) {
							return repo;
						}
					}
				}
			} catch (Exception e) {
				// Skip repos with config issues
			}
		}
		return null;
	}

	private void onPRSelected(PullRequest pr) {
		selectedPullRequest = pr;

		// Resolve the Git repository for this PR
		final Repository resolvedRepo = resolveGitRepository(pr);

		// Fetch changed files in a background job
		Job job = new Job("Fetching changed files") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Fetching changed files from Bitbucket", //$NON-NLS-1$
						IProgressMonitor.UNKNOWN);

				try {
					final String serverUrl = Activator.getDefault()
							.getPreferenceStore()
							.getString(UIPreferences.BITBUCKET_SERVER_URL);
					final String token = Activator.getDefault()
							.getPreferenceStore()
							.getString(UIPreferences.BITBUCKET_ACCESS_TOKEN);

					BitbucketClient client = new BitbucketClient(serverUrl,
							token);

					String projectKey = pr.getToRef().getRepository()
							.getProject().getKey();
					String repoSlug = pr.getToRef().getRepository().getSlug();

					// Fetch changed files
					String jsonResponse = client.getPullRequestChanges(
							projectKey, repoSlug, pr.getId());

					List<ChangedFile> apiChangedFiles = PullRequestJsonParser
							.parseChangedFiles(jsonResponse);

					final List<PullRequestChangedFile> uiChangedFiles = apiChangedFiles
							.stream()
							.map(PullRequestChangedFile::fromChangedFile)
							.collect(Collectors.toList());

					// Fetch pull request activities (including comments)
					final List<PullRequestComment> comments = new ArrayList<>();
					try {
						String activitiesJson = client
								.getPullRequestActivities(projectKey, repoSlug,
										pr.getId());
						comments.addAll(PullRequestJsonParser
								.parseActivities(activitiesJson));
					} catch (Exception e) {
						Activator.logError("Failed to fetch PR activities", e); //$NON-NLS-1$
					}

					Display.getDefault().asyncExec(() -> {
						if (!changedFilesViewer.getControl().isDisposed()) {
							changedFiles.clear();
							changedFiles.addAll(uiChangedFiles);
							allComments.clear();
							allComments.addAll(comments);

							// Set the resolved repository on the view and all files
							gitRepository = resolvedRepo;
							if (gitRepository != null) {
								for (PullRequestChangedFile file : changedFiles) {
									file.setRepository(gitRepository);
								}
							}

							changedFilesViewer.setInput(changedFiles);
							changedFilesViewer.refresh();
							updateFormTitle();
						}
					});

					return Status.OK_STATUS;
				} catch (IOException e) {
					Display.getDefault().asyncExec(() -> {
						if (!changedFilesViewer.getControl().isDisposed()) {
							MessageDialog.openError(
									changedFilesViewer.getControl().getShell(),
									"Error", //$NON-NLS-1$
									"Failed to fetch changed files: " //$NON-NLS-1$
											+ e.getMessage());
						}
					});
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Failed to fetch changed files", e); //$NON-NLS-1$
				} finally {
					monitor.done();
				}
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private void openCompareEditor(PullRequestChangedFile file) {
		if (selectedPullRequest == null) {
			return;
		}

		// Open compare editor in a background job
		Job job = new Job("Opening file comparison") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Preparing comparison", //$NON-NLS-1$
							IProgressMonitor.UNKNOWN);

					final String serverUrl = Activator.getDefault()
							.getPreferenceStore()
							.getString(UIPreferences.BITBUCKET_SERVER_URL);
					final String token = Activator.getDefault()
							.getPreferenceStore()
							.getString(UIPreferences.BITBUCKET_ACCESS_TOKEN);

					BitbucketClient client = new BitbucketClient(serverUrl,
							token);

					// Create compare editor input
					final BitbucketCompareEditorInput input = new BitbucketCompareEditorInput(
							client, selectedPullRequest, file);

					// Filter comments for this specific file
					List<PullRequestComment> fileComments = allComments.stream()
							.filter(comment -> {
								if (comment.getPath() == null) {
									return false;
								}
								String commentPath = comment.getPath();
								String filePath = file.getPath();
								String srcPath = file.getSrcPath();
								return commentPath.equals(filePath)
										|| (srcPath != null && commentPath.equals(srcPath));
							})
							.collect(Collectors.toList());

					// Set comments on the compare input
					input.setComments(fileComments);

					// Open compare editor in UI thread
					Display.getDefault().asyncExec(() -> {
						CompareUI.openCompareEditor(input);
					});

					return Status.OK_STATUS;
				} catch (Exception e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Failed to open comparison", e); //$NON-NLS-1$
				} finally {
					monitor.done();
				}
			}
		};
		job.setUser(false);
		job.schedule();
	}

	private void updateFormTitle() {
		if (selectedPullRequest != null) {
			form.setText(MessageFormat.format(
					"Changed Files - PR #{0}: {1}", //$NON-NLS-1$
					Long.valueOf(selectedPullRequest.getId()),
					selectedPullRequest.getTitle()));
		} else {
			form.setText("Changed Files"); //$NON-NLS-1$
		}
	}

	private int getCommentCountForFile(String filePath, String srcPath) {
		if (allComments == null || allComments.isEmpty()) {
			return 0;
		}

		return (int) allComments.stream().filter(comment -> {
			if (comment.getPath() == null) {
				return false;
			}
			String commentPath = comment.getPath();
			return commentPath.equals(filePath)
					|| (srcPath != null && commentPath.equals(srcPath));
		}).mapToLong(comment -> {
			long count = 1;
			if (comment.getReplies() != null) {
				count += comment.getReplies().size();
			}
			return count;
		}).sum();
	}

	private int getCommentCountForFolder(PullRequestFolderEntry folder) {
		if (allComments == null || allComments.isEmpty()) {
			return 0;
		}

		String folderPath = folder.getPath().toString();

		return (int) allComments.stream().filter(comment -> {
			if (comment.getPath() == null) {
				return false;
			}
			return comment.getPath().startsWith(folderPath);
		}).mapToLong(comment -> {
			long count = 1;
			if (comment.getReplies() != null) {
				count += comment.getReplies().size();
			}
			return count;
		}).sum();
	}

	/**
	 * Get all comments for the currently selected PR
	 *
	 * @return list of comments
	 */
	public List<PullRequestComment> getAllComments() {
		return allComments;
	}

	/**
	 * Get the currently selected pull request
	 *
	 * @return the selected pull request, or null
	 */
	public PullRequest getSelectedPullRequest() {
		return selectedPullRequest;
	}

	@Override
	public void setFocus() {
		changedFilesViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		if (prSelectionListener != null) {
			getSite().getWorkbenchWindow().getSelectionService()
					.removeSelectionListener(PullRequestListView.VIEW_ID,
							prSelectionListener);
		}
		if (toolkit != null) {
			toolkit.dispose();
		}
		super.dispose();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IShowInSource.class) {
			return (T) (IShowInSource) () -> getShowInContext();
		}
		return super.getAdapter(adapter);
	}

	/**
	 * Creates a ShowInContext for the current selection.
	 *
	 * @return the ShowInContext, or null if no valid selection
	 */
	private ShowInContext getShowInContext() {
		if (changedFilesViewer == null
				|| changedFilesViewer.getControl().isDisposed()) {
			return null;
		}

		IStructuredSelection selection = changedFilesViewer
				.getStructuredSelection();
		if (selection.isEmpty()) {
			return null;
		}

		// Collect workspace resources from selection
		List<IResource> resources = new ArrayList<>();
		for (Object element : selection.toArray()) {
			IResource resource = null;
			if (element instanceof PullRequestChangedFile) {
				resource = ((PullRequestChangedFile) element).getWorkspaceFile();
			} else if (element instanceof PullRequestFolderEntry) {
				resource = ((PullRequestFolderEntry) element).getContainer();
			}
			if (resource != null) {
				resources.add(resource);
			}
		}

		if (resources.isEmpty()) {
			return null;
		}

		return new ShowInContext(null, new StructuredSelection(resources));
	}

	/**
	 * Creates a context menu for the changed files tree viewer.
	 * Follows the pattern from StagingView.createPopupMenu().
	 *
	 * @param treeViewer
	 *            the tree viewer to add the popup menu to
	 */
	private void createChangedFilesPopupMenu(final TreeViewer treeViewer) {
		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		Control control = treeViewer.getControl();
		control.setMenu(menuMgr.createContextMenu(control));
		menuMgr.addMenuListener(new IMenuListener() {

			@Override
			public void menuAboutToShow(IMenuManager manager) {
				control.setFocus();
				final IStructuredSelection selection = treeViewer
						.getStructuredSelection();
				if (selection.isEmpty()) {
					return;
				}

				// Collect selected files
				List<PullRequestChangedFile> selectedFiles = new ArrayList<>();
				List<PullRequestFolderEntry> selectedFolders = new ArrayList<>();
				boolean onlyFoldersSelected = true;

				for (Object element : selection.toArray()) {
					if (element instanceof PullRequestFolderEntry) {
						selectedFolders.add((PullRequestFolderEntry) element);
					} else if (element instanceof PullRequestChangedFile) {
						onlyFoldersSelected = false;
						selectedFiles.add((PullRequestChangedFile) element);
					}
				}

				// "Open in Workspace" action - only for files that exist in workspace or filesystem
				if (!onlyFoldersSelected && !selectedFiles.isEmpty()) {
					Action openInWorkspaceAction = new Action(
							UIText.CommitFileDiffViewer_OpenWorkingTreeVersionInEditorMenuLabel,
							UIIcons.GOTO_INPUT) {
						@Override
						public void run() {
							openInWorkspace(selectedFiles);
						}
					};
					// Enable only if at least one file exists (not deleted) and can potentially be opened
					boolean anyOpenable = selectedFiles.stream()
							.anyMatch(f -> f.getChangeType() != PullRequestChangedFile.ChangeType.DELETED
									&& (f.getWorkspaceFile() != null || f.getLocation() != null));
					openInWorkspaceAction.setEnabled(anyOpenable);
					menuMgr.add(openInWorkspaceAction);
				}

				menuMgr.add(new Separator());

				// "Show In >" submenu
				menuMgr.add(createShowInMenu());

				// "Copy Path" action
				menuMgr.add(createCopyPathAction(treeViewer));
			}
		});
	}

	/**
	 * Creates the "Show In >" submenu.
	 *
	 * @return the show in menu contribution item
	 */
	private MenuManager createShowInMenu() {
		return UIUtils.createShowInMenu(getSite().getWorkbenchWindow());
	}

	/**
	 * Creates an action to copy the selected file/folder paths to the clipboard.
	 * Follows the pattern from StagingView.createSelectionPathCopyAction().
	 *
	 * @param viewer
	 *            the tree viewer
	 * @return the copy path action
	 */
	private IAction createCopyPathAction(final TreeViewer viewer) {
		IStructuredSelection selection = viewer.getStructuredSelection();
		String copyPathActionText = MessageFormat.format(
				UIText.StagingView_CopyPaths,
				Integer.valueOf(selection.size()));
		IAction copyAction = ActionUtils.createGlobalAction(ActionFactory.COPY,
				() -> copyPathToClipboard(viewer));
		copyAction.setText(copyPathActionText);
		return copyAction;
	}

	/**
	 * Copies the paths of the selected items to the clipboard.
	 *
	 * @param viewer
	 *            the tree viewer
	 */
	private void copyPathToClipboard(final TreeViewer viewer) {
		Clipboard cb = new Clipboard(viewer.getControl().getDisplay());
		try {
			TextTransfer t = TextTransfer.getInstance();
			String text = getPathsFromSelection(viewer.getStructuredSelection());
			if (text != null) {
				cb.setContents(new Object[] { text }, new Transfer[] { t });
			}
		} finally {
			cb.dispose();
		}
	}

	/**
	 * Gets the paths from the current selection as a string.
	 *
	 * @param selection
	 *            the current selection
	 * @return the paths as a string, or null if no valid paths
	 */
	private String getPathsFromSelection(IStructuredSelection selection) {
		Object[] selectionEntries = selection.toArray();
		if (selectionEntries.length <= 0) {
			return null;
		} else if (selectionEntries.length == 1) {
			return getPathFromElement(selectionEntries[0]);
		} else {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < selectionEntries.length; i++) {
				String text = getPathFromElement(selectionEntries[i]);
				if (text != null) {
					if (i < selectionEntries.length - 1) {
						sb.append(text).append(System.lineSeparator());
					} else {
						sb.append(text);
					}
				}
			}
			return sb.toString();
		}
	}

	/**
	 * Gets the path from a single element.
	 *
	 * @param element
	 *            the element (PullRequestChangedFile or PullRequestFolderEntry)
	 * @return the path string, or null
	 */
	private String getPathFromElement(Object element) {
		if (element instanceof PullRequestChangedFile) {
			PullRequestChangedFile file = (PullRequestChangedFile) element;
			// Return workspace location if available, otherwise repo-relative path
			IPath location = file.getLocation();
			if (location != null) {
				return location.toOSString();
			}
			return file.getPath();
		} else if (element instanceof PullRequestFolderEntry) {
			PullRequestFolderEntry folder = (PullRequestFolderEntry) element;
			// Return workspace location if available, otherwise folder path
			IPath location = folder.getLocation();
			if (location != null) {
				return location.toOSString();
			}
			return folder.getPath().toString();
		}
		return null;
	}

	/**
	 * Opens the selected files in the workspace editor. If a file cannot be
	 * found in the workspace, attempts to open it from the filesystem using
	 * the repository's working tree.
	 *
	 * @param files
	 *            the files to open
	 */
	private void openInWorkspace(List<PullRequestChangedFile> files) {
		int successCount = 0;
		int failureCount = 0;
		StringBuilder failedFiles = new StringBuilder();

		for (PullRequestChangedFile file : files) {
			// Skip deleted files
			if (file.getChangeType() == PullRequestChangedFile.ChangeType.DELETED) {
				continue;
			}

			boolean opened = false;

			// First try: workspace file
			IFile workspaceFile = file.getWorkspaceFile();
			if (workspaceFile != null && workspaceFile.exists()) {
				try {
					IDE.openEditor(getSite().getPage(), workspaceFile);
					opened = true;
					successCount++;
				} catch (PartInitException e) {
					Activator.logError("Failed to open file in editor: " //$NON-NLS-1$
							+ workspaceFile.getFullPath(), e);
				}
			}

			// Second try: filesystem fallback (if file is in repository working tree)
			if (!opened) {
				IPath location = file.getLocation();
				if (location != null) {
					java.io.File fsFile = location.toFile();
					if (fsFile.exists()) {
						try {
							org.eclipse.egit.ui.internal.EgitUiEditorUtils
									.openEditor(fsFile, getSite().getPage());
							opened = true;
							successCount++;
						} catch (Exception e) {
							Activator.logError(
									"Failed to open file from filesystem: " //$NON-NLS-1$
											+ location.toOSString(),
									e);
						}
					}
				}
			}

			// Track failures
			if (!opened) {
				failureCount++;
				if (failedFiles.length() > 0) {
					failedFiles.append("\n"); //$NON-NLS-1$
				}
				failedFiles.append(file.getPath());
			}
		}

		// Show error message if any files couldn't be opened
		if (failureCount > 0) {
			String message = MessageFormat.format(
					"Failed to open {0} file(s):\n\n{1}\n\nThe files may not exist in the workspace or repository working tree. The PR branch may need to be checked out locally.", //$NON-NLS-1$
					Integer.valueOf(failureCount), failedFiles.toString());
			Activator.showError(message, null);
		}
	}
}
