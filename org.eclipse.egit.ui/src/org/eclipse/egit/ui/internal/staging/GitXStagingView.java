/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.events.IndexChangedEvent;
import org.eclipse.jgit.events.IndexChangedListener;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.UserConfig;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

/**
 * A GitX style staging view with embedded commit dialog.
 */
public class GitXStagingView extends ViewPart {
	private TableViewer stagedTableViewer;
	private TableViewer unstagedTableViewer;
	private SpellcheckableMessageArea commitMessageText;
	private Text committerText;
	private Text authorText;
	private Button commitButton;

	private boolean reactOnSelection = true;

	private final List<ListenerHandle> myListeners = new LinkedList<ListenerHandle>();
	private ISelectionListener selectionChangedListener;
	private Repository currentRepository;

	private final RefsChangedListener myRefsChangedListener = new RefsChangedListener() {
		public void onRefsChanged(RefsChangedEvent event) {
			// refs change when files are committed, we naturally want to remove
			// committed files from the view
			reload(event.getRepository());
		}
	};

	private final IndexChangedListener myIndexChangedListener = new IndexChangedListener() {
		public void onIndexChanged(IndexChangedEvent event) {
			reload(event.getRepository());
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout(SWT.HORIZONTAL));

		SashForm sashForm = new SashForm(parent, SWT.NONE);

		Composite unstagedComposite = new Composite(sashForm, SWT.NONE);
		unstagedComposite.setLayout(new GridLayout(1, false));

		Label lblUnstagedChanges = new Label(unstagedComposite, SWT.NONE);
		lblUnstagedChanges.setText(UIText.GitXStagingView_UnstagedChanges);

		Composite unstagedTableComposite = new Composite(unstagedComposite, SWT.NONE);
		unstagedTableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		TableColumnLayout tcl_unstagedTableComposite = new TableColumnLayout();
		unstagedTableComposite.setLayout(tcl_unstagedTableComposite);

		unstagedTableViewer = new TableViewer(unstagedTableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		Table unstagedChangesTable = unstagedTableViewer.getTable();
		unstagedChangesTable.setLinesVisible(true);
		unstagedTableViewer.setLabelProvider(new GitXStagingViewLabelProvider());
		unstagedTableViewer.setContentProvider(new GitXStagingViewContentProvider(true));

		Composite commitMessageComposite = new Composite(sashForm, SWT.NONE);
		commitMessageComposite.setLayout(new GridLayout(2, false));

		Label lblCommitMessage = new Label(commitMessageComposite, SWT.NONE);
		lblCommitMessage.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblCommitMessage.setText(UIText.GitXStagingView_CommitMessage);

		commitMessageText = new SpellcheckableMessageArea(commitMessageComposite, ""); //$NON-NLS-1$
		commitMessageText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Composite composite = new Composite(commitMessageComposite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		composite.setLayout(new GridLayout(2, false));

		Label lblCommitter = new Label(composite, SWT.NONE);
		lblCommitter.setText(UIText.GitXStagingView_Committer);

		committerText = new Text(composite, SWT.BORDER);
		committerText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblNewLabel = new Label(composite, SWT.NONE);
		lblNewLabel.setText(UIText.GitXStagingView_Author);

		authorText = new Text(composite, SWT.BORDER);
		authorText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Button btnAmendPreviousCommit = new Button(commitMessageComposite, SWT.CHECK);
		btnAmendPreviousCommit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		btnAmendPreviousCommit.setText(UIText.GitXStagingView_Ammend_Previous_Commit);

		Button btnCheckButton = new Button(commitMessageComposite, SWT.CHECK);
		btnCheckButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		btnCheckButton.setText(UIText.GitXStagingView_Add_Signed_Off_By);

		Button btnAddChangeid = new Button(commitMessageComposite, SWT.CHECK);
		GridData gd_btnAddChangeid = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnAddChangeid.minimumHeight = 1;
		btnAddChangeid.setLayoutData(gd_btnAddChangeid);
		btnAddChangeid.setText(UIText.GitXStagingView_Add_Change_ID);

		commitButton = new Button(commitMessageComposite, SWT.NONE);
		commitButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		commitButton.setText(UIText.GitXStagingView_Commit);

		Composite stagedComposite = new Composite(sashForm, SWT.NONE);
		stagedComposite.setLayout(new GridLayout(1, false));

		Label lblStagedChanges = new Label(stagedComposite, SWT.NONE);
		lblStagedChanges.setText(UIText.GitXStagingView_StagedChanges);

		Composite stagedTableComposite = new Composite(stagedComposite, SWT.NONE);
		stagedTableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		stagedTableComposite.setLayout(new TableColumnLayout());

		stagedTableViewer = new TableViewer(stagedTableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		Table stagedChangesTable = stagedTableViewer.getTable();
		stagedChangesTable.setLinesVisible(true);
		stagedTableViewer.setLabelProvider(new GitXStagingViewLabelProvider());
		stagedTableViewer.setContentProvider(new GitXStagingViewContentProvider(false));
		sashForm.setWeights(new int[] {25, 50, 25});

		selectionChangedListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (!reactOnSelection)
					return;

				// this may happen if we switch between editors
				if (part instanceof IEditorPart) {
					IEditorInput input = ((IEditorPart) part).getEditorInput();
					if (input instanceof IFileEditorInput)
						reactOnSelection(new StructuredSelection(
								((IFileEditorInput) input).getFile()));
				} else
					reactOnSelection(selection);
			}
		};

		// react on selection changes
		ISelectionService srv = (ISelectionService) getSite().getService(
				ISelectionService.class);
		srv.addPostSelectionListener(selectionChangedListener);

		getSite().setSelectionProvider(unstagedTableViewer);
	}

	private void reactOnSelection(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			StructuredSelection ssel = (StructuredSelection) selection;
			if (ssel.size() != 1)
				return;
			if (ssel.getFirstElement() instanceof IResource)
				showResource((IResource) ssel.getFirstElement());
			if (ssel.getFirstElement() instanceof IAdaptable) {
				IResource adapted = (IResource) ((IAdaptable) ssel
						.getFirstElement()).getAdapter(IResource.class);
				if (adapted != null)
					showResource(adapted);
			}
		}
	}

