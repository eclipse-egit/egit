/*******************************************************************************
 * Copyright (C) 2011, 2013 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.GitLabels;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jgit.diff.DiffConfig;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Allows to select a single commit
 */
public class CommitSelectionDialog extends TitleAreaDialog {
	private static final int BATCH_SIZE = 256;

	private final Repository repository;

	private final IResource[] filterResources;

	private CommitGraphTable table;

	private SWTCommitList allCommits;

	private RevFlag highlightFlag;

	private ObjectId commitId;

	/**
	 * @param parentShell
	 * @param repository
	 */
	public CommitSelectionDialog(Shell parentShell, Repository repository) {
		this(parentShell, repository, null);
	}

	/**
	 * Create a commit selection dialog which shows only commits which changed
	 * the given resources.
	 *
	 * @param parentShell
	 * @param repository
	 * @param filterResources
	 *            the resources to use to filter commits, null for no filter
	 */
	public CommitSelectionDialog(Shell parentShell, Repository repository,
			IResource[] filterResources) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.repository = repository;
		this.filterResources = filterResources;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		final ResourceManager resources = new LocalResourceManager(
				JFaceResources.getResources());
		UIUtils.hookDisposal(main, resources);
		// Table never shows e-mail addresses because it might get rather wide.
		table = new CommitGraphTable(main, null, resources, false);
		table.getTableView().addSelectionChangedListener(
				new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						commitId = null;
						IStructuredSelection sel = (IStructuredSelection) event
								.getSelection();
						if (sel.size() == 1)
							commitId = ((SWTCommit) sel.getFirstElement())
									.getId();
						getButton(OK).setEnabled(commitId != null);
					}
				});
		table.getTableView().addOpenListener(new IOpenListener() {
			@Override
			public void open(OpenEvent event) {
				if (getButton(OK).isEnabled())
					buttonPressed(OK);
			}
		});
		// allow for some room here
		GridDataFactory.fillDefaults().grab(true, true).minSize(SWT.DEFAULT,
				400).applyTo(table.getControl());
		allCommits = new SWTCommitList(resources);
		table.getControl().addDisposeListener(e -> allCommits.clear());
		return main;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.CommitSelectionDialog_WindowTitle);
	}

	@Override
	public void create() {
		super.create();
		getButton(OK).setEnabled(false);
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true,
					new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							monitor.beginTask(
									UIText.CommitSelectionDialog_BuildingCommitListMessage,
									IProgressMonitor.UNKNOWN);
							try (SWTWalk currentWalk = new SWTWalk(
									repository)) {
								currentWalk.setTreeFilter(createTreeFilter());
								currentWalk.sort(RevSort.COMMIT_TIME_DESC,
										true);
								currentWalk.sort(RevSort.BOUNDARY, true);
								highlightFlag = currentWalk
										.newFlag("highlight"); //$NON-NLS-1$
								allCommits.source(currentWalk);

								if (Activator.getDefault().getPreferenceStore()
										.getBoolean(
												UIPreferences.RESOURCEHISTORY_SHOW_ALL_BRANCHES)) {
									markStartAllRefs(currentWalk,
											Constants.R_HEADS);
									markStartAllRefs(currentWalk,
											Constants.R_REMOTES);
								} else {
									currentWalk
											.markStart(currentWalk.parseCommit(
													repository.resolve(
															Constants.HEAD)));
								}
								for (;;) {
									final int oldsz = allCommits.size();
									allCommits.fillTo(oldsz + BATCH_SIZE - 1);

									if (monitor.isCanceled()
											|| oldsz == allCommits.size()) {
										break;
									}
									String taskName = MessageFormat.format(
											UIText.CommitSelectionDialog_FoundCommitsMessage,
											Integer.valueOf(allCommits.size()));
									monitor.setTaskName(taskName);
								}
								if (monitor.isCanceled()) {
									throw new InterruptedException();
								}
								getShell().getDisplay().asyncExec(() -> updateUi());
							} catch (IOException e) {
								throw new InvocationTargetException(e);
							} finally {
								monitor.done();
							}
						}
					});
		} catch (InvocationTargetException e) {
			setErrorMessage(e.getCause().getMessage());
		} catch (InterruptedException e) {
			setMessage(UIText.CommitSelectionDialog_IncompleteListMessage,
					IMessageProvider.WARNING);
		}
	}

	/**
	 * @return the commit id
	 */
	public ObjectId getCommitId() {
		return commitId;
	}

	private void updateUi() {
		setTitle(MessageFormat.format(UIText.CommitSelectionDialog_DialogTitle,
				Integer.valueOf(allCommits.size()),
				GitLabels.getPlainShortLabel(repository)));
		setMessage(UIText.CommitSelectionDialog_DialogMessage);
		table.setInput(highlightFlag, allCommits, allCommits
				.toArray(new SWTCommit[0]), null, true);
	}

	private void markStartAllRefs(RevWalk currentWalk, String prefix)
			throws IOException, MissingObjectException,
			IncorrectObjectTypeException {
		for (Ref ref : repository.getRefDatabase().getRefsByPrefix(prefix)) {
			if (ref.isSymbolic())
				continue;
			currentWalk.markStart(currentWalk.parseCommit(ref.getObjectId()));
		}
	}

	private TreeFilter createTreeFilter() {
		if (filterResources == null)
			return TreeFilter.ALL;

		List<TreeFilter> filters = new ArrayList<>();
		for (IResource resource : filterResources) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
			if (mapping != null) {
				DiffConfig diffConfig = mapping.getRepository().getConfig().get(DiffConfig.KEY);
				String path = mapping.getRepoRelativePath(resource);
				if (path != null && !"".equals(path)) { //$NON-NLS-1$
					if (resource.getType() == IResource.FILE)
						filters.add(FollowFilter.create(path, diffConfig));
					else
						filters.add(AndTreeFilter.create(
								PathFilter.create(path), TreeFilter.ANY_DIFF));
				}
			}
		}

		if (filters.isEmpty())
			return TreeFilter.ALL;
		else if (filters.size() == 1)
			return filters.get(0);
		else
			return OrTreeFilter.create(filters);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				UIText.HistoryCommitSelectionDialog_ButtonOK, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

}
