/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.egit.core.op.CommitOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.actions.ActionCommands;
import org.eclipse.egit.ui.internal.commit.CommitHelper;
import org.eclipse.egit.ui.internal.commit.CommitUI;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponent;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponentState;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponentStateManager;
import org.eclipse.egit.ui.internal.dialogs.ICommitMessageComponentNotifications;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.events.IndexChangedEvent;
import org.eclipse.jgit.events.IndexChangedListener;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

/**
 * A GitX style staging view with embedded commit dialog.
 */
public class StagingView extends ViewPart {
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private Label repositoryLabel;

	private TableViewer stagedTableViewer;

	private TableViewer unstagedTableViewer;

	private SpellcheckableMessageArea commitMessageText;

	private Text committerText;

	private Text authorText;

	private Button commitButton;

	private CommitMessageComponent commitMessageComponent;

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

	private Button signedOffByButton;

	private Button addChangeIdButton;

	private Button amendPreviousCommitButton;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));

		repositoryLabel = new Label(parent, SWT.NONE);
		repositoryLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 1, 1));

		SashForm horizontalSashForm = new SashForm(parent, SWT.NONE);
		horizontalSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true, 1, 1));

		Composite leftHandComposite = new Composite(horizontalSashForm,
				SWT.NONE);
		leftHandComposite.setLayout(new GridLayout(1, false));

		SashForm veriticalSashForm = new SashForm(leftHandComposite,
				SWT.VERTICAL);
		veriticalSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true, 1, 1));

		Composite unstagedComposite = new Composite(veriticalSashForm, SWT.NONE);
		unstagedComposite.setLayout(new GridLayout(1, false));

		new Label(unstagedComposite, SWT.NONE)
				.setText(UIText.StagingView_UnstagedChanges);

		Composite unstagedTableComposite = new Composite(unstagedComposite,
				SWT.NONE);
		unstagedTableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				true, true, 1, 1));
		unstagedTableComposite.setLayout(new TableColumnLayout());

		unstagedTableViewer = new TableViewer(unstagedTableComposite,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		unstagedTableViewer.getTable().setLinesVisible(true);
		unstagedTableViewer.setLabelProvider(new StagingViewLabelProvider());
		unstagedTableViewer.setContentProvider(new StagingViewContentProvider(
				true));
		unstagedTableViewer.addDragSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DragSourceAdapter() {
					public void dragStart(DragSourceEvent event) {
						IStructuredSelection selection = (IStructuredSelection) unstagedTableViewer
								.getSelection();
						event.doit = !selection.isEmpty();
					}
				});
		unstagedTableViewer.addDropSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DropTargetAdapter() {
					public void drop(DropTargetEvent event) {
						final IStructuredSelection selection = (IStructuredSelection) stagedTableViewer
								.getSelection();
						unstage(selection);
					}

					public void dragOver(DropTargetEvent event) {
						event.detail = DND.DROP_MOVE;
					}
				});
		unstagedTableViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				compareWith(event);
			}
		});

		Composite commitMessageComposite = new Composite(horizontalSashForm,
				SWT.NONE);
		commitMessageComposite.setLayout(new GridLayout(2, false));

		Label commitMessageLabel = new Label(commitMessageComposite, SWT.NONE);
		commitMessageLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
				false, false, 2, 1));
		commitMessageLabel.setText(UIText.StagingView_CommitMessage);

		commitMessageText = new SpellcheckableMessageArea(
				commitMessageComposite, EMPTY_STRING);
		commitMessageText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true, 2, 1));

		Composite composite = new Composite(commitMessageComposite, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,
				2, 1));
		composite.setLayout(new GridLayout(2, false));

		new Label(composite, SWT.NONE).setText(UIText.StagingView_Committer);

		committerText = new Text(composite, SWT.BORDER);
		committerText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 1, 1));

		new Label(composite, SWT.NONE).setText(UIText.StagingView_Author);

		authorText = new Text(composite, SWT.BORDER);
		authorText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 1, 1));

		amendPreviousCommitButton = new Button(commitMessageComposite,
				SWT.CHECK);
		amendPreviousCommitButton.setLayoutData(new GridData(SWT.LEFT,
				SWT.CENTER, false, false, 2, 1));
		amendPreviousCommitButton
				.setText(UIText.StagingView_Ammend_Previous_Commit);

		signedOffByButton = new Button(commitMessageComposite, SWT.CHECK);
		signedOffByButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
				false, false, 2, 1));
		signedOffByButton.setText(UIText.StagingView_Add_Signed_Off_By);

		addChangeIdButton = new Button(commitMessageComposite, SWT.CHECK);
		GridData addChangeIdButtonGridData = new GridData(SWT.LEFT, SWT.CENTER,
				false, false, 1, 1);
		addChangeIdButtonGridData.minimumHeight = 1;
		addChangeIdButton.setLayoutData(addChangeIdButtonGridData);
		addChangeIdButton.setText(UIText.StagingView_Add_Change_ID);

		final ICommitMessageComponentNotifications listener = new ICommitMessageComponentNotifications() {

			public void updateSignedOffToggleSelection(boolean selection) {
				signedOffByButton.setSelection(selection);
			}

			public void updateChangeIdToggleSelection(boolean selection) {
				addChangeIdButton.setSelection(selection);
			}
		};
		commitMessageComponent = new CommitMessageComponent(listener);
		commitMessageComponent.attachControls(commitMessageText, authorText,
				committerText);
		addChangeIdButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				commitMessageComponent
						.setChangeIdButtonSelection(addChangeIdButton
								.getSelection());
			}
		});
		signedOffByButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				commitMessageComponent
						.setSignedOffButtonSelection(signedOffByButton
								.getSelection());
			}
		});
		amendPreviousCommitButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				commitMessageComponent
						.setAmendingButtonSelection(amendPreviousCommitButton
								.getSelection());
			}
		});

		commitButton = new Button(commitMessageComposite, SWT.NONE);
		commitButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		commitButton.setText(UIText.StagingView_Commit);
		commitButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				commit();
			}

		});

		Composite stagedComposite = new Composite(veriticalSashForm, SWT.NONE);
		stagedComposite.setLayout(new GridLayout(1, false));

		new Label(stagedComposite, SWT.NONE)
				.setText(UIText.StagingView_StagedChanges);

		Composite stagedTableComposite = new Composite(stagedComposite,
				SWT.NONE);
		stagedTableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				true, true, 1, 1));
		stagedTableComposite.setLayout(new TableColumnLayout());

		stagedTableViewer = new TableViewer(stagedTableComposite, SWT.BORDER
				| SWT.FULL_SELECTION | SWT.MULTI);
		stagedTableViewer.getTable().setLinesVisible(true);
		stagedTableViewer.setLabelProvider(new StagingViewLabelProvider());
		stagedTableViewer.setContentProvider(new StagingViewContentProvider(
				false));
		stagedTableViewer.addDragSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DragSourceAdapter() {
					public void dragStart(DragSourceEvent event) {
						IStructuredSelection selection = (IStructuredSelection) stagedTableViewer
								.getSelection();
						event.doit = !selection.isEmpty();
					}
				});
		stagedTableViewer.addDropSupport(DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DropTargetAdapter() {
					public void drop(DropTargetEvent event) {
						final IStructuredSelection selection = (IStructuredSelection) unstagedTableViewer
								.getSelection();
						stage(selection);
					}

					public void dragOver(DropTargetEvent event) {
						event.detail = DND.DROP_MOVE;
					}
				});
		stagedTableViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				compareWith(event);
			}
		});

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

		horizontalSashForm.setWeights(new int[] { 40, 60 });
		veriticalSashForm.setWeights(new int[] { 50, 50 });

		// react on selection changes
		ISelectionService srv = (ISelectionService) getSite().getService(
				ISelectionService.class);
		srv.addPostSelectionListener(selectionChangedListener);

		getSite().setSelectionProvider(unstagedTableViewer);
	}

	private void compareWith(OpenEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event
				.getSelection();
		if (selection.isEmpty())
			return;
		StagingEntry stagingEntry = (StagingEntry) selection.getFirstElement();
		switch (stagingEntry.getState()) {
		case ADDED:
		case CHANGED:
		case REMOVED:
			runCommand(ActionCommands.COMPARE_INDEX_WITH_HEAD_ACTION, selection);
			break;

		case MISSING:
		case MODIFIED:
		case PARTIALLY_MODIFIED:
		case CONFLICTING:
		case UNTRACKED:
		default:
			// compare with index
			runCommand(ActionCommands.COMPARE_WITH_INDEX_ACTION, selection);
		}
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
			} else if (ssel.getFirstElement() instanceof RepositoryTreeNode) {
				RepositoryTreeNode repoNode = (RepositoryTreeNode) ssel
						.getFirstElement();
				reload(repoNode.getRepository());
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
		myListeners.add(repository.getListenerList().addIndexChangedListener(
				myIndexChangedListener));
		myListeners.add(repository.getListenerList().addRefsChangedListener(
				myRefsChangedListener));
	}

	private void removeListeners() {
		for (ListenerHandle lh : myListeners)
			lh.remove();
		myListeners.clear();
	}

	private void stage(IStructuredSelection selection) {
		Git git = new Git(currentRepository);
		AddCommand add = null;
		RmCommand rm = null;
		Iterator iterator = selection.iterator();
		while (iterator.hasNext()) {
			StagingEntry entry = (StagingEntry) iterator.next();
			switch (entry.getState()) {
			case ADDED:
			case CHANGED:
			case REMOVED:
				// already staged
				break;
			case CONFLICTING:
			case MODIFIED:
			case PARTIALLY_MODIFIED:
			case UNTRACKED:
				if (add == null)
					add = git.add();
				add.addFilepattern(entry.getPath());
				break;
			case MISSING:
				if (rm == null)
					rm = git.rm();
				rm.addFilepattern(entry.getPath());
				break;
			}
		}

		if (add != null)
			try {
				add.call();
			} catch (NoFilepatternException e1) {
				// cannot happen
			}
		if (rm != null)
			try {
				rm.call();
			} catch (NoFilepatternException e) {
				// cannot happen
			}

		reload(currentRepository);
	}

	private void unstage(IStructuredSelection selection) {
		if (selection.isEmpty())
			return;

		final RevCommit headRev;
		try {
			final Ref head = currentRepository.getRef(Constants.HEAD);
			headRev = new RevWalk(currentRepository).parseCommit(head
					.getObjectId());
		} catch (IOException e1) {
			// TODO fix text
			MessageDialog.openError(getSite().getShell(),
					UIText.CommitAction_MergeHeadErrorTitle,
					UIText.CommitAction_ErrorReadingMergeMsg);
			return;
		}

		final DirCache dirCache;
		final DirCacheEditor edit;
		try {
			dirCache = currentRepository.lockDirCache();
			edit = dirCache.editor();
		} catch (IOException e) {
			// TODO fix text
			MessageDialog.openError(getSite().getShell(),
					UIText.CommitAction_MergeHeadErrorTitle,
					UIText.CommitAction_ErrorReadingMergeMsg);
			return;
		}

		try {
			updateDirCache(selection, headRev, edit);

			try {
				edit.commit();
			} catch (IOException e) {
				// TODO fix text
				MessageDialog.openError(getSite().getShell(),
						UIText.CommitAction_MergeHeadErrorTitle,
						UIText.CommitAction_ErrorReadingMergeMsg);
			}
		} finally {
			dirCache.unlock();
		}

		reload(currentRepository);
	}

	private void updateDirCache(IStructuredSelection selection,
			final RevCommit headRev, final DirCacheEditor edit) {
		Iterator iterator = selection.iterator();
		while (iterator.hasNext()) {
			StagingEntry entry = (StagingEntry) iterator.next();
			switch (entry.getState()) {
			case ADDED:
				edit.add(new DirCacheEditor.DeletePath(entry.getPath()));
				break;
			case CHANGED:
			case REMOVED:
				// set the index object id/file mode back to our head revision
				try {
					final TreeWalk tw = TreeWalk.forPath(currentRepository,
							entry.getPath(), headRev.getTree());
					if (tw != null) {
						edit.add(new DirCacheEditor.PathEdit(entry.getPath()) {
							@Override
							public void apply(DirCacheEntry ent) {
								ent.setFileMode(tw.getFileMode(0));
								ent.setObjectId(tw.getObjectId(0));
								// for index & working tree compare
								ent.setLastModified(0);
							}
						});
					}
				} catch (IOException e) {
					// TODO fix text
					MessageDialog.openError(getSite().getShell(),
							UIText.CommitAction_MergeHeadErrorTitle,
							UIText.CommitAction_ErrorReadingMergeMsg);
				}
				break;
			default:
				// unstaged
			}
		}
	}

	private static boolean runCommand(String commandId,
			IStructuredSelection selection) {
		ICommandService commandService = (ICommandService) PlatformUI
				.getWorkbench().getService(ICommandService.class);
		Command cmd = commandService.getCommand(commandId);
		if (!cmd.isDefined()) {
			return false;
		}

		IHandlerService handlerService = (IHandlerService) PlatformUI
				.getWorkbench().getService(IHandlerService.class);
		EvaluationContext c = null;
		if (selection != null) {
			c = new EvaluationContext(
					handlerService.createContextSnapshot(false),
					selection.toList());
			c.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME, selection);
			c.removeVariable(ISources.ACTIVE_MENU_SELECTION_NAME);
		}
		try {
			if (c != null) {
				handlerService.executeCommandInContext(
						new ParameterizedCommand(cmd, null), null, c);
			} else {
				handlerService.executeCommand(commandId, null);
			}
			return true;
		} catch (ExecutionException e) {
		} catch (NotDefinedException e) {
		} catch (NotEnabledException e) {
		} catch (NotHandledException e) {
		}
		return false;
	}

	// TODO move to a Job?
	private IndexDiff reload(final Repository repository) {
		final boolean repositoryChanged = repository != currentRepository;
		currentRepository = repository;

		final IndexDiff indexDiff;
		try {
			WorkingTreeIterator iterator = IteratorService
					.createInitialIterator(repository);
			indexDiff = new IndexDiff(repository, Constants.HEAD, iterator);
			indexDiff.diff();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		removeListeners();
		attachListeners(repository);

		unstagedTableViewer.getTable().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!unstagedTableViewer.getTable().isDisposed())
					unstagedTableViewer.setInput(new Object[] { repository,
							indexDiff });
				if (!stagedTableViewer.getTable().isDisposed())
					stagedTableViewer.setInput(new Object[] { repository,
							indexDiff });
				if (!commitButton.isDisposed())
					commitButton.setEnabled(repository.getRepositoryState()
							.canCommit());
				if (!repositoryLabel.isDisposed()) {
					repositoryLabel.setText(StagingView
							.getRepositoryName(repository));
				}
				updateCommitMessageComponent(repositoryChanged);
			}

		});

		return indexDiff;
	}

	private void clearCommitMessageToggles() {
		amendPreviousCommitButton.setSelection(false);
		addChangeIdButton.setSelection(false);
		signedOffByButton.setSelection(false);
	}

	void updateCommitMessageComponent(boolean repositoryChanged) {
		CommitHelper helper = new CommitHelper(currentRepository);
		CommitMessageComponentState oldState = null;
		if (repositoryChanged) {
			if (userEnteredCommmitMessage())
				saveCommitMessageComponentState();
			else
				deleteCommitMessageComponentState();
			oldState = loadCommitMessageComponentState();
			commitMessageComponent.setRepository(currentRepository);
			if (oldState == null) {
				loadInitialState(helper);
			} else {
				loadExistingState(helper, oldState);
			}
		} else {
			// repository did not change
			if (userEnteredCommmitMessage()) {
				if (!commitMessageComponent.getHeadCommit().equals(helper.getPreviousCommit()))
					addHeadChangedWarning(commitMessageComponent.getCommitMessage());
			} else
				loadInitialState(helper);
		}
		amendPreviousCommitButton.setSelection(commitMessageComponent
				.isAmending());
	}

	private void loadExistingState(CommitHelper helper,
			CommitMessageComponentState oldState) {
		boolean headCommitChanged = !oldState.getHeadCommit().equals(
				getCommitId(helper.getPreviousCommit()));
		commitMessageComponent.enableListers(false);
		commitMessageComponent.setAuthor(oldState.getAuthor());
		if (headCommitChanged)
			addHeadChangedWarning(oldState.getCommitMessage());
		else
			commitMessageComponent.setCommitMessage(oldState
					.getCommitMessage());
		commitMessageComponent.setCommitter(oldState.getCommitter());
		commitMessageComponent.setHeadCommit(getCommitId(helper
				.getPreviousCommit()));
		if (!headCommitChanged && oldState.getAmend())
			commitMessageComponent.setAmending(true);
		else
			commitMessageComponent.setAmending(false);
		commitMessageComponent.updateUIFromState();
		commitMessageComponent.updateSignedOffAndChangeIdButton();
		commitMessageComponent.enableListers(true);
	}

	private void addHeadChangedWarning(String commitMessage) {
		String message = UIText.StagingView_headCommitChanged + "\n\n" + //$NON-NLS-1$
				commitMessage;
		commitMessageComponent.setCommitMessage(message);
	}

	private void loadInitialState(CommitHelper helper) {
		commitMessageComponent.enableListers(false);
		commitMessageComponent.resetState();
		commitMessageComponent.setAuthor(helper.getAuthor());
		commitMessageComponent.setCommitMessage(helper
				.getCommitMessage());
		commitMessageComponent.setCommitter(helper.getCommitter());
		commitMessageComponent.setHeadCommit(getCommitId(helper
				.getPreviousCommit()));
		commitMessageComponent.setAmendAllowed(true);
		commitMessageComponent.setAmending(false);
		commitMessageComponent.setSignedOff(false);
		commitMessageComponent.setCreateChangeId(false);
		commitMessageComponent.updateUI();
		commitMessageComponent.enableListers(true);
	}

	private boolean userEnteredCommmitMessage() {
		if (commitMessageComponent.getRepository() == null)
			return false;
		String message = commitMessageComponent.getCommitMessage();
		if (message == null || message.trim().length() == 0)
			return false;
		return true;
	}

	private ObjectId getCommitId(RevCommit commit) {
		if (commit == null)
			return ObjectId.zeroId();
		return commit.getId();
	}

	private void saveCommitMessageComponentState() {
		CommitMessageComponentStateManager.persistState(
				commitMessageComponent.getRepository(),
				commitMessageComponent.getState());
	}

	private void deleteCommitMessageComponentState() {
		if (commitMessageComponent.getRepository() != null)
			CommitMessageComponentStateManager
					.deleteState(commitMessageComponent.getRepository());
	}

	private CommitMessageComponentState loadCommitMessageComponentState() {
		return CommitMessageComponentStateManager.loadState(currentRepository);
	}

	private static String getRepositoryName(Repository repository) {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		RepositoryState state = repository.getRepositoryState();
		if (state != RepositoryState.SAFE)
			return repoName + '|' + state.getDescription();
		else
			return repoName;
	}

	private void commit() {
		if (!commitMessageComponent.checkCommitInfo())
			return;
		CommitOperation commitOperation = null;
		try {
			commitOperation = new CommitOperation(currentRepository,
					commitMessageComponent.getAuthor(),
					commitMessageComponent.getCommitter(),
					commitMessageComponent.getCommitMessage());
		} catch (CoreException e) {
			Activator.handleError(UIText.StagingView_commitFailed, e, true);
			return;
		}
		if (amendPreviousCommitButton.getSelection())
			commitOperation.setAmending(true);
		commitOperation.setComputeChangeId(addChangeIdButton.getSelection());
		CommitUI.performCommit(currentRepository, commitOperation);
		clearCommitMessageToggles();
		commitMessageText.setText(EMPTY_STRING);
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
