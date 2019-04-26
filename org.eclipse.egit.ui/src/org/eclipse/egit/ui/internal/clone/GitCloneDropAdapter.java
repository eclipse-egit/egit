/*******************************************************************************
 * Copyright (c) 2011, 2017 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Ian Pun - reimplemented to work with Git Cloning DND using MarketplaceDropAdapter
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.repository.tree.command.CloneCommand;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

/**
 * Adapter to listen for any Drag and Drop operations that transfer a valid git
 * URL. If it goes through the URL parser correctly, a Clone Git Repo wizard
 * will appear and be populated.
 */
public class GitCloneDropAdapter implements IStartup {

	private static final int[] PREFERRED_DROP_OPERATIONS = { DND.DROP_DEFAULT,
			DND.DROP_COPY, DND.DROP_MOVE, DND.DROP_LINK };

	private static final int DROP_OPERATIONS = DND.DROP_MOVE | DND.DROP_COPY
			| DND.DROP_LINK | DND.DROP_DEFAULT;

	private final DropTargetAdapter dropListener = new GitDropTargetListener();

	private final WorkbenchListener workbenchListener = new WorkbenchListener();

	private Transfer[] transferAgents;

	@Override
	public void earlyStartup() {
		UIJob registerJob = new UIJob(PlatformUI.getWorkbench().getDisplay(),
				"Git Clone DND Initialization") { //$NON-NLS-1$
			{
				setPriority(Job.SHORT);
				setSystem(true);
			}

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IWorkbench workbench = PlatformUI.getWorkbench();
				workbench.addWindowListener(workbenchListener);
				IWorkbenchWindow[] workbenchWindows = workbench
						.getWorkbenchWindows();
				for (IWorkbenchWindow window : workbenchWindows) {
					workbenchListener.hookWindow(window);
				}
				return Status.OK_STATUS;
			}

		};
		registerJob.schedule();
	}

	private void installDropTarget(final Shell shell) {
		hookUrlTransfer(shell, dropListener);
	}

	private DropTarget hookUrlTransfer(final Shell shell,
			DropTargetAdapter dropAdapter) {
		DropTarget target = findDropTarget(shell);
		if (target != null) {
			// target exists, get it and check proper registration
			registerWithExistingTarget(target);
		} else {
			target = new DropTarget(shell, DROP_OPERATIONS);
			if (transferAgents == null) {
				transferAgents = new Transfer[] { URLTransfer.getInstance() };
			}
			target.setTransfer(transferAgents);
		}
		registerDropListener(target, dropAdapter);

		Control[] children = shell.getChildren();
		for (Control child : children) {
			hookRecursive(child, dropAdapter);
		}
		return target;
	}

	private void registerDropListener(DropTarget target,
			DropTargetListener dropAdapter) {
		target.removeDropListener(dropAdapter);
		target.addDropListener(dropAdapter);
	}

	private void hookRecursive(Control child, DropTargetListener dropAdapter) {
		DropTarget childTarget = findDropTarget(child);
		if (childTarget != null) {
			registerWithExistingTarget(childTarget);
			registerDropListener(childTarget, dropAdapter);
		}
		if (child instanceof Composite) {
			Composite composite = (Composite) child;
			Control[] children = composite.getChildren();
			for (Control control : children) {
				hookRecursive(control, dropAdapter);
			}
		}
	}

	private void registerWithExistingTarget(DropTarget target) {
		Transfer[] transfers = target.getTransfer();
		if (transfers != null) {
			for (Transfer transfer : transfers) {
				if (transfer instanceof URLTransfer) {
					return;
				}
			}
			Transfer[] newTransfers = new Transfer[transfers.length + 1];
			System.arraycopy(transfers, 0, newTransfers, 0, transfers.length);
			newTransfers[transfers.length] = URLTransfer.getInstance();
			target.setTransfer(newTransfers);
		}
	}

	private DropTarget findDropTarget(Control control) {
		Object object = control.getData(DND.DROP_TARGET_KEY);
		if (object instanceof DropTarget) {
			return (DropTarget) object;
		}
		return null;
	}

	/**
	 * @param url
	 */
	protected void proceedClone(String url) {
		CloneCommand command = new CloneCommand(url);
		try {
			command.execute(new ExecutionEvent());
		} catch (ExecutionException e) {
			Activator.logError(e.getLocalizedMessage(), e);
		}
	}

	private class GitDropTargetListener extends DropTargetAdapter {

		@Override
		public void dragEnter(DropTargetEvent e) {
			updateDragDetails(e);
		}

		@Override
		public void dragOver(DropTargetEvent e) {
			updateDragDetails(e);
		}

		@Override
		public void dragLeave(DropTargetEvent e) {
			if (e.detail == DND.DROP_NONE) {
				setDropOperation(e);
			}
		}

		@Override
		public void dropAccept(DropTargetEvent e) {
			updateDragDetails(e);
		}

		@Override
		public void dragOperationChanged(DropTargetEvent e) {
			updateDragDetails(e);
		}

		private void setDropOperation(DropTargetEvent e) {
			int allowedOperations = e.operations;
			for (int op : PREFERRED_DROP_OPERATIONS) {
				if ((allowedOperations & op) != 0) {
					e.detail = op;
					return;
				}
			}
			e.detail = allowedOperations;
		}

		private void updateDragDetails(DropTargetEvent e) {
			if (dropTargetIsValid(e, false)) {
				setDropOperation(e);
			}
		}

		private boolean dropTargetIsValid(DropTargetEvent e, boolean isDrop) {
			if (URLTransfer.getInstance().isSupportedType(e.currentDataType)) {
				// on Windows, we get the URL already during drag operations...
				// FIXME find a way to check the URL early on other platforms,
				// too...
				if (isDrop || Util.isWindows()) {
					if (e.data == null && !extractEventData(e)) {
						// ... but if we don't, it's no problem, unless this is
						// already the final drop event
						return !isDrop;
					}
					final String url = getUrl(e.data);
					if (!GitUrlChecker.isValidGitUrl(url)) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		private boolean extractEventData(DropTargetEvent e) {
			TransferData transferData = e.currentDataType;
			if (transferData != null) {
				Object data = URLTransfer.getInstance()
						.nativeToJava(transferData);
				if (data != null && getUrl(data) != null) {
					e.data = data;
					return true;
				}
			}
			return false;
		}

		@Override
		public void drop(DropTargetEvent event) {
			if (!URLTransfer.getInstance()
					.isSupportedType(event.currentDataType)) {
				// ignore
				return;
			}
			if (event.data == null) {
				// reject
				event.detail = DND.DROP_NONE;
				return;
			}
			if (!dropTargetIsValid(event, true)) {
				// reject
				event.detail = DND.DROP_NONE;
				return;
			}
			final String url = getUrl(event.data);
			DropTarget source = (DropTarget) event.getSource();
			Display display = source.getDisplay();
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					proceedClone(url);
				}
			});

		}

		private String getUrl(Object eventData) {
			if (!(eventData instanceof String)) {
				return null;
			}
			// Depending on the form the link and browser/os,
			// we get the url twice in the data separated by new lines
			String[] dataLines = ((String) eventData)
					.split(System.getProperty("line.separator")); //$NON-NLS-1$
			String url = dataLines[0];
			return url;
		}
	}

	private class WorkbenchListener implements IPartListener2, IPageListener,
			IPerspectiveListener, IWindowListener {

		@Override
		public void perspectiveActivated(IWorkbenchPage page,
				IPerspectiveDescriptor perspective) {
			pageChanged(page);
		}

		@Override
		public void perspectiveChanged(IWorkbenchPage page,
				IPerspectiveDescriptor perspective, String changeId) {
			// Nothing to do
		}

		@Override
		public void pageActivated(IWorkbenchPage page) {
			pageChanged(page);
		}

		@Override
		public void pageClosed(IWorkbenchPage page) {
			// Nothing to do
		}

		@Override
		public void pageOpened(IWorkbenchPage page) {
			pageChanged(page);
		}

		private void pageChanged(IWorkbenchPage page) {
			if (page == null) {
				return;
			}
			IWorkbenchWindow workbenchWindow = page.getWorkbenchWindow();
			windowChanged(workbenchWindow);
		}

		@Override
		public void windowActivated(IWorkbenchWindow window) {
			windowChanged(window);
		}

		private void windowChanged(IWorkbenchWindow window) {
			if (window == null) {
				return;
			}
			Shell shell = window.getShell();
			runUpdate(shell);
		}

		@Override
		public void windowDeactivated(IWorkbenchWindow window) {
			// Nothing to do
		}

		@Override
		public void windowClosed(IWorkbenchWindow window) {
			// Nothing to do
		}

		@Override
		public void windowOpened(IWorkbenchWindow window) {
			hookWindow(window);
		}

		public void hookWindow(IWorkbenchWindow window) {
			if (window == null) {
				return;
			}
			window.addPageListener(this);
			window.addPerspectiveListener(this);
			IPartService partService = window.getService(IPartService.class);
			partService.addPartListener(this);
			windowChanged(window);
		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			partUpdate(partRef);
		}

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			partUpdate(partRef);
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
			partUpdate(partRef);
		}

		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
			// Nothing to do
		}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			partUpdate(partRef);
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {
			partUpdate(partRef);
		}

		@Override
		public void partHidden(IWorkbenchPartReference partRef) {
			partUpdate(partRef);
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
			// Nothing to do
		}

		private void partUpdate(IWorkbenchPartReference partRef) {
			if (partRef == null) {
				return;
			}
			IWorkbenchPage page = partRef.getPage();
			pageChanged(page);
		}

		private void runUpdate(final Shell shell) {
			if (shell == null || shell.isDisposed()) {
				return;
			}
			Display display = shell.getDisplay();
			if (display == null || display.isDisposed()) {
				return;
			}
			try {
				display.asyncExec(new Runnable() {

					@Override
					public void run() {
						if (!shell.isDisposed()) {
							installDropTarget(shell);
						}
					}
				});
			} catch (RuntimeException ex) {
				// Swallow
			}
		}
	}
}
