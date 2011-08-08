/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.GitProvider;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.core.internal.trace.GitTraceLocation;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.WindowCache;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;

/**
 * This class keeps information about how a project is mapped to
 * a Git repository.
 */
public class GitProjectData {

	private static final Map<IProject, GitProjectData> projectDataCache = new HashMap<IProject, GitProjectData>();

	private static Set<RepositoryChangeListener> repositoryChangeListeners = new HashSet<RepositoryChangeListener>();

	@SuppressWarnings("synthetic-access")
	private static final IResourceChangeListener rcl = new RCL();

	private static class RCL implements IResourceChangeListener {
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
			default:
				break;
			}
		}
	}

	private static QualifiedName MAPPING_KEY = new QualifiedName(
			GitProjectData.class.getName(), "RepositoryMapping");  //$NON-NLS-1$

	/**
	 * Start listening for resource changes.
	 *
	 * @param includeChange true to listen to content changes
	 */
	public static void attachToWorkspace(final boolean includeChange) {
		trace("attachToWorkspace - addResourceChangeListener");  //$NON-NLS-1$
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				rcl,
				(includeChange ? IResourceChangeEvent.POST_CHANGE : 0)
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
			final RepositoryChangeListener objectThatCares) {
		if (objectThatCares == null)
			throw new NullPointerException();
		repositoryChangeListeners.add(objectThatCares);
	}

	/**
	 * Remove a registered {@link RepositoryChangeListener}
	 *
	 * @param objectThatCares
	 *            The listener to remove
	 */
	public static synchronized void removeRepositoryChangeListener(
			final RepositoryChangeListener objectThatCares) {
		repositoryChangeListeners.remove(objectThatCares);
	}

	/**
	 * Notify registered {@link RepositoryChangeListener}s of a change.
	 *
	 * @param which
	 *            the repository which has had changes occur within it.
	 */
	static void fireRepositoryChanged(final RepositoryMapping which) {
		Job job = new Job(CoreText.GitProjectData_repositoryChangedJobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				RepositoryChangeListener[] listeners = getRepositoryChangeListeners();
				monitor.beginTask(
						CoreText.GitProjectData_repositoryChangedTaskName,
						listeners.length);

				for (RepositoryChangeListener listener : listeners) {
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

		job.schedule();
	}

	/**
	 * Get a copy of the current set of repository change listeners
	 * <p>
	 * The array has no references, so is safe for iteration and modification
	 *
	 * @return a copy of the current repository change listeners
	 */
	private static synchronized RepositoryChangeListener[] getRepositoryChangeListeners() {
		return repositoryChangeListeners
				.toArray(new RepositoryChangeListener[repositoryChangeListeners
						.size()]);
	}

	/**
	 * @param p
	 * @return {@link GitProjectData} for the specified project
	 */
	public synchronized static GitProjectData get(final IProject p) {
		try {
			GitProjectData d = lookup(p);
			if (d == null
					&& RepositoryProvider.getProvider(p) instanceof GitProvider) {
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

	private synchronized static GitProjectData lookup(final IProject p) {
		return projectDataCache.get(p);
	}

	/**
	 * Update the settings for the global window cache of the workspace.
	 */
	public static void reconfigureWindowCache() {
		final WindowCacheConfig c = new WindowCacheConfig();
		IEclipsePreferences d = new DefaultScope().getNode(Activator.getPluginId());
		IEclipsePreferences p = new InstanceScope().getNode(Activator.getPluginId());
		c.setPackedGitLimit(p.getInt(GitCorePreferences.core_packedGitLimit, d.getInt(GitCorePreferences.core_packedGitLimit, 0)));
		c.setPackedGitWindowSize(p.getInt(GitCorePreferences.core_packedGitWindowSize, d.getInt(GitCorePreferences.core_packedGitWindowSize, 0)));
		c.setPackedGitMMAP(p.getBoolean(GitCorePreferences.core_packedGitMMAP, d.getBoolean(GitCorePreferences.core_packedGitMMAP, false)));
		c.setDeltaBaseCacheLimit(p.getInt(GitCorePreferences.core_deltaBaseCacheLimit, d.getInt(GitCorePreferences.core_deltaBaseCacheLimit, 0)));
		WindowCache.reconfigure(c);
	}

	private final IProject project;

	private final Collection<RepositoryMapping> mappings = new ArrayList<RepositoryMapping>();

	private final Set<IResource> protectedResources = new HashSet<IResource>();

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
		mappings.addAll(newMappings);
		remapAll();
	}

	/**
	 * Hide our private parts from the navigators other browsers.
	 *
	 * @throws CoreException
	 */
	public void markTeamPrivateResources() throws CoreException {
		for (final Object rmObj : mappings) {
			final RepositoryMapping rm = (RepositoryMapping)rmObj;
			final IContainer c = rm.getContainer();
			if (c == null)
				continue; // Not fully mapped yet?

			final IResource dotGit = c.findMember(Constants.DOT_GIT);
			if (dotGit != null) {
				try {
					final Repository r = rm.getRepository();
					final File dotGitDir = dotGit.getLocation().toFile()
							.getCanonicalFile();
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
	public RepositoryMapping getRepositoryMapping(IResource resource) {
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
				for (final RepositoryMapping repoMapping : mappings) {
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
					mappings.add(new RepositoryMapping(p, key));
				}
			}
		} finally {
			o.close();
		}

		remapAll();
		return this;
	}

	private void remapAll() {
		protectedResources.clear();
		for (final RepositoryMapping repoMapping : mappings) {
			map(repoMapping);
		}
	}

	private void map(final RepositoryMapping m) {
		final IResource r;
		final File git;
		final IResource dotGit;
		IContainer c = null;

		m.clear();
		r = getProject().findMember(m.getContainerPath());
		if (r instanceof IContainer) {
			c = (IContainer) r;
		} else if (r != null) {
			c = (IContainer) r.getAdapter(IContainer.class);
		}

		if (c == null) {
			Activator.logError(CoreText.GitProjectData_mappedResourceGone,
					new FileNotFoundException(m.getContainerPath().toString()));
			m.clear();
			return;
		}
		m.setContainer(c);

		git = c.getLocation().append(m.getGitDirPath()).toFile();
		if (!git.isDirectory()
				|| !new File(git, "config").isFile()) {  //$NON-NLS-1$
			Activator.logError(CoreText.GitProjectData_mappedResourceGone,
					new FileNotFoundException(m.getContainerPath().toString()));
			m.clear();
			return;
		}

		try {
			m.setRepository(Activator.getDefault().getRepositoryCache()
					.lookupRepository(git));
		} catch (IOException ioe) {
			Activator.logError(CoreText.GitProjectData_mappedResourceGone,
					new FileNotFoundException(m.getContainerPath().toString()));
			m.clear();
			return;
		}

		m.fireRepositoryChanged();

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
		if (dotGit != null && dotGit.getLocation().toFile().equals(git)) {
			protect(dotGit);
		}
	}

	private void protect(IResource resource) {
		IResource c = resource;
		while (c != null && !c.equals(getProject())) {
			trace("protect " + c);  //$NON-NLS-1$
			protectedResources.add(c);
			c = c.getParent();
		}
	}
}
