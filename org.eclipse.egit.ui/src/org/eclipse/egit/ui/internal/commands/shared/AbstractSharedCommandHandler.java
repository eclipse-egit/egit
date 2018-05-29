/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza <dariusz@luksza.org> - add getRef() and getShell methods
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
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
		Object ctx = event.getApplicationContext();
		if (ctx instanceof IEvaluationContext)
			return SelectionUtils.getRepository((IEvaluationContext) ctx);
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
