package org.eclipse.egit.ui;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jgit.lib.Repository;

/**
 * Interface that provides access to the internal "Select Repository"
 * wizard page, which can be reused in external wizards building upon EGit.
 */
public interface IGitSelectRepositoryWizardPage extends IWizardPage {

	/**
	 * @return the repository
	 */
	public abstract Repository getRepository();

}