/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2015, IBM Corporation (Dani Megert <daniel_megert@ch.ibm.com>)
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 * Copyright (C) 2016, Andre Bossert <anb0s@anbos.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;

/**
 * This class keeps information about how a project is mapped to
 * a Git repository.
 */
public class GitProjectData {

	private static final Map<IProject, GitProjectData> projectDataCache = new HashMap<>();

	private static Set<RepositoryMappingChangeListener> repositoryChangeListeners = new HashSet<>();

	@SuppressWarnings("synthetic-access")
	private static final IResourceChangeListener rcl = new RCL();

	private static class RCL implements IResourceChangeListener {
		@Override
		@SuppressWarnings("synthetic-access")
		public void resourceChanged(final IResourceChangeEvent event) {
			switch (event.getType()) {
			case IResourceChangeEvent.PRE_CLOSE:
				uncache((IProject) event.getResource());
				break;
			case IResourceChangeEvent.PRE_DELETE:
				try {
					delete((IProject) event.getResource());
				} catch (IOException e) {
					Activator.logError(e.getMessage(), e);
				}
				break;
			case IResourceChangeEvent.POST_CHANGE:
				update(event);
				break;
			default:
				break;
			}
		}
	}

	private static QualifiedName MAPPING_KEY = new QualifiedName(
			GitProjectData.class.getName(), "RepositoryMapping");  //$NON-NLS-1$

