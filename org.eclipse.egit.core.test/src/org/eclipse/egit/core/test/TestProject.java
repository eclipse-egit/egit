/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;

public class TestProject {
	public IProject project;

	public IJavaProject javaProject;

	private IPackageFragmentRoot sourceFolder;

	/**
	 * @throws CoreException
	 *             If project already exists
	 */
	public TestProject() throws CoreException {
		this(false);
	}

	/**
	 * @param remove
	 *            should project be removed if already exists
	 * @throws CoreException
	 */
	public TestProject(final boolean remove) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		project = root.getProject("Project-1");
		if (remove)
			project.delete(true, null);
		project.create(null);
		project.open(null);
		javaProject = JavaCore.create(project);
		IFolder binFolder = createBinFolder();
		setJavaNature();
		javaProject.setRawClasspath(new IClasspathEntry[0], null);
		createOutputFolder(binFolder);
		addSystemLibraries();
	}

	public IProject getProject() {
		return project;
	}

	public IJavaProject getJavaProject() {
		return javaProject;
	}

	public void addJar(String plugin, String jar) throws MalformedURLException,
			IOException, JavaModelException {
		Path result = findFileInPlugin(plugin, jar);
		addClassPathEntry(JavaCore.newLibraryEntry(result, null, null));
	}

	public IPackageFragment createPackage(String name) throws CoreException {
		if (sourceFolder == null)
			sourceFolder = createSourceFolder();
		return sourceFolder.createPackageFragment(name, false, null);
	}

	public IType createType(IPackageFragment pack, String cuName, String source)
			throws JavaModelException {
		StringBuffer buf = new StringBuffer();
		buf.append("package " + pack.getElementName() + ";\n");
		buf.append("\n");
		buf.append(source);
		ICompilationUnit cu = pack.createCompilationUnit(cuName,
				buf.toString(), false, null);
		return cu.getTypes()[0];
	}

	public void dispose() throws CoreException {
		waitForIndexer();
		project.delete(true, true, null);
	}

	private IFolder createBinFolder() throws CoreException {
		IFolder binFolder = project.getFolder("bin");
		binFolder.create(false, true, null);
		return binFolder;
	}

	private void setJavaNature() throws CoreException {
		IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		project.setDescription(description, null);
	}

	private void createOutputFolder(IFolder binFolder)
			throws JavaModelException {
		IPath outputLocation = binFolder.getFullPath();
		javaProject.setOutputLocation(outputLocation, null);
	}

	public IPackageFragmentRoot createSourceFolder() throws CoreException {
		IFolder folder = project.getFolder("src");
		folder.create(false, true, null);
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(folder);
		addClassPathEntry(JavaCore.newSourceEntry(root.getPath()));
		return root;
	}

	public void createLinkedSourceFolder(File directory) throws CoreException {
		IFolder folder = project.getFolder("src");
		IPath ipath = new Path(directory.getAbsolutePath());
		if (!project.getWorkspace().validateLinkLocation(folder, ipath).isOK()) {
			throw new CoreException(new Status(IStatus.ERROR, "TestProject",
					"Link location validation failed"));
		}
		folder.createLink(ipath, IResource.NONE, null);
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(folder);
		addClassPathEntry(JavaCore.newSourceEntry(root.getPath()));
	}

	private void addSystemLibraries() throws JavaModelException {
		addClassPathEntry(JavaRuntime.getDefaultJREContainerEntry());
	}

	private void addClassPathEntry(IClasspathEntry entry) throws JavaModelException {
		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = entry;
		javaProject.setRawClasspath(newEntries, null);
	}

	private Path findFileInPlugin(String plugin, String file)
			throws MalformedURLException, IOException {
		IPluginRegistry registry = Platform.getPluginRegistry();
		IPluginDescriptor descriptor = registry.getPluginDescriptor(plugin);
		URL pluginURL = descriptor.getInstallURL();
		URL jarURL = new URL(pluginURL, file);
		URL localJarURL = Platform.asLocalURL(jarURL);
		return new Path(localJarURL.getPath());
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
}
