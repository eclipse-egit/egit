/*******************************************************************************
 * Copyright (C) 2010, Benjamin Muskalla <bmuskalla@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Benjamin Muskalla (EclipseSource) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.Iterator;
import java.util.List;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.SubMenuManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.MarginPainter;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ActiveShellExpression;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

/**
 * Text field with support for spellchecking.
 */
public class CommitMessageArea extends Composite {

	private static class TextViewerAction extends Action implements IUpdate {

		private int fOperationCode= -1;
		private ITextOperationTarget fOperationTarget;

		/**
		 * Creates a new action.
		 *
		 * @param viewer the viewer
		 * @param operationCode the opcode
		 */
		public TextViewerAction(ITextViewer viewer, int operationCode) {
			fOperationCode= operationCode;
			fOperationTarget= viewer.getTextOperationTarget();
			update();
		}

		/**
		 * Updates the enabled state of the action.
		 * Fires a property change if the enabled state changes.
		 *
		 * @see Action#firePropertyChange(String, Object, Object)
		 */
		public void update() {
			// XXX: workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=206111
			if (fOperationCode == ITextOperationTarget.REDO)
				return;

			boolean wasEnabled= isEnabled();
			boolean isEnabled= (fOperationTarget != null && fOperationTarget.canDoOperation(fOperationCode));
			setEnabled(isEnabled);

			if (wasEnabled != isEnabled) {
				firePropertyChange(ENABLED, wasEnabled ? Boolean.TRUE : Boolean.FALSE, isEnabled ? Boolean.TRUE : Boolean.FALSE);
			}
		}

		/**
		 * @see Action#run()
		 */
		public void run() {
			if (fOperationCode != -1 && fOperationTarget != null) {
				fOperationTarget.doOperation(fOperationCode);
			}
		}
	}

	private final SourceViewer sourceViewer;

