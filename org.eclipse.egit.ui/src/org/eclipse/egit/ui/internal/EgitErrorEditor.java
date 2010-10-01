/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

/**
 * An error editor displayed when opening an editor failed
 */
public class EgitErrorEditor extends EditorPart implements
		IEditorMatchingStrategy {

	private static final class StatusInput implements IEditorInput {
		private final IStatus myStatus;

		private final String name;

		private final String tooltip;

		public StatusInput(IStatus status, String title, String tooltip) {
			this.myStatus = status;
			this.name = title != null ? title : UIText.EgitErrorEditor_Title;
			this.tooltip = tooltip != null ? tooltip
					: UIText.EgitErrorEditor_Tooltip;
		}

		public IStatus getStatus() {
			return myStatus;
		}

		public Object getAdapter(Class adapter) {
			return null;
		}

		public String getToolTipText() {
			return this.tooltip;
		}

		public IPersistableElement getPersistable() {
			return null;
		}

		public String getName() {
			return this.name;
		}

		public ImageDescriptor getImageDescriptor() {
			return PlatformUI.getWorkbench().getSharedImages()
					.getImageDescriptor(ISharedImages.IMG_ELCL_STOP);
		}

		public boolean exists() {
			return false;
		}
	}

	/**
	 * The Editor ID
	 */
	public static final String EDITOR_ID = "org.eclipse.egit.ui.errorEditor"; //$NON-NLS-1$

	private EGitStatusPart statusControl;

	private IStatus errorStatus;

	/**
	 * @param status
	 *            the status
	 * @param title
	 *            the title for the error editor, may be null
	 * @param tooltip
	 *            the tool tip for the error editor, may be null
	 * @return the input
	 */
	public static IEditorInput createInput(final IStatus status, String title,
			String tooltip) {
		return new StatusInput(status, title, tooltip);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing to save
	}

	@Override
	public void doSaveAs() {
		// nothing to save
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		if (input instanceof StatusInput) {
			this.errorStatus = ((StatusInput) input).getStatus();

			if (this.statusControl != null)
				this.statusControl.changeReason(this.errorStatus);
		}
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		if (errorStatus != null)
			statusControl = new EGitStatusPart(parent, errorStatus);
		setTitleImage(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_ERROR_TSK)
				.createImage());
	}

	@Override
	public void setFocus() {
		// nothing
	}

	public boolean matches(IEditorReference editorRef, IEditorInput input) {
		return input instanceof StatusInput;
	}
}
