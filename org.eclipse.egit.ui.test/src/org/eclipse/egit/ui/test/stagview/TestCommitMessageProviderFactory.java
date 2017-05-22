package org.eclipse.egit.ui.test.stagview;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.egit.ui.ICommitMessageProvider;

class TestCommitMessageProviderFactory
		implements IExecutableExtensionFactory {

	private ICommitMessageProvider currentProvider;

	private final ICommitMessageProvider emptyProvider = new ICommitMessageProvider() {

		@Override
		public String getMessage(IResource[] resources) {
			return "";
		}
	};

	public void setCommitMessageProvider(ICommitMessageProvider provider) {
		currentProvider = provider;
	}

	public ICommitMessageProvider getProvider() {
		return currentProvider;
	}

	@Override
	public Object create() throws CoreException {
		return currentProvider != null ? currentProvider : emptyProvider;
	}

}