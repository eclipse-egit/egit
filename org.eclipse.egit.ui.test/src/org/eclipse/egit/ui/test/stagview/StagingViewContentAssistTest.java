/*******************************************************************************
 * Copyright (C) 2026 Lars Vogel <Lars.Vogel@vogella.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.stagview;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.commands.Command;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.egit.ui.common.StagingViewTester;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.junit.Test;

/**
 * Tests that content assist in the Staging view commit message area survives a
 * perspective switch. The spurious {@code SWT.Deactivate} triggering the
 * regression is platform specific (notably Windows); elsewhere these tests
 * pass without exercising it.
 */
public class StagingViewContentAssistTest extends AbstractStagingViewTestCase {

	// ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS (inlined to avoid
	// an extra bundle dependency).
	private static final String CONTENT_ASSIST = "org.eclipse.ui.edit.text.contentAssist.proposals";
	private static final String RESOURCE_PERSPECTIVE_ID = "org.eclipse.ui.resourcePerspective";
	private static final String JAVA_PERSPECTIVE_ID = "org.eclipse.jdt.ui.JavaPerspective";

	// HandlerServiceImpl.H_ID (inlined to avoid an extra bundle dependency).
	private static final String HANDLER_PREFIX = "handler::";

	// LegacyHandlerService logs conflicting handlers, it does not throw.
	private static final String CONFLICTING_HANDLERS = "Conflicting handlers";

	@Test
	public void testContentAssistAvailableAfterPerspectiveSwitch()
			throws Exception {
		setContent("Some change in the working tree");
		StagingViewTester staging = StagingViewTester.openStagingView();

		focusCommitMessage(staging);
		awaitContentAssistHandler(staging);

		switchPerspectiveAndBack();

		// Still registered: the commit message kept the focus across the switch.
		awaitContentAssistHandler(staging);
	}

	@Test
	public void testNoConflictingHandlersOnPerspectiveSwitch()
			throws Exception {
		setContent("Some change in the working tree");
		StagingViewTester staging = StagingViewTester.openStagingView();

		// Register both the tree and commit message copy handlers: the bug
		// 536645 conflict constellation.
		staging.getView().bot().tree(0).setFocus();
		focusCommitMessage(staging);

		ConflictLogListener listener = new ConflictLogListener();
		Platform.addLogListener(listener);
		try {
			switchPerspectiveAndBack();
			awaitContentAssistHandler(staging);
			resolveCopyHandler(staging);
		} finally {
			Platform.removeLogListener(listener);
		}
		assertFalse("Logged a conflicting handler error: "
				+ listener.getMessage(), listener.hasConflict());
	}

	@Test
	public void testNoConflictingHandlersWhenFocusMovesAfterPerspectiveSwitch()
			throws Exception {
		setContent("Some change in the working tree");
		StagingViewTester staging = StagingViewTester.openStagingView();

		// The tree has the focus when the view is hidden; on return the focus
		// goes to the commit message, which must not end up with a second
		// handler for copy.
		staging.getView().bot().tree(0).setFocus();

		ConflictLogListener listener = new ConflictLogListener();
		Platform.addLogListener(listener);
		try {
			switchPerspectiveAndBack();
			focusCommitMessage(staging);
			resolveCopyHandler(staging);
		} finally {
			Platform.removeLogListener(listener);
		}
		assertFalse("Logged a conflicting handler error: "
				+ listener.getMessage(), listener.hasConflict());
	}

	// Resolving the copy handler in the Staging view's part context makes the
	// handler service log a conflict if several handlers are active for it.
	private static void resolveCopyHandler(StagingViewTester staging) {
		IViewPart part = staging.getView().getViewReference().getView(false);
		UIThreadRunnable.syncExec(() -> {
			IEclipseContext context = part.getSite()
					.getService(IEclipseContext.class);
			context.get(HANDLER_PREFIX + IWorkbenchCommandConstants.EDIT_COPY);
		});
	}

	private static void focusCommitMessage(StagingViewTester staging) {
		SWTBotView view = staging.getView();
		SWTBotStyledText commitMessage = view.bot()
				.styledTextWithLabel(UIText.StagingView_CommitMessage);
		StyledText widget = commitMessage.widget;
		boolean focused = UIThreadRunnable.syncExec(widget.getDisplay(),
				() -> Boolean.valueOf(widget.setFocus())).booleanValue();
		assertTrue("Could not focus the commit message area", focused);
	}

	private void awaitContentAssistHandler(StagingViewTester staging) {
		staging.getView().bot().waitUntil(new DefaultCondition() {

			@Override
			public boolean test() {
				return isContentAssistHandled();
			}

			@Override
			public String getFailureMessage() {
				return "Content assist handler is not registered";
			}
		}, 10000);
	}

	private static boolean isContentAssistHandled() {
		return UIThreadRunnable.syncExec(() -> {
			ICommandService service = PlatformUI.getWorkbench()
					.getService(ICommandService.class);
			Command command = service.getCommand(CONTENT_ASSIST);
			return Boolean.valueOf(command.isHandled());
		}).booleanValue();
	}

	private static void switchPerspectiveAndBack() {
		UIThreadRunnable.syncExec(() -> {
			IWorkbenchPage page = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage();
			IPerspectiveDescriptor original = page.getPerspective();
			page.setPerspective(otherPerspective(original));
			page.setPerspective(original);
		});
	}

	private static IPerspectiveDescriptor otherPerspective(
			IPerspectiveDescriptor original) {
		String otherId = RESOURCE_PERSPECTIVE_ID;
		if (original != null
				&& RESOURCE_PERSPECTIVE_ID.equals(original.getId())) {
			otherId = JAVA_PERSPECTIVE_ID;
		}
		return PlatformUI.getWorkbench().getPerspectiveRegistry()
				.findPerspectiveWithId(otherId);
	}

	private static final class ConflictLogListener implements ILogListener {

		private volatile String message;

		@Override
		public void logging(IStatus status, String plugin) {
			String text = status.getMessage();
			if (text != null && text.contains(CONFLICTING_HANDLERS)) {
				message = text;
			}
		}

		boolean hasConflict() {
			return message != null;
		}

		String getMessage() {
			return message;
		}
	}
}
