/*******************************************************************************
 * Copyright (c) 2010, SAP AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A wizard for creating a patch file by running the git diff command.
 */
public class GitCreatePatchWizard extends Wizard {

	private RevCommit commit;

	private TreeWalk walker;

	private Repository db;

	private LocationPage locationPage;

	private OptionsPage optionsPage;

	// the encoding for the currently processed file
	private String currentEncoding = null;

	// The initial size of this wizard.
	private final static int INITIAL_WIDTH = 300;

	private final static int INITIAL_HEIGHT = 150;

	/**
	 *
	 * @param part
	 * @param commit
	 * @param walker
	 * @param db
	 */
	public static void run(IWorkbenchPart part, final RevCommit commit,
			TreeWalk walker, Repository db) {
		final String title = UIText.GitCreatePatchWizard_CreatePatchTitle;
		final GitCreatePatchWizard wizard = new GitCreatePatchWizard(commit,
				walker, db);
		wizard.setWindowTitle(title);
		WizardDialog dialog = new WizardDialog(part.getSite().getShell(),
				wizard);
		dialog.setMinimumPageSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		dialog.open();
	}

	/**
	 * Creates a wizard which is used to export the changes introduced by a
	 * commit, filtered by a TreeWalk
	 *
	 * @param commit
	 * @param walker
	 * @param db
	 */
	public GitCreatePatchWizard(RevCommit commit, TreeWalk walker, Repository db) {
		this.commit = commit;
		this.walker = walker;
		this.db = db;
	}

	@Override
	public void addPages() {
		String pageTitle = UIText.GitCreatePatchWizard_SelectLocationTitle;
		String pageDescription = UIText.GitCreatePatchWizard_SelectLocationDescription;

		locationPage = new LocationPage(pageTitle, pageTitle, UIIcons.WIZBAN_CREATE_PATCH);
		locationPage.setDescription(pageDescription);
		addPage(locationPage);

		pageTitle = UIText.GitCreatePatchWizard_SelectOptionsTitle;
		pageDescription = UIText.GitCreatePatchWizard_SelectOptionsDescription;
		optionsPage = new OptionsPage(pageTitle, pageTitle, UIIcons.WIZBAN_CREATE_PATCH);
		optionsPage.setDescription(pageDescription);
		addPage(optionsPage);
	}

