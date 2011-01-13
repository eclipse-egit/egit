/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Some utilities for UI code
 */
public class UIUtils {
	/**
	 * these activate the content assist; alphanumeric,
	 * space plus some expected special chars
	 */
	public static final char[] VALUE_HELP_ACTIVATIONCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123457890*@ <>".toCharArray(); //$NON-NLS-1$

	/**
	 * Handles a "previously used values" content assist.
	 * <p>
	 * Adding this to a text field will enable "content assist" by keeping track
	 * of the previously used valued for this field. The previously used values
	 * will be shown in the order they were last used (most recently used ones
	 * coming first in the list) and the number of entries is limited.
	 * <p>
	 * A "bulb" decorator will indicate that content assist is available for the
	 * field, and a tool tip is provided giving more information.
	 * <p>
	 * Content assist is activated by either typing in the field or by using a
	 * dedicated key stroke which is indicated in the tool tip. The list will be
	 * filtered with the content already in the text field with '*' being usable
	 * as wild card.
	 * <p>
	 * Note that the application must issue a call to {@link #updateProposals()}
	 * in order to add a new value to the "previously used values" list.
	 * <p>
	 * The list will be persisted in the plug-in dialog settings.
	 *
	 * @noextend not to be extended by clients
	 * @noimplement not to be implemented by clients, use
	 *              {@link UIUtils#addPreviousValuesContentProposalToText(Text, String)}
	 *              to create instances of this
	 */
	public interface IPreviousValueProposalHandler {
		/**
		 * Updates the proposal list from the value in the text field.
		 * <p>
		 * The value will be truncated to the first 2000 characters in order to
		 * limit data size.
		 * <p>
		 * Note that this must be called in the UI thread, since it accesses the
		 * text field.
		 * <p>
		 * If the value is already in the list, it will become the first entry,
		 * otherwise it will be added at the beginning. Note that empty Strings
		 * will not be stored. The length of the list is limited, and the
		 * "oldest" entries will be removed once the limit is exceeded.
		 * <p>
		 * This call should only be issued if the value in the text field is
		 * "valid" in terms of the application.
		 */
		public void updateProposals();
	}

	/**
	 * @param id see {@link FontRegistry#get(String)}
	 * @return the font
	 */
	public static Font getFont(final String id) {
		return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme()
				.getFontRegistry().get(id);
	}

	/**
	 * @param id see {@link FontRegistry#getBold(String)}
	 * @return the font
	 */
	public static Font getBoldFont(final String id) {
		return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme()
				.getFontRegistry().getBold(id);
	}

	/**
	 * Adds little bulb decoration to given control. Bulb will appear in top left
	 * corner of control after giving focus for this control.
	 *
	 * After clicking on bulb image text from <code>tooltip</code> will appear.
	 *
	 * @param control instance of {@link Control} object with should be decorated
	 * @param tooltip text value which should appear after clicking on bulb image.
	 */
	public static void addBulbDecorator(final Control control, final String tooltip) {
		ControlDecoration dec = new ControlDecoration(control, SWT.TOP | SWT.LEFT);

		dec.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(
				FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());

		dec.setShowOnlyOnFocus(true);
		dec.setShowHover(true);

		dec.setDescriptionText(tooltip);
	}

	/**
	 * Adds a "previously used values" content proposal handler to a text field.
	 * <p>
	 * The keyboard shortcut will be "M1+SPACE" and the list will be limited to
	 * 10 values.
	 *
	 * @param textField
	 *            the text field
	 * @param preferenceKey
	 *            the key under which to store the "previously used values" in
	 *            the dialog settings
	 * @return the handler the proposal handler
	 */
	public static IPreviousValueProposalHandler addPreviousValuesContentProposalToText(
			final Text textField, final String preferenceKey) {
		KeyStroke stroke;
		try {
			stroke = KeyStroke.getInstance("M1+SPACE"); //$NON-NLS-1$
			addBulbDecorator(textField, NLS.bind(
					UIText.UIUtils_PressShortcutMessage, stroke.format()));
		} catch (ParseException e1) {
			Activator.handleError(e1.getMessage(), e1, false);
			stroke = null;
			addBulbDecorator(textField,
					UIText.UIUtils_StartTypingForPreviousValuesMessage);
		}

		IContentProposalProvider cp = new IContentProposalProvider() {

			public IContentProposal[] getProposals(String contents, int position) {

				List<IContentProposal> resultList = new ArrayList<IContentProposal>();

				// make the simplest possible pattern check: allow "*"
				// for multiple characters
				String patternString = contents;
				// ignore spaces in the beginning
				while (patternString.length() > 0
						&& patternString.charAt(0) == ' ') {
					patternString = patternString.substring(1);
				}

				// we quote the string as it may contain spaces
				// and other stuff colliding with the Pattern
				patternString = Pattern.quote(patternString);

				patternString = patternString.replaceAll("\\x2A", ".*"); //$NON-NLS-1$ //$NON-NLS-2$

				// make sure we add a (logical) * at the end
				if (!patternString.endsWith(".*")) { //$NON-NLS-1$
					patternString = patternString + ".*"; //$NON-NLS-1$
				}

				// let's compile a case-insensitive pattern (assumes ASCII only)
				Pattern pattern;
				try {
					pattern = Pattern.compile(patternString,
							Pattern.CASE_INSENSITIVE);
				} catch (PatternSyntaxException e) {
					pattern = null;
				}

				String[] proposals = org.eclipse.egit.ui.Activator.getDefault()
						.getDialogSettings().getArray(preferenceKey);

				if (proposals != null)
					for (final String uriString : proposals) {

						if (pattern != null
								&& !pattern.matcher(uriString).matches())
							continue;

						IContentProposal propsal = new IContentProposal() {

							public String getLabel() {
								return null;
							}

							public String getDescription() {
								return null;
							}

							public int getCursorPosition() {
								return 0;
							}

							public String getContent() {
								return uriString;
							}
						};
						resultList.add(propsal);
					}

				return resultList.toArray(new IContentProposal[resultList
						.size()]);
			}
		};

		ContentProposalAdapter adapter = new ContentProposalAdapter(textField,
				new TextContentAdapter(), cp, stroke,
				VALUE_HELP_ACTIVATIONCHARS);
		// set the acceptance style to always replace the complete content
		adapter
				.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

		return new IPreviousValueProposalHandler() {
			public void updateProposals() {
				String value = textField.getText();
				// don't store empty values
				if (value.length() > 0) {
					// we don't want to save too much in the preferences
					if (value.length() > 2000) {
						value = value.substring(0, 1999);
					}
					// now we need to mix the value into the list
					IDialogSettings settings = org.eclipse.egit.ui.Activator
							.getDefault().getDialogSettings();
					String[] existingValues = settings.getArray(preferenceKey);
					if (existingValues == null) {
						existingValues = new String[] { value };
						settings.put(preferenceKey, existingValues);
					} else {

						List<String> values = new ArrayList<String>(
								existingValues.length + 1);

						for (String existingValue : existingValues)
							values.add(existingValue);
						// if it is already the first value, we don't need to do
						// anything
						if (values.indexOf(value) == 0)
							return;

						values.remove(value);
						// we insert at the top
						values.add(0, value);
						// make sure to not store more than the maximum number
						// of values
						while (values.size() > 10)
							values.remove(values.size() - 1);

						settings.put(preferenceKey, values
								.toArray(new String[values.size()]));
					}
				}
			}
		};
	}

}