	/**
	 * @param parent
	 * @param initialText
	 */
	public CommitMessageArea(Composite parent, String initialText) {
		super(parent, SWT.BORDER);
		setLayout(new FillLayout());

		AnnotationModel annotationModel = new AnnotationModel();
		sourceViewer = new SourceViewer(this, null, null, true, SWT.MULTI
				| SWT.V_SCROLL | SWT.WRAP);
		getTextWidget().setIndent(2);

		createMarginPainter();

		final SourceViewerDecorationSupport support = configureAnnotationPreferences();
		final IHandlerActivation handlerActivation = installQuickFixActionHandler();

		configureContextMenu();

		Document document = new Document(initialText);

		sourceViewer.configure(new TextSourceViewerConfiguration(EditorsUI
				.getPreferenceStore()));
		sourceViewer.setDocument(document, annotationModel);

		getTextWidget().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent disposeEvent) {
				support.uninstall();
				getHandlerService().deactivateHandler(handlerActivation);
			}
		});
	}

	private void configureContextMenu() {
		final TextViewerAction cutAction = new TextViewerAction(sourceViewer, ITextOperationTarget.CUT);
		cutAction.setText(UIText.CommitMessageArea_cut);
		cutAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_CUT);

		final TextViewerAction copyAction = new TextViewerAction(sourceViewer, ITextOperationTarget.COPY);
		copyAction.setText(UIText.CommitMessageArea_copy);
		copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);

		final TextViewerAction pasteAction = new TextViewerAction(sourceViewer, ITextOperationTarget.PASTE);
		pasteAction.setText(UIText.CommitMessageArea_paste);
		pasteAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_PASTE);

		final TextViewerAction selectAllAction = new TextViewerAction(sourceViewer, ITextOperationTarget.SELECT_ALL);
		selectAllAction.setText(UIText.CommitMessageArea_selectAll);
		selectAllAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);

		MenuManager contextMenu = new MenuManager();
		contextMenu.add(cutAction);
		contextMenu.add(copyAction);
		contextMenu.add(pasteAction);
		contextMenu.add(selectAllAction);
		contextMenu.add(new Separator());

		final SubMenuManager quickFixMenu = new SubMenuManager(contextMenu);
		quickFixMenu.setVisible(true);
		quickFixMenu.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				quickFixMenu.removeAll();
				addProposals(quickFixMenu);
			}
		});
		StyledText textWidget = getTextWidget();
		getTextWidget().setMenu(contextMenu.createContextMenu(textWidget));

		getTextWidget().addFocusListener(new FocusListener() {

			private IHandlerActivation cutHandlerActivation;
			private IHandlerActivation copyHandlerActivation;
			private IHandlerActivation pasteHandlerActivation;
			private IHandlerActivation selectAllHandlerActivation;

			public void focusGained(FocusEvent e) {
				cutAction.update();
				copyAction.update();
				IHandlerService service = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
				this.cutHandlerActivation = service.activateHandler(IWorkbenchCommandConstants.EDIT_CUT, new ActionHandler(cutAction), new ActiveShellExpression(getParent().getShell()));
	            this.copyHandlerActivation = service.activateHandler(IWorkbenchCommandConstants.EDIT_COPY, new ActionHandler(copyAction), new ActiveShellExpression(getParent().getShell()));
	            this.pasteHandlerActivation = service.activateHandler(IWorkbenchCommandConstants.EDIT_PASTE, new ActionHandler(pasteAction), new ActiveShellExpression(getParent().getShell()));
	            this.selectAllHandlerActivation = service.activateHandler(IWorkbenchCommandConstants.EDIT_SELECT_ALL, new ActionHandler(selectAllAction), new ActiveShellExpression(getParent().getShell()));
			}

			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.FocusAdapter#focusLost(org.eclipse.swt.events.FocusEvent)
			 */
			public void focusLost(FocusEvent e) {
				IHandlerService service = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);

				if (cutHandlerActivation != null) {
					service.deactivateHandler(cutHandlerActivation);
				}

				if (copyHandlerActivation != null) {
					service.deactivateHandler(copyHandlerActivation);
				}

				if (pasteHandlerActivation != null) {
					service.deactivateHandler(pasteHandlerActivation);
				}

				if (selectAllHandlerActivation != null) {
					service.deactivateHandler(selectAllHandlerActivation);
				}
			}

		});

        sourceViewer.addSelectionChangedListener(new ISelectionChangedListener() {

        	public void selectionChanged(SelectionChangedEvent event) {
        		cutAction.update();
        		copyAction.update();
        	}

        });
	}

	private void addProposals(final SubMenuManager quickFixMenu) {
		IAnnotationModel sourceModel = sourceViewer.getAnnotationModel();
		Iterator annotationIterator = sourceModel.getAnnotationIterator();
		while (annotationIterator.hasNext()) {
			Annotation annotation = (Annotation) annotationIterator.next();
			boolean isDeleted = annotation.isMarkedDeleted();
			boolean isIncluded = includes(sourceModel.getPosition(annotation),
					getTextWidget().getCaretOffset());
			boolean isFixable = sourceViewer.getQuickAssistAssistant().canFix(
					annotation);
			if (!isDeleted && isIncluded && isFixable) {
				IQuickAssistProcessor processor = sourceViewer
				.getQuickAssistAssistant()
				.getQuickAssistProcessor();
				IQuickAssistInvocationContext context = sourceViewer
				.getQuickAssistInvocationContext();
				ICompletionProposal[] proposals = processor
				.computeQuickAssistProposals(context);

				for (ICompletionProposal proposal : proposals)
					quickFixMenu.add(createQuickFixAction(proposal));
			}
		}
	}

	private boolean includes(Position position, int caretOffset) {
		return position.includes(caretOffset)
		|| (position.offset + position.length) == caretOffset;
	}

	private IAction createQuickFixAction(final ICompletionProposal proposal) {
		return new Action(proposal.getDisplayString()) {

			public void run() {
				proposal.apply(sourceViewer.getDocument());
			}

			public ImageDescriptor getImageDescriptor() {
				Image image = proposal.getImage();
				if (image != null) {
					return ImageDescriptor.createFromImage(image);
				}
				return null;
			}
		};
	}

	private IHandlerService getHandlerService() {
		final IHandlerService handlerService = (IHandlerService) PlatformUI
		.getWorkbench().getService(IHandlerService.class);
		return handlerService;
	}

	private SourceViewerDecorationSupport configureAnnotationPreferences() {
		ISharedTextColors textColors = EditorsUI.getSharedTextColors();
		IAnnotationAccess annotationAccess = new DefaultMarkerAnnotationAccess();
		final SourceViewerDecorationSupport support = new SourceViewerDecorationSupport(
				sourceViewer, null, annotationAccess, textColors);

		List annotationPreferences = new MarkerAnnotationPreferences()
		.getAnnotationPreferences();
		Iterator e = annotationPreferences.iterator();
		while (e.hasNext())
			support.setAnnotationPreference((AnnotationPreference) e.next());

		support.install(EditorsUI.getPreferenceStore());
		return support;
	}

	private void createMarginPainter() {
		MarginPainter marginPainter = new MarginPainter(sourceViewer);
		marginPainter.setMarginRulerColumn(65);
		marginPainter.setMarginRulerColor(Display.getDefault().getSystemColor(
				SWT.COLOR_GRAY));
		sourceViewer.addPainter(marginPainter);
	}

	/**
	 * @return widget
	 */
	public StyledText getTextWidget() {
		return sourceViewer.getTextWidget();
	}

	private IHandlerActivation installQuickFixActionHandler() {
		IHandlerService handlerService = getHandlerService();
		ActionHandler handler = createQuickFixActionHandler(sourceViewer);
		ActiveShellExpression expression = new ActiveShellExpression(
				sourceViewer.getTextWidget().getShell());
		return handlerService.activateHandler(
				ITextEditorActionDefinitionIds.QUICK_ASSIST, handler,
				expression);
	}

	private ActionHandler createQuickFixActionHandler(
			final ITextOperationTarget textOperationTarget) {
		Action quickFixAction = new Action() {
			/*
			 * (non-Javadoc)
			 *
			 * @see org.eclipse.jface.action.Action#run()
			 */
			public void run() {
				textOperationTarget.doOperation(ISourceViewer.QUICK_ASSIST);
			}
		};
		quickFixAction
		.setActionDefinitionId(ITextEditorActionDefinitionIds.QUICK_ASSIST);
		return new ActionHandler(quickFixAction);
	}

	/**
	 * @return text
	 */
	public String getText() {
		return getTextWidget().getText();
	}

	/**
	 * @param text
	 */
	public void setText(String text) {
		getTextWidget().setText(text);
	}

	/**
	 *
	 */
	public boolean setFocus() {
		return getTextWidget().setFocus();
	}

}
