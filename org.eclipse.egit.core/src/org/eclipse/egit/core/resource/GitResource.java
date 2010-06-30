/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.resource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;

/**
 * Representation of resource in Git repository
 */
public abstract class GitResource extends PlatformObject implements IResource {

	private final IProject project;

	private final TreeEntry treeEntry;

	/**
	 * Parent of this resource
	 */
	protected final IContainer parent;

	GitResource(IContainer parent, TreeEntry treeEntry) {
		this.parent = parent;
		this.treeEntry = treeEntry;
		IProject project = parent.getProject();
		if (project instanceof GitProject)
			this.project = ((GitProject) project).getEclipseProject();
		else
			this.project = parent.getProject();
	}

	public boolean contains(ISchedulingRule rule) {
		// unused
		return false;
	}

	public boolean isConflicting(ISchedulingRule rule) {
		// unused
		return false;
	}

	public void accept(IResourceProxyVisitor visitor, int memberFlags)
			throws CoreException {
		// unused
	}

	public void accept(IResourceVisitor visitor) throws CoreException {
		// unused
	}

	public void accept(IResourceVisitor visitor, int depth,
			boolean includePhantoms) throws CoreException {
		// unused
	}

	public void accept(IResourceVisitor visitor, int depth, int memberFlags)
			throws CoreException {
		// unused
	}

	public void clearHistory(IProgressMonitor monitor) throws CoreException {
		// unused
	}

	public void copy(IPath destination, boolean force, IProgressMonitor monitor)
			throws CoreException {
		// unused
	}

	public void copy(IPath destination, int updateFlags,
			IProgressMonitor monitor) throws CoreException {
		// unused
	}

	public void copy(IProjectDescription description, boolean force,
			IProgressMonitor monitor) throws CoreException {
		// unused
	}

	public void copy(IProjectDescription description, int updateFlags,
			IProgressMonitor monitor) throws CoreException {
		// unused
	}

	public IMarker createMarker(String type) throws CoreException {
		// unused
		return null;
	}

	public IResourceProxy createProxy() {
		// unused
		return null;
	}

	public void delete(boolean force, IProgressMonitor monitor)
			throws CoreException {
		// unused

	}

	public void delete(int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		// unused

	}

	public void deleteMarkers(String type, boolean includeSubtypes, int depth)
			throws CoreException {
		// unused

	}

	public boolean exists() {
		return treeEntry != null;
	}

	public IMarker findMarker(long id) throws CoreException {
		// unused
		return null;
	}

