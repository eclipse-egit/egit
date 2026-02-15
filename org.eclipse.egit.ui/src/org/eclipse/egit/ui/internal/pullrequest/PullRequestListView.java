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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.bitbucket.BitbucketClient;
import org.eclipse.egit.core.internal.bitbucket.PullRequest;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

/**
 * View for displaying a list of Bitbucket Data Center pull requests
 */
public class PullRequestListView extends ViewPart {

	/**
	 * View ID
	 */
	public static final String VIEW_ID = "org.eclipse.egit.ui.PullRequestListView"; //$NON-NLS-1$

	private FormToolkit toolkit;

	private Form form;

	private TreeViewer pullRequestViewer;

	private PreferenceBasedDateFormatter dateFormatter;

	private ResourceManager imageCache;

	private List<PullRequest> pullRequests = new ArrayList<>();

	private Action refreshAction;

	// Filter controls
	private Text authorFilterText;

	private Text titleFilterText;

	private Combo stateCombo;

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

		// Create composite for filters below headers and tree
		Composite tableComposite = new Composite(form.getBody(), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableComposite);
		GridLayoutFactory.fillDefaults().applyTo(tableComposite);

		// Create filter row
		createFilterRow(tableComposite);

		TreeColumnLayout treeColumnLayout = new TreeColumnLayout();
		Composite layoutComposite = new Composite(tableComposite, SWT.NONE);
		layoutComposite.setLayout(treeColumnLayout);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(layoutComposite);

		pullRequestViewer = new TreeViewer(layoutComposite,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		pullRequestViewer.getTree().setHeaderVisible(true);
		pullRequestViewer.getTree().setLinesVisible(true);

		setupColumns(treeColumnLayout);

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

		// Register as selection provider for view communication
		getSite().setSelectionProvider(pullRequestViewer);

		createActions();
		contributeToActionBars();

		// Automatically refresh pull requests when view opens
		refreshPullRequests();
	}

