/*******************************************************************************
 * Copyright (C) 2010, 2012 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.compare.CompareNavigator;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.GitSubscriberMergeContext;
import org.eclipse.egit.core.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.synchronize.compare.ComparisonDataSource;
import org.eclipse.egit.ui.internal.synchronize.compare.GitCompareInput;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.MergeContext;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Git model synchronization participant
 */
public class GitModelSynchronizeParticipant extends ModelSynchronizeParticipant {

	/**
	 * Key value for obtaining {@link GitSynchronizeDataSet} from {@link ISynchronizePageConfiguration}
	 */
	public static final String SYNCHRONIZATION_DATA = "GIT_SYNCHRONIZE_DATA_SET"; //$NON-NLS-1$

	/**
	 * Id of model compare participant
	 */
	public static final String ID = "org.eclipse.egit.ui.modelCompareParticipant"; //$NON-NLS-1$

	/**
	 * Id of model synchronization participant
	 */
	public static final String VIEWER_ID = "org.eclipse.egit.ui.compareSynchronization"; //$NON-NLS-1$

	private static final String P_NAVIGATOR = "org.eclipse.team.ui.P_NAVIGATOR"; //$NON-NLS-1$

	private static final String WORKSPACE_MODEL_PROVIDER_ID = "org.eclipse.core.resources.modelProvider"; //$NON-NLS-1$

	private static final String DATA_NODE_KEY = "gitSynchronizeData"; //$NON-NLS-1$

	private static final String INCLUDED_PATHS_NODE_KEY = "includedPaths"; //$NON-NLS-1$

	private static final String INCLUDED_PATH_KEY = "path"; //$NON-NLS-1$

	private static final String CONTAINER_PATH_KEY = "container"; //$NON-NLS-1$

	private static final String SRC_REV_KEY = "srcRev"; //$NON-NLS-1$

	private static final String DST_REV_KEY = "dstRev"; //$NON-NLS-1$

	private static final String INCLUDE_LOCAL_KEY = "inludeLocal"; //$NON-NLS-1$

	private static final String FORCE_FETCH_KEY = "forceFetch"; //$NON-NLS-1$


	private GitSynchronizeDataSet gsds;

	/**
	 * DO NOT USE. This constructor is preserved for dynamic initialization when
	 * synchronization context is restored
	 */
	public GitModelSynchronizeParticipant() {
	}

	/**
	 * Creates {@link GitModelSynchronizeParticipant} for given context
	 *
	 * @param context
	 */
	public GitModelSynchronizeParticipant(GitSubscriberMergeContext context) {
		super(context);
		gsds = context.getSyncData();

		try {
			setInitializationData(TeamUI.getSynchronizeManager()
					.getParticipantDescriptor(ID));
		} catch (CoreException e) {
			Activator.logError(e.getMessage(), e);
		}

		setSecondaryId(Long.toString(System.currentTimeMillis()));
	}

