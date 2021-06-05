/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.revision;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;

/**
 * A {@link DiffNode} with a public {@link #fireChange()} method.
 */
public class NotifiableDiffNode extends DiffNode {

	/**
	 * Creates a new {@link NotifiableDiffNode} and initializes with the given
	 * values.
	 *
	 * @param parent
	 *            for the new node
	 * @param kind
	 *            of difference as defined in
	 *            {@link org.eclipse.compare.structuremergeviewer.Differencer
	 *            Differencer}
	 * @param ancestor
	 *            the common ancestor input to a compare
	 * @param left
	 *            the left input to a compare
	 * @param right
	 *            the right input to a compare
	 * @see DiffNode#DiffNode(IDiffContainer, int, ITypedElement, ITypedElement,
	 *      ITypedElement)
	 */
	public NotifiableDiffNode(IDiffContainer parent, int kind,
			ITypedElement ancestor, ITypedElement left, ITypedElement right) {
		super(parent, kind, ancestor, left, right);
	}

	/**
	 * Creates a new {@link NotifiableDiffNode} and initializes with the given
	 * values.
	 *
	 * @param kind
	 *            of difference as defined in
	 *            {@link org.eclipse.compare.structuremergeviewer.Differencer
	 *            Differencer}
	 * @param ancestor
	 *            the common ancestor input to a compare
	 * @param left
	 *            the left input to a compare
	 * @param right
	 *            the right input to a compare
	 * @see DiffNode#DiffNode(IDiffContainer, int, ITypedElement, ITypedElement,
	 *      ITypedElement)
	 */
	public NotifiableDiffNode(int kind, ITypedElement ancestor,
			ITypedElement left, ITypedElement right) {
		super(kind, ancestor, left, right);
	}

	@Override
	public void fireChange() {
		super.fireChange();
	}
}