	private void showResource(final IResource resource) {
		IProject project = resource.getProject();
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		if (mapping == null)
			return;
		if (mapping.getRepository() != currentRepository) {
			reload(mapping.getRepository());
		}
	}

	private void attachListeners(Repository repository) {
		myListeners.add(repository.getListenerList()
				.addIndexChangedListener(myIndexChangedListener));
		myListeners.add(repository.getListenerList()
				.addRefsChangedListener(myRefsChangedListener));
	}

	private void removeListeners() {
		for (ListenerHandle lh : myListeners)
			lh.remove();
		myListeners.clear();
	}

	// TODO move to a Job?
	private IndexDiff reload(final Repository repository) {
		currentRepository = repository;

		final IndexDiff indexDiff;
		try {
			// TODO IteratorService.createInitialIterator(repository)?
			FileTreeIterator fileTreeIterator = new FileTreeIterator(repository);
			indexDiff = new IndexDiff(repository, Constants.HEAD,
					fileTreeIterator);
			indexDiff.diff();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		removeListeners();
		attachListeners(repository);

		unstagedTableViewer.getTable().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!unstagedTableViewer.getTable().isDisposed())
					unstagedTableViewer.setInput(new Object[] { repository, indexDiff });
				if (!stagedTableViewer.getTable().isDisposed())
					stagedTableViewer.setInput(new Object[] { repository, indexDiff });
				if (!commitButton.isDisposed())
					commitButton.setEnabled(repository.getRepositoryState().canCommit());
				if (!authorText.isDisposed())
					updateAuthorAndCommitter(repository);
				if (!commitMessageText.isDisposed())
					updateCommitMessage(repository);
			}
		});

		return indexDiff;
	}

	private void updateAuthorAndCommitter(Repository repository) {
		final UserConfig config = repository.getConfig().get(UserConfig.KEY);

		String author;
		if (repository.getRepositoryState() == RepositoryState.CHERRY_PICKING_RESOLVED)
			author = getCherryPickOriginalAuthor(repository);
		else {
			author = config.getAuthorName();
			final String authorEmail = config.getAuthorEmail();
			author = author + " <" + authorEmail + ">"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		String committer = config.getCommitterName();
		final String committerEmail = config.getCommitterEmail();
		committer = committer + " <" + committerEmail + ">"; //$NON-NLS-1$ //$NON-NLS-2$

		authorText.setText(author);
		committerText.setText(committer);
	}

	private void updateCommitMessage(Repository repository) {
		if (repository.getRepositoryState() == RepositoryState.MERGING_RESOLVED
				|| repository.getRepositoryState() == RepositoryState.CHERRY_PICKING_RESOLVED)
			commitMessageText.setText(getMergeResolveMessage(repository));
	}

	private String getCherryPickOriginalAuthor(Repository mergeRepository) {
		try {
			ObjectId cherryPickHead = mergeRepository.readCherryPickHead();
			PersonIdent author = new RevWalk(mergeRepository).parseCommit(cherryPickHead).getAuthorIdent();
			return author.getName() + " <" + author.getEmailAddress() + ">";  //$NON-NLS-1$//$NON-NLS-2$
		} catch (IOException e) {
			Activator.handleError(UIText.CommitAction_errorRetrievingCommit, e,
					true);
			throw new IllegalStateException(e);
		}
	}

	private String getMergeResolveMessage(Repository mergeRepository) {
		File mergeMsg = new File(mergeRepository.getDirectory(), Constants.MERGE_MSG);
		FileReader reader;
		try {
			reader = new FileReader(mergeMsg);
			BufferedReader br = new BufferedReader(reader);
			try {
				StringBuilder message = new StringBuilder();
				String s;
				String newLine = newLine();
				while ((s = br.readLine()) != null) {
					message.append(s).append(newLine);
				}
				return message.toString();
			} catch (IOException e) {
				MessageDialog.openError(getSite().getShell(),
						UIText.CommitAction_MergeHeadErrorTitle,
						UIText.CommitAction_ErrorReadingMergeMsg);
				throw new IllegalStateException(e);
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					// Empty
				}
			}
		} catch (FileNotFoundException e) {
			MessageDialog.openError(getSite().getShell(),
					UIText.CommitAction_MergeHeadErrorTitle,
					UIText.CommitAction_MergeHeadErrorMessage);
			throw new IllegalStateException(e);
		}
	}

	private String newLine() {
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

	@Override
	public void setFocus() {
		unstagedTableViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();

		ISelectionService srv = (ISelectionService) getSite().getService(
				ISelectionService.class);
		srv.removePostSelectionListener(selectionChangedListener);

		removeListeners();
	}

}
