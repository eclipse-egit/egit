/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza <dariusz@luksza.org> - add getRef() and getShell methods
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Abstract super class for commands shared between different components in EGit
 */
public abstract class AbstractSharedCommandHandler extends AbstractHandler {

	private static final IWorkbench WORKBENCH = PlatformUI.getWorkbench();

	/**
	 * @param event
	 *            the {@link ExecutionEvent}
	 * @return a {@link Repository} if all elements in the current selection map
	 *         to the same {@link Repository}, otherwise null
	 */
	protected Repository getRepository(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection.isEmpty())
			return null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			Repository result = null;
			for (Object element : ssel.toList()) {
				Repository elementRepository = null;
				if (element instanceof RepositoryTreeNode) {
					elementRepository = ((RepositoryTreeNode) element)
							.getRepository();
				} else if (element instanceof IResource) {
					IResource resource = (IResource) element;
					RepositoryMapping mapping = RepositoryMapping
							.getMapping(resource.getProject());
					if (mapping != null)
						elementRepository = mapping.getRepository();
				} else if (element instanceof IAdaptable) {
					IResource adapted = (IResource) ((IAdaptable) element)
							.getAdapter(IResource.class);
					if (adapted != null) {
						RepositoryMapping mapping = RepositoryMapping
								.getMapping(adapted.getProject());
						if (mapping != null)
							elementRepository = mapping.getRepository();
					}
				}
				if (elementRepository == null)
					continue;
				if (result != null && !elementRepository.equals(result))
					return null;
				if (result == null)
					result = elementRepository;
			}
			return result;
		}
		if (selection instanceof TextSelection) {
			IEditorInput activeEditor = WORKBENCH.getActiveWorkbenchWindow()
					.getActivePage().getActiveEditor().getEditorInput();
			IResource resource = (IResource) activeEditor
					.getAdapter(IResource.class);

			if (resource != null)
				return RepositoryMapping.getMapping(resource).getRepository();
		}
		return null;
	}

	/**
	 *
	 * @param selected
	 * @return {@link Ref} connected with given {@code selected} node or
	 *         {@code null} when ref cannot be determined
	 */
	protected Ref getRef(Object selected) {
		if (selected instanceof RepositoryTreeNode<?>) {
			RepositoryTreeNode node = (RepositoryTreeNode) selected;
			if (node.getType() == RepositoryTreeNodeType.REF)
				return ((RefNode) node).getObject();
		}

		return null;
	}

	/**
	 *
	 * @param event
	 * @return {@link Shell} connected with given {@code event}, or {@code null}
	 *         when shell cannot be determined
	 */
	protected Shell getShell(ExecutionEvent event) {
		return HandlerUtil.getActiveShell(event);
	}

}
