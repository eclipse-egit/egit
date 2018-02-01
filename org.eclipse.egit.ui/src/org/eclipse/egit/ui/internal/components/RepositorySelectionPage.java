/*******************************************************************************
 * Copyright (C) 2007, 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.UIUtils.IPreviousValueProposalHandler;
import org.eclipse.egit.ui.internal.SecureStoreUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.clone.GitUrlChecker;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo.IRemoteSelectionListener;
import org.eclipse.egit.ui.internal.components.RemoteSelectionCombo.SelectionType;
import org.eclipse.egit.ui.internal.provisional.wizards.GitRepositoryInfo;
import org.eclipse.egit.ui.internal.provisional.wizards.IRepositorySearchResult;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page that allows the user entering the location of a remote repository
 * by specifying URL manually or selecting a preconfigured remote repository.
 */
public class RepositorySelectionPage extends WizardPage implements IRepositorySearchResult {

	private static final String EMPTY_STRING = "";  //$NON-NLS-1$

	private final static String USED_URIS_PREF = "RepositorySelectionPage.UsedUris"; //$NON-NLS-1$

	private final List<RemoteConfig> configuredRemotes;

	private final boolean sourceSelection;

	private final String presetUri;

	private Group authGroup;

	private Text uriText;

	private Text hostText;

	private Text pathText;

	private Text userText;

	private Text passText;

	private Button storeCheckbox;

	private Combo scheme;

	private Text portText;

	private int eventDepth;

	private URIish uri;

	private RemoteConfig remoteConfig;

	private RepositorySelection selection;

	private Composite remotePanel;

	private Button remoteButton;

	private RemoteSelectionCombo remoteCombo;

	private Composite uriPanel;

	private Button uriButton;

	private IPreviousValueProposalHandler uriProposalHandler;

	private String user = EMPTY_STRING;

	private String password = EMPTY_STRING;

	private boolean storeInSecureStore;

	private String helpContext = null;

	/**
	 * Transport protocol abstraction
	 *
	 * TODO rework this to become part of JGit API
	 */
	public static class Protocol {
		/** Ordered list of all protocols **/
		private static final TreeMap<String, Protocol> protocols = new TreeMap<>();

		/** Git native transfer */
		public static final Protocol GIT = new Protocol("git", //$NON-NLS-1$
				UIText.RepositorySelectionPage_tip_git, true, true, false);

		/** Git over SSH */
		public static final Protocol SSH = new Protocol("ssh", //$NON-NLS-1$
				UIText.RepositorySelectionPage_tip_ssh, true, true, true) {
			@Override
			public boolean handles(URIish uri) {
				if (!uri.isRemote())
					return false;
				final String scheme = uri.getScheme();
				if (getDefaultScheme().equals(scheme))
					return true;
				if ("ssh+git".equals(scheme)) //$NON-NLS-1$
					return true;
				if ("git+ssh".equals(scheme)) //$NON-NLS-1$
					return true;
				if (scheme == null && uri.getHost() != null
						&& uri.getPath() != null)
					return true;
				return false;
			}
		};

		/** Secure FTP */
		public static final Protocol SFTP = new Protocol("sftp", //$NON-NLS-1$
				UIText.RepositorySelectionPage_tip_sftp, true, true, true);

		/** HTTP */
		public static final Protocol HTTP = new Protocol("http", //$NON-NLS-1$
				UIText.RepositorySelectionPage_tip_http, true, true, true);

		/** Secure HTTP */
		public static final Protocol HTTPS = new Protocol("https", //$NON-NLS-1$
				UIText.RepositorySelectionPage_tip_https, true, true, true);

		/** FTP */
		public static final Protocol FTP = new Protocol("ftp", //$NON-NLS-1$
				UIText.RepositorySelectionPage_tip_ftp, true, true, true);

