/*******************************************************************************
 * Copyright (C) 2017 Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test.stagview;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;

public class TestCommitMessageProviderExtensionFactory
		implements IExecutableExtensionFactory {

	// Indirection needed since the extension point may create new factory
	// instances.
	protected static final TestCommitMessageProviderFactory INSTANCE = new TestCommitMessageProviderFactory();

	@Override
	public Object create() throws CoreException {
		return INSTANCE.create();
	}

}
