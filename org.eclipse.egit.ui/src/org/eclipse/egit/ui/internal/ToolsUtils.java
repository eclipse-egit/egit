/*
 * Copyright (C) 2018-2020, Andre Bossert <andre.bossert@siemens.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.egit.ui.internal;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.Activator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;

/**
 * @author anb0s
 *
 */
public class ToolsUtils {

	/**
	 * @param textHeader
	 * @param message
	 * @return yes or no
	 */
	public static int askUserAboutToolExecution(String textHeader,
			String message) {
		AtomicInteger result = new AtomicInteger();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				MessageBox mbox = new MessageBox(
						Display.getDefault().getActiveShell(),
						SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
				mbox.setText(textHeader);
				mbox.setMessage(message);
				result.set(mbox.open());
			}
		};
		if (Display.getCurrent() == null) {
			PlatformUI.getWorkbench().getDisplay().syncExec(runnable);
		} else {
			runnable.run();
		}
		return result.get();
	}

	/**
	 * @param textHeader
	 * @param message
	 */
	public static void informUserAboutError(String textHeader, String message) {
		IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
				message);
		Runnable runnable = () -> ErrorDialog.openError(null, textHeader,
				null, status);
		if (Display.getCurrent() == null) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		} else {
			runnable.run();
		}
	}

	/**
	 * Inform the user about something
	 *
	 * @param textHeader
	 *            The title
	 * @param message
	 *            the message
	 */
	public static void informUser(String textHeader, String message) {
		Runnable runnable = () -> MessageDialog.openInformation(null,
				textHeader, message);
		if (Display.getCurrent() == null) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		} else {
			runnable.run();
		}
	}
}
