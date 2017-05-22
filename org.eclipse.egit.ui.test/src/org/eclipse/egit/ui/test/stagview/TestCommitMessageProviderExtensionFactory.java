/*******************************************************************************
 * Copyright (C) 2017 Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test.stagview;

import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.egit.ui.ICommitMessageProvider;

public class TestCommitMessageProviderExtensionFactory
		implements IExecutableExtensionFactory {

	// Indirection needed since the extension point may create new factory
	// instances.
	protected static final TestCommitMessageProviderFactory INSTANCE = new TestCommitMessageProviderFactory();

	@Override
	public Object create() throws CoreException {
		return INSTANCE.create();
	}

	static class TestCommitMessageProviderFactory
			implements IExecutableExtensionFactory {

		private Queue<ICommitMessageProvider> providers = new LinkedList<>();

		private final ICommitMessageProvider emptyProvider = new ICommitMessageProvider() {
			@Override
			public String getMessage(IResource[] resources) {
				return "";
			}
		};

		@Override
		public Object create() throws CoreException {
			if (!providers.isEmpty()) {
				ICommitMessageProvider p = providers.poll();
				if (p != null) {
					return p;
				}
			}
			return emptyProvider;
		}

		public void reset() { // To be called in @After
			providers.clear();
		}

		public void setCommitMessageProviders(
				ICommitMessageProvider... newProviders) {
			providers.clear();
			for (ICommitMessageProvider p : newProviders) {
				providers.add(p);
			}
		}

	}

}
