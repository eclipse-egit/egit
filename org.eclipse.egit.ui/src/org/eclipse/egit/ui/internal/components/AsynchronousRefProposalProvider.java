/*******************************************************************************
 * Copyright (C) 2018, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.UIUtils.ExplicitContentProposalAdapter;
import org.eclipse.egit.ui.UIUtils.IContentProposalCandidateProvider;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CancelableFuture;
import org.eclipse.egit.ui.internal.dialogs.NonBlockingWizardDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * An {@link IContentProposalCandidateProvider} that is intended to be used with
 * an asynchronous {@link CancelableFuture} to get proposals in the background.
 * <p>
 * Note that is must be used with an {@link ExplicitContentProposalAdapter},
 * which must be made known to it via
 * {@link #setContentProposalAdapter(ExplicitContentProposalAdapter)
 * setContentProposalAdapter()}.
 */
public class AsynchronousRefProposalProvider
		implements IContentProposalCandidateProvider<Ref> {

	private final IWizardContainer container;

	private final Text textField;

	private final Supplier<String> uriProvider;

	private final Function<String, CancelableFuture<Collection<Ref>>> listProvider;

	private ExplicitContentProposalAdapter contentProposer;

	/**
	 * Creates a new {@link AsynchronousRefProposalProvider}. Because this is
	 * supposed to run truly asynchronously, typically in a
	 * {@link NonBlockingWizardDialog}, it needs to know the text field it
	 * belongs to and also the URI it was run for. It opens the proposals only
	 * if both are in a state where it still makes sense to show the proposals.
	 *
	 * @param container
	 *            this candidate provider will be run for
	 * @param uriProvider
	 *            a function returning the upstream URI to get proposals from
	 * @param textField
	 *            this candidate provider belongs to
	 * @param listProvider
	 *            a function that provides the CancelableFuture used to obtain
	 *            the upstream refs
	 */
	public AsynchronousRefProposalProvider(
			IWizardContainer container, Text textField,
			Supplier<String> uriProvider,
			Function<String, CancelableFuture<Collection<Ref>>> listProvider) {
		this.container = container;
		this.textField = textField;
		this.uriProvider = uriProvider;
		this.listProvider = listProvider;
	}

	/**
	 * Makes the content proposal adapter known to this candidate provider. This
	 * is needed to be able to open the proposal popup asynchronously. If set to
	 * {@code null}, proposals will not be opened.
	 *
	 * @param adapter
	 *            to set
	 */
	public void setContentProposalAdapter(
			ExplicitContentProposalAdapter adapter) {
		contentProposer = adapter;
	}

	@Override
	public Collection<? extends Ref> getCandidates() {
		String uri = uriProvider.get();
		if (uri == null) {
			return null;
		}
		CancelableFuture<Collection<Ref>> list = listProvider.apply(uri);
		try {
			if (!list.isFinished()) {
				IRunnableWithProgress operation = monitor -> {
					monitor.beginTask(MessageFormat.format(
							UIText.AsynchronousRefProposalProvider_FetchingRemoteRefsMessage,
							uri), IProgressMonitor.UNKNOWN);
					Collection<Ref> result = list.get();
					if (monitor.isCanceled()) {
						return;
					}
					// If we get here, the ChangeList future is done.
					if (result == null || result.isEmpty()) {
						// Don't bother if we didn't get any results
						return;
					}
					// If we do have results now, open the proposals.
					Job showProposals = new WorkbenchJob(
							UIText.AsynchronousRefProposalProvider_ShowingProposalsJobName) {

						@Override
						public boolean shouldRun() {
							return super.shouldRun() && contentProposer != null;
						}

						@Override
						public IStatus runInUIThread(
								IProgressMonitor uiMonitor) {
							// But only if we're not disposed, the focus is
							// still (or again) in the Change field, and the uri
							// is still the same
							try {
								if (container instanceof NonBlockingWizardDialog) {
									// Otherwise the dialog was blocked anyway,
									// and focus will be restored
									if (textField.isDisposed()
											|| !textField.isVisible()
											|| textField != textField
													.getDisplay()
													.getFocusControl()) {
										return Status.CANCEL_STATUS;
									}
									String uriNow = uriProvider.get();
									if (!uriNow.equals(uri)) {
										return Status.CANCEL_STATUS;
									}
								}
								contentProposer.openProposalPopup();
							} catch (SWTException e) {
								// Disposed already
								return Status.CANCEL_STATUS;
							} finally {
								uiMonitor.done();
							}
							return Status.OK_STATUS;
						}

					};
					showProposals.schedule();
				};
				if (container instanceof NonBlockingWizardDialog) {
					NonBlockingWizardDialog dialog = (NonBlockingWizardDialog) container;
					dialog.run(operation, () -> list
							.cancel(CancelableFuture.CancelMode.ABANDON));
				} else {
					container.run(true, true, operation);
				}
				return null;
			}
			return list.get();
		} catch (InterruptedException | InvocationTargetException e) {
			return null;
		}
	}

}
