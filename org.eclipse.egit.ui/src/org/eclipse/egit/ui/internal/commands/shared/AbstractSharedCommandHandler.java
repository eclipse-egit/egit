/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
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
import org.eclipse.egit.core.internal.project.RepositoryMapping;
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
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Abstract super class for commands shared between different components in EGit
 */
public abstract class AbstractSharedCommandHandler extends AbstractHandler {


	/**
	 * @param event
	 *            the {@link ExecutionEvent}
	 * @return a {@link Repository} if all elements in the current selection map
	 *         to the same {@link Repository}, otherwise null
	 */
	public static Repository getRepository(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		IEditorInput editorInput = getActiveEditorInput(event);
		return getRepository(selection, editorInput);
	}

	/**
	 * Get repository from selection
	 *
	 * @param selection the selection or <code>null</code> if not available
	 * @param editorInput the editor input to be used in case of a text selection or <code>null</code> if not available
	 * @return a {@link Repository} if all elements in the current selection map
	 *         to the same {@link Repository}, otherwise null
	 */
	protected static Repository getRepository(ISelection selection, IEditorInput editorInput) {
		if (selection == null || selection.isEmpty())
			return null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			Repository result = null;
			for (Object element : ssel.toList()) {
				Repository elementRepository = null;
				if (element instanceof RepositoryTreeNode)
					elementRepository = ((RepositoryTreeNode) element)
							.getRepository();
				else if (element instanceof IResource) {
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
		if (selection instanceof TextSelection && editorInput != null) {
			IResource resource = (IResource) editorInput
					.getAdapter(IResource.class);
			if (resource != null) {
				RepositoryMapping mapping = RepositoryMapping
						.getMapping(resource);
				if (mapping != null)
					return mapping.getRepository();
			}
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

	/**
	 * Return the input of the active editor.
	 * <strong>Note:</strong> Copied from org.eclipse.ui.handlers.HandlerUtil.getActiveEditorInput(ExecutionEvent) for compatibility reasons.
	 *
	 * @param event
	 *            The execution event that contains the application context
	 * @return the input of the active editor, or <code>null</code>.
	 */
	private static IEditorInput getActiveEditorInput(ExecutionEvent event) {
		Object o = HandlerUtil.getVariable(event, ISources.ACTIVE_EDITOR_INPUT_NAME);
		if (o instanceof IEditorInput)
			return (IEditorInput) o;
		return null;
	}

}
