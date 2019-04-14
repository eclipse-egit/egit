/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

class EclipseProxySelector extends ProxySelector {
	private final IProxyService service;

	EclipseProxySelector(final IProxyService s) {
		service = s;
	}

	@Override
	public List<Proxy> select(final URI uri) {
		final ArrayList<Proxy> r = new ArrayList<>();
		final String host = uri.getHost();

		if (host != null) {
			String type = IProxyData.SOCKS_PROXY_TYPE;
			if ("http".equals(uri.getScheme())) //$NON-NLS-1$
				type = IProxyData.HTTP_PROXY_TYPE;
			else if ("ftp".equals(uri.getScheme())) //$NON-NLS-1$
				type = IProxyData.HTTP_PROXY_TYPE;
			else if ("https".equals(uri.getScheme())) //$NON-NLS-1$
				type = IProxyData.HTTPS_PROXY_TYPE;
			try {
				URI queryUri = new URI(type, "//" + host, null); //$NON-NLS-1$
				final IProxyData[] dataArray = service.select(queryUri);
				for (IProxyData data : dataArray) {
					if (IProxyData.HTTP_PROXY_TYPE.equals(data.getType()))
						addProxy(r, Proxy.Type.HTTP, data);
					else if (IProxyData.HTTPS_PROXY_TYPE.equals(data.getType()))
						addProxy(r, Proxy.Type.HTTP, data);
					else if (IProxyData.SOCKS_PROXY_TYPE.equals(data.getType()))
						addProxy(r, Proxy.Type.SOCKS, data);
				}
			} catch (URISyntaxException e) {
				// just add nothing to r
			}
		}
		if (r.isEmpty())
			r.add(Proxy.NO_PROXY);
		return r;
	}

	private void addProxy(final ArrayList<Proxy> r, final Proxy.Type type,
			final IProxyData d) {
		try {
			r.add(new Proxy(type, new InetSocketAddress(InetAddress.getByName(d
					.getHost()), d.getPort())));
		} catch (UnknownHostException uhe) {
			// Oh well.
		}
	}

	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		// Don't tell Eclipse.
	}
}
