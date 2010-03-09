/*******************************************************************************
 * Copyright (C) 2007, 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;


/**
 * Wizard page that allows the user entering the location of a remote repository
 * by specifying URL manually or selecting a preconfigured remote repository.
 */
public class RepositorySelectionPage extends BaseWizardPage {
	private final static String USED_URIS_PREF = "RepositorySelectionPage.UsedUris"; //$NON-NLS-1$
	private final static String USED_URIS_LENGTH_PREF = "RepositorySelectionPage.UsedUrisLength"; //$NON-NLS-1$

	private static final int REMOTE_CONFIG_TEXT_MAX_LENGTH = 80;

	private static final int S_GIT = 0;

	private static final int S_SSH = 1;

	private static final int S_SFTP = 2;

	private static final int S_HTTP = 3;

	private static final int S_HTTPS = 4;

	private static final int S_FTP = 5;

	private static final int S_FILE = 6;

	private static final String[] DEFAULT_SCHEMES;
	static {
		DEFAULT_SCHEMES = new String[7];
		DEFAULT_SCHEMES[S_GIT] = "git"; //$NON-NLS-1$
		DEFAULT_SCHEMES[S_SSH] = "git+ssh"; //$NON-NLS-1$
		DEFAULT_SCHEMES[S_SFTP] = "sftp"; //$NON-NLS-1$
		DEFAULT_SCHEMES[S_HTTP] = "http"; //$NON-NLS-1$
		DEFAULT_SCHEMES[S_HTTPS] = "https"; //$NON-NLS-1$
		DEFAULT_SCHEMES[S_FTP] = "ftp"; //$NON-NLS-1$
		DEFAULT_SCHEMES[S_FILE] = "file"; //$NON-NLS-1$
	}

	private static void setEnabledRecursively(final Control control,
			final boolean enable) {
		control.setEnabled(enable);
		if (control instanceof Composite)
			for (final Control child : ((Composite) control).getChildren())
				setEnabledRecursively(child, enable);
	}

	private final List<RemoteConfig> configuredRemotes;

	private Group authGroup;

	private Text uriText;

	private Text hostText;

	private Text pathText;

	private Text userText;

	private Text passText;

	private Combo scheme;

	private Text portText;

	private int eventDepth;

	private URIish uri;

	private RemoteConfig remoteConfig;

	private RepositorySelection selection;

	private Composite remotePanel;

	private Button remoteButton;

	private Combo remoteCombo;

	private Composite uriPanel;