	private void setupColumns(TreeColumnLayout layout) {
		// ID Column
		TreeViewerColumn idColumn = createColumn(layout, "ID", 10, SWT.LEFT); //$NON-NLS-1$
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
		TreeViewerColumn titleColumn = createColumn(layout, "Title", 40, //$NON-NLS-1$
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
						return UIIcons.getImage(getImageCache(),
								UIIcons.BRANCH);
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
		TreeViewerColumn authorColumn = createColumn(layout, "Author", 20, //$NON-NLS-1$
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
		TreeViewerColumn stateColumn = createColumn(layout, "State", 10, //$NON-NLS-1$
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
		TreeViewerColumn commentsColumn = createColumn(layout, "Comments", 10, //$NON-NLS-1$
				SWT.LEFT);
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
		TreeViewerColumn updatedColumn = createColumn(layout, "Updated", 20, //$NON-NLS-1$
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
	}

	private TreeViewerColumn createColumn(TreeColumnLayout layout, String text,
			int weight, int style) {
		TreeViewerColumn column = new TreeViewerColumn(pullRequestViewer,
				style);
		column.getColumn().setText(text);
		layout.setColumnData(column.getColumn(), new ColumnWeightData(weight));
		return column;
	}

	/**
	 * Creates a filter row with text fields aligned to table columns
	 */
	private void createFilterRow(Composite parent) {
		Composite filterRow = toolkit.createComposite(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(filterRow);
		GridLayoutFactory.fillDefaults().numColumns(6).equalWidth(false)
				.applyTo(filterRow);

		// ID column - no filter (10% width)
		Label idSpacer = new Label(filterRow, SWT.NONE);
		GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).applyTo(idSpacer);

		// Title column filter (40% width)
		titleFilterText = new Text(filterRow, SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		titleFilterText.setMessage("Filter title..."); //$NON-NLS-1$
		GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT)
				.applyTo(titleFilterText);
		titleFilterText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					refreshPullRequests();
				}
			}
		});
		titleFilterText.addModifyListener(e -> {
			// Live filtering for title (client-side)
			filterAndRefreshViewer();
		});

		// Author column filter (20% width)
		authorFilterText = new Text(filterRow, SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		authorFilterText.setMessage("Filter author..."); //$NON-NLS-1$
		GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT)
				.applyTo(authorFilterText);
		
		// Default to configured username
		String username = Activator.getDefault().getPreferenceStore()
				.getString(UIPreferences.BITBUCKET_USERNAME);
		if (username != null && !username.isEmpty()) {
			authorFilterText.setText(username);
		}
		
		authorFilterText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					refreshPullRequests();
				}
			}
		});

		// State column filter (10% width)
		stateCombo = new Combo(filterRow, SWT.READ_ONLY | SWT.BORDER);
		stateCombo.setItems(new String[] { "OPEN", "MERGED", "DECLINED", "ALL" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		stateCombo.select(0); // Default to OPEN
		GridDataFactory.fillDefaults().hint(80, SWT.DEFAULT)
				.applyTo(stateCombo);
		stateCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshPullRequests();
			}
		});

		// Comments column - no filter (10% width)
		Label commentsSpacer = new Label(filterRow, SWT.NONE);
		GridDataFactory.fillDefaults().hint(60, SWT.DEFAULT)
				.applyTo(commentsSpacer);

		// Updated column - no filter (20% width)
		Label updatedSpacer = new Label(filterRow, SWT.NONE);
		GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT)
				.applyTo(updatedSpacer);
	}

	/**
	 * Filters the viewer content based on title filter (client-side filtering)
	 */
	private void filterAndRefreshViewer() {
		String titleFilter = titleFilterText.getText().trim().toLowerCase();
		
		List<PullRequest> filtered = new ArrayList<>();
		for (PullRequest pr : pullRequests) {
			if (titleFilter.isEmpty()
					|| pr.getTitle().toLowerCase().contains(titleFilter)) {
				filtered.add(pr);
			}
		}
		
		Display.getDefault().asyncExec(() -> {
			if (!pullRequestViewer.getControl().isDisposed()) {
				pullRequestViewer.setInput(filtered);
				pullRequestViewer.refresh();
				updateFormTitle(filtered.size());
			}
		});
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
	}

	private void contributeToActionBars() {
		IToolBarManager toolBarManager = getViewSite().getActionBars()
				.getToolBarManager();
		toolBarManager.add(refreshAction);
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

		if (serverUrl.isEmpty() || token.isEmpty() || projectKey.isEmpty()
				|| repoSlug.isEmpty()) {
			form.setText("Pull Requests - Not configured"); //$NON-NLS-1$
			return;
		}

		// Get filter values from UI controls
		final String authorFilter = authorFilterText.getText().trim();
		final String titleFilter = titleFilterText.getText().trim();
		final String selectedState = stateCombo.getText();
		final String stateFilter = "ALL".equals(selectedState) ? null : selectedState; //$NON-NLS-1$

		Job job = new Job("Fetching pull requests") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Fetching pull requests from Bitbucket", //$NON-NLS-1$
						IProgressMonitor.UNKNOWN);

				try {
					BitbucketClient client = new BitbucketClient(serverUrl,
							token);

					// Apply author filter to server request (empty string means all authors)
					String serverAuthorFilter = authorFilter.isEmpty() ? null : authorFilter;

					// Fetch PRs with server-side filters
					String jsonResponse = client.getPullRequests(projectKey,
							repoSlug, stateFilter, serverAuthorFilter, null, 100, 0);

					// Parse JSON response
					List<PullRequest> fetchedPRs = PullRequestJsonParser
							.parsePullRequests(jsonResponse);

					// Apply client-side title filter if specified
					List<PullRequest> filteredPRs = fetchedPRs;
					if (!titleFilter.isEmpty()) {
						filteredPRs = new ArrayList<>();
						String lowerTitleFilter = titleFilter.toLowerCase();
						for (PullRequest pr : fetchedPRs) {
							if (pr.getTitle() != null && pr.getTitle().toLowerCase()
									.contains(lowerTitleFilter)) {
								filteredPRs.add(pr);
							}
						}
					}

					// Build filter info for display
					final List<String> filterParts = new ArrayList<>();
					if (!authorFilter.isEmpty()) {
						filterParts.add("Author: " + authorFilter); //$NON-NLS-1$
					}
					if (!titleFilter.isEmpty()) {
						filterParts.add("Title: " + titleFilter); //$NON-NLS-1$
					}
					filterParts.add("State: " + selectedState); //$NON-NLS-1$
					final String filterInfo = String.join(", ", filterParts); //$NON-NLS-1$

					final List<PullRequest> finalPRs = filteredPRs;
					Display.getDefault().asyncExec(() -> {
						if (!pullRequestViewer.getControl().isDisposed()) {
							pullRequests.clear();
							pullRequests.addAll(finalPRs);
							pullRequestViewer.setInput(pullRequests);
							pullRequestViewer.refresh();
							
							// Build filter info for display
							List<String> displayFilters = new ArrayList<>();
							if (!authorFilter.isEmpty()) {
								displayFilters.add("Author: " + authorFilter); //$NON-NLS-1$
							}
							if (!titleFilter.isEmpty()) {
								displayFilters.add("Title: " + titleFilter); //$NON-NLS-1$
							}
							displayFilters.add("State: " + selectedState); //$NON-NLS-1$
							
							String filterDisplay = String.join(", ", displayFilters); //$NON-NLS-1$
							form.setText(MessageFormat.format(
									"Pull Requests ({0}) - {1}", //$NON-NLS-1$
									Integer.valueOf(pullRequests.size()),
									filterDisplay));
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

	/**
	 * Updates the form title with the current PR count
	 *
	 * @param count
	 *            the number of PRs currently displayed
	 */
	private void updateFormTitle(int count) {
		form.setText(MessageFormat.format("Pull Requests ({0})", //$NON-NLS-1$
				Integer.valueOf(count)));
	}

	@Override
	public void setFocus() {
		pullRequestViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		if (imageCache != null) {
			imageCache.dispose();
		}
		if (toolkit != null) {
			toolkit.dispose();
		}
		super.dispose();
	}

	private ResourceManager getImageCache() {
		if (imageCache == null) {
			imageCache = new LocalResourceManager(
					JFaceResources.getResources());
		}
		return imageCache;
	}
}
