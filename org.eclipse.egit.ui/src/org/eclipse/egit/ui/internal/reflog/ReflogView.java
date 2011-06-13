/*******************************************************************************
 * Copyright (c) 2011, Chris Aniszczyk <caniszczyk@gmail.com> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Chris Aniszczyk <caniszczyk@gmail.com> - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.reflog;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

/**
 * A view that shows reflog entries
 */
public class ReflogView extends ViewPart {

	/**
	 * View id
	 */
	public static final String VIEW_ID = "org.eclipse.egit.ui.ReflogView"; //$NON-NLS-1$

	private Form form;

	private TableViewer reflogTableViewer;

	private ISelectionListener selectionChangedListener;

	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});

		form = toolkit.createForm(parent);

		Image repoImage = UIIcons.REPOSITORY.createImage();
		UIUtils.hookDisposal(form, repoImage);
		form.setImage(repoImage);
		form.setText(UIText.StagingView_NoSelectionTitle);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);
		toolkit.decorateFormHeading(form);
		GridLayoutFactory.swtDefaults().applyTo(form.getBody());

		Composite composite = toolkit
				.createComposite(form.getBody());
		toolkit.paintBordersFor(composite);
		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2)
				.applyTo(composite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);

		reflogTableViewer = new TableViewer(toolkit.createTable(
				composite, SWT.FULL_SELECTION | SWT.MULTI));
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(reflogTableViewer.getControl());
		reflogTableViewer.getTable().setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TREE_BORDER);
		reflogTableViewer.getTable().setLinesVisible(true);
		reflogTableViewer.setLabelProvider(new ReflogLabelProvider());
		reflogTableViewer.setContentProvider(new ReflogViewContentProvider());

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

		ISelectionService service = (ISelectionService) getSite().getService(
				ISelectionService.class);
		service.addPostSelectionListener(selectionChangedListener);

		getSite().setSelectionProvider(reflogTableViewer);
	}

	@Override
	public void setFocus() {
		reflogTableViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		ISelectionService service = (ISelectionService) getSite().getService(
				ISelectionService.class);
		service.removePostSelectionListener(selectionChangedListener);
	}

	private void reactOnSelection(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			StructuredSelection ssel = (StructuredSelection) selection;
			if (ssel.size() != 1)
				return;
			Repository repository = null;
			if (ssel.getFirstElement() instanceof IResource) {
				IResource resource = (IResource) ssel.getFirstElement();
				RepositoryMapping mapping = RepositoryMapping.getMapping(resource.getProject());
				repository = mapping.getRepository();
			}
			if (ssel.getFirstElement() instanceof IAdaptable) {
				IResource adapted = (IResource) ((IAdaptable) ssel
						.getFirstElement()).getAdapter(IResource.class);
				if (adapted != null) {
					RepositoryMapping mapping = RepositoryMapping.getMapping(adapted);
					repository = mapping.getRepository();
				}
			} else if (ssel.getFirstElement() instanceof RepositoryTreeNode) {
				RepositoryTreeNode repoNode = (RepositoryTreeNode) ssel
						.getFirstElement();
				repository = repoNode.getRepository();
			}
			if (repository != null)
				reflogTableViewer.setInput(repository);
		}
	}

}
