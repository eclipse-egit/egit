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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.bitbucket.BitbucketClient;
import org.eclipse.egit.core.internal.bitbucket.ChangedFile;
import org.eclipse.egit.core.internal.bitbucket.PullRequest;
import org.eclipse.egit.core.internal.bitbucket.PullRequestComment;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.ActionUtils;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;

/**
 * View for displaying Bitbucket Data Center pull requests
 */
public class PullRequestsView extends ViewPart {

	/**
	 * View ID
	 */
	public static final String VIEW_ID = "org.eclipse.egit.ui.PullRequestsView"; //$NON-NLS-1$

	private FormToolkit toolkit;

	private Form form;

	private TreeViewer pullRequestViewer;

	private PreferenceBasedDateFormatter dateFormatter;

	private ResourceManager imageCache;

	private List<PullRequest> pullRequests = new ArrayList<>();

	private Action refreshAction;

	private Action showAllPRsAction;

	private boolean showAllPRs = false; // Default: show only current user's PRs

	private String currentUsername = null;

	// View mode management
	private enum ViewMode {
		PR_LIST, CHANGES_VIEW
	}

	private ViewMode currentMode = ViewMode.PR_LIST;

	private PullRequest selectedPullRequest = null;

	private List<PullRequestChangedFile> changedFiles = new ArrayList<>();

	private List<PullRequestComment> allComments = new ArrayList<>();

	private Action backAction;

	private ISelectionChangedListener fileSelectionListener;

	private TreeColumnLayout treeColumnLayout;

	private Composite layoutComposite;

	// Split view components for changes view
	private SashForm changesSashForm;

	private SashForm rightSashForm; // Vertical split: compare viewer + comments

	private CompareViewerPane compareViewerPane;

	private Viewer compareViewer;

	private CompareConfiguration compareConfiguration;

	private TreeViewer commentsViewer;

	private Composite commentsPanel;

	private Job currentLoadCompareJob;

