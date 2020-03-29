/*******************************************************************************
 *  Copyright (c) 2011, 2020 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Daniel Megert <daniel_megert@ch.ibm.com> - Added context menu to the Commit Editor's header text
 *    Tomasz Zarna <Tomasz.Zarna@pl.ibm.com> - Add "Revert" action to Commit Editor
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.text.MessageFormat;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.ActionCommands;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionManager;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.editor.SharedHeaderFormEditor;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.services.IEvaluationService;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * Editor class to view a commit in a form editor.
 */
public class CommitEditor extends SharedHeaderFormEditor implements
		RefsChangedListener, IShowInSource, IShowInTargetList {

	/**
	 * ID - editor id
	 */
	public static final String ID = "org.eclipse.egit.ui.commitEditor"; //$NON-NLS-1$

	private static final String TOOLBAR_HEADER_ID = ID + ".header.toolbar"; //$NON-NLS-1$

	/**
	 * Open commit in editor
	 *
	 * @param commit
	 * @return opened editor part
	 * @throws PartInitException
	 */
	public static final IEditorPart open(RepositoryCommit commit)
			throws PartInitException {
		return open(commit, true);
	}

	/**
	 * Open commit in editor
	 *
	 * @param commit
	 * @param activateOnOpen <code>true</code> if the newly opened editor should be activated
	 * @return opened editor part
	 * @throws PartInitException
	 * @since 2.1
	 */
	public static final IEditorPart open(RepositoryCommit commit, boolean activateOnOpen)
			throws PartInitException {
		CommitEditorInput input = new CommitEditorInput(commit);
		return IDE.openEditor(PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage(), input, ID, activateOnOpen);
	}

	/**
	 * Open commit in editor
	 *
	 * @param commit
	 * @param activateOnOpen <code>true</code> if the newly opened editor should be activated
	 * @return opened editor part or null if opening fails
	 * @since 2.1
	 */
	public static final IEditorPart openQuiet(RepositoryCommit commit, boolean activateOnOpen) {
		try {
			return open(commit, activateOnOpen);
		} catch (PartInitException e) {
			Activator.logError(e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Open commit in editor
	 *
	 * @param commit
	 * @return opened editor part or null if opening fails
	 */
	public static final IEditorPart openQuiet(RepositoryCommit commit) {
		return openQuiet(commit, true);
	}

	private IContentOutlinePage outlinePage;

	private CommitEditorPage commitPage;

	private DiffEditorPage diffPage;

	private NotesEditorPage notePage;

	private ListenerHandle refListenerHandle;

	private FocusTracker headerFocusTracker = new FocusTracker();

	private IToolBarManager toolbar;

	private IPageChangedListener pageListener;

	/** Ensures that the toolbar buttons in the header are properly updated. */
	private final IPartListener activationListener = new IPartListener() {

		private boolean isActive;

		@Override
		public void partActivated(IWorkbenchPart part) {
			if (part == CommitEditor.this) {
				if (!isActive) {
					isActive = true;
					updateToolbar();
				}
			} else if (isActive) {
				isActive = false;
				updateToolbar();
			}
		}

		@Override
		public void partBroughtToTop(IWorkbenchPart part) {
			// Nothing to do
		}

		@Override
		public void partClosed(IWorkbenchPart part) {
			// Nothing to do
		}

		@Override
		public void partDeactivated(IWorkbenchPart part) {
			// Nothing to do
		}

		@Override
		public void partOpened(IWorkbenchPart part) {
			// Nothing to do
		}

	};

	private static class CommitEditorNestedSite extends MultiPageEditorSite {

		public CommitEditorNestedSite(CommitEditor topLevelEditor,
				IEditorPart nestedEditor) {
			super(topLevelEditor, nestedEditor);
		}

		@Override
		public IEditorActionBarContributor getActionBarContributor() {
			IEditorActionBarContributor globalContributor = getMultiPageEditor()
					.getEditorSite().getActionBarContributor();
			if (globalContributor instanceof CommitEditorActionBarContributor) {
				return ((CommitEditorActionBarContributor) globalContributor)
						.getTextEditorActionContributor();
			}
			return super.getActionBarContributor();
		}

	}

	@Override
	protected IEditorSite createSite(IEditorPart editor) {
		return new CommitEditorNestedSite(this, editor);
	}

	/**
	 * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
	 */
	@Override
	protected void addPages() {
		try {
			if (getCommit().isStash()) {
				commitPage = new StashEditorPage(this);
			} else {
				commitPage = new CommitEditorPage(this);
			}
			addPage(commitPage);
			RepositoryCommit commit = getCommit();
			if (commit != null) {
				diffPage = new DiffEditorPage(this);
				addPage(diffPage, new DiffEditorInput(commit));
				if (commit.getNotes().length > 0) {
					notePage = new NotesEditorPage(this);
					addPage(notePage);
				}
			}
		} catch (PartInitException e) {
			Activator.error("Error adding page", e); //$NON-NLS-1$
		}
		refListenerHandle = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().getGlobalListenerList()
				.addRefsChangedListener(this);
		pageListener = event -> {
			IEvaluationService service = PlatformUI.getWorkbench()
					.getService(IEvaluationService.class);
			if (service != null) {
				// Update enablement of "Save As..."
				service.requestEvaluation(ISources.ACTIVE_PART_NAME);
			}
		};
		addPageChangedListener(pageListener);
	}

	private IContributionItem createActionContributionItem(String commandId,
			String title, ImageDescriptor icon) {
		IAction action = new Action(title, icon) {

			@Override
			public void run() {
				CommonUtils.runCommand(commandId,
						new StructuredSelection(getCommit()));
			}
		};
		return new ActionContributionItem(action);
	}

	/**
	 * @see org.eclipse.ui.forms.editor.SharedHeaderFormEditor#createHeaderContents(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	protected void createHeaderContents(IManagedForm headerForm) {
		headerForm.addPart(new FocusManagerFormPart(headerFocusTracker) {

			@Override
			public void setDefaultFocus() {
				headerForm.getForm().getForm().setFocus();
			}
		});
		RepositoryCommit commit = getCommit();
		ScrolledForm form = headerForm.getForm();
		String commitName = commit.getRevCommit().name();
		String title = getFormattedHeaderTitle(commitName);
		HeaderText text = new HeaderText(form.getForm(), title, commitName);
		Control textControl = text.getControl();
		if (textControl != null) {
			headerFocusTracker.addToFocusTracking(textControl);
		}
		form.setToolTipText(commitName);
		getToolkit().decorateFormHeading(form.getForm());

		toolbar = form.getToolBarManager();

		ControlContribution repositoryLabelControl = new ControlContribution(
				"repositoryLabel") { //$NON-NLS-1$
			@Override
			protected Control createControl(Composite parent) {
				FormToolkit toolkit = getHeaderForm().getToolkit();
				String label = getCommit().getRepositoryName();

				ImageHyperlink link = new ImageHyperlink(parent, SWT.NONE);
				// Focus tracking on this link doesn't really work. It's a
				// focusable control inside another focusable control (the
				// toolbar). When focus leaves this control through tabbing
				// or deactivating the editor, the toolbar gets the focus (and
				// possibly loses it right away again). Thus the focus tracker
				// will always see the toolbar as the last focused control.
				// Unfortunately there is no other way to get some text onto
				// the first line of a FormHeading.
				headerFocusTracker.addToFocusTracking(link);
				link.setText(label);
				link.setFont(JFaceResources.getBannerFont());
				link.setForeground(toolkit.getColors().getColor(
						IFormColors.TITLE));
				link.setToolTipText(UIText.CommitEditor_showGitRepo);
				link.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent event) {
						RepositoriesView view;
						try {
							view = (RepositoriesView) PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getActivePage()
									.showView(RepositoriesView.VIEW_ID);
							view.showRepository(getCommit().getRepository());
						} catch (PartInitException e) {
							Activator.handleError(
									UIText.CommitEditor_couldNotShowRepository,
									e, false);
						}
					}
				});

				return link;
			}
		};
		toolbar.add(repositoryLabelControl);
		getSite().getService(IPartService.class)
				.addPartListener(activationListener);
		if (commit.isStash()) {
			toolbar.add(createActionContributionItem(ActionCommands.STASH_APPLY,
					UIText.CommitEditor_toolbarApplyStash,
					UIIcons.STASH_APPLY));
			toolbar.add(createActionContributionItem(ActionCommands.STASH_DROP,
					UIText.CommitEditor_toolbarDeleteStash,
					PlatformUI.getWorkbench().getSharedImages()
							.getImageDescriptor(
									ISharedImages.IMG_TOOL_DELETE)));
		} else {
			toolbar.add(createActionContributionItem(ActionCommands.TAG_ACTION,
					UIText.CommitEditor_toolbarCreateTag, UIIcons.CREATE_TAG));
			toolbar.add(
					createActionContributionItem(ActionCommands.BRANCH_CREATE,
					UIText.CommitEditor_toolbarCreateBranch,
					UIIcons.CREATE_BRANCH));
			toolbar.add(createActionContributionItem(ActionCommands.CHECK_OUT,
					UIText.CommitEditor_toolbarCheckOut, UIIcons.CHECKOUT));
			toolbar.add(createActionContributionItem(ActionCommands.CHERRY_PICK,
					UIText.CommitEditor_toolbarCherryPick,
					UIIcons.CHERRY_PICK));
			toolbar.add(createActionContributionItem(ActionCommands.REVERT,
					UIText.CommitEditor_toolbarRevert, UIIcons.REVERT));
			toolbar.add(createActionContributionItem(
					ActionCommands.SHOW_IN_HISTORY,
					UIText.CommitEditor_toolbarShowInHistory, UIIcons.HISTORY));
		}
		addContributions(toolbar);
		toolbar.update(true);
		getSite().setSelectionProvider(new ISelectionProvider() {

			@Override
			public void setSelection(ISelection selection) {
				// Ignored
			}

			@Override
			public void removeSelectionChangedListener(
					ISelectionChangedListener listener) {
				// Ignored
			}

			@Override
			public ISelection getSelection() {
				return new StructuredSelection(getCommit());
			}

			@Override
			public void addSelectionChangedListener(
					ISelectionChangedListener listener) {
				// Ignored
			}
		});
		if (toolbar instanceof ToolBarManager) {
			Control control = ((ToolBarManager) toolbar).getControl();
			if (control != null) {
				headerFocusTracker.addToFocusTracking(control);
			}
		}
	}

	private void updateToolbar() {
		if (toolbar != null) {
			// isEnabled() on a CommandContributionItem actually re-evaluates
			// the enablement.
			for (IContributionItem item : toolbar.getItems()) {
				item.isEnabled();
			}
			toolbar.update(true);
		}
	}

	private String getFormattedHeaderTitle(String commitName) {
		if (getCommit().isStash()) {
			int stashIndex = getStashIndex(getCommit().getRepository(),
					getCommit().getRevCommit().getId());
			String stashName = MessageFormat.format("stash@'{'{0}'}'", //$NON-NLS-1$
					Integer.valueOf(stashIndex));
			return MessageFormat.format(
					UIText.CommitEditor_TitleHeaderStashedCommit,
					stashName);
		} else {
			return MessageFormat.format(UIText.CommitEditor_TitleHeaderCommit,
					commitName);
		}
	}

	private int getStashIndex(Repository repo, ObjectId id) {
		int index = 0;
		try {
			for (RevCommit commit : Git.wrap(repo).stashList().call())
				if (commit.getId().equals(id))
					return index;
				else
					index++;
			throw new IllegalStateException(
					UIText.CommitEditor_couldNotFindStashCommit);
		} catch (Exception e) {
			String message = MessageFormat.format(
					UIText.CommitEditor_couldNotGetStashIndex, id.name());
			Activator.logError(message, e);
			index = -1;
		}
		return index;
	}

	private void addContributions(IToolBarManager toolBarManager) {
		IMenuService menuService = getSite().getService(IMenuService.class);
		if (menuService != null
				&& toolBarManager instanceof ContributionManager) {
			ContributionManager contributionManager = (ContributionManager) toolBarManager;
			String toolbarUri = "toolbar:" + TOOLBAR_HEADER_ID; //$NON-NLS-1$
			menuService.populateContributionManager(contributionManager,
					toolbarUri);
		}
	}

	private RepositoryCommit getCommit() {
		return getAdapter(RepositoryCommit.class);
	}

	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#getAdapter(java.lang.Class)
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (RepositoryCommit.class == adapter) {
			return Adapters.adapt(getEditorInput(), adapter);
		} else if (IContentOutlinePage.class == adapter) {
			return adapter.cast(getOutlinePage());
		}
		return super.getAdapter(adapter);
	}

	/**
	 * @see org.eclipse.ui.forms.editor.FormEditor#init(org.eclipse.ui.IEditorSite,
	 *      org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (Adapters.adapt(input, RepositoryCommit.class) == null)
			throw new PartInitException(
					"Input could not be adapted to commit object"); //$NON-NLS-1$
		super.init(site, input);
		setPartName(input.getName());
		setTitleToolTip(input.getToolTipText());
	}

	@Override
	public void dispose() {
		if (pageListener != null) {
			removePageChangedListener(pageListener);
			pageListener = null;
		}
		getSite().getService(IPartService.class)
				.removePartListener(activationListener);
		refListenerHandle.remove();
		headerFocusTracker.dispose();
		super.dispose();
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		// Save not supported
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		IEditorPart editor = getActiveEditor();
		if (editor != null && editor.isSaveAsAllowed()) {
			editor.doSaveAs();
		}
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		IEditorPart editor = getActiveEditor();
		return editor != null && editor.isSaveAsAllowed();
	}

	@Override
	public void onRefsChanged(RefsChangedEvent event) {
		if (getCommit().getRepository().getDirectory()
				.equals(event.getRepository().getDirectory())) {
			UIJob job = new UIJob("Refreshing editor") { //$NON-NLS-1$

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					if (!getContainer().isDisposed())
						commitPage.refresh();
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}

	private IContentOutlinePage getOutlinePage() {
		if (outlinePage == null) {
			outlinePage = new MultiPageEditorContentOutlinePage(this);
		}
		return outlinePage;
	}

	@Override
	public ShowInContext getShowInContext() {
		IFormPage currentPage = getActivePageInstance();
		IShowInSource showInSource = Adapters.adapt(currentPage,
				IShowInSource.class);
		if (showInSource != null) {
			return showInSource.getShowInContext();
		}
		return null;
	}

	@Override
	public String[] getShowInTargetIds() {
		IFormPage currentPage = getActivePageInstance();
		IShowInTargetList targetList = Adapters.adapt(currentPage,
				IShowInTargetList.class);
		if (targetList != null) {
			return targetList.getShowInTargetIds();
		}
		return null;
	}

	@Override
	public void setFocus() {
		// super class sets focus to form header
		IFormPage currentPage = getActivePageInstance();
		if (currentPage != null) {
			currentPage.setFocus();
		}
	}
}
