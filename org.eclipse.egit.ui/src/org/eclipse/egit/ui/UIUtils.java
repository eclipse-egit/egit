/*******************************************************************************
 * Copyright (c) 2010, 2018 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.RepositorySaveableFilter;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.RefContentProposal;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.bindings.Trigger;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Some utilities for UI code
 */
public class UIUtils {

	/** Default image descriptor for files */
	public static final ImageDescriptor DEFAULT_FILE_IMG = PlatformUI
			.getWorkbench().getSharedImages()
			.getImageDescriptor(ISharedImages.IMG_OBJ_FILE);

	/**
	 * these activate the content assist; alphanumeric, space plus some expected
	 * special chars
	 */
	private static final char[] VALUE_HELP_ACTIVATIONCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123457890*@ <>".toCharArray(); //$NON-NLS-1$

	/**
	 * A keystroke for a "submit" action, see {@link #isSubmitKeyEvent(KeyEvent)}
	 */
	public static final KeyStroke SUBMIT_KEY_STROKE = KeyStroke.getInstance(SWT.MOD1, SWT.CR);

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
	 * A provider of candidate elements for which content proposals may be
	 * generated.
	 *
	 * @param <T>
	 *            type of the candidate elements
	 */
	public interface IContentProposalCandidateProvider<T> {

		/**
		 * Retrieves the collection of candidates eligible for content proposal
		 * generation.
		 *
		 * @return collection of candidates
		 */
		public Collection<? extends T> getCandidates();
	}

	/**
	 * A factory for creating {@link IContentProposal}s for {@link Ref}s.
	 *
	 * @param <T>
	 *            type of elements to create proposals for
	 */
	public interface IContentProposalFactory<T> {

		/**
		 * Gets a new {@link IContentProposal} for the given element. May or may
		 * not consider the {@link Pattern} and creates a proposal only if it
		 * matches the element with implementation-defined semantics.
		 *
		 * @param pattern
		 *            constructed from current input to aid in selecting
		 *            meaningful proposals; may be {@code null}
		 * @param element
		 *            to consider creating a proposal for
		 * @return a new {@link IContentProposal}, or {@code null} if none
		 */
		public IContentProposal getProposal(Pattern pattern, T element);
	}

	/**
	 * A {@link ContentProposalAdapter} with a <em>public</em>
	 * {@link #openProposalPopup()} method.
	 */
	public static class ExplicitContentProposalAdapter
			extends ContentProposalAdapter {

		/**
		 * Construct a content proposal adapter that can assist the user with
		 * choosing content for the field.
		 *
		 * @param control
		 *            the control for which the adapter is providing content
		 *            assist. May not be {@code null}.
		 * @param controlContentAdapter
		 *            the {@link IControlContentAdapter} used to obtain and
		 *            update the control's contents as proposals are accepted.
		 *            May not be {@code null}.
		 * @param proposalProvider
		 *            the {@link IContentProposalProvider}> used to obtain
		 *            content proposals for this control.
		 * @param keyStroke
		 *            the keystroke that will invoke the content proposal popup.
		 *            If this value is {@code null}, then proposals will be
		 *            activated automatically when any of the auto activation
		 *            characters are typed.
		 * @param autoActivationCharacters
		 *            characters that trigger auto-activation of content
		 *            proposal. If specified, these characters will trigger
		 *            auto-activation of the proposal popup, regardless of
		 *            whether an explicit invocation keyStroke was specified. If
		 *            this parameter is {@code null}, then only a specified
		 *            keyStroke will invoke content proposal. If this parameter
		 *            is {@code null} and the keyStroke parameter is
		 *            {@code null}, then all alphanumeric characters will
		 *            auto-activate content proposal.
		 */
		public ExplicitContentProposalAdapter(Control control,
				IControlContentAdapter controlContentAdapter,
				IContentProposalProvider proposalProvider,
				KeyStroke keyStroke, char[] autoActivationCharacters) {
			super(control, controlContentAdapter, proposalProvider, keyStroke,
					autoActivationCharacters);
		}

		@Override
		public void openProposalPopup() {
			// Make this method accessible
			super.openProposalPopup();
		}
	}

	/**
	 * @param id
	 *            see {@link FontRegistry#get(String)}
	 * @return the font
	 */
	public static Font getFont(final String id) {
		return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme()
				.getFontRegistry().get(id);
	}

