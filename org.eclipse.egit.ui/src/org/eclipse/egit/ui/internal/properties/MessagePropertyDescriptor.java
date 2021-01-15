/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.properties;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.AbstractInformationControlManager.IInformationControlCloser;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * A {@link PropertyDescriptor} for a property that shows a message when
 * selected. The message is shown via the {@link IInformationControl} mechanism
 * from the JFace text editor framework.
 */
public class MessagePropertyDescriptor extends GitPropertyDescriptor {

	private final String message;

	private final PropertySheetPage page;

	/**
	 * Creates a new {@link MessagePropertyDescriptor}.
	 *
	 * @param id
	 *            for the property
	 * @param label
	 *            for the property
	 * @param message
	 *            to show when the property is selected
	 * @param page
	 *            to show the property and the message on
	 */
	public MessagePropertyDescriptor(Object id, String label, String message,
			PropertySheetPage page) {
		super(id, label);
		this.message = message;
		this.page = page;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent) {
		if (StringUtils.isEmptyOrNull(message)) {
			return null;
		}
		return new PopupCellEditor(parent);
	}

	/**
	 * A dummy {@link CellEditor} that it invisible and that doesn't allow
	 * editing. It gives us a defined moment to start showing the pop-up, and it
	 * gives us access to the cell's rectangle so that we know where to show the
	 * pop-up.
	 */
	private class PopupCellEditor extends CellEditor {

		private Label editor;

		private Tree tree;

		private TreeItem mySelf;

		private Object content;

		private AbstractInformationControlManager popupControlManager;

		PopupCellEditor(Composite parent) {
			super(parent);
		}

