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
	private static class DoubleBool {
		private boolean value;
		public boolean changed;

		DoubleBool (boolean value) {
			this.value = value;
			changed = false;
		}

		public void setValue(boolean value) {
			this.value = value;
			changed = true;
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
		DoubleBool retval = new DoubleBool(false);
		String name = "/" + resource.getName(); //$NON-NLS-1$

		//Continue checking .gitignore in parent containers until we get to project root
		//Quit as soon as retval is changed -- the lower level .gitignores take precedence
		while (!retval.changed && res.getProjectRelativePath() != Path.EMPTY) {
			res = res.getParent();
			System.out.println("Checking file in " + res.getName());
			IFile gitignore = ((IContainer) res).getFile(
					new Path(Constants.GITIGNORE_FILENAME));

			if (res instanceof IContainer)
				isContainer = true;

			if (!gitignore.exists())
				continue;
			retval = searchForExact(name, gitignore, isContainer);
			//To the parent directory, the name of the target resource is /dir/resource
			name = "/" + res.getName() + name; //$NON-NLS-1$
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
	private static DoubleBool searchForExact(String target, final IFile file, boolean isContainer) {
		DoubleBool retval = new DoubleBool(false);

		File f = new File(file.getLocation().toOSString());
		try {
			final BufferedReader br = new BufferedReader(new FileReader(f));
			try	 {
				String patt;
				while ((patt = br.readLine()) != null) {
					String tar = target;

					System.out.println("*********");
					System.out.println("Parsing: " + patt);
					//Special rules: Skip blank lines, skip comments
					if (patt.length() < 1 | patt.charAt(0) == '#')
						continue;


					//Special rules: End with slash -- only match directories
					if (patt.endsWith("/") && !isContainer) { //$NON-NLS-1$
						//TODO: Test
						System.out.println("Ends with '/' and " + target + " is not a container, skipping");
						continue;
					}


					//Special rules: Negation
					if (retval.value) {
						if (!patt.startsWith("!")) {//$NON-NLS-1$
							System.out.println(patt + " is not a negation but a match has already been found, skipping");
							continue;
						}
						else {
							patt = patt.substring(1);
							System.out.println("Truncating to " + patt + " and seeking negations");
						}
					} else {
						if (patt.startsWith("!")) {//$NON-NLS-1$
							System.out.println(patt + " is a negation but no matches have been found. Skipping");
							continue;
						}
					}


					//Special rules: Treat as glob pattern against filename
					if (!patt.contains("/")) {//$NON-NLS-1$
						System.out.println("Pattern does not contain a /, treating as glob against filename");
						tar = tar.substring(tar.lastIndexOf("/"), tar.length());
					}



					//Direct match
					if (patt.equals(tar)) {
						System.out.println("Direct match: " + patt + ", " + tar);
						retval.setValue(retval.value ? false : true);
						continue;
					}


					//This seems to be necessary for compatibility issues
					if (!patt.startsWith("/")) { //$NON-NLS-1$
						System.out.println(patt + " does not start with /, changing to /" + patt );
						patt = "/" + patt; //$NON-NLS-1$
					}


					String temppat = patt.replace(".", "\\."). //$NON-NLS-1$ //$NON-NLS-2$
					replace("*", "[^/]*").replace("?", "[^/]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					//Possible regular expression -- make modifications to match Java regex
					//TODO: Char-by-char replace instead of calling .replace 3 times
					Pattern p = Pattern.compile(temppat);
					System.out.println("Changing to regular expression " + temppat);
					if (p.matcher(tar).matches()) {
						System.out.println("   - matched " + tar + " to regex" );
						retval.setValue(retval.value ? false : true);
					} else
						System.out.println("   - No match for " + tar);
				}
				br.close();
				return retval;
			} catch (IOException e) {
				//May wish to throw an error here
				return retval;
			} finally {
				System.out.println("\n\n\n");
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
