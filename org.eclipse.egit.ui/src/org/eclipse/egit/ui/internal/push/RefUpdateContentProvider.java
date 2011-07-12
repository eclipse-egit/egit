/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * Content provided for push result table viewer.
 * <p>
 * Input of this provided must be {@link PushOperationResult} instance, while
 * returned elements are instances of {@link RefUpdateElement}. Null input is
 * allowed, resulting in no elements.
 *
 * @see PushOperationResult
 * @see RefUpdateElement
 */
class RefUpdateContentProvider extends WorkbenchContentProvider implements
		IStructuredContentProvider {

	public Object[] getElements(final Object element) {
		return element instanceof Object[] ? (Object[]) element : new Object[0];
	}

	public Object[] getChildren(Object element) {
		if (element instanceof RepositoryCommit)
			return ((RepositoryCommit) element).getDiffs();
		return super.getChildren(element);
	}
}
