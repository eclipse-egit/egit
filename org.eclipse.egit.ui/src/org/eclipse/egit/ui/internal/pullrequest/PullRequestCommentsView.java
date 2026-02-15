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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.bitbucket.BitbucketClient;
import org.eclipse.egit.core.internal.bitbucket.PullRequest;
import org.eclipse.egit.core.internal.bitbucket.PullRequestComment;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

/**
 * View for displaying comments on pull request files. Supports showing comments
 * for a selected file or all comments for the entire PR. Includes a detail
 * panel for reading full comment text, and context menu actions for replying,
 * creating tasks, and resolving tasks.
 */
public class PullRequestCommentsView extends ViewPart {

	/**
	 * View ID
	 */
	public static final String VIEW_ID = "org.eclipse.egit.ui.PullRequestCommentsView"; //$NON-NLS-1$

	private FormToolkit toolkit;

	private Form form;

	private SashForm sashForm;

	private TreeViewer commentsViewer;

	private StyledText detailText;

	private Font boldFont;

	private PreferenceBasedDateFormatter dateFormatter;

	private List<PullRequestComment> allComments = new ArrayList<>();

	private PullRequestChangedFile selectedFile;

	private boolean showAllComments;

	private ISelectionListener fileSelectionListener;

	private ISelectionListener prSelectionListener;

	private Button replyButton;

	private Button taskButton;

	private Button resolveButton;

	private PullRequestComment selectedComment;

	// Fields for tracking highlighting state
	private Control lastHighlightedControl;

	private LineBackgroundListener lastListener;

	private Color highlightColor;

	// Fields for tracking collapsed comment threads
	private java.util.Set<Long> collapsedCommentIds = new java.util.HashSet<>();

