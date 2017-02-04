/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010, Benjamin Muskalla <bmuskalla@eclipsesource.com>
 * Copyright (C) 2012, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.CloneOperation.PostCloneTask;
import org.eclipse.egit.core.op.ConfigureFetchAfterCloneTask;
import org.eclipse.egit.core.op.ConfigureGerritAfterCloneTask;
import org.eclipse.egit.core.op.ConfigurePushAfterCloneTask;
import org.eclipse.egit.core.op.SetRepositoryConfigPropertyTask;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.SecureStoreUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitCloneSourceProviderExtension.CloneSourceProvider;
import org.eclipse.egit.ui.internal.components.RepositorySelection;
import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.egit.ui.internal.provisional.wizards.GitRepositoryInfo;
import org.eclipse.egit.ui.internal.provisional.wizards.GitRepositoryInfo.PushInfo;
import org.eclipse.egit.ui.internal.provisional.wizards.GitRepositoryInfo.RepositoryConfigProperty;
import org.eclipse.egit.ui.internal.provisional.wizards.IRepositorySearchResult;
import org.eclipse.egit.ui.internal.provisional.wizards.NoRepositoryInfoException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkingSet;

/**
 * Implements the basic functionality of a clone wizard
 */
public abstract class AbstractGitCloneWizard extends Wizard {

	/**
	 * a page for branch selection
	 */
	protected SourceBranchPage validSource;

	/**
	 * a page for selection of the clone destination
	 */
	protected CloneDestinationPage cloneDestination;

	/**
	 * the path where a clone has been created in
	 */
	protected String alreadyClonedInto;

	/**
	 * whether the clone operation is done later on by the caller of the wizard
	 */
	protected boolean callerRunsCloneOperation;

	/**
	 * the result which was found when the last search was done
	 */
	protected IRepositorySearchResult currentSearchResult;

	private CloneOperation cloneOperation;

	/**
	 * Construct the clone wizard based on given repository search result. If
	 * the search result is an instance of org.eclipse.jface.wizard.WizardPage,
	 * then the page is shown in the wizard before the repository info is read.
	 * The repository location page that allows the repository info to be
	 * provided by different search providers is not shown.
	 *
	 * @param searchResult
	 *            the search result to initialize the clone wizard with.
	 */
	public AbstractGitCloneWizard(IRepositorySearchResult searchResult) {
		this();
		this.currentSearchResult = searchResult;
	}

	/**
	 * Construct the clone wizard with a repository location page that allows
	 * the repository info to be provided by different search providers.
	 */
	public AbstractGitCloneWizard() {
		setNeedsProgressMonitor(true);
		validSource = new SourceBranchPage() {

			@Override
			public void setVisible(boolean visible) {
				RepositorySelection selection = getRepositorySelection();
				if (selection != null && visible) {
					setSelection(selection);
					setCredentials(getCredentials());
				}
				super.setVisible(visible);
			}
		};
		cloneDestination = new CloneDestinationPage() {
			@Override
			public void setVisible(boolean visible) {
				RepositorySelection selection = getRepositorySelection();
				if (selection != null && visible) {
					setSelection(selection,
							validSource.getAvailableBranches(),
							validSource.getSelectedBranches(),
							validSource.getHEAD());
				}
				super.setVisible(visible);
			}
		};
	}

	/**
	 * subclasses may add pages to the Wizard which will be shown before the clone step
	 */
	protected abstract void addPreClonePages();

	/**
	 * subclasses may add pages to the Wizard which will be shown after the clone step
	 */
	protected abstract void addPostClonePages();

	@Override
	final public void addPages() {
		if (hasSearchResult())
			addRepositorySearchPage();
		else
			addRepositoryLocationPage();
		addPreClonePages();
		addPage(validSource);
		addPage(cloneDestination);
		addPostClonePages();
	}

	/**
	 * @return if the search result is set
	 */
	protected boolean hasSearchResult() {
		return currentSearchResult != null;
	}

	private void addRepositorySearchPage() {
		if (currentSearchResult instanceof WizardPage)
			addPage((WizardPage)currentSearchResult);
	}

	private void addRepositoryLocationPage() {
		List<CloneSourceProvider> cloneSourceProviders = getCloneSourceProviders();
		if (hasSingleCloneSourceProviderWithFixedLocation(cloneSourceProviders))
			try {
				addPage(cloneSourceProviders.get(0).getRepositorySearchPage());
			} catch (CoreException e) {
				Activator.logError(e.getLocalizedMessage(), e);
			}
		else
			addPage(new RepositoryLocationPage(cloneSourceProviders));
	}

	private boolean hasSingleCloneSourceProviderWithFixedLocation(
			List<CloneSourceProvider> cloneSourceProviders) {
		return cloneSourceProviders.size() == 1 && cloneSourceProviders.get(0).hasFixLocation();
	}

	/**
	 * @return a list of CloneSourceProviders, may be extended by a subclass
	 */
	protected List<CloneSourceProvider> getCloneSourceProviders() {
		return GitCloneSourceProviderExtension.getCloneSourceProvider();
	}

