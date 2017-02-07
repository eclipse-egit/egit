/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza <dariusz@luksza.org>
 *    Steffen Pingel (Tasktop Technologies) - fixes for bug 352253
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 499482
 *    Wim Jongman <wim.jongman@remainsoftware.com> - Bug 509878
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.ui.IBranchNameProvider;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.egit.ui.internal.components.UpstreamConfigComponent;
import org.eclipse.egit.ui.internal.dialogs.AbstractBranchSelectionDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Allows to create a new local branch based on another branch or commit.
 * <p>
 * The source can be selected using a branch selection dialog.
 * <p>
 * The user can select a strategy for configuring "Pull". The default as read
 * from the repository's autosetupmerge and autosetuprebase configuration is
 * suggested initially.
 */
class CreateBranchPage extends WizardPage {

	private static final String BRANCH_NAME_PROVIDER_ID = "org.eclipse.egit.ui.branchNameProvider"; //$NON-NLS-1$

	/**
	 * Get proposed target branch name for given source branch name
	 *
	 * @param sourceName
	 * @return target name
	 */
	public String getProposedTargetName(String sourceName) {
		if (sourceName == null)
			return null;

		if (sourceName.startsWith(Constants.R_REMOTES))
			return myRepository.shortenRemoteBranchName(sourceName);

		if (sourceName.startsWith(Constants.R_TAGS))
			return sourceName.substring(Constants.R_TAGS.length()) + "-branch"; //$NON-NLS-1$

		return ""; //$NON-NLS-1$
	}

	private final Repository myRepository;

	private final IInputValidator myValidator;

	private final String myBaseRef;

	private final RevCommit myBaseCommit;

	private Text nameText;

	/**
	 * Whether the contents of {@code nameText} is a suggestion or was entered by the user.
	 */
	private boolean nameIsSuggestion;

	private Button checkout;

	private BranchRebaseMode upstreamConfig;

	private UpstreamConfigComponent upstreamConfigComponent;

	private Label sourceIcon;

	private StyledText sourceNameLabel;

	private String sourceRefName = ""; //$NON-NLS-1$

	private final LocalResourceManager resourceManager = new LocalResourceManager(
			JFaceResources.getResources());

	private BranchNormalizer branchNormalizer = new BranchNormalizer();

	/**
	 * Constructs this page.
	 * <p>
	 * If a base branch is provided, the drop down will be selected accordingly
	 *
	 * @param repo
	 *            the repository
	 * @param baseRef
	 *            the branch or tag to base the new branch on, may be null
	 */
	public CreateBranchPage(Repository repo, Ref baseRef) {
		super(CreateBranchPage.class.getName());
		this.myRepository = repo;
		if (baseRef != null) {
			this.myBaseRef = baseRef.getName();
		} else {
			this.myBaseRef = null;
		}
		this.myBaseCommit = null;
		this.myValidator = ValidationUtils.getRefNameInputValidator(
				myRepository, Constants.R_HEADS, false);
		if (baseRef != null) {
			this.upstreamConfig = CreateLocalBranchOperation
					.getDefaultUpstreamConfig(repo, baseRef.getName());
		} else {
			this.upstreamConfig = null;
		}
		setTitle(UIText.CreateBranchPage_Title);
		setMessage(UIText.CreateBranchPage_ChooseBranchAndNameMessage);
	}

	/**
	 * Constructs this page.
	 * <p>
	 * If a base branch is provided, the drop down will be selected accordingly
	 *
	 * @param repo
	 *            the repository
	 * @param baseCommit
	 *            the commit to base the new branch on, may be null
	 */
	public CreateBranchPage(Repository repo, RevCommit baseCommit) {
		super(CreateBranchPage.class.getName());
		this.myRepository = repo;
		this.myBaseRef = null;
		this.myBaseCommit = baseCommit;
		this.myValidator = ValidationUtils.getRefNameInputValidator(
				myRepository, Constants.R_HEADS, false);
		this.upstreamConfig = null;
		setTitle(UIText.CreateBranchPage_Title);
		setMessage(UIText.CreateBranchPage_ChooseNameMessage);
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(4, false));

		Label sourceLabel = new Label(main, SWT.NONE);
		sourceLabel.setText(UIText.CreateBranchPage_SourceLabel);
		sourceLabel.setToolTipText(UIText.CreateBranchPage_SourceTooltip);

