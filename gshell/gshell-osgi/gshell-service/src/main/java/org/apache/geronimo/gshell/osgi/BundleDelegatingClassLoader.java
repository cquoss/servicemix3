/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.apache.geronimo.gshell.osgi;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * ClassLoader backed by an OSGi bundle. Will use the Bundle class loading.
 * Contains facilities for tracing classloading behavior so that issues can be
 * easily resolved. Debugging can be enabled by setting the system property
 * <code>org.springframework.osgi.DebugClassLoading</code> to true.
 *
 *
 * @author Adrian Colyer
 * @author Andy Piper
 * @author Costin Leau
 * @since 2.0
 */
public class BundleDelegatingClassLoader extends ClassLoader {
	private ClassLoader bridge;

	private Bundle backingBundle;

	public static BundleDelegatingClassLoader createBundleClassLoaderFor(Bundle aBundle) {
		return createBundleClassLoaderFor(aBundle, BundleDelegatingClassLoader.class.getClassLoader());
	}

	public static BundleDelegatingClassLoader createBundleClassLoaderFor(final Bundle bundle, final ClassLoader bridge) {
		return (BundleDelegatingClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return new BundleDelegatingClassLoader(bundle, bridge);
			}
		});
	}

	private BundleDelegatingClassLoader(Bundle bundle, ClassLoader bridgeLoader) {
		super(null);
		this.backingBundle = bundle;
		this.bridge = bridgeLoader;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (!(o instanceof BundleDelegatingClassLoader))
			return false;

		final BundleDelegatingClassLoader bundleDelegatingClassLoader = (BundleDelegatingClassLoader) o;

		if (backingBundle.equals(bundleDelegatingClassLoader.backingBundle))
			return (bridge == null || bridge.equals(bundleDelegatingClassLoader.bridge));

		return false;
	}

	public int hashCode() {
		int hashCode = backingBundle.hashCode();
		if (bridge != null)
			hashCode |= bridge.hashCode();

		return hashCode;
	}

	protected Class findClass(String name) throws ClassNotFoundException {
		try {
			return this.backingBundle.loadClass(name);
		}
		catch (ClassNotFoundException cnfe) {
			throw new ClassNotFoundException(name + " not found from bundle [" + backingBundle.getSymbolicName() + "]",
					cnfe);
		}
		catch (NoClassDefFoundError ncdfe) {
			NoClassDefFoundError e = new NoClassDefFoundError(name + " not found from bundle [" + backingBundle + "]");
			e.initCause(ncdfe);
			throw e;
		}
	}

	protected URL findResource(String name) {
		URL url = this.backingBundle.getResource(name);
		return url;
	}

	protected Enumeration findResources(String name) throws IOException {
		Enumeration enm = this.backingBundle.getResources(name);
		return enm;
	}

	public URL getResource(String name) {
		URL resource = findResource(name);
		if (resource == null) {
			resource = bridge.getResource(name);
		}
		return resource;
	}

	protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class clazz;
		try {
			clazz = findClass(name);
		}
		catch (ClassNotFoundException e) {
			clazz = bridge.loadClass(name);
		}
		if (resolve) {
			resolveClass(clazz);
		}
		return clazz;
	}

	// For testing
	public Bundle getBundle() {
		return backingBundle;
	}

	public String toString() {
		Dictionary dict = backingBundle.getHeaders();
		String bname = dict.get(Constants.BUNDLE_NAME) + "(" + dict.get(Constants.BUNDLE_SYMBOLICNAME) + ")";
		return "BundleDelegatingClassLoader for [" + bname + "]";
	}
}
