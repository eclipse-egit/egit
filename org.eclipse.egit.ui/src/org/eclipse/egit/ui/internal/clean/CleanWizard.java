package org.eclipse.egit.ui.internal.clean;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;

/**
 * Represents the contents of the Clean Wizard
 */
public class CleanWizard extends Wizard {

	private CleanRepositoryPage cleanPage;

	/**
	 * Creates a new Wizard and adds all required pages.
	 * @param repository the repository to clean
	 */
	public CleanWizard(Repository repository) {
		setNeedsProgressMonitor(true);
		cleanPage = new CleanRepositoryPage(repository);
		addPage(cleanPage);
	}

	@Override
	public boolean performFinish() {
		cleanPage.finish();
		return true;
	}

}
