/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010, Benjamin Muskalla <bmuskalla@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.ConfigurePushAfterCloneTask;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.core.op.SetChangeIdTask;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.SecureStoreUtils;
import org.eclipse.egit.ui.internal.components.RepositorySelectionPage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.osgi.util.NLS;

/**
 * Import Git Repository Wizard. A front end to a git clone operation.
 */
public class GitCloneWizard extends Wizard {

	/**
	 * Job family of the Clone Repository job.
	 */
	public static final Object CLONE_JOB_FAMILY = new Object();

	private static final String HELP_CONTEXT = "org.eclipse.egit.ui.GitCloneWizard"; //$NON-NLS-1$

	private RepositorySelectionPage cloneSource;

	private SourceBranchPage validSource;

	private CloneDestinationPage cloneDestination;

	private GerritConfigurationPage gerritConfiguration;

	private String alreadyClonedInto;

	private boolean callerRunsCloneOperation;

	private CloneOperation cloneOperation;

	private RepositorySourceFileSelectionPage repositorySelection;

	private Ref head;

	/**
	 * The default constructor
	 */
	public GitCloneWizard() {
		this(null);
	}

	/**
	 * Construct Clone Wizard
	 *
	 * @param presetURI
	 *            the clone URI to prepopulate the URI field of the clone wizard
	 *            with.
	 */
	public GitCloneWizard(String presetURI) {
		setWindowTitle(UIText.GitCloneWizard_title);
		setDefaultPageImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		setNeedsProgressMonitor(true);

		repositorySelection = new RepositorySourceFileSelectionPage();

		cloneSource = new RepositorySelectionPage(true, null);
		cloneSource.setHelpContext(HELP_CONTEXT);
		validSource = new SourceBranchPage() {

			@Override
			public void setVisible(boolean visible) {
				if (visible) {
					setSelection(cloneSource.getSelection());
					setCredentials(cloneSource.getCredentials());
				}
				super.setVisible(visible);
			}
		};
		validSource.setHelpContext(HELP_CONTEXT);
		cloneDestination = new CloneDestinationPage() {
			@Override
			public void setVisible(boolean visible) {
				if (visible)
					setSelection(cloneSource.getSelection(), validSource
							.getAvailableBranches(), validSource
							.getSelectedBranches(), validSource.getHEAD());
				super.setVisible(visible);
			}
		};
		cloneDestination.setHelpContext(HELP_CONTEXT);
		gerritConfiguration = new GerritConfigurationPage() {

			@Override
			public void setVisible(boolean visible) {
				if (visible)
					setSelection(cloneSource.getSelection());
				super.setVisible(visible);
			}
		};
		gerritConfiguration.setHelpContext(HELP_CONTEXT);
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

	@Override
	public boolean performCancel() {
		if (alreadyClonedInto != null) {
			File test = new File(alreadyClonedInto);
			if (test.exists()
					&& MessageDialog.openQuestion(getShell(),
							UIText.GitCloneWizard_abortingCloneTitle,
							UIText.GitCloneWizard_abortingCloneMsg)) {
				try {
					FileUtils.delete(test, FileUtils.RECURSIVE | FileUtils.RETRY);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return true;
	}

	@Override
	public void addPages() {
		addPage(repositorySelection);
		addPage(cloneSource);
		addPage(validSource);
		addPage(cloneDestination);
		addPage(gerritConfiguration);
	}

	@Override
	public boolean canFinish() {
		if (repositorySelection.isSingleRepo()) {
			return cloneDestination.isPageComplete()
					&& gerritConfiguration.isPageComplete();
		} else {
			return repositorySelection.isPageComplete();
		}
	}

	@Override
	public boolean performFinish() {
		try {
			if (repositorySelection.isSingleRepo()) {
				if (cloneSource.getStoreInSecureStore()) {
					if (!SecureStoreUtils.storeCredentials(cloneSource
							.getCredentials(), cloneSource.getSelection()
							.getURI()))
						return false;
				}
				return performClone();
			} else {
				try {

					return performMultipleClone();
				} finally {
					setWindowTitle(UIText.GitCloneWizard_title);
				}
			}
		} finally {
			setWindowTitle(UIText.GitCloneWizard_title);
		}
	}

	boolean performMultipleClone() {
		final Object[] uris = repositorySelection.getURIs();
		setWindowTitle(UIText.GitCloneWizard_jobName);
		final String workdirLocation = repositorySelection.getDestination();
		if (uris == null) {
			return true;
		}

		Job job = new Job("Cloning") { //$NON-NLS-1$

			@Override
			public IStatus run(IProgressMonitor monitor) {
				for (Object uriString : uris) {
					URIish uri = null;
					try {
						uri = new URIish((String) uriString);
					} catch (URISyntaxException e) {
						// if invalid uri, ignore the error and skip it
					}
					if (uri == null) {
						continue;
					}
					Collection<Ref> selectedBranches;
					selectedBranches = getAvailableBranches(uri);

					final File workdir = new File(workdirLocation
							+ "/" + uri.getPath()); //$NON-NLS-1$

					final Ref ref = head;
					final String remoteName = "orignial"; //$NON-NLS-1$

					boolean created = workdir.exists();
					if (!created)
						created = workdir.mkdirs();

					if (!created || !workdir.isDirectory()) {
						final String errorMessage = NLS.bind(
								UIText.GitCloneWizard_errorCannotCreate,
								workdir.getPath());
						Status status = new Status(IStatus.ERROR,
								Activator.getPluginId(), 0, errorMessage, null);
						// ErrorDialog
						// .openError(getShell(), getWindowTitle(),
						// UIText.GitCloneWizard_failed,status);
						// let's give user a chance to fix this minor problem
						return status;
					}

					int timeout = Activator.getDefault().getPreferenceStore()
							.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
					final CloneOperation op = new CloneOperation(uri, true,
							selectedBranches, workdir,
							ref != null ? ref.getName() : null, remoteName,
							timeout);

					// UserPasswordCredentials credentials =
					// cloneSource.getCredentials();
					// if (credentials != null)
					// op.setCredentialsProvider(new
					// UsernamePasswordCredentialsProvider(
					// credentials.getUser(), credentials.getPassword()));

					// alreadyClonedInto = workdir.getPath();

					runAsJob(uri, op);
				}
				return Status.OK_STATUS;
			}
		};

		job.schedule();

		return true;
	}

	/**
	 * @param uri
	 * @return list of ref.
	 */
	public List<Ref> getAvailableBranches(URIish uri) {
		final ListRemoteOperation listRemoteOp;

		try {
			final Repository db = new FileRepository(new File("/tmp")); //$NON-NLS-1$
			int timeout = Activator.getDefault().getPreferenceStore()
					.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
			listRemoteOp = new ListRemoteOperation(db, uri, timeout);

			Job job = new Job("Retrieving remote branches") { //$NON-NLS-1$

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						listRemoteOp.run(monitor);
					} catch (InvocationTargetException e) {
						return new Status(IStatus.ERROR,
								Activator.getPluginId(), "Error");//$NON-NLS-1$
					} catch (InterruptedException e) {
						return Status.CANCEL_STATUS;
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
			job.join();

			final Ref idHEAD = listRemoteOp.getRemoteRef(Constants.HEAD);
			head = null;
			List<Ref> availableRefs = new ArrayList<Ref>();
			for (final Ref r : listRemoteOp.getRemoteRefs()) {
				final String n = r.getName();
				if (!n.startsWith(Constants.R_HEADS))
					continue;
				availableRefs.add(r);
				if (idHEAD == null || head != null)
					continue;
				if (r.getObjectId().equals(idHEAD.getObjectId()))
					head = r;
			}
			Collections.sort(availableRefs, new Comparator<Ref>() {
				public int compare(final Ref o1, final Ref o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			if (idHEAD != null && head == null) {
				head = idHEAD;
				availableRefs.add(0, idHEAD);
			}
			return availableRefs;
		} catch (IOException e) {
			// Ignore the error
		} catch (InterruptedException e) {
			// Ignore the error
		}
		return null;
	}

	boolean performClone() {
		final URIish uri = cloneSource.getSelection().getURI();
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

		int timeout = Activator.getDefault().getPreferenceStore().getInt(
				UIPreferences.REMOTE_CONNECTION_TIMEOUT);
		final CloneOperation op = new CloneOperation(uri, allSelected,
				selectedBranches, workdir, ref != null ? ref.getName() : null,
				remoteName, timeout);
		if (gerritConfiguration.configureGerrit())
			doGerritConfiguration(remoteName, op);
		UserPasswordCredentials credentials = cloneSource.getCredentials();
		if (credentials != null)
			op.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
					credentials.getUser(), credentials.getPassword()));

		alreadyClonedInto = workdir.getPath();

		cloneSource.saveUriInPrefs();
		if (!callerRunsCloneOperation)
			runAsJob(uri, op);
		else
			cloneOperation = op;
		return true;
	}

	private void doGerritConfiguration(final String remoteName,
			final CloneOperation op) {
		String gerritBranch = gerritConfiguration.getBranch();
		URIish pushURI = gerritConfiguration.getURI();
		if (gerritBranch != null && gerritBranch.length() > 0) {
			ConfigurePushAfterCloneTask push = new ConfigurePushAfterCloneTask(remoteName,
					"HEAD:refs/for/" + gerritBranch, pushURI); //$NON-NLS-1$
			op.addPostCloneTask(push);
		}
		op.addPostCloneTask(new SetChangeIdTask(true));
	}

	/**
	 * @param container
	 */
	public void runCloneOperation(IWizardContainer container) {
		try {
				container.run(true, true,
						new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor)
									throws InvocationTargetException,
									InterruptedException {
								executeCloneOperation(cloneOperation, monitor);
							}
						});
			} catch (InvocationTargetException e) {
				Activator.handleError(UIText.GitCloneWizard_failed,
						e.getCause(), true);
			} catch (InterruptedException e) {
				// nothing to do
			}
	}

	private void runAsJob(final URIish uri, final CloneOperation op) {
		final Job job = new Job(NLS.bind(UIText.GitCloneWizard_jobName,
				uri.toString())) {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {
					return executeCloneOperation(op, monitor);
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
				if (family.equals(JobFamilies.CLONE))
					return true;
				return super.belongsTo(family);
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private IStatus executeCloneOperation(final CloneOperation op,
			final IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {

		final RepositoryUtil util = Activator.getDefault()
				.getRepositoryUtil();

		op.run(monitor);
		util.addConfiguredRepository(op.getGitDir());
		return Status.OK_STATUS;
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (repositorySelection.isSingleRepo()) {
			return super.getNextPage(page);
		} else {
			return null;
		}
	}
}
