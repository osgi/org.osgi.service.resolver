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
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

/**
 * An optional resource must never be the reason that a resolve operation
 * fails, also if its class space is inconsistent with the one of the other
 * resources of the solution.
 * <p>
 * Resource {@code middle} imports package {@code p} and exports package
 * {@code q} that uses {@code p}. Resource {@code a} imports {@code q} and
 * {@code p} in version range [1,2), resource {@code b} imports {@code q} and
 * {@code p} in version range [2,3). The resource {@code middle} can only
 * import one version of {@code p}, so {@code a} and {@code b} can not be
 * resolved together.
 */
public class ResolveOptionalResourcesTestCase extends AbstractResolverTestCase {

	private final TestResource							p1		= createResource("p1");
	private final TestResource							p2		= createResource("p2");
	private final TestResource							middle	= createResource("middle");
	private final TestResource							a		= createResource("a");
	private final TestResource							b		= createResource("b");
	private final Map<Requirement,List<Capability>>	candidates	= new HashMap<>();

	public ResolveOptionalResourcesTestCase() {
		Capability p1_p = exportPackage(p1, "p", "1.0.0", null);
		Capability p2_p = exportPackage(p2, "p", "2.0.0", null);

		Requirement middle_p = importPackage(middle, "(" + PackageNamespace.PACKAGE_NAMESPACE + "=p)");
		Capability middle_q = exportPackage(middle, "q", "1.0.0", "p");

		Requirement a_q = importPackage(a, "(" + PackageNamespace.PACKAGE_NAMESPACE + "=q)");
		Requirement a_p = importPackage(a, "(&(" + PackageNamespace.PACKAGE_NAMESPACE + "=p)(version>=1.0.0)(!(version>=2.0.0)))");

		Requirement b_q = importPackage(b, "(" + PackageNamespace.PACKAGE_NAMESPACE + "=q)");
		Requirement b_p = importPackage(b, "(&(" + PackageNamespace.PACKAGE_NAMESPACE + "=p)(version>=2.0.0)(!(version>=3.0.0)))");

		// the provider of p that is preferred for middle is not the one that a needs
		candidates.put(middle_p, Arrays.asList(p2_p, p1_p));
		candidates.put(a_p, Collections.singletonList(p1_p));
		candidates.put(b_p, Collections.singletonList(p2_p));
		candidates.put(a_q, Collections.singletonList(middle_q));
		candidates.put(b_q, Collections.singletonList(middle_q));
	}

	/**
	 * The optional resource that can not be added to a consistent solution is
	 * left out, the resolve operation succeeds.
	 */
	@Test
	public void testOptionalResourceInConflictIsLeftOut() throws Exception {
		TestResolveContext context = context(Arrays.<Resource> asList(a), Arrays.<Resource> asList(b));

		Map<Resource,List<Wire>> result = shouldResolve(context);

		assertThat(result).containsKeys(a, middle, p1);
		assertThat(result).doesNotContainKeys(b, p2);
	}

	/**
	 * The same resources with a mandatory resource instead of the optional one
	 * have no solution.
	 */
	@Test
	public void testMandatoryResourceInConflictFails() throws Exception {
		shouldNotResolve(context(Arrays.<Resource> asList(a, b), null));
	}

	private TestResolveContext context(List<Resource> mandatory, List<Resource> optional) {
		return new TestResolveContext(mandatory, optional, Arrays.<Resource> asList(middle, p1, p2)) {
			@Override
			public List<Capability> findProviders(Requirement requirement) {
				List<Capability> result = candidates.get(requirement);
				return result == null ? new ArrayList<Capability>() : new ArrayList<>(result);
			}
		};
	}

	private static Capability exportPackage(TestResource resource, String name, String version, String uses) {
		Map<String,Object> attributes = new HashMap<>();
		attributes.put(PackageNamespace.PACKAGE_NAMESPACE, name);
		attributes.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, Version.parseVersion(version));
		Capability capability = resource.addCapability(PackageNamespace.PACKAGE_NAMESPACE, attributes);
		if (uses != null) {
			capability.getDirectives().put(PackageNamespace.CAPABILITY_USES_DIRECTIVE, uses);
		}
		return capability;
	}

	private static Requirement importPackage(TestResource resource, String filter) {
		return resource.addRequirement(PackageNamespace.PACKAGE_NAMESPACE, filter);
	}

	private static TestResource createResource(String name) {
		TestResource resource = new TestResource();
		resource.addIdentity(name, null, IdentityNamespace.TYPE_BUNDLE);
		return resource;
	}
}
