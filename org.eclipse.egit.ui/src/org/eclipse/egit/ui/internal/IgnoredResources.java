/*******************************************************************************
 * Copyright (C) 2009, Charley Wang <charley.wang@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.team.core.Team;

/**
 * Contains methods for checking if a given resource is ignored
 */
public class IgnoredResources {

	/**
	 * Private class used to store the IgnoredStatus of the target
	 *
	 */
	private static class IgnoredStatus {
		private boolean value;
		public boolean changed;
		private boolean locked;

		IgnoredStatus (boolean value) {
			this.value = value;
			changed = false;
			locked = false;
		}

		public void setValue(boolean value) {
			if (!locked)
				this.value = value;
			changed = true;
		}

		/**
		 * If we encounter a !/file.x without encountering a /file.x, we
		 * want to ignore any changes to the value, but still keep any
		 * changes to 'change'
		 *
		 */
		public void lock() {
			locked = true;
		}

		public void unlock() {
			locked = false;
		}
	}


	/**
	 *	Same as isIgnored, but without a check in the exclude file
	 *
	 * @param resource
	 * @return true if resource is ignored by Team or .gitignore
	 */
	public static boolean isGitIgnored(IResource resource) {
		return (Team.isIgnoredHint(resource) || checkGitIgnore(resource));
	}



	private static boolean checkGitIgnore(IResource resource) {
		IResource res = resource;
		boolean isContainer = false;
		IgnoredStatus retval = new IgnoredStatus(false);
		String name = ""; //$NON-NLS-1$

		/*
		 * Loop through directories until the IgnoredStatus gets changed
		 * or until we hit project root.
		 */
		while (!retval.changed && res.getProjectRelativePath() != Path.EMPTY) {
			//Next name to seek is /parent/dirs/resource
			name = "/" + res.getName() + name; //$NON-NLS-1$

			res = res.getParent();
			IFile gitignore = ((IContainer) res).getFile(
					new Path(Constants.GITIGNORE_FILENAME));
			if (!gitignore.exists())
				continue;

			if (res instanceof IContainer)
				isContainer = true;
			retval = searchForExact(name, gitignore, isContainer);
		}
		return retval.value;
	}



	/**
	 * Searches the file for the target string. Searches for the exact target in the file,
	 * does not consider the fact that entries in file may be regular expressions.
	 *
	 * @param target
	 * @param file
	 * @param isContainer
	 * @return true if target was found in file
	 */
	private static IgnoredStatus searchForExact(String target, final IFile file, boolean isContainer) {
		IgnoredStatus retval = new IgnoredStatus(false);

		File f = new File(file.getLocation().toOSString());
		try {
			final BufferedReader br = new BufferedReader(new FileReader(f));
			try	 {
				String patt;
				while ((patt = br.readLine()) != null) {
					retval.unlock();
					String tar = target;

					//Special rules: Skip blank lines, skip comments
					if (patt.length() < 1 | patt.charAt(0) == '#')
						continue;


					//Special rules: End with slash -- only match directories
					if (patt.endsWith("/")) { //$NON-NLS-1$
						if (!isContainer) {
							continue;
						} else {
							//Remove trailing '/', since target will not have that
							patt = patt.substring(0, patt.length() - 1);
						}
					}


					//Special rules: Negation
					if (retval.value) {
						if (!patt.startsWith("!")) {//$NON-NLS-1$
							continue;
						}
						else {
							patt = patt.substring(1);
						}
					} else {
						if (patt.startsWith("!")) {//$NON-NLS-1$
							//If patt matches, we want to set retval to changed, but not toggle value
							retval.lock();
						}
					}


					//Special rules: Treat as glob pattern against filename
					if (!patt.contains("/")) {//$NON-NLS-1$
						tar = tar.substring(tar.lastIndexOf("/"), tar.length()); //$NON-NLS-1$
					} else if (patt.equals(tar)) {
						//Direct match only possible if patt starts with /, since target is prefixed
						retval.setValue(retval.value ? false : true);
						continue;
					}


					//This is necessary because the target has a leading "/"
					if (!patt.startsWith("/")) { //$NON-NLS-1$
						patt = "/" + patt; //$NON-NLS-1$
					}


					//Compile temporary pattern
					//TODO: Faster to look char-by-char and use StringBuilder? Irrelevant?
					String temppat = patt.replace(".", "\\."). //$NON-NLS-1$ //$NON-NLS-2$
					replace("*", "[^/]*").replace("?", "[^/]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

					//Possible regular expression -- make modifications to match Java regex
					Pattern p = Pattern.compile(temppat);
					if (p.matcher(tar).matches()) {
						retval.setValue(retval.value ? false : true);
					}
				}
				br.close();
				return retval;
			} catch (IOException e) {
				//May wish to throw an error here
				return retval;
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					//May wish to throw an error here
					return retval;
				}
			}
		} catch (FileNotFoundException e) {
			return retval;
		}
	}
}
