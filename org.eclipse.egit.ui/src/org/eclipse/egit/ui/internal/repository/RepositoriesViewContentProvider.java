/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.clone.GitProjectsImportPage;
import org.eclipse.egit.ui.internal.repository.RepositoryTreeNode.RepositoryTreeNodeType;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;

/**
 * Content Provider for the Git Repositories View
 */
public class RepositoriesViewContentProvider implements ITreeContentProvider {

	@SuppressWarnings("unchecked")
	public Object[] getElements(Object inputElement) {

		List<RepositoryTreeNode> nodes = (List<RepositoryTreeNode>) inputElement;
		Collections.sort(nodes);
		return nodes.toArray();
	}

	public void dispose() {
		// nothing
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing
	}

	public Object[] getChildren(Object parentElement) {

		RepositoryTreeNode node = (RepositoryTreeNode) parentElement;
		Repository repo = node.getRepository();

		switch (node.getType()) {

		case BRANCHES: {

			List<RepositoryTreeNode<Repository>> nodes = new ArrayList<RepositoryTreeNode<Repository>>();

			nodes.add(new RepositoryTreeNode<Repository>(node,
					RepositoryTreeNodeType.LOCALBRANCHES, repo, repo));
			nodes.add(new RepositoryTreeNode<Repository>(node,
					RepositoryTreeNodeType.REMOTEBRANCHES, repo, repo));

			return nodes.toArray();
		}

		case LOCALBRANCHES: {
			List<RepositoryTreeNode<Ref>> refs = new ArrayList<RepositoryTreeNode<Ref>>();

			try {
				for (Entry<String, Ref> refEntry : repo.getRefDatabase()
						.getRefs(Constants.R_HEADS).entrySet()) {
					if (!refEntry.getValue().isSymbolic())
						refs.add(new RepositoryTreeNode<Ref>(node,
								RepositoryTreeNodeType.REF, repo, refEntry
										.getValue()));
				}
			} catch (IOException e) {
				handleException(e, node);
			}

			return refs.toArray();
		}

		case REMOTEBRANCHES: {
			List<RepositoryTreeNode<Ref>> refs = new ArrayList<RepositoryTreeNode<Ref>>();

			try {
				for (Entry<String, Ref> refEntry : repo.getRefDatabase()
						.getRefs(Constants.R_REMOTES).entrySet()) {
					if (!refEntry.getValue().isSymbolic())
						refs.add(new RepositoryTreeNode<Ref>(node,
								RepositoryTreeNodeType.REF, repo, refEntry
										.getValue()));
				}
			} catch (IOException e) {
				handleException(e, node);
			}

			return refs.toArray();
		}
		case TAGS: {
			List<RepositoryTreeNode<Ref>> refs = new ArrayList<RepositoryTreeNode<Ref>>();

			try {
				for (Entry<String, Ref> refEntry : repo.getRefDatabase()
						.getRefs(Constants.R_TAGS).entrySet()) {
					refs.add(new RepositoryTreeNode<Ref>(node,
							RepositoryTreeNodeType.TAG, repo, refEntry
									.getValue()));
				}
			} catch (IOException e) {
				handleException(e, node);
			}

			return refs.toArray();
		}

		case SYMBOLICREFS: {
			List<RepositoryTreeNode<Ref>> refs = new ArrayList<RepositoryTreeNode<Ref>>();

			try {
				for (Entry<String, Ref> refEntry : repo.getRefDatabase()
						.getRefs(RefDatabase.ALL).entrySet()) {
					if (refEntry.getValue().isSymbolic())
						refs.add(new RepositoryTreeNode<Ref>(node,
								RepositoryTreeNodeType.SYMBOLICREF, repo,
								refEntry.getValue()));
				}
			} catch (IOException e) {
				handleException(e, node);
			}

			return refs.toArray();
		}

		case REMOTES: {
			List<RepositoryTreeNode<String>> remotes = new ArrayList<RepositoryTreeNode<String>>();

			Repository rep = node.getRepository();

			Set<String> configNames = rep.getConfig().getSubsections(
					RepositoriesView.REMOTE);

			for (String configName : configNames) {
				remotes.add(new RepositoryTreeNode<String>(node,
						RepositoryTreeNodeType.REMOTE, repo, configName));
			}

			return remotes.toArray();
		}

		case REPO: {

			List<RepositoryTreeNode<? extends Object>> nodeList = new ArrayList<RepositoryTreeNode<? extends Object>>();

			nodeList.add(new RepositoryTreeNode<Repository>(node,
					RepositoryTreeNodeType.BRANCHES, node.getRepository(), node
							.getRepository()));

			nodeList.add(new RepositoryTreeNode<Repository>(node,
					RepositoryTreeNodeType.TAGS, repo, repo));

			nodeList.add(new RepositoryTreeNode<Repository>(node,
					RepositoryTreeNodeType.SYMBOLICREFS, repo, repo));

			nodeList.add(new RepositoryTreeNode<Repository>(node,
					RepositoryTreeNodeType.WORKINGDIR, node.getRepository(),
					node.getRepository()));

			nodeList.add(new RepositoryTreeNode<Repository>(node,
					RepositoryTreeNodeType.PROJECTS, node.getRepository(), node
							.getRepository()));

			nodeList.add(new RepositoryTreeNode<Repository>(node,
					RepositoryTreeNodeType.REMOTES, node.getRepository(), node
							.getRepository()));

			return nodeList.toArray();
		}

		case PROJECTS: {
			List<RepositoryTreeNode<File>> projects = new ArrayList<RepositoryTreeNode<File>>();

			// TODO do we want to show the projects here?
			Collection<File> result = new HashSet<File>();
			Set<String> traversed = new HashSet<String>();
			collectProjectFilesFromDirectory(result, repo.getDirectory()
					.getParentFile(), traversed, new NullProgressMonitor());
			for (File file : result) {
				projects.add(new RepositoryTreeNode<File>(node,
						RepositoryTreeNodeType.PROJ, repo, file));
			}

			Comparator<RepositoryTreeNode<File>> sorter = new Comparator<RepositoryTreeNode<File>>() {

				public int compare(RepositoryTreeNode<File> o1,
						RepositoryTreeNode<File> o2) {
					return o1.getObject().getName().compareTo(
							o2.getObject().getName());
				}
			};
			Collections.sort(projects, sorter);

			return projects.toArray();
		}

		case WORKINGDIR: {
			List<RepositoryTreeNode<File>> children = new ArrayList<RepositoryTreeNode<File>>();

			File workingDir = repo.getWorkDir();
			if (workingDir == null || !workingDir.exists())
				return null;

			File[] childFiles = workingDir.listFiles();
			Arrays.sort(childFiles, new Comparator<File>() {
				public int compare(File o1, File o2) {
					if (o1.isDirectory()) {
						if (o2.isDirectory()) {
							return o1.compareTo(o2);
						}
						return -1;
					} else if (o2.isDirectory()) {
						return 1;
					}
					return o1.compareTo(o2);
				}
			});
			for (File file : childFiles) {
				if (file.isDirectory()) {
					children.add(new RepositoryTreeNode<File>(node,
							RepositoryTreeNodeType.FOLDER, repo, file));
				} else {
					children.add(new RepositoryTreeNode<File>(node,
							RepositoryTreeNodeType.FILE, repo, file));
				}
			}

			return children.toArray();
		}

		case FOLDER: {
			List<RepositoryTreeNode<File>> children = new ArrayList<RepositoryTreeNode<File>>();

			File parent = ((File) node.getObject());

			File[] childFiles = parent.listFiles();
			Arrays.sort(childFiles, new Comparator<File>() {
				public int compare(File o1, File o2) {
					if (o1.isDirectory()) {
						if (o2.isDirectory()) {
							return o1.compareTo(o2);
						}
						return -1;
					} else if (o2.isDirectory()) {
						return 1;
					}
					return o1.compareTo(o2);
				}
			});
			for (File file : childFiles) {
				if (file.isDirectory()) {
					children.add(new RepositoryTreeNode<File>(node,
							RepositoryTreeNodeType.FOLDER, repo, file));
				} else {
					children.add(new RepositoryTreeNode<File>(node,
							RepositoryTreeNodeType.FILE, repo, file));
				}
			}

			return children.toArray();
		}

		case REMOTE: {

			List<RepositoryTreeNode<String>> children = new ArrayList<RepositoryTreeNode<String>>();

			String remoteName = (String) node.getObject();
			RemoteConfig rc;
			try {
				rc = new RemoteConfig(node.getRepository().getConfig(),
						remoteName);
			} catch (URISyntaxException e) {
				handleException(e, node);
				return children.toArray();
			}

			if (!rc.getURIs().isEmpty())
				children.add(new RepositoryTreeNode<String>(node,
						RepositoryTreeNodeType.FETCH, node.getRepository(), rc
								.getURIs().get(0).toPrivateString()));

			if (!rc.getPushURIs().isEmpty())
				if (rc.getPushURIs().size() == 1)
					children.add(new RepositoryTreeNode<String>(node,
							RepositoryTreeNodeType.PUSH, node.getRepository(),
							rc.getPushURIs().get(0).toPrivateString()));
				else
					children.add(new RepositoryTreeNode<String>(node,
							RepositoryTreeNodeType.PUSH, node.getRepository(),
							rc.getPushURIs().get(0).toPrivateString() + "...")); //$NON-NLS-1$

			return children.toArray();

		}

		case FILE:
			// fall through
		case REF:
			// fall through
		case PUSH:
			// fall through
		case PROJ:
			// fall through
		case HEAD:
			// fall through
		case TAG:
			// fall through
		case FETCH:
			// fall through
		case ERROR:
			// fall through
		case SYMBOLICREF:
			return null;

		}

		return null;

	}

