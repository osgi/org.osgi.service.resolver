/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/
package org.osgi.test.cases.resolver.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

/**
 * Test that a package exported by a fragment can be imported with the
 * {@code bundle-symbolic-name} and {@code bundle-version} of the host when the
 * resolve context offers it for that import.
 */
public class ResolveHostedAttributesTestCase extends AbstractResolverTestCase {

	@Test
	public void testFragmentExportMatchesHostAttributes() {
		TestResource host = new TestResource();
		host.addIdentity("host", new Version(1, 0, 0), null);

		TestResource fragment = new TestResource();
		fragment.addIdentity("host.frag", new Version(2, 0, 0),
				IdentityNamespace.TYPE_FRAGMENT);
		fragment.addRequirement(HostNamespace.HOST_NAMESPACE,
				"(" + HostNamespace.HOST_NAMESPACE + "=host)");
		Map<String,Object> export = new HashMap<>();
		export.put(PackageNamespace.PACKAGE_NAMESPACE, "p");
		export.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE,
				new Version(1, 0, 0));
		export.put(PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE,
				"host.frag");
		export.put(PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
				new Version(2, 0, 0));
		fragment.addCapability(PackageNamespace.PACKAGE_NAMESPACE, export);

		TestResource importer = new TestResource();
		importer.addIdentity("importer", new Version(1, 0, 0), null);
		importer.addRequirement(PackageNamespace.PACKAGE_NAMESPACE,
				"(&(osgi.wiring.package=p)(bundle-symbolic-name=host)(bundle-version=1.0.0))");

		HostAttributesResolveContext context = new HostAttributesResolveContext(
				Arrays.<Resource> asList(host, importer),
				Collections.<Resource> singleton(fragment), fragment, host);
		Map<Resource,List<Wire>> result = shouldResolve(context);

		assertThat(result).as("fragment is attached").containsKey(fragment);
		assertThat(result.get(fragment)).as("fragment is wired to the host")
				.extracting(Wire::getProvider).contains(host);
		assertThat(result.get(importer)).as("package is wired to the host")
				.filteredOn(wire -> PackageNamespace.PACKAGE_NAMESPACE
						.equals(wire.getRequirement().getNamespace()))
				.extracting(Wire::getProvider).containsExactly(host);
	}
}
