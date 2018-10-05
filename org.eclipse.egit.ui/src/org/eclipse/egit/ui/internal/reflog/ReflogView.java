/*******************************************************************************
 * Copyright (c) 2011, 2017 Chris Aniszczyk <caniszczyk@gmail.com> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Chris Aniszczyk <caniszczyk@gmail.com> - initial implementation
 *   EclipseSource - Filtered Viewer
 *   Robin Stocker <robin@nibor.org> - Show In support
 *   Tobias Baumann <tobbaumann@gmail.com> - Bug 475836
 *   Thomas Wolf <thomas.wolf@paranor.ch> - Bug 477248, 518607
 *******************************************************************************/
package org.eclipse.egit.ui.internal.reflog;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.egit.ui.internal.TreeColumnPatternFilter;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.actions.ResetMenu;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.components.RepositoryMenuUtil.RepositoryToolbarAction;
import org.eclipse.egit.ui.internal.reflog.ReflogViewContentProvider.ReflogInput;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.selection.RepositorySelectionProvider;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;

/**
 * A view that shows reflog entries. The View includes a quick filter that
 * searches on both the commit hashes and commit messages.
 */
public class ReflogView extends ViewPart implements RefsChangedListener, IShowInTarget {

	/**
	 * View id
	 */
	public static final String VIEW_ID = "org.eclipse.egit.ui.ReflogView"; //$NON-NLS-1$

	/**
	 * Context menu id
	 */
	public static final String POPUP_MENU_ID = "org.eclipse.egit.ui.internal.reflogview.popup";//$NON-NLS-1$

	private FormToolkit toolkit;

	private Form form;

	private TreeViewer refLogTableTreeViewer;

	private ISelectionListener selectionChangedListener;

	private ListenerHandle addRefsChangedListener;

	private IPropertyChangeListener uiPrefsListener;

	private PreferenceBasedDateFormatter dateFormatter;

	private IWorkbenchAction switchRepositoriesAction;