		sourceIcon = new Label(main, SWT.NONE);
		sourceIcon.setImage(UIIcons.getImage(resourceManager, UIIcons.BRANCH));
		sourceIcon.setLayoutData(GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER).create());

		sourceNameLabel = new StyledText(main, SWT.NONE);
		sourceNameLabel.setBackground(main.getBackground());
		sourceNameLabel.setEditable(false);
		sourceNameLabel.setLayoutData(GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false).create());

		Button selectButton = new Button(main, SWT.NONE);
		selectButton.setText(UIText.CreateBranchPage_SourceSelectButton);
		selectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectSource();
			}
		});
		UIUtils.setButtonLayoutData(selectButton);

		Label nameLabel = new Label(main, SWT.NONE);
		nameLabel.setText(UIText.CreateBranchPage_BranchNameLabel);
		nameLabel.setLayoutData(GridDataFactory.fillDefaults().span(1, 1)
				.align(SWT.BEGINNING, SWT.CENTER).create());
		nameLabel.setToolTipText(UIText.CreateBranchPage_BranchNameToolTip);

		nameText = new Text(main, SWT.BORDER);
		// give focus to the nameText if label is activated using the mnemonic
		nameLabel.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				nameText.setFocus();
			}
		});

		nameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				nameIsSuggestion = false;
			}
		});
		// enable testing with SWTBot
		nameText.setData("org.eclipse.swtbot.widget.key", "BranchName"); //$NON-NLS-1$ //$NON-NLS-2$
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.applyTo(nameText);

		upstreamConfigComponent = new UpstreamConfigComponent(main, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(4, 1)
				.applyTo(upstreamConfigComponent.getContainer());

		upstreamConfigComponent
				.addUpstreamConfigSelectionListener((newConfig) -> {
					upstreamConfig = newConfig;
					checkPage();
				});

		boolean isBare = myRepository.isBare();
		checkout = new Button(main, SWT.CHECK);
		checkout.setText(UIText.CreateBranchPage_CheckoutButton);
		// most of the time, we probably will check this out
		// unless we have a bare repository which doesn't allow
		// check out at all
		checkout.setSelection(!isBare);
		checkout.setEnabled(!isBare);
		checkout.setVisible(!isBare);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(
				checkout);
		checkout.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkPage();
			}
		});

		Dialog.applyDialogFont(main);
		setControl(main);

		if (this.myBaseCommit != null)
			setSourceCommit(this.myBaseCommit);
		else if (myBaseRef != null)
			setSourceRef(myBaseRef);

		nameText.setFocus();
		// add the listeners just now to avoid unneeded checkPage()
		nameText.addModifyListener(branchNormalizer);
		nameText.addModifyListener(e -> checkPage());
	}

	@Override
	public void dispose() {
		resourceManager.dispose();
	}

	private void setSourceRef(String refName) {
		String shortName = Repository.shortenRefName(refName);
		sourceNameLabel.setText(shortName);
		if (refName.startsWith(Constants.R_HEADS)
				|| refName.startsWith(Constants.R_REMOTES))
			sourceIcon.setImage(UIIcons.getImage(resourceManager,
					UIIcons.BRANCH));
		else if (refName.startsWith(Constants.R_TAGS))
			sourceIcon.setImage(UIIcons.getImage(resourceManager, UIIcons.TAG));
		else
			sourceIcon.setImage(UIIcons.getImage(resourceManager,
					UIIcons.CHANGESET));

		sourceRefName = refName;

		suggestBranchName(refName);
		upstreamConfig = CreateLocalBranchOperation
				.getDefaultUpstreamConfig(myRepository, refName);
		updateUpstreamComponent();
		checkPage();
	}

	private void setSourceCommit(RevCommit commit) {
		sourceNameLabel.setText(commit.abbreviate(7).name());
		sourceIcon.setImage(UIIcons
				.getImage(resourceManager, UIIcons.CHANGESET));

		sourceRefName = commit.name();

		upstreamConfig = null;
		updateUpstreamComponent();
		checkPage();
	}

	private void selectSource() {
		SourceSelectionDialog dialog = new SourceSelectionDialog(getShell(),
				myRepository, sourceRefName);
		int result = dialog.open();
		if (result == Window.OK) {
			String refName = dialog.getRefName();
			setSourceRef(refName);
			nameText.setFocus();
		}
	}

	private void updateUpstreamComponent() {
		upstreamConfigComponent.setUpstreamConfig(upstreamConfig);

		boolean showUpstreamConfig = sourceRefName.startsWith(Constants.R_HEADS)
				|| sourceRefName.startsWith(Constants.R_REMOTES);
		Composite container = upstreamConfigComponent.getContainer();
		GridData gd = (GridData) container.getLayoutData();
		if (gd.exclude == showUpstreamConfig) {
			gd.exclude = !showUpstreamConfig;
			container.setVisible(showUpstreamConfig);
			container.getParent().layout(true);
			ensurePreferredHeight(getShell());
		}
	}

	private void checkPage() {
		try {
			boolean basedOnLocalBranch = sourceRefName
					.startsWith(Constants.R_HEADS);
			if (basedOnLocalBranch && upstreamConfig != null) {
				setMessage(UIText.CreateBranchPage_LocalBranchWarningMessage,
						IMessageProvider.INFORMATION);
			} else {
				setMessage(UIText.CreateBranchPage_ChooseBranchAndNameMessage);
			}

			if (sourceRefName.length() == 0) {
				setErrorMessage(UIText.CreateBranchPage_MissingSourceMessage);
				return;
			}
			String message = this.myValidator.isValid(nameText.getText());
			if (message != null) {
				setErrorMessage(message);
				return;
			}

			setErrorMessage(null);
		} finally {
			setPageComplete(getErrorMessage() == null
					&& nameText.getText().length() > 0);
		}
	}

	public String getBranchName() {
		return nameText.getText();
	}

	public boolean checkoutNewBranch() {
		return checkout.getSelection();
	}

	/**
	 * @param newRefName
	 * @param checkoutNewBranch
	 * @param monitor
	 * @throws CoreException
	 * @throws IOException
	 */
	public void createBranch(String newRefName, boolean checkoutNewBranch,
			IProgressMonitor monitor)
			throws CoreException, IOException {
		SubMonitor progress = SubMonitor.convert(monitor,
				checkoutNewBranch ? 2 : 1);
		progress.setTaskName(UIText.CreateBranchPage_CreatingBranchMessage);

		final CreateLocalBranchOperation cbop;

		if (myBaseCommit != null
				&& this.sourceRefName.equals(myBaseCommit.name()))
			cbop = new CreateLocalBranchOperation(myRepository, newRefName,
					myBaseCommit);
		else
			cbop = new CreateLocalBranchOperation(myRepository, newRefName,
					myRepository.findRef(this.sourceRefName),
					upstreamConfig);

		cbop.execute(progress.newChild(1));

		if (checkoutNewBranch && !progress.isCanceled()) {
			progress.setTaskName(UIText.CreateBranchPage_CheckingOutMessage);
			BranchOperationUI.checkout(myRepository, Constants.R_HEADS + newRefName)
					.run(progress.newChild(1));
		}
	}

	private void suggestBranchName(String ref) {
		if (nameText.getText().length() == 0 || nameIsSuggestion) {
			String branchNameSuggestion = getBranchNameSuggestionFromProvider();
			if (branchNameSuggestion == null)
				branchNameSuggestion = getProposedTargetName(ref);

			if (branchNameSuggestion != null) {
				nameText.setText(branchNameSuggestion);
				nameText.selectAll();
				nameIsSuggestion = true;
			}
		}
	}

	private IBranchNameProvider getBranchNameProvider() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] config = registry
				.getConfigurationElementsFor(BRANCH_NAME_PROVIDER_ID);
		if (config.length > 0) {
			Object provider;
			try {
				provider = config[0].createExecutableExtension("class"); //$NON-NLS-1$
				if (provider instanceof IBranchNameProvider)
					return (IBranchNameProvider) provider;
			} catch (Throwable e) {
				Activator.logError(
						UIText.CreateBranchPage_CreateBranchNameProviderFailed,
						e);
			}
		}
		return null;
	}

	private String getBranchNameSuggestionFromProvider() {
		final AtomicReference<String> ref = new AtomicReference<>();
		final IBranchNameProvider branchNameProvider = getBranchNameProvider();
		if (branchNameProvider != null)
			SafeRunner.run(new SafeRunnable() {
				@Override
				public void run() throws Exception {
					ref.set(branchNameProvider.getBranchNameSuggestion());
				}
			});
		return ref.get();
	}

	private static void ensurePreferredHeight(Shell shell) {
		int preferredHeight = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		Point size = shell.getSize();
		if (size.y < preferredHeight)
			shell.setSize(size.x, preferredHeight);
	}

	private static class SourceSelectionDialog extends
			AbstractBranchSelectionDialog {

		public SourceSelectionDialog(Shell parentShell, Repository repository,
				String refToMark) {
			super(parentShell, repository, refToMark, SHOW_LOCAL_BRANCHES
					| SHOW_REMOTE_BRANCHES | SHOW_TAGS | SHOW_REFERENCES
					| SELECT_CURRENT_REF | EXPAND_LOCAL_BRANCHES_NODE
					| EXPAND_REMOTE_BRANCHES_NODE);
		}

		@Override
		protected void refNameSelected(String refName) {
			setOkButtonEnabled(refName != null);
		}

		@Override
		protected String getTitle() {
			return UIText.CreateBranchPage_SourceSelectionDialogTitle;
		}

		@Override
		protected String getMessageText() {
			return UIText.CreateBranchPage_SourceSelectionDialogMessage;
		}
	}

	private final class BranchNormalizer implements ModifyListener {
		private static final String UNDERSCORE = "_"; //$NON-NLS-1$

		private String oldName = ""; //$NON-NLS-1$

		private boolean listenerActive;

		@Override
		public void modifyText(ModifyEvent e) {
			nameText.setFocus();
			if (listenerActive)
				return;
			try {
				listenerActive = true;
				normalize();
			} finally {
				listenerActive = false;
			}
		}

		private void normalize() {
			String name = nameText.getText();
			// if not pasting then allow the user to type a space
			if (!isPaste()) {
				name = name.replaceAll("\\s$", UNDERSCORE);//$NON-NLS-1$
			}
			name = Repository.normalizeBranchName(name);
			nameText.setText(name);
			nameText.setSelection(nameText.getText().length() + 1);
		}

		private boolean isPaste() {
			boolean result = Math
					.abs(oldName.length() - nameText.getText().length()) > 1;
			oldName = nameText.getText();
			return result;
		}
	}
}
