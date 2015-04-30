package org.eclipse.egit.gitflow.ui.internal.selection;

import static org.eclipse.egit.gitflow.ui.Activator.error;

import java.io.IOException;

import org.eclipse.egit.gitflow.Activator;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.lib.Repository;

/**
 * Testing Git Flow states.
 */
public class SelectionPropertyTester extends org.eclipse.core.expressions.PropertyTester {
	private static final String IS_MASTER = "isMaster"; //$NON-NLS-1$

	private static final String IS_DEVELOP = "isDevelop"; //$NON-NLS-1$

	private static final String IS_HOTFIX = "isHotfix"; //$NON-NLS-1$

	private static final String IS_RELEASE = "isRelease"; //$NON-NLS-1$

	private static final String IS_INITIALIZED = "isInitialized"; //$NON-NLS-1$

	private static final String IS_FEATURE = "isFeature"; //$NON-NLS-1$

	private static final String HAS_DEFAULT_REMOTE = "hasDefaultRemote"; //$NON-NLS-1$

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		Repository repository = (Repository) receiver;

		GitFlowRepository gitFlowRepository = new GitFlowRepository(repository);
		try {
			if (IS_INITIALIZED.equals(property)) {
				return gitFlowRepository.isInitialized();
			} else if (IS_FEATURE.equals(property)) {
				return gitFlowRepository.isFeature();
			} else if (IS_RELEASE.equals(property)) {
				return gitFlowRepository.isRelease();
			} else if (IS_HOTFIX.equals(property)) {
				return gitFlowRepository.isHotfix();
			} else if (IS_DEVELOP.equals(property)) {
				return gitFlowRepository.isDevelop();
			} else if (IS_MASTER.equals(property)) {
				return gitFlowRepository.isMaster();
			} else if (HAS_DEFAULT_REMOTE.equals(property)) {
				return gitFlowRepository.hasDefaultRemote();
			}
		} catch (IOException e) {
			Activator.getDefault().getLog().log(error(e.getMessage(), e));
		}
		return false;
	}
}
