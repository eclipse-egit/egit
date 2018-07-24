/*******************************************************************************
 *  Copyright (c) 2011, 2018 GitHub Inc. and others
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewContentProvider;
import org.eclipse.egit.ui.internal.repository.RepositoriesViewLabelProvider;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * Commit search dialog page class.
 */
public class CommitSearchPage extends DialogPage implements ISearchPage {

	/**
	 * ID
	 */
	public static final String ID = "org.eclipse.egit.ui.commitSearchPage"; //$NON-NLS-1$

	private static class SearchComposite extends Composite {

		/**
		 * @param parent
		 * @param style
		 */
		public SearchComposite(Composite parent, int style) {
			super(parent, style);
		}

		@Override
		public void setLayoutData(Object layoutData) {
			// Prevent search dialog from overriding the locally set data
			if (getLayoutData() == null)
				super.setLayoutData(layoutData);
		}
	}

	private static final int HISTORY_SIZE = 12;

	// Dialog store id constants
	private static final String PAGE_NAME = "GitCommitSearchPage"; //$NON-NLS-1$

	private static final String STORE_HISTORY = "HISTORY"; //$NON-NLS-1$

	private static final String STORE_HISTORY_SIZE = "HISTORY_SIZE"; //$NON-NLS-1$

	private List<CommitSearchSettings> fPreviousSearchPatterns = new ArrayList<>(
			HISTORY_SIZE);

	private boolean firstTime = true;

	private Combo patternCombo;

	private Button isCaseSensitiveButton;

	private Button isRegExButton;

	private Button searchTreeButton;

	private Button searchCommitButton;

	private Button searchParentsButton;

	private Button searchAuthorButton;

	private Button searchCommitterButton;

	private Button searchMessageButton;

	private Button searchAllBranchesButton;

	private CLabel statusLabel;

	private Group repositoryGroup;

	private CheckboxTableViewer repositoryViewer;

	private ISearchPageContainer container;

	private ContentAssistCommandAdapter patternFieldContentAssist;

	private ISearchQuery newQuery() {
		CommitSearchSettings settings = new CommitSearchSettings();
		settings.setTextPattern(patternCombo.getText());
		settings.setRegExSearch(isRegExButton.getSelection());
		settings.setCaseSensitive(isCaseSensitiveButton.getSelection());
		settings.setMatchAuthor(searchAuthorButton.getSelection());
		settings.setMatchCommitter(searchCommitterButton.getSelection());
		settings.setMatchMessage(searchMessageButton.getSelection());
		settings.setMatchCommit(searchCommitButton.getSelection());
		settings.setMatchTree(searchTreeButton.getSelection());
		settings.setMatchParents(searchParentsButton.getSelection());
		settings.setAllBranches(searchAllBranchesButton.getSelection());
		for (Object checked : repositoryViewer.getCheckedElements())
			settings.addRepository(((RepositoryNode) checked).getRepository()
					.getDirectory().getAbsolutePath());
		fPreviousSearchPatterns.add(0, settings);
		return new CommitSearchQuery(settings);
	}

	/**
	 * Perform search by running query in background
	 *
	 * @see org.eclipse.search.ui.ISearchPage#performAction()
	 */
	@Override
	public boolean performAction() {
		NewSearchUI.runQueryInBackground(newQuery());
		return true;
	}

