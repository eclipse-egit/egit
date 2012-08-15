package org.eclipse.egit.ui.internal.push;

import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;

/**
 * Wizard for adding a new remote.
 */
public class AddRemoteWizard extends Wizard {

	private AddRemotePage page;

	private URIish uri;

	private String remoteName;

	/**
	 * @param repository
	 */
	public AddRemoteWizard(Repository repository) {
		setWindowTitle("Add Remote"); //$NON-NLS-1$
		page = new AddRemotePage(repository);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		uri = page.getSelection().getURI();
		remoteName = page.getRemoteName();
		return uri != null;
	}

	/**
	 * @return repository selection of URI page
	 */
	public RepositorySelection getRepositorySelection() {
		return page.getSelection();
	}

	/**
	 * @return the entered URI
	 */
	public URIish getUri() {
		return uri;
	}

	/**
	 * @return the entered remote name
	 */
	public String getRemoteName() {
		return remoteName;
	}
}