	public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth)
			throws CoreException {
		return project.findMarkers(type, includeSubtypes, depth);
	}

	public int findMaxProblemSeverity(String type, boolean includeSubtypes,
			int depth) throws CoreException {
		// unused
		return 0;
	}

	public String getFileExtension() {
		if (getType() != IResource.FOLDER && treeEntry != null
				&& treeEntry.getName().contains(".")) { //$NON-NLS-1$
			String name = treeEntry.getName();
			return name.substring(name.lastIndexOf('.') + 1);
		}

		return null;
	}

	public IPath getFullPath() {
		if (treeEntry == null) {
			File repoWorkDir = RepositoryMapping.getMapping(project)
					.getWorkDir();
			return new Path(repoWorkDir.toString());
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		Repository repo = treeEntry.getRepository();
		IPath absolutePath = new Path(repo.getWorkDir().getAbsolutePath())
				.append(treeEntry.getFullName());
		IResource resource = root.getFileForLocation(absolutePath);

		return resource.getFullPath();
	}

	public long getLocalTimeStamp() {
		// TODO implement
		return 0;
	}

	public IPath getLocation() {
		if (treeEntry == null)
			return null;

		String repoDir = treeEntry.getRepository().getWorkDir().toString();
		Path path = new Path(repoDir + "/", treeEntry.getFullName()); //$NON-NLS-1$
		return path;
	}

	public URI getLocationURI() {
		// unused
		return null;
	}

	public IMarker getMarker(long id) {
		// unused
		return null;
	}

	public long getModificationStamp() {
		// unused
		return 0;
	}

	public String getName() {
		return treeEntry.getName();
	}

	public IPathVariableManager getPathVariableManager() {
		// unused
		return null;
	}

	public IContainer getParent() {
		return parent;
	}

	public Map getPersistentProperties() throws CoreException {
		// unused
		return null;
	}

	public String getPersistentProperty(QualifiedName key) throws CoreException {
		// unused
		return null;
	}

	public IProject getProject() {
		return parent.getProject();
	}

	public IPath getProjectRelativePath() {
		if (treeEntry == null)
			return null;

		String path = treeEntry.getFullName().replace(project.getName(), ""); //$NON-NLS-1$
		return new Path(path);
	}

	/**
	 * @return content identifier of this resource. This is simply SHA-1 of tree
	 *         entry connected with this resource
	 */
	public String getContentIdentifier() {
		return treeEntry.getId().getName();
	}

	public IPath getRawLocation() {
		// unused
		return null;
	}

	public URI getRawLocationURI() {
		// unused
		return null;
	}

	public ResourceAttributes getResourceAttributes() {
		// unused
		return null;
	}

	public Map getSessionProperties() throws CoreException {
		return project.getSessionProperties();
	}

	public Object getSessionProperty(QualifiedName key) throws CoreException {
		return project.getSessionProperty(key);
	}

	public IWorkspace getWorkspace() {
		return project.getWorkspace();
	}

	public boolean isAccessible() {
		return exists();
	}

	public boolean isDerived() {
		return false;
	}

	public boolean isDerived(int options) {
		return isDerived();
	}

	public boolean isHidden() {
		// unused
		return false;
	}

	public boolean isHidden(int options) {
		// unused
		return false;
	}

	public boolean isLinked() {
		// unused
		return false;
	}

	public boolean isVirtual() {
		// unused
		return false;
	}

	public boolean isLinked(int options) {
		// unused
		return false;
	}

	public boolean isLocal(int depth) {
		// unused
		return false;
	}

	public boolean isPhantom() {
		// TODO implement
		return false;
	}

	public boolean isReadOnly() {
		// unused
		return false;
	}

	public boolean isSynchronized(int depth) {
		// unused
		return false;
	}

	public boolean isTeamPrivateMember() {
		// unused
		return false;
	}

	public boolean isTeamPrivateMember(int options) {
		// unused
		return false;
	}

	public void move(IPath destination, boolean force, IProgressMonitor monitor)
			throws CoreException {
		// unused

	}

	public void move(IPath destination, int updateFlags,
			IProgressMonitor monitor) throws CoreException {
		// unused

	}

	public void move(IProjectDescription description, boolean force,
			boolean keepHistory, IProgressMonitor monitor) throws CoreException {
		// unused

	}

	public void move(IProjectDescription description, int updateFlags,
			IProgressMonitor monitor) throws CoreException {
		// unused

	}

	public void refreshLocal(int depth, IProgressMonitor monitor)
			throws CoreException {
		// unused

	}

	public void revertModificationStamp(long value) throws CoreException {
		// unused

	}

	public void setDerived(boolean isDerived) throws CoreException {
		// unused

	}

	public void setDerived(boolean isDerived, IProgressMonitor monitor)
			throws CoreException {
		// unused

	}

	public void setHidden(boolean isHidden) throws CoreException {
		// unused

	}

	public void setLocal(boolean flag, int depth, IProgressMonitor monitor)
			throws CoreException {
		// unused

	}

	public long setLocalTimeStamp(long value) throws CoreException {
		// unused
		return 0;
	}

	public void setPersistentProperty(QualifiedName key, String value)
			throws CoreException {
		// unused

	}

	public void setReadOnly(boolean readOnly) {
		// unused

	}

	public void setResourceAttributes(ResourceAttributes attributes)
			throws CoreException {
		// unused

	}

	public void setSessionProperty(QualifiedName key, Object value)
			throws CoreException {
		// unused

	}

	public void setTeamPrivateMember(boolean isTeamPrivate)
			throws CoreException {
		// unused

	}

	public void touch(IProgressMonitor monitor) throws CoreException {
		// unused
	}

	@Override
	public String toString() {
		return getTypeString() + getFullPath().toString();
	}

	/**
	 *
	 * @param tree
	 * @param name
	 * @return {@link TreeEntry} of given tree and name or <code>null</code> if
	 *         given tree does not contains blob with given name
	 */
	protected TreeEntry findBlobMember(Tree tree, String name) {
		try {
			return tree.findBlobMember(name);
		} catch (IOException e) {
			// TODO log exception
			return null;
		}
	}

	/**
	 *
	 * @param tree
	 * @param name
	 * @return {@link TreeEntry} for given tree and name or <code>null</code> if
	 *         given tree does not contains tree with given name
	 */
	protected TreeEntry findTreeMember(Tree tree, String name) {
		try {
			return tree.findTreeMember(name);
		} catch (IOException e) {
			// TODO log exception
			return null;
		}
	}

	private String getTypeString() {
		switch (getType()) {
		case FILE:
			return "L"; //$NON-NLS-1$
		case FOLDER:
			return "F"; //$NON-NLS-1$
		case PROJECT:
			return "P"; //$NON-NLS-1$
		case ROOT:
			return "R"; //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

}