	private String[] getPreviousSearchPatterns() {
		int size = fPreviousSearchPatterns.size();
		String[] patterns = new String[size];
		for (int i = 0; i < size; i++)
			patterns[i] = fPreviousSearchPatterns.get(i).getTextPattern();
		return patterns;
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible && patternCombo != null) {
			if (firstTime) {
				firstTime = false;
				// Set item and text here to prevent page from resizing
				patternCombo.setItems(getPreviousSearchPatterns());
				if (!initializePatternControl()) {
					patternCombo.select(0);
					handleWidgetSelected();
				}
			}
			patternCombo.setFocus();
		}
		updateOKStatus();
		super.setVisible(visible);
	}

	private void updateOKStatus() {
		boolean status = validateRegex();
		if (status)
			status = validateScope();
		if (status)
			status = validateRepositories();
		getContainer().setPerformActionEnabled(status);
	}

	private boolean validateRepositories() {
		return this.repositoryViewer.getCheckedElements().length > 0;
	}

	private boolean validateScope() {
		return this.searchAuthorButton.getSelection()
				|| this.searchCommitterButton.getSelection()
				|| this.searchMessageButton.getSelection()
				|| this.searchCommitButton.getSelection()
				|| this.searchParentsButton.getSelection()
				|| this.searchTreeButton.getSelection();
	}

	@Override
	public void createControl(Composite parent) {
		readConfiguration();

		initializeDialogUnits(parent);
		SearchComposite result = new SearchComposite(parent, SWT.NONE);
		result.setFont(parent.getFont());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(result);
		GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false)
				.applyTo(result);
		addTextPatternControls(result);
		addScopeControls(result);
		addRepositoryControl(result);
		setControl(result);
		Dialog.applyDialogFont(result);
	}

	private boolean validateRegex() {
		if (isRegExButton.getSelection()) {
			try {
				PatternUtils.createPattern(patternCombo.getText(),
						isCaseSensitiveButton.getSelection(), true);
			} catch (PatternSyntaxException e) {
				String locMessage = e.getLocalizedMessage();
				int i = 0;
				while (i < locMessage.length()
						&& "\n\r".indexOf(locMessage.charAt(i)) == -1) //$NON-NLS-1$
					i++;
				statusMessage(true, locMessage.substring(0, i));
				return false;
			}
			statusMessage(false, ""); //$NON-NLS-1$
		} else {
			statusMessage(false, UIText.CommitSearchPage_ContainingTextHint);
		}
		return true;
	}

	private void addScopeControls(Composite parent) {
		SelectionAdapter statusAdapter = new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				updateOKStatus();
			}

		};

		Group scopeArea = new Group(parent, SWT.NONE);
		scopeArea.setText(UIText.CommitSearchPage_Scope);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.applyTo(scopeArea);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(scopeArea);

		this.searchMessageButton = new Button(scopeArea, SWT.CHECK);
		this.searchMessageButton.setText(UIText.CommitSearchPage_Message);
		this.searchMessageButton.setSelection(true);
		this.searchMessageButton.addSelectionListener(statusAdapter);

		this.searchAuthorButton = new Button(scopeArea, SWT.CHECK);
		this.searchAuthorButton.setText(UIText.CommitSearchPage_Author);
		this.searchAuthorButton.setSelection(true);
		this.searchAuthorButton.addSelectionListener(statusAdapter);

		this.searchCommitterButton = new Button(scopeArea, SWT.CHECK);
		this.searchCommitterButton.setText(UIText.CommitSearchPage_Committer);
		this.searchCommitterButton.setSelection(true);
		this.searchCommitterButton.addSelectionListener(statusAdapter);

		this.searchCommitButton = new Button(scopeArea, SWT.CHECK);
		this.searchCommitButton.setText(UIText.CommitSearchPage_CommitId);
		this.searchCommitButton.setSelection(true);
		this.searchCommitButton.addSelectionListener(statusAdapter);

		this.searchTreeButton = new Button(scopeArea, SWT.CHECK);
		this.searchTreeButton.setText(UIText.CommitSearchPage_TreeId);
		this.searchTreeButton.setSelection(true);
		this.searchTreeButton.addSelectionListener(statusAdapter);

		this.searchParentsButton = new Button(scopeArea, SWT.CHECK);
		this.searchParentsButton.setText(UIText.CommitSearchPage_ParentIds);
		this.searchParentsButton.setSelection(true);
		this.searchParentsButton.addSelectionListener(statusAdapter);
	}

	private void addRepositoryControl(Composite parent) {
		repositoryGroup = new Group(parent, SWT.NONE);
		repositoryGroup.setBackgroundMode(SWT.INHERIT_DEFAULT);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1)
				.applyTo(repositoryGroup);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(repositoryGroup);

		this.repositoryViewer = CheckboxTableViewer.newCheckList(
				repositoryGroup, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL
						| SWT.BORDER);
		this.repositoryViewer
				.setLabelProvider(new RepositoriesViewLabelProvider());
		this.repositoryViewer
				.setContentProvider(new RepositoriesViewContentProvider());
		this.repositoryViewer
				.setInput(ResourcesPlugin.getWorkspace().getRoot());
		this.repositoryViewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateOKStatus();
				repositoryGroup.setText(getRepositoryText());
			}
		});
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 40)
				.applyTo(this.repositoryViewer.getControl());

		ToolBar checkBar = new ToolBar(repositoryGroup, SWT.FLAT | SWT.VERTICAL);
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.TOP)
				.grab(false, true).applyTo(checkBar);
		ToolItem checkItem = new ToolItem(checkBar, SWT.PUSH);
		checkItem.setToolTipText(UIText.CommitSearchPage_CheckAll);
		Image checkImage = UIIcons.CHECK_ALL.createImage();
		UIUtils.hookDisposal(checkItem, checkImage);
		checkItem.setImage(checkImage);
		checkItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				repositoryViewer.setAllChecked(true);
				repositoryGroup.setText(getRepositoryText());
				updateOKStatus();
			}

		});
		ToolItem uncheckItem = new ToolItem(checkBar, SWT.PUSH);
		uncheckItem.setToolTipText(UIText.CommitSearchPage_UncheckAll);
		Image uncheckImage = UIIcons.UNCHECK_ALL.createImage();
		UIUtils.hookDisposal(uncheckItem, uncheckImage);
		uncheckItem.setImage(uncheckImage);
		uncheckItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				repositoryViewer.setAllChecked(false);
				repositoryGroup.setText(getRepositoryText());
				updateOKStatus();
			}

		});

		this.searchAllBranchesButton = new Button(repositoryGroup, SWT.CHECK);
		this.searchAllBranchesButton
				.setText(UIText.CommitSearchPage_SearchAllBranches);
		GridDataFactory.swtDefaults().grab(true, false).span(2, 1)
				.applyTo(this.searchAllBranchesButton);

		repositoryGroup.setText(getRepositoryText());
	}

	private String getRepositoryText() {
		return MessageFormat.format(UIText.CommitSearchPage_Repositories,
				Integer.valueOf(repositoryViewer.getCheckedElements().length),
				Integer.valueOf(repositoryViewer.getTable().getItemCount()));
	}

	private void addTextPatternControls(Composite group) {
		// Info text
		Label label = new Label(group, SWT.NONE);
		label.setText(UIText.CommitSearchPage_ContainingText);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2,
				1));
		label.setFont(group.getFont());

		// Pattern combo
		patternCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		patternCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelected();
				updateOKStatus();
			}
		});
		// add some listeners for regex syntax checking
		patternCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateOKStatus();
			}
		});
		patternCombo.setFont(group.getFont());
		GridData data = new GridData(GridData.FILL, GridData.FILL, true, false,
				1, 1);
		data.widthHint = convertWidthInCharsToPixels(50);
		patternCombo.setLayoutData(data);

		ComboContentAdapter contentAdapter = new ComboContentAdapter();
		FindReplaceDocumentAdapterContentProposalProvider findProposer = new FindReplaceDocumentAdapterContentProposalProvider(
				true);
		patternFieldContentAssist = new ContentAssistCommandAdapter(
				patternCombo, contentAdapter, findProposer,
				ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS,
				new char[0], true);
		patternFieldContentAssist.setEnabled(false);

		isCaseSensitiveButton = new Button(group, SWT.CHECK);
		isCaseSensitiveButton.setText(UIText.CommitSearchPage_CaseSensitive);
		isCaseSensitiveButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
				false, false, 1, 1));
		isCaseSensitiveButton.setFont(group.getFont());

		// Text line which explains the special characters
		statusLabel = new CLabel(group, SWT.LEAD);
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 1, 1));
		statusLabel.setFont(group.getFont());
		statusLabel.setAlignment(SWT.LEFT);
		statusLabel.setText(UIText.CommitSearchPage_ContainingTextHint);

		// RegEx checkbox
		isRegExButton = new Button(group, SWT.CHECK);
		isRegExButton.setText(UIText.CommitSearchPage_RegularExpression);
		isRegExButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateOKStatus();

				patternFieldContentAssist.setEnabled(isRegExButton
						.getSelection());
			}
		});
		isRegExButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
				false, 1, 1));
		isRegExButton.setFont(group.getFont());
	}

	private void handleWidgetSelected() {
		int selectionIndex = patternCombo.getSelectionIndex();
		if (selectionIndex < 0
				|| selectionIndex >= fPreviousSearchPatterns.size()) {
			repositoryViewer.setAllChecked(true);
			repositoryGroup.setText(getRepositoryText());
			return;
		}

		CommitSearchSettings settings = fPreviousSearchPatterns
				.get(selectionIndex);
		if (!patternCombo.getText().equals(settings.getTextPattern()))
			return;

		isCaseSensitiveButton.setSelection(settings.isCaseSensitive());
		isRegExButton.setSelection(settings.isRegExSearch());
		patternCombo.setText(settings.getTextPattern());
		patternFieldContentAssist.setEnabled(settings.isRegExSearch());

		searchAuthorButton.setSelection(settings.isMatchAuthor());
		searchCommitButton.setSelection(settings.isMatchCommit());
		searchCommitterButton.setSelection(settings.isMatchCommitter());
		searchMessageButton.setSelection(settings.isMatchMessage());
		searchParentsButton.setSelection(settings.isMatchParents());
		searchTreeButton.setSelection(settings.isMatchTree());
		searchAllBranchesButton.setSelection(settings.isAllBranches());

		List<RepositoryNode> repositories = new LinkedList<>();
		for (String path : settings.getRepositories()) {
			File file = new File(path);
			if (file.exists())
				try {
					RepositoryNode node = new RepositoryNode(null,
							org.eclipse.egit.core.Activator.getDefault()
									.getRepositoryCache()
									.lookupRepository(file));
					repositories.add(node);
				} catch (IOException ignore) {
					// Ignore and don't check
				}
		}
		repositoryViewer.setCheckedElements(repositories.toArray());
		repositoryGroup.setText(getRepositoryText());
	}

	private boolean initializePatternControl() {
		ISelection selection = getSelection();
		if (selection instanceof ITextSelection && !selection.isEmpty()) {
			String text = ((ITextSelection) selection).getText();
			if (text != null) {
				if (isRegExButton.getSelection())
					patternCombo.setText(FindReplaceDocumentAdapter
							.escapeForRegExPattern(text));
				else
					patternCombo.setText(insertEscapeChars(text));

				return true;
			}
		}
		return false;
	}

	private String insertEscapeChars(String text) {
		if (text == null || text.length() == 0)
			return ""; //$NON-NLS-1$
		StringBuilder sbIn = new StringBuilder(text);
		BufferedReader reader = new BufferedReader(new StringReader(text));
		int lengthOfFirstLine = 0;
		try {
			String l = reader.readLine();
			if (l != null)
				lengthOfFirstLine = l.length();
		} catch (IOException ex) {
			return ""; //$NON-NLS-1$
		}
		StringBuilder sbOut = new StringBuilder(lengthOfFirstLine + 5);
		int i = 0;
		while (i < lengthOfFirstLine) {
			char ch = sbIn.charAt(i);
			if (ch == '*' || ch == '?' || ch == '\\')
				sbOut.append('\\');
			sbOut.append(ch);
			i++;
		}
		return sbOut.toString();
	}

	/**
	 * Sets the search page's container.
	 *
	 * @param container
	 *            the container to set
	 */
	@Override
	public void setContainer(ISearchPageContainer container) {
		this.container = container;
	}

	private ISearchPageContainer getContainer() {
		return this.container;
	}

	private ISelection getSelection() {
		return this.container.getSelection();
	}

	@Override
	public void dispose() {
		writeConfiguration();
		super.dispose();
	}

	/**
	 * Returns the page settings for this Text search page.
	 *
	 * @return the page settings to be used
	 */
	private IDialogSettings getDialogSettings() {
		IDialogSettings dialogSettings = Activator.getDefault()
				.getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(PAGE_NAME);
		if (section == null)
			section = dialogSettings.addNewSection(PAGE_NAME);
		return section;
	}

	/**
	 * Initializes itself from the stored page settings.
	 */
	private void readConfiguration() {
		IDialogSettings settings = getDialogSettings();
		try {
			int historySize = settings.getInt(STORE_HISTORY_SIZE);
			for (int i = 0; i < historySize; i++) {
				IDialogSettings histSettings = settings
						.getSection(STORE_HISTORY + i);
				if (histSettings != null)
					fPreviousSearchPatterns.add(CommitSearchSettings
							.create(histSettings));
			}
		} catch (NumberFormatException ignored) {
			// Ignored
		}
	}

	/**
	 * Stores it current configuration in the dialog store.
	 */
	private void writeConfiguration() {
		IDialogSettings s = getDialogSettings();
		int historySize = Math
				.min(fPreviousSearchPatterns.size(), HISTORY_SIZE);
		s.put(STORE_HISTORY_SIZE, historySize);
		for (int i = 0; i < historySize; i++)
			fPreviousSearchPatterns.get(i).store(
					s.addNewSection(STORE_HISTORY + i));
	}

	private void statusMessage(boolean error, String message) {
		statusLabel.setText(message);
		if (error)
			statusLabel.setForeground(JFaceColors.getErrorText(statusLabel
					.getDisplay()));
		else
			statusLabel.setForeground(null);
	}

}