	@Override
	public void createPartControl(Composite parent) {
		dateFormatter = PreferenceBasedDateFormatter.create();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(e -> toolkit.dispose());

		// Create highlight color (light yellow/amber)
		highlightColor = new Color(parent.getDisplay(), 255, 255, 180);
		parent.addDisposeListener(e -> {
			if (highlightColor != null && !highlightColor.isDisposed()) {
				highlightColor.dispose();
			}
		});

		form = toolkit.createForm(parent);
		form.setText("Comments"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);
		toolkit.decorateFormHeading(form);
		GridLayoutFactory.fillDefaults().applyTo(form.getBody());

		// Create toolbar with "Show All Comments" toggle
		setupToolbar();

		// SashForm: top = comment tree, bottom = detail panel
		sashForm = new SashForm(form.getBody(), SWT.VERTICAL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(sashForm);

		// Top part: tree viewer with columns
		Composite treeComposite = new Composite(sashForm, SWT.NONE);
		TreeColumnLayout treeColumnLayout = new TreeColumnLayout();
		treeComposite.setLayout(treeColumnLayout);

		commentsViewer = new TreeViewer(treeComposite,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		commentsViewer.getTree().setHeaderVisible(true);
		commentsViewer.getTree().setLinesVisible(true);

		// Enable native tooltip support for column tooltips
		ColumnViewerToolTipSupport.enableFor(commentsViewer);

		setupColumns(treeColumnLayout);

		commentsViewer.setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof List) {
					return ((List<?>) inputElement).toArray();
				}
				return new Object[0];
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof PullRequestComment) {
					PullRequestComment parentComment = (PullRequestComment) parentElement;
					// Return empty array if this comment is collapsed
					if (collapsedCommentIds.contains(parentComment.getId())) {
						return new Object[0];
					}
					List<PullRequestComment> replies = parentComment.getReplies();
					return replies != null ? replies.toArray() : new Object[0];
				}
				return new Object[0];
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof PullRequestComment) {
					List<PullRequestComment> replies = ((PullRequestComment) element)
							.getReplies();
					return replies != null && !replies.isEmpty();
				}
				return false;
			}
		});

		commentsViewer.setInput(new ArrayList<>());

		// Bottom part: button bar + detail panel
		Composite detailComposite = toolkit.createComposite(sashForm);
		GridLayoutFactory.fillDefaults().applyTo(detailComposite);

		// Button bar
		Composite buttonBar = toolkit.createComposite(detailComposite);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 5)
				.applyTo(buttonBar);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(buttonBar);

		replyButton = toolkit.createButton(buttonBar, "Reply", SWT.PUSH); //$NON-NLS-1$
		replyButton.setEnabled(false);
		replyButton.addListener(SWT.Selection, event -> {
			if (selectedComment != null) {
				replyToComment(selectedComment);
			}
		});

		taskButton = toolkit.createButton(buttonBar, "Create Task", SWT.PUSH); //$NON-NLS-1$
		taskButton.setEnabled(false);
		taskButton.addListener(SWT.Selection, event -> {
			if (selectedComment != null) {
				String severity = selectedComment.getSeverity();
				if ("BLOCKER".equals(severity)) { //$NON-NLS-1$
					updateSeverity(selectedComment, "NORMAL"); //$NON-NLS-1$
				} else {
					updateSeverity(selectedComment, "BLOCKER"); //$NON-NLS-1$
				}
			}
		});

		resolveButton = toolkit.createButton(buttonBar, "Resolve Task", //$NON-NLS-1$
				SWT.PUSH);
		resolveButton.setEnabled(false);
		resolveButton.addListener(SWT.Selection, event -> {
			if (selectedComment != null) {
				String state = selectedComment.getState();
				if ("RESOLVED".equals(state)) { //$NON-NLS-1$
					updateState(selectedComment, "OPEN"); //$NON-NLS-1$
				} else {
					updateState(selectedComment, "RESOLVED"); //$NON-NLS-1$
				}
			}
		});

		// Detail text
		detailText = new StyledText(detailComposite,
				SWT.BORDER | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(detailText);
		detailText.setEditable(false);
		detailText.setBackground(
				parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		// Create bold font for headers in detail panel
		FontData[] fontData = detailText.getFont().getFontData();
		for (FontData fd : fontData) {
			fd.setStyle(SWT.BOLD);
		}
		boldFont = new Font(parent.getDisplay(), fontData);
		parent.addDisposeListener(e -> boldFont.dispose());

		sashForm.setWeights(new int[] { 65, 35 });

		// Update detail panel and buttons when comment is selected in tree
		commentsViewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event
					.getSelection();
			if (!selection.isEmpty()) {
				Object element = selection.getFirstElement();
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					selectedComment = comment;
					updateDetailPanel(comment);
					updateButtonStates(comment);
					highlightCommentInCompareEditor(comment);
				}
			} else {
				selectedComment = null;
				detailText.setText(""); //$NON-NLS-1$
				updateButtonStates(null);
				clearHighlight();
			}
		});

		// Add double-click listener to toggle expand/collapse or jump to line
		commentsViewer.addDoubleClickListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event
					.getSelection();
			if (!selection.isEmpty()) {
				Object element = selection.getFirstElement();
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					// If comment has a line number, jump to it
					if (comment.getLine() != null && comment.getLine() > 0
							&& comment.getPath() != null) {
						jumpToCommentLine(comment);
					} else if (comment.getReplies() != null
							&& !comment.getReplies().isEmpty()) {
						// Otherwise, toggle expand/collapse if comment has replies
						toggleCommentCollapse(comment);
					}
				}
			}
		});

		// Context menu
		setupContextMenu();

		// Listen for file selection from PullRequestChangedFilesView
		fileSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (selection instanceof IStructuredSelection) {
					Object first = ((IStructuredSelection) selection)
							.getFirstElement();
					if (first instanceof PullRequestChangedFile) {
						onFileSelected((PullRequestChangedFile) first);
					}
				}
			}
		};
		getSite().getWorkbenchWindow().getSelectionService()
				.addSelectionListener(PullRequestChangedFilesView.VIEW_ID,
						fileSelectionListener);

		// Listen for PR selection from PullRequestListView
		prSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (selection instanceof IStructuredSelection) {
					Object first = ((IStructuredSelection) selection)
							.getFirstElement();
					if (first instanceof PullRequest) {
						onPRSelected();
					}
				}
			}
		};
		getSite().getWorkbenchWindow().getSelectionService()
				.addSelectionListener(PullRequestListView.VIEW_ID,
						prSelectionListener);
	}

	private void setupToolbar() {
		Action showAllAction = new Action("Show All Comments", //$NON-NLS-1$
				IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				showAllComments = isChecked();
				refreshComments();
			}
		};
		showAllAction.setToolTipText(
				"Toggle between file-specific and all PR comments"); //$NON-NLS-1$

		form.getToolBarManager().add(showAllAction);
		form.getToolBarManager().update(true);
	}

	private void setupContextMenu() {
		MenuManager menuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		Menu menu = menuManager.createContextMenu(commentsViewer.getControl());
		commentsViewer.getControl().setMenu(menu);
	}

	private void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = commentsViewer
				.getStructuredSelection();
		if (selection.isEmpty()) {
			return;
		}
		Object element = selection.getFirstElement();
		if (!(element instanceof PullRequestComment)) {
			return;
		}

		PullRequestComment comment = (PullRequestComment) element;

		// Reply action
		manager.add(new Action("Reply...") { //$NON-NLS-1$
			@Override
			public void run() {
				replyToComment(comment);
			}
		});

		manager.add(new Separator());

		// Create Task action (set severity to BLOCKER)
		String severity = comment.getSeverity();
		if (severity == null || "NORMAL".equals(severity)) { //$NON-NLS-1$
			manager.add(new Action("Create Task") { //$NON-NLS-1$
				@Override
				public void run() {
					updateSeverity(comment, "BLOCKER"); //$NON-NLS-1$
				}
			});
		} else if ("BLOCKER".equals(severity)) { //$NON-NLS-1$
			manager.add(new Action("Remove Task") { //$NON-NLS-1$
				@Override
				public void run() {
					updateSeverity(comment, "NORMAL"); //$NON-NLS-1$
				}
			});
		}

		// Resolve / Reopen Task action
		String state = comment.getState();
		if ("BLOCKER".equals(severity)) { //$NON-NLS-1$
			if (state == null || "OPEN".equals(state)) { //$NON-NLS-1$
				manager.add(new Action("Resolve Task") { //$NON-NLS-1$
					@Override
					public void run() {
						updateState(comment, "RESOLVED"); //$NON-NLS-1$
					}
				});
			} else if ("RESOLVED".equals(state)) { //$NON-NLS-1$
				manager.add(new Action("Reopen Task") { //$NON-NLS-1$
					@Override
					public void run() {
						updateState(comment, "OPEN"); //$NON-NLS-1$
					}
				});
			}
		}
	}

	private void replyToComment(PullRequestComment comment) {
		MultiLineInputDialog dialog = new MultiLineInputDialog(
				getSite().getShell(),
				"Reply to Comment", //$NON-NLS-1$
				"Enter your reply:", //$NON-NLS-1$
				""); //$NON-NLS-1$
		if (dialog.open() != Window.OK) {
			return;
		}

		String replyText = dialog.getValue();
		if (replyText == null || replyText.trim().isEmpty()) {
			return;
		}

		PullRequest pr = getSelectedPullRequest();
		if (pr == null) {
			return;
		}

		Job job = new Job("Posting reply") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					BitbucketClient client = createClient();
					String projectKey = pr.getToRef().getRepository()
							.getProject().getKey();
					String repoSlug = pr.getToRef().getRepository().getSlug();

					client.addPullRequestComment(projectKey, repoSlug,
							pr.getId(), replyText, comment.getId());

					// Refresh comments after posting
					refreshCommentsFromServer(pr);
					return Status.OK_STATUS;
				} catch (IOException e) {
					Activator.logError("Failed to post reply", e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Failed to post reply: " + e.getMessage(), e); //$NON-NLS-1$
				}
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private void updateSeverity(PullRequestComment comment, String severity) {
		PullRequest pr = getSelectedPullRequest();
		if (pr == null) {
			return;
		}

		Job job = new Job("Updating comment severity") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					BitbucketClient client = createClient();
					String projectKey = pr.getToRef().getRepository()
							.getProject().getKey();
					String repoSlug = pr.getToRef().getRepository().getSlug();

					client.updateCommentSeverity(projectKey, repoSlug,
							pr.getId(), comment.getId(),
							comment.getVersion(), severity);

					refreshCommentsFromServer(pr);
					return Status.OK_STATUS;
				} catch (IOException e) {
					Activator.logError("Failed to update severity", e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Failed to update severity: " + e.getMessage(), //$NON-NLS-1$
							e);
				}
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private void updateState(PullRequestComment comment, String state) {
		PullRequest pr = getSelectedPullRequest();
		if (pr == null) {
			return;
		}

		Job job = new Job("Updating comment state") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					BitbucketClient client = createClient();
					String projectKey = pr.getToRef().getRepository()
							.getProject().getKey();
					String repoSlug = pr.getToRef().getRepository().getSlug();

					client.updateCommentState(projectKey, repoSlug,
							pr.getId(), comment.getId(),
							comment.getVersion(), state);

					refreshCommentsFromServer(pr);
					return Status.OK_STATUS;
				} catch (IOException e) {
					Activator.logError("Failed to update state", e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Failed to update state: " + e.getMessage(), e); //$NON-NLS-1$
				}
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private void refreshCommentsFromServer(PullRequest pr) {
		try {
			BitbucketClient client = createClient();
			String projectKey = pr.getToRef().getRepository().getProject()
					.getKey();
			String repoSlug = pr.getToRef().getRepository().getSlug();

			String activitiesJson = client.getPullRequestActivities(projectKey,
					repoSlug, pr.getId());
			List<PullRequestComment> freshComments = PullRequestJsonParser
					.parseActivities(activitiesJson);

			Display.getDefault().asyncExec(() -> {
				if (!commentsViewer.getControl().isDisposed()) {
					allComments.clear();
					allComments.addAll(freshComments);
					refreshComments();
				}
			});
		} catch (IOException e) {
			Activator.logError("Failed to refresh comments", e); //$NON-NLS-1$
		}
	}

	private BitbucketClient createClient() {
		String serverUrl = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.BITBUCKET_SERVER_URL);
		String token = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.BITBUCKET_ACCESS_TOKEN);
		return new BitbucketClient(serverUrl, token);
	}

	private PullRequest getSelectedPullRequest() {
		IWorkbenchPart part = getSite().getWorkbenchWindow().getActivePage()
				.findView(PullRequestChangedFilesView.VIEW_ID);
		if (part instanceof PullRequestChangedFilesView) {
			return ((PullRequestChangedFilesView) part)
					.getSelectedPullRequest();
		}
		return null;
	}

	private void setupColumns(TreeColumnLayout layout) {
		// State column (OPEN/RESOLVED indicator)
		TreeViewerColumn stateCol = new TreeViewerColumn(commentsViewer,
				SWT.CENTER);
		stateCol.getColumn().setText("State"); //$NON-NLS-1$
		layout.setColumnData(stateCol.getColumn(), new ColumnWeightData(8));
		stateCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					String severity = comment.getSeverity();
					if ("BLOCKER".equals(severity)) { //$NON-NLS-1$
						String state = comment.getState();
						if ("RESOLVED".equals(state)) { //$NON-NLS-1$
							return "\u2611"; // ballot box with check //$NON-NLS-1$
						}
						return "\u2610"; // empty ballot box //$NON-NLS-1$
					}
				}
				return ""; //$NON-NLS-1$
			}

			@Override
			public Color getForeground(Object element) {
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					if ("RESOLVED".equals(comment.getState())) { //$NON-NLS-1$
						return Display.getDefault()
								.getSystemColor(SWT.COLOR_DARK_GREEN);
					}
				}
				return null;
			}
		});

		// Line column
		TreeViewerColumn lineCol = new TreeViewerColumn(commentsViewer,
				SWT.LEFT);
		lineCol.getColumn().setText("Line"); //$NON-NLS-1$
		layout.setColumnData(lineCol.getColumn(), new ColumnWeightData(7));
		lineCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					if (comment.isGeneralComment()) {
						return "General"; //$NON-NLS-1$
					} else if (comment.isFileLevelComment()) {
						return "File"; //$NON-NLS-1$
					} else if (comment.getLine() != null) {
						return String.valueOf(comment.getLine());
					}
				}
				return ""; //$NON-NLS-1$
			}
		});

		// Author column
		TreeViewerColumn authorCol = new TreeViewerColumn(commentsViewer,
				SWT.LEFT);
		authorCol.getColumn().setText("Author"); //$NON-NLS-1$
		layout.setColumnData(authorCol.getColumn(), new ColumnWeightData(12));
		authorCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					return comment.getAuthorDisplayName() != null
							? comment.getAuthorDisplayName()
							: comment.getAuthorName();
				}
				return ""; //$NON-NLS-1$
			}
		});

		// Comment column (with tooltip showing full text)
		TreeViewerColumn commentCol = new TreeViewerColumn(commentsViewer,
				SWT.LEFT);
		commentCol.getColumn().setText("Comment"); //$NON-NLS-1$
		layout.setColumnData(commentCol.getColumn(), new ColumnWeightData(48));
		commentCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					String text = comment.getText();
					if (text != null) {
						StringBuilder displayText = new StringBuilder();

						// Calculate depth for indentation
						int depth = getCommentDepth(comment, allComments);

						// Add indentation and reply indicator for nested comments
						if (depth > 0) {
							// Add spaces for depth (2 spaces per level)
							for (int i = 0; i < depth; i++) {
								displayText.append("  "); //$NON-NLS-1$
							}
							// Add reply arrow
							displayText.append("\u21B3 "); //$NON-NLS-1$ - arrow pointing down-right
						}

						// Add expand/collapse indicator for comments with replies
						List<PullRequestComment> replies = comment.getReplies();
						if (replies != null && !replies.isEmpty()) {
							boolean isCollapsed = collapsedCommentIds
									.contains(comment.getId());
							if (isCollapsed) {
								displayText.append("\u25B8 "); //$NON-NLS-1$ - right-pointing triangle (collapsed)
							} else {
								displayText.append("\u25BE "); //$NON-NLS-1$ - down-pointing triangle (expanded)
							}
						}

						// Prefix with severity indicator for tasks
						if ("BLOCKER".equals(comment.getSeverity())) { //$NON-NLS-1$
							displayText.append("[TASK] "); //$NON-NLS-1$
						}

						// Add the comment text (first line only)
						int newlineIndex = text.indexOf('\n');
						if (newlineIndex > 0) {
							displayText.append(text.substring(0, newlineIndex));
							displayText.append("..."); //$NON-NLS-1$
						} else {
							displayText.append(text);
						}

						// Add reply count for comments with replies
						if (replies != null && !replies.isEmpty()) {
							int replyCount = countAllReplies(comment);
							displayText.append(" ("); //$NON-NLS-1$
							displayText.append(replyCount);
							displayText
									.append(replyCount == 1 ? " reply)" : " replies)"); //$NON-NLS-1$ //$NON-NLS-2$
						}

						return displayText.toString();
					}
				}
				return ""; //$NON-NLS-1$
			}

			@Override
			public String getToolTipText(Object element) {
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					return comment.getText();
				}
				return null;
			}

			@Override
			public Color getForeground(Object element) {
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					if ("RESOLVED".equals(comment.getState())) { //$NON-NLS-1$
						return Display.getDefault()
								.getSystemColor(SWT.COLOR_GRAY);
					}
					// Lighter color for reply comments
					int depth = getCommentDepth(comment, allComments);
					if (depth > 0) {
						return Display.getDefault()
								.getSystemColor(SWT.COLOR_DARK_GRAY);
					}
				}
				return null;
			}

			@Override
			public Color getBackground(Object element) {
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					// Light gray background for replies
					int depth = getCommentDepth(comment, allComments);
					if (depth > 0) {
						return Display.getDefault()
								.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
					}
				}
				return null;
			}
		});

		// File path column (visible in "show all" mode)
		TreeViewerColumn pathCol = new TreeViewerColumn(commentsViewer,
				SWT.LEFT);
		pathCol.getColumn().setText("File"); //$NON-NLS-1$
		layout.setColumnData(pathCol.getColumn(), new ColumnWeightData(15));
		pathCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					String path = comment.getPath();
					if (path != null) {
						int lastSlash = path.lastIndexOf('/');
						if (lastSlash >= 0) {
							return path.substring(lastSlash + 1);
						}
						return path;
					}
					if (comment.isGeneralComment()) {
						return "PR"; //$NON-NLS-1$
					}
				}
				return ""; //$NON-NLS-1$
			}

			@Override
			public String getToolTipText(Object element) {
				if (element instanceof PullRequestComment) {
					return ((PullRequestComment) element).getPath();
				}
				return null;
			}
		});

		// Date column
		TreeViewerColumn dateCol = new TreeViewerColumn(commentsViewer,
				SWT.LEFT);
		dateCol.getColumn().setText("Date"); //$NON-NLS-1$
		layout.setColumnData(dateCol.getColumn(), new ColumnWeightData(10));
		dateCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					if (comment.getCreatedDate() != null) {
						return dateFormatter
								.formatDate(comment.getCreatedDate());
					}
				}
				return ""; //$NON-NLS-1$
			}
		});
	}

	private void updateDetailPanel(PullRequestComment comment) {
		detailText.setText(""); //$NON-NLS-1$

		List<StyleRange> styles = new ArrayList<>();
		StringBuilder sb = new StringBuilder();

		// Author and date header
		String author = comment.getAuthorDisplayName() != null
				? comment.getAuthorDisplayName()
				: comment.getAuthorName();
		if (author != null) {
			int start = sb.length();
			sb.append(author);
			styles.add(createBoldRange(start, author.length()));
		}

		if (comment.getCreatedDate() != null) {
			sb.append("  \u00B7  "); //$NON-NLS-1$ - middle dot separator
			sb.append(dateFormatter.formatDate(comment.getCreatedDate()));
		}

		// Severity / State info
		String severity = comment.getSeverity();
		String state = comment.getState();
		if ("BLOCKER".equals(severity)) { //$NON-NLS-1$
			sb.append("  [TASK"); //$NON-NLS-1$
			if ("RESOLVED".equals(state)) { //$NON-NLS-1$
				sb.append(" - RESOLVED"); //$NON-NLS-1$
			}
			sb.append("]"); //$NON-NLS-1$
		}

		sb.append("\n"); //$NON-NLS-1$

		// File / line info
		if (comment.getPath() != null) {
			sb.append(comment.getPath());
			if (comment.getLine() != null) {
				sb.append(":").append(comment.getLine()); //$NON-NLS-1$
			}
			sb.append("\n"); //$NON-NLS-1$
		}

		sb.append("\n"); //$NON-NLS-1$

		// Full comment text
		String text = comment.getText();
		if (text != null) {
			sb.append(text);
		}

		detailText.setText(sb.toString());
		for (StyleRange style : styles) {
			detailText.setStyleRange(style);
		}
	}

	private StyleRange createBoldRange(int start, int length) {
		StyleRange range = new StyleRange();
		range.start = start;
		range.length = length;
		range.font = boldFont;
		return range;
	}

	private void updateButtonStates(PullRequestComment comment) {
		if (comment == null) {
			replyButton.setEnabled(false);
			taskButton.setEnabled(false);
			resolveButton.setEnabled(false);
			return;
		}

		// Reply button always enabled for any comment
		replyButton.setEnabled(true);

		// Task button - can toggle between NORMAL and BLOCKER
		String severity = comment.getSeverity();
		if ("BLOCKER".equals(severity)) { //$NON-NLS-1$
			taskButton.setText("Remove Task"); //$NON-NLS-1$
			taskButton.setEnabled(true);
		} else {
			taskButton.setText("Create Task"); //$NON-NLS-1$
			taskButton.setEnabled(true);
		}

		// Resolve button - only visible/enabled for BLOCKER tasks
		if ("BLOCKER".equals(severity)) { //$NON-NLS-1$
			String state = comment.getState();
			if ("RESOLVED".equals(state)) { //$NON-NLS-1$
				resolveButton.setText("Reopen Task"); //$NON-NLS-1$
			} else {
				resolveButton.setText("Resolve Task"); //$NON-NLS-1$
			}
			resolveButton.setVisible(true);
			resolveButton.setEnabled(true);
		} else {
			resolveButton.setVisible(false);
			resolveButton.setEnabled(false);
		}
	}

	private void onPRSelected() {
		// When a new PR is selected, clear file selection and reset
		selectedFile = null;
		allComments.clear();
		refreshComments();
	}

	private void onFileSelected(PullRequestChangedFile file) {
		selectedFile = file;

		// Get comments from PullRequestChangedFilesView
		IWorkbenchPart part = getSite().getWorkbenchWindow().getActivePage()
				.findView(PullRequestChangedFilesView.VIEW_ID);
		if (part instanceof PullRequestChangedFilesView) {
			PullRequestChangedFilesView filesView = (PullRequestChangedFilesView) part;
			allComments = filesView.getAllComments();
		}

		refreshComments();
	}

	private void refreshComments() {
		List<PullRequestComment> displayComments;

		if (showAllComments) {
			// Fetch latest from the files view if needed
			if (allComments.isEmpty()) {
				IWorkbenchPart part = getSite().getWorkbenchWindow()
						.getActivePage()
						.findView(PullRequestChangedFilesView.VIEW_ID);
				if (part instanceof PullRequestChangedFilesView) {
					allComments = ((PullRequestChangedFilesView) part)
							.getAllComments();
				}
			}
			displayComments = new ArrayList<>(allComments);
		} else if (selectedFile != null) {
			displayComments = allComments.stream().filter(comment -> {
				if (comment.getPath() == null) {
					return false;
				}
				String commentPath = comment.getPath();
				String filePath = selectedFile.getPath();
				String srcPath = selectedFile.getSrcPath();
				return commentPath.equals(filePath)
						|| (srcPath != null && commentPath.equals(srcPath));
			}).collect(Collectors.toList());
		} else {
			displayComments = new ArrayList<>();
		}

		Display.getDefault().asyncExec(() -> {
			if (!commentsViewer.getControl().isDisposed()) {
				commentsViewer.setInput(displayComments);
				commentsViewer.refresh();
				updateFormTitle(displayComments.size());
			}
		});
	}

	private void updateFormTitle(int commentCount) {
		if (showAllComments) {
			form.setText("All Comments (" + commentCount + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (selectedFile != null) {
			form.setText("Comments (" + commentCount + ") - " //$NON-NLS-1$ //$NON-NLS-2$
					+ selectedFile.getName());
		} else {
			form.setText("Comments"); //$NON-NLS-1$
		}
	}

	/**
	 * Jumps to a comment's line in the compare editor. If the compare editor is
	 * not open, this method attempts to open it first.
	 *
	 * @param comment
	 *            the comment to jump to
	 */
	private void jumpToCommentLine(PullRequestComment comment) {
		if (comment == null || comment.getLine() == null
				|| comment.getPath() == null) {
			return;
		}

		// Try to find an open compare editor for this file
		IWorkbenchPage page = getSite().getWorkbenchWindow().getActivePage();
		if (page == null) {
			return;
		}

		boolean editorFound = false;
		IEditorReference[] editorRefs = page.getEditorReferences();
		for (IEditorReference ref : editorRefs) {
			try {
				IEditorInput input = ref.getEditorInput();
				if (input instanceof BitbucketCompareEditorInput) {
					BitbucketCompareEditorInput compareInput = (BitbucketCompareEditorInput) input;
					PullRequestChangedFile changedFile = compareInput
							.getChangedFile();

					// Match the file path
					if (changedFile != null && comment.getPath() != null) {
						String commentPath = comment.getPath();
						String filePath = changedFile.getPath();
						String srcPath = changedFile.getSrcPath();

						boolean pathMatches = commentPath.equals(filePath)
								|| (srcPath != null
										&& commentPath.equals(srcPath));

						if (pathMatches) {
							// Activate the editor
							IEditorPart editor = ref.getEditor(true);
							if (editor != null) {
								page.activate(editor);
								editorFound = true;
								// Highlight the line
								highlightCommentInCompareEditor(comment);
								break;
							}
						}
					}
				}
			} catch (Exception e) {
				// Ignore and continue to next editor
				continue;
			}
		}

		// If no editor was found, we can't open one here (would need access to
		// the BitbucketClient and PR info, which is in ChangedFilesView)
		// For now, just highlight if the editor is already open
	}

	/**
	 * Highlights a comment line in the compare editor by finding the matching
	 * editor and applying a LineBackgroundListener to color the line.
	 *
	 * @param comment
	 *            the comment to highlight
	 */
	private void highlightCommentInCompareEditor(PullRequestComment comment) {
		if (comment == null || comment.getLine() == null
				|| comment.getPath() == null) {
			clearHighlight();
			return;
		}

		// Clear previous highlight
		clearHighlight();

		// Find open compare editors
		IWorkbenchPage page = getSite().getWorkbenchWindow().getActivePage();
		if (page == null) {
			return;
		}

		IEditorReference[] editorRefs = page.getEditorReferences();
		for (IEditorReference ref : editorRefs) {
			try {
				IEditorInput input = ref.getEditorInput();
				if (input instanceof BitbucketCompareEditorInput) {
					BitbucketCompareEditorInput compareInput = (BitbucketCompareEditorInput) input;
					PullRequestChangedFile changedFile = compareInput
							.getChangedFile();

					// Match the file path
					if (changedFile != null && comment.getPath() != null) {
						String commentPath = comment.getPath();
						String filePath = changedFile.getPath();
						String srcPath = changedFile.getSrcPath();

						boolean pathMatches = commentPath.equals(filePath)
								|| (srcPath != null
										&& commentPath.equals(srcPath));

						if (pathMatches) {
							System.out.println(
									"[HighlightComment] Found matching editor for path: " //$NON-NLS-1$
											+ commentPath);
							// Get the editor part - use true to restore it if
							// needed
							IEditorPart editor = ref.getEditor(true);
							if (editor == null) {
								System.out.println(
										"[HighlightComment] Editor is null, skipping"); //$NON-NLS-1$
								continue;
							}

							System.out.println(
									"[HighlightComment] Editor: " + editor); //$NON-NLS-1$

							// Get the control directly from the editor
							Control viewerControl = findEditorControl(editor,
									compareInput);

							if (viewerControl != null
									&& !viewerControl.isDisposed()) {
								System.out.println(
										"[HighlightComment] Found control: " //$NON-NLS-1$
												+ viewerControl);
								// Traverse widget tree to find StyledText
								// controls
								highlightLineInControl(viewerControl, comment);
								return; // Found and highlighted
							} else {
								System.out.println(
										"[HighlightComment] No valid control found"); //$NON-NLS-1$
							}
						}
					}
				}
			} catch (Exception e) {
				// Ignore and continue to next editor
				continue;
			}
		}
	}

	/**
	 * Finds the control for an editor that contains the compare viewer's
	 * StyledText widgets.
	 *
	 * @param editor
	 *            the editor part
	 * @param compareInput
	 *            the compare input
	 * @return the control containing StyledText widgets, or null if not found
	 */
	private Control findEditorControl(IEditorPart editor,
			BitbucketCompareEditorInput compareInput) {
		// First try: get from BitbucketCompareEditorInput directly
		Control viewerControl = compareInput.getViewerControl();
		if (viewerControl != null && !viewerControl.isDisposed()) {
			return viewerControl;
		}

		// Second try: get the editor's underlying control via adapter
		Object adapter = editor.getAdapter(Control.class);
		if (adapter instanceof Control) {
			Control control = (Control) adapter;
			if (!control.isDisposed()) {
				List<StyledText> styledTexts = new ArrayList<>();
				collectStyledTexts(control, styledTexts);
				if (styledTexts.size() >= 2) {
					return control;
				}
			}
		}

		// Third try: get Composite adapter
		adapter = editor.getAdapter(Composite.class);
		if (adapter instanceof Composite) {
			Composite composite = (Composite) adapter;
			if (!composite.isDisposed()) {
				List<StyledText> styledTexts = new ArrayList<>();
				collectStyledTexts(composite, styledTexts);
				if (styledTexts.size() >= 2) {
					return composite;
				}
			}
		}

		// Fourth try: traverse from the editor's site shell, but be careful to
		// find the right editor's control
		if (editor.getSite() != null) {
			Shell shell = editor.getSite().getShell();
			if (shell != null && !shell.isDisposed()) {
				// Find a composite that contains at least 2 StyledText widgets
				// This is characteristic of a compare editor
				return findCompareEditorControl(shell);
			}
		}

		return null;
	}

	/**
	 * Finds a composite that contains at least 2 StyledText widgets,
	 * characteristic of a compare editor. Prefers the deepest/smallest
	 * composite to avoid getting a parent container.
	 *
	 * @param parent
	 *            the parent control to search
	 * @return the compare editor's content control, or null if not found
	 */
	private Control findCompareEditorControl(Control parent) {
		if (parent == null || parent.isDisposed()) {
			return null;
		}

		if (parent instanceof Composite) {
			Composite composite = (Composite) parent;

			// First recursively search children to find the deepest match
			for (Control child : composite.getChildren()) {
				Control found = findCompareEditorControl(child);
				if (found != null) {
					return found; // Return the deepest match first
				}
			}

			// If no child matched, check if this composite itself matches
			List<StyledText> styledTexts = new ArrayList<>();
			collectStyledTexts(composite, styledTexts);

			// A compare editor typically has 2 StyledText widgets (left and
			// right panes) or 3 (with ancestor pane)
			if (styledTexts.size() >= 2 && styledTexts.size() <= 3) {
				System.out.println(
						"[FindCompareEditor] Found composite with " //$NON-NLS-1$
								+ styledTexts.size() + " StyledText widgets: " //$NON-NLS-1$
								+ composite);
				return composite;
			}
		}

		return null;
	}

	/**
	 * Recursively traverses the control tree to find StyledText widgets and
	 * applies highlighting to the appropriate side (left = FROM, right = TO).
	 *
	 * @param control
	 *            the control to search
	 * @param comment
	 *            the comment containing line number and file type
	 */
	private void highlightLineInControl(Control control,
			PullRequestComment comment) {
		if (control == null || control.isDisposed()) {
			return;
		}

		// First, collect all StyledText widgets
		List<StyledText> styledTexts = new ArrayList<>();
		collectStyledTexts(control, styledTexts);

		if (styledTexts.isEmpty()) {
			return;
		}

		// Determine which StyledText to highlight based on fileType
		// "FROM" = left side (ancestor/old) = first StyledText
		// "TO" = right side (new) = second StyledText
		String fileType = comment.getFileType();
		StyledText targetText = null;

		if ("FROM".equals(fileType) && styledTexts.size() >= 1) { //$NON-NLS-1$
			targetText = styledTexts.get(0); // Left side
		} else if ("TO".equals(fileType) && styledTexts.size() >= 2) { //$NON-NLS-1$
			targetText = styledTexts.get(1); // Right side
		} else if (styledTexts.size() == 1) {
			// Single pane editor - use the only available pane
			targetText = styledTexts.get(0);
		}

		if (targetText == null || targetText.isDisposed()) {
			return;
		}

		int targetLine = comment.getLine().intValue();
		int lineCount = targetText.getLineCount();

		if (targetLine > 0 && targetLine <= lineCount) {
			// Create and apply line background listener
			final StyledText finalTargetText = targetText;
			LineBackgroundListener listener = new LineBackgroundListener() {
				@Override
				public void lineGetBackground(LineBackgroundEvent event) {
					int lineIndex = finalTargetText
							.getLineAtOffset(event.lineOffset);
					int displayLine = lineIndex + 1; // Convert to 1-based

					if (displayLine == targetLine) {
						event.lineBackground = highlightColor;
					}
				}
			};

			targetText.addLineBackgroundListener(listener);
			targetText.redraw();

			// Scroll to the line
			int lineIndex = targetLine - 1; // Convert to 0-based
			try {
				int offset = targetText.getOffsetAtLine(lineIndex);
				targetText.setSelection(offset);
				targetText.showSelection();
				targetText.setTopIndex(Math.max(0, lineIndex - 5)); // Show context
			} catch (IllegalArgumentException e) {
				// Line doesn't exist, ignore
			}

			// Track this control and listener for cleanup
			lastHighlightedControl = targetText;
			lastListener = listener;
		}
	}

	/**
	 * Recursively collects all StyledText widgets from a control tree.
	 *
	 * @param control
	 *            the control to search
	 * @param result
	 *            the list to add found StyledText widgets to
	 */
	private void collectStyledTexts(Control control, List<StyledText> result) {
		if (control == null || control.isDisposed()) {
			return;
		}

		if (control instanceof StyledText) {
			result.add((StyledText) control);
		} else if (control instanceof Composite) {
			Composite composite = (Composite) control;
			for (Control child : composite.getChildren()) {
				collectStyledTexts(child, result);
			}
		}
	}

	/**
	 * Clears the current highlight by removing the LineBackgroundListener from
	 * the last highlighted control.
	 */
	private void clearHighlight() {
		if (lastHighlightedControl != null
				&& !lastHighlightedControl.isDisposed()
				&& lastListener != null) {
			if (lastHighlightedControl instanceof StyledText) {
				StyledText styledText = (StyledText) lastHighlightedControl;
				styledText.removeLineBackgroundListener(lastListener);
				styledText.redraw();
			}
		}
		lastHighlightedControl = null;
		lastListener = null;
	}

	/**
	 * Recursively counts all replies (direct and nested) for a comment
	 *
	 * @param comment
	 *            the comment to count replies for
	 * @return total number of replies at all depth levels
	 */
	private int countAllReplies(PullRequestComment comment) {
		List<PullRequestComment> replies = comment.getReplies();
		if (replies == null || replies.isEmpty()) {
			return 0;
		}
		int count = replies.size();
		for (PullRequestComment reply : replies) {
			count += countAllReplies(reply);
		}
		return count;
	}

	/**
	 * Gets the tree depth level of a comment by walking up its parent chain
	 *
	 * @param comment
	 *            the comment to get depth for
	 * @param allComments
	 *            list of all top-level comments to search
	 * @return depth level (0 for top-level, 1 for direct replies, etc.)
	 */
	private int getCommentDepth(PullRequestComment comment,
			List<PullRequestComment> allComments) {
		// Try to find parent by walking the tree
		for (PullRequestComment topLevel : allComments) {
			int depth = findCommentDepth(comment, topLevel, 0);
			if (depth >= 0) {
				return depth;
			}
		}
		return 0; // Default to top-level if not found
	}

	/**
	 * Recursive helper to find comment depth in tree
	 */
	private int findCommentDepth(PullRequestComment target,
			PullRequestComment current, int currentDepth) {
		if (current.getId() == target.getId()) {
			return currentDepth;
		}
		List<PullRequestComment> replies = current.getReplies();
		if (replies != null) {
			for (PullRequestComment reply : replies) {
				int depth = findCommentDepth(target, reply, currentDepth + 1);
				if (depth >= 0) {
					return depth;
				}
			}
		}
		return -1; // Not found in this branch
	}

	/**
	 * Toggles the collapsed state of a comment thread
	 *
	 * @param comment
	 *            the comment to toggle
	 */
	private void toggleCommentCollapse(PullRequestComment comment) {
		long commentId = comment.getId();
		if (collapsedCommentIds.contains(commentId)) {
			collapsedCommentIds.remove(commentId);
		} else {
			collapsedCommentIds.add(commentId);
		}
		commentsViewer.refresh(comment);
	}

	/**
	 * Multi-line input dialog for entering comment replies with a larger,
	 * resizable text area.
	 */
	private static class MultiLineInputDialog extends Dialog {
		private String title;

		private String message;

		private String value = ""; //$NON-NLS-1$

		private Text textControl;

		public MultiLineInputDialog(Shell parentShell, String dialogTitle,
				String dialogMessage, String initialValue) {
			super(parentShell);
			this.title = dialogTitle;
			this.message = dialogMessage;
			if (initialValue != null) {
				this.value = initialValue;
			}
			// Enable resizing
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}

		@Override
		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			if (title != null) {
				shell.setText(title);
			}
			// Set minimum size
			shell.setMinimumSize(400, 200);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);
			GridLayoutFactory.swtDefaults().applyTo(composite);

			// Message label
			if (message != null) {
				Label label = new Label(composite, SWT.WRAP);
				label.setText(message);
				GridDataFactory.fillDefaults().grab(true, false)
						.hint(350, SWT.DEFAULT).applyTo(label);
			}

			// Multi-line text field
			textControl = new Text(composite,
					SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
			textControl.setText(value);
			GridDataFactory.fillDefaults().grab(true, true).hint(400, 150)
					.applyTo(textControl);

			return composite;
		}

		@Override
		protected void okPressed() {
			value = textControl.getText();
			super.okPressed();
		}

		public String getValue() {
			return value;
		}
	}

	@Override
	public void setFocus() {
		commentsViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		// Clear any active highlight
		clearHighlight();

		if (fileSelectionListener != null) {
			getSite().getWorkbenchWindow().getSelectionService()
					.removeSelectionListener(
							PullRequestChangedFilesView.VIEW_ID,
							fileSelectionListener);
		}
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
}
