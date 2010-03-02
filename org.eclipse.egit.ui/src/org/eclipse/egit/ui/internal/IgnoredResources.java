package org.eclipse.egit.ui.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.team.core.Team;

/**
 * Contains methods for checking if a given resource is ignored
 *
 * @author Charley Wang
 *
 */
public class IgnoredResources {


	/**
	 *	Resource is considered ignored it appears in any of the three checked locations
	 *
	 * @param resource
	 * @return true if resource is ignored
	 */
	public static boolean isIgnored(IResource resource) {
		return (Team.isIgnoredHint(resource) || checkGitIgnore(resource) || checkGitExclude(resource)) ;

	}

	private static boolean checkGitIgnore(IResource resource) {
		IContainer container = resource.getParent();
		IFile gitignore = container.getFile(new Path(Constants.GITIGNORE_FILENAME));

		return searchForExact("/" + resource.getName(), gitignore); //$NON-NLS-1$
	}

	private static boolean checkGitExclude(IResource resource) {
		IProject project = resource.getProject();
		//Is there any other way to get the exclude file? I can't find any references
		IFile exclude = project.getFile(".git/info/exclude"); //$NON-NLS-1$

		return searchForExact("/" + resource.getName(), exclude); //$NON-NLS-1$
	}

	/**
	 * Searches the file for the target string. Searches for the exact target in the file,
	 * does not consider the fact that entries in file may be regular expressions.
	 *
	 * @param target
	 * @param file
	 * @return true if target was found in file
	 */
	private static boolean searchForExact(String target, final IFile file) {

		/*Unfortunately calling file.refreshLocal doesn't seem to work properly, so the only workaround
		* seems to be to use the underlying file
		* Possible problems:
		*  - Changes to the resource not reflected in the actual file
		*  - File does not exist (not a problem, just catch the error and return false)
		*/
			File f = new File(file.getLocation().toOSString());
			try {
				final BufferedReader br = new BufferedReader(new FileReader(f));
				try	 {
					String patt;
					while ((patt = br.readLine()) != null) {
						if (patt.length() < 1 | patt.charAt(0) == '#')
							continue;

						if (patt.equals(target))
							return true;
						//Possible regular expression -- make modifications to match Java regex
						Pattern p = Pattern.compile(patt.replace(".", "\\."). //$NON-NLS-1$ //$NON-NLS-2$
								replace("*", ".*")); //$NON-NLS-1$ //$NON-NLS-2$
						if (p.matcher(target).matches()) {
							return true;
						}
					}
					br.close();
				} catch (IOException e) {
					return false;
				} finally {
					try {
						br.close();
					} catch (IOException e) {
						return false;
					}
				}
			} catch (FileNotFoundException e) {
				return false;
			}
		return false;
	}
}