	private Button uriButton;

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
	 *            given order in GUI, with {@value Constants#DEFAULT_REMOTE_NAME} as the
	 *            default choice. List may be null or empty - no remotes
	 *            configurations appear in this case. Note that the provided
	 *            list may be changed by this constructor.
	 */
	public RepositorySelectionPage(final boolean sourceSelection,
			final List<RemoteConfig> configuredRemotes) {
		super(RepositorySelectionPage.class.getName());
		this.uri = new URIish();

		if (configuredRemotes != null)
			removeUnusableRemoteConfigs(configuredRemotes);
		if (configuredRemotes == null || configuredRemotes.isEmpty())
			this.configuredRemotes = null;
		else {
			this.configuredRemotes = configuredRemotes;
			this.remoteConfig = selectDefaultRemoteConfig();
		}
		selection = RepositorySelection.INVALID_SELECTION;

		if (sourceSelection) {
			setTitle(UIText.RepositorySelectionPage_sourceSelectionTitle);
			setDescription(UIText.RepositorySelectionPage_sourceSelectionDescription);
		} else {
			setTitle(UIText.RepositorySelectionPage_destinationSelectionTitle);
			setDescription(UIText.RepositorySelectionPage_destinationSelectionDescription);
		}
	}

	/**
	 * Create repository selection page, allowing user specifying URI, with no
	 * preconfigured remotes selection.
	 *
	 * @param sourceSelection
	 *            true if dialog is used for source selection; false otherwise
	 *            (destination selection). This indicates appropriate text
	 *            messages.
	 */
	public RepositorySelectionPage(final boolean sourceSelection) {
		this(sourceSelection, null);
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

	public void createControl(final Composite parent) {
		final Composite panel = new Composite(parent, SWT.NULL);
		panel.setLayout(new GridLayout());

		if (configuredRemotes != null)
			createRemotePanel(panel);
		createUriPanel(panel);

		updateRemoteAndURIPanels();
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

		remoteCombo = new Combo(remotePanel, SWT.READ_ONLY | SWT.DROP_DOWN);
		final String items[] = new String[configuredRemotes.size()];
		int i = 0;
		for (final RemoteConfig rc : configuredRemotes)
			items[i++] = getTextForRemoteConfig(rc);
		final int defaultIndex = configuredRemotes.indexOf(remoteConfig);
		remoteCombo.setItems(items);
		remoteCombo.select(defaultIndex);
		remoteCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final int idx = remoteCombo.getSelectionIndex();
				remoteConfig = configuredRemotes.get(idx);
				checkPage();
			}
		});
	}

	private void createUriPanel(final Composite parent) {
		if (configuredRemotes != null) {
			uriButton = new Button(parent, SWT.RADIO);
			uriButton.setText(UIText.RepositorySelectionPage_uriChoice + ":"); //$NON-NLS-1$
			uriButton.addSelectionListener(new SelectionAdapter() {
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
		uriText.setLayoutData(createFieldGridData());
		uriText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				try {
					eventDepth++;
					if (eventDepth != 1)
						return;

					final URIish u = new URIish(uriText.getText());
					safeSet(hostText, u.getHost());
					safeSet(pathText, u.getPath());
					safeSet(userText, u.getUser());
					safeSet(passText, u.getPass());

					if (u.getPort() > 0)
						portText.setText(Integer.toString(u.getPort()));
					else
						portText.setText(""); //$NON-NLS-1$

					if (isFile(u))
						scheme.select(S_FILE);
					else if (isSSH(u))
						scheme.select(S_SSH);
					else {
						for (int i = 0; i < DEFAULT_SCHEMES.length; i++) {
							if (DEFAULT_SCHEMES[i].equals(u.getScheme())) {
								scheme.select(i);
								break;
							}
						}
					}

					updateAuthGroup();
					uri = u;
				} catch (URISyntaxException err) {
					// leave uriText as it is, but clean up underlying uri and
					// decomposed fields
					uri = new URIish();
					hostText.setText(""); //$NON-NLS-1$
					pathText.setText(""); //$NON-NLS-1$
					userText.setText(""); //$NON-NLS-1$
					passText.setText(""); //$NON-NLS-1$
					portText.setText(""); //$NON-NLS-1$
					scheme.select(0);
				} finally {
					eventDepth--;
				}
				checkPage();
			}
		});

		addContentProposalToUriText(uriText);

		Button browseButton = new Button(g, SWT.NULL);
		browseButton.setText(UIText.RepositorySelectionPage_BrowseLocalFile);
		browseButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				// if a file-uri was selected before, let's try to open
				// the directory dialog on the same directory
				if (!uriText.getText().equals("")) { //$NON-NLS-1$
					try {
						URI testUri = URI.create(uriText.getText().replace(
								'\\', '/'));
						if (testUri.getScheme().equals("file")) { //$NON-NLS-1$
							String path = testUri.getPath();
							if (path.length() > 1 && path.startsWith("/")) //$NON-NLS-1$
								path = path.substring(1);

							dialog.setFilterPath(path);
						}
					} catch (IllegalArgumentException e1) {
						// ignore here, we just' don't set the directory in the
						// browser
					}

				}
				String result = dialog.open();
				if (result != null)
					uriText.setText("file:///" + result); //$NON-NLS-1$
			}

		});

		newLabel(g, UIText.RepositorySelectionPage_promptHost + ":"); //$NON-NLS-1$
		hostText = new Text(g, SWT.BORDER);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(hostText);
		hostText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				setURI(uri.setHost(nullString(hostText.getText())));
			}
		});

		newLabel(g, UIText.RepositorySelectionPage_promptPath + ":"); //$NON-NLS-1$
		pathText = new Text(g, SWT.BORDER);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(pathText);
		pathText.addModifyListener(new ModifyListener() {
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
			public void modifyText(final ModifyEvent e) {
				setURI(uri.setUser(nullString(userText.getText())));
			}
		});

		newLabel(g, UIText.RepositorySelectionPage_promptPassword + ":"); //$NON-NLS-1$
		passText = new Text(g, SWT.BORDER | SWT.PASSWORD);
		passText.setLayoutData(createFieldGridData());
		return g;
	}

	private void createConnectionGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.RepositorySelectionPage_groupConnection);

		newLabel(g, UIText.RepositorySelectionPage_promptScheme + ":"); //$NON-NLS-1$
		scheme = new Combo(g, SWT.DROP_DOWN | SWT.READ_ONLY);
		scheme.setItems(DEFAULT_SCHEMES);
		scheme.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				final int idx = scheme.getSelectionIndex();
				if (idx < 0)
					setURI(uri.setScheme(null));
				else
					setURI(uri.setScheme(nullString(scheme.getItem(idx))));
				updateAuthGroup();
			}
		});

		newLabel(g, UIText.RepositorySelectionPage_promptPort + ":"); //$NON-NLS-1$
		portText = new Text(g, SWT.BORDER);
		portText.addVerifyListener(new VerifyListener() {
			final Pattern p = Pattern.compile("^(?:[1-9][0-9]*)?$"); //$NON-NLS-1$

			public void verifyText(final VerifyEvent e) {
				final String v = portText.getText();
				e.doit = p.matcher(
						v.substring(0, e.start) + e.text + v.substring(e.end))
						.matches();
			}
		});
		portText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				final String val = nullString(portText.getText());
				if (val == null)
					setURI(uri.setPort(-1));
				else {
					try {
						setURI(uri.setPort(Integer.parseInt(val)));
					} catch (NumberFormatException err) {
						// Ignore it for now.
					}
				}
			}
		});
	}

	private static Group createGroup(final Composite parent, final String text) {
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

	private static void newLabel(final Group g, final String text) {
		new Label(g, SWT.NULL).setText(text);
	}

	private static GridData createFieldGridData() {
		return new GridData(SWT.FILL, SWT.DEFAULT, true, false);
	}

	private static boolean isGIT(final URIish uri) {
		return "git".equals(uri.getScheme()); //$NON-NLS-1$
	}

	private static boolean isFile(final URIish uri) {
		if ("file".equals(uri.getScheme()) || uri.getScheme() == null) //$NON-NLS-1$
			return true;
		if (uri.getHost() != null || uri.getPort() > 0 || uri.getUser() != null
				|| uri.getPass() != null || uri.getPath() == null)
			return false;
		if (uri.getScheme() == null)
			return FS.resolve(new File("."), uri.getPath()).isDirectory(); //$NON-NLS-1$
		return false;
	}

	private static boolean isSSH(final URIish uri) {
		if (!uri.isRemote())
			return false;
		final String scheme = uri.getScheme();
		if ("ssh".equals(scheme)) //$NON-NLS-1$
			return true;
		if ("ssh+git".equals(scheme)) //$NON-NLS-1$
			return true;
		if ("git+ssh".equals(scheme)) //$NON-NLS-1$
			return true;
		if (scheme == null && uri.getHost() != null && uri.getPath() != null)
			return true;
		return false;
	}

	private static String nullString(final String value) {
		if (value == null)
			return null;
		final String v = value.trim();
		return v.length() == 0 ? null : v;
	}

	private static void safeSet(final Text text, final String value) {
		text.setText(value != null ? value : ""); //$NON-NLS-1$
	}

	private boolean isURISelected() {
		return configuredRemotes == null || uriButton.getSelection();
	}

	private void setURI(final URIish u) {
		try {
			eventDepth++;
			if (eventDepth == 1) {
				uri = u;
				uriText.setText(uri.toString());
				checkPage();
			}
		} finally {
			eventDepth--;
		}
	}

	private static void removeUnusableRemoteConfigs(
			final List<RemoteConfig> remotes) {
		final Iterator<RemoteConfig> iter = remotes.iterator();
		while (iter.hasNext()) {
			final RemoteConfig rc = iter.next();
			if (rc.getURIs().isEmpty())
				iter.remove();
		}
	}

	private RemoteConfig selectDefaultRemoteConfig() {
		for (final RemoteConfig rc : configuredRemotes)
			if (Constants.DEFAULT_REMOTE_NAME.equals(getTextForRemoteConfig(rc)))
				return rc;
		return configuredRemotes.get(0);
	}

	private static String getTextForRemoteConfig(final RemoteConfig rc) {
		final StringBuilder sb = new StringBuilder(rc.getName());
		sb.append(": "); //$NON-NLS-1$
		boolean first = true;
		for (final URIish u : rc.getURIs()) {
			final String uString = u.toString();
			if (first)
				first = false;
			else {
				sb.append(", "); //$NON-NLS-1$
				if (sb.length() + uString.length() > REMOTE_CONFIG_TEXT_MAX_LENGTH) {
					sb.append("..."); //$NON-NLS-1$
					break;
				}
			}
			sb.append(uString);
		}
		return sb.toString();
	}

	private void checkPage() {
		if (isURISelected()) {
			assert uri != null;
			if (uriText.getText().length() == 0) {
				selectionIncomplete(null);
				return;
			}

			try {
				final URIish finalURI = new URIish(uriText.getText());
				String proto = finalURI.getScheme();
				if (proto == null && scheme.getSelectionIndex() >= 0)
					proto = scheme.getItem(scheme.getSelectionIndex());

				if (uri.getPath() == null) {
					selectionIncomplete(NLS.bind(
							UIText.RepositorySelectionPage_fieldRequired,
							unamp(UIText.RepositorySelectionPage_promptPath), proto));
					return;
				}

				if (isFile(finalURI)) {
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

					final File d = FS.resolve(new File("."), uri.getPath()); //$NON-NLS-1$
					if (!d.exists()) {
						selectionIncomplete(NLS.bind(
								UIText.RepositorySelectionPage_fileNotFound, d
										.getAbsolutePath()));
						return;
					}

					selectionComplete(finalURI, null);
					return;
				}

				if (uri.getHost() == null) {
					selectionIncomplete(NLS.bind(
							UIText.RepositorySelectionPage_fieldRequired,
							unamp(UIText.RepositorySelectionPage_promptHost), proto));
					return;
				}

				if (isGIT(finalURI)) {
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

				selectionComplete(finalURI, null);
				return;
			} catch (URISyntaxException e) {
				selectionIncomplete(e.getReason());
				return;
			} catch (Exception e) {
				Activator.logError("Error validating " + getClass().getName(),
						e);
				selectionIncomplete(UIText.RepositorySelectionPage_internalError);
				return;
			}
		} else {
			assert remoteButton.getSelection();
			selectionComplete(null, remoteConfig);
			return;
		}
	}

	private String unamp(String s) {
		return s.replace("&",""); //$NON-NLS-1$ //$NON-NLS-2$
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
		notifySelectionChanged();
	}

	private void updateRemoteAndURIPanels() {
		setEnabledRecursively(uriPanel, isURISelected());
		if (uriPanel.getEnabled())
			updateAuthGroup();
		if (configuredRemotes != null)
			setEnabledRecursively(remotePanel, !isURISelected());
	}

	private void updateAuthGroup() {
		switch (scheme.getSelectionIndex()) {
		case S_GIT:
			hostText.setEnabled(true);
			portText.setEnabled(true);
			setEnabledRecursively(authGroup, false);
			break;
		case S_SSH:
		case S_SFTP:
		case S_HTTP:
		case S_HTTPS:
		case S_FTP:
			hostText.setEnabled(true);
			portText.setEnabled(true);
			setEnabledRecursively(authGroup, true);
			break;
		case S_FILE:
			hostText.setEnabled(false);
			portText.setEnabled(false);
			setEnabledRecursively(authGroup, false);
			break;
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			uriText.setFocus();
	}

	/**
	 * Adds a URI string to the list of previously added ones
	 *
	 * @param stringToAdd
	 */
	public void saveUriInPrefs(String stringToAdd) {

		Set<String> uriStrings = getUrisFromPrefs();

		if (uriStrings.add(stringToAdd)) {

			IEclipsePreferences prefs = new InstanceScope().getNode(Activator
					.getPluginId());

			StringBuilder sb = new StringBuilder();
			StringBuilder lb = new StringBuilder();

			// there is no "good" separator for URIish, so we
			// keep track of the URI lengths separately
			for (String uriString : uriStrings) {
				sb.append(uriString);
				lb.append(uriString.length());
				lb.append(" "); //$NON-NLS-1$
			}
			prefs.put(USED_URIS_PREF, sb.toString());
			prefs.put(USED_URIS_LENGTH_PREF, lb.toString());

			try {
				prefs.flush();
			} catch (BackingStoreException e) {
				// we simply ignore this here
			}
		}
	}

	/**
	 * Gets the previously added URIs from the preferences
	 *
	 * @return a (possibly empty) list of URIs, never <code>null</code>
	 */
	public Set<String> getUrisFromPrefs() {

		// use a TreeSet to get the same sorting always
		Set<String> uriStrings = new TreeSet<String>();

		IEclipsePreferences prefs = new InstanceScope().getNode(Activator
				.getPluginId());
		// since there is no "good" separator for URIish, so we
		// keep track of the URI lengths separately
		String uriLengths = prefs.get(USED_URIS_LENGTH_PREF, ""); //$NON-NLS-1$
		String uris = prefs.get(USED_URIS_PREF, ""); //$NON-NLS-1$

		StringTokenizer tok = new StringTokenizer(uriLengths, " "); //$NON-NLS-1$
		int offset = 0;
		while (tok.hasMoreTokens()) {
			try {
				int length = Integer.parseInt(tok.nextToken());
				if (uris.length() >= (offset + length)) {
					uriStrings.add(uris.substring(offset, offset + length));
					offset += length;
				}
			} catch (NumberFormatException nfe) {
				// ignore here
			}

		}

		return uriStrings;
	}

	private void addContentProposalToUriText(Text uriTextField) {

		ControlDecoration dec = new ControlDecoration(uriTextField, SWT.TOP
				| SWT.LEFT);

		if (Platform.isRunning()) {
			dec.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(
							FieldDecorationRegistry.DEC_CONTENT_PROPOSAL)
					.getImage());
		}
		dec.setShowOnlyOnFocus(true);
		dec.setShowHover(true);

		dec.setDescriptionText(UIText.RepositorySelectionPage_ShowPreviousURIs_HoverText);

		IContentProposalProvider cp = new IContentProposalProvider() {

			public IContentProposal[] getProposals(String contents, int position) {
				List<IContentProposal> resultList = new ArrayList<IContentProposal>();

				// make the simplest possible pattern check: allow "*"
				// for multiple characters
				String patternString = contents.replaceAll("\\x2A", ".*"); //$NON-NLS-1$ //$NON-NLS-2$
				// make sure we add a (logical) * at the end
				if (!patternString.endsWith(".*")) { //$NON-NLS-1$
					patternString = patternString + ".*"; //$NON-NLS-1$
				}
				// let's compile a case-insensitive pattern (assumes ASCII only)
				Pattern pattern;
				try {
					pattern = Pattern.compile(patternString,
							Pattern.CASE_INSENSITIVE);
				} catch (PatternSyntaxException e) {
					pattern = null;
				}

				Set<String> uriStrings = getUrisFromPrefs();
				for (final String uriString : uriStrings) {

					if (pattern!=null && !pattern.matcher(uriString).matches())
						continue;

					IContentProposal propsal = new IContentProposal() {

						public String getLabel() {
							return null;
						}

						public String getDescription() {
							return null;
						}

						public int getCursorPosition() {
							return 0;
						}

						public String getContent() {
							return uriString;
						}
					};
					resultList.add(propsal);
				}

				return resultList.toArray(new IContentProposal[resultList
						.size()]);
			}
		};

		// set the acceptance style to always replace the complete content
		new ContentProposalAdapter(uriTextField, new TextContentAdapter(), cp,
				null, null)
				.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

	}
}
