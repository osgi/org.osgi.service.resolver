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

import java.util.ArrayList;
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
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

/**
 * Test that a fragment is attached to each host individually: a host that can
 * not be resolved with the fragment is resolved without it and does not keep
 * the fragment from the other hosts.
 */
public class ResolveFragmentHostsTestCase extends AbstractResolverTestCase {

	@Test
	public void testFragmentAttachedToHostWithoutConflict() {
		TestResource first = exporter(new Version(1, 0, 0));
		TestResource second = exporter(new Version(2, 0, 0));
		TestResource hostOne = host(new Version(1, 0, 0),
				"(&(osgi.wiring.package=p)(version>=2.0.0)(!(version>=3.0.0)))");
		TestResource hostTwo = host(new Version(2, 0, 0),
				"(&(osgi.wiring.package=p)(version>=1.0.0)(!(version>=2.0.0)))");
		TestResource fragment = new TestResource();
		fragment.addIdentity("host.frag", null, IdentityNamespace.TYPE_FRAGMENT);
		fragment.addRequirement(
				HostNamespace.HOST_NAMESPACE,
				"(" + HostNamespace.HOST_NAMESPACE + "=host)");
		fragment.addRequirement(PackageNamespace.PACKAGE_NAMESPACE,
				"(&(osgi.wiring.package=p)(version>=1.0.0)(!(version>=2.0.0)))");

		TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(first, second, hostOne, hostTwo),
				Collections.<Resource> singleton(fragment), null);
		Map<Resource,List<Wire>> result = shouldResolve(context);

		assertThat(result).as("hosts are resolved").containsKeys(hostOne,
				hostTwo);
		assertThat(result).as("fragment is attached").containsKey(fragment);
		List<Wire> fragmentWires = result.get(fragment);
		assertThat(wiredHosts(fragmentWires))
				.containsExactly(hostTwo);
	}

	private static List<Resource> wiredHosts(List<Wire> wires) {
		List<Resource> hosts = new ArrayList<>();
		for (Wire wire : wires) {
			if (HostNamespace.HOST_NAMESPACE
					.equals(wire.getRequirement().getNamespace())) {
				hosts.add(wire.getProvider());
			}
		}
		return hosts;
	}

	private static TestResource exporter(Version version) {
		TestResource resource = new TestResource();
		Map<String,Object> attributes = new HashMap<>();
		attributes.put(PackageNamespace.PACKAGE_NAMESPACE, "p");
		attributes.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
		resource.addCapability(PackageNamespace.PACKAGE_NAMESPACE, attributes);
		return resource;
	}

	private static TestResource host(Version version, String importFilter) {
		TestResource resource = new TestResource();
		resource.addIdentity("host", version, null);
		resource.addRequirement(PackageNamespace.PACKAGE_NAMESPACE,
				importFilter);
		return resource;
	}
}
