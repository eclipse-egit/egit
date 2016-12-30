/*******************************************************************************
 * Copyright (c) 2012, 2016 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Christian Georgi (SAP SE) - Bug 466900 (Make PushResultDialog amodal)
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 449493: Topic input
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.egit.core.internal.gerrit.GerritUtil;
import org.eclipse.egit.core.op.PushOperationSpecification;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.gerrit.GerritDialogSettings;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchCommandConstants;

/**
 * Push the current HEAD to Gerrit
 */
public class PushToGerritPage extends WizardPage {
	private static final String LAST_BRANCH_POSTFIX = ".lastBranch"; //$NON-NLS-1$

	private static final String LAST_TOPICS_POSTFIX = ".lastTopics"; //$NON-NLS-1$

	private static final String GERRIT_TOPIC_KEY = "gerritTopic"; //$NON-NLS-1$

	private static final String GERRIT_TOPIC_USE_KEY = "gerritTopicUse"; //$NON-NLS-1$

	private static final Pattern WHITESPACE = Pattern
			.compile("\\p{javaWhitespace}"); //$NON-NLS-1$

	private final Repository repository;

	private final IDialogSettings settings;

	private final String lastUriKey;

	private final String lastBranchKey;

	private Combo uriCombo;

	private Combo prefixCombo;

	private Label branchTextlabel;

	private Text branchText;

	private Button useTopic;

	private Label topicLabel;

	private Text topicText;

	private Set<String> knownRemoteRefs = new TreeSet<>(
			String.CASE_INSENSITIVE_ORDER);

