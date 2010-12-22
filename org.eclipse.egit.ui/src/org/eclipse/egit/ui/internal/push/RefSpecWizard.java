package org.eclipse.egit.ui.internal.push;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.ui.internal.components.RefSpecPage;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Wizard to maintain RefSpecs
 */
public class RefSpecWizard extends Wizard {
	private final boolean pushMode;

	private final RemoteConfig config;

	private RefSpecPage page;

	IProgressMonitor monitor;

	/**
	 * @param repository
	 * @param config
	 * @param pushMode
	 * @param monitor
	 */
	public RefSpecWizard(Repository repository, RemoteConfig config,
			boolean pushMode, IProgressMonitor monitor) {
		setNeedsProgressMonitor(true);
		this.pushMode = pushMode;
		this.config = config;
		this.monitor = monitor;
		if (this.monitor == null)
			this.monitor = new NullProgressMonitor();
		page = new RefSpecPage(repository, pushMode);
	}

	@Override
	public void addPages() {
		addPage(page);
	}

	@Override
	public IWizardPage getStartingPage() {
			page.setSelection(new RepositorySelection(null, config));
		return super.getStartingPage();
	}

	@Override
	public boolean performFinish() {
		if (pushMode) {
			config.setPushRefSpecs(page.getRefSpecs());
		} else {
			config.setFetchRefSpecs(page.getRefSpecs());
			config.setTagOpt(page.getTagOpt());
		}
		return true;
	}

}
