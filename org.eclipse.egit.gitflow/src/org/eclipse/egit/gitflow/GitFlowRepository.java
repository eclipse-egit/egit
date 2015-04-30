package org.eclipse.egit.gitflow;

import static org.eclipse.egit.gitflow.GitFlowDefaults.DEVELOP;
import static org.eclipse.egit.gitflow.GitFlowDefaults.FEATURE_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.HOTFIX_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.RELEASE_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.VERSION_TAG;
import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_TAGS;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RemoteConfig;

/**
 * Wrapper for JGit repository.
 */
public class GitFlowRepository {
	/** Key for .git/config */
	public static final String MASTER_KEY = "master"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String DEVELOP_KEY = "develop"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String HOTFIX_KEY = "hotfix"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String RELEASE_KEY = "release"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String FEATURE_KEY = "feature"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String VERSION_TAG_KEY = "versiontag"; //$NON-NLS-1$

	/** Name of .git/config section. */
	public static final String USER_SECTION = "user"; //$NON-NLS-1$

	/** Name of .git/config section. */
	public static final String BRANCH_SECTION = "branch"; //$NON-NLS-1$

	/** Name of .git/config section. */
	public static final String PREFIX_SECTION = "prefix"; //$NON-NLS-1$

	/** Name of .git/config section. */
	public static final String GITFLOW_SECTION = "gitflow"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String REMOTE_KEY = "remote"; //$NON-NLS-1$

	/** Key for .git/config */
	public static final String MERGE_KEY = "merge"; //$NON-NLS-1$

	private Repository repository;

	/**
	 * @param repository
	 */
	public GitFlowRepository(Repository repository) {
		Assert.isNotNull(repository);
		this.repository = repository;
	}

