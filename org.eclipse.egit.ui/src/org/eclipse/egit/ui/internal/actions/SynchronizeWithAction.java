/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.synchronize.GitSynchronize;
import org.eclipse.egit.ui.internal.synchronize.SelectSynchronizeResourceDialog;
import org.eclipse.egit.ui.internal.synchronize.SyncRepoEntity;
import org.eclipse.egit.ui.internal.synchronize.SyncRepoEntity.SyncRefEntity;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * An action that launch synchronization with selected repository
 */
public class SynchronizeWithAction extends RepositoryAction {

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	protected void execute(IAction action) throws InvocationTargetException,
			InterruptedException {
		Repository[] repos = getRepositories();

		if (repos.length != repos.length) {
			return;
		}

		GitSynchronizeDataSet gsdSet = new GitSynchronizeDataSet();
		for (Repository repo : repos) {
			List<SyncRepoEntity> syncRepoEntitys = createSyncRepoEntitys(repo);
			SelectSynchronizeResourceDialog dialog = new SelectSynchronizeResourceDialog(
					getShell(), repo.getDirectory(), syncRepoEntitys);

			if (dialog.open() != IDialogConstants.OK_ID)
				return;

			gsdSet.add(new GitSynchronizeData(repo, dialog.getSrcRef(), dialog
					.getDstRef(), getProcjetsInRepository(repo), dialog.shouldIncludeLocal()));
		}

		new GitSynchronize(gsdSet);
	}

	private List<SyncRepoEntity> createSyncRepoEntitys(Repository repo)
			throws InvocationTargetException {
		RefDatabase refDatabase = repo.getRefDatabase();
		List<RemoteConfig> remoteConfigs = getRemoteConfigs(repo);
		List<SyncRepoEntity> syncRepoEntitys = new ArrayList<SyncRepoEntity>();

		syncRepoEntitys.add(getLocalSyncRepo(repo));
		for (RemoteConfig rc : remoteConfigs) {
			syncRepoEntitys.add(getRemoteSyncRepo(refDatabase, rc));
		}
		return syncRepoEntitys;
	}

	private List<RemoteConfig> getRemoteConfigs(Repository repo)
			throws InvocationTargetException {
		try {
			return RemoteConfig.getAllRemoteConfigs(repo.getConfig());
		} catch (URISyntaxException e) {
			throw new InvocationTargetException(e);
		}
	}

	private SyncRepoEntity getLocalSyncRepo(Repository repo) {
		Set<String> allRefs = repo.getAllRefs().keySet();
		SyncRepoEntity local = new SyncRepoEntity(
				UIText.SynchronizeWithAction_localRepoName);
		for (String ref : allRefs) {
			if (!ref.startsWith(Constants.R_REMOTES)) {
				String name = ref.substring(ref.lastIndexOf('/') + 1);
				local.addRef(new SyncRefEntity(name, ref));
			}
		}
		return local;
	}

	private SyncRepoEntity getRemoteSyncRepo(RefDatabase refDatabase,
			RemoteConfig rc) throws InvocationTargetException {
		SyncRepoEntity syncRepoEnt = new SyncRepoEntity(rc.getName());
		Collection<Ref> remoteRefs = getRemoteRef(refDatabase, rc.getName());

		for (Ref ref : remoteRefs) {
			String refName = ref.getName();
			String refHumanName = refName
					.substring(refName.lastIndexOf('/') + 1);
			syncRepoEnt.addRef(new SyncRefEntity(refHumanName, refName));
		}
		return syncRepoEnt;
	}

	private Collection<Ref> getRemoteRef(RefDatabase refDb, String remoteName)
			throws InvocationTargetException {
		try {
			return refDb
					.getRefs(Constants.R_REMOTES + remoteName + "/").values(); //$NON-NLS-1$
		} catch (IOException e) {
			throw new InvocationTargetException(e);
		}
	}

}
