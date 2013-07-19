/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer <to.pfeifer@sap.com> - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitHelper;
import org.eclipse.egit.ui.internal.commit.CommitHelper.CommitInfo;
import org.eclipse.egit.ui.internal.commit.CommitProposalProcessor;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponent.CommitStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.api.RebaseCommand.Step;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;

/**
 * Dialog is shown to user when they request to commit files. Changes in the
 * selected portion of the tree are shown.
 */

public class ModifyCommitMessageInteractiveHandler implements
		InteractiveHandler {

	private final Repository repository;

	/**
	 * @param repository
	 */
	public ModifyCommitMessageInteractiveHandler(Repository repository) {
		super();
		this.repository = repository;
	}

	public void prepareSteps(List<Step> steps) {
		// do nothing
	}

	private interface CommitMessageChangeRunnable extends Runnable {
		public String getChangedCommitMessage();
	}



	public String modifyCommitMessage(final String commit) {
		final Display display = PlatformUI.getWorkbench().getDisplay();

		CommitMessageChangeRunnable runnable = new CommitMessageChangeRunnable() {
			private String message = commit;

			public void run() {
				final ModifyCommitMessageDialog dialog;
				dialog = new ModifyCommitMessageDialog(
						display.getActiveShell(), repository);
				CommitInfo info = CommitHelper.getHeadCommitInfo(repository);

				dialog.setCommitMessage(message);
				// TODO: load author from file
				dialog.setAuthor(info.getAuthor());
				// TODO: use current user as commiter
				dialog.setCommitter(info.getCommitter());

				dialog.setBlockOnOpen(true);
				// dialog.create();
				dialog.open();
				message = dialog.getCommitMessage();
			}

			public String getChangedCommitMessage() {
				return message;
			}
		};

		display.syncExec(runnable);
		return runnable.getChangedCommitMessage();
	}

}

class ModifyCommitMessageDialog extends TitleAreaDialog {

	public ModifyCommitMessageDialog(Shell parentShell, Repository repository) {
		super(parentShell);
		this.repository = repository;
	}

	static class CommitFileContentProvider extends BaseWorkbenchContentProvider {
		@Override
		public Object[] getElements(Object element) {
			if (element instanceof Object[])
				return (Object[]) element;
			if (element instanceof Collection)
				return ((Collection) element).toArray();
			return new Object[0];
		}

		public Object[] getChildren(Object parentElement) {
			return new Object[0];
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return false;
		}
	}

	static class CommitStatusLabelProvider extends BaseLabelProvider implements
			IStyledLabelProvider {

		private Image DEFAULT = PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJ_FILE);

		private ResourceManager resourceManager = new LocalResourceManager(
				JFaceResources.getResources());

		private final Image SUBMODULE = UIIcons.REPOSITORY.createImage();

		private Image getEditorImage(CommitItem item) {
			if (!item.submodule) {
				Image image = DEFAULT;
				String name = new Path(item.path).lastSegment();
				if (name != null) {
					ImageDescriptor descriptor = PlatformUI.getWorkbench()
							.getEditorRegistry().getImageDescriptor(name);
					image = (Image) this.resourceManager.get(descriptor);
				}
				return image;
			} else
				return SUBMODULE;
		}

		private Image getDecoratedImage(Image base, ImageDescriptor decorator) {
			DecorationOverlayIcon decorated = new DecorationOverlayIcon(base,
					decorator, IDecoration.BOTTOM_RIGHT);
			return (Image) this.resourceManager.get(decorated);
		}

		public StyledString getStyledText(Object element) {
			return new StyledString();
		}

		public Image getImage(Object element) {
			CommitItem item = (CommitItem) element;
			ImageDescriptor decorator = null;
			switch (item.status) {
			case UNTRACKED:
				decorator = UIIcons.OVR_UNTRACKED;
				break;
			case ADDED:
			case ADDED_INDEX_DIFF:
				decorator = UIIcons.OVR_STAGED_ADD;
				break;
			case REMOVED:
			case REMOVED_NOT_STAGED:
			case REMOVED_UNTRACKED:
				decorator = UIIcons.OVR_STAGED_REMOVE;
				break;
			default:
				break;
			}
			return decorator != null ? getDecoratedImage(getEditorImage(item),
					decorator) : getEditorImage(item);
		}

