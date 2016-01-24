/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.Test.None;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.eclipse.swtbot.swt.finder.junit.ScreenshotCaptureListener;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * Like {@link org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner},
 * this class captures screenshots on test failures, but it does so before any
 * potential @After clean-ups run. Compare Eclipse bug 372209.
 */
public class CapturingSWTBotJunit4Runner extends BlockJUnit4ClassRunner {

	public CapturingSWTBotJunit4Runner(Class<?> testClass)
			throws InitializationError {
		super(testClass);
	}

	@Override
	protected Statement methodBlock(FrameworkMethod method) {
		return super.methodBlock(
				new CapturingFrameworkMethod(method.getMethod()));
	}

	private static class CapturingFrameworkMethod extends FrameworkMethod {

		// We test for this exception using the name because it only became
		// public recently in JUnit 4.12
		private static final String ASSUMPTION_VIOLATED_EXCEPTION_NAME = "AssumptionViolatedException";

		private final Class<? extends Throwable> expectedException;

		public CapturingFrameworkMethod(Method method) {
			super(method);
			// Determine expected exception, if any
			Test annotation = method.getAnnotation(Test.class);
			expectedException = getExpectedException(annotation);
		}

		@Override
		public Object invokeExplosively(Object target, Object... params)
				throws Throwable {
			Object result = null;
			try {
				result = super.invokeExplosively(target, params);
			} catch (Throwable e) {
				// A timeout will give us an InterruptedException here and make
				// us capture a screenshot, too.
				if (!ASSUMPTION_VIOLATED_EXCEPTION_NAME
						.equals(e.getClass().getSimpleName())) {
					if (expectedException == null || !expectedException
							.isAssignableFrom(e.getClass())) {
						createScreenshot(e); // Unexpected exception
					}
				}
				throw e;
			}
			if (expectedException != null) {
				createScreenshot(null); // No exception, but we expected one
				// No need to raise an exception, an outer statement will do so.
			}
			return result;
		}

		private Class<? extends Throwable> getExpectedException(
				Test annotation) {
			if (annotation == null || annotation.expected() == None.class) {
				return null;
			} else {
				return annotation.expected();
			}
		}

		private void createScreenshot(Throwable t) {
			Description testDescription = Description.createTestDescription(
					getDeclaringClass(), getMethod().getName());
			Failure failure = new Failure(testDescription, t);
			ScreenshotCaptureListener screenshotCreator = new ScreenshotCaptureListener();
			try {
				screenshotCreator.testFailure(failure);
			} catch (Exception e) {
				// Ignore, cannot happen anyway.
			}
		}
	}
}