		/** Local repository */
		public static final Protocol FILE = new Protocol("file", //$NON-NLS-1$
				UIText.RepositorySelectionPage_tip_file, false, false, false) {
			@Override
			public boolean handles(URIish uri) {
				if (getDefaultScheme().equals(uri.getScheme()))
					return true;
				if (uri.getHost() != null || uri.getPort() > 0
						|| uri.getUser() != null || uri.getPass() != null
						|| uri.getPath() == null)
					return false;
				if (uri.getScheme() == null)
					return FS.DETECTED
							.resolve(new File("."), uri.getPath()).isDirectory(); //$NON-NLS-1$
				return false;
			}
		};

		private final String defaultScheme;

		private final String tooltip;

		private final boolean hasHost;

		private final boolean hasPort;

		private final boolean canAuthenticate;

		private Protocol(String defaultScheme, String tooltip,
				boolean hasHost, boolean hasPort, boolean canAuthenticate) {
			this.defaultScheme = defaultScheme;
			this.tooltip = tooltip;
			this.hasHost = hasHost;
			this.hasPort = hasPort;
			this.canAuthenticate = canAuthenticate;
			protocols.put(defaultScheme, this);
		}

		/**
		 * @param uri
		 *            URI to match against this protocol
		 * @return {@code true} if the uri is handled by this protocol
		 */
		public boolean handles(URIish uri) {
			return getDefaultScheme().equals(uri.getScheme());
		}

		/**
		 * @return the default protocol scheme
		 */
		public String getDefaultScheme() {
			return defaultScheme;
		}

		/**
		 * @return the tooltip text describing the protocol
		 */
		public String getTooltip() {
			return tooltip;
		}

		/**
		 * @return true if protocol has host segment
		 */
		public boolean hasHost() {
			return hasHost;
		}

		/**
		 * @return true if protocol has port
		 */
		public boolean hasPort() {
			return hasPort;
		}

		/**
		 * @return true if protocol can authenticate
		 */
		public boolean canAuthenticate() {
			return canAuthenticate;
		}

		/**
		 * @return all protocols
		 */
		public static Protocol[] values() {
			return protocols.values().toArray(new Protocol[protocols.size()]);
		}

		/**
		 * Lookup protocol supporting given default URL scheme
		 *
		 * @param scheme
		 *            default scheme to lookup protocol for
		 * @return protocol matching scheme or null
		 */
		public static Protocol fromDefaultScheme(String scheme) {
			return protocols.get(scheme);
		}

		/**
		 * Lookup protocol handling given URI
		 *
		 * @param uri URI to lookup protocol for
		 * @return protocol handling this URI
		 */
		public static Protocol fromUri(URIish uri) {
			for (Protocol p : protocols.values())
				if (p.handles(uri))
					return p;
			return null;
		}
	}

	/**
	 * Create repository selection page, allowing user specifying URI or
	 * (optionally) choosing from preconfigured remotes list.
	 * <p>
	 * Wizard page is created without image, just with text description.
	 *
	 * @param sourceSelection
	 *            true if dialog is used for source selection; false otherwise
	 *            (destination selection). This indicates appropriate text
	 *            messages.
	 * @param configuredRemotes
	 *            list of configured remotes that user may select as an
	 *            alternative to manual URI specification. Remotes appear in
	 *            given order in GUI, with
	 *            {@value Constants#DEFAULT_REMOTE_NAME} as the default choice.
	 *            List may be null or empty - no remotes configurations appear
	 *            in this case. Note that the provided list may be changed by
	 *            this constructor.
	 * @param presetUri
	 *            the pre-set URI, may be null
	 */
	public RepositorySelectionPage(final boolean sourceSelection,
			final List<RemoteConfig> configuredRemotes, String presetUri) {

		super(RepositorySelectionPage.class.getName());

		this.uri = new URIish();
		this.sourceSelection = sourceSelection;

		String preset = presetUri;
		if (presetUri == null) {
			Clipboard clipboard = new Clipboard(Display.getCurrent());
			String text = (String) clipboard
					.getContents(TextTransfer.getInstance());
			if (text != null) {
				text = GitUrlChecker.sanitizeAsGitUrl(text);
				if (GitUrlChecker.isValidGitUrl(text)) {
					preset = text;
				}
			}
			clipboard.dispose();
		}
		this.presetUri = preset;

		this.configuredRemotes = getUsableConfigs(configuredRemotes);

		selection = RepositorySelection.INVALID_SELECTION;

		if (sourceSelection) {
			setTitle(UIText.RepositorySelectionPage_sourceSelectionTitle);
			setDescription(UIText.RepositorySelectionPage_sourceSelectionDescription);
		} else {
			setTitle(UIText.RepositorySelectionPage_destinationSelectionTitle);
			setDescription(UIText.RepositorySelectionPage_destinationSelectionDescription);
		}

		storeInSecureStore = getPreferenceStore().getBoolean(
				UIPreferences.CLONE_WIZARD_STORE_SECURESTORE);
	}

