package org.eclipse.egit.ui.internal.fetch;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;

/**
 * Wizard for fetching a Gerrit change
 */
public class FetchGerritChangeWizard extends Wizard {
	private final Repository repository;

	FetchGerritChangePage page;

	/**
	 * @param repository
	 *            the repository
	 */
	public FetchGerritChangeWizard(Repository repository) {
		this.repository = repository;
		setNeedsProgressMonitor(true);
		setHelpAvailable(false);
		setWindowTitle(UIText.FetchGerritChangeWizard_WizardTitle);
	}

	@Override
	public void addPages() {
		page = new FetchGerritChangePage(repository);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		return page.doFetch();
	}
}
