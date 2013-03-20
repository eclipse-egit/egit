/*******************************************************************************
 * Copyright (C) 2011, 2012  Markus Duft <markus.duft@salomon.at> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.push;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.core.internal.op.PushOperation;
import org.eclipse.egit.core.internal.op.PushOperationSpecification;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.UIPreferences;
import org.eclipse.egit.ui.internal.components.RefContentAssistProvider;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.egit.ui.internal.components.RepositorySelectionPage;
import org.eclipse.egit.ui.internal.components.SimplePushSpecPage;
import org.eclipse.egit.ui.internal.push.PushWizard.PushJob;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.Transport;

/**
 * A simple push wizard, which only pushes out the selected ref/commit to the
 * selected repo.
 */
public class SimplePushRefWizard extends Wizard {

	/**
	 * The commit that will be pushed out.
	 */
	private ObjectId pushObj;

	private RepositorySelectionPage repoPage;

	private String nicePushName;

	private Repository repo;

	private SimplePushSpecPage targetPage;

	/**
	 * Creates a new simple push wizard for a ref
	 *
	 * @param repo
	 *            the repository the ref belongs to
	 * @param refToPush
	 *            the ref to push
	 * @param title
	 *            the wizard title
	 * @throws URISyntaxException
	 */
	public SimplePushRefWizard(Repository repo, Ref refToPush, String title)
			throws URISyntaxException {
		this(repo, refToPush.getObjectId(), refToPush.getName(), title);
	}

	/**
	 * Creates a new simple push wizard which can be used to push out a certain
	 * object.
	 *
	 * @param repo
	 *            the repository the object belongs to
	 * @param objectId
	 *            the object that should be pushed.
	 * @param title
	 *            the wizard title
	 * @throws URISyntaxException
	 */
	public SimplePushRefWizard(Repository repo, ObjectId objectId, String title)
			throws URISyntaxException {
		this(repo, objectId, AbbreviatedObjectId.fromObjectId(objectId).name(), title);
	}

	private SimplePushRefWizard(Repository repo, ObjectId objectId, String name, String title)
			throws URISyntaxException {
		final List<RemoteConfig> remotes = RemoteConfig
				.getAllRemoteConfigs(repo.getConfig());

		this.nicePushName = name;
		this.pushObj = objectId;
		this.repo = repo;

		repoPage = new RepositorySelectionPage(false, remotes, null);
		targetPage = new SimplePushSpecPage(nicePushName, repo) {
			@Override
			public void setVisible(boolean visible) {
				super.setVisible(visible);

				if (visible)
					try {
						if (assist != null
								&& assist.getRepository().equals(
										SimplePushRefWizard.this.repo)
								&& assist.getRemoteURI().equals(
										repoPage.getSelection().getURI(true)))
							return;

						assist = new RefContentAssistProvider(
								SimplePushRefWizard.this.repo, repoPage
										.getSelection().getURI(true), getShell());

					} finally {
						updateDestinationField();
					}
			}
		};
		setWindowTitle(title);
	}

	@Override
	public void addPages() {
		addPage(repoPage);
		addPage(targetPage);
	}

	@Override
	public boolean performFinish() {
		try {
			int timeout = Activator.getDefault().getPreferenceStore()
					.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);

			PushOperationSpecification specification = new PushOperationSpecification();
			RepositorySelection remote = repoPage.getSelection();

			RefSpec refSpec = new RefSpec().
					setSourceDestination(pushObj.name(), targetPage.getTargetRef()).
					setForceUpdate(targetPage.isForceUpdate());

			// Include fetchSpecs in calculation so that tracking refs are also updated
			RemoteConfig remoteConfig = remote.getConfig();
			List<RefSpec> fetchSpecs = remoteConfig != null ? remoteConfig.getFetchRefSpecs() : null;

			Collection<RemoteRefUpdate> remoteRefUpdates = Transport
					.findRemoteRefUpdatesFor(repo,
							Collections.singleton(refSpec), fetchSpecs);

			specification.addURIRefUpdates(remote.getURI(true), remoteRefUpdates);

			PushOperation pop = new PushOperation(repo, specification, false,
					timeout);

			PushJob job = new PushWizard.PushJob(repo, pop, null,
					PushWizard.getDestinationString(remote));
			job.setUser(true);
			job.schedule();

			repoPage.saveUriInPrefs();
		} catch (Exception e) {
			Activator.handleError(e.getMessage(), e, true);
		}

		return true;
	}

}
