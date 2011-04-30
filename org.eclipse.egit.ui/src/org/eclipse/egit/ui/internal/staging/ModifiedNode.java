/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.lib.Repository;

class ModifiedNode extends StatusNode {

	ModifiedNode(Repository repository) {
		super(repository);
	}

	@Override
	String getLabel() {
		return UIText.ModifiedNode_Label;
	}

}
