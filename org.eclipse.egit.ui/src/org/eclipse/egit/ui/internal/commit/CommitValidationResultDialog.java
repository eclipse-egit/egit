package org.eclipse.egit.ui.internal.commit;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.ui.ICommitValidator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * A dialog that presents a series of validation results to the user.
 */
public class CommitValidationResultDialog extends TitleAreaDialog {

	private static final int UNDO_ID = 100;
	private static final String COMMIT_VALIDATOR_ID = "org.eclipse.egit.ui.commitValidator"; //$NON-NLS-1$

	private TableViewer tv;

	private final boolean showAbortButton;

	private final Repository repository;

	private final RevCommit commit;

	private final List<IStatus> status = new ArrayList<>();
	private final LocalResourceManager resources = new LocalResourceManager(
			JFaceResources.getResources());


	/**
	 * Creates the dialog to let the user confirm the validation result.
	 *
	 * @param parentShell
	 *            the parent shell to use
	 * @param status
	 *            the status to present
	 * @param abortButton
	 *            whether an abort button should be displayed to indicate
	 *            abortion of further processing.
	 * @param commit
	 * @param repository
	 */
	public CommitValidationResultDialog(Shell parentShell, MultiStatus status,
			boolean abortButton, Repository repository, RevCommit commit) {
		super(parentShell);

		this.showAbortButton = abortButton;
		this.repository = repository;
		this.commit = commit;

		extractStatus(status);
		setBlockOnOpen(true);
	}

	private void extractStatus(MultiStatus multi) {
		for (IStatus s : multi.getChildren()) {
			if (s instanceof MultiStatus) {
				extractStatus((MultiStatus) s);
			} else {
				status.add(s);
			}
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite root = (Composite) super.createDialogArea(parent);

		setTitle(UIText.CommitValidationResultDialog_Title);
		setMessage(UIText.CommitValidationResultDialog_TitleMessage,
				IMessageProvider.WARNING);

		tv = new TableViewer(root);
		tv.setContentProvider(new ArrayContentProvider());
		tv.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((IStatus) element).getMessage();
			}

			@Override
			public Image getImage(Object element) {
				IStatus s = (IStatus) element;
				switch (s.getSeverity()) {
				case IStatus.INFO:
					return UIIcons.getImage(resources, UIIcons.INFO);
				case IStatus.WARNING:
					return UIIcons.getImage(resources, UIIcons.WARNING);
				case IStatus.ERROR:
					return UIIcons.getImage(resources, UIIcons.ERROR);
				}
				return null;
			}
		});
		tv.setInput(status);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(tv.getControl());

		return root;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, UNDO_ID, UIText.CommitValidationResultDialog_Undo,
				false).setEnabled(commit.getParentCount() == 1);

		createButton(parent, IDialogConstants.OK_ID,
				UIText.CommitValidationResultDialog_ButtonContinue, true);
		if (showAbortButton) {
			createButton(parent, IDialogConstants.CANCEL_ID,
					UIText.CommitValidationResultDialog_ButtonAbort, false);
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == UNDO_ID) {
			undoCommit();
		}
		super.buttonPressed(buttonId);
	}

	private void undoCommit() {
		String fullMessage = commit.getFullMessage();

		ResetOperation operation = new ResetOperation(repository, commit
				.getParent(0).getId().getName(), ResetType.SOFT);
		JobUtil.scheduleUserWorkspaceJob(operation,
				UIText.SoftResetToRevisionAction_softReset, JobFamilies.RESET);

		TextTransfer trans = TextTransfer.getInstance();
		Clipboard cb = new Clipboard(getShell().getDisplay());
		cb.setContents(new Object[] { fullMessage }, new Transfer[] { trans });
		cb.dispose();

		MessageBox mb = new MessageBox(getParentShell(), SWT.ICON_INFORMATION
				| SWT.OK);
		mb.setText(UIText.CommitValidationResultDialog_CopyMessageTitle);
		mb.setMessage(UIText.CommitValidationResultDialog_CopyMessageText);
		mb.open();

		this.setReturnCode(CANCEL);
		this.close();
	}

	/**
	 * Validates the given commit and displays the result of the validation.
	 * <p>
	 * The dialog will be presented in the UI thread.
	 *
	 * @param repository
	 *            the repository the commit belongs to.
	 * @param commit
	 *            the commit to validate
	 * @param abortButton
	 *            should an abort button be displayed?
	 * @return the {@link Window#OK} if user acknowledged the result,
	 *         {@link Window#CANCEL} if the abort button was pressed
	 */
	public static int validate(final Repository repository,
			final RevCommit commit,
			final boolean abortButton) {
		final MultiStatus valResult = new MultiStatus("unknown", 0, "", null); //$NON-NLS-1$ //$NON-NLS-2$

		// validate
		Collection<ICommitValidator> validators = getCommitValidators();
		for (ICommitValidator validator : validators) {
			valResult.add(validator.validate(commit));
		}

		// present result
		if (!valResult.isOK()) {
			final AtomicInteger result = new AtomicInteger();
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					CommitValidationResultDialog dlg = new CommitValidationResultDialog(
							PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getShell(),
							valResult, abortButton, repository, commit);
					result.set(dlg.open());
				}
			});
			return result.get();
		} else {
			return Window.OK;
		}
	}

	private static Collection<ICommitValidator> getCommitValidators() {
		List<ICommitValidator> validators = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] config = registry
				.getConfigurationElementsFor(COMMIT_VALIDATOR_ID);
		for (IConfigurationElement elem : config) {
			try {
				Object validator = elem.createExecutableExtension("class");//$NON-NLS-1$
				if (validator instanceof ICommitValidator) {
					validators.add((ICommitValidator) validator);
				} else {
					Activator
							.logError(
									UIText.CommitValidationResultDialog_WrongExtensionType,
									null);
				}
			} catch (CoreException e) {
				Activator
						.logError(
								MessageFormat
										.format(UIText.CommitValidationResultDialog_ErrorCreatingExtension,
												elem), e);
			}
		}
		return validators;
	}

}
