/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.egit.ui.internal.revision.NotifiableDiffNode;

/**
 * A {@link NotifiableDiffNode} specific to the Egit merge editor.
 */
public class MergeDiffNode extends NotifiableDiffNode {

	/**
	 * Creates a new {@link MergeDiffNode} and initializes with the given
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
	 * @see org.eclipse.compare.structuremergeviewer.DiffNode#DiffNode(IDiffContainer,
	 *      int, ITypedElement, ITypedElement, ITypedElement)
	 */
	public MergeDiffNode(IDiffContainer parent, int kind,
			ITypedElement ancestor, ITypedElement left, ITypedElement right) {
		super(parent, kind, ancestor, left, right);
	}
}
