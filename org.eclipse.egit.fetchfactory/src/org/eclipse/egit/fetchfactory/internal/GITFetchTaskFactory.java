/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation of CVS factory
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.fetchfactory.internal;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.*;
import org.eclipse.pde.internal.build.*;

/**
 * An <code>FetchTaskFactory</code> for building fetch scripts that will
 * fetch content from a Git repository (id: <code>GIT</code>).
 * <p>
 * Map file format:
 * <code><pre>
 * 	type@id,[version]=GIT,args
 * </pre></code>
 * <code>args</code> is a comma-separated list of key/value pairs
 * Accepted args include:
 * <ul>
 * <li><code>tag</code> - mandatory Git tag</li>
 * <li><code>repo</code> - mandatory repo location</li>
 * <li><code>path</code> - optional path relative to <code>repo</code> which points to the element
 * (otherwise it's assumed that the element is at the repo root)</li>
 * <li><code>prebuilt</code> - optional boolean value indicating that the path points to
 * a pre-built bundle in the repository</li>
 * </ul>
 * </p>
 * <p>
 * Fetching is implemented as a three-step process.
 * <ol>
 * <li>The repository is cloned to local disc. If it already exists, it is assumed that it was previously cloned
 * and just new commits will be fetched.</li>
 * <li>The specified tag will be checked out in the local clone.</li>
 * <li>The content of the path will be copied to the final build location.</li>
 * </ol>
 * </p>
 */
@SuppressWarnings("restriction")
public class GITFetchTaskFactory implements IFetchFactory {


	public static final String ID = "GIT"; //$NON-NLS-1$
	public static final String OVERRIDE_TAG = ID;

	private static final String TARGET_GET_ELEMENT_FROM_REPO = "GitFetchElementFromLocalRepo"; //$NON-NLS-1$
	private static final String TARGET_GET_FILE_FROM_REPO = "GitFetchFileFromLocalRepo"; //$NON-NLS-1$
	private static final String TARGET_CLONE_REPO = "GitCloneRepoToLocalRepo"; //$NON-NLS-1$
	private static final String TARGET_UPDATE_REPO = "GitUpdateLocalRepo"; //$NON-NLS-1$
	private static final String TARGET_CHECKOUT_TAG = "GitCheckoutTagInLocalRepo"; //$NON-NLS-1$
	private static final String SEPARATOR = ","; //$NON-NLS-1$

	// Git specific keys used in the map being passed around.
	private static final String KEY_REPO = "repo"; //$NON-NLS-1$
	private static final String KEY_PATH = "path"; //$NON-NLS-1$
	private static final String KEY_PREBUILT = "prebuilt"; //$NON-NLS-1$

	//Properties used in the Git part of the scripts
	private static final String PROP_DESTINATIONFOLDER = "destinationFolder"; //$NON-NLS-1$
	private static final String PROP_GITREPO = "gitRepo"; //$NON-NLS-1$
	private static final String PROP_GITREPO_LOCAL_PATH = "gitRepoLocalPath"; //$NON-NLS-1$
	private static final String PROP_PATH = "path"; //$NON-NLS-1$
	private static final String PROP_FILE = "file"; //$NON-NLS-1$
	private static final String PROP_TAG = "tag"; //$NON-NLS-1$
	private static final String PROP_FILETOCHECK = "fileToCheck"; //$NON-NLS-1$

	// copied from FetchScriptGenerator to be independent from changes there
	public static String PROP_FETCH_CACHE_LOCATION = "fetchCacheLocation"; //$NON-NLS-1$
	public static String DEFAULT_FETCH_CACHE_LOCATION = "scmCache"; //$NON-NLS-1$

	private void addProjectReference(Map<String,String> entryInfos) {
		String repoLocation = (String) entryInfos.get(KEY_REPO);
		String path = (String) entryInfos.get(KEY_PATH);
		String projectName = (String) entryInfos.get(KEY_ELEMENT_NAME);
		String tag = (String) entryInfos.get(IFetchFactory.KEY_ELEMENT_TAG);

		if (repoLocation != null && projectName != null) {
			String sourceUrl = asReference(repoLocation, path, projectName, tag);
			if (sourceUrl != null) {
				entryInfos.put(Constants.KEY_SOURCE_REFERENCES, sourceUrl);
			}
		}
	}