	/**
	 * @param id
	 *            see {@link FontRegistry#getBold(String)}
	 * @return the font
	 */
	public static Font getBoldFont(final String id) {
		return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme()
				.getFontRegistry().getBold(id);
	}

	/**
	 * @param id
	 *            see {@link FontRegistry#getItalic(String)}
	 * @return the font
	 */
	public static Font getItalicFont(final String id) {
		return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme()
				.getFontRegistry().getItalic(id);
	}

	/**
	 * @return the indent of controls that depend on the previous control (e.g.
	 *         a checkbox that is only enabled when the checkbox above it is
	 *         checked)
	 */
	public static int getControlIndent() {
		// Eclipse 4.3: Use LayoutConstants.getIndent once we depend on 4.3
		return 20;
	}

	/**
	 * @param parent
	 * @param style
	 * @return a text field which is read-only but can be selected
	 */
	public static Text createSelectableLabel(Composite parent, int style) {
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=71765
		Text text = new Text(parent, style | SWT.READ_ONLY);
		text.setBackground(text.getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_BACKGROUND));
		return text;
	}

	/**
	 * Adds little bulb decoration to given control. Bulb will appear in top
	 * left corner of control after giving focus for this control.
	 *
	 * After clicking on bulb image text from <code>tooltip</code> will appear.
	 *
	 * @param control
	 *            instance of {@link Control} object with should be decorated
	 * @param tooltip
	 *            text value which should appear after clicking on bulb image.
	 * @return the {@link ControlDecoration} created
	 */
	public static ControlDecoration addBulbDecorator(final Control control,
			final String tooltip) {
		ControlDecoration dec = new ControlDecoration(control, SWT.TOP
				| SWT.LEFT);

		dec.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(
				FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());

		dec.setShowOnlyOnFocus(true);
		dec.setShowHover(true);

		dec.setDescriptionText(tooltip);
		return dec;
	}

	/**
	 * Creates a simple {@link Pattern} that can be used for matching content
	 * assist proposals. The pattern ignores leading blanks and allows '*' as a
	 * wildcard matching multiple arbitrary characters.
	 *
	 * @param content
	 *            to create the pattern from
	 * @return the pattern, or {@code null} if none could be created
	 */
	public static Pattern createProposalPattern(String content) {
		// Make the simplest possible pattern check: allow "*"
		// for multiple characters.
		String patternString = content;
		// Ignore spaces in the beginning.
		while (patternString.length() > 0 && patternString.charAt(0) == ' ') {
			patternString = patternString.substring(1);
		}

		// We quote the string as it may contain spaces
		// and other stuff colliding with the pattern.
		patternString = Pattern.quote(patternString);

		patternString = patternString.replaceAll("\\x2A", ".*"); //$NON-NLS-1$ //$NON-NLS-2$

		// Make sure we add a (logical) * at the end.
		if (!patternString.endsWith(".*")) { //$NON-NLS-1$
			patternString = patternString + ".*"; //$NON-NLS-1$
		}

		// Compile a case-insensitive pattern (assumes ASCII only).
		Pattern pattern;
		try {
			pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
		} catch (PatternSyntaxException e) {
			pattern = null;
		}
		return pattern;
	}

	/**
	 * Adds a "previously used values" content proposal handler to a text field.
	 * <p>
	 * The list will be limited to 10 values.
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
		KeyStroke stroke = UIUtils
				.getKeystrokeOfBestActiveBindingFor(IWorkbenchCommandConstants.EDIT_CONTENT_ASSIST);
		if (stroke == null)
			addBulbDecorator(textField,
					UIText.UIUtils_StartTypingForPreviousValuesMessage);
		else
			addBulbDecorator(
					textField,
					NLS.bind(UIText.UIUtils_PressShortcutMessage,
							stroke.format()));

		IContentProposalProvider cp = new IContentProposalProvider() {

			@Override
			public IContentProposal[] getProposals(String contents, int position) {
				List<IContentProposal> resultList = new ArrayList<>();

				Pattern pattern = createProposalPattern(contents);
				String[] proposals = org.eclipse.egit.ui.Activator.getDefault()
						.getDialogSettings().getArray(preferenceKey);
				if (proposals != null) {
					for (final String uriString : proposals) {

						if (pattern != null
								&& !pattern.matcher(uriString).matches()) {
							continue;
						}
						IContentProposal propsal = new IContentProposal() {

							@Override
							public String getLabel() {
								return null;
							}

							@Override
							public String getDescription() {
								return null;
							}

							@Override
							public int getCursorPosition() {
								return 0;
							}

							@Override
							public String getContent() {
								return uriString;
							}
						};
						resultList.add(propsal);
					}
				}
				return resultList.toArray(new IContentProposal[resultList
						.size()]);
			}
		};

		ContentProposalAdapter adapter = new ContentProposalAdapter(textField,
				new TextContentAdapter(), cp, stroke,
				VALUE_HELP_ACTIVATIONCHARS);
		// set the acceptance style to always replace the complete content
		adapter.setProposalAcceptanceStyle(
				ContentProposalAdapter.PROPOSAL_REPLACE);

		return new IPreviousValueProposalHandler() {
			@Override
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

						List<String> values = new ArrayList<>(
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

	/**
	 * Adds a content proposal for {@link Ref}s (branches, tags...) to a text
	 * field
	 *
	 * @param textField
	 *            the text field
	 * @param repository
	 *            the repository
	 * @param refListProvider
	 *            provides the {@link Ref}s to show in the proposal
	 * @param upstream
	 *            {@code true} if the candidates provided by the
	 *            {@code refListProvider} are from an upstream repository
	 * @return the content proposal adapter set on the {@code textField}
	 */
	public static final ExplicitContentProposalAdapter addRefContentProposalToText(
			Text textField,
			Repository repository,
			IContentProposalCandidateProvider<Ref> refListProvider,
			boolean upstream) {
		return UIUtils.<Ref> addContentProposalToText(textField,
				refListProvider, (pattern, ref) -> {
					String shortenedName = Repository
							.shortenRefName(ref.getName());
					if (pattern != null
							&& !pattern.matcher(ref.getName()).matches()
							&& !pattern.matcher(shortenedName).matches()) {
						return null;
					}
					return new RefContentProposal(repository, ref, upstream);
				}, null,
				UIText.UIUtils_StartTypingForRemoteRefMessage,
				UIText.UIUtils_PressShortcutForRemoteRefMessage);
	}

	/**
	 * Adds a content proposal for arbitrary elements to a text field.
	 *
	 * @param <T>
	 *            type of the proposal candidate objects
	 *
	 * @param textField
	 *            the text field
	 * @param candidateProvider
	 *            {@link IContentProposalCandidateProvider} providing the
	 *            candidates eligible for creating {@link IContentProposal}s
	 * @param factory
	 *            {@link IContentProposalFactory} to use to create proposals
	 *            from candidates
	 * @param patternProvider
	 *            to convert the current text of the field into a pattern
	 *            suitable for filtering the candidates. If {@code null}, a
	 *            default pattern is constructed using
	 *            {@link #createProposalPattern(String)}.
	 * @param startTypingMessage
	 *            hover message if no content assist key binding is active
	 * @param shortcutMessage
	 *            hover message if a content assist key binding is active,
	 *            should have a "{0}" placeholder that will be filled by the
	 *            appropriate keystroke
	 * @return the content proposal adapter set on the {@code textField}
	 */
	public static final <T> ExplicitContentProposalAdapter addContentProposalToText(
			Text textField,
			IContentProposalCandidateProvider<T> candidateProvider,
			IContentProposalFactory<T> factory,
			Function<String, Pattern> patternProvider,
			String startTypingMessage,
			String shortcutMessage) {
		KeyStroke stroke = UIUtils
				.getKeystrokeOfBestActiveBindingFor(IWorkbenchCommandConstants.EDIT_CONTENT_ASSIST);
		if (stroke == null) {
			if (startTypingMessage == null) {
				return null;
			}
			addBulbDecorator(textField, startTypingMessage);
		} else {
			addBulbDecorator(textField,
					NLS.bind(shortcutMessage, stroke.format()));
		}
		IContentProposalProvider cp = new IContentProposalProvider() {
			@Override
			public IContentProposal[] getProposals(String contents, int position) {
				List<IContentProposal> resultList = new ArrayList<>();

				Collection<? extends T> candidates = candidateProvider
						.getCandidates();
				if (candidates == null) {
					return null;
				}
				Pattern pattern = patternProvider != null
						? patternProvider.apply(contents)
						: createProposalPattern(contents);
				for (final T candidate : candidates) {
					IContentProposal proposal = factory.getProposal(pattern,
							candidate);
					if (proposal != null) {
						resultList.add(proposal);
					}
				}
				return resultList.toArray(new IContentProposal[resultList
						.size()]);
			}
		};

		ExplicitContentProposalAdapter adapter = new ExplicitContentProposalAdapter(
				textField, new TextContentAdapter(), cp, stroke,
				UIUtils.VALUE_HELP_ACTIVATIONCHARS);
		// set the acceptance style to always replace the complete content
		adapter.setProposalAcceptanceStyle(
				ContentProposalAdapter.PROPOSAL_REPLACE);
		return adapter;
	}

	/**
	 * Set enabled state of the control and all its children
	 * @param control
	 * @param enable
	 */
	public static void setEnabledRecursively(final Control control,
			final boolean enable) {
		control.setEnabled(enable);
		if (control instanceof Composite)
			for (final Control child : ((Composite) control).getChildren())
				setEnabledRecursively(child, enable);
	}

	/**
	 * Dispose of the resource when the widget is disposed
	 *
	 * @param widget
	 * @param resource
	 */
	public static void hookDisposal(Widget widget, final Resource resource) {
		if (widget == null || resource == null)
			return;

		widget.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				resource.dispose();
			}
		});
	}

	/**
	 * Dispose of the resource manager when the widget is disposed
	 *
	 * @param widget
	 * @param resources
	 */
	public static void hookDisposal(Widget widget,
			final ResourceManager resources) {
		if (widget == null || resources == null)
			return;

		widget.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				resources.dispose();
			}
		});
	}

	/** Key is file extension, value is the reference to the image descriptor */
	private static Map<String, SoftReference<ImageDescriptor>> extensionToDescriptor = new HashMap<>();

	/**
	 * Get editor image for path
	 *
	 * @param path
	 * @return image descriptor
	 */
	public static ImageDescriptor getEditorImage(final String path) {
		if (path == null || path.length() <= 0) {
			return DEFAULT_FILE_IMG;
		}
		final String fileName = new Path(path).lastSegment();
		if (fileName == null) {
			return DEFAULT_FILE_IMG;
		}
		IEditorRegistry registry = PlatformUI.getWorkbench()
				.getEditorRegistry();
		IEditorDescriptor defaultEditor = registry.getDefaultEditor(fileName);
		if (defaultEditor != null) {
			return defaultEditor.getImageDescriptor();
		}
		// now we know there is no Eclipse editor for the file, and Eclipse will
		// check Program.findProgram() and this will be slow, see bug 464891
		int extensionIndex = fileName.lastIndexOf('.');
		if (extensionIndex < 0) {
			// Program.findProgram() uses extensions only
			return DEFAULT_FILE_IMG;
		}
		String key = fileName.substring(extensionIndex);
		SoftReference<ImageDescriptor> cached = extensionToDescriptor.get(key);
		if (cached != null) {
			ImageDescriptor descriptor = cached.get();
			if (descriptor != null) {
				return descriptor;
			}
		}
		// In worst case this calls Program.findProgram() and blocks UI
		ImageDescriptor descriptor = registry.getImageDescriptor(fileName);
		extensionToDescriptor.put(key, new SoftReference<>(descriptor));
		return descriptor;
	}

	/**
	 * Get size of image descriptor as point.
	 *
	 * @param descriptor
	 * @return size
	 */
	public static Point getSize(ImageDescriptor descriptor) {
		ImageData data = descriptor.getImageData();
		if (data == null)
			return new Point(0, 0);
		return new Point(data.width, data.height);
	}

	/**
	 * Add expand all and collapse all toolbar items to the given toolbar bound
	 * to the given tree viewer
	 *
	 * @param toolbar
	 * @param viewer
	 * @return given toolbar
	 */
	public static ToolBar addExpansionItems(final ToolBar toolbar,
			final AbstractTreeViewer viewer) {
		ToolItem collapseItem = new ToolItem(toolbar, SWT.PUSH);
		Image collapseImage = UIIcons.COLLAPSEALL.createImage();
		UIUtils.hookDisposal(collapseItem, collapseImage);
		collapseItem.setImage(collapseImage);
		collapseItem.setToolTipText(UIText.UIUtils_CollapseAll);
		collapseItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.collapseAll();
			}

		});

		ToolItem expandItem = new ToolItem(toolbar, SWT.PUSH);
		Image expandImage = UIIcons.EXPAND_ALL.createImage();
		UIUtils.hookDisposal(expandItem, expandImage);
		expandItem.setImage(expandImage);
		expandItem.setToolTipText(UIText.UIUtils_ExpandAll);
		expandItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.expandAll();
			}

		});
		return toolbar;
	}

	/**
	 * Get dialog bound settings for given class using standard section name
	 *
	 * @param clazz
	 * @return dialog setting
	 */
	public static IDialogSettings getDialogBoundSettings(final Class<?> clazz) {
		return getDialogSettings(clazz.getName() + ".dialogBounds"); //$NON-NLS-1$
	}

	/**
	 * Get dialog settings for given section name
	 *
	 * @param sectionName
	 * @return dialog settings
	 */
	public static IDialogSettings getDialogSettings(final String sectionName) {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(sectionName);
		if (section == null)
			section = settings.addNewSection(sectionName);
		return section;
	}

	/**
	 * Is viewer in a usable state?
	 *
	 * @param viewer
	 * @return true if usable, false if null or underlying control is null or
	 *         disposed
	 */
	public static boolean isUsable(final Viewer viewer) {
		return viewer != null && isUsable(viewer.getControl());
	}

	/**
	 * Is control usable?
	 *
	 * @param control
	 * @return true if usable, false if null or disposed
	 */
	public static boolean isUsable(final Control control) {
		return control != null && !control.isDisposed();
	}

	/**
	 * Run command with specified id
	 *
	 * @param service
	 * @param id
	 */
	public static void executeCommand(IHandlerService service, String id) {
		executeCommand(service, id, null);
	}

	/**
	 * Run command with specified id
	 *
	 * @param service
	 * @param id
	 * @param event
	 */
	public static void executeCommand(IHandlerService service, String id,
			Event event) {
		try {
			service.executeCommand(id, event);
		} catch (ExecutionException e) {
			Activator.handleError(e.getMessage(), e, false);
		} catch (NotDefinedException e) {
			Activator.handleError(e.getMessage(), e, false);
		} catch (NotEnabledException e) {
			Activator.handleError(e.getMessage(), e, false);
		} catch (NotHandledException e) {
			Activator.handleError(e.getMessage(), e, false);
		}
	}

	/**
	 * Determine if the key event represents a "submit" action
	 * (&lt;modifier&gt;+Enter).
	 *
	 * @param event
	 * @return true, if it means submit, false otherwise
	 */
	public static boolean isSubmitKeyEvent(KeyEvent event) {
		return (event.stateMask & SWT.MODIFIER_MASK) != 0
				&& event.keyCode == SUBMIT_KEY_STROKE.getNaturalKey();
	}

	/**
	 * Prompt for saving all dirty editors for resources in the working
	 * directory of the specified repository.
	 *
	 * @param repository
	 * @return true, if the user opted to continue, false otherwise
	 * @see IWorkbench#saveAllEditors(boolean)
	 */
	public static boolean saveAllEditors(Repository repository) {
		return saveAllEditors(repository, null);
	}

	/**
	 * Prompt for saving all dirty editors for resources in the working
	 * directory of the specified repository.
	 *
	 * If at least one file was saved, a dialog is displayed, asking the user if
	 * she wants to cancel the operation. Cancelling allows the user to do
	 * something with the newly saved files, before possibly restarting the
	 * operation.
	 *
	 * @param repository
	 * @param cancelConfirmationQuestion
	 *            A string asking the user if she wants to cancel the operation.
	 *            May be null to not open a dialog, but rather always continue.
	 * @return true, if the user opted to continue, false otherwise
	 * @see IWorkbench#saveAllEditors(boolean)
	 */
	public static boolean saveAllEditors(Repository repository,
			String cancelConfirmationQuestion) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		RepositorySaveableFilter filter = new RepositorySaveableFilter(
				repository);
		boolean success = workbench.saveAll(window, window, filter, true);
		if (success && cancelConfirmationQuestion != null && filter.isAnythingSaved()){
			// allow the user to cancel the operation to first do something with
			// the newly saved files
			String[] buttons = new String[] { IDialogConstants.YES_LABEL,
					IDialogConstants.NO_LABEL };
			MessageDialog dialog = new MessageDialog(window.getShell(),
					UIText.CancelAfterSaveDialog_Title, null,
					cancelConfirmationQuestion,
					MessageDialog.QUESTION, buttons, 0) {
				@Override
				protected int getShellStyle() {
					return (SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL
							| SWT.SHEET | getDefaultOrientation());
				}
			};
			int choice = dialog.open();
			if (choice != 1) // user clicked "yes" or closed dialog -> cancel
				return false;
		}
		return success;
	}

	/**
	 * @param workbenchWindow the workbench window to use for creating the show in menu.
	 * @return the show in menu
	 */
	public static MenuManager createShowInMenu(IWorkbenchWindow workbenchWindow) {
		MenuManager showInSubMenu = new MenuManager(getShowInMenuLabel());
		showInSubMenu.add(ContributionItemFactory.VIEWS_SHOW_IN.create(workbenchWindow));
		return showInSubMenu;
	}

	private static String getShowInMenuLabel() {
		IBindingService bindingService = AdapterUtils.adapt(PlatformUI
		.getWorkbench(), IBindingService.class);
		if (bindingService != null) {
			String keyBinding = bindingService
					.getBestActiveBindingFormattedFor(IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU);
			if (keyBinding != null)
				return UIText.UIUtils_ShowInMenuLabel + '\t' + keyBinding;
		}

		return UIText.UIUtils_ShowInMenuLabel;
	}

	/**
	 * Look up best active binding's keystroke for the given command
	 *
	 * @param commandId
	 *            The identifier of the command for which the best active
	 *            binding's keystroke should be retrieved; must not be null.
	 * @return {@code KeyStroke} for the best active binding for the specified
	 *         commandId or {@code null} if no binding is defined or if the
	 *         binding service returns a {@code TriggerSequence} containing more
	 *         than one {@code Trigger}.
	 */
	@Nullable
	public static KeyStroke getKeystrokeOfBestActiveBindingFor(String commandId) {
		IBindingService bindingService = AdapterUtils
				.adapt(PlatformUI.getWorkbench(), IBindingService.class);
		if (bindingService == null) {
			return null;
		}
		TriggerSequence ts = bindingService.getBestActiveBindingFor(commandId);
		if (ts == null)
			return null;

		Trigger[] triggers = ts.getTriggers();
		if (triggers.length == 1 && triggers[0] instanceof KeyStroke)
			return (KeyStroke) triggers[0];
		else
			return null;
	}

	/**
	 * Copy from {@link org.eclipse.jface.dialogs.DialogPage} with changes to
	 * accommodate the lack of a Dialog context.
	 *
	 * @param button
	 *            the button to set the <code>GridData</code>
	 */
	public static void setButtonLayoutData(Button button) {
		GC gc = new GC(button);
		gc.setFont(JFaceResources.getDialogFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();

		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		int widthHint = Dialog.convertHorizontalDLUsToPixels(fontMetrics,
				IDialogConstants.BUTTON_WIDTH);
		Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(widthHint, minSize.x);
		button.setLayoutData(data);
	}

	/**
	 * Locates the current part and selection and fires
	 * {@link ISelectionListener#selectionChanged(IWorkbenchPart, ISelection)}
	 * on the passed listener.
	 *
	 * @param serviceLocator
	 * @param selectionListener
	 */
	public static void notifySelectionChangedWithCurrentSelection(
			ISelectionListener selectionListener, IServiceLocator serviceLocator) {
		IHandlerService handlerService = CommonUtils.getService(serviceLocator, IHandlerService.class);
		IEvaluationContext state = handlerService.getCurrentState();
		// This seems to be the most reliable way to get the active part, it
		// also returns a part when it is called while creating a view that is
		// being shown.Getting the active part through the active workbench
		// window returned null in that case.
		Object partObject = state.getVariable(ISources.ACTIVE_PART_NAME);
		Object selectionObject = state
				.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
		if (partObject instanceof IWorkbenchPart
				&& selectionObject instanceof ISelection) {
			IWorkbenchPart part = (IWorkbenchPart) partObject;
			ISelection selection = (ISelection) selectionObject;
			if (!selection.isEmpty())
				selectionListener.selectionChanged(part, selection);
		}
	}
}