	/**
	 * Create repository selection page, allowing user specifying URI, with no
	 * preconfigured remotes selection.
	 *
	 * @param sourceSelection
	 *            true if dialog is used for source selection; false otherwise
	 *            (destination selection). This indicates appropriate text
	 *            messages.
	 * @param presetUri
	 *            the pre-set URI, may be null
	 */
	public RepositorySelectionPage(final boolean sourceSelection,
			String presetUri) {
		this(sourceSelection, null, presetUri);
	}

	/**
	 * No args constructor; needed because the page is provided by the extension
	 * point {@code org.eclipse.egit.ui.cloneSourceProvider}
	 */
	public RepositorySelectionPage() {
		this(true, null);
	}

	/**
	 * @return repository selection representing current page state.
	 */
	public RepositorySelection getSelection() {
		return selection;
	}

	/**
	 * Compare current repository selection set by user to provided one.
	 *
	 * @param s
	 *            repository selection to compare.
	 * @return true if provided selection is equal to current page selection,
	 *         false otherwise.
	 */
	public boolean selectionEquals(final RepositorySelection s) {
		return selection.equals(s);
	}

	@Override
	public void createControl(final Composite parent) {
		final Composite panel = new Composite(parent, SWT.NULL);
		panel.setLayout(new GridLayout());

		if (configuredRemotes != null)
			createRemotePanel(panel);
		else
			createRemoteNamePanel(panel);

		createUriPanel(panel);

		if (presetUri != null)
			updateFields(presetUri);

		updateRemoteAndURIPanels();
		Dialog.applyDialogFont(panel);
		setControl(panel);

		checkPage();
	}

	private void createRemotePanel(final Composite parent) {
		remoteButton = new Button(parent, SWT.RADIO);
		remoteButton
				.setText(UIText.RepositorySelectionPage_configuredRemoteChoice
						+ ":"); //$NON-NLS-1$
		remoteButton.setSelection(true);

		remotePanel = new Composite(parent, SWT.NULL);
		remotePanel.setLayout(new GridLayout());
		final GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = SWT.FILL;
		remotePanel.setLayoutData(gd);

		SelectionType selectionType = sourceSelection ? SelectionType.FETCH : SelectionType.PUSH;
		remoteCombo = new RemoteSelectionCombo(remotePanel, SWT.NULL, selectionType);
		remoteConfig = remoteCombo.setItems(configuredRemotes);
		remoteCombo.addRemoteSelectionListener(new IRemoteSelectionListener() {
			@Override
			public void remoteSelected(RemoteConfig rc) {
				remoteConfig = rc;
				checkPage();
			}
		});
	}

	/**
	 *
	 * @param panel
	 */
	protected void createRemoteNamePanel(Composite panel) {
		// Only used by subclass
	}