	/**
	 * @return Whether or not this repository has branches.
	 */
	public boolean hasBranches() {
		List<Ref> branches;
		try {
			branches = Git.wrap(repository).branchList().call();
			return !branches.isEmpty();
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param branch
	 * @return Whether or not branch exists in this repository.
	 * @throws GitAPIException
	 */
	public boolean hasBranch(String branch) throws GitAPIException {
		String fullBranchName = R_HEADS + branch;
		List<Ref> branchList = Git.wrap(repository).branchList().call();
		for (Ref ref : branchList) {
			if (fullBranchName.equals(ref.getTarget().getName())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @param branchName
	 * @return Ref for branchName.
	 * @throws IOException
	 */
	public Ref findBranch(String branchName) throws IOException {
		return repository.getRef(R_HEADS + branchName);
	}

	/**
	 * @return git init done?
	 * @throws IOException
	 */
	public boolean isInitialized() throws IOException {
		StoredConfig config = repository.getConfig();
		Set<String> sections = config.getSections();
		return sections.contains(GITFLOW_SECTION);
	}

	/**
	 * @return current branchs has feature-prefix?
	 * @throws IOException
	 */
	public boolean isFeature() throws IOException {
		return repository.getBranch().startsWith(getFeaturePrefix());
	}

	/**
	 * @return branch name has develop name?
	 * @throws IOException
	 */
	public boolean isDevelop() throws IOException {
		return repository.getBranch().equals(getDevelop());
	}

	/**
	 * @return branch name has master name?
	 * @throws IOException
	 */
	public boolean isMaster() throws IOException {
		return repository.getBranch().equals(getMaster());
	}

	/**
	 * @return current branchs has release-prefix?
	 * @throws IOException
	 */
	public boolean isRelease() throws IOException {
		return repository.getBranch().startsWith(getReleasePrefix());
	}

	/**
	 * @return current branchs has hotfix-prefix?
	 * @throws IOException
	 */
	public boolean isHotfix() throws IOException {
		return repository.getBranch().startsWith(getHotfixPrefix());
	}

	/**
	 * @return Local user of this repository.
	 */
	public String getUser() {
		StoredConfig config = repository.getConfig();
		String userName = config.getString(USER_SECTION, null, "name"); //$NON-NLS-1$
		String email = config.getString(USER_SECTION, null, "email"); //$NON-NLS-1$
		return String.format("%s <%s>", userName, email); //$NON-NLS-1$
	}

	/**
	 * @return feature prefix configured for this repository.
	 */
	public String getFeaturePrefix() {
		return getPrefix(FEATURE_KEY, FEATURE_PREFIX);
	}

	/**
	 * @return release prefix configured for this repository.
	 */
	public String getReleasePrefix() {
		return getPrefix(RELEASE_KEY, RELEASE_PREFIX);
	}

	/**
	 * @return hotfix prefix configured for this repository.
	 */
	public String getHotfixPrefix() {
		return getPrefix(HOTFIX_KEY, HOTFIX_PREFIX);
	}

	/**
	 * @return version prefix configured for this repository, that is used in
	 *         tags.
	 */
	public String getVersionTagPrefix() {
		return getPrefix(VERSION_TAG_KEY, VERSION_TAG);
	}

	/**
	 * @return name of develop configured for this repository.
	 */
	public String getDevelop() {
		return getBranch(DEVELOP_KEY, DEVELOP);
	}

	/**
	 * @return name of master configured for this repository.
	 */
	public String getMaster() {
		return getBranch(MASTER_KEY, GitFlowDefaults.MASTER);
	}

	/**
	 * @return full name of develop configured for this repository.
	 */
	public String getDevelopFull() {
		return R_HEADS + getDevelop();
	}

	/**
	 * @param prefixName
	 * @param defaultPrefix
	 * @return value for key prefixName from .git/config or default
	 */
	public String getPrefix(String prefixName, String defaultPrefix) {
		StoredConfig config = repository.getConfig();
		String result = config.getString(GITFLOW_SECTION, PREFIX_SECTION,
				prefixName);
		return (result == null) ? defaultPrefix : result;
	}

	/**
	 * @param branch
	 * @param defaultBranch
	 * @return value for key branch from .git/config or default
	 */
	public String getBranch(String branch, String defaultBranch) {
		StoredConfig config = repository.getConfig();
		String result = config.getString(GITFLOW_SECTION, BRANCH_SECTION,
				branch);
		return (result == null) ? defaultBranch : result;
	}

	/**
	 * Set prefix in .git/config
	 *
	 * @param prefixName
	 * @param value
	 */
	public void setPrefix(String prefixName, String value) {
		StoredConfig config = repository.getConfig();
		config.setString(GITFLOW_SECTION, PREFIX_SECTION, prefixName, value);
	}

	/**
	 * Set branchName in .git/config
	 *
	 * @param branchName
	 * @param value
	 */
	public void setBranch(String branchName, String value) {
		StoredConfig config = repository.getConfig();
		config.setString(GITFLOW_SECTION, BRANCH_SECTION, branchName, value);
	}

	/**
	 * @return HEAD commit
	 */
	public RevCommit findHead() {
		try (RevWalk walk = new RevWalk(repository)) {
			try {
				ObjectId head = repository.resolve(HEAD);
				return walk.parseCommit(head);
			} catch (RevisionSyntaxException | IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * @param branchName
	 * @return HEAD commit on branch branchName
	 */
	public RevCommit findHead(String branchName) {
		try (RevWalk walk = new RevWalk(repository)) {
			try {
				ObjectId head = repository.resolve(R_HEADS + branchName);
				return walk.parseCommit(head);
			} catch (RevisionSyntaxException | IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * @param sha1
	 * @return Commit for SHA1
	 */
	public RevCommit findCommit(String sha1) {
		try (RevWalk walk = new RevWalk(repository)) {
			try {
				ObjectId head = repository.resolve(sha1);
				return walk.parseCommit(head);
			} catch (RevisionSyntaxException | IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	/**
	 * @param featureName
	 * @return full name of branch featureName
	 */
	public String getFullFeatureBranchName(String featureName) {
		return R_HEADS + getFeatureBranchName(featureName);
	}

	/**
	 * @param featureName
	 * @return name of branch featureName
	 */
	public String getFeatureBranchName(String featureName) {
		return getFeaturePrefix() + featureName;
	}

	/**
	 * @param hotfixName
	 * @return name of branch hotfixName
	 */
	public String getHotfixBranchName(String hotfixName) {
		return getHotfixPrefix() + hotfixName;
	}

	/**
	 * @param hotfixName
	 * @return full name of branch hotfixName
	 */
	public String getFullHotfixBranchName(String hotfixName) {
		return R_HEADS + getHotfixBranchName(hotfixName);
	}

	/**
	 * @param releaseName
	 * @return full name of branch releaseName
	 */
	public String getFullReleaseBranchName(String releaseName) {
		return R_HEADS + getReleaseBranchName(releaseName);
	}

	/**
	 * @param releaseName
	 * @return name of branch releaseName
	 */
	public String getReleaseBranchName(String releaseName) {
		return getReleasePrefix() + releaseName;
	}

	/**
	 * @return JGit repository
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @return git flow feature branches
	 */
	public List<Ref> getFeatureBranches() {
		return getPrefixBranches(R_HEADS + getFeaturePrefix());
	}

	/**
	 * @return git flow release branches
	 */
	public List<Ref> getReleaseBranches() {
		return getPrefixBranches(R_HEADS + getReleasePrefix());
	}

	/**
	 * @return git flow hotfix branches
	 */
	public List<Ref> getHotfixBranches() {
		return getPrefixBranches(R_HEADS + getHotfixPrefix());
	}

	private List<Ref> getPrefixBranches(String prefix) {
		try {
			List<Ref> branches = Git.wrap(repository).branchList().call();
			List<Ref> prefixBranches = new ArrayList<Ref>();
			for (Ref ref : branches) {
				if (ref.getName().startsWith(prefix)) {
					prefixBranches.add(ref);
				}
			}

			return prefixBranches;
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param ref
	 * @return branch name for ref
	 */
	public String getFeatureBranchName(Ref ref) {
		return ref.getName().substring((R_HEADS + getFeaturePrefix()).length());
	}

	/**
	 * @param tagName
	 * @return commit tag tagName points to
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	public RevCommit findCommitForTag(String tagName)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		try (RevWalk revWalk = new RevWalk(repository)) {
			Ref tagRef = repository.getRef(R_TAGS + tagName);
			if (tagRef == null) {
				return null;
			}
			RevCommit result = revWalk.parseCommit(tagRef.getObjectId());
			return result;
		}
	}

	/**
	 * @return Configured origin.
	 */
	public RemoteConfig getDefaultRemoteConfig() {
		StoredConfig rc = repository.getConfig();
		RemoteConfig result;
		try {
			result = new RemoteConfig(rc, DEFAULT_REMOTE_NAME);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
		return result;
	}

	/**
	 * @return Whether or not there is a default remote configured.
	 */
	public boolean hasDefaultRemote() {
		RemoteConfig config = getDefaultRemoteConfig();
		return !config.getURIs().isEmpty();
	}

	/**
	 * @param featureName
	 * @param value
	 * @throws IOException
	 */
	public void setRemote(String featureName, String value) throws IOException {
		setBranchValue(featureName, value, REMOTE_KEY);
	}

	/**
	 * @param featureName
	 * @param value
	 * @throws IOException
	 */
	public void setMerge(String featureName, String value) throws IOException {
		setBranchValue(featureName, value, MERGE_KEY);
	}

	private void setBranchValue(String featureName, String value,
			String mergeKey) throws IOException {
		StoredConfig config = repository.getConfig();
		config.setString(BRANCH_SECTION, featureName, mergeKey, value);
		config.save();
	}

	/**
	 * @return Current branch name
	 */
	public String getCurrentBranchName() {
		try {
			return repository.getBranch();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
