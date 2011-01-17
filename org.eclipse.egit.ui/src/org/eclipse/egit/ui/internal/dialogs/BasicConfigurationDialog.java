package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.preferences.GlobalConfigurationPreferencePage;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.UserConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Dialog for basic configuration (User name, e-mail)
 *
 */
public class BasicConfigurationDialog extends TitleAreaDialog {
	private Button dontShowAgain;

	private FileBasedConfig userScopedConfig;

	private UserConfig config;

	private Text email;

	private Text userName;

	private boolean needsUpdate = false;

	/**
	 * Opens the dialog if the {@link UIPreferences#SHOW_INITIAL_CONFIG_DIALOG}
	 * is true
	 */
	public static void show() {
		if (Activator.getDefault().getPreferenceStore().getBoolean(
				UIPreferences.SHOW_INITIAL_CONFIG_DIALOG))
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					new BasicConfigurationDialog(PlatformUI.getWorkbench()
							.getDisplay().getActiveShell()).open();
				}
			});
	}

	/**
	 * @param parentShell
	 */
	public BasicConfigurationDialog(Shell parentShell) {
		super(parentShell);
		setHelpAvailable(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		userScopedConfig = SystemReader.getInstance().openUserConfig(null,
				FS.DETECTED);
		try {
			userScopedConfig.load();
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		} catch (ConfigInvalidException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
		config = userScopedConfig.get(UserConfig.KEY);
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		// user name
		Label userNameLabel = new Label(main, SWT.NONE);
		userNameLabel.setText(UIText.BasicConfigurationDialog_UserNameLabel);
		userName = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(userName);
		String currentName = null;
		if (config != null)
			currentName = config.getAuthorName();
		if (currentName != null)
			userName.setText(currentName);
		userName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				needsUpdate = true;
			}
		});

		// user email
		Label emailLabel = new Label(main, SWT.NONE);
		emailLabel.setText(UIText.BasicConfigurationDialog_UserEmailLabel);
		email = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(email);
		String currentMail = null;
		if (config != null)
			currentMail = config.getAuthorEmail();
		if (currentMail != null)
			email.setText(currentMail);
		email.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				needsUpdate = true;
			}
		});

		dontShowAgain = new Button(main, SWT.CHECK);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(dontShowAgain);
		dontShowAgain.setText("&Don't show this dialog again"); //$NON-NLS-1$
		dontShowAgain.setSelection(true);

		Link link = new Link(main, SWT.UNDERLINE_LINK);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(link);
		link.setText("Open the <a>Git Configuration</a> Preference Page"); //$NON-NLS-1$
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(getShell(),
						GlobalConfigurationPreferencePage.ID, null, null)
						.open();
			}
		});
		applyDialogFont(main);
		return main;
	}

	@Override
	public void create() {
		super.create();
		setTitle(UIText.BasicConfigurationDialog_DialogTitle);
		setMessage(UIText.BasicConfigurationDialog_DialogMessage);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.BasicConfigurationDialog_WindowTitle);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == Window.OK) {
			if (needsUpdate) {
				userScopedConfig.setString(ConfigConstants.CONFIG_USER_SECTION,
						null, ConfigConstants.CONFIG_KEY_NAME, userName
								.getText());
				userScopedConfig
						.setString(ConfigConstants.CONFIG_USER_SECTION, null,
								ConfigConstants.CONFIG_KEY_EMAIL, email
										.getText());
				try {
					userScopedConfig.save();
				} catch (IOException e) {
					Activator.handleError(e.getMessage(), e, true);
				}
			}
			if (dontShowAgain.getSelection())
				Activator.getDefault().getPreferenceStore().setValue(
						UIPreferences.SHOW_INITIAL_CONFIG_DIALOG, false);
		}
		super.buttonPressed(buttonId);
	}
}
