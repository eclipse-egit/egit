/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;

class NotifiableDiffNode extends DiffNode {

	NotifiableDiffNode(IDiffContainer parent, int kind, ITypedElement ancestor,
			ITypedElement left, ITypedElement right) {
		super(parent, kind, ancestor, left, right);
	}

	@Override
	protected void fireChange() {
		super.fireChange();
	}

}