	@SuppressWarnings("unused")
	@Override
	public void createPartControl(Composite parent) {
		dateFormatter = PreferenceBasedDateFormatter.create();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});

		form = toolkit.createForm(parent);

		Image repoImage = UIIcons.REPOSITORY.createImage();
		UIUtils.hookDisposal(form, repoImage);
		Image commitImage = UIIcons.CHANGESET.createImage();
		UIUtils.hookDisposal(form, commitImage);
		form.setImage(repoImage);
		form.setText(UIText.StagingView_NoSelectionTitle);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);
		toolkit.decorateFormHeading(form);
		GridLayoutFactory.fillDefaults().applyTo(form.getBody());

		Composite tableComposite = toolkit.createComposite(form.getBody());
		tableComposite.setLayout(new GridLayout());

		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableComposite);

		final TreeColumnLayout layout = new TreeColumnLayout();

		FilteredTree filteredTree = new FilteredTree(tableComposite, SWT.NONE
				| SWT.BORDER | SWT.FULL_SELECTION,
				new TreeColumnPatternFilter(), true) {
			@Override
			protected void createControl(Composite composite, int treeStyle) {
				super.createControl(composite, treeStyle);
				treeComposite.setLayout(layout);
			}
		};

		toolkit.adapt(filteredTree);
		refLogTableTreeViewer = filteredTree.getViewer();
		refLogTableTreeViewer.getTree().setLinesVisible(true);
		refLogTableTreeViewer.getTree().setHeaderVisible(true);
		refLogTableTreeViewer
				.setContentProvider(new ReflogViewContentProvider());

		ColumnViewerToolTipSupport.enableFor(refLogTableTreeViewer);

		TreeViewerColumn toColumn = createColumn(layout,
				UIText.ReflogView_CommitColumnHeader, 10, SWT.LEFT);
		toColumn.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof ReflogEntry) {
					final ReflogEntry entry = (ReflogEntry) element;
					return entry.getNewId().abbreviate(7).name();
				}
				return null;
			}

			@Override
			public String getToolTipText(Object element) {
				if (element instanceof ReflogEntry) {
					final ReflogEntry entry = (ReflogEntry) element;
					return entry.getNewId().name();
				}
				return null;
			}

			@Override
			public Image getImage(Object element) {
				if (element instanceof ReflogEntry) {
					return commitImage;
				}
				return null;
			}

		});

		TreeViewerColumn commitMessageColumn = createColumn(layout,
				UIText.ReflogView_CommitMessageColumnHeader, 40, SWT.LEFT);
		commitMessageColumn.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof ReflogItem) {
					ReflogItem entry = (ReflogItem) element;
					String c = entry.getCommitMessage();
					return c == null ? "" : c; //$NON-NLS-1$
				} else if (element instanceof IWorkbenchAdapter) {
					return ((IWorkbenchAdapter) element).getLabel(element);
				}
				return null;
			}
		});

		TreeViewerColumn dateColumn = createColumn(layout,
				UIText.ReflogView_DateColumnHeader, 15, SWT.LEFT);
		dateColumn.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof ReflogEntry) {
					final ReflogEntry entry = (ReflogEntry) element;
					final PersonIdent who = entry.getWho();
					return dateFormatter.formatDate(who);
				}
				return null;
			}

		});

		TreeViewerColumn messageColumn = createColumn(layout,
				UIText.ReflogView_MessageColumnHeader, 40, SWT.LEFT);
		messageColumn.setLabelProvider(new ColumnLabelProvider() {

			private ResourceManager resourceManager = new LocalResourceManager(
					JFaceResources.getResources());

			@Override
			public String getText(Object element) {
				if (element instanceof ReflogEntry) {
					final ReflogEntry entry = (ReflogEntry) element;
					return entry.getComment();
				}
				return null;
			}

			@Override
			public Image getImage(Object element) {
				if (!(element instanceof ReflogEntry)) {
					return null;
				}
				String comment = ((ReflogEntry) element).getComment();
				if (comment.startsWith("commit:") || comment.startsWith("commit (initial):")) //$NON-NLS-1$ //$NON-NLS-2$
					return (Image) resourceManager.get(UIIcons.COMMIT);
				if (comment.startsWith("commit (amend):")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.AMEND_COMMIT);
				if (comment.startsWith("pull")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.PULL);
				if (comment.startsWith("clone")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.CLONEGIT);
				if (comment.startsWith("rebase")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.REBASE);
				if (comment.startsWith("merge")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.MERGE);
				if (comment.startsWith("fetch")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.FETCH);
				if (comment.startsWith("branch")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.CREATE_BRANCH);
				if (comment.startsWith("checkout")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.CHECKOUT);
				if (comment.startsWith("cherry-pick")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.CHERRY_PICK);
				if (comment.startsWith("Branch: renamed ")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.EDITCONFIG);
				if (comment.startsWith("reset")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.RESET);
				return null;
			}

			@Override
			public void dispose() {
				resourceManager.dispose();
				super.dispose();
			}
		});

		new OpenAndLinkWithEditorHelper(refLogTableTreeViewer) {
			@Override
			protected void linkToEditor(ISelection selection) {
				// Not supported

			}
			@Override
			protected void open(ISelection sel, boolean activate) {
				handleOpen(sel, OpenStrategy.activateOnOpen());
			}
			@Override
			protected void activate(ISelection selection) {
				handleOpen(selection, true);
			}
			private void handleOpen(ISelection selection, boolean activateOnOpen) {
				if (selection instanceof IStructuredSelection)
					if (selection.isEmpty())
						return;
				Repository repo = getRepository();
				if (repo == null)
					return;
				try (RevWalk walk = new RevWalk(repo)) {
					for (Object element : ((IStructuredSelection)selection).toArray()) {
						ReflogEntry entry = (ReflogEntry) element;
						ObjectId id = entry.getNewId();
						if (id == null || id.equals(ObjectId.zeroId()))
							id = entry.getOldId();
						if (id != null && !id.equals(ObjectId.zeroId()))
							CommitEditor.openQuiet(new RepositoryCommit(repo,
									walk.parseCommit(id)), activateOnOpen);
					}
				} catch (IOException e) {
					Activator.logError(UIText.ReflogView_ErrorOnOpenCommit, e);
				}
			}
		};

		uiPrefsListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				if (UIPreferences.DATE_FORMAT.equals(property)
						|| UIPreferences.DATE_FORMAT_CHOICE.equals(property)) {
					dateFormatter = PreferenceBasedDateFormatter.create();
					refLogTableTreeViewer.refresh();
				}
			}
		};
		Activator.getDefault().getPreferenceStore()
				.addPropertyChangeListener(uiPrefsListener);
		selectionChangedListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				if (part instanceof IEditorPart) {
					IEditorInput input = ((IEditorPart) part).getEditorInput();
					if (input instanceof IFileEditorInput)
						reactOnSelection(new StructuredSelection(
								((IFileEditorInput) input).getFile()));
				} else
					reactOnSelection(selection);
			}
		};

		IWorkbenchPartSite site = getSite();
		ISelectionService service = CommonUtils.getService(site, ISelectionService.class);
		service.addPostSelectionListener(selectionChangedListener);

		// Use current selection to populate reflog view
		UIUtils.notifySelectionChangedWithCurrentSelection(
				selectionChangedListener, site);

		site.setSelectionProvider(new RepositorySelectionProvider(
				refLogTableTreeViewer, () -> getRepository()));

		addRefsChangedListener = Repository.getGlobalListenerList()
				.addRefsChangedListener(this);

		// Toolbar
		IToolBarManager toolbar = getViewSite().getActionBars()
				.getToolBarManager();
		switchRepositoriesAction = new RepositoryToolbarAction(false,
				() -> getRepository(),
				repo -> reactOnSelection(new StructuredSelection(repo)));
		toolbar.add(switchRepositoriesAction);
		getViewSite().getActionBars().updateActionBars();
		// register context menu
		MenuManager menuManager = new MenuManager();
		menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		Tree tree = refLogTableTreeViewer.getTree();
		tree.setMenu(menuManager.createContextMenu(tree));

		MenuManager resetManager = ResetMenu.createMenu(getSite());
		menuManager.add(resetManager);

		getSite().registerContextMenu(POPUP_MENU_ID, menuManager, refLogTableTreeViewer);
	}

	@Override
	public void setFocus() {
		refLogTableTreeViewer.getControl().setFocus();
		activateContextService();
	}

	private void activateContextService() {
		IContextService contextService = CommonUtils.getService(getSite(), IContextService.class);
		if (contextService != null)
			contextService.activateContext(VIEW_ID);

	}

	@Override
	public void dispose() {
		super.dispose();
		ISelectionService service = CommonUtils.getService(getSite(), ISelectionService.class);
		service.removePostSelectionListener(selectionChangedListener);
		if (addRefsChangedListener != null) {
			addRefsChangedListener.remove();
		}
		Activator.getDefault().getPreferenceStore()
				.removePropertyChangeListener(uiPrefsListener);
		if (switchRepositoriesAction != null) {
			switchRepositoriesAction.dispose();
			switchRepositoriesAction = null;
		}
	}

	private void reactOnSelection(ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() != 1) {
			return;
		}
		Repository selectedRepo = null;
		Object first = ssel.getFirstElement();
		IResource adapted = AdapterUtils.adaptToAnyResource(first);
		if (adapted != null) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(adapted);
			if (mapping != null) {
				selectedRepo = mapping.getRepository();
			}
		}
		if (selectedRepo == null) {
			selectedRepo = AdapterUtils.adapt(first, Repository.class);
		}
		if (selectedRepo == null) {
			return;
		}

		// Only update when different repository is selected
		Repository currentRepo = getRepository();
		if (currentRepo == null
				|| !selectedRepo.getDirectory().equals(
						currentRepo.getDirectory())) {
			showReflogFor(selectedRepo);
		}
	}

	private void updateRefLink(final String name) {
		IToolBarManager toolbar = form.getToolBarManager();
		toolbar.removeAll();

		ControlContribution refLabelControl = new ControlContribution(
				"refLabel") { //$NON-NLS-1$
			@Override
			protected Control createControl(Composite cParent) {
				Composite composite = toolkit.createComposite(cParent);
				composite.setLayout(new RowLayout());
				composite.setBackground(null);

				final ImageHyperlink refLink = new ImageHyperlink(composite,
						SWT.NONE);
				Image image = UIIcons.BRANCH.createImage();
				UIUtils.hookDisposal(refLink, image);
				refLink.setImage(image);
				refLink.setFont(JFaceResources.getBannerFont());
				refLink.setForeground(toolkit.getColors().getColor(
						IFormColors.TITLE));
				refLink.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent event) {
						Repository repository = getRepository();
						if (repository == null)
							return;
						RefSelectionDialog dialog = new RefSelectionDialog(
								refLink.getShell(), repository);
						if (Window.OK == dialog.open())
							showReflogFor(repository, dialog.getRefName());
					}
				});
				refLink.setText(Repository.shortenRefName(name));

				return composite;
			}
		};
		toolbar.add(refLabelControl);
		toolbar.update(true);
	}

	/**
	 * @return the repository the view is showing the reflog for
	 */
	public Repository getRepository() {
		Object input = refLogTableTreeViewer.getInput();
		if (input instanceof ReflogInput)
			return ((ReflogInput) input).getRepository();
		return null;
	}

	@Override
	public boolean show(ShowInContext context) {
		ISelection selection = context.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			for (Object element : structuredSelection.toList()) {
				if (element instanceof RepositoryTreeNode) {
					RepositoryTreeNode node = (RepositoryTreeNode) element;
					showReflogFor(node.getRepository());
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Defines the repository for the reflog to show.
	 *
	 * @param repository
	 */
	private void showReflogFor(Repository repository) {
		showReflogFor(repository, Constants.HEAD);
	}

	/**
	 * Defines the repository for the reflog to show.
	 *
	 * @param repository
	 * @param ref
	 */
	private void showReflogFor(Repository repository, String ref) {
		if (repository != null && ref != null) {
			refLogTableTreeViewer.setInput(new ReflogInput(repository, ref));
			updateRefLink(ref);
			form.setText(getRepositoryName(repository));
		}
	}

	private TreeViewerColumn createColumn(
			final TreeColumnLayout columnLayout, final String text,
			final int weight, final int style) {
		final TreeViewerColumn viewerColumn = new TreeViewerColumn(
				refLogTableTreeViewer, style);
		final TreeColumn column = viewerColumn.getColumn();
		column.setText(text);
		columnLayout.setColumnData(column, new ColumnWeightData(weight, 10));
		return viewerColumn;
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

	@Override
	public void onRefsChanged(RefsChangedEvent event) {
		PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
			Object currentInput = refLogTableTreeViewer.getInput();
			if (currentInput instanceof ReflogInput) {
				ReflogInput oldInput = (ReflogInput) currentInput;
				refLogTableTreeViewer.setInput(new ReflogInput(
						oldInput.getRepository(), oldInput.getRef()));
			}
		});
	}
}
