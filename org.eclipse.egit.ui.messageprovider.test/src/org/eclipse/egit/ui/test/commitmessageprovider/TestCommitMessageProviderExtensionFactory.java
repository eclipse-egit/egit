package org.eclipse.egit.ui.test.commitmessageprovider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;

public class TestCommitMessageProviderExtensionFactory implements IExecutableExtensionFactory {

	@Override
	public Object create() throws CoreException {
		return CommitMessageProviderFactory.getCommitMessageProvider();
	}

}
