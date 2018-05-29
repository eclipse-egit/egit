/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010, Benjamin Muskalla <bmuskalla@eclipsesource.com>
 *
 * All rights reserved. This program and the acco	mpanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RepositorySelectionPage;
import org.eclipse.egit.ui.internal.provisional.wizards.IRepositorySearchResult;
import org.eclipse.egit.ui.internal.provisional.wizards.NoRepositoryInfoException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.util.FileUtils;

/**
 * Import Git Repository Wizard. A front end to a git clone operation.
 */
public class GitCloneWizard extends AbstractGitCloneWizard {

	private static final String HELP_CONTEXT = "org.eclipse.egit.ui.GitCloneWizard"; //$NON-NLS-1$

	/**
	 * Construct the clone wizard with a repository location page that allows
	 * the repository info to be provided by different search providers.
	 */
	public GitCloneWizard() {
		initialize();
	}

	/**
	 * Construct the clone wizard with a preset URI. The wizard skips the
	 * repository location page in this case. Instead, it starts with the Custom
	 * URI repository selection page.
	 *
	 * @param presetUri
	 *            the clone URI to prepopulate the URI field of the clone wizard
	 *            with.
	 */
	public GitCloneWizard(String presetUri) {
		super(new RepositorySelectionPage(true, presetUri));
		initialize();
	}

	/**
	 * Construct the clone wizard based on given repository search result. The
	 * wizard skips the repository location page in this case.
	 *
	 * @param searchResult
	 *            the search result to initialize the clone wizard with.
	 */
	public GitCloneWizard(IRepositorySearchResult searchResult) {
		super(searchResult);
		initialize();
	}

	private void initialize() {
		setWindowTitle(UIText.GitCloneWizard_title);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		setNeedsProgressMonitor(true);
		validSource.setHelpContext(HELP_CONTEXT);
		cloneDestination.setHelpContext(HELP_CONTEXT);
	}

	/**
	 * Set whether to show project import options on the destination page
	 *
	 * @param show
	 * @return this wizard
	 */
	public AbstractGitCloneWizard setShowProjectImport(boolean show) {
		cloneDestination.setShowProjectImport(show);
		return this;
	}

	@Override
	public boolean performCancel() {
		if (alreadyClonedInto != null) {
			File test = new File(alreadyClonedInto);
			if (test.exists()
					&& MessageDialog.openQuestion(getShell(),
							UIText.GitCloneWizard_abortingCloneTitle,
							UIText.GitCloneWizard_abortingCloneMsg)) {
				try {
					FileUtils.delete(test, FileUtils.RECURSIVE | FileUtils.RETRY);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return true;
	}

	@Override
	protected void addPreClonePages() {
		// no pages to add
	}

	@Override
	protected void addPostClonePages() {
		// no pages to add
	}

	@Override
	public boolean canFinish() {
		return cloneDestination.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		try {
			return performClone(currentSearchResult.getGitRepositoryInfo());
		} catch (URISyntaxException e) {
			Activator.error(UIText.GitImportWizard_errorParsingURI, e);
		} catch (NoRepositoryInfoException e) {
			Activator.error(UIText.GitImportWizard_noRepositoryInfo, e);
		} catch (Exception e) {
			Activator.error(e.getMessage(), e);
		} finally {
			setWindowTitle(UIText.GitCloneWizard_title);
		}
		return false;
	}
}