		@Override
		protected Control createControl(Composite parent) {
			tree = (Tree) parent;
			// Use a transparent label on which there is never any text, so the
			// original selected cell is still visible. Additionally, the editor
			// deactivates itself when focused as transparency doesn't work on
			// Windows.
			editor = new Label(parent, SWT.LEFT);
			editor.setBackground(
					parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			return editor;
		}

		@Override
		protected Object doGetValue() {
			return content;
		}

		@Override
		protected void doSetValue(Object value) {
			content = value;
		}

		@Override
		protected void doSetFocus() {
			if (popupControlManager != null) {
				deactivate();
				return;
			}
			// The label can't take the focus. But this is a good time to show
			// the pop-up.
			Rectangle bounds = editor.getBounds();
			Composite parent = editor.getParent();
			deactivate();
			// Create a "Show information" action like in text editors, and
			// activate the text editor context to get the key binding (F2 by
			// default, to focus the pop-up.)
			FocusPopupAction focusAction;
			IActionBars bars = page.getSite().getActionBars();
			IAction existingAction = bars.getGlobalActionHandler(
					ITextEditorActionConstants.SHOW_INFORMATION);
			if (existingAction == null
					|| !(existingAction instanceof FocusPopupAction)) {
				focusAction = new FocusPopupAction();
				bars.setGlobalActionHandler(focusAction.getId(), focusAction);
				bars.updateActionBars();
			} else {
				existingAction.setEnabled(true);
				focusAction = (FocusPopupAction) existingAction;
			}
			IContextService ctxSrv = page.getSite()
					.getService(IContextService.class);
			IContextActivation[] ctxActivation = { null };
			ctxActivation[0] = ctxSrv
					.activateContext("org.eclipse.ui.textEditorScope"); //$NON-NLS-1$
			// Set up the IInformationControl framework. This needs a manager
			// configured with a pop-up creator and a pop-up closer.
			IInformationControlCreator popupCreator = parentShell -> new DefaultInformationControl(
					parentShell, EditorsUI.getTooltipAffordanceString(), null);
			class Manager extends AbstractInformationControlManager {

				Manager(IInformationControlCreator creator) {
					super(creator);
				}

				@Override
				protected void setCloser(IInformationControlCloser closer) {
					// Make this method accessible
					super.setCloser(closer);
				}

				@Override
				protected void computeInformation() {
					setInformation(message, bounds);
				}

			}
			Manager popupManager = new Manager(popupCreator);
			IInformationControlCloser closer = new IInformationControlCloser() {

				private Control subject;

				private IInformationControl popup;

				private Listener subjectListener;

				private FocusListener focusListener;

				private Display display;

				@Override
				public void setSubjectControl(Control subject) {
					this.subject = subject;
					if (subject != null) {
						display = subject.getDisplay();
					}
				}

				@Override
				public void setInformationControl(IInformationControl control) {
					focusAction.setPopup(control);
					if (control != null) {
						popup = control;
					} else if (ctxActivation[0] != null) {
						ctxSrv.deactivateContext(ctxActivation[0]);
						ctxActivation[0] = null;
					}
				}

				@Override
				public void start(Rectangle subjectArea) {
					if (subject != null && popup != null) {
						hookSubject();
						popup.addDisposeListener(event -> {
							focusAction.setPopup(null);
							unhookSubject();
							if (popupControlManager != null) {
								popupControlManager.dispose();
								popupControlManager = null;
							}
						});
						focusListener = new FocusListener() {

							@Override
							public void focusGained(FocusEvent e) {
								// Nothing
							}

							@Override
							public void focusLost(FocusEvent e) {
								popup.dispose();
							}
						};
						popup.addFocusListener(focusListener);
					}
				}

				@Override
				public void stop() {
					unhookSubject();
					if (popup != null) {
						if (focusListener != null) {
							popup.removeFocusListener(focusListener);
							focusListener = null;
						}
						popup = null;
					}
				}

				private void hookSubject() {
					if (subject == null || popup == null) {
						return;
					}
					subjectListener = event -> {
						switch (event.type) {
						case SWT.Selection:
							// The focus remains on the tree initially. Close
							// the pop-up if another item is selected.
							if (event.item != mySelf && popup != null) {
								popup.dispose();
								popup = null;
							}
							break;
						case SWT.Dispose:
						case SWT.Expand:
						case SWT.Collapse:
						case SWT.Resize:
						case SWT.Move:
						case SWT.MouseWheel:
							// Close the popup if the subject is scrolled,
							// resized, or moved, or tree nodes are expanded or
							// collapsed.
							if (popup != null) {
								popup.dispose();
								popup = null;
							}
							break;
						case SWT.Deactivate:
						case SWT.FocusOut:
							// If the focus goes anywhere else but the pop-up
							// itself, close the pop-up.
							//
							// Must run async, otherwise the popup itself may
							// not be focused yet.
							display.asyncExec(() -> {
								if (popup != null && !popup.isFocusControl()) {
									popup.dispose();
									popup = null;
								}
							});
							break;
						default:
							break;
						}
					};
					subject.addListener(SWT.Selection, subjectListener);
					subject.addListener(SWT.Dispose, subjectListener);
					subject.addListener(SWT.Deactivate, subjectListener);
					subject.addListener(SWT.FocusOut, subjectListener);
					subject.addListener(SWT.MouseWheel, subjectListener);
					subject.addListener(SWT.Resize, subjectListener);
					subject.addListener(SWT.Move, subjectListener);
					subject.addListener(SWT.Expand, subjectListener);
					subject.addListener(SWT.Collapse, subjectListener);
					if (subject instanceof Composite) {
						Composite composite = (Composite) subject;
						ScrollBar scroll = composite.getHorizontalBar();
						if (scroll != null) {
							scroll.addListener(SWT.Selection, subjectListener);
						}
						scroll = composite.getVerticalBar();
						if (scroll != null) {
							scroll.addListener(SWT.Selection, subjectListener);
						}
					}
					subject.getShell().addListener(SWT.Deactivate,
							subjectListener);
				}

				private void unhookSubject() {
					if (subject == null || subjectListener == null) {
						return;
					}
					subject.removeListener(SWT.Selection, subjectListener);
					subject.removeListener(SWT.Dispose, subjectListener);
					subject.removeListener(SWT.Deactivate, subjectListener);
					subject.removeListener(SWT.FocusOut, subjectListener);
					subject.removeListener(SWT.MouseWheel, subjectListener);
					subject.removeListener(SWT.Resize, subjectListener);
					subject.removeListener(SWT.Move, subjectListener);
					subject.removeListener(SWT.Expand, subjectListener);
					subject.removeListener(SWT.Collapse, subjectListener);
					if (subject instanceof Composite) {
						Composite composite = (Composite) subject;
						ScrollBar scroll = composite.getHorizontalBar();
						if (scroll != null) {
							scroll.removeListener(SWT.Selection,
									subjectListener);
						}
						scroll = composite.getVerticalBar();
						if (scroll != null) {
							scroll.removeListener(SWT.Selection,
									subjectListener);
						}
					}
					Shell shell = subject.getShell();
					if (shell != null) {
						shell.removeListener(SWT.Deactivate, subjectListener);
					}
					subjectListener = null;
				}
			};
			popupManager.setCloser(closer);
			popupControlManager = popupManager;
			TreeItem[] selection = tree.getSelection();
			mySelf = selection == null || selection.length == 0 ? null
					: selection[0];
			popupManager.install(parent);
			popupManager.showInformation();
		}
	}

	/**
	 * This action gets installed globally (once) and subsequently enabled and
	 * disabled depending on there being a pop-up. It uses the action definition
	 * ID from the editor framework so getting the status line via
	 * {@link EditorsUI#getTooltipAffordanceString()} works. The action focuses
	 * the pop-up, if present.
	 */
	private static class FocusPopupAction extends Action {

		private IInformationControl popup;

		FocusPopupAction() {
			super();
		}

		void setPopup(IInformationControl control) {
			popup = control;
			setEnabled(control != null);
		}

		@Override
		public String getId() {
			return ITextEditorActionConstants.SHOW_INFORMATION;
		}

		@Override
		public String getActionDefinitionId() {
			return ITextEditorActionDefinitionIds.SHOW_INFORMATION;
		}

		@Override
		public void run() {
			if (popup != null) {
				popup.setFocus();
			}
		}
	}

}