	@Override
	public void createPartControl(Composite parent) {
		dateFormatter = PreferenceBasedDateFormatter.create();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(e -> toolkit.dispose());

		form = toolkit.createForm(parent);
		form.setText("Pull Requests"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);
		toolkit.decorateFormHeading(form);
		GridLayoutFactory.fillDefaults().applyTo(form.getBody());

		treeColumnLayout = new TreeColumnLayout();
		layoutComposite = new Composite(form.getBody(), SWT.NONE);
		layoutComposite.setLayout(treeColumnLayout);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(layoutComposite);

		pullRequestViewer = new TreeViewer(layoutComposite,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		pullRequestViewer.getTree().setHeaderVisible(true);
		pullRequestViewer.getTree().setLinesVisible(true);

		// ID Column
		TreeViewerColumn idColumn = createColumn(treeColumnLayout, "ID", 10, SWT.LEFT); //$NON-NLS-1$
		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequest) {
					return String.valueOf(((PullRequest) element).getId());
				}
				return ""; //$NON-NLS-1$
			}
		});

		// Title Column
		TreeViewerColumn titleColumn = createColumn(treeColumnLayout, "Title", 40, //$NON-NLS-1$
				SWT.LEFT);
		titleColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequest) {
					return ((PullRequest) element).getTitle();
				}
				return ""; //$NON-NLS-1$
			}

			@Override
			public Image getImage(Object element) {
				if (element instanceof PullRequest) {
					PullRequest pr = (PullRequest) element;
					if ("OPEN".equals(pr.getState())) { //$NON-NLS-1$
						return UIIcons.getImage(getImageCache(), UIIcons.BRANCH);
					} else if ("MERGED".equals(pr.getState())) { //$NON-NLS-1$
						return UIIcons.getImage(getImageCache(), UIIcons.MERGE);
					} else if ("DECLINED".equals(pr.getState())) { //$NON-NLS-1$
						return UIIcons.getImage(getImageCache(), UIIcons.RESET);
					}
				}
				return null;
			}
		});

		// Author Column
		TreeViewerColumn authorColumn = createColumn(treeColumnLayout, "Author", 20, //$NON-NLS-1$
				SWT.LEFT);
		authorColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequest) {
					PullRequest pr = (PullRequest) element;
					if (pr.getAuthor() != null
							&& pr.getAuthor().getUser() != null) {
						return pr.getAuthor().getUser().getDisplayName();
					}
				}
				return ""; //$NON-NLS-1$
			}
		});

		// State Column
		TreeViewerColumn stateColumn = createColumn(treeColumnLayout, "State", 10, //$NON-NLS-1$
				SWT.LEFT);
		stateColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequest) {
					return ((PullRequest) element).getState();
				}
				return ""; //$NON-NLS-1$
			}
		});

		// Comments Column
		TreeViewerColumn commentsColumn = createColumn(treeColumnLayout,
				"Comments", 10, SWT.LEFT); //$NON-NLS-1$
		commentsColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequest) {
					int count = ((PullRequest) element).getCommentCount();
					return count > 0 ? String.valueOf(count) : ""; //$NON-NLS-1$
				}
				return ""; //$NON-NLS-1$
			}
		});

		// Updated Column
		TreeViewerColumn updatedColumn = createColumn(treeColumnLayout, "Updated", 20, //$NON-NLS-1$
				SWT.LEFT);
		updatedColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequest) {
					PullRequest pr = (PullRequest) element;
					if (pr.getUpdatedDate() != null) {
						return dateFormatter.formatDate(pr.getUpdatedDate());
					}
				}
				return ""; //$NON-NLS-1$
			}
		});

		pullRequestViewer.setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof List) {
					return ((List<?>) inputElement).toArray();
				}
				return new Object[0];
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return null;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}
		});

		pullRequestViewer.setInput(pullRequests);

		// Add double-click listener for PRs
		pullRequestViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (!selection.isEmpty()) {
					Object element = selection.getFirstElement();
					if (element instanceof PullRequest) {
						onPullRequestDoubleClick((PullRequest) element);
					}
				}
			}
		});

		createActions();
		contributeToActionBars();
		
		// Automatically refresh pull requests when view opens
		refreshPullRequests();
	}

	private TreeViewerColumn createColumn(TreeColumnLayout layout,
			String text, int weight, int style) {
		TreeViewerColumn column = new TreeViewerColumn(pullRequestViewer,
				style);
		column.getColumn().setText(text);
		layout.setColumnData(column.getColumn(), new ColumnWeightData(weight));
		return column;
	}

	private void createActions() {
		refreshAction = new Action("Refresh") { //$NON-NLS-1$
			@Override
			public void run() {
				refreshPullRequests();
			}
		};
		refreshAction.setImageDescriptor(UIIcons.ELCL16_REFRESH);
		refreshAction.setToolTipText("Refresh pull requests"); //$NON-NLS-1$

		showAllPRsAction = new Action("Show All Pull Requests", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
			@Override
			public void run() {
				showAllPRs = isChecked();
				refreshPullRequests();
			}
		};
		showAllPRsAction.setChecked(false); // Default: show only PRs authored by current user
		showAllPRsAction.setToolTipText(
				"Check to show all pull requests, uncheck to show only PRs you authored (default)"); //$NON-NLS-1$

		backAction = new Action("Back to Pull Requests") { //$NON-NLS-1$
			@Override
			public void run() {
				switchToPRListView();
			}
		};
		backAction.setImageDescriptor(UIIcons.ELCL16_PREVIOUS);
		backAction.setToolTipText("Back to pull requests list"); //$NON-NLS-1$
		backAction.setEnabled(false); // Disabled by default
	}

	private void contributeToActionBars() {
		IToolBarManager toolBarManager = getViewSite().getActionBars()
				.getToolBarManager();
		toolBarManager.add(backAction);
		toolBarManager.add(refreshAction);
		toolBarManager.add(showAllPRsAction);
	}

	private void refreshPullRequests() {
		// Get configuration from preferences
		final String serverUrl = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.BITBUCKET_SERVER_URL);
		final String token = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.BITBUCKET_ACCESS_TOKEN);
		final String projectKey = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.BITBUCKET_PROJECT_KEY);
		final String repoSlug = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.BITBUCKET_REPO_SLUG);
		final String username = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.BITBUCKET_USERNAME);

		if (serverUrl.isEmpty() || token.isEmpty() || projectKey.isEmpty()
				|| repoSlug.isEmpty()) {
			form.setText("Pull Requests - Not configured"); //$NON-NLS-1$
			return;
		}

		Job job = new Job("Fetching pull requests") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Fetching pull requests from Bitbucket", //$NON-NLS-1$
						IProgressMonitor.UNKNOWN);

				System.out.println("Starting pull request fetch job"); //$NON-NLS-1$
				try {
					BitbucketClient client = new BitbucketClient(serverUrl,
							token);

				// Always use the latest username from preferences
				currentUsername = username;
				if (!currentUsername.isEmpty()) {
					System.out.println(
							"Using configured username: " //$NON-NLS-1$
									+ currentUsername);
				} else {
					System.out.println(
							"Username not configured - showing all PRs"); //$NON-NLS-1$
				}

				// Fetch PRs - filter by author (current user) unless "Show All" is checked
				// If username is not configured, we'll show all PRs (authorFilter = null)
				String authorFilter = (showAllPRs
						|| currentUsername == null
						|| currentUsername.isEmpty()) ? null
								: currentUsername;

					System.out.println("showAllPRs: " + showAllPRs); //$NON-NLS-1$
					System.out.println("authorFilter: " + authorFilter); //$NON-NLS-1$

					String jsonResponse = client.getPullRequests(projectKey,
							repoSlug, "OPEN", authorFilter, null, 100, 0); //$NON-NLS-1$

				// Parse JSON response to populate pullRequests list
				List<PullRequest> fetchedPRs = parsePullRequests(
						jsonResponse);

					// Debug: Log all unique author usernames found
					System.out.println("Found " + fetchedPRs.size() //$NON-NLS-1$
							+ " PRs. Unique authors:"); //$NON-NLS-1$
					fetchedPRs.stream()
							.filter(pr -> pr.getAuthor() != null
									&& pr.getAuthor().getUser() != null)
							.map(pr -> pr.getAuthor().getUser().getName()
									+ " (" //$NON-NLS-1$
									+ pr.getAuthor().getUser().getDisplayName()
									+ ")") //$NON-NLS-1$
							.distinct().forEach(System.out::println);

					final String filterInfo;
					if (showAllPRs) {
						filterInfo = "All PRs"; //$NON-NLS-1$
					} else if (currentUsername != null
							&& !currentUsername.isEmpty()) {
						filterInfo = "Authored by " + currentUsername; //$NON-NLS-1$
					} else {
						filterInfo = "All PRs (username not configured)"; //$NON-NLS-1$
					}

					Display.getDefault().asyncExec(() -> {
						if (!pullRequestViewer.getControl().isDisposed()) {
							pullRequests.clear();
							pullRequests.addAll(fetchedPRs);
							pullRequestViewer.refresh();
							form.setText(MessageFormat.format(
									"Pull Requests ({0}) - {1}", //$NON-NLS-1$
									Integer.valueOf(pullRequests.size()),
									filterInfo));
						}
					});

					return Status.OK_STATUS;
				} catch (IOException e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Failed to fetch pull requests", e); //$NON-NLS-1$
				} finally {
					monitor.done();
				}
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private List<PullRequest> parsePullRequests(String json) {
		List<PullRequest> result = new ArrayList<>();

		// Find the "values" array in the response
		int valuesStart = json.indexOf("\"values\":"); //$NON-NLS-1$
		if (valuesStart == -1) {
			return result;
		}

		// Find the opening bracket of the values array
		int arrayStart = json.indexOf('[', valuesStart);
		if (arrayStart == -1) {
			return result;
		}

		// Parse each pull request object in the array
		int pos = arrayStart + 1;
		while (pos < json.length()) {
			// Skip whitespace
			while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
				pos++;
			}

			if (pos >= json.length() || json.charAt(pos) == ']') {
				break;
			}

			// Find the opening brace of the PR object
			if (json.charAt(pos) == '{') {
				int objEnd = findMatchingBrace(json, pos);
				if (objEnd == -1) {
					break;
				}

				String prJson = json.substring(pos, objEnd + 1);
				PullRequest pr = parseSinglePullRequest(prJson);
				if (pr != null) {
					result.add(pr);
				}

				pos = objEnd + 1;
				// Skip comma if present
				while (pos < json.length() && (Character.isWhitespace(json.charAt(pos)) || json.charAt(pos) == ',')) {
					pos++;
				}
			} else {
				pos++;
			}
		}

		return result;
	}

	private int findMatchingBrace(String json, int startPos) {
		int depth = 0;
		boolean inString = false;
		boolean escape = false;

		for (int i = startPos; i < json.length(); i++) {
			char c = json.charAt(i);

			if (escape) {
				escape = false;
				continue;
			}

			if (c == '\\') {
				escape = true;
				continue;
			}

			if (c == '"') {
				inString = !inString;
				continue;
			}

			if (!inString) {
				if (c == '{') {
					depth++;
				} else if (c == '}') {
					depth--;
					if (depth == 0) {
						return i;
					}
				}
			}
		}

		return -1;
	}

	private PullRequest parseSinglePullRequest(String json) {
		PullRequest pr = new PullRequest();

		// Parse id
		Long id = extractLongValue(json, "\"id\":"); //$NON-NLS-1$
		if (id != null) {
			pr.setId(id.longValue());
		}

		// Parse version
		Integer version = extractIntValue(json, "\"version\":"); //$NON-NLS-1$
		if (version != null) {
			pr.setVersion(version.intValue());
		}

		// Parse title
		String title = extractStringValue(json, "\"title\":"); //$NON-NLS-1$
		if (title != null) {
			pr.setTitle(title);
		}

		// Parse description
		String description = extractStringValue(json, "\"description\":"); //$NON-NLS-1$
		if (description != null) {
			pr.setDescription(description);
		}

		// Parse state
		String state = extractStringValue(json, "\"state\":"); //$NON-NLS-1$
		if (state != null) {
			pr.setState(state);
		}

		// Parse open
		Boolean open = extractBooleanValue(json, "\"open\":"); //$NON-NLS-1$
		if (open != null) {
			pr.setOpen(open.booleanValue());
		}

		// Parse closed
		Boolean closed = extractBooleanValue(json, "\"closed\":"); //$NON-NLS-1$
		if (closed != null) {
			pr.setClosed(closed.booleanValue());
		}

		// Parse createdDate
		Long createdDate = extractLongValue(json, "\"createdDate\":"); //$NON-NLS-1$
		if (createdDate != null) {
			pr.setCreatedDate(new java.util.Date(createdDate.longValue()));
		}

		// Parse updatedDate
		Long updatedDate = extractLongValue(json, "\"updatedDate\":"); //$NON-NLS-1$
		if (updatedDate != null) {
			pr.setUpdatedDate(new java.util.Date(updatedDate.longValue()));
		}

		// Parse fromRef
		String fromRefJson = extractObjectValue(json, "\"fromRef\":"); //$NON-NLS-1$
		if (fromRefJson != null) {
			pr.setFromRef(parseRef(fromRefJson));
		}

		// Parse toRef
		String toRefJson = extractObjectValue(json, "\"toRef\":"); //$NON-NLS-1$
		if (toRefJson != null) {
			pr.setToRef(parseRef(toRefJson));
		}

		// Parse author
		String authorJson = extractObjectValue(json, "\"author\":"); //$NON-NLS-1$
		if (authorJson != null) {
			pr.setAuthor(parseParticipant(authorJson));
		}

		// Parse comment count from properties
		String propertiesJson = extractObjectValue(json, "\"properties\":"); //$NON-NLS-1$
		if (propertiesJson != null) {
			Integer commentCount = extractIntValue(propertiesJson, "\"commentCount\":"); //$NON-NLS-1$
			if (commentCount != null) {
				pr.setCommentCount(commentCount.intValue());
			}
		}

		return pr;
	}

	private PullRequest.PullRequestRef parseRef(String json) {
		PullRequest.PullRequestRef ref = new PullRequest.PullRequestRef();

		String id = extractStringValue(json, "\"id\":"); //$NON-NLS-1$
		if (id != null) {
			ref.setId(id);
		}

		String displayId = extractStringValue(json, "\"displayId\":"); //$NON-NLS-1$
		if (displayId != null) {
			ref.setDisplayId(displayId);
		}

		String repoJson = extractObjectValue(json, "\"repository\":"); //$NON-NLS-1$
		if (repoJson != null) {
			ref.setRepository(parseRepository(repoJson));
		}

		return ref;
	}

	private PullRequest.Repository parseRepository(String json) {
		PullRequest.Repository repo = new PullRequest.Repository();

		String slug = extractStringValue(json, "\"slug\":"); //$NON-NLS-1$
		if (slug != null) {
			repo.setSlug(slug);
		}

		String name = extractStringValue(json, "\"name\":"); //$NON-NLS-1$
		if (name != null) {
			repo.setName(name);
		}

		String projectJson = extractObjectValue(json, "\"project\":"); //$NON-NLS-1$
		if (projectJson != null) {
			repo.setProject(parseProject(projectJson));
		}

		return repo;
	}

	private PullRequest.Project parseProject(String json) {
		PullRequest.Project project = new PullRequest.Project();

		String key = extractStringValue(json, "\"key\":"); //$NON-NLS-1$
		if (key != null) {
			project.setKey(key);
		}

		String name = extractStringValue(json, "\"name\":"); //$NON-NLS-1$
		if (name != null) {
			project.setName(name);
		}

		return project;
	}

	private PullRequest.PullRequestParticipant parseParticipant(String json) {
		PullRequest.PullRequestParticipant participant = new PullRequest.PullRequestParticipant();

		String userJson = extractObjectValue(json, "\"user\":"); //$NON-NLS-1$
		if (userJson != null) {
			participant.setUser(parseUser(userJson));
		}

		String role = extractStringValue(json, "\"role\":"); //$NON-NLS-1$
		if (role != null) {
			participant.setRole(role);
		}

		Boolean approved = extractBooleanValue(json, "\"approved\":"); //$NON-NLS-1$
		if (approved != null) {
			participant.setApproved(approved.booleanValue());
		}

		return participant;
	}

	private PullRequest.User parseUser(String json) {
		PullRequest.User user = new PullRequest.User();

		String name = extractStringValue(json, "\"name\":"); //$NON-NLS-1$
		if (name != null) {
			user.setName(name);
		}

		String emailAddress = extractStringValue(json, "\"emailAddress\":"); //$NON-NLS-1$
		if (emailAddress != null) {
			user.setEmailAddress(emailAddress);
		}

		String displayName = extractStringValue(json, "\"displayName\":"); //$NON-NLS-1$
		if (displayName != null) {
			user.setDisplayName(displayName);
		}

		return user;
	}

	private String extractStringValue(String json, String key) {
		int keyPos = json.indexOf(key);
		if (keyPos == -1) {
			return null;
		}

		int valueStart = json.indexOf('"', keyPos + key.length());
		if (valueStart == -1) {
			return null;
		}

		int valueEnd = valueStart + 1;
		boolean escape = false;
		while (valueEnd < json.length()) {
			char c = json.charAt(valueEnd);
			if (escape) {
				escape = false;
				valueEnd++;
				continue;
			}
			if (c == '\\') {
				escape = true;
				valueEnd++;
				continue;
			}
			if (c == '"') {
				return json.substring(valueStart + 1, valueEnd);
			}
			valueEnd++;
		}

		return null;
	}

	private Long extractLongValue(String json, String key) {
		int keyPos = json.indexOf(key);
		if (keyPos == -1) {
			return null;
		}

		int valueStart = keyPos + key.length();
		while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
			valueStart++;
		}

		int valueEnd = valueStart;
		while (valueEnd < json.length() && (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '-')) {
			valueEnd++;
		}

		if (valueEnd > valueStart) {
			try {
				return Long.valueOf(json.substring(valueStart, valueEnd));
			} catch (NumberFormatException e) {
				return null;
			}
		}

		return null;
	}

	private Integer extractIntValue(String json, String key) {
		Long value = extractLongValue(json, key);
		return value != null ? Integer.valueOf(value.intValue()) : null;
	}

	private Boolean extractBooleanValue(String json, String key) {
		int keyPos = json.indexOf(key);
		if (keyPos == -1) {
			return null;
		}

		int valueStart = keyPos + key.length();
		while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
			valueStart++;
		}

		if (json.startsWith("true", valueStart)) { //$NON-NLS-1$
			return Boolean.TRUE;
		} else if (json.startsWith("false", valueStart)) { //$NON-NLS-1$
			return Boolean.FALSE;
		}

		return null;
	}

	private String extractObjectValue(String json, String key) {
		int keyPos = json.indexOf(key);
		if (keyPos == -1) {
			return null;
		}

		int objectStart = json.indexOf('{', keyPos + key.length());
		if (objectStart == -1) {
			return null;
		}

		int objectEnd = findMatchingBrace(json, objectStart);
		if (objectEnd == -1) {
			return null;
		}

		return json.substring(objectStart, objectEnd + 1);
	}

	private void onPullRequestDoubleClick(PullRequest pr) {
		selectedPullRequest = pr;
		System.out.println("Double-clicked PR #" + pr.getId() + ": " + pr.getTitle()); //$NON-NLS-1$ //$NON-NLS-2$

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

					System.out.println("Fetching changes for PR #" + pr.getId()); //$NON-NLS-1$
					BitbucketClient client = new BitbucketClient(serverUrl,
							token);

					String projectKey = pr.getToRef().getRepository()
							.getProject().getKey();
					String repoSlug = pr.getToRef().getRepository().getSlug();

					System.out.println("Project: " + projectKey + ", Repo: " + repoSlug); //$NON-NLS-1$ //$NON-NLS-2$

					// Fetch changed files
					String jsonResponse = client.getPullRequestChanges(
							projectKey, repoSlug, pr.getId());

					System.out.println("Got JSON response, length: " + jsonResponse.length()); //$NON-NLS-1$

					List<ChangedFile> apiChangedFiles = parseChangedFiles(
							jsonResponse);
					System.out.println("Parsed " + apiChangedFiles.size() + " changed files"); //$NON-NLS-1$ //$NON-NLS-2$

					final List<PullRequestChangedFile> uiChangedFiles = apiChangedFiles
							.stream()
							.map(PullRequestChangedFile::fromChangedFile)
							.collect(Collectors.toList());

					System.out.println("Converted to " + uiChangedFiles.size() + " UI changed files"); //$NON-NLS-1$ //$NON-NLS-2$

					// Fetch pull request activities (including comments)
					final List<PullRequestComment> comments = new ArrayList<>();
					try {
						System.out.println("Fetching activities for PR #" + pr.getId()); //$NON-NLS-1$
						String activitiesJson = client.getPullRequestActivities(
								projectKey, repoSlug, pr.getId());
						System.out.println("Got activities JSON, length: " + activitiesJson.length()); //$NON-NLS-1$
						
						comments.addAll(parseActivities(activitiesJson));
						System.out.println("Parsed " + comments.size() + " comments"); //$NON-NLS-1$ //$NON-NLS-2$
					} catch (Exception e) {
						System.err.println("Failed to fetch/parse activities: " + e.getMessage()); //$NON-NLS-1$
						e.printStackTrace();
					}

					Display.getDefault().asyncExec(() -> {
						if (!pullRequestViewer.getControl().isDisposed()) {
							changedFiles.clear();
							changedFiles.addAll(uiChangedFiles);
							allComments.clear();
							allComments.addAll(comments);
							System.out.println("Switching to changes view with " + changedFiles.size() + " files and " + allComments.size() + " comments"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							switchToChangesView();
						}
					});

					return Status.OK_STATUS;
				} catch (IOException e) {
					Display.getDefault().asyncExec(() -> {
						MessageDialog.openError(
								pullRequestViewer.getControl().getShell(),
								"Error", //$NON-NLS-1$
								"Failed to fetch changed files: " //$NON-NLS-1$
										+ e.getMessage());
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

	private List<ChangedFile> parseChangedFiles(String json) {
		System.out.println("parseChangedFiles() called, json length: " + json.length()); //$NON-NLS-1$
		List<ChangedFile> result = new ArrayList<>();

		// Find the "values" array in the response
		int valuesStart = json.indexOf("\"values\":"); //$NON-NLS-1$
		if (valuesStart == -1) {
			System.out.println("No 'values' array found in JSON"); //$NON-NLS-1$
			return result;
		}

		// Find the opening bracket of the values array
		int arrayStart = json.indexOf('[', valuesStart);
		if (arrayStart == -1) {
			System.out.println("No array start '[' found"); //$NON-NLS-1$
			return result;
		}

		// Parse each changed file object in the array
		int pos = arrayStart + 1;
		while (pos < json.length()) {
			// Skip whitespace
			while (pos < json.length()
					&& Character.isWhitespace(json.charAt(pos))) {
				pos++;
			}

			if (pos >= json.length() || json.charAt(pos) == ']') {
				break;
			}

			// Find the opening brace of the changed file object
			if (json.charAt(pos) == '{') {
				int objEnd = findMatchingBrace(json, pos);
				if (objEnd == -1) {
					break;
				}

				String changedFileJson = json.substring(pos, objEnd + 1);
				ChangedFile cf = parseSingleChangedFile(changedFileJson);
				if (cf != null) {
					result.add(cf);
				}

				pos = objEnd + 1;
				// Skip comma if present
				while (pos < json.length() && (Character.isWhitespace(
						json.charAt(pos)) || json.charAt(pos) == ',')) {
					pos++;
				}
			} else {
				pos++;
			}
		}

		System.out.println("parseChangedFiles() completed, found " + result.size() + " files"); //$NON-NLS-1$ //$NON-NLS-2$
		return result;
	}

	private ChangedFile parseSingleChangedFile(String json) {
		ChangedFile cf = new ChangedFile();

		// Parse type (ADD, MODIFY, DELETE, MOVE, COPY)
		String type = extractStringValue(json, "\"type\":"); //$NON-NLS-1$
		if (type != null) {
			cf.setType(type);
		}

		// Parse path
		String pathJson = extractObjectValue(json, "\"path\":"); //$NON-NLS-1$
		if (pathJson != null) {
			cf.setPath(parsePath(pathJson));
		}

		// Parse srcPath (for MOVE/COPY operations)
		String srcPathJson = extractObjectValue(json, "\"srcPath\":"); //$NON-NLS-1$
		if (srcPathJson != null) {
			cf.setSrcPath(parsePath(srcPathJson));
		}

		return cf;
	}

	private ChangedFile.Path parsePath(String json) {
		ChangedFile.Path path = new ChangedFile.Path();

		// Parse toString
		String toStringValue = extractStringValue(json, "\"toString\":"); //$NON-NLS-1$
		if (toStringValue != null) {
			path.setToString(toStringValue);
		}

		// Parse name
		String name = extractStringValue(json, "\"name\":"); //$NON-NLS-1$
		if (name != null) {
			path.setName(name);
		}

		// Parse extension
		String extension = extractStringValue(json, "\"extension\":"); //$NON-NLS-1$
		if (extension != null) {
			path.setExtension(extension);
		}

		// Parse components array
		List<String> components = new ArrayList<>();
		String componentsJson = json.substring(
				json.indexOf("\"components\":")); //$NON-NLS-1$
		int arrayStart = componentsJson.indexOf('[');
		if (arrayStart != -1) {
			int arrayEnd = componentsJson.indexOf(']', arrayStart);
			if (arrayEnd != -1) {
				String arrayContent = componentsJson.substring(arrayStart + 1,
						arrayEnd);
				// Simple string array parser
				int compPos = 0;
				while (compPos < arrayContent.length()) {
					int quoteStart = arrayContent.indexOf('"', compPos);
					if (quoteStart == -1) {
						break;
					}
					int quoteEnd = arrayContent.indexOf('"', quoteStart + 1);
					if (quoteEnd == -1) {
						break;
					}
					components.add(
							arrayContent.substring(quoteStart + 1, quoteEnd));
					compPos = quoteEnd + 1;
				}
			}
		}
		path.setComponents(components);

		return path;
	}

	private void switchToChangesView() {
		System.out.println("switchToChangesView() called, mode changing to CHANGES_VIEW"); //$NON-NLS-1$
		currentMode = ViewMode.CHANGES_VIEW;

		// Dispose the existing tree viewer and layout
		if (layoutComposite != null && !layoutComposite.isDisposed()) {
			layoutComposite.dispose();
		}

		// Create a new SashForm for split view (horizontal: file tree | [compare + comments])
		changesSashForm = new SashForm(form.getBody(), SWT.HORIZONTAL);
		toolkit.adapt(changesSashForm, true, true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(changesSashForm);

		// LEFT PANE: Create tree composite with column layout directly in sash
		treeColumnLayout = new TreeColumnLayout();
		layoutComposite = new Composite(changesSashForm, SWT.NONE);
		layoutComposite.setLayout(treeColumnLayout);

		// Recreate tree viewer
		pullRequestViewer = new TreeViewer(layoutComposite,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		pullRequestViewer.getTree().setHeaderVisible(true);
		pullRequestViewer.getTree().setLinesVisible(true);

		// Set up changed files columns
		System.out.println("Setting up changed files columns"); //$NON-NLS-1$
		setupChangedFilesColumns();

		// Set content provider
		System.out.println("Setting content provider"); //$NON-NLS-1$
		pullRequestViewer.setContentProvider(new PullRequestChangesContentProvider());

		// Set input
		System.out.println("Setting input with " + changedFiles.size() + " changed files"); //$NON-NLS-1$ //$NON-NLS-2$
		pullRequestViewer.setInput(changedFiles);

		// Add file selection listener
		if (fileSelectionListener == null) {
			fileSelectionListener = new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					IStructuredSelection selection = (IStructuredSelection) event
							.getSelection();
					if (!selection.isEmpty()) {
						Object element = selection.getFirstElement();
						if (element instanceof PullRequestChangedFile) {
							onFileSelected((PullRequestChangedFile) element);
						}
					}
				}
			};
		}
		pullRequestViewer.addSelectionChangedListener(fileSelectionListener);

		// Create context menu for changed files
		createChangedFilesPopupMenu(pullRequestViewer);

		// RIGHT PANE: Create vertical SashForm for compare viewer + comments panel
		rightSashForm = new SashForm(changesSashForm, SWT.VERTICAL);
		toolkit.adapt(rightSashForm, true, true);

		// Compare viewer pane (top of right pane)
		compareViewerPane = new CompareViewerPane(rightSashForm, SWT.BORDER | SWT.FLAT);
		toolkit.adapt(compareViewerPane, true, true);
		compareViewerPane.setText("Select a file to view changes"); //$NON-NLS-1$

		// Comments panel (bottom of right pane)
		createCommentsPanel(rightSashForm);

		// Set weights for vertical split (70% compare, 30% comments)
		rightSashForm.setWeights(new int[] { 70, 30 });

		// Restore or set default sash weights for horizontal split (30% files, 70% compare+comments)
		int[] weights = restoreSashWeights();
		changesSashForm.setWeights(weights);

		// Save sash weights when changed
		changesSashForm.addListener(SWT.Resize, e -> saveSashWeights());

		// Update form title
		updateFormTitle();

		// Enable back action
		if (backAction != null) {
			backAction.setEnabled(true);
		}

		// Force layout
		form.getBody().layout(true, true);
		System.out.println("switchToChangesView() completed"); //$NON-NLS-1$

		// Auto-select first file if available
		if (!changedFiles.isEmpty()) {
			Display.getDefault().asyncExec(() -> {
				if (!pullRequestViewer.getControl().isDisposed()) {
					pullRequestViewer.getTree().select(pullRequestViewer.getTree().getItem(0));
					pullRequestViewer.setSelection(pullRequestViewer.getSelection());
				}
			});
		}
	}

	/**
	 * Creates the comments panel at the bottom of the right pane
	 *
	 * @param parent
	 *            the parent composite (rightSashForm)
	 */
	private void createCommentsPanel(Composite parent) {
		// Create tree composite with column layout
		Composite treeComposite = new Composite(parent, SWT.NONE);
		TreeColumnLayout commentColumnLayout = new TreeColumnLayout();
		treeComposite.setLayout(commentColumnLayout);

		commentsViewer = new TreeViewer(treeComposite,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		commentsViewer.getTree().setHeaderVisible(true);
		commentsViewer.getTree().setLinesVisible(true);

		// Line column
		TreeViewerColumn lineCol = new TreeViewerColumn(commentsViewer,
				SWT.LEFT);
		lineCol.getColumn().setText("Line"); //$NON-NLS-1$
		commentColumnLayout.setColumnData(lineCol.getColumn(),
				new ColumnWeightData(10));
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
		commentColumnLayout.setColumnData(authorCol.getColumn(),
				new ColumnWeightData(15));
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

		// Comment column
		TreeViewerColumn commentCol = new TreeViewerColumn(commentsViewer,
				SWT.LEFT);
		commentCol.getColumn().setText("Comment"); //$NON-NLS-1$
		commentColumnLayout.setColumnData(commentCol.getColumn(),
				new ColumnWeightData(60));
		commentCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					// Show first line of comment text
					String text = comment.getText();
					if (text != null) {
						int newlineIndex = text.indexOf('\n');
						if (newlineIndex > 0) {
							return text.substring(0, newlineIndex) + "..."; //$NON-NLS-1$
						}
						return text;
					}
				}
				return ""; //$NON-NLS-1$
			}
		});

		// Date column
		TreeViewerColumn dateCol = new TreeViewerColumn(commentsViewer,
				SWT.LEFT);
		dateCol.getColumn().setText("Date"); //$NON-NLS-1$
		commentColumnLayout.setColumnData(dateCol.getColumn(),
				new ColumnWeightData(15));
		dateCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					if (comment.getCreatedDate() != null) {
						return dateFormatter.formatDate(comment.getCreatedDate());
					}
				}
				return ""; //$NON-NLS-1$
			}
		});

		// Content provider for tree (parent comments + replies)
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
					List<PullRequestComment> replies = ((PullRequestComment) parentElement)
							.getReplies();
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

		// Set empty input initially
		commentsViewer.setInput(new ArrayList<PullRequestComment>());

		// Add double-click listener to scroll to comment line
		commentsViewer.addDoubleClickListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			if (!selection.isEmpty()) {
				Object element = selection.getFirstElement();
				if (element instanceof PullRequestComment) {
					PullRequestComment comment = (PullRequestComment) element;
					scrollToCommentLine(comment);
				}
			}
		});
	}

	private void switchToPRListView() {
		currentMode = ViewMode.PR_LIST;

		// Save sash weights before disposal
		saveSashWeights();

		// Dispose compare viewer and configuration
		if (compareViewer != null) {
			compareViewer.getControl().dispose();
			compareViewer = null;
		}
		if (compareConfiguration != null) {
			compareConfiguration.dispose();
			compareConfiguration = null;
		}

		// Dispose compare viewer pane
		if (compareViewerPane != null && !compareViewerPane.isDisposed()) {
			compareViewerPane.dispose();
			compareViewerPane = null;
		}

		// Dispose comments panel and viewer
		if (commentsViewer != null && !commentsViewer.getControl().isDisposed()) {
			commentsViewer.getControl().dispose();
			commentsViewer = null;
		}
		if (commentsPanel != null && !commentsPanel.isDisposed()) {
			commentsPanel.dispose();
			commentsPanel = null;
		}

		// Dispose right sash form
		if (rightSashForm != null && !rightSashForm.isDisposed()) {
			rightSashForm.dispose();
			rightSashForm = null;
		}

		// Dispose sash form
		if (changesSashForm != null && !changesSashForm.isDisposed()) {
			changesSashForm.dispose();
			changesSashForm = null;
		}

		// Clear comments list
		allComments.clear();

		// Remove file selection listener
		if (fileSelectionListener != null && pullRequestViewer != null) {
			pullRequestViewer.removeSelectionChangedListener(fileSelectionListener);
		}

		// Recreate the original single-pane layout
		treeColumnLayout = new TreeColumnLayout();
		layoutComposite = new Composite(form.getBody(), SWT.NONE);
		layoutComposite.setLayout(treeColumnLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(layoutComposite);

		// Recreate tree viewer
		pullRequestViewer = new TreeViewer(layoutComposite,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		pullRequestViewer.getTree().setHeaderVisible(true);
		pullRequestViewer.getTree().setLinesVisible(true);

		// Set up PR list columns
		setupPRListColumns();

		// Restore PR list content provider
		pullRequestViewer.setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof List) {
					return ((List<?>) inputElement).toArray();
				}
				return new Object[0];
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return null;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}
		});

		// Set input
		pullRequestViewer.setInput(pullRequests);

		// Re-add double-click listener for PRs
		pullRequestViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (!selection.isEmpty()) {
					Object element = selection.getFirstElement();
					if (element instanceof PullRequest) {
						onPullRequestDoubleClick((PullRequest) element);
					}
				}
			}
		});

		// Update form title
		updateFormTitle();

		// Disable back action
		if (backAction != null) {
			backAction.setEnabled(false);
		}

		// Force layout
		form.getBody().layout(true, true);

		// Refresh viewer
		pullRequestViewer.refresh();
	}

	private void setupChangedFilesColumns() {
		System.out.println("setupChangedFilesColumns() called"); //$NON-NLS-1$
		// File Column - explicitly set label provider for icons and file names
		TreeViewerColumn fileColumn = createColumn(treeColumnLayout, "File", //$NON-NLS-1$
				60, SWT.LEFT);
		fileColumn.setLabelProvider(new PullRequestChangesLabelProvider());

		// Change Type Column
		TreeViewerColumn changeColumn = createColumn(treeColumnLayout,
				"Change", 10, SWT.LEFT); //$NON-NLS-1$
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
		TreeViewerColumn commentsColumn = createColumn(treeColumnLayout,
				"Comments", 10, SWT.CENTER); //$NON-NLS-1$
		commentsColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequestChangedFile) {
					PullRequestChangedFile file = (PullRequestChangedFile) element;
					int count = getCommentCountForFile(file.getPath(), file.getSrcPath());
					return count > 0 ? String.valueOf(count) : ""; //$NON-NLS-1$
				} else if (element instanceof PullRequestFolderEntry) {
					// Show total comments for all files in folder
					PullRequestFolderEntry folder = (PullRequestFolderEntry) element;
					int count = getCommentCountForFolder(folder);
					return count > 0 ? String.valueOf(count) : ""; //$NON-NLS-1$
				}
				return ""; //$NON-NLS-1$
			}
		});

		// Path Column
		TreeViewerColumn pathColumn = createColumn(treeColumnLayout, "Path", //$NON-NLS-1$
				30, SWT.LEFT);
		pathColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequestChangedFile) {
					return ((PullRequestChangedFile) element).getPath();
				} else if (element instanceof PullRequestFolderEntry) {
					return ((PullRequestFolderEntry) element)
							.getPath().toString();
				}
				return ""; //$NON-NLS-1$
			}
		});
		System.out.println("setupChangedFilesColumns() completed - created 4 columns"); //$NON-NLS-1$
	}

	private void setupPRListColumns() {
		// ID Column
		TreeViewerColumn idColumn = createColumn(treeColumnLayout, "ID", 10, //$NON-NLS-1$
				SWT.LEFT);
		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequest) {
					return String.valueOf(((PullRequest) element).getId());
				}
				return ""; //$NON-NLS-1$
			}
		});

		// Title Column
		TreeViewerColumn titleColumn = createColumn(treeColumnLayout, "Title", //$NON-NLS-1$
				40, SWT.LEFT);
		titleColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequest) {
					return ((PullRequest) element).getTitle();
				}
				return ""; //$NON-NLS-1$
			}

			@Override
			public Image getImage(Object element) {
				if (element instanceof PullRequest) {
					PullRequest pr = (PullRequest) element;
					if ("OPEN".equals(pr.getState())) { //$NON-NLS-1$
						return UIIcons.getImage(getImageCache(), UIIcons.BRANCH);
					} else if ("MERGED".equals(pr.getState())) { //$NON-NLS-1$
						return UIIcons.getImage(getImageCache(), UIIcons.MERGE);
					} else if ("DECLINED".equals(pr.getState())) { //$NON-NLS-1$
						return UIIcons.getImage(getImageCache(), UIIcons.RESET);
					}
				}
				return null;
			}
		});

		// Author Column
		TreeViewerColumn authorColumn = createColumn(treeColumnLayout,
				"Author", 20, SWT.LEFT); //$NON-NLS-1$
		authorColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequest) {
					PullRequest pr = (PullRequest) element;
					if (pr.getAuthor() != null
							&& pr.getAuthor().getUser() != null) {
						return pr.getAuthor().getUser().getDisplayName();
					}
				}
				return ""; //$NON-NLS-1$
			}
		});

		// State Column
		TreeViewerColumn stateColumn = createColumn(treeColumnLayout, "State", //$NON-NLS-1$
				10, SWT.LEFT);
		stateColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequest) {
					return ((PullRequest) element).getState();
				}
				return ""; //$NON-NLS-1$
			}
		});

		// Comments Column
		TreeViewerColumn commentsColumn = createColumn(treeColumnLayout,
				"Comments", 10, SWT.LEFT); //$NON-NLS-1$
		commentsColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequest) {
					int count = ((PullRequest) element).getCommentCount();
					return count > 0 ? String.valueOf(count) : ""; //$NON-NLS-1$
				}
				return ""; //$NON-NLS-1$
			}
		});

		// Updated Column
		TreeViewerColumn updatedColumn = createColumn(treeColumnLayout,
				"Updated", 20, SWT.LEFT); //$NON-NLS-1$
		updatedColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PullRequest) {
					PullRequest pr = (PullRequest) element;
					if (pr.getUpdatedDate() != null) {
						return dateFormatter.formatDate(pr.getUpdatedDate());
					}
				}
				return ""; //$NON-NLS-1$
			}
		});
	}

	private void updateFormTitle() {
		if (currentMode == ViewMode.CHANGES_VIEW
				&& selectedPullRequest != null) {
			form.setText(MessageFormat.format(
					"Changed Files - PR #{0}: {1}", //$NON-NLS-1$
					Long.valueOf(selectedPullRequest.getId()),
					selectedPullRequest.getTitle()));
		} else {
			// Restore PR list title with filter info
			final String filterInfo;
			if (showAllPRs) {
				filterInfo = "All PRs"; //$NON-NLS-1$
			} else if (currentUsername != null && !currentUsername.isEmpty()) {
				filterInfo = "Authored by " + currentUsername; //$NON-NLS-1$
			} else {
				filterInfo = "All PRs (username not configured)"; //$NON-NLS-1$
			}
			form.setText(MessageFormat.format("Pull Requests ({0}) - {1}", //$NON-NLS-1$
					Integer.valueOf(pullRequests.size()), filterInfo));
		}
	}

	private void onFileSelected(PullRequestChangedFile file) {
		if (selectedPullRequest == null || changesSashForm == null
				|| changesSashForm.isDisposed()) {
			return;
		}

		// Cancel any pending load job
		if (currentLoadCompareJob != null) {
			currentLoadCompareJob.cancel();
		}

		// Create and set compare input in a job
		currentLoadCompareJob = new Job("Loading file comparison") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Preparing comparison", IProgressMonitor.UNKNOWN); //$NON-NLS-1$

					// Check if job was cancelled
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}

					// Get Bitbucket client
					final String serverUrl = Activator.getDefault().getPreferenceStore()
							.getString(UIPreferences.BITBUCKET_SERVER_URL);
					final String token = Activator.getDefault().getPreferenceStore()
							.getString(UIPreferences.BITBUCKET_ACCESS_TOKEN);

					BitbucketClient client = new BitbucketClient(serverUrl, token);

					// Create compare configuration
					final CompareConfiguration config = BitbucketCompareEditorInput
							.createCompareConfiguration(selectedPullRequest, file);

					// Check if job was cancelled before creating compare input
					if (monitor.isCanceled()) {
						if (config != null) {
							config.dispose();
						}
						return Status.CANCEL_STATUS;
					}

					// Create compare input
					final Object compareInput = BitbucketCompareEditorInput
							.createCompareInput(client, selectedPullRequest, file, monitor);

					// Check if job was cancelled after creating compare input
					if (monitor.isCanceled()) {
						if (config != null) {
							config.dispose();
						}
						return Status.CANCEL_STATUS;
					}

					// Update UI on main thread
					Display.getDefault().asyncExec(() -> {
						// Check if this job is still the current one
						if (this != currentLoadCompareJob) {
							if (config != null) {
								config.dispose();
							}
							return;
						}

						if (changesSashForm == null
								|| changesSashForm.isDisposed()
								|| rightSashForm == null
								|| rightSashForm.isDisposed()) {
							if (config != null) {
								config.dispose();
							}
							return;
						}

						try {
							// Dispose old viewer, configuration, and pane
							// completely to avoid stale toolbar references
							if (compareViewer != null) {
								compareViewer = null;
							}
							if (compareConfiguration != null) {
								compareConfiguration.dispose();
								compareConfiguration = null;
							}
							if (compareViewerPane != null
									&& !compareViewerPane.isDisposed()) {
								compareViewerPane.dispose();
							}

							// Store new configuration
							compareConfiguration = config;

							// Create a fresh CompareViewerPane in the RIGHT sash (top of vertical split)
							compareViewerPane = new CompareViewerPane(
									rightSashForm,
									SWT.BORDER | SWT.FLAT);
							toolkit.adapt(compareViewerPane, true, true);
							compareViewerPane.setText(file.getName());

						// Create new compare viewer
						boolean useInlineComments = Activator.getDefault()
								.getPreferenceStore().getBoolean(
										UIPreferences.PULLREQUEST_SHOW_INLINE_COMMENTS);

						InlineCommentTextMergeViewer inlineMergeViewer = null;
						if (useInlineComments) {
							inlineMergeViewer = new InlineCommentTextMergeViewer(
									compareViewerPane,
									compareConfiguration);
							compareViewer = inlineMergeViewer;
						} else {
							compareViewer = CompareUI
									.findContentViewer(null,
											compareInput,
											compareViewerPane,
											compareConfiguration);
						}

							if (compareViewer != null) {
								compareViewer.setInput(compareInput);
								compareViewerPane.setContent(
										compareViewer.getControl());
							}

							// Restore vertical sash weights (compare viewer + comments panel)
							rightSashForm.setWeights(new int[] { 70, 30 });
							
							// Restore horizontal sash weights (files + right pane)
							int[] weights = restoreSashWeights();
							changesSashForm.setWeights(weights);
							
							// Force layout
							rightSashForm.layout(true, true);
							changesSashForm.layout(true, true);

							// Filter and display comments for selected file
							if (commentsViewer != null && !commentsViewer.getControl().isDisposed()) {
								List<PullRequestComment> fileComments = allComments.stream()
										.filter(comment -> {
											if (comment.getPath() == null) {
												return false; // Skip general/file-level comments
											}
											String commentPath = comment.getPath();
											String filePath = file.getPath();
											// Handle MOVE/COPY where srcPath might be relevant
											String srcPath = file.getSrcPath();
											return commentPath.equals(filePath) 
													|| (srcPath != null && commentPath.equals(srcPath));
										})
									.collect(Collectors.toList());
							commentsViewer.setInput(fileComments);
								commentsViewer.refresh();

							// Apply inline comment annotations if enabled
							if (useInlineComments
									&& inlineMergeViewer != null
									&& !fileComments.isEmpty()) {
								inlineMergeViewer
										.setComments(fileComments);
							}

								// Apply line highlighting for commented lines
								// (skip when inline annotations are active
								// to avoid visual redundancy)
								if (compareViewer != null
										&& !fileComments.isEmpty()
										&& !useInlineComments) {
									highlightCommentedLines(compareViewer.getControl(), fileComments);
								}
							}
						} catch (Exception e) {
							Activator.logError("Failed to create compare viewer", e); //$NON-NLS-1$
							if (changesSashForm != null
									&& !changesSashForm.isDisposed()
									&& changesSashForm.getShell() != null) {
								MessageDialog.openError(
										changesSashForm.getShell(),
										"Error", //$NON-NLS-1$
										"Failed to display comparison: " //$NON-NLS-1$
												+ e.getMessage());
							}
						}
					});

					return Status.OK_STATUS;
				} catch (Exception e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Failed to load comparison", e); //$NON-NLS-1$
				} finally {
					monitor.done();
				}
			}
		};
		currentLoadCompareJob.setUser(false);
		currentLoadCompareJob.schedule();
	}

	@Override
	public void setFocus() {
		pullRequestViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		// Cancel any pending load job
		if (currentLoadCompareJob != null) {
			currentLoadCompareJob.cancel();
		}
		if (compareConfiguration != null) {
			compareConfiguration.dispose();
		}
		if (imageCache != null) {
			imageCache.dispose();
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
	 * Creates a ShowInContext for the current selection in changes view.
	 *
	 * @return the ShowInContext, or null if not in changes view mode
	 */
	private ShowInContext getShowInContext() {
		if (currentMode != ViewMode.CHANGES_VIEW || pullRequestViewer == null
				|| pullRequestViewer.getControl().isDisposed()) {
			return null;
		}

		IStructuredSelection selection = pullRequestViewer.getStructuredSelection();
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

	private ResourceManager getImageCache() {
		if (imageCache == null) {
			imageCache = new LocalResourceManager(JFaceResources.getResources());
		}
		return imageCache;
	}

	// Helper methods for sash weight management
	private int[] restoreSashWeights() {
		IDialogSettings settings = getDialogSettings();
		String weights = settings.get(UIPreferences.PULLREQUEST_CHANGES_SASH_WEIGHTS);
		if (weights != null && !weights.isEmpty()) {
			int[] restored = stringToIntArray(weights);
			// Validate that we have exactly 2 weights for the horizontal sash
			if (restored != null && restored.length == 2) {
				return restored;
			}
		}
		return new int[] { 30, 70 }; // Default: 30% files, 70% compare
	}

	private void saveSashWeights() {
		if (changesSashForm != null && !changesSashForm.isDisposed()) {
			IDialogSettings settings = getDialogSettings();
			settings.put(UIPreferences.PULLREQUEST_CHANGES_SASH_WEIGHTS,
					intArrayToString(changesSashForm.getWeights()));
		}
	}

	private IDialogSettings getDialogSettings() {
		return DialogSettings.getOrCreateSection(
				Activator.getDefault().getDialogSettings(),
				PullRequestsView.class.getName());
	}

	private static String intArrayToString(int[] ints) {
		StringBuilder res = new StringBuilder();
		if (ints != null && ints.length > 0) {
			res.append(String.valueOf(ints[0]));
			for (int i = 1; i < ints.length; i++) {
				res.append(',');
				res.append(String.valueOf(ints[i]));
			}
		}
		return res.toString();
	}

	private static int[] stringToIntArray(String s) {
		try {
			String[] parts = s.split(","); //$NON-NLS-1$
			int[] ints = new int[parts.length];
			for (int i = 0; i < parts.length; i++) {
				ints[i] = Integer.parseInt(parts[i].trim());
			}
			return ints;
		} catch (NumberFormatException e) {
			return null; // Return null on parse error
		}
	}

	/**
	 * Parses pull request activities from JSON response and extracts comments
	 *
	 * @param json
	 *            the JSON response from /activities endpoint
	 * @return list of PullRequestComment objects
	 */
	private List<org.eclipse.egit.core.internal.bitbucket.PullRequestComment> parseActivities(
			String json) {
		List<org.eclipse.egit.core.internal.bitbucket.PullRequestComment> result = new ArrayList<>();

		// Find the "values" array
		int valuesStart = json.indexOf("\"values\":"); //$NON-NLS-1$
		if (valuesStart == -1) {
			return result;
		}

		int arrayStart = json.indexOf('[', valuesStart);
		if (arrayStart == -1) {
			return result;
		}

		// Parse each activity object
		int pos = arrayStart + 1;
		while (pos < json.length()) {
			while (pos < json.length()
					&& Character.isWhitespace(json.charAt(pos))) {
				pos++;
			}

			if (pos >= json.length() || json.charAt(pos) == ']') {
				break;
			}

			if (json.charAt(pos) == '{') {
				int objEnd = findMatchingBrace(json, pos);
				if (objEnd == -1) {
					break;
				}

				String activityJson = json.substring(pos, objEnd + 1);

				// Check if this is a COMMENTED action
				String action = extractStringValue(activityJson, "\"action\":"); //$NON-NLS-1$
				if ("COMMENTED".equals(action)) { //$NON-NLS-1$
					org.eclipse.egit.core.internal.bitbucket.PullRequestComment comment = parseCommentActivity(
							activityJson);
					if (comment != null) {
						result.add(comment);
					}
				}

				pos = objEnd + 1;
				while (pos < json.length() && (Character.isWhitespace(
						json.charAt(pos)) || json.charAt(pos) == ',')) {
					pos++;
				}
			} else {
				pos++;
			}
		}

		return result;
	}

	/**
	 * Parses a single comment activity
	 *
	 * @param activityJson
	 *            the activity JSON object
	 * @return PullRequestComment or null
	 */
	private org.eclipse.egit.core.internal.bitbucket.PullRequestComment parseCommentActivity(
			String activityJson) {
		// Extract the "comment" object
		String commentJson = extractObjectValue(activityJson, "\"comment\":"); //$NON-NLS-1$
		if (commentJson == null) {
			return null;
		}

		org.eclipse.egit.core.internal.bitbucket.PullRequestComment comment = parseComment(
				commentJson);
		if (comment == null) {
			return null;
		}

		// Extract the "commentAnchor" object if present
		String anchorJson = extractObjectValue(activityJson,
				"\"commentAnchor\":"); //$NON-NLS-1$
		if (anchorJson != null) {
			parseCommentAnchor(comment, anchorJson);
		}

		return comment;
	}

	/**
	 * Parses a comment object
	 *
	 * @param commentJson
	 *            the comment JSON object
	 * @return PullRequestComment or null
	 */
	private org.eclipse.egit.core.internal.bitbucket.PullRequestComment parseComment(
			String commentJson) {
		org.eclipse.egit.core.internal.bitbucket.PullRequestComment comment = new org.eclipse.egit.core.internal.bitbucket.PullRequestComment();

		// Parse id
		Long id = extractLongValue(commentJson, "\"id\":"); //$NON-NLS-1$
		if (id != null) {
			comment.setId(id.longValue());
		}

		// Parse version
		Integer version = extractIntValue(commentJson, "\"version\":"); //$NON-NLS-1$
		if (version != null) {
			comment.setVersion(version.intValue());
		}

		// Parse text
		String text = extractStringValue(commentJson, "\"text\":"); //$NON-NLS-1$
		if (text != null) {
			comment.setText(text);
		}

		// Parse author
		String authorJson = extractObjectValue(commentJson, "\"author\":"); //$NON-NLS-1$
		if (authorJson != null) {
			String name = extractStringValue(authorJson, "\"name\":"); //$NON-NLS-1$
			String displayName = extractStringValue(authorJson,
					"\"displayName\":"); //$NON-NLS-1$
			String email = extractStringValue(authorJson, "\"emailAddress\":"); //$NON-NLS-1$
			comment.setAuthorName(name);
			comment.setAuthorDisplayName(displayName);
			comment.setAuthorEmail(email);
		}

		// Parse createdDate
		Long createdDate = extractLongValue(commentJson, "\"createdDate\":"); //$NON-NLS-1$
		if (createdDate != null) {
			comment.setCreatedDate(new java.util.Date(createdDate.longValue()));
		}

		// Parse updatedDate
		Long updatedDate = extractLongValue(commentJson, "\"updatedDate\":"); //$NON-NLS-1$
		if (updatedDate != null) {
			comment.setUpdatedDate(new java.util.Date(updatedDate.longValue()));
		}

		// Parse state
		String state = extractStringValue(commentJson, "\"state\":"); //$NON-NLS-1$
		if (state != null) {
			comment.setState(state);
		}

		// Parse severity
		String severity = extractStringValue(commentJson, "\"severity\":"); //$NON-NLS-1$
		if (severity != null) {
			comment.setSeverity(severity);
		}

		// Parse replies (nested comments)
		String repliesJson = extractObjectValue(commentJson, "\"comments\":"); //$NON-NLS-1$
		if (repliesJson != null && repliesJson.startsWith("[")) { //$NON-NLS-1$
			List<org.eclipse.egit.core.internal.bitbucket.PullRequestComment> replies = parseCommentArray(
					repliesJson);
			comment.setReplies(replies);
		}

		return comment;
	}

	/**
	 * Parses a comment anchor into the comment object
	 *
	 * @param comment
	 *            the comment to update
	 * @param anchorJson
	 *            the anchor JSON object
	 */
	private void parseCommentAnchor(
			org.eclipse.egit.core.internal.bitbucket.PullRequestComment comment,
			String anchorJson) {
		// Parse line
		Integer line = extractIntValue(anchorJson, "\"line\":"); //$NON-NLS-1$
		comment.setLine(line);

		// Parse lineType
		String lineType = extractStringValue(anchorJson, "\"lineType\":"); //$NON-NLS-1$
		comment.setLineType(lineType);

		// Parse fileType
		String fileType = extractStringValue(anchorJson, "\"fileType\":"); //$NON-NLS-1$
		comment.setFileType(fileType);

		// Parse path
		String path = extractStringValue(anchorJson, "\"path\":"); //$NON-NLS-1$
		comment.setPath(path);

		// Parse srcPath
		String srcPath = extractStringValue(anchorJson, "\"srcPath\":"); //$NON-NLS-1$
		comment.setSrcPath(srcPath);
	}

	/**
	 * Parses an array of comments
	 *
	 * @param arrayJson
	 *            the JSON array string
	 * @return list of comments
	 */
	private List<org.eclipse.egit.core.internal.bitbucket.PullRequestComment> parseCommentArray(
			String arrayJson) {
		List<org.eclipse.egit.core.internal.bitbucket.PullRequestComment> result = new ArrayList<>();

		if (!arrayJson.startsWith("[")) { //$NON-NLS-1$
			return result;
		}

		int pos = 1; // Skip opening bracket
		while (pos < arrayJson.length()) {
			while (pos < arrayJson.length()
					&& Character.isWhitespace(arrayJson.charAt(pos))) {
				pos++;
			}

			if (pos >= arrayJson.length() || arrayJson.charAt(pos) == ']') {
				break;
			}

			if (arrayJson.charAt(pos) == '{') {
				int objEnd = findMatchingBrace(arrayJson, pos);
				if (objEnd == -1) {
					break;
				}

				String commentJson = arrayJson.substring(pos, objEnd + 1);
				org.eclipse.egit.core.internal.bitbucket.PullRequestComment comment = parseComment(
						commentJson);
				if (comment != null) {
					result.add(comment);
				}

				pos = objEnd + 1;
				while (pos < arrayJson.length() && (Character.isWhitespace(
						arrayJson.charAt(pos)) || arrayJson.charAt(pos) == ',')) {
					pos++;
				}
			} else {
				pos++;
			}
		}

		return result;
	}

	/**
	 * Highlights lines in the compare viewer that have comments.
	 * Traverses the widget tree to find StyledText widgets and applies
	 * LineBackgroundListener to highlight commented lines.
	 *
	 * @param control
	 *            the compare viewer control (root of widget tree)
	 * @param comments
	 *            list of comments for the current file
	 */
	private void highlightCommentedLines(Control control,
			List<org.eclipse.egit.core.internal.bitbucket.PullRequestComment> comments) {
		if (control == null || control.isDisposed() || comments.isEmpty()) {
			return;
		}

		// Traverse widget tree to find StyledText widgets
		if (control instanceof org.eclipse.swt.custom.StyledText) {
			org.eclipse.swt.custom.StyledText styledText = (org.eclipse.swt.custom.StyledText) control;
			
			// Determine if this is left or right side based on widget data or parent
			// For now, we'll check if any comments match this side
			// Create a line background listener for this StyledText
			org.eclipse.swt.custom.LineBackgroundListener listener = new org.eclipse.swt.custom.LineBackgroundListener() {
				@Override
				public void lineGetBackground(
						org.eclipse.swt.custom.LineBackgroundEvent event) {
					int lineIndex = styledText.getLineAtOffset(event.lineOffset);
					int displayLine = lineIndex + 1; // Convert to 1-based line number

					// Check if this line has a comment
					boolean hasComment = comments.stream().anyMatch(comment -> {
						if (comment.getLine() == null) {
							return false;
						}
						return comment.getLine().intValue() == displayLine;
					});

					if (hasComment) {
						// Light yellow background for commented lines
						event.lineBackground = styledText.getDisplay()
								.getSystemColor(org.eclipse.swt.SWT.COLOR_YELLOW);
					}
				}
			};

			// Remove existing listeners to avoid duplicates
			// Note: StyledText doesn't provide a way to get existing listeners,
			// so we need to track them or recreate the widget
			styledText.addLineBackgroundListener(listener);
			styledText.redraw();
		} else if (control instanceof Composite) {
			// Recursively traverse children
			Composite composite = (Composite) control;
			for (Control child : composite.getChildren()) {
				highlightCommentedLines(child, comments);
			}
		}
	}

	/**
	 * Scrolls the compare viewer to show the line where the comment was made.
	 *
	 * @param comment
	 *            the comment to scroll to
	 */
	private void scrollToCommentLine(
			org.eclipse.egit.core.internal.bitbucket.PullRequestComment comment) {
		if (comment == null || comment.getLine() == null
				|| compareViewer == null) {
			return;
		}

		int targetLine = comment.getLine().intValue();
		String fileType = comment.getFileType(); // "FROM" (left) or "TO" (right)

		System.out.println("Scrolling to line " + targetLine + " on side: " + fileType); //$NON-NLS-1$ //$NON-NLS-2$

		// Find the appropriate StyledText widget (left or right side)
		Control viewerControl = compareViewer.getControl();
		scrollToLineInControl(viewerControl, targetLine, fileType);
	}

	/**
	 * Recursively searches for StyledText widgets and scrolls to the target line.
	 *
	 * @param control
	 *            the control to search
	 * @param targetLine
	 *            the line number to scroll to (1-based)
	 * @param fileType
	 *            "FROM" for left side, "TO" for right side
	 */
	private void scrollToLineInControl(Control control, int targetLine,
			String fileType) {
		if (control == null || control.isDisposed()) {
			return;
		}

		if (control instanceof org.eclipse.swt.custom.StyledText) {
			org.eclipse.swt.custom.StyledText styledText = (org.eclipse.swt.custom.StyledText) control;

			// Try to determine if this is the correct side
			// This is a heuristic - in practice, the left/right determination
			// may need to be more sophisticated based on widget hierarchy
			int lineCount = styledText.getLineCount();
			if (targetLine > 0 && targetLine <= lineCount) {
				// Convert 1-based line number to 0-based index
				int lineIndex = targetLine - 1;
				int offset = styledText.getOffsetAtLine(lineIndex);

				// Set selection to this line
				styledText.setSelection(offset);
				styledText.showSelection();

				// Ensure the line is visible
				styledText.setTopIndex(Math.max(0, lineIndex - 5)); // Show some context
				System.out.println("Scrolled to line " + targetLine + " in StyledText widget"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else if (control instanceof Composite) {
			// Recursively search children
			Composite composite = (Composite) control;
			for (Control child : composite.getChildren()) {
				scrollToLineInControl(child, targetLine, fileType);
			}
		}
	}

	/**
	 * Counts the number of comments for a specific file.
	 *
	 * @param filePath
	 *            the file path
	 * @param srcPath
	 *            the source path (for MOVE/COPY operations)
	 * @return the number of comments (including replies)
	 */
	private int getCommentCountForFile(String filePath, String srcPath) {
		if (allComments == null || allComments.isEmpty()) {
			return 0;
		}

		return (int) allComments.stream()
				.filter(comment -> {
					if (comment.getPath() == null) {
						return false;
					}
					String commentPath = comment.getPath();
					// Match against both path and srcPath
					return commentPath.equals(filePath)
							|| (srcPath != null && commentPath.equals(srcPath));
				})
				.mapToLong(comment -> {
					// Count the comment itself plus all replies
					long count = 1;
					if (comment.getReplies() != null) {
						count += comment.getReplies().size();
					}
					return count;
				})
				.sum();
	}

	/**
	 * Counts the total number of comments for all files in a folder.
	 *
	 * @param folder
	 *            the folder entry
	 * @return the total number of comments
	 */
	private int getCommentCountForFolder(PullRequestFolderEntry folder) {
		if (allComments == null || allComments.isEmpty()) {
			return 0;
		}

		String folderPath = folder.getPath().toString();
		
		return (int) allComments.stream()
				.filter(comment -> {
					if (comment.getPath() == null) {
						return false;
					}
					// Check if comment path starts with folder path
					return comment.getPath().startsWith(folderPath);
				})
				.mapToLong(comment -> {
					// Count the comment itself plus all replies
					long count = 1;
					if (comment.getReplies() != null) {
						count += comment.getReplies().size();
					}
					return count;
				})
				.sum();
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

				// "Open in Workspace" action - only for files that exist in workspace
				if (!onlyFoldersSelected && !selectedFiles.isEmpty()) {
					Action openInWorkspaceAction = new Action(
							UIText.CommitFileDiffViewer_OpenWorkingTreeVersionInEditorMenuLabel,
							UIIcons.GOTO_INPUT) {
						@Override
						public void run() {
							openInWorkspace(selectedFiles);
						}
					};
					// Enable only if at least one file exists in workspace
					boolean anyInWorkspace = selectedFiles.stream()
							.anyMatch(f -> f.getWorkspaceFile() != null);
					openInWorkspaceAction.setEnabled(anyInWorkspace);
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
	 * Opens the selected files in the workspace editor.
	 *
	 * @param files
	 *            the files to open
	 */
	private void openInWorkspace(List<PullRequestChangedFile> files) {
		for (PullRequestChangedFile file : files) {
			IFile workspaceFile = file.getWorkspaceFile();
			if (workspaceFile != null && workspaceFile.exists()) {
				try {
					IDE.openEditor(getSite().getPage(), workspaceFile);
				} catch (PartInitException e) {
					Activator.logError("Failed to open file in editor: " //$NON-NLS-1$
							+ workspaceFile.getFullPath(), e);
				}
			}
		}
	}
}
