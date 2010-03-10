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
		 * When locked, the value of IgnoredStatus will not change,
		 * but IgnoredSTatus will still register as having been changed.
		 *
		 * This is so if we encounter a lone !/file.ext in the highest
		 * priority file we will not search any further.
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

			retval = seek(name, gitignore, res instanceof IContainer);
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
	private static IgnoredStatus seek(String target, final IFile file, boolean isContainer) {
		IgnoredStatus ignored = new IgnoredStatus(false);

		File f = new File(file.getLocation().toOSString());
		try {
			final BufferedReader br = new BufferedReader(new FileReader(f));
			try	 {
				String patt;
				while ((patt = br.readLine()) != null) {
					String tar = target;
					boolean nameOnly = false;
					ignored.unlock();

					//Special rules: Skip blank lines, skip comments
					if (patt.length() < 1 | patt.charAt(0) == '#')
						continue;

					//Special rules: Negation
					if (ignored.value) {
						//Already ignored and pattern does not start with '!' -- no changes
						if (!patt.startsWith("!")) {//$NON-NLS-1$
							continue;
						}
						else {
							patt = patt.substring(1);
						}
					} else {
						if (patt.startsWith("!")) {//$NON-NLS-1$
							//Encounter negation but file not ignored -- lock value
							ignored.lock();
							patt = patt.substring(1, patt.length());
						}
					}

					String folder = target.substring(0, tar.lastIndexOf("/")); //$NON-NLS-1$
					//End with "/" -- only match directories
					if (patt.endsWith("/")) { //$NON-NLS-1$
						//Ignore tar
						if (!isContainer) tar = "/"; //$NON-NLS-1$s
						//Remove trailing '/'
						patt = patt.substring(0, patt.length() - 1);
					}


					//No "/": treat as glob pattern against filename
					if (!patt.contains("/")) {//$NON-NLS-1$
						tar = tar.substring(tar.lastIndexOf("/"), tar.length()); //$NON-NLS-1$
						nameOnly = true;
					} else if (patt.equals(tar)) {
						//Direct match only possible if patt starts with /, since target is prefixed
						ignored.setValue(ignored.value ? false : true);
						continue;
					}


					//This is necessary because the target has a leading "/"
					if (!patt.startsWith("/")) { //$NON-NLS-1$
						patt = "/" + patt; //$NON-NLS-1$
					}


					//Possible regular expression -- make modifications to match Java regex
					//TODO: Faster to look char-by-char and use StringBuilder? Irrelevant?
					String temppat = patt.replace(".", "\\."). //$NON-NLS-1$ //$NON-NLS-2$
					replace("*", "[^/]*").replace("?", "[^/]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$


					Pattern p = Pattern.compile(temppat);
					if (p.matcher(tar).matches()) {
						ignored.setValue(ignored.value ? false : true);
						continue;
					}

					if (!nameOnly) {
						//Check for partial path matches
						Pattern p2 = Pattern.compile(temppat + "/.*"); //$NON-NLS-1$
						if (p2.matcher(tar).matches()) {
							ignored.setValue(ignored.value ? false : true);
							continue;
						}
					} else {
						//Check for name-only matches against all parent folders
						//This seems to be what command-line git does
						for (String fol : folder.split("/")) { //$NON-NLS-1$
							if (fol.length() < 1)
								continue;
							//Pad with leading /
							fol = "/" + fol; //$NON-NLS-1$

							//Check folder
							if (p.matcher(fol).matches()) {
								ignored.setValue(ignored.value ? false : true);
								continue;
							}
						}
					}
				}
				br.close();
				return ignored;
			} catch (IOException e) {
				//May wish to throw an error here
				return ignored;
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					//May wish to throw an error here
					return ignored;
				}
			}
		} catch (FileNotFoundException e) {
			return ignored;
		}
	}
}
