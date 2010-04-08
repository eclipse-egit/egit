package org.eclipse.egit.ui.internal.repository;

import org.eclipse.egit.ui.internal.components.RepositorySelectionPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.transport.URIish;

/**
 * @author D022737
 *
 */
public class SelectUriWiazrd extends Wizard {

	private URIish uri;

	/**
	 * @param sourceSelection
	 */
	public SelectUriWiazrd(boolean sourceSelection) {
		addPage(new RepositorySelectionPage(sourceSelection, null));
	}

	/**
	 * @param sourceSelection
	 * @param presetUri
	 */
	public SelectUriWiazrd(boolean sourceSelection, String presetUri) {
		addPage(new RepositorySelectionPage(sourceSelection, presetUri));
	}

	/**
	 * @return the URI
	 */
	public URIish getUri() {
		return uri;
	}

	@Override
	public boolean performFinish() {
		uri = ((RepositorySelectionPage) getPages()[0]).getSelection().getURI();
		return uri != null;
	}

}