	protected void initializeConfiguration(
			final ISynchronizePageConfiguration configuration) {
		configuration.setProperty(ISynchronizePageConfiguration.P_VIEWER_ID,
				VIEWER_ID);
		String modelProvider = WORKSPACE_MODEL_PROVIDER_ID;
		final IPreferenceStore preferenceStore = Activator.getDefault()
				.getPreferenceStore();
		if (!gsds.containsFolderLevelSynchronizationRequest()) {
			if (preferenceStore
					.getBoolean(UIPreferences.SYNC_VIEW_ALWAYS_SHOW_CHANGESET_MODEL)) {
				modelProvider = GitChangeSetModelProvider.ID;
			} else {
				String lastSelectedModel = preferenceStore.getString(UIPreferences.SYNC_VIEW_LAST_SELECTED_MODEL);
				if (!"".equals(lastSelectedModel)) //$NON-NLS-1$
					modelProvider = lastSelectedModel;
			}
		}

		configuration.setProperty(
				ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER,
				modelProvider);

		configuration.setProperty(SYNCHRONIZATION_DATA, gsds);

		super.initializeConfiguration(configuration);

		configuration.addActionContribution(new GitActionContributor());

		configuration.addPropertyChangeListener(new IPropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				if (property.equals(
						ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER)) {
					String newValue = (String) event.getNewValue();
					preferenceStore.setValue(
							UIPreferences.SYNC_VIEW_LAST_SELECTED_MODEL,
							newValue);
				} else if (property.equals(P_NAVIGATOR)) {
					Object oldNavigator = configuration
							.getProperty(P_NAVIGATOR);
					if (!(oldNavigator instanceof GitTreeCompareNavigator))
						configuration.setProperty(P_NAVIGATOR,
								new GitTreeCompareNavigator(
										(CompareNavigator) oldNavigator));
				}
			}
		});
	}

	@Override
	public ModelProvider[] getEnabledModelProviders() {
		ModelProvider[] avaliableProviders = super.getEnabledModelProviders();

		for (ModelProvider provider : avaliableProviders)
			if (provider.getId().equals(GitChangeSetModelProvider.ID))
				return avaliableProviders;

		int capacity = avaliableProviders.length + 1;
		ArrayList<ModelProvider> providers = new ArrayList<ModelProvider>(
				capacity);
		providers.add(GitChangeSetModelProvider.getProvider());

		return providers.toArray(new ModelProvider[providers.size()]);
	}

	@Override
	public boolean hasCompareInputFor(Object object) {
		if (object instanceof GitModelBlob || object instanceof IFile)
			return true;
		// in Java Workspace model Java source files are passed as type
		// CompilationUnit which can be adapted to IResource
		if (object instanceof IAdaptable) {
			IResource res = (IResource) ((IAdaptable) object)
					.getAdapter(IResource.class);
			if (res != null && res.getType() == IResource.FILE)
				return true;
		}
		return super.hasCompareInputFor(object);
	}

	@Override
	public ICompareInput asCompareInput(Object object) {
		// handle file comparison in Workspace model
		if (object instanceof IFile) {
			IFile file = (IFile) object;
			GitSynchronizeData gsd = gsds.getData(file.getProject());
			if (gsd != null && !gsd.shouldIncludeLocal())
				return getFileFromGit(gsd, file.getLocation());
		}

		return super.asCompareInput(object);
	}

	@Override
	public void run(final IWorkbenchPart part) {
		boolean launchFetch = Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.SYNC_VIEW_FETCH_BEFORE_LAUNCH);
		if (launchFetch || gsds.forceFetch()) {
			Job fetchJob = new SynchronizeFetchJob(gsds);
			fetchJob.setUser(true);
			fetchJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					GitModelSynchronizeParticipant.super.run(part);
				}
			});

			fetchJob.schedule();
		} else
			super.run(part);
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		for (GitSynchronizeData gsd : gsds) {
			Repository repo = gsd.getRepository();
			RepositoryMapping mapping = RepositoryMapping.findRepositoryMapping(repo);
			if (mapping != null) {
				IMemento child = memento.createChild(DATA_NODE_KEY);
				child.putString(CONTAINER_PATH_KEY, getPathForContainer(mapping.getContainer()));
				child.putString(SRC_REV_KEY, gsd.getSrcRev());
				child.putString(DST_REV_KEY, gsd.getDstRev());
				child.putBoolean(INCLUDE_LOCAL_KEY, gsd.shouldIncludeLocal());
				Set<IContainer> includedPaths = gsd.getIncludedPaths();
				if (includedPaths != null && !includedPaths.isEmpty()) {
					IMemento paths = child.createChild(INCLUDED_PATHS_NODE_KEY);
					for (IContainer container : includedPaths) {
						String path = getPathForContainer(container);
						paths.createChild(INCLUDED_PATH_KEY).putString(
								INCLUDED_PATH_KEY, path);
					}
				}
			}
		}
		memento.putBoolean(FORCE_FETCH_KEY, gsds.forceFetch());
	}

	@Override
	public void init(String secondaryId, IMemento memento)
			throws PartInitException {
		try {
			boolean forceFetchPref = Activator.getDefault().getPreferenceStore()
					.getBoolean(UIPreferences.SYNC_VIEW_FETCH_BEFORE_LAUNCH);
			boolean forceFetch = getBoolean(memento.getBoolean(FORCE_FETCH_KEY), forceFetchPref);
			gsds = new GitSynchronizeDataSet(forceFetch);
			IMemento[] children = memento.getChildren(DATA_NODE_KEY);
			if (children != null)
				restoreSynchronizationData(children);
		} finally {
			super.init(secondaryId, memento);
		}
	}

	@Override
	protected MergeContext restoreContext(ISynchronizationScopeManager manager)
			throws CoreException {
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				gsds);
		subscriber.init(new NullProgressMonitor());
		return new GitSubscriberMergeContext(subscriber, manager, gsds);
	}

	@Override
	protected ISynchronizationScopeManager createScopeManager(
			ResourceMapping[] mappings) {
		GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				gsds);
		subscriber.init(new NullProgressMonitor());
		GitSubscriberResourceMappingContext context = new GitSubscriberResourceMappingContext(
				subscriber, gsds);
		return new SynchronizationScopeManager(
				UIText.GitModelSynchronizeParticipant_initialScopeName,
				mappings, context, true);
	}

	private ICompareInput getFileFromGit(GitSynchronizeData gsd, IPath location) {
		Repository repo = gsd.getRepository();
		File workTree = repo.getWorkTree();
		String repoRelativeLocation = Repository.stripWorkDir(workTree,
				location.toFile());

		TreeWalk tw = new TreeWalk(repo);
		tw.setRecursive(true);
		tw.setFilter(PathFilter.create(repoRelativeLocation.toString()));
		RevCommit baseCommit = gsd.getSrcRevCommit();
		RevCommit remoteCommit = gsd.getDstRevCommit();

		try {
			int baseNth = tw.addTree(baseCommit.getTree());
			int remoteNth = tw.addTree(remoteCommit.getTree());

			if (tw.next()) {
				ComparisonDataSource baseData = new ComparisonDataSource(
						baseCommit, tw.getObjectId(baseNth));
				ComparisonDataSource remoteData = new ComparisonDataSource(
						remoteCommit, tw.getObjectId(remoteNth));
				return new GitCompareInput(repo, baseData, baseData,
						remoteData, repoRelativeLocation);
			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		return null;
	}

	private void restoreSynchronizationData(IMemento[] children) {
		for (IMemento child : children) {
			String containerPath = child.getString(CONTAINER_PATH_KEY);
			Repository repo = getRepositoryForPath(containerPath);
			if (repo == null)
				continue;
			String srcRev = child.getString(SRC_REV_KEY);
			String dstRev = child.getString(DST_REV_KEY);
			boolean includeLocal = getBoolean(
					child.getBoolean(INCLUDE_LOCAL_KEY), true);
			Set<IContainer> includedPaths = getIncludedPaths(child);
			try {
				GitSynchronizeData data = new GitSynchronizeData(repo, srcRev,
						dstRev, includeLocal);
				if (includedPaths != null)
					data.setIncludedPaths(includedPaths);
				gsds.add(data);
			} catch (IOException e) {
				Activator.logError(e.getMessage(), e);
				continue;
			}
		}
	}

	private Repository getRepositoryForPath(String containerPath) {
		IPath path = Path.fromPortableString(containerPath);
		IContainer mappedContainer = ResourcesPlugin.getWorkspace().getRoot()
				.getContainerForLocation(path);
		GitProjectData projectData = GitProjectData.get((IProject) mappedContainer);
		RepositoryMapping mapping = projectData.getRepositoryMapping(mappedContainer);
		if (mapping != null)
			return mapping.getRepository();
		return null;
	}

	private boolean getBoolean(Boolean value, boolean defaultValue) {
		return value != null ? value.booleanValue() : defaultValue;
	}

	private String getPathForContainer(IContainer container) {
		return container.getLocation().toPortableString();
	}

	private Set<IContainer> getIncludedPaths(IMemento memento) {
		IMemento child = memento.getChild(INCLUDED_PATHS_NODE_KEY);
		Set<IContainer> result = new HashSet<IContainer>();
		if (child != null) {
			IMemento[] pathNode = child.getChildren(INCLUDED_PATH_KEY);
			if (pathNode != null) {
				for (IMemento path : pathNode) {
					String includedPath = path.getString(INCLUDED_PATH_KEY);
					IContainer container = ResourcesPlugin.getWorkspace().getRoot()
							.getContainerForLocation(new Path(includedPath));
					result.add(container);
				}
				return result;
			}
		}
		return null;
	}

}