	@Override
	public void addTargets(IAntScript script) {
		Map<String,String> params = new HashMap<String,String>(3);
		List<String> args = new ArrayList<String>(5);

		script.printComment("Start of common Git fetch factory targets."); //$NON-NLS-1$

		// determine if clone git operation should be skipped
		script.printTargetDeclaration("GitCheckSkipClone", null, null, null, null); //$NON-NLS-1$
		printGitRepoBaseLocationDefault(script);
		printConditionStart(script, "skipClone", null, null); //$NON-NLS-1$
		script.printStartTag("or"); //$NON-NLS-1$
		script.incrementIdent();
		printAvailableFile(script, Utils.getPropertyFormat(PROP_GITREPO_LOCAL_PATH));
		printAvailableFile(script, Utils.getPropertyFormat(PROP_FILETOCHECK));
		script.decrementIdent();
		script.printEndTag("or"); //$NON-NLS-1$
		printConditionEnd(script);
		script.printTargetEnd();

		// clone repo task
		script.printTargetDeclaration(TARGET_CLONE_REPO, "GitCheckSkipClone", null, "skipClone", null); //$NON-NLS-1$ //$NON-NLS-2$
		printGitRepoBaseLocationDefault(script);
		params.put("dir", Utils.getPropertyFormat(PROP_GITREPO_LOCAL_PATH)); //$NON-NLS-1$
		script.printElement("mkdir", params); //$NON-NLS-1$
		args.add(Utils.getPropertyFormat(PROP_GITREPO));
		args.add("."); //$NON-NLS-1$
		printGitTask(script, "clone", args); //$NON-NLS-1$
		script.printTargetEnd();

		// pull repo task
		script.printTargetDeclaration(TARGET_UPDATE_REPO, null, Utils.getPropertyFormat(PROP_GITREPO_LOCAL_PATH), "${fileToCheck}", null); //$NON-NLS-1$
		printGitRepoBaseLocationDefault(script);
		args.clear();
		args.add("--all"); //$NON-NLS-1$
		printGitTask(script, "fetch", null); //$NON-NLS-1$
		script.printTargetEnd();

		// checkout tag task
		script.printTargetDeclaration(TARGET_CHECKOUT_TAG, null, Utils.getPropertyFormat(PROP_GITREPO_LOCAL_PATH), "${fileToCheck}", null); //$NON-NLS-1$
		printGitRepoBaseLocationDefault(script);
		args.clear();
		args.add("--force"); //$NON-NLS-1$
		args.add(Utils.getPropertyFormat(PROP_TAG));
		printGitTask(script, "checkout", args); //$NON-NLS-1$
		script.printTargetEnd();

		// copy an elements from repo to the destination
		script.printTargetDeclaration(TARGET_GET_ELEMENT_FROM_REPO, null, Utils.getPropertyFormat(PROP_GITREPO_LOCAL_PATH), "${fileToCheck}", null); //$NON-NLS-1$
		printGitRepoBaseLocationDefault(script);
		params.clear();
		params.put("todir", Utils.getPropertyFormat(PROP_DESTINATIONFOLDER)); //$NON-NLS-1$
		script.printStartTag("copy", params); //$NON-NLS-1$
		script.incrementIdent();
		params.clear();
		params.put("dir", Utils.getPropertyFormat(PROP_GITREPO_LOCAL_PATH) + "/" + Utils.getPropertyFormat(PROP_PATH)); //$NON-NLS-1$ //$NON-NLS-2$
		script.printElement("fileset", params); //$NON-NLS-1$
		script.decrementIdent();
		script.printEndTag("copy"); //$NON-NLS-1$
		script.printTargetEnd();

		// copy a file from repo to the destination
		script.printTargetDeclaration(TARGET_GET_FILE_FROM_REPO, null, Utils.getPropertyFormat(PROP_GITREPO_LOCAL_PATH), "${fileToCheck}", null); //$NON-NLS-1$
		printGitRepoBaseLocationDefault(script);
		params.clear();
		params.put("file", Utils.getPropertyFormat(PROP_GITREPO_LOCAL_PATH) + "/" + Utils.getPropertyFormat(PROP_PATH)); //$NON-NLS-1$ //$NON-NLS-2$
		params.put("tofile", Utils.getPropertyFormat(PROP_DESTINATIONFOLDER) + "/" + Utils.getPropertyFormat(PROP_FILE)); //$NON-NLS-1$ //$NON-NLS-2$
		params.put("failOnError", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		script.printElement("copy", params); //$NON-NLS-1$
		script.printTargetEnd();

		script.printComment("End of common Git fetch factory targets."); //$NON-NLS-1$
	}

	private void printGitRepoBaseLocationDefault(IAntScript script) {
		script.println("<property name=\"" + PROP_FETCH_CACHE_LOCATION + "\" value=\"" + DEFAULT_FETCH_CACHE_LOCATION + "\" />"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private void printGitTask(IAntScript script, String commandName, List args) {
		// print command
		StringBuffer m = new StringBuffer();
		m.append("[GIT] "); //$NON-NLS-1$
		m.append(Utils.getPropertyFormat(PROP_GITREPO_LOCAL_PATH));
		m.append(" >> git ").append(commandName); //$NON-NLS-1$
		if (args != null) {
			for (int i = 0; i < args.size(); i++) {
				m.append(" ").append(args.get(i)); //$NON-NLS-1$
			}
		}
		script.printEchoTask(null, m.toString(), "info"); //$NON-NLS-1$

		Map<String,String> params = new HashMap<String,String>(3);
		params.put("executable", "git"); //$NON-NLS-1$ //$NON-NLS-2$
		params.put("dir", Utils.getPropertyFormat(PROP_GITREPO_LOCAL_PATH)); //$NON-NLS-1$
		params.put("failOnError", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		script.printStartTag("exec", params); //$NON-NLS-1$
		script.incrementIdent();

		// cmd
		printArg(script, commandName);

		// append arguments
		if (args != null) {
			for (int i = 0; i < args.size(); i++) {
				String arg = (String) args.get(i);
				printArg(script, arg);
			}
		}

		script.decrementIdent();
		script.printEndTag("exec"); //$NON-NLS-1$
	}

	private static void printArg(IAntScript script, String value) {
		Map<String,String> params = new HashMap<String,String>(1);
		params.put("value", value); //$NON-NLS-1$
		script.printElement("arg", params); //$NON-NLS-1$
	}

	/**
	 * Generates a path where the specified repository should be cloned to.
	 *
	 * @param repoLocation
	 * @return local file system path
	 */
	private String asLocalRepo(String repoLocation) {
		StringBuffer b = new StringBuffer(repoLocation.length());
		b.append(Utils.getPropertyFormat(PROP_FETCH_CACHE_LOCATION)).append('/');
		for (int i = 0; i < repoLocation.length(); i++) {
			char c = repoLocation.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				b.append(c);
			} else {
				// replace with '_'
				b.append('_');
			}
		}
		if (b.charAt(b.length() - 1) == '/')
			return b.substring(0, b.length() - 1);
		return b.toString();
	}

	/**
	 * Creates an SCMURL reference to the associated source.
	 *
	 * @param repoLocation
	 * @param path
	 * @param projectName
	 * @return project reference string or <code>null</code> if none
	 */
	private String asReference(String repoLocation, String path, String projectName, String tagName) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("scm:git:"); //$NON-NLS-1$

		// use repoLocation as is (Git support many URLs)
		buffer.append(repoLocation);

		// path
		if (path != null) {
			Path projectPath = new Path(path);

			buffer.append(";path=\""); //$NON-NLS-1$
			buffer.append(projectPath.toString());
			buffer.append('"');

			// project name if different then last path segment
			if (!projectPath.lastSegment().equals(projectName)) {
				buffer.append(";project=\""); //$NON-NLS-1$
				buffer.append(projectName);
				buffer.append('"');
			}
		}

		if (tagName != null && !tagName.equals("HEAD")) { //$NON-NLS-1$
			buffer.append(";tag="); //$NON-NLS-1$
			buffer.append(tagName);
		}
		return buffer.toString();
	}

	@Override
	public void generateRetrieveElementCall(Map entryInfos, IPath destination, IAntScript script) {
		String type = (String) entryInfos.get(KEY_ELEMENT_TYPE);
		boolean prebuilt = Boolean.valueOf((String) entryInfos.get(KEY_PREBUILT)).booleanValue();
		String gitRepo = (String) entryInfos.get(KEY_REPO);
		String localGitRepo = asLocalRepo(gitRepo);
		String path = (String) entryInfos.get(KEY_PATH);
		String tag = (String) entryInfos.get(IFetchFactory.KEY_ELEMENT_TAG);

		final String gitCopyTarget;
		IPath locationToCheck = null;
		Map<String,String> params = new HashMap<String,String>(5);
		params.put(PROP_GITREPO_LOCAL_PATH, localGitRepo);
		if (prebuilt) {
			// if we have a pre-built JAR then we want to put it right in the plugins/features directory
			// and not a sub-directory so strip off last segment
			params.put(PROP_DESTINATIONFOLDER, destination.removeLastSegments(1).toString());
			params.put(PROP_PATH, path);

			// extract file name from path
			String prebuiltJarFile = new Path(path).lastSegment();
			params.put(PROP_FILE, prebuiltJarFile);

			// if we have a pre-built plug-in then we want to check the existence of the JAR file
			// rather than the plug-in manifest.
			locationToCheck = destination.removeLastSegments(1).append(prebuiltJarFile);

			// get single file
			gitCopyTarget = TARGET_GET_FILE_FROM_REPO;
		} else {
			params.put(PROP_DESTINATIONFOLDER, destination.toString());
			if (path != null) {
				params.put(PROP_PATH, new Path(path).makeRelative().toString());
			}

			// check for existence of element descriptor
			if (type.equals(ELEMENT_TYPE_FEATURE)) {
				locationToCheck = destination.append(Constants.FEATURE_FILENAME_DESCRIPTOR);
			} else if (type.equals(ELEMENT_TYPE_PLUGIN)) {
				locationToCheck = destination.append(Constants.PLUGIN_FILENAME_DESCRIPTOR);
			} else if (type.equals(ELEMENT_TYPE_FRAGMENT)) {
				locationToCheck = destination.append(Constants.FRAGMENT_FILENAME_DESCRIPTOR);
			} else if (type.equals(ELEMENT_TYPE_BUNDLE)) {
				locationToCheck = destination.append(Constants.BUNDLE_FILENAME_DESCRIPTOR);
			}

			// copy complete element
			gitCopyTarget = TARGET_GET_ELEMENT_FROM_REPO;
		}

		// check for availability of element in destination
		if (locationToCheck != null) {
			params.put(PROP_FILETOCHECK, locationToCheck.toString());
			printAvailableTask(locationToCheck.toString(), locationToCheck.toString(), script);
			// plug-ins/fragments may not have an xml descriptor anymore, thus also check for MANIFEST.MF
			if (!prebuilt && (type.equals(IFetchFactory.ELEMENT_TYPE_PLUGIN) || type.equals(IFetchFactory.ELEMENT_TYPE_FRAGMENT))) {
				printAvailableTask(locationToCheck.toString(), destination.append(Constants.BUNDLE_FILENAME_DESCRIPTOR).toString(), script);
			}
		}

		// clone the Git repo to a local repo and checkout the tag
		printCloneRepoAndCheckoutTagTasks(script, gitRepo, localGitRepo, tag, locationToCheck);

		// copy the content into the destination
		script.printAntCallTask(gitCopyTarget, true, params);
	}

	@Override
	public void generateRetrieveFilesCall(final Map entryInfos, IPath destination, final String[] files, IAntScript script) {
		String gitRepo = (String) entryInfos.get(KEY_REPO);
		String localGitRepo = asLocalRepo(gitRepo);
		String path = (String) entryInfos.get(KEY_PATH);
		String tag = (String) entryInfos.get(IFetchFactory.KEY_ELEMENT_TAG);

		// clone the Git repo to a local repo and checkout the tag
		printCloneRepoAndCheckoutTagTasks(script, gitRepo, localGitRepo, tag, null);

		// copy files to destination
		Map<String,String> params = new HashMap<String,String>(4);
		for (int i = 0; i < files.length; i++) {
			String file = files[i];
			IPath filePath;
			if (path != null) {
				filePath = new Path(path).append(file);
			} else {
				filePath = new Path((String) entryInfos.get(KEY_ELEMENT_NAME)).append(file);
			}

			params.clear();
			params.put(PROP_GITREPO_LOCAL_PATH, localGitRepo);
			params.put(PROP_DESTINATIONFOLDER, destination.toString());
			params.put(PROP_PATH, filePath.toString());
			params.put(PROP_FILE, file);
			script.printAntCallTask(TARGET_GET_FILE_FROM_REPO, true, params);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void parseMapFileEntry(String repoSpecificentry, Properties overrideTags, Map entryInfos) throws CoreException {
		// build up the table of arguments in the map file entry
		String[] arguments = Utils.getArrayFromStringWithBlank(repoSpecificentry, SEPARATOR);
		Map<String,String> table = new HashMap<String,String>();
		for (int i = 0; i < arguments.length; i++) {
			String arg = arguments[i];
			// if we have at least one arg without an equals sign, then
			// revert back to the legacy parsing
			int index = arg.indexOf('=');
			if (index == -1) {
				String message = NLS.bind(Messages.error_incorrectDirectoryEntryKeyValue, entryInfos.get(KEY_ELEMENT_NAME));
				throw new CoreException(new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, 1, message, null));
			}
			String key = arg.substring(0, index);
			String value = arg.substring(index + 1);
			table.put(key, value);
		}

		// sanity check that all required attributes are present
		if (!table.containsKey(KEY_REPO)) {
			String message = NLS.bind(Messages.error_directoryEntryRequiresRepo, entryInfos.get(KEY_ELEMENT_NAME));
			throw new CoreException(new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, 1, message, null));
		}

		// add entries to the entryInfo map here instead of inside the loop
		// to avoid contaminating entryInfos
		String overrideTag = overrideTags != null ? overrideTags.getProperty(OVERRIDE_TAG) : null;
		entryInfos.put(IFetchFactory.KEY_ELEMENT_TAG, (overrideTag != null && overrideTag.trim().length() != 0 ? overrideTag : table.get(IFetchFactory.KEY_ELEMENT_TAG)));
		entryInfos.put(KEY_REPO, table.get(KEY_REPO));
		if (table.get(KEY_PATH) != null)
			entryInfos.put(KEY_PATH, new Path((String) table.get(KEY_PATH)).makeRelative().removeTrailingSeparator().toString()); // sanitize path
		entryInfos.put(KEY_PREBUILT, table.get(KEY_PREBUILT));
		addProjectReference(entryInfos);
	}

	/**
	 * Print the <code>available</code> Ant task to this script. This task sets a property
	 * value if the given file exists at runtime.
	 *
	 * @param property the property to set
	 * @param file the file to look for
	 */
	private void printAvailableTask(String property, String file, IAntScript script) {
		Map<String, String> params = new HashMap<String, String>(2);
		params.put("property", property); //$NON-NLS-1$
		params.put("file", file); //$NON-NLS-1$
		script.printElement("available", params); //$NON-NLS-1$
	}

	private void printCloneRepoAndCheckoutTagTasks(IAntScript script, String gitRepo, String localGitRepo, String tag, IPath locationToCheckIfPluginLocal) {
		// determine availability of local repo (done to avoid unnecessary Git operations)
		printAvailableTask(localGitRepo, localGitRepo, script);

		// pull if already cloned
		Map<String, String>  params = new HashMap<String, String>(5);
		params.put(PROP_GITREPO_LOCAL_PATH, localGitRepo);
		if (locationToCheckIfPluginLocal != null)
			params.put(PROP_FILETOCHECK, locationToCheckIfPluginLocal.toString());
		script.printAntCallTask(TARGET_UPDATE_REPO, true, params);

		// clone if not cloned
		params.put(PROP_GITREPO, gitRepo);
		params.put(PROP_GITREPO_LOCAL_PATH, localGitRepo);
		if (locationToCheckIfPluginLocal != null)
			params.put(PROP_FILETOCHECK, locationToCheckIfPluginLocal.toString());
		script.printAntCallTask(TARGET_CLONE_REPO, true, params);

		// checkout the tag
		params.clear();
		params.put(PROP_GITREPO_LOCAL_PATH, localGitRepo);
		params.put(PROP_TAG, tag);
		if (locationToCheckIfPluginLocal != null)
			params.put(PROP_FILETOCHECK, locationToCheckIfPluginLocal.toString());
		script.printAntCallTask(TARGET_CHECKOUT_TAG, true, params);

		// re-determine availability of local repo (done to avoid unnecessary Git operations)
		printAvailableTask(localGitRepo, localGitRepo, script);
	}

	private void printConditionStart(IAntScript script, String property, String value, String elseValue) {
		script.printTabs();
		script.print("<condition"); //$NON-NLS-1$
		script.printAttribute("property", property, true); //$NON-NLS-1$
		script.printAttribute("value", value, false); //$NON-NLS-1$
		script.printAttribute("else", elseValue, false); //$NON-NLS-1$
		script.print(">"); //$NON-NLS-1$
		script.println();
		script.incrementIdent();
	}

	private void printAvailableFile(IAntScript script, String file) {
		script.println("<available file=\"" + file + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void printConditionEnd(IAntScript script) {
		script.decrementIdent();
		script.printEndTag("condition"); //$NON-NLS-1$
	}
}