	private void createUriPanel(final Composite parent) {
		if (configuredRemotes != null) {
			uriButton = new Button(parent, SWT.RADIO);
			uriButton.setText(UIText.RepositorySelectionPage_uriChoice + ":"); //$NON-NLS-1$
			uriButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					// occurs either on selection or unselection event
					updateRemoteAndURIPanels();
					checkPage();
				}
			});
		}

		uriPanel = new Composite(parent, SWT.NULL);
		uriPanel.setLayout(new GridLayout());
		final GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = SWT.FILL;
		uriPanel.setLayoutData(gd);

		createLocationGroup(uriPanel);
		createConnectionGroup(uriPanel);
		authGroup = createAuthenticationGroup(uriPanel);
	}

	private void createLocationGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.RepositorySelectionPage_groupLocation);

		g.setLayout(new GridLayout(3, false));

		newLabel(g, UIText.RepositorySelectionPage_promptURI + ":"); //$NON-NLS-1$
		uriText = new Text(g, SWT.BORDER);

		if (presetUri != null) {
			uriText.setText(presetUri);
			uriText.selectAll();
		}

		uriText.setLayoutData(createFieldGridData());
		uriText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				updateFields(uriText.getText());
			}
		});

		uriProposalHandler = UIUtils.addPreviousValuesContentProposalToText(
				uriText, USED_URIS_PREF);

		Button browseButton = new Button(g, SWT.NULL);
		browseButton.setText(UIText.RepositorySelectionPage_BrowseLocalFile);
		browseButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent evt) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				if (sourceSelection) {
					dialog.setMessage(
							UIText.RepositorySelectionPage_sourceSelectionTitle);
				} else {
					dialog.setMessage(
							UIText.RepositorySelectionPage_destinationSelectionTitle);
				}
				// if a file was selected before, let's try to open
				// the directory dialog on the same directory
				if (!uriText.getText().isEmpty()) {
					try {
						// first we try if this is a simple file name
						File testFile = new File(uriText.getText());
						if (testFile.exists()) {
							dialog.setFilterPath(testFile.getPath());
						} else {
							// this could still be a file URIish
							URIish testUri = new URIish(uriText.getText());
							if (Protocol.FILE.defaultScheme
									.equals(testUri.getScheme())) {
								testFile = new File(testUri.getPath());
								if (testFile.exists()) {
									dialog.setFilterPath(testFile.getPath());
								}
							}
						}
					} catch (IllegalArgumentException | URISyntaxException e) {
						// ignore here, we just' don't set the directory in the
						// browser
					}
				}
				// if nothing else, we start the search from the default folder for repositories
				String filterPath = dialog.getFilterPath();
				if (filterPath == null || filterPath.isEmpty()) {
					dialog.setFilterPath(
							RepositoryUtil.getDefaultRepositoryDir());
				}
				String result = dialog.open();
				if (result != null) {
					uriText.setText("file:///" + result); //$NON-NLS-1$
				}
			}

		});

		newLabel(g, UIText.RepositorySelectionPage_promptHost + ":"); //$NON-NLS-1$
		hostText = new Text(g, SWT.BORDER);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(hostText);
		hostText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				setURI(uri.setHost(nullString(hostText.getText())));
			}
		});

		newLabel(g, UIText.RepositorySelectionPage_promptPath + ":"); //$NON-NLS-1$
		pathText = new Text(g, SWT.BORDER);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(pathText);
		pathText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				setURI(uri.setPath(nullString(pathText.getText())));
			}
		});

	}

	private Group createAuthenticationGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.RepositorySelectionPage_groupAuthentication);

		newLabel(g, UIText.RepositorySelectionPage_promptUser + ":"); //$NON-NLS-1$
		userText = new Text(g, SWT.BORDER);
		userText.setLayoutData(createFieldGridData());
		userText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				Protocol protocol = getProtocol();
				if (protocol != Protocol.HTTP && protocol != Protocol.HTTPS)
					setURI(uri.setUser(nullString(userText.getText())));
				user = userText.getText();
			}
		});

		newLabel(g, UIText.RepositorySelectionPage_promptPassword + ":"); //$NON-NLS-1$
		passText = new Text(g, SWT.BORDER | SWT.PASSWORD);
		passText.setLayoutData(createFieldGridData());
		passText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				setURI(uri.setPass(null));
				password = passText.getText();
			}
		});

		storeCheckbox = new Button(g, SWT.CHECK);
		storeCheckbox
				.setText(UIText.RepositorySelectionPage_storeInSecureStore);
		storeCheckbox.setSelection(storeInSecureStore);
		storeCheckbox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				storeInSecureStore = storeCheckbox.getSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				storeInSecureStore = storeCheckbox.getSelection();
			}
		});

		return g;
	}

	private void createConnectionGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.RepositorySelectionPage_groupConnection);

		newLabel(g, UIText.RepositorySelectionPage_promptScheme + ":"); //$NON-NLS-1$
		scheme = new Combo(g, SWT.DROP_DOWN | SWT.READ_ONLY);
		for (Protocol p : Protocol.values())
			scheme.add(p.getDefaultScheme());
		scheme.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final int idx = scheme.getSelectionIndex();
				if (idx < 0) {
					setURI(uri.setScheme(null));
					scheme.setToolTipText(EMPTY_STRING);
				} else {
					setURI(uri.setScheme(nullString(scheme.getItem(idx))));
					scheme.setToolTipText(Protocol.values()[idx].getTooltip());
				}
				updateGroups();
			}
		});

		newLabel(g, UIText.RepositorySelectionPage_promptPort + ":"); //$NON-NLS-1$
		portText = new Text(g, SWT.BORDER);
		portText.addVerifyListener(new VerifyListener() {
			final Pattern p = Pattern.compile("^(?:[1-9][0-9]*)?$"); //$NON-NLS-1$

			@Override
			public void verifyText(final VerifyEvent e) {
				final String v = portText.getText();
				e.doit = p.matcher(
						v.substring(0, e.start) + e.text + v.substring(e.end))
						.matches();
			}
		});
		portText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				final String val = nullString(portText.getText());
				if (val == null)
					setURI(uri.setPort(-1));
				else
					try {
						setURI(uri.setPort(Integer.parseInt(val)));
					} catch (NumberFormatException err) {
						// Ignore it for now.
					}
			}
		});
	}

	private Group createGroup(final Composite parent, final String text) {
		final Group g = new Group(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		g.setLayout(layout);
		g.setText(text);
		final GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = SWT.FILL;
		g.setLayoutData(gd);
		return g;
	}

	private void newLabel(final Group g, final String text) {
		new Label(g, SWT.NULL).setText(text);
	}

	private GridData createFieldGridData() {
		return new GridData(SWT.FILL, SWT.DEFAULT, true, false);
	}

	private String nullString(final String value) {
		if (value == null)
			return null;
		final String v = value.trim();
		return v.length() == 0 ? null : v;
	}

	private void safeSet(final Text text, final String value) {
		text.setText(value != null ? value : EMPTY_STRING);
	}

	private boolean isURISelected() {
		return uriButton == null || uriButton.getSelection();
	}

	private void setURI(final URIish u) {
		try {
			eventDepth++;
			if (eventDepth == 1) {
				uri = u;
				String oldUriText = uriText.getText();
				String newUriText = uri.toString();
				// avoid moving the cursor to the first position if there are no
				// changes by this automatic update
				if (!oldUriText.equals(newUriText)) {
					uriText.setText(newUriText);
				}
				checkPage();
			}
		} finally {
			eventDepth--;
		}
	}

	private List<RemoteConfig> getUsableConfigs(final List<RemoteConfig> remotes) {

		if (remotes == null)
			return null;

		List<RemoteConfig> result = new ArrayList<>();

		for (RemoteConfig config : remotes)
			if ((sourceSelection && !config.getURIs().isEmpty() || !sourceSelection
					&& (!config.getPushURIs().isEmpty() || !config.getURIs()
							.isEmpty())))
				result.add(config);

		if (!result.isEmpty())
			return result;

		return null;
	}

	/**
	 * Check the user input and set messages in case of invalid input.
	 */
	protected void checkPage() {
		if (isURISelected()) {
			assert uri != null;
			if (uriText.getText().length() == 0) {
				selectionIncomplete(null);
				return;
			} else if (uriText.getText().endsWith(" ")) { //$NON-NLS-1$
				selectionIncomplete(UIText.RepositorySelectionPage_UriMustNotHaveTrailingSpacesMessage);
				return;
			}

			try {
				final URIish finalURI = new URIish(
						GitUrlChecker.sanitizeAsGitUrl(uriText.getText()));
				String proto = finalURI.getScheme();
				if (proto == null && scheme.getSelectionIndex() >= 0)
					proto = scheme.getItem(scheme.getSelectionIndex());

				if (uri.getPath() == null) {
					selectionIncomplete(NLS.bind(
							UIText.RepositorySelectionPage_fieldRequired,
							unamp(UIText.RepositorySelectionPage_promptPath),
							proto));
					return;
				}

				if (Protocol.FILE.handles(finalURI)) {
					String badField = null;
					if (uri.getHost() != null)
						badField = UIText.RepositorySelectionPage_promptHost;
					else if (uri.getUser() != null)
						badField = UIText.RepositorySelectionPage_promptUser;
					else if (uri.getPass() != null)
						badField = UIText.RepositorySelectionPage_promptPassword;
					if (badField != null) {
						selectionIncomplete(NLS
								.bind(
										UIText.RepositorySelectionPage_fieldNotSupported,
										unamp(badField), proto));
						return;
					}

					final File d = FS.DETECTED.resolve(
							new File("."), uri.getPath()); //$NON-NLS-1$
					if (!d.exists()) {
						selectionIncomplete(NLS.bind(
								UIText.RepositorySelectionPage_fileNotFound,
										d.getAbsolutePath()));
						return;
					}

					selectionComplete(finalURI, null);
					return;
				}

				if (uri.getHost() == null) {
					selectionIncomplete(NLS.bind(
							UIText.RepositorySelectionPage_fieldRequired,
							unamp(UIText.RepositorySelectionPage_promptHost),
							proto));
					return;
				}

				if (Protocol.GIT.handles(finalURI)) {
					String badField = null;
					if (uri.getUser() != null)
						badField = UIText.RepositorySelectionPage_promptUser;
					else if (uri.getPass() != null)
						badField = UIText.RepositorySelectionPage_promptPassword;
					if (badField != null) {
						selectionIncomplete(NLS
								.bind(
										UIText.RepositorySelectionPage_fieldNotSupported,
										unamp(badField), proto));
						return;
					}
				}

				if (Protocol.HTTP.handles(finalURI)
						|| Protocol.HTTPS.handles(finalURI)) {
					UserPasswordCredentials credentials = SecureStoreUtils
							.getCredentials(finalURI);
					if (credentials != null) {
						String u = credentials.getUser();
						String p = credentials.getPassword();
						String uriUser = finalURI.getUser();
						if (uriUser == null) {
							if (setSafeUser(u) && setSafePassword(p))
								setStoreInSecureStore(true);
						} else if (uriUser.length() != 0 && uriUser.equals(u)) {
							if (setSafePassword(p))
								setStoreInSecureStore(true);
						}
					}
				}

				selectionComplete(finalURI, null);
				return;
			} catch (URISyntaxException e) {
				selectionIncomplete(e.getReason());
				return;
			} catch (Exception e) {
				Activator.logError(NLS.bind(
						UIText.RepositorySelectionPage_errorValidating,
						getClass().getName()), e);
				selectionIncomplete(UIText.RepositorySelectionPage_internalError);
				return;
			}
		} else {
			assert remoteButton.getSelection();
			selectionComplete(null, remoteConfig);
			return;
		}
	}

	private boolean setSafePassword(String p) {
		if ((password == null || password.length() == 0) && p != null
				&& p.length() != 0) {
			password = p;
			passText.setText(p);
			return true;
		}
		return false;
	}

	private boolean setSafeUser(String u) {
		if ((user == null || user.length() == 0) && u != null
				&& u.length() != 0) {
			user = u;
			userText.setText(u);
			return true;
		}
		return false;
	}

	private void setStoreInSecureStore(boolean store) {
		storeInSecureStore = store;
		storeCheckbox.setSelection(store);
	}

	private String unamp(String s) {
		return s.replace("&", EMPTY_STRING); //$NON-NLS-1$
	}

	private void selectionIncomplete(final String errorMessage) {
		setExposedSelection(null, null);
		setErrorMessage(errorMessage);
		setPageComplete(false);
	}

	private void selectionComplete(final URIish u, final RemoteConfig rc) {
		setExposedSelection(u, rc);
		setErrorMessage(null);
		setPageComplete(true);
	}

	private void setExposedSelection(final URIish u, final RemoteConfig rc) {
		final RepositorySelection newSelection = new RepositorySelection(u, rc);
		if (newSelection.equals(selection))
			return;

		selection = newSelection;
	}

	private void updateRemoteAndURIPanels() {
		UIUtils.setEnabledRecursively(uriPanel, isURISelected());
		if (uriPanel.getEnabled())
			updateGroups();
		if (configuredRemotes != null)
			UIUtils.setEnabledRecursively(remotePanel, !isURISelected());
	}

	private void updateGroups() {
		Protocol p = getProtocol();
		if (p != null) {
			hostText.setEnabled(p.hasHost());
			if (!p.hasHost())
				hostText.setText(EMPTY_STRING);
			portText.setEnabled(p.hasPort());
			if (!p.hasPort())
				portText.setText(EMPTY_STRING);

			UIUtils.setEnabledRecursively(authGroup, p.canAuthenticate());
			if (!p.canAuthenticate()) {
				userText.setText(EMPTY_STRING);
				passText.setText(EMPTY_STRING);
				storeCheckbox.setSelection(false);
			}
		}
	}

	private Protocol getProtocol() {
		int idx = scheme.getSelectionIndex();
		if (idx >= 0)
			return Protocol.values()[idx];
		return null;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			uriText.setFocus();
	}

	/**
	 * Updates the proposal list for the URI field
	 */
	public void saveUriInPrefs() {
		uriProposalHandler.updateProposals();
	}

	/**
	 * @return credentials
	 */
	public UserPasswordCredentials getCredentials() {
		if ((user == null || user.length() == 0)
				&& (password == null || password.length() == 0))
			return null;
		return new UserPasswordCredentials(user, password);
	}

	/**
	 * @return true if credentials should be stored
	 */
	public boolean getStoreInSecureStore() {
		return this.storeInSecureStore;
	}

	/**
	 * Set the ID for context sensitive help
	 *
	 * @param id
	 *            help context
	 */
	public void setHelpContext(String id) {
		helpContext = id;
	}

	@Override
	public void performHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp(helpContext);
	}

	private void updateFields(final String text) {
		try {
			eventDepth++;
			if (eventDepth != 1)
				return;

			String strippedText = GitUrlChecker.sanitizeAsGitUrl(text);
			final URIish u = new URIish(strippedText);
			if (!text.equals(strippedText)) {
				uriText.setText(strippedText);
			}
			safeSet(hostText, u.getHost());
			safeSet(pathText, u.getPath());
			safeSet(userText, u.getUser());
			safeSet(passText, u.getPass());

			if (u.getPort() > 0)
				portText.setText(Integer.toString(u.getPort()));
			else
				portText.setText(EMPTY_STRING);

			Protocol protocol = Protocol.fromUri(u);
			if (protocol != null) {
				scheme.select(scheme.indexOf(protocol.getDefaultScheme()));
				scheme.notifyListeners(SWT.Selection, new Event());
			}

			updateGroups();
			uri = u;
		} catch (URISyntaxException err) {
			// leave uriText as it is, but clean up underlying uri and
			// decomposed fields
			uri = new URIish();
			hostText.setText(EMPTY_STRING);
			pathText.setText(EMPTY_STRING);
			userText.setText(EMPTY_STRING);
			passText.setText(EMPTY_STRING);
			portText.setText(EMPTY_STRING);
			scheme.select(-1);
		} finally {
			eventDepth--;
		}
		checkPage();
	}

	private IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	public GitRepositoryInfo getGitRepositoryInfo() {
		GitRepositoryInfo info = new GitRepositoryInfo(uri.toString());
		info.setCredentials(user, password);
		info.setShouldSaveCredentialsInSecureStore(storeInSecureStore);
		uriProposalHandler.updateProposals();
		return info;
	}

}
