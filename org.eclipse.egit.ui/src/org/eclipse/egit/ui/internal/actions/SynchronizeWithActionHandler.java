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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.GitSubscriberMergeContext;
import org.eclipse.egit.core.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.synchronize.GitModelSynchronizeParticipant;
import org.eclipse.egit.ui.internal.synchronize.SelectSynchronizeResourceDialog;
import org.eclipse.egit.ui.internal.synchronize.SyncRepoEntity;
import org.eclipse.egit.ui.internal.synchronize.SyncRepoEntity.SyncRefEntity;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

/**
 * An action that launch synchronization with selected repository
 */
public class SynchronizeWithActionHandler extends RepositoryActionHandler {

	@Override
	public boolean isEnabled() {
		return true;
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository[] repos = getRepositories(event);

		if (repos.length != repos.length)
			return null;

		GitSynchronizeDataSet gsdSet = new GitSynchronizeDataSet();
		for (Repository repo : repos) {
			try {
				List<SyncRepoEntity> syncRepoEntitys = createSyncRepoEntitys(repo);
				SelectSynchronizeResourceDialog dialog = new SelectSynchronizeResourceDialog(
						getShell(event), repo.getDirectory(), syncRepoEntitys);

				if (dialog.open() != IDialogConstants.OK_ID)
					return null;

				gsdSet.add(new GitSynchronizeData(repo, dialog.getSrcRef(),
						dialog.getDstRef(), dialog.shouldIncludeLocal()));
			} catch (URISyntaxException e) {
				Activator.handleError(e.getMessage(), e, true);
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		}

		// new GitSynchronize(gsdSet);

		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				gsdSet);
		ResourceMapping[] mappings = getSelectedResourceMappings(event);
		RemoteResourceMappingContext remoteContext = new GitSubscriberResourceMappingContext(gsdSet);
		SubscriberScopeManager manager = new SubscriberScopeManager(
				subscriber.getName(), mappings, subscriber, remoteContext, true);
		SynchronizationContext context = new GitSubscriberMergeContext(
				subscriber, manager, gsdSet);
		GitModelSynchronizeParticipant participant = new GitModelSynchronizeParticipant(context);
		TeamUI.getSynchronizeManager().addSynchronizeParticipants(
				new ISynchronizeParticipant[] { participant });
		participant.run(getTargetPart());

		return null;
	}

	private List<SyncRepoEntity> createSyncRepoEntitys(Repository repo)
			throws URISyntaxException, IOException {
		RefDatabase refDatabase = repo.getRefDatabase();
		List<RemoteConfig> remoteConfigs = getRemoteConfigs(repo);
		List<SyncRepoEntity> syncRepoEntitys = new ArrayList<SyncRepoEntity>();

		syncRepoEntitys.add(getLocalSyncRepo(repo));
		for (RemoteConfig rc : remoteConfigs)
			syncRepoEntitys.add(getRemoteSyncRepo(refDatabase, rc));

		return syncRepoEntitys;
	}

	private List<RemoteConfig> getRemoteConfigs(Repository repo)
			throws URISyntaxException {
		return RemoteConfig.getAllRemoteConfigs(repo.getConfig());
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
			RemoteConfig rc) throws IOException {
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
			throws IOException {
		return refDb.getRefs(Constants.R_REMOTES + remoteName + "/").values(); //$NON-NLS-1$
	}


	/**
     * Based on {@link TeamAction#getSelectedResourceMappings}
     *
	 * @param event
     * @return the resource mappings that contain resources associated with the given provider
	 * @throws ExecutionException
	 */
    private ResourceMapping[] getSelectedResourceMappings(ExecutionEvent event) throws ExecutionException {
        Object[] elements = getSelectedResources(event);
        ArrayList providerMappings = new ArrayList();
        for (int i = 0; i < elements.length; i++) {
            Object object = elements[i];
            Object adapted = getResourceMapping(object);
            if (adapted instanceof ResourceMapping) {
                ResourceMapping mapping = (ResourceMapping) adapted;
                if (isMappedToProvider(mapping, "org.eclipse.egit.core.GitProvider")) { //$NON-NLS-1$
                    providerMappings.add(mapping);
                }
            }
        }
        return (ResourceMapping[]) providerMappings.toArray(new ResourceMapping[providerMappings.size()]);
    }

    /**
     * Copied from TeamAction#getResourceMapping(Object)
     *
     * @param object
     * @return resource mapping
     */
    private Object getResourceMapping(Object object) {
        if (object instanceof ResourceMapping)
            return object;

        if (object instanceof IAdaptable)
        	return ((IAdaptable) object).getAdapter(ResourceMapping.class);

        return Utils.getResourceMapping(object);
    }

    /**
     * Copied from TeamAction#isMappedToProvider(ResourceMapping, String)
     *
     * @param element
     * @param providerId
     * @return TODO
     */
    private boolean isMappedToProvider(ResourceMapping element, String providerId) {
        IProject[] projects = element.getProjects();
        for (int k = 0; k < projects.length; k++) {
            IProject project = projects[k];
            RepositoryProvider provider = RepositoryProvider.getProvider(project);
            if (provider != null && provider.getID().equals(providerId)) {
                return true;
            }
        }
        return false;
    }

	private IWorkbenchPart getTargetPart() {
		IWorkbenchPart targetPart = null;
		IWorkbenchPage page = TeamUIPlugin.getActivePage();
		if (page != null) {
			targetPart = page.getActivePart();
		}
		return targetPart;
	}

}