	/**
	 * Start listening for resource changes.
	 */
	public static void attachToWorkspace() {
		trace("attachToWorkspace - addResourceChangeListener");  //$NON-NLS-1$
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				rcl,
				IResourceChangeEvent.POST_CHANGE
						| IResourceChangeEvent.PRE_CLOSE
						| IResourceChangeEvent.PRE_DELETE);
	}

	/**
	 * Stop listening to resource changes
	 */
	public static void detachFromWorkspace() {
		trace("detachFromWorkspace - removeResourceChangeListener"); //$NON-NLS-1$
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(rcl);
	}

	/**
	 * Register a new listener for repository modification events.
	 * <p>
	 * This is a no-op if <code>objectThatCares</code> has already been
	 * registered.
	 * </p>
	 *
	 * @param objectThatCares
	 *            the new listener to register. Must not be null.
	 */
	public static synchronized void addRepositoryChangeListener(
			final RepositoryMappingChangeListener objectThatCares) {
		if (objectThatCares == null)
			throw new NullPointerException();
		repositoryChangeListeners.add(objectThatCares);
	}

	/**
	 * Remove a registered {@link RepositoryMappingChangeListener}
	 *
	 * @param objectThatCares
	 *            The listener to remove
	 */
	public static synchronized void removeRepositoryChangeListener(
			final RepositoryMappingChangeListener objectThatCares) {
		repositoryChangeListeners.remove(objectThatCares);
	}

	/**
	 * Notify registered {@link RepositoryMappingChangeListener}s of a change.
	 *
	 * @param which
	 *            the repository which has had changes occur within it.
	 */
	static void fireRepositoryChanged(final RepositoryMapping which) {
		Job job = new Job(CoreText.GitProjectData_repositoryChangedJobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				RepositoryMappingChangeListener[] listeners = getRepositoryChangeListeners();
				monitor.beginTask(
						CoreText.GitProjectData_repositoryChangedTaskName,
						listeners.length);

				for (RepositoryMappingChangeListener listener : listeners) {
					listener.repositoryChanged(which);
					monitor.worked(1);
				}

				monitor.done();

				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.REPOSITORY_CHANGED.equals(family))
					return true;

				return super.belongsTo(family);
			}
		};
		job.setUser(false);
		job.schedule();
	}

	/**
	 * Get a copy of the current set of repository change listeners
	 * <p>
	 * The array has no references, so is safe for iteration and modification
	 *
	 * @return a copy of the current repository change listeners
	 */
	private static synchronized RepositoryMappingChangeListener[] getRepositoryChangeListeners() {
		return repositoryChangeListeners
				.toArray(new RepositoryMappingChangeListener[repositoryChangeListeners
						.size()]);
	}

	/**
	 * @param p
	 * @return {@link GitProjectData} for the specified project, or null if the
	 *         Git provider is not associated with the project or an exception
	 *         occurred
	 */
	@Nullable
	public synchronized static GitProjectData get(final @NonNull IProject p) {
		try {
			GitProjectData d = lookup(p);
			if (d == null && ResourceUtil.isSharedWithGit(p)) {
				d = new GitProjectData(p).load();
				cache(p, d);
			}
			return d;
		} catch (IOException err) {
			Activator.logError(CoreText.GitProjectData_missing, err);
			return null;
		}
	}

	/**
	 * Drop the Eclipse project from our association of projects/repositories
	 *
	 * @param p
	 *            Eclipse project
	 * @throws IOException
	 *             if deletion of property files failed
	 */
	public static void delete(final IProject p) throws IOException {
		trace("delete(" + p.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		GitProjectData d = lookup(p);
		if (d == null)
			deletePropertyFiles(p);
		else
			d.deletePropertyFilesAndUncache();
	}

	/**
	 * Drop the Eclipse project from our association of projects/repositories
	 * and remove all RepositoryMappings.
	 *
	 * @param p
	 *            to deconfigure
	 * @throws IOException
	 *             if the property file cannot be removed.
	 */
	public static void deconfigure(final IProject p) throws IOException {
		trace("deconfigure(" + p.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		GitProjectData d = lookup(p);
		if (d == null) {
			deletePropertyFiles(p);
		} else {
			d.deletePropertyFilesAndUncache();
			unmap(d);
		}
	}

	/**
	 * Add the Eclipse project to our association of projects/repositories
	 *
	 * @param p
	 *            Eclipse project
	 * @param d
	 *            {@link GitProjectData} associated with this project
	 */
	public static void add(final IProject p, final GitProjectData d) {
		trace("add(" + p.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

		cache(p, d);
	}

	/**
	 * Update mappings of EGit-managed projects in response to new DOT_GIT
	 * repositories appearing.
	 *
	 * @param event
	 *            A {@link IResourceChangeEvent#POST_CHANGE} event
	 */
	public static void update(IResourceChangeEvent event) {
		// If the project is EGit-managed, let's see if this added any DOT_GIT
		// files or folders. If so, update the project's RepositoryMappings
		// and then mark as team private, if anything added. We won't get
		// deletions of DOT_GIT directories or files here, those are
		// "protected" and the GitMoveDeleteHook will prevent their deletion --
		// the project has to be disconnected first.
		final Set<GitProjectData> modified = new HashSet<>();
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta)
						throws CoreException {
					IResource resource = delta.getResource();
					int type = resource.getType();
					if (type == IResource.ROOT) {
						return true;
					} else if (type == IResource.PROJECT) {
						return (delta.getKind() & (IResourceDelta.ADDED
								| IResourceDelta.CHANGED)) != 0
								&& ResourceUtil.isSharedWithGit(resource);
					}
					// Files & folders
					if ((delta.getKind() & (IResourceDelta.ADDED
							| IResourceDelta.CHANGED)) == 0
							|| resource.isLinked()) {
						return false;
					}
					IPath location = resource.getLocation();
					if (location == null) {
						return false;
					}
					if (!Constants.DOT_GIT.equals(resource.getName())) {
						return type == IResource.FOLDER;
					}
					// A file or folder named .git
					File gitCandidate = location.toFile().getParentFile();
					File git = new FileRepositoryBuilder()
							.addCeilingDirectory(gitCandidate)
							.findGitDir(gitCandidate).getGitDir();
					if (git == null) {
						return false;
					}
					// Yes, indeed a valid git directory.
					GitProjectData data = get(resource.getProject());
					if (data == null) {
						return false;
					}
					RepositoryMapping m = RepositoryMapping
							.create(resource.getParent(), git);
					// Is its working directory really here? If not,
					// a submodule folder may have been copied.
					try {
						Repository r = Activator.getDefault()
								.getRepositoryCache().lookupRepository(git);
						if (m != null && r != null && !r.isBare()
								&& gitCandidate.equals(r.getWorkTree())) {
							if (data.map(m)) {
								data.mappings.put(m.getContainerPath(), m);
								modified.add(data);
							}
						}
					} catch (IOException e) {
						Activator.logError(e.getMessage(), e);
					}
					return false;
				}
			});
		} catch (CoreException e) {
			Activator.logError(e.getMessage(), e);
		} finally {
			for (GitProjectData data : modified) {
				try {
					data.store();
				} catch (CoreException e) {
					Activator.logError(e.getMessage(), e);
				}
			}
		}
	}

	static void trace(final String m) {
		// TODO is this the right location?
		if (GitTraceLocation.CORE.isActive())
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.CORE.getLocation(),
					"(GitProjectData) " + m); //$NON-NLS-1$
	}

	private synchronized static void cache(final IProject p,
			final GitProjectData d) {
		projectDataCache.put(p, d);
	}

	private synchronized static void uncache(final IProject p) {
		if (projectDataCache.remove(p) != null) {
			trace("uncacheDataFor(" //$NON-NLS-1$
				+ p.getName() + ")"); //$NON-NLS-1$
		}
	}

	private static void unmap(GitProjectData data) {
		for (RepositoryMapping m : data.mappings.values()) {
			IContainer c = m.getContainer();
			if (c != null && c.isAccessible()) {
				try {
					c.setSessionProperty(MAPPING_KEY, null);
					// Team private members are re-set in
					// DisconnectProviderOperation
				} catch (CoreException e) {
					Activator.logWarning(MessageFormat.format(
							CoreText.GitProjectData_failedToUnmapRepoMapping,
							c.getFullPath()), e);
				}
			}
		}
	}

	private synchronized static GitProjectData lookup(final IProject p) {
		return projectDataCache.get(p);
	}

	/**
	 * Update the settings for the global window cache of the workspace.
	 */
	public static void reconfigureWindowCache() {
		final WindowCacheConfig c = new WindowCacheConfig();
		IEclipsePreferences d = DefaultScope.INSTANCE.getNode(Activator.getPluginId());
		IEclipsePreferences p = InstanceScope.INSTANCE.getNode(Activator.getPluginId());
		c.setPackedGitLimit(p.getInt(GitCorePreferences.core_packedGitLimit, d.getInt(GitCorePreferences.core_packedGitLimit, 0)));
		c.setPackedGitWindowSize(p.getInt(GitCorePreferences.core_packedGitWindowSize, d.getInt(GitCorePreferences.core_packedGitWindowSize, 0)));
		if (SystemReader.getInstance().isWindows()) {
			c.setPackedGitMMAP(false);
		} else {
			c.setPackedGitMMAP(
					p.getBoolean(GitCorePreferences.core_packedGitMMAP,
							d.getBoolean(GitCorePreferences.core_packedGitMMAP,
									false)));
		}
		c.setDeltaBaseCacheLimit(p.getInt(GitCorePreferences.core_deltaBaseCacheLimit, d.getInt(GitCorePreferences.core_deltaBaseCacheLimit, 0)));
		c.setStreamFileThreshold(p.getInt(GitCorePreferences.core_streamFileThreshold, d.getInt(GitCorePreferences.core_streamFileThreshold, 0)));
		c.install();
	}

	private final IProject project;

	private final Map<IPath, RepositoryMapping> mappings = new HashMap<>();

	private final Set<IResource> protectedResources = new HashSet<>();

	/**
	 * Construct a {@link GitProjectData} for the mapping
	 * of a project.
	 *
	 * @param p Eclipse project
	 */
	public GitProjectData(final IProject p) {
		project = p;
	}

	/**
	 * @return the Eclipse project mapped through this resource.
	 */
	public IProject getProject() {
		return project;
	}

	/**
	 * Set repository mappings
	 *
	 * @param newMappings
	 */
	public void setRepositoryMappings(final Collection<RepositoryMapping> newMappings) {
		mappings.clear();
		for (RepositoryMapping mapping : newMappings) {
			mappings.put(mapping.getContainerPath(), mapping);
		}
		remapAll();
	}

	/**
	 * Get repository mappings
	 *
	 * @return the repository mappings for a project
	 */
	public final Map<IPath, RepositoryMapping> getRepositoryMappings() {
		return mappings;
	}

	/**
	 * Hide our private parts from the navigators other browsers.
	 *
	 * @throws CoreException
	 */
	public void markTeamPrivateResources() throws CoreException {
		for (final RepositoryMapping rm : mappings.values()) {
			final IContainer c = rm.getContainer();
			if (c == null)
				continue; // Not fully mapped yet?

			final IResource dotGit = c.findMember(Constants.DOT_GIT);
			if (dotGit != null) {
				try {
					final Repository r = rm.getRepository();
					final File dotGitDir = dotGit.getLocation().toFile()
							.getCanonicalFile();
					// TODO: .git *files* with gitdir: "redirect"
					// TODO: check whether Repository.getDirectory() is
					// canonicalized! If not, this check will fail anyway.
					if (dotGitDir.equals(r.getDirectory())) {
						trace("teamPrivate " + dotGit);  //$NON-NLS-1$
						dotGit.setTeamPrivateMember(true);
					}
				} catch (IOException err) {
					throw new CoreException(Activator.error(CoreText.Error_CanonicalFile, err));
				}
			}
		}
	}

	/**
	 * Determines whether the project this instance belongs to has any inner
	 * repositories like submodules or nested repositories.
	 *
	 * @return {@code true} if the project has inner repositories; {@code false}
	 *         otherwise.
	 */
	public boolean hasInnerRepositories() {
		return !protectedResources.isEmpty();
	}

	/**
	 * @param f
	 * @return true if a resource is protected in this repository
	 */
	public boolean isProtected(final IResource f) {
		return protectedResources.contains(f);
	}

	/**
	 * @param resource any workbench resource contained within this project.
	 * @return the mapping for the specified project
	 */
	@Nullable
	public /* TODO static */ RepositoryMapping getRepositoryMapping(
			@Nullable IResource resource) {
		IResource r = resource;
		try {
			for (; r != null; r = r.getParent()) {
				final RepositoryMapping m;

				if (!r.isAccessible())
					continue;
				m = (RepositoryMapping) r.getSessionProperty(MAPPING_KEY);
				if (m != null)
					return m;
			}
		} catch (CoreException err) {
			Activator.logError(
					CoreText.GitProjectData_failedFindingRepoMapping, err);
		}
		return null;
	}

	private void deletePropertyFilesAndUncache() throws IOException {
		deletePropertyFiles(getProject());
		uncache(getProject());
	}

	private static void deletePropertyFiles(IProject project) throws IOException {
		final File dir = propertyFile(project).getParentFile();
		FileUtils.delete(dir, FileUtils.RECURSIVE);
		trace("deleteDataFor(" //$NON-NLS-1$
				+ project.getName() + ")"); //$NON-NLS-1$
	}

	/**
	 * Store information about the repository connection in the workspace
	 *
	 * @throws CoreException
	 */
	public void store() throws CoreException {
		final File dat = propertyFile();
		final File tmp;
		boolean ok = false;

		try {
			trace("save " + dat);  //$NON-NLS-1$
			tmp = File.createTempFile(
					"gpd_",  //$NON-NLS-1$
					".prop",   //$NON-NLS-1$
					dat.getParentFile());
			final FileOutputStream o = new FileOutputStream(tmp);
			try {
				final Properties p = new Properties();
				for (final RepositoryMapping repoMapping : mappings.values()) {
					repoMapping.store(p);
				}
				p.store(o, "GitProjectData");  //$NON-NLS-1$
				ok = true;
			} finally {
				o.close();
				if (!ok && tmp.exists()) {
					FileUtils.delete(tmp);
				}
			}
			if (dat.exists())
				FileUtils.delete(dat);
			if (!tmp.renameTo(dat)) {
				if (tmp.exists())
					FileUtils.delete(tmp);
				throw new CoreException(
						Activator.error(NLS.bind(
								CoreText.GitProjectData_saveFailed, dat), null));
			}
		} catch (IOException ioe) {
			throw new CoreException(Activator.error(
					NLS.bind(CoreText.GitProjectData_saveFailed, dat), ioe));
		}
	}

	private File propertyFile() {
		return propertyFile(getProject());
	}

	private static File propertyFile(IProject project) {
		return new File(project.getWorkingLocation(Activator.getPluginId())
				.toFile(), "GitProjectData.properties"); //$NON-NLS-1$
	}

	private GitProjectData load() throws IOException {
		final File dat = propertyFile();
		trace("load " + dat);  //$NON-NLS-1$

		final FileInputStream o = new FileInputStream(dat);
		try {
			final Properties p = new Properties();
			p.load(o);

			mappings.clear();
			for (final Object keyObj : p.keySet()) {
				final String key = keyObj.toString();
				if (RepositoryMapping.isInitialKey(key)) {
					RepositoryMapping mapping = new RepositoryMapping(p, key);
					mappings.put(mapping.getContainerPath(), mapping);
				}
			}
		} finally {
			o.close();
		}

		if (!remapAll()) {
			try {
				store();
			} catch (CoreException e) {
				IStatus status = e.getStatus();
				Activator.logError(status.getMessage(), status.getException());
			}
		}
		return this;
	}

	private boolean remapAll() {
		protectedResources.clear();
		boolean allMapped = true;
		Iterator<RepositoryMapping> iterator = mappings.values().iterator();
		while (iterator.hasNext()) {
			RepositoryMapping m = iterator.next();
			if (!map(m)) {
				iterator.remove();
				allMapped = false;
			}
		}
		return allMapped;
	}

	private boolean map(final RepositoryMapping m) {
		final IResource r;
		final File git;
		final IResource dotGit;
		IContainer c = null;

		m.clear();
		r = getProject().findMember(m.getContainerPath());
		if (r instanceof IContainer) {
			c = (IContainer) r;
		} else if (r != null) {
			c = Utils.getAdapter(r, IContainer.class);
		}

		if (c == null) {
			logAndUnmapGoneMappedResource(m, null);
			return false;
		}
		m.setContainer(c);

		IPath absolutePath = m.getGitDirAbsolutePath();
		if (absolutePath == null) {
			logAndUnmapGoneMappedResource(m, c);
			return false;
		}
		git = absolutePath.toFile();

		if (!RepositoryCache.FileKey.isGitRepository(git, FS.DETECTED)) {
			logAndUnmapGoneMappedResource(m, c);
			return false;
		}

		try {
			m.setRepository(Activator.getDefault().getRepositoryCache()
					.lookupRepository(git));
		} catch (IOException ioe) {
			logAndUnmapGoneMappedResource(m, c);
			return false;
		}

		trace("map "  //$NON-NLS-1$
				+ c
				+ " -> "  //$NON-NLS-1$
				+ m.getRepository());
		try {
			c.setSessionProperty(MAPPING_KEY, m);
		} catch (CoreException err) {
			Activator.logError(
					CoreText.GitProjectData_failedToCacheRepoMapping, err);
		}

		dotGit = c.findMember(Constants.DOT_GIT);
		if (dotGit != null) {
			protect(dotGit);
		}

		fireRepositoryChanged(m);

		return true;
	}

	private void logAndUnmapGoneMappedResource(final RepositoryMapping m,
			final IContainer c) {
		Activator.logError(MessageFormat.format(
				CoreText.GitProjectData_mappedResourceGone, m.toString()),
				new FileNotFoundException(m.getContainerPath().toString()));
		m.clear();
		if (c instanceof IProject) {
			UnmapJob unmapJob = new UnmapJob((IProject) c);
			unmapJob.schedule();
		} else if (c != null) {
			try {
				c.setSessionProperty(MAPPING_KEY, null);
			} catch (CoreException e) {
				Activator.logWarning(MessageFormat.format(
						CoreText.GitProjectData_failedToUnmapRepoMapping,
						c.getFullPath()), e);
			}
		}
	}

	private void protect(IResource resource) {
		IResource c = resource;
		try {
			c.setTeamPrivateMember(true);
		} catch (CoreException e) {
			Activator.logError(MessageFormat.format(
					CoreText.GitProjectData_FailedToMarkTeamPrivate,
					c.getFullPath()), e);
		}
		while (c != null && !c.equals(getProject())) {
			trace("protect " + c);  //$NON-NLS-1$
			protectedResources.add(c);
			c = c.getParent();
		}
	}

	private static class UnmapJob extends Job {

		private final IProject project;

		private UnmapJob(IProject project) {
			super(MessageFormat.format(CoreText.GitProjectData_UnmapJobName,
					project.getName()));
			this.project = project;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				RepositoryProvider.unmap(project);
				return Status.OK_STATUS;
			} catch (TeamException e) {
				return new Status(IStatus.ERROR, Activator.getPluginId(),
						MessageFormat.format(
								CoreText.GitProjectData_UnmappingGoneResourceFailed,
								project.getName()),
						e);
			}
		}
	}
}