	private void handleException(Exception e, RepositoryTreeNode parentNode) {
		Activator.handleError(e.getMessage(), e, false);
		// add a node indicating that there was an Exception
		new RepositoryTreeNode<String>(parentNode,
				RepositoryTreeNodeType.ERROR, parentNode.getRepository(),
				UIText.RepositoriesViewContentProvider_ExceptionNodeText);
	}

	public Object getParent(Object element) {

		return ((RepositoryTreeNode) element).getParent();
	}

	public boolean hasChildren(Object element) {
		Object[] children = getChildren(element);
		return children != null && children.length > 0;
	}

	private boolean collectProjectFilesFromDirectory(Collection<File> files,
			File directory, Set<String> directoriesVisited,
			IProgressMonitor monitor) {

		// stolen from the GitCloneWizard; perhaps we should completely drop
		// the projects from this view, though
		if (monitor.isCanceled()) {
			return false;
		}
		monitor.subTask(NLS.bind(UIText.RepositoriesView_Checking_Message,
				directory.getPath()));
		File[] contents = directory.listFiles();
		if (contents == null)
			return false;

		// first look for project description files
		final String dotProject = IProjectDescription.DESCRIPTION_FILE_NAME;
		for (int i = 0; i < contents.length; i++) {
			File file = contents[i];
			if (file.isFile() && file.getName().equals(dotProject)) {
				files.add(file.getParentFile());
				// don't search sub-directories since we can't have nested
				// projects
				return true;
			}
		}
		// no project description found, so recurse into sub-directories
		for (int i = 0; i < contents.length; i++) {
			if (contents[i].isDirectory()) {
				if (!contents[i].getName().equals(
						GitProjectsImportPage.METADATA_FOLDER)) {
					try {
						String canonicalPath = contents[i].getCanonicalPath();
						if (!directoriesVisited.add(canonicalPath)) {
							// already been here --> do not recurse
							continue;
						}
					} catch (IOException e) {
						Activator.handleError(e.getMessage(), e, false);
					}
					collectProjectFilesFromDirectory(files, contents[i],
							directoriesVisited, monitor);
				}
			}
		}
		return true;
	}

}
