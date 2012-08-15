/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza <dariusz@luksza.org>
 *    Steffen Pingel (Tasktop Technologies) - fixes for bug 352253
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation.UpstreamConfig;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.ValidationUtils;
import org.eclipse.egit.ui.internal.branch.BranchOperationUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Allows to create a new local branch based on another branch or commit.
 * <p>
 * If the base is a branch, the source branch can be selected using a drop down.
 * <p>
 * The user can select a strategy for configuring "Pull". The default as read
 * from the repository's autosetupmerge and autosetuprebase configuration is
 * suggested initially.
 */
class CreateBranchPage extends WizardPage {

	/**
	 * Get proposed target branch name for given source branch name
	 *
	 * @param sourceName
	 * @return target name
	 */
	public static String getProposedTargetName(String sourceName) {
		if (sourceName == null)
			return null;

		if (sourceName.startsWith(Constants.R_REMOTES)) {
			String target = sourceName.substring(Constants.R_REMOTES.length());
			int postSlash = target.indexOf('/') + 1;
			if (postSlash > 0 && postSlash < target.length())
				return target.substring(postSlash);
			else
				return target;
		}

		if (sourceName.startsWith(Constants.R_TAGS))
			return sourceName.substring(Constants.R_TAGS.length());

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

	private Combo branchCombo;

	private Composite warningComposite;

	private UpstreamConfig upstreamConfig;

	private Group upstreamConfigGroup;

	private Button buttonConfigRebase;

	private Button buttonConfigMerge;

	private Button buttonConfigNone;

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
		if (baseRef != null)
			this.myBaseRef = baseRef.getName();
		else
			this.myBaseRef = null;
		this.myBaseCommit = null;
		this.myValidator = ValidationUtils.getRefNameInputValidator(
				myRepository, Constants.R_HEADS, true);
		if (baseRef != null)
			this.upstreamConfig = UpstreamConfig.getDefault(repo, baseRef.getName());
		else
			this.upstreamConfig = UpstreamConfig.NONE;
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
				myRepository, Constants.R_HEADS, true);
		this.upstreamConfig = UpstreamConfig.NONE;
		setTitle(UIText.CreateBranchPage_Title);
		setMessage(UIText.CreateBranchPage_ChooseNameMessage);
	}

	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));

		Label sourceLabel = new Label(main, SWT.NONE);
		if (this.myBaseCommit != null) {
			sourceLabel.setText(UIText.CreateBranchPage_SourceCommitLabel);
			sourceLabel
					.setToolTipText(UIText.CreateBranchPage_SourceCommitTooltip);

		} else {
			sourceLabel.setText(UIText.CreateBranchPage_SourceBranchLabel);
			sourceLabel
					.setToolTipText(UIText.CreateBranchPage_SourceBranchTooltip);
		}
		this.branchCombo = new Combo(main, SWT.READ_ONLY | SWT.DROP_DOWN);
		branchCombo.setData("org.eclipse.swtbot.widget.key", "BaseBranch"); //$NON-NLS-1$ //$NON-NLS-2$

		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(
				this.branchCombo);

		Label nameLabel = new Label(main, SWT.NONE);
		nameLabel.setText(UIText.CreateBranchPage_BranchNameLabel);

		// we visualize the prefix here
		Text prefix = new Text(main, SWT.NONE);
		prefix.setText(Constants.R_HEADS);
		prefix.setEnabled(false);

		nameText = new Text(main, SWT.BORDER);
		// give focus to the nameText if label is activated using the mnemonic
		nameLabel.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				nameText.setFocus();
			}
		});

		if (this.myBaseCommit != null) {
			this.branchCombo.add(myBaseCommit.name());
			this.branchCombo.setText(myBaseCommit.name());
			try {
				Map<String, Ref> map = myRepository.getRefDatabase().getRefs(
						Constants.R_HEADS);
				for (Entry<String, Ref> entry : map.entrySet()) {
					if (entry.getValue().getLeaf().getObjectId()
							.equals(myBaseCommit))
						this.branchCombo.add(entry.getValue().getName());
				}
				map = myRepository.getRefDatabase()
						.getRefs(Constants.R_REMOTES);
				String firstRemote = null;
				for (Entry<String, Ref> entry : map.entrySet()) {
					if (entry.getValue().getLeaf().getObjectId()
							.equals(myBaseCommit)) {
						this.branchCombo.add(entry.getValue().getName());
						if (firstRemote == null)
							firstRemote = entry.getValue().getName();
					}
				}
				if (firstRemote != null) {
					this.branchCombo.setText(firstRemote);
					suggestBranchName(firstRemote);
				}
			} catch (IOException e) {
				// bad luck, we can't extend the drop down; let's log an error
				Activator.logError(
						"Exception while trying to find Refs for Commit", e); //$NON-NLS-1$
			}
			this.branchCombo.setEnabled(this.branchCombo.getItemCount() > 1);
		} else {
			List<String> refs = new ArrayList<String>();
			RefDatabase refDatabase = myRepository.getRefDatabase();
			try {
				for (Ref ref : refDatabase.getAdditionalRefs())
					refs.add(ref.getName());

				Set<Entry<String, Ref>> entrys = refDatabase.getRefs(
						RefDatabase.ALL).entrySet();
				for (Entry<String, Ref> ref : entrys)
					refs.add(ref.getValue().getName());
			} catch (IOException e1) {
				// ignore here
			}

			Collections.sort(refs, CommonUtils.STRING_ASCENDING_COMPARATOR);
			for (String refName : refs)
				this.branchCombo.add(refName);

			// select the current branch in the drop down
			if (myBaseRef != null)
				this.branchCombo.setText(myBaseRef);
		}

		this.branchCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String ref = branchCombo.getText();
				suggestBranchName(ref);
				upstreamConfig = UpstreamConfig.getDefault(myRepository, ref);
				checkPage();
			}
		});

		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				nameIsSuggestion = false;
			}
		});
		// enable testing with SWTBot
		nameText.setData("org.eclipse.swtbot.widget.key", "BranchName"); //$NON-NLS-1$ //$NON-NLS-2$
		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameText);

		// when the new branch is based on another branch, we offer to
		// configure the upstream in the configuration
		// ([branch][<name>][merge/rebase])
		upstreamConfigGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		upstreamConfigGroup
				.setToolTipText(UIText.CreateBranchPage_PullStrategyTooltip);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(
				upstreamConfigGroup);
		upstreamConfigGroup
				.setText(UIText.CreateBranchPage_PullStrategyGroupHeader);
		upstreamConfigGroup.setLayout(new GridLayout(1, false));

		warningComposite = new Composite(upstreamConfigGroup, SWT.NONE);
		warningComposite.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(
				warningComposite);

		CLabel warningLabel = new CLabel(warningComposite, SWT.NONE);
		warningLabel.setText(UIText.CreateBranchPage_LocalBranchWarningText);
		warningLabel.setToolTipText(UIText.CreateBranchPage_LocalBranchWarningTooltip);
		warningLabel.setImage(PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJS_INFO_TSK));

		buttonConfigRebase = new Button(upstreamConfigGroup, SWT.RADIO);
		buttonConfigRebase.setText(UIText.CreateBranchPage_RebaseRadioButton);
		buttonConfigRebase.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (buttonConfigRebase.getSelection())
					upstreamConfig = UpstreamConfig.REBASE;
			}
		});
		buttonConfigRebase
				.setToolTipText(UIText.CreateBranchPage_PullRebaseTooltip);

		buttonConfigMerge = new Button(upstreamConfigGroup, SWT.RADIO);
		buttonConfigMerge.setText(UIText.CreateBranchPage_MergeRadioButton);
		buttonConfigMerge.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (buttonConfigMerge.getSelection())
					upstreamConfig = UpstreamConfig.MERGE;
			}
		});
		buttonConfigMerge
				.setToolTipText(UIText.CreateBranchPage_PullMergeTooltip);

		buttonConfigNone = new Button(upstreamConfigGroup, SWT.RADIO);
		buttonConfigNone.setText(UIText.CreateBranchPage_NoneRadioButton);
		buttonConfigNone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (buttonConfigNone.getSelection())
					upstreamConfig = UpstreamConfig.NONE;
			}
		});
		buttonConfigNone
				.setToolTipText(UIText.CreateBranchPage_PullNoneTooltip);

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
		nameText.setFocus();
		suggestBranchName(myBaseRef);
		checkPage();
		// add the listener just now to avoid unneeded checkPage()
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});
	}

	private void checkPage() {
		try {
			boolean layoutChanged = false;

			GridData gd = (GridData) warningComposite.getLayoutData();
			if (gd.exclude != !branchCombo.getText().startsWith(Constants.R_HEADS)) {
				gd.exclude = !branchCombo.getText().startsWith(Constants.R_HEADS);
				warningComposite.setVisible(!gd.exclude);
				layoutChanged = true;
			}

			warningComposite.getParent().getParent().layout(true);

			boolean showRebase = !branchCombo.getText().startsWith(Constants.R_TAGS) && !ObjectId.isId(branchCombo.getText());
			gd = (GridData) upstreamConfigGroup.getLayoutData();
			if (gd.exclude == showRebase) {
				gd.exclude = !showRebase;
				upstreamConfigGroup.setVisible(!gd.exclude);
				layoutChanged = true;
			}

			if (layoutChanged)
				upstreamConfigGroup.getParent().layout(true);

			if (!gd.exclude)
				buttonConfigMerge.setSelection(false);
			buttonConfigRebase.setSelection(false);
			buttonConfigNone.setSelection(false);
			switch (upstreamConfig) {
			case MERGE:
				buttonConfigMerge.setSelection(true);
				break;
			case REBASE:
				buttonConfigRebase.setSelection(true);
				break;
			case NONE:
				buttonConfigNone.setSelection(true);
				break;
			}

			if (branchCombo.getText().length() == 0) {
				setErrorMessage(UIText.CreateBranchPage_MissingSourceMessage);
				return;
			}
			if (nameText.getText().length() == 0) {
				setErrorMessage(UIText.CreateBranchPage_ChooseNameMessage);
				return;
			}
			String message = this.myValidator.isValid(nameText.getText());
			if (message != null) {
				setErrorMessage(message);
				return;
			}

			setErrorMessage(null);
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

	public String getBranchName() {
		return nameText.getText();
	}

	/**
	 * @param monitor
	 * @throws CoreException
	 * @throws IOException
	 */
	public void createBranch(IProgressMonitor monitor) throws CoreException,
			IOException {
		monitor.beginTask(UIText.CreateBranchPage_CreatingBranchMessage,
				IProgressMonitor.UNKNOWN);

		String newRefName = getBranchName();

		final CreateLocalBranchOperation cbop;

		if (myBaseCommit != null && this.branchCombo.getText().equals(myBaseCommit.name()))
			cbop = new CreateLocalBranchOperation(myRepository, newRefName,
					myBaseCommit);
		else
			cbop = new CreateLocalBranchOperation(myRepository, newRefName,
					myRepository.getRef(this.branchCombo.getText()),
					upstreamConfig);

		cbop.execute(monitor);

		if (checkout.getSelection()) {
			if (monitor.isCanceled())
				return;
			monitor.beginTask(UIText.CreateBranchPage_CheckingOutMessage,
					IProgressMonitor.UNKNOWN);
			BranchOperationUI.checkout(myRepository, Constants.R_HEADS + newRefName)
					.run(monitor);
		}
	}

	private void suggestBranchName(String ref) {
		if (nameText.getText().length() == 0 || nameIsSuggestion) {
			String branchNameSuggestion = getProposedTargetName(ref);
			if (branchNameSuggestion != null) {
				nameText.setText(branchNameSuggestion);
				nameText.selectAll();
				nameIsSuggestion = true;
			}
		}
	}
}
