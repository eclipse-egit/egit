/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2013, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jgit.util.FileUtils;
import org.osgi.framework.Bundle;

public class TestProject {
	public IProject project;

	public IJavaProject javaProject;

	private IPackageFragmentRoot sourceFolder;
	private String location;
	private TestUtils testUtils = new TestUtils();

	private final File workspaceSupplement;

	private IFolder binFolder;

	/**
	 * @throws CoreException
	 *             If project already exists
	 */
	public TestProject() throws CoreException {
		this(false);
	}

	public TestProject(boolean remove) throws CoreException {
		this(remove, "Project-1");
	}

	/**
	 * @param remove
	 *            should project be removed if already exists
	 * @param path
	 * @throws CoreException
	 */
	public TestProject(final boolean remove, String path) throws CoreException {
		this(remove, path, true, null);
	}

	/**
	 * @param remove
	 *            should project be removed if already exists
	 * @param path
	 * @param insidews set false to create in temp
	 * @param workspaceSupplement
	 * @throws CoreException
	 */
	public TestProject(final boolean remove, String path, boolean insidews, File workspaceSupplement) throws CoreException {
		this.workspaceSupplement = workspaceSupplement;
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProjectDescription description = createDescription(path, insidews,
				root);
		project = root.getProject(description.getName());
		if (remove) {
			TestUtils.deleteProject(project);
		}
		IPath locationBefore = null;
		URI locationURI = description.getLocationURI();
		if (locationURI != null) {
			locationBefore = URIUtil.toPath(locationURI);
		}
		if (locationBefore == null) {
			locationBefore = root.getRawLocation().append(path);
		}
		location = locationBefore.toOSString();
		project.create(description, null);
		project.open(null);
		javaProject = JavaCore.create(project);
		binFolder = createBinFolder();
		setJavaNature();
		javaProject.setRawClasspath(new IClasspathEntry[0], null);
		createOutputFolder(binFolder);
		addSystemLibraries();
	}

	public void setBinFolderDerived() throws CoreException {
		binFolder.setDerived(true, null);
	}

	public File getWorkspaceSupplement() {
		return workspaceSupplement;
	}

	private IProjectDescription createDescription(String path,
			boolean insidews, IWorkspaceRoot root) {
		Path ppath = new Path(path);
		String projectName = ppath.lastSegment();
		URI locationURI;
		URI top;
		if (insidews) {
			top = root.getRawLocationURI();
		} else {
			top = URIUtil.toURI(workspaceSupplement.getAbsolutePath());
		}
		if (!insidews || !ppath.lastSegment().equals(path)) {
			locationURI = URIUtil.toURI(URIUtil.toPath(top).append(path));
		} else
			locationURI = null;
		IProjectDescription description = ResourcesPlugin.getWorkspace()
				.newProjectDescription(projectName);

		description.setName(projectName);
		description.setLocationURI(locationURI);
		return description;
	}

	public IProject getProject() {
		return project;
	}

	public IJavaProject getJavaProject() {
		return javaProject;
	}

	public void addJar(String plugin, String jar) throws JavaModelException {
		Path result = findFileInPlugin(plugin, jar);
		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaCore.newLibraryEntry(result, null,
				null);
		javaProject.setRawClasspath(newEntries, null);
	}

	public IPackageFragment createPackage(String name) throws CoreException {
		if (sourceFolder == null)
			sourceFolder = createSourceFolder();
		return sourceFolder.createPackageFragment(name, false, null);
	}

	public IType createType(IPackageFragment pack, String cuName, String source)
			throws JavaModelException {
		StringBuilder buf = new StringBuilder();
		buf.append("package " + pack.getElementName() + ";\n");
		buf.append("\n");
		buf.append(source);
		ICompilationUnit cu = pack.createCompilationUnit(cuName,
				buf.toString(), false, null);
		return cu.getTypes()[0];
	}

	public IFile createFile(String name, byte[] content) throws Exception {
		IFile file = project.getFile(name);
		InputStream inputStream = new ByteArrayInputStream(content);
		file.create(inputStream, true, null);

		return file;
	}

	public IFolder createFolder(String name) throws Exception {
		IFolder folder = project.getFolder(name);
		folder.create(true, true, null);
		return folder;
	}

	public IFolder createFolderWithKeep(String name) throws Exception {
		IFolder folder = createFolder(name);

		IFile keep = project.getFile(name + "/keep");
		keep.create(new ByteArrayInputStream(new byte[] {0}), true, null);

		return folder;
	}

	public void dispose() throws CoreException, IOException {
		waitForIndexer();
		try {
			if (project.exists()) {
				TestUtils.deleteProject(project);
			} else {
				File f = new File(location);
				if (f.exists()) {
					FileUtils.delete(f, FileUtils.RECURSIVE | FileUtils.RETRY);
				}
			}
		} catch (CoreException | IOException e) {
			System.err.println(e.toString());
			TestUtils.listDirectory(new File(location), true);
			throw e;
		}
	}

	private IFolder createBinFolder() throws CoreException {
		IFolder folder = project.getFolder("bin");
		folder.create(false, true, null);
		return folder;
	}

	private void setJavaNature() throws CoreException {
		IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		project.setDescription(description, null);
	}

	private void createOutputFolder(IFolder folder)
			throws JavaModelException {
		IPath outputLocation = folder.getFullPath();
		javaProject.setOutputLocation(outputLocation, null);
	}

	public IPackageFragmentRoot createSourceFolder() throws CoreException {
		IFolder folder = project.getFolder("src");
		folder.create(false, true, null);
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(folder);
		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaCore.newSourceEntry(root.getPath());
		javaProject.setRawClasspath(newEntries, null);
		return root;
	}

	private void addSystemLibraries() throws JavaModelException {
		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaRuntime
				.getDefaultJREContainerEntry();
		javaProject.setRawClasspath(newEntries, null);
	}

	private Path findFileInPlugin(String plugin, String file) {
		Bundle bundle = Platform.getBundle(plugin);
		URL resource = bundle.getResource(file);
		return new Path(resource.getPath());
	}

	public void waitForIndexer() {
		//                new SearchEngine().searchAllTypeNames(ResourcesPlugin.getWorkspace(),
		//                                null, null, IJavaSearchConstants.EXACT_MATCH,
		//                                IJavaSearchConstants.CASE_SENSITIVE,
		//                                IJavaSearchConstants.CLASS, SearchEngine
		//                                                .createJavaSearchScope(new IJavaElement[0]),
		//                                new ITypeNameRequestor() {
		//                                        public void acceptClass(char[] packageName,
		//                                                        char[] simpleTypeName, char[][] enclosingTypeNames,
		//                                                        String path) {
		//                                        }
		//                                        public void acceptInterface(char[] packageName,
		//                                                        char[] simpleTypeName, char[][] enclosingTypeNames,
		//                                                        String path) {
		//                                        }
		//                                }, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);
	}

	public String getFileContent(String filepath) throws Exception {
		IFile file = project.getFile(filepath);
		InputStream stream = file.getContents();
		return testUtils.slurpAndClose(stream);
	}
	/**
	 * @return Returns the sourceFolder.
	 */
	public IPackageFragmentRoot getSourceFolder() {
		return sourceFolder;
	}

	/**
	 * @param sourceFolder The sourceFolder to set.
	 */
	public void setSourceFolder(IPackageFragmentRoot sourceFolder) {
		this.sourceFolder = sourceFolder;
	}

	public String getLocation() {
		return location;
	}
}