	/**
	 * Do the clone using data which were collected on the pages
	 * {@code validSource} and {@code cloneDestination}
	 *
	 * @param gitRepositoryInfo
	 * @return if clone was successful
	 * @throws URISyntaxException
	 */
	protected boolean performClone(GitRepositoryInfo gitRepositoryInfo) throws URISyntaxException {
		URIish uri = new URIish(gitRepositoryInfo.getCloneUri());
		UserPasswordCredentials credentials = gitRepositoryInfo.getCredentials();
		setWindowTitle(NLS.bind(UIText.GitCloneWizard_jobName, uri.toString()));
		final boolean allSelected;
		final Collection<Ref> selectedBranches;
		if (validSource.isSourceRepoEmpty()) {
			// fetch all branches of empty repo
			allSelected = true;
			selectedBranches = Collections.emptyList();
		} else {
			allSelected = validSource.isAllSelected();
			selectedBranches = validSource.getSelectedBranches();
		}
		final File workdir = cloneDestination.getDestinationFile();
		final Ref ref = cloneDestination.getInitialBranch();
		final String remoteName = cloneDestination.getRemote();

		boolean created = workdir.exists();
		if (!created)
			created = workdir.mkdirs();

		if (!created || !workdir.isDirectory()) {
			final String errorMessage = NLS.bind(
					UIText.GitCloneWizard_errorCannotCreate, workdir.getPath());
			ErrorDialog.openError(getShell(), getWindowTitle(),
					UIText.GitCloneWizard_failed, new Status(IStatus.ERROR,
							Activator.getPluginId(), 0, errorMessage, null));
			// let's give user a chance to fix this minor problem
			return false;
		}

		int timeout = Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
		final CloneOperation op = new CloneOperation(uri, allSelected,
				selectedBranches, workdir, ref != null ? ref.getName() : null,
				remoteName, timeout);
		CredentialsProvider credentialsProvider = null;
		if (credentials != null) {
			credentialsProvider = new EGitCredentialsProvider(
					credentials.getUser(), credentials.getPassword());
		} else {
			credentialsProvider = new EGitCredentialsProvider();
		}
		op.setCredentialsProvider(credentialsProvider);
		op.setCloneSubmodules(cloneDestination.isCloneSubmodules());

		rememberHttpHost(op, uri);
		configureFetchSpec(op, gitRepositoryInfo, remoteName);
		configurePush(op, gitRepositoryInfo, remoteName);
		configureRepositoryConfig(op, gitRepositoryInfo);
		configureGerrit(op, gitRepositoryInfo, credentialsProvider, remoteName,
				timeout);

		if (cloneDestination.isImportProjects()) {
			final IWorkingSet[] sets = cloneDestination.getWorkingSets();
			op.addPostCloneTask(new PostCloneTask() {
				@Override
				public void execute(Repository repository,
						IProgressMonitor monitor) throws CoreException {
					importProjects(repository, sets);
				}
			});
		}

		alreadyClonedInto = workdir.getPath();

		if (!callerRunsCloneOperation)
			runAsJob(uri, op, gitRepositoryInfo);
		else
			cloneOperation = op;
		return true;
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page instanceof IRepositorySearchResult) {
			currentSearchResult = (IRepositorySearchResult)page;
			return validSource;
		}
		return super.getNextPage(page);
	}

	/**
	 * @return the repository selected by the user or {@code null} if an error
	 *         occurred
	 */
	@Nullable
	protected RepositorySelection getRepositorySelection() {
		try {
			return (new RepositorySelection(new URIish(currentSearchResult.getGitRepositoryInfo().getCloneUri()), null));
		} catch (URISyntaxException e) {
			Activator.error(UIText.GitImportWizard_errorParsingURI, e);
			return null;
		} catch (NoRepositoryInfoException e) {
			Activator.error(UIText.GitImportWizard_noRepositoryInfo, e);
			return null;
		} catch (Exception e) {
			Activator.error(e.getMessage(), e);
			return null;
		}
	}

	/**
	 * @return the credentials
	 */
	protected UserPasswordCredentials getCredentials() {
		try {
			return currentSearchResult.getGitRepositoryInfo().getCredentials();
		} catch (NoRepositoryInfoException e) {
			Activator.error(UIText.GitImportWizard_noRepositoryInfo, e);
			return null;
		} catch (Exception e) {
			Activator.error(e.getMessage(), e);
			return null;
		}
	}

	private void rememberHttpHost(CloneOperation op, URIish uri) {
		String scheme = uri.getScheme();
		if (scheme != null && scheme.toLowerCase().startsWith("http")) { //$NON-NLS-1$
			String host = uri.getHost();
			if (host != null) {
				op.addPostCloneTask(new RememberHostTask(host));
			}
		}
	}

	private void configureFetchSpec(CloneOperation op,
			GitRepositoryInfo gitRepositoryInfo, String remoteName) {
		for (String fetchRefSpec : gitRepositoryInfo.getFetchRefSpecs())
			op.addPostCloneTask(new ConfigureFetchAfterCloneTask(remoteName, fetchRefSpec));
	}

	private void configurePush(CloneOperation op,
			GitRepositoryInfo gitRepositoryInfo, String remoteName) {
		for (PushInfo pushInfo : gitRepositoryInfo.getPushInfos())
			try {
				URIish uri = pushInfo.getPushUri() != null ? new URIish(
						pushInfo.getPushUri()) : null;
				ConfigurePushAfterCloneTask task = new ConfigurePushAfterCloneTask(
						remoteName, pushInfo.getPushRefSpec(), uri);
				op.addPostCloneTask(task);
			} catch (URISyntaxException e) {
				Activator.handleError(UIText.GitCloneWizard_failed, e, true);
			}
	}

	private void configureRepositoryConfig(CloneOperation op, GitRepositoryInfo gitRepositoryInfo) {
		for (RepositoryConfigProperty p : gitRepositoryInfo.getRepositoryConfigProperties()) {
			SetRepositoryConfigPropertyTask task = new SetRepositoryConfigPropertyTask(
					p.getSection(), p.getSubsection(), p.getName(),
					p.getValue());
			op.addPostCloneTask(task);
		}
	}

	private void configureGerrit(CloneOperation op,
			GitRepositoryInfo gitRepositoryInfo,
			CredentialsProvider credentialsProvider, String remoteName,
			int timeout) {
		ConfigureGerritAfterCloneTask task = new ConfigureGerritAfterCloneTask(
				gitRepositoryInfo.getCloneUri(), remoteName,
				credentialsProvider, timeout);
		op.addPostCloneTask(task);
	}

	private void importProjects(final Repository repository,
			final IWorkingSet[] sets) {
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);
		Job importJob = new WorkspaceJob(MessageFormat.format(
				UIText.GitCloneWizard_jobImportProjects, repoName)) {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				SubMonitor progress = SubMonitor.convert(monitor, 2);
				List<File> files = new ArrayList<>();
				ProjectUtil.findProjectFiles(files, repository.getWorkTree(),
						true, progress.newChild(1));
				if (files.isEmpty()) {
					return Status.OK_STATUS;
				}
				Set<ProjectRecord> records = new LinkedHashSet<>();
				for (File file : files) {
					records.add(new ProjectRecord(file));
				}
				try {
					ProjectUtils.createProjects(records, sets,
							progress.newChild(1));
				} catch (InvocationTargetException e) {
					Activator.logError(e.getLocalizedMessage(), e);
				} catch (InterruptedException e) {
					Activator.logError(e.getLocalizedMessage(), e);
				}
				return Status.OK_STATUS;
			}
		};
		importJob.schedule();
	}

	/**
	 * @param container
	 * @param repositoryInfo
	 */
	public void runCloneOperation(IWizardContainer container, final GitRepositoryInfo repositoryInfo) {
		try {
			container.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					executeCloneOperation(cloneOperation, repositoryInfo, monitor);
				}
			});
		} catch (InvocationTargetException e) {
			Activator.handleError(UIText.GitCloneWizard_failed, e.getCause(),
					true);
		} catch (InterruptedException e) {
			// nothing to do
		}
	}

	private void runAsJob(final URIish uri, final CloneOperation op,
			final GitRepositoryInfo repositoryInfo) {
		final Job job = new Job(NLS.bind(UIText.GitCloneWizard_jobName,
				uri.toString())) {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {
					return executeCloneOperation(op, repositoryInfo, monitor);
				} catch (InterruptedException e) {
					return Status.CANCEL_STATUS;
				} catch (InvocationTargetException e) {
					Throwable thr = e.getCause();
					return new Status(IStatus.ERROR, Activator.getPluginId(),
							0, thr.getMessage(), thr);
				}
			}

			@Override
			public boolean belongsTo(Object family) {
				if (JobFamilies.CLONE.equals(family))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private IStatus executeCloneOperation(final CloneOperation op,
			final GitRepositoryInfo repositoryInfo,
			final IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {

		final RepositoryUtil util = Activator.getDefault().getRepositoryUtil();

		op.run(monitor);
		util.addConfiguredRepository(op.getGitDir());
		try {
			if (repositoryInfo.shouldSaveCredentialsInSecureStore())
				SecureStoreUtils.storeCredentials(repositoryInfo.getCredentials(),
						new URIish(repositoryInfo.getCloneUri()));
		} catch (Exception e) {
			Activator.error(e.getMessage(), e);
		}
		return Status.OK_STATUS;
	}

	/**
	 * @param newValue
	 *            if true the clone wizard just creates a clone operation. The
	 *            caller has to run this operation using runCloneOperation. If
	 *            false the clone operation is performed using a job.
	 */
	public void setCallerRunsCloneOperation(boolean newValue) {
		callerRunsCloneOperation = newValue;
	}

}