		@Override
		public void dispose() {
			SUBMODULE.dispose();
			resourceManager.dispose();
			super.dispose();
		}
	}

	private static final String DIALOG_SETTINGS_SECTION_NAME = Activator
			.getPluginId() + ".COMMIT_DIALOG_SECTION"; //$NON-NLS-1$

	FormToolkit toolkit;

	CommitMessageComponent commitMessageComponent;

	SpellcheckableMessageArea commitText;

	Text authorText;

	Text committerText;

	ToolItem amendingItem;

	ToolItem signedOffItem;

	ToolItem changeIdItem;

	Button rewordButton;

	String commitMessage = null;

	private String author = null;

	private String committer = null;

	private boolean amending = true;

	private boolean amendAllowed = true;

	// private boolean createChangeId = false;

	private Repository repository;

	/**
	 * @return The message the user entered
	 */
	public String getCommitMessage() {
		return commitMessage;
	}

	/**
	 * Preset a commit message. This might be for amending a commit.
	 *
	 * @param s
	 *            the commit message
	 */
	public void setCommitMessage(String s) {
		this.commitMessage = s;
	}

	/**
	 * @return The author to set for the commit
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Pre-set author for the commit
	 *
	 * @param author
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * @return The committer to set for the commit
	 */
	public String getCommitter() {
		return committer;
	}

	/**
	 * Pre-set committer for the commit
	 *
	 * @param committer
	 */
	public void setCommitter(String committer) {
		this.committer = committer;
	}

	/**
	 * @return whether the last commit is to be amended
	 */
	public boolean isAmending() {
		return amending;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		toolkit.adapt(parent, false, false);
		rewordButton = createButton(parent, IDialogConstants.OK_ID,
				"Reword", true); //$NON-NLS-1$ TODO:UIText
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		updateMessage();
	}

	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId)
			okPressed();
		else if (IDialogConstants.CANCEL_ID == buttonId)
			cancelPressed();
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		toolkit.adapt(parent, false, false);
		return super.createButtonBar(parent);
	}

	@Override
	protected Control createHelpControl(Composite parent) {
		toolkit.adapt(parent, false, false);
		Control help = super.createHelpControl(parent);
		toolkit.adapt(help, false, false);
		return help;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = settings
				.getSection(DIALOG_SETTINGS_SECTION_NAME);
		if (section == null)
			section = settings.addNewSection(DIALOG_SETTINGS_SECTION_NAME);
		return section;
	}

	/**
	 * Add message drop down toolbar item
	 *
	 * @param parent
	 * @return toolbar
	 */
	protected ToolBar addMessageDropDown(Composite parent) {
		final ToolBar dropDownBar = new ToolBar(parent, SWT.FLAT | SWT.RIGHT);
		final ToolItem dropDownItem = new ToolItem(dropDownBar, SWT.PUSH);
		dropDownItem.setImage(PlatformUI.getWorkbench().getSharedImages()
				.getImage("IMG_LCL_RENDERED_VIEW_MENU")); //$NON-NLS-1$
		final Menu menu = new Menu(dropDownBar);
		dropDownItem.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				menu.dispose();
			}
		});
		MenuItem preferencesItem = new MenuItem(menu, SWT.PUSH);
		preferencesItem.setText(UIText.CommitDialog_ConfigureLink);
		preferencesItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				String[] pages = new String[] { UIPreferences.PAGE_COMMIT_PREFERENCES };
				PreferencesUtil.createPreferenceDialogOn(getShell(), pages[0],
						pages, null).open();
			}

		});
		dropDownItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				Rectangle b = dropDownItem.getBounds();
				Point p = dropDownItem.getParent().toDisplay(
						new Point(b.x, b.y + b.height));
				menu.setLocation(p.x, p.y);
				menu.setVisible(true);
			}

		});
		return dropDownBar;
	}

	@Override
	protected Control createContents(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		parent.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				toolkit.dispose();
			}
		});
		return super.createContents(parent);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		parent.getShell().setText(UIText.CommitDialog_CommitChanges);

		container = toolkit.createComposite(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		toolkit.paintBordersFor(container);
		GridLayoutFactory.swtDefaults().applyTo(container);

		final SashForm sashForm = new SashForm(container, SWT.VERTICAL
				| SWT.FILL);
		toolkit.adapt(sashForm, true, true);
		sashForm.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.create());
		createMessageAndPersonArea(sashForm);
		// sashForm.setWeights(new int[] { 50, 50 });

		applyDialogFont(container);
		container.pack();
		commitText.setFocus();
		Image titleImage = UIIcons.WIZBAN_CONNECT_REPO.createImage();
		UIUtils.hookDisposal(parent, titleImage);
		setTitleImage(titleImage);
		setTitle(UIText.CommitDialog_Title);
		setMessage(UIText.CommitDialog_Message, IMessageProvider.INFORMATION);

		ModifyListener validator = new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				updateMessage();
			}
		};
		commitText.getDocument().addDocumentListener(new IDocumentListener() {

			public void documentChanged(DocumentEvent event) {
				updateMessage();
			}

			public void documentAboutToBeChanged(DocumentEvent event) {
				// Intentionally empty
			}
		});
		authorText.addModifyListener(validator);
		committerText.addModifyListener(validator);
		return container;
	}

	private Composite createMessageAndPersonArea(Composite container) {

		Composite messageAndPersonArea = toolkit.createComposite(container);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(messageAndPersonArea);
		GridLayoutFactory.swtDefaults().margins(0, 0).spacing(0, 0)
				.applyTo(messageAndPersonArea);

		Section messageSection = toolkit.createSection(messageAndPersonArea,
				ExpandableComposite.TITLE_BAR
						| ExpandableComposite.CLIENT_INDENT);
		messageSection.setText(UIText.CommitDialog_CommitMessage);
		Composite messageArea = toolkit.createComposite(messageSection);
		GridLayoutFactory.fillDefaults().spacing(0, 0)
				.extendedMargins(2, 2, 2, 2).applyTo(messageArea);
		toolkit.paintBordersFor(messageArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(messageSection);
		GridLayoutFactory.swtDefaults().applyTo(messageSection);

		Composite headerArea = new Composite(messageSection, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(2)
				.applyTo(headerArea);

		ToolBar messageToolbar = new ToolBar(headerArea, SWT.FLAT
				| SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL)
				.grab(true, false).applyTo(messageToolbar);

		addMessageDropDown(headerArea);

		messageSection.setTextClient(headerArea);

		final CommitProposalProcessor commitProposalProcessor = new CommitProposalProcessor() {
			@Override
			protected Collection<String> computeFileNameProposals() {
				return Collections.emptyList();
			}

			@Override
			protected Collection<String> computeMessageProposals() {
				return Collections.emptyList();
			}
		};
		commitText = new CommitMessageArea(messageArea, commitMessage, SWT.NONE) {
			@Override
			protected CommitProposalProcessor getCommitProposalProcessor() {
				return commitProposalProcessor;
			}
		};
		commitText
				.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		messageSection.setClient(messageArea);
		Point size = commitText.getTextWidget().getSize();
		int minHeight = commitText.getTextWidget().getLineHeight() * 3;
		commitText.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, true).hint(size).minSize(size.x, minHeight)
				.align(SWT.FILL, SWT.FILL).create());

		UIUtils.addBulbDecorator(commitText.getTextWidget(),
				UIText.CommitDialog_ContentAssist);

		Composite personArea = toolkit.createComposite(messageAndPersonArea);
		toolkit.paintBordersFor(personArea);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(personArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(personArea);

		toolkit.createLabel(personArea, UIText.CommitDialog_Author)
				.setForeground(
						toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
		authorText = toolkit.createText(personArea, null);
		authorText.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());
		authorText.setEnabled(false);
		authorText.setEditable(false);

		toolkit.createLabel(personArea, UIText.CommitDialog_Committer)
				.setForeground(
						toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
		committerText = toolkit.createText(personArea, null);
		committerText.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, false).create());
		committerText.setEnabled(false);
		committerText.setEditable(false);
		if (committer != null)
			committerText.setText(committer);

		amendingItem = new ToolItem(messageToolbar, SWT.CHECK);
		amendingItem.setSelection(amending);
		if (amending)
			amendingItem.setEnabled(false); // if already set, don't allow any
											// changes
		else if (!amendAllowed)
			amendingItem.setEnabled(false);
		amendingItem.setToolTipText(UIText.CommitDialog_AmendPreviousCommit);
		Image amendImage = UIIcons.AMEND_COMMIT.createImage();
		UIUtils.hookDisposal(amendingItem, amendImage);
		amendingItem.setImage(amendImage);

		signedOffItem = new ToolItem(messageToolbar, SWT.CHECK);

		signedOffItem.setToolTipText(UIText.CommitDialog_AddSOB);
		Image signedOffImage = UIIcons.SIGNED_OFF.createImage();
		UIUtils.hookDisposal(signedOffItem, signedOffImage);
		signedOffItem.setImage(signedOffImage);

		changeIdItem = new ToolItem(messageToolbar, SWT.CHECK);
		Image changeIdImage = UIIcons.GERRIT.createImage();
		UIUtils.hookDisposal(changeIdItem, changeIdImage);
		changeIdItem.setImage(changeIdImage);
		changeIdItem.setToolTipText(UIText.CommitDialog_AddChangeIdLabel);

		final ICommitMessageComponentNotifications listener = new ICommitMessageComponentNotifications() {

			public void updateSignedOffToggleSelection(boolean selection) {
				signedOffItem.setSelection(selection);
			}

			public void updateChangeIdToggleSelection(boolean selection) {
				changeIdItem.setSelection(selection);
			}
		};

		commitMessageComponent = new CommitMessageComponent(repository,
				listener);
		commitMessageComponent.setDefaults();
		commitMessageComponent.attachControls(commitText, authorText,
				committerText);
		commitMessageComponent.setCommitMessage(commitMessage);
		commitMessageComponent.setAuthor(author);
		commitMessageComponent.setCommitter(committer);
		commitMessageComponent.setAmending(amending);

		amendingItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent arg0) {
				commitMessageComponent.setAmendingButtonSelection(amendingItem
						.getSelection());
			}
		});

		changeIdItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent arg0) {
				commitMessageComponent.setChangeIdButtonSelection(changeIdItem
						.getSelection());
			}
		});

		signedOffItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent arg0) {
				commitMessageComponent
						.setSignedOffButtonSelection(signedOffItem
								.getSelection());
			}
		});

		commitMessageComponent.updateUI();

		return messageAndPersonArea;
	}

	private void updateMessage() {
		String message = null;
		int type = IMessageProvider.NONE;

		String commitMsg = commitMessageComponent.getCommitMessage();
		if (commitMsg == null || commitMsg.trim().length() == 0) {
			message = UIText.CommitDialog_Message;
			type = IMessageProvider.INFORMATION;
		} else {
			CommitStatus status = commitMessageComponent.getStatus();
			message = status.getMessage();
			type = status.getMessageType();
		}

		setMessage(message, type);
		boolean commitEnabled = type == IMessageProvider.WARNING
				|| type == IMessageProvider.NONE;
		rewordButton.setEnabled(commitEnabled);
	}

	@Override
	protected void okPressed() {
//		if (!commitMessageComponent.checkCommitInfo())

		commitMessage = commitMessageComponent.getCommitMessage();
		author = commitMessageComponent.getAuthor();
		committer = commitMessageComponent.getCommitter();
		// createChangeId = changeIdItem.getSelection();

		super.okPressed();
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}
}
