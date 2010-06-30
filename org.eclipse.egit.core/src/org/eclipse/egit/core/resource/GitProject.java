/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.resource;

import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.jgit.lib.FileTreeEntry;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;

/**
 * Representation of project in Git repository.
 */
public class GitProject extends GitContainer implements IProject {

	private final Tree projTree;

	private final IProject iProject;

	/**
	 * Construct GitProject instance
	 *
	 * @param project
	 *            corresponding local project
	 * @param projTree
	 *            Git {@link Tree} that is connected with this project
	 */
	public GitProject(IProject project, Tree projTree) {
		super(project, projTree);

		this.iProject = project;
		this.projTree = projTree;
	}

	public void build(int kind, String builderName, Map args,
			IProgressMonitor monitor) throws CoreException {
		iProject.build(kind, monitor);
	}

	public void build(int kind, IProgressMonitor monitor) throws CoreException {
		iProject.build(kind, monitor);
	}

	public void close(IProgressMonitor monitor) throws CoreException {
		iProject.close(monitor);
	}

	public void create(IProjectDescription description, IProgressMonitor monitor)
			throws CoreException {
		iProject.create(monitor);
	}

	public void create(IProgressMonitor monitor) throws CoreException {
		iProject.create(monitor);
	}

	public void create(IProjectDescription description, int updateFlags,
			IProgressMonitor monitor) throws CoreException {
		iProject.create(monitor);
	}

	public void delete(boolean deleteContent, boolean force,
			IProgressMonitor monitor) throws CoreException {
		// TODO unused
	}

	public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
		return iProject.getContentTypeMatcher();
	}

	public IProjectDescription getDescription() throws CoreException {
		return iProject.getDescription();
	}

	public IFile getFile(String name) {
		TreeEntry member = findBlobMember(projTree, name);
		if (member != null && member instanceof FileTreeEntry)
			return new GitFile(this, (FileTreeEntry) member);

		return null;
	}

	public IFolder getFolder(String name) {
		return super.getFolder(name);
	}

	public IProjectNature getNature(String natureId) throws CoreException {
		return iProject.getNature(natureId);
	}

	public IPath getPluginWorkingLocation(IPluginDescriptor plugin) {
		return iProject.getPluginWorkingLocation(plugin);
	}

	public IPath getWorkingLocation(String id) {
		return iProject.getWorkingLocation(id);
	}

	public IProject[] getReferencedProjects() throws CoreException {
		return iProject.getReferencedProjects();
	}

	public IProject[] getReferencingProjects() {
		return iProject.getReferencingProjects();
	}

	public boolean hasNature(String natureId) throws CoreException {
		return iProject.hasNature(natureId);
	}

	public boolean isNatureEnabled(String natureId) throws CoreException {
		return iProject.isNatureEnabled(natureId);
	}

	public boolean isOpen() {
		return iProject.isOpen();
	}

	public void loadSnapshot(int options, URI snapshotLocation,
			IProgressMonitor monitor) throws CoreException {
		iProject.loadSnapshot(options, snapshotLocation, monitor);
	}

	public void move(IProjectDescription description, boolean force,
			IProgressMonitor monitor) throws CoreException {
		iProject.move(description, force, monitor);
	}

	public void open(int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		iProject.open(updateFlags, monitor);
	}

	public void open(IProgressMonitor monitor) throws CoreException {
		iProject.open(monitor);
	}

	public void saveSnapshot(int options, URI snapshotLocation,
			IProgressMonitor monitor) throws CoreException {
		iProject.saveSnapshot(options, snapshotLocation, monitor);
	}

	public void setDescription(IProjectDescription description,
			IProgressMonitor monitor) throws CoreException {
		iProject.setDescription(description, monitor);
	}

	public void setDescription(IProjectDescription description,
			int updateFlags, IProgressMonitor monitor) throws CoreException {
		iProject.setDescription(description, updateFlags, monitor);
	}

	@Override
	public String getName() {
		return iProject.getName();
	}

	@Override
	public IPath getFullPath() {
		return iProject.getFullPath();
	}

	IProject getEclipseProject() {
		return iProject;
	}

}
