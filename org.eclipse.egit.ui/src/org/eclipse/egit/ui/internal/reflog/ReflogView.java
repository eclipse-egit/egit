/*******************************************************************************
 * Copyright (c) 2011, Chris Aniszczyk <caniszczyk@gmail.com> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Chris Aniszczyk <caniszczyk@gmail.com> - initial implementation
 *   EclipseSource - Filtered Viewer
 *******************************************************************************/
package org.eclipse.egit.ui.internal.reflog;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.reflog.ReflogViewContentProvider.ReflogInput;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.ReflogEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.part.ViewPart;

/**
 * A view that shows reflog entries. The View includes a quick filter that
 * searches on both the commit hashes and commit messages.
 */
public class ReflogView extends ViewPart implements RefsChangedListener {

	/**
	 * View id
	 */
	public static final String VIEW_ID = "org.eclipse.egit.ui.ReflogView"; //$NON-NLS-1$

	private FormToolkit toolkit;

	private Form form;

	private ImageHyperlink refLink;

	private TreeViewer refLogTableTreeViewer;

	private ISelectionListener selectionChangedListener;

	private ListenerHandle addRefsChangedListener;

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});

		form = toolkit.createForm(parent);

		Image repoImage = UIIcons.REPOSITORY.createImage();
		UIUtils.hookDisposal(form, repoImage);
		final Image branchImage = UIIcons.CHANGESET.createImage();
		UIUtils.hookDisposal(form, branchImage);
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
				| SWT.BORDER, new PatternFilter(), true) {
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

		TreeViewerColumn fromColum = createColumn(layout, "From", 10, SWT.LEFT); //$NON-NLS-1$
		fromColum.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				final ReflogEntry entry = (ReflogEntry) element;
				return entry.getOldId().abbreviate(6).name();
			}

			@Override
			public String getToolTipText(Object element) {
				final ReflogEntry entry = (ReflogEntry) element;
				return entry.getOldId().name();
			}

			@Override
			public Image getImage(Object element) {
				return branchImage;
			}
		});

		TreeViewerColumn toColumn = createColumn(layout, "To", 10, SWT.LEFT); //$NON-NLS-1$
		toColumn.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				final ReflogEntry entry = (ReflogEntry) element;
				return entry.getNewId().abbreviate(6).name();
			}

			@Override
			public String getToolTipText(Object element) {
				final ReflogEntry entry = (ReflogEntry) element;
				return entry.getNewId().name();
			}

			@Override
			public Image getImage(Object element) {
				return branchImage;
			}

		});
		TreeViewerColumn messageColumn = createColumn(layout,
				"Message", 50, SWT.LEFT); //$NON-NLS-1$
		messageColumn.setLabelProvider(new ColumnLabelProvider() {

			private ResourceManager resourceManager = new LocalResourceManager(
					JFaceResources.getResources());

			@Override
			public String getText(Object element) {
				final ReflogEntry entry = (ReflogEntry) element;
				return entry.getComment();
			}

			public Image getImage(Object element) {
				String comment = ((ReflogEntry) element).getComment();
				if (comment.startsWith("commit:")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.COMMIT);
				if (comment.startsWith("commit (amend):")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.AMEND_COMMIT);
				if (comment.startsWith("pull :")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.PULL);
				if (comment.startsWith("clone:")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.CLONEGIT);
				if (comment.startsWith("rebase finished:")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.REBASE);
				if (comment.startsWith("merge ")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.MERGE);
				if (comment.startsWith("fetch: ")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.FETCH);
				if (comment.startsWith("branch: ")) //$NON-NLS-1$
					return (Image) resourceManager.get(UIIcons.CREATE_BRANCH);
				return null;
			}

			public void dispose() {
				resourceManager.dispose();
				super.dispose();
			}
		});
		refLogTableTreeViewer.addOpenListener(new IOpenListener() {

			public void open(OpenEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (selection.isEmpty())
					return;
				Repository repo = getRepository();
				if (repo == null)
					return;
				RevWalk walk = new RevWalk(repo);
				try {
					for (Object element : selection.toArray()) {
						ReflogEntry entry = (ReflogEntry) element;
						ObjectId id = entry.getNewId();
						if (id == null || id.equals(ObjectId.zeroId()))
							id = entry.getOldId();
						if (id != null && !id.equals(ObjectId.zeroId()))
							CommitEditor.openQuiet(new RepositoryCommit(repo,
									walk.parseCommit(id)));
					}
				} catch (IOException e) {
					Activator.logError("Error opening commit", e); //$NON-NLS-1$
				} finally {
					walk.release();
				}

			}
		});

		selectionChangedListener = new ISelectionListener() {
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
		ISelectionService service = (ISelectionService) site
				.getService(ISelectionService.class);
		service.addPostSelectionListener(selectionChangedListener);

		// Use current selection to populate reflog view
		ISelection selection = service.getSelection();
		if (selection != null && !selection.isEmpty()) {
			IWorkbenchPart part = site.getPage().getActivePart();
			if (part != null)
				selectionChangedListener.selectionChanged(part, selection);
		}

		site.setSelectionProvider(refLogTableTreeViewer);

		addRefsChangedListener = Repository.getGlobalListenerList()
				.addRefsChangedListener(this);
	}

	@Override
	public void setFocus() {
		refLogTableTreeViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		ISelectionService service = (ISelectionService) getSite().getService(
				ISelectionService.class);
		service.removePostSelectionListener(selectionChangedListener);
		if (addRefsChangedListener != null)
			addRefsChangedListener.remove();
	}

	private void reactOnSelection(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			StructuredSelection ssel = (StructuredSelection) selection;
			if (ssel.size() != 1)
				return;
			Repository selectedRepo = null;
			if (ssel.getFirstElement() instanceof IResource) {
				IResource resource = (IResource) ssel.getFirstElement();
				RepositoryMapping mapping = RepositoryMapping
						.getMapping(resource.getProject());
				if (mapping != null)
					selectedRepo = mapping.getRepository();
			}
			if (ssel.getFirstElement() instanceof IAdaptable) {
				IResource adapted = (IResource) ((IAdaptable) ssel
						.getFirstElement()).getAdapter(IResource.class);
				if (adapted != null) {
					RepositoryMapping mapping = RepositoryMapping
							.getMapping(adapted);
					if (mapping != null)
						selectedRepo = mapping.getRepository();
				}
			} else if (ssel.getFirstElement() instanceof RepositoryTreeNode) {
				RepositoryTreeNode repoNode = (RepositoryTreeNode) ssel
						.getFirstElement();
				selectedRepo = repoNode.getRepository();
			}

			showReflogFor(selectedRepo);
		}
	}

	private void updateRefLink(final String name) {
		IToolBarManager toolbar = form.getToolBarManager();
		toolbar.removeAll();

		ControlContribution repositoryLabelControl = new ControlContribution(
				"refLabel") { //$NON-NLS-1$
			@Override
			protected Control createControl(Composite cParent) {
				Composite composite = toolkit.createComposite(cParent);
				composite.setLayout(new RowLayout());
				composite.setBackground(null);

				refLink = new ImageHyperlink(composite, SWT.NONE);
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
		toolbar.add(repositoryLabelControl);
		toolbar.update(true);
	}

	private Repository getRepository() {
		Object input = refLogTableTreeViewer.getInput();
		if (input instanceof ReflogInput)
			return ((ReflogInput) input).getRepository();
		return null;
	}

	/**
	 * Defines the repository for the reflog to show.
	 *
	 * @param repository
	 */
	public void showReflogFor(Repository repository) {
		showReflogFor(repository, Constants.R_HEADS + Constants.MASTER);
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

	public void onRefsChanged(RefsChangedEvent event) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				refLogTableTreeViewer.refresh();
			}
		});
	}
}