	@Override
	public boolean performFinish() {
		final boolean isGit = optionsPage.gitFormat.getSelection();
		final boolean isFile = locationPage.fsRadio.getSelection();
		final String fileName = locationPage.fsPathText.getText();

		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					final StringBuilder sb = new StringBuilder();
					final DiffFormatter diffFmt = new DiffFormatter(
							new BufferedOutputStream(new ByteArrayOutputStream() {

						@Override
						public synchronized void write(byte[] b, int off, int len) {
							super.write(b, off, len);
							if (currentEncoding == null)
								sb.append(toString());
							else try {
								sb.append(toString(currentEncoding));
							} catch (UnsupportedEncodingException e) {
								sb.append(toString());
							}
							reset();
						}

					}));
					try {
						FileDiff[] diffs = FileDiff.compute(walker, commit);
						for (FileDiff diff : diffs) {
							currentEncoding = CompareUtils.
								getResourceEncoding(db, diff.getPath());
							diff.outputDiff(sb, db, diffFmt, isGit);
							diffFmt.flush();
						}

						if (isFile) {
							Writer output = new BufferedWriter(new FileWriter(
									fileName));
							try {
								// FileWriter always assumes default encoding is
								// OK!
								output.write(sb.toString());
							} finally {
								output.close();
							}
						} else {
							copyToClipboard(sb.toString());
						}
					} catch (IOException e) {
						Activator
							.logError("Patch file could not be written", e); //$NON-NLS-1$
					}
				}
			});
		} catch (InvocationTargetException e) {
			((WizardPage) getContainer().getCurrentPage()).setErrorMessage(e
					.getMessage() == null ? e.getMessage()
					: UIText.GitCreatePatchWizard_InternalError);
			Activator.logError("Patch file was not written", e); //$NON-NLS-1$
			return false;
		} catch (InterruptedException e) {
			Activator.logError("Patch file was not written", e); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	private void copyToClipboard(final String content) {
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				TextTransfer plainTextTransfer = TextTransfer.getInstance();
				Clipboard clipboard = new Clipboard(getShell().getDisplay());
				clipboard.setContents(new String[] { content },
						new Transfer[] { plainTextTransfer });
				clipboard.dispose();
			}
		});
	}

	/**
	 * A wizard page to choose the target location
	 */
	public class LocationPage extends WizardPage {

		private Button cpRadio;

		private Button fsRadio;

		private Text fsPathText;

		private Button fsBrowseButton;

		private boolean pageValid;

		/**
		 * @param pageName
		 * @param title
		 * @param titleImage
		 */
		protected LocationPage(String pageName, String title,
				ImageDescriptor titleImage) {
			super(pageName, title, titleImage);
		}

		public void createControl(Composite parent) {
			final Composite composite = new Composite(parent, SWT.NULL);
			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 3;
			composite.setLayout(gridLayout);
			composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			initializeDialogUnits(composite);

			// clipboard
			GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
			gd.horizontalSpan = 3;
			cpRadio = new Button(composite, SWT.RADIO);
			cpRadio.setText(UIText.GitCreatePatchWizard_Clipboard);
			cpRadio.setLayoutData(gd);
			cpRadio.setSelection(true);

			// filesystem
			fsRadio = new Button(composite, SWT.RADIO);
			fsRadio.setText(UIText.GitCreatePatchWizard_File);
			fsRadio.setSelection(false);

			fsPathText = new Text(composite, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			fsPathText.setLayoutData(gd);
			fsPathText.setText(createFileName());
			fsPathText.setEnabled(false);

			fsBrowseButton = new Button(composite, SWT.PUSH);
			fsBrowseButton.setText(UIText.GitCreatePatchWizard_Browse);
			GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			Point minSize = fsBrowseButton.computeSize(SWT.DEFAULT,
					SWT.DEFAULT, true);
			data.widthHint = Math.max(widthHint, minSize.x);
			fsBrowseButton.setLayoutData(data);
			fsBrowseButton.setEnabled(false);

			cpRadio.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					// disable other input controls
					fsPathText.setEnabled(false);
					fsBrowseButton.setEnabled(false);
					validatePage();
				}
			});

			fsRadio.addListener(SWT.Selection, new Listener() {

				public void handleEvent(Event event) {
					// enable filesystem input controls
					fsPathText.setEnabled(true);
					fsBrowseButton.setEnabled(true);
					// set focus to filesystem input text control
					fsPathText.setFocus();
					validatePage();
				}
			});

			fsPathText.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					validatePage();
				}
			});

			fsBrowseButton.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					final FileDialog dialog = new FileDialog(getShell(),
							SWT.PRIMARY_MODAL | SWT.SAVE);
					if (pageValid) {
						final File file = new File(fsPathText.getText());
						dialog.setFilterPath(file.getParent());
						dialog.setFileName(file.getName());
					} else {
						dialog.setFileName(""); //$NON-NLS-1$
					}
					dialog.setText(""); //$NON-NLS-1$
					final String path = dialog.open();
					if (path != null)
						fsPathText.setText(new Path(path).toOSString());
					validatePage();
				}
			});

			Dialog.applyDialogFont(composite);
			setControl(composite);

		}

		private String createFileName() {
			String name = commit.getShortMessage();

			name = name.trim();
			try {
				name = URLEncoder.encode(name, "UTF-8"); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				// We're pretty sure that UTF-8 will be supported in future
			}
			if (name.length() > 80)
				name = name.substring(0, 80);
			while (name.endsWith(".")) //$NON-NLS-1$
				name = name.substring(0, name.length() - 1);
			name = name.concat(".patch"); //$NON-NLS-1$

			String defaultPath = db.getWorkTree().getAbsolutePath();

			return (new File(defaultPath, name)).getPath();
		}

		/**
		 * Allow the user to finish if a valid file has been entered.
		 *
		 * @return page is valid
		 */
		protected boolean validatePage() {
			if (fsRadio.getSelection()) {
				pageValid = validateFilesystemLocation();
			} else if (cpRadio.getSelection()) {
				pageValid = true;
			}

			/**
			 * Avoid draw flicker by clearing error message if all is valid.
			 */
			if (pageValid) {
				setMessage(null);
				setErrorMessage(null);
			}
			setPageComplete(pageValid);
			return pageValid;
		}

		/**
		 * The following conditions must hold for the file system location to be
		 * valid: - the path must be valid and non-empty - the path must be
		 * absolute - the specified file must be of type file - the parent must
		 * exist (new folders can be created via the browse button)
		 *
		 * @return if the location is valid
		 */
		private boolean validateFilesystemLocation() {
			final String pathString = fsPathText.getText().trim();
			if (pathString.length() == 0
					|| !new Path("").isValidPath(pathString)) { //$NON-NLS-1$
				setErrorMessage(UIText.GitCreatePatchWizard_FilesystemError);
				return false;
			}

			final File file = new File(pathString);
			if (!file.isAbsolute()) {
				setErrorMessage(UIText.GitCreatePatchWizard_FilesystemInvalidError);
				return false;
			}

			if (file.isDirectory()) {
				setErrorMessage(UIText.GitCreatePatchWizard_FilesystemDirectoryError);
				return false;
			}

			if (pathString.endsWith("/") || pathString.endsWith("\\")) { //$NON-NLS-1$//$NON-NLS-2$
				setErrorMessage(UIText.GitCreatePatchWizard_FilesystemDirectoryNotExistsError);
				return false;
			}

			final File parent = file.getParentFile();
			if (!(parent.exists() && parent.isDirectory())) {
				setErrorMessage(UIText.GitCreatePatchWizard_FilesystemDirectoryNotExistsError);
				return false;
			}
			return true;
		}
	}

	/**
	 *
	 * A wizard Page used to specify options of the created patch
	 */
	public static class OptionsPage extends WizardPage {
		private Button gitFormat;

		/**
		 *
		 * @param pageName
		 * @param title
		 * @param titleImage
		 */
		protected OptionsPage(String pageName, String title,
				ImageDescriptor titleImage) {
			super(pageName, title, titleImage);
		}

		public void createControl(Composite parent) {
			final Composite composite = new Composite(parent, SWT.NULL);
			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 3;
			composite.setLayout(gridLayout);
			composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			// clipboard
			GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
			gd.horizontalSpan = 3;
			gitFormat = new Button(composite, SWT.CHECK);
			gitFormat.setText(UIText.GitCreatePatchWizard_GitFormat);
			gitFormat.setLayoutData(gd);

			Dialog.applyDialogFont(composite);
			setControl(composite);
		}
	}
}