	@SuppressWarnings("serial")
	private Map<String, String> topicProposals = new LinkedHashMap<String, String>(
			30, 0.75f, true) {

		private static final int TOPIC_PROPOSALS_MAXIMUM = 20;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() > TOPIC_PROPOSALS_MAXIMUM;
		}
	};

	/**
	 * @param repository
	 */
	PushToGerritPage(Repository repository) {
		super(PushToGerritPage.class.getName());
		this.repository = repository;
		setTitle(NLS.bind(UIText.PushToGerritPage_Title, Activator.getDefault()
				.getRepositoryUtil().getRepositoryName(repository)));
		setMessage(UIText.PushToGerritPage_Message);
		settings = getDialogSettings();
		lastUriKey = repository + GerritDialogSettings.LAST_URI_SUFFIX;
		lastBranchKey = repository + LAST_BRANCH_POSTFIX;
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		return GerritDialogSettings
				.getSection(GerritDialogSettings.PUSH_TO_GERRIT_SECTION);
	}

	@Override
	public void createControl(Composite parent) {
		loadKnownRemoteRefs();
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		new Label(main, SWT.NONE).setText(UIText.PushToGerritPage_UriLabel);
		uriCombo = new Combo(main, SWT.DROP_DOWN);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.applyTo(uriCombo);
		uriCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		branchTextlabel = new Label(main, SWT.NONE);

		// we visualize the prefix here
		prefixCombo = new Combo(main, SWT.READ_ONLY | SWT.DROP_DOWN);
		prefixCombo.add(GerritUtil.REFS_FOR);
		prefixCombo.add(GerritUtil.REFS_DRAFTS);
		prefixCombo.select(0);

		branchTextlabel.setText(UIText.PushToGerritPage_BranchLabel);
		branchText = new Text(main, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(branchText);
		branchText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});

		// give focus to the branchText if label is activated using the mnemonic
		branchTextlabel.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				branchText.setFocus();
				branchText.selectAll();
			}
		});
		addRefContentProposalToText(branchText);

		useTopic = new Button(main, SWT.CHECK | SWT.LEFT);
		useTopic.setText(UIText.PushToGerritPage_TopicUseLabel);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.applyTo(useTopic);
		topicLabel = new Label(main, SWT.NONE);
		topicLabel.setText(UIText.PushToGerritPage_TopicLabel);
		topicText = new Text(main, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.applyTo(topicText);
		topicText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				checkPage();
			}
		});
		topicLabel.addTraverseListener(new TraverseListener() {

			@Override
			public void keyTraversed(TraverseEvent e) {
				topicText.setFocus();
				topicText.selectAll();
			}
		});

		useTopic.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				topicText.setEnabled(useTopic.getSelection());
				checkPage();
			}
		});

		// get all available Gerrit URIs from the repository
		SortedSet<String> uris = new TreeSet<>();
		try {
			for (RemoteConfig rc : RemoteConfig.getAllRemoteConfigs(repository
					.getConfig())) {
				if (GerritUtil.isGerritPush(rc)) {
					if (rc.getURIs().size() > 0) {
						uris.add(rc.getURIs().get(0).toPrivateString());
					}
					for (URIish u : rc.getPushURIs()) {
						uris.add(u.toPrivateString());
					}
				}
			}
		} catch (URISyntaxException e) {
			Activator.handleError(e.getMessage(), e, false);
			setErrorMessage(e.getMessage());
		}
		for (String aUri : uris) {
			uriCombo.add(aUri);
		}
		selectLastUsedUri();
		setLastUsedBranch();
		initializeTopic(branchText.getText());
		addTopicProposal(topicText);
		branchText.setFocus();
		Dialog.applyDialogFont(main);
		setControl(main);
	}

	private void loadKnownRemoteRefs() {
		try {
			Set<String> remotes = repository.getRefDatabase()
					.getRefs(Constants.R_REMOTES).keySet();
			for (String remote : remotes) {
				// these are "origin/master", "origin/xxx"...
				int slashIndex = remote.indexOf('/');
				if (slashIndex > 0 && slashIndex < remote.length() - 1) {
					knownRemoteRefs.add(remote.substring(slashIndex + 1));
				}
			}
		} catch (IOException e) {
			// simply ignore, no proposals and no topic check then
		}
	}

	private void storeLastUsedUri(String uri) {
		settings.put(lastUriKey, uri.trim());
	}

	private void storeLastUsedBranch(String branch) {
		settings.put(lastBranchKey, branch.trim());
	}

	private void storeLastUsedTopic(boolean enabled, String topic,
			String branch) {
		boolean isValid = validateTopic(topic) == null;
		if (topic.equals(branch)) {
			topic = null;
		} else if (topic.isEmpty()) {
			enabled = false;
		} else if (isValid) {
			topicProposals.put(topic, null);
			settings.put(repository + LAST_TOPICS_POSTFIX, topicProposals
					.keySet().toArray(new String[topicProposals.size()]));
		}
		if (branch != null && !ObjectId.isId(branch)) {
			// Don't store on detached HEAD
			StoredConfig config = repository.getConfig();
			if (enabled) {
				config.setBoolean(ConfigConstants.CONFIG_BRANCH_SECTION, branch,
						GERRIT_TOPIC_USE_KEY, enabled);
			} else {
				config.unset(ConfigConstants.CONFIG_BRANCH_SECTION, branch,
						GERRIT_TOPIC_USE_KEY);
			}
			if (topic == null || topic.isEmpty()) {
				config.unset(ConfigConstants.CONFIG_BRANCH_SECTION, branch,
						GERRIT_TOPIC_KEY);
			} else if (isValid) {
				config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branch,
						GERRIT_TOPIC_KEY, topic);
			}
			try {
				config.save();
			} catch (IOException e) {
				Activator.logError(
						NLS.bind(UIText.PushToGerritPage_TopicSaveFailure,
								repository),
						e);
			}
		}
	}

	private void selectLastUsedUri() {
		String lastUri = settings.get(lastUriKey);
		if (lastUri != null) {
			int i = uriCombo.indexOf(lastUri);
			if (i != -1) {
				uriCombo.select(i);
				return;
			}
		}
		uriCombo.select(0);
	}

	private void setLastUsedBranch() {
		String lastBranch = settings.get(lastBranchKey);
		try {
			// use upstream if the current branch is tracking a branch
			final BranchConfig branchConfig = new BranchConfig(
					repository.getConfig(), repository.getBranch());
			final String trackedBranch = branchConfig.getMerge();
			if (trackedBranch != null) {
				lastBranch = trackedBranch.replace(Constants.R_HEADS, ""); //$NON-NLS-1$
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		if (lastBranch != null) {
			branchText.setText(lastBranch);
		}
	}

	private void initializeTopic(String remoteBranch) {
		boolean enabled = false;
		String storedTopic = null;
		String branch = null;
		try {
			branch = repository.getBranch();
			// On detached HEAD don't do anything: "Use topic" will be disabled
			// and the topic field empty.
			if (ObjectId.isId(branch)) {
				branch = null;
			}
		} catch (final IOException e) {
			Activator.logError(e.getLocalizedMessage(), e);
		}
		if (branch != null) {
			StoredConfig config = repository.getConfig();
			enabled = config.getBoolean(ConfigConstants.CONFIG_BRANCH_SECTION,
					branch, GERRIT_TOPIC_USE_KEY, false);
			storedTopic = config.getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branch,
					GERRIT_TOPIC_KEY);
		}
		if (storedTopic == null || storedTopic.isEmpty()) {
			if (branch != null && !branch.isEmpty()
					&& !branch.equals(remoteBranch)) {
				topicText.setText(branch);
			}
		} else {
			topicText.setText(storedTopic);
		}
		useTopic.setSelection(enabled);
		topicText.setEnabled(enabled);
		// Load topicProposals from settings.
		String[] proposals = settings
				.getArray(repository + LAST_TOPICS_POSTFIX);
		if (proposals != null) {
			for (int i = proposals.length - 1; i >= 0; i--) {
				if (!proposals[i].isEmpty()) {
					topicProposals.put(proposals[i], null);
				}
			}
		}
	}

	private void checkPage() {
		setErrorMessage(null);
		try {
			if (uriCombo.getText().length() == 0) {
				setErrorMessage(UIText.PushToGerritPage_MissingUriMessage);
				return;
			}
			if (branchText.getText().trim().isEmpty()) {
				setErrorMessage(UIText.PushToGerritPage_MissingBranchMessage);
				return;
			}
			if (topicText.isEnabled()) {
				setErrorMessage(validateTopic(topicText.getText().trim()));
			}
		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

	private String validateTopic(String topic) {
		if (WHITESPACE.matcher(topic).find()) {
			return UIText.PushToGerritPage_TopicHasWhitespace;
		}
		if (topic.indexOf(',') >= 0) {
			if (topic.indexOf('%') >= 0) {
				return UIText.PushToGerritPage_TopicInvalidCharacters;
			}
			String withTopic = branchText.getText().trim();
			int i = withTopic.indexOf('%');
			if (i >= 0) {
				withTopic = withTopic.substring(0, i);
			}
			withTopic += '/' + topic;
			if (knownRemoteRefs.contains(withTopic)) {
				return NLS.bind(UIText.PushToGerritPage_TopicCollidesWithBranch,
						withTopic);
			}
		}
		return null;
	}

	private String setTopicInRef(String ref, String topic) {
		String baseRef;
		String options;
		int i = ref.indexOf('%');
		if (i >= 0) {
			baseRef = ref.substring(0, i);
			options = ref.substring(i + 1);
			options = options.replaceAll("topic=[^,]*", ""); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			baseRef = ref;
			options = ""; //$NON-NLS-1$
		}
		if (topic.indexOf(',') >= 0) {
			// Cannot use %topic=, since Gerrit splits on commas
			baseRef += '/' + topic;
		} else {
			if (!options.isEmpty()) {
				options += ',';
			}
			options += "topic=" + topic; //$NON-NLS-1$
		}
		if (!options.isEmpty()) {
			return baseRef + '%' + options;
		}
		return baseRef;
	}

	void doPush() {
		try {
			URIish uri = new URIish(uriCombo.getText());
			Ref currentHead = repository.exactRef(Constants.HEAD);
			String ref = prefixCombo.getItem(prefixCombo.getSelectionIndex())
					+ branchText.getText().trim();
			if (topicText.isEnabled()) {
				ref = setTopicInRef(ref, topicText.getText().trim());
			}
			RemoteRefUpdate update = new RemoteRefUpdate(repository,
					currentHead, ref, false, null, null);
			PushOperationSpecification spec = new PushOperationSpecification();

			spec.addURIRefUpdates(uri, Arrays.asList(update));
			final PushOperationUI op = new PushOperationUI(repository, spec,
					false);
			storeLastUsedUri(uriCombo.getText());
			storeLastUsedBranch(branchText.getText());
			storeLastUsedTopic(topicText.isEnabled(),
					topicText.getText().trim(), repository.getBranch());
			op.start();
		} catch (URISyntaxException | IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
	}

	private void addTopicProposal(Text textField) {
		if (topicProposals.isEmpty()) {
			return;
		}
		KeyStroke stroke = UIUtils.getKeystrokeOfBestActiveBindingFor(
				IWorkbenchCommandConstants.EDIT_CONTENT_ASSIST);
		if (stroke != null) {
			UIUtils.addBulbDecorator(textField,
					NLS.bind(
							UIText.PushToGerritPage_TopicContentProposalHoverText,
							stroke.format()));
		}
		String[] recentTopics = topicProposals.keySet()
				.toArray(new String[topicProposals.size()]);
		Arrays.sort(recentTopics, CommonUtils.STRING_ASCENDING_COMPARATOR);
		SimpleContentProposalProvider proposalProvider = new SimpleContentProposalProvider(
				recentTopics);
		proposalProvider.setFiltering(true);
		ContentProposalAdapter adapter = new ContentProposalAdapter(textField,
				new TextContentAdapter(), proposalProvider, stroke, null);
		adapter.setProposalAcceptanceStyle(
				ContentProposalAdapter.PROPOSAL_REPLACE);
	}

	private void addRefContentProposalToText(final Text textField) {
		UIUtils.<String> addContentProposalToText(textField,
				() -> knownRemoteRefs, (pattern, refName) -> {
					if (pattern != null
							&& !pattern.matcher(refName).matches()) {
						return null;
					}
					return new ContentProposal(refName);
				}, UIText.PushToGerritPage_ContentProposalStartTypingText,
				UIText.PushToGerritPage_ContentProposalHoverText);
	}
}
