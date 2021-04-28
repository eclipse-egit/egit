/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.efs;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.Activator;

/**
 * Manages a hidden project containing {@link IFile}s linked to EFS URIs.
 */
public enum HiddenResources {

	// Based on JDT's ExternalFolderManager and on
	// CompareWithOtherResourceDialog from Compare UI.

	/**
	 * The singleton instance.
	 */
	INSTANCE;

	private static final String PROJECT_NAME = ".org.eclipse.egit.core.cmp"; //$NON-NLS-1$

	private static final String SRC_FOLDER_PREFIX = "src"; //$NON-NLS-1$

	private final static String PROJECT_FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //$NON-NLS-1$
			+ "<projectDescription>\n" //$NON-NLS-1$
			+ "\t<name>" + PROJECT_NAME + "</name>\n" //$NON-NLS-1$ //$NON-NLS-2$
			+ "\t<comment></comment>\n" //$NON-NLS-1$
			+ "\t<projects>\n" //$NON-NLS-1$
			+ "\t</projects>\n" //$NON-NLS-1$
			+ "\t<buildSpec>\n" //$NON-NLS-1$
			+ "\t</buildSpec>\n" //$NON-NLS-1$
			+ "\t<natures>\n" //$NON-NLS-1$
			+ "\t</natures>\n" //$NON-NLS-1$
			+ "</projectDescription>"; //$NON-NLS-1$

	private boolean initialized;

	/**
	 * Create new linked {@link IFile} in a hidden project with the given uri
	 * and encoding.
	 *
	 * @param uri
	 *            to link to
	 * @param name
	 *            for the new file
	 * @param encoding
	 *            to use for the file
	 * @param monitor
	 *            for progress reporting
	 * @return the new {@link IFile}
	 * @throws CoreException
	 *             if the file could not be created
	 */
	public IFile createFile(URI uri, String name, Charset encoding,
			IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 3);
		IProject project = getHiddenProject(progress.newChild(1));
		initialize(progress.newChild(1));
		IResource[] children = project.members();
		progress.setWorkRemaining(children.length + 2);
		for (IResource rsc : children) {
			if (rsc.getType() == IResource.FOLDER) {
				try {
					return linkFile((IFolder) rsc, uri, name, encoding,
							progress.newChild(1));
				} catch (CoreException e) {
					// Swallow here; try the next folder
				}
			} else {
				progress.worked(1);
			}
		}
		IFolder newFolder = createFolder(project, children.length,
				progress.newChild(1));
		return linkFile(newFolder, uri, name, encoding, progress.newChild(1));
	}

	/**
	 * Initializes the hidden project.
	 *
	 * @param monitor
	 *            for progress reporting and cancellation
	 */
	public void initialize(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		try {
			IProject project = getHiddenProject(progress.newChild(1));
			initialize(project, progress.newChild(1));
		} catch (CoreException e) {
			Activator.logWarning("Cannot clean up internal hidden project", e); //$NON-NLS-1$
		}
	}

	private synchronized void initialize(IProject project,
			IProgressMonitor monitor) {
		if (initialized) {
			return;
		}
		initialized = true;
		// Clean out all existing linked resources. There are internal methods
		// that might simplify this, but they're not accessible.
		IWorkspaceRunnable clean = m -> {
			IResource[] resources = project.members();
			SubMonitor progress = SubMonitor.convert(m, resources.length);
			for (IResource rsc : project.members()) {
				if (rsc.getType() == IResource.FOLDER) {
					IResource[] children = ((IFolder) rsc).members();
					SubMonitor sub = SubMonitor.convert(progress.newChild(1),
							children.length);
					for (IResource f : children) {
						if (f.isLinked()) {
							try {
								f.delete(true, sub.newChild(1));
							} catch (CoreException e) {
								Activator.logWarning(MessageFormat.format(
										"Cannot clean up internal hidden resource {}", //$NON-NLS-1$
										f), e);
							}
						}
						if (sub.isCanceled()) {
							return;
						}
					}
					children = ((IFolder) rsc).members();
					if (children.length == 0) {
						try {
							rsc.delete(true, null);
						} catch (CoreException e) {
							Activator.logWarning(MessageFormat.format(
									"Cannot clean up internal hidden folder {}", //$NON-NLS-1$
									rsc), e);
						}
					}
				} else {
					progress.worked(1);
				}
				if (progress.isCanceled()) {
					return;
				}
			}
		};
		try {
			project.getWorkspace().run(clean, null, IWorkspace.AVOID_UPDATE,
					monitor);
		} catch (CoreException e) {
			Activator.logWarning(MessageFormat.format(
					"Cannot clean up internal hidden project {}", project), e); //$NON-NLS-1$
		}
	}

	/**
	 * Determines whether the {@link IResource} is the hidden project.
	 *
	 * @param resource
	 *            to test
	 * @return {@code true} if the resource is the hidden project, {@code false}
	 *         otherwise
	 */
	public boolean isHiddenProject(IResource resource) {
		if (resource.getType() != IResource.PROJECT) {
			return false;
		}
		return PROJECT_NAME.equals(resource.getName());
	}

	private IProject getHiddenProject(IProgressMonitor monitor) throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		if (!project.isAccessible()) {
			SubMonitor progress = SubMonitor.convert(monitor, 2);
			if (!project.exists()) {
				createProject(project, progress.newChild(1));
			}
			progress.setWorkRemaining(1);
			openProject(project, progress.newChild(1));
		}
		return project;
	}

	private void createProject(IProject project, IProgressMonitor monitor)
			throws CoreException {
		IProjectDescription desc = project.getWorkspace()
				.newProjectDescription(project.getName());
		IPath stateLocation = Activator.getDefault().getStateLocation();
		desc.setLocation(stateLocation.append(PROJECT_NAME));
		project.create(desc, IResource.HIDDEN, monitor);
	}

	private void openProject(IProject project, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		try {
			project.open(progress.newChild(1));
		} catch (CoreException e) {
			progress.setWorkRemaining(3);
			if (e.getStatus()
					.getCode() == IResourceStatus.FAILED_READ_METADATA) {
				// Workspace moved? Re-create.
				project.delete(false, true, progress.newChild(1));
				createProject(project, progress.newChild(1));
			} else {
				// .project or folder on disk have been deleted, recreate them
				IPath stateLocation = Activator.getDefault().getStateLocation();
				IPath projectPath = stateLocation.append(PROJECT_NAME);
				projectPath.toFile().mkdirs();
				try {
					Files.write(
							projectPath.append(
									IProjectDescription.DESCRIPTION_FILE_NAME)
									.toFile().toPath(),
							PROJECT_FILE.getBytes());
					progress.worked(2);
				} catch (IOException ioe) {
					// Re-create from scratch
					project.delete(true, true, progress.newChild(1));
					createProject(project, progress.newChild(1));
				}
			}
			project.open(progress.newChild(1));
		}
	}

	private IFolder createFolder(IProject project, int n,
			IProgressMonitor monitor) throws CoreException {
		IFolder folder = project.getFolder(SRC_FOLDER_PREFIX + n);
		folder.create(IResource.NONE, true, monitor);
		return folder;
	}

	private IFile linkFile(IFolder folder, URI uri, String name,
			Charset encoding, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		IFile file = folder.getFile(name);
		file.createLink(uri, IResource.NONE, progress.newChild(1));
		if (encoding != null) {
			file.setCharset(encoding.name(), progress.newChild(1));
		}
		return file;
	}
}
