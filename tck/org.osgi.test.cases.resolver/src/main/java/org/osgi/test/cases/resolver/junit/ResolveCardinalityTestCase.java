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
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

/**
 * Test that the cardinality directive of a requirement is ignored in the
 * osgi.wiring.package and osgi.wiring.bundle namespaces and honored in a
 * generic namespace.
 */
public class ResolveCardinalityTestCase extends AbstractResolverTestCase {

	@Test
	public void testPackageRequirementWithCardinalityMultiple() {
		TestResource first = new TestResource();
		first.addCapability(PackageNamespace.PACKAGE_NAMESPACE,
				Collections.<String, Object> singletonMap(
						PackageNamespace.PACKAGE_NAMESPACE, "pkg"));
		TestResource second = new TestResource();
		second.addCapability(PackageNamespace.PACKAGE_NAMESPACE,
				Collections.<String, Object> singletonMap(
						PackageNamespace.PACKAGE_NAMESPACE, "pkg"));
		TestResource consumer = new TestResource();
		Requirement requirement = consumer.addRequirement(
				PackageNamespace.PACKAGE_NAMESPACE,
				"(" + PackageNamespace.PACKAGE_NAMESPACE + "=pkg)");
		requirement.getDirectives().put(
				Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE,
				Namespace.CARDINALITY_MULTIPLE);

		assertThat(wiresOf(consumer, first, second)).hasSize(1);
	}

	@Test
	public void testBundleRequirementWithCardinalityMultiple() {
		TestResource first = bundle("lib", new Version(1, 0, 0));
		TestResource second = bundle("lib", new Version(2, 0, 0));
		TestResource consumer = new TestResource();
		Requirement requirement = consumer.addRequirement(
				BundleNamespace.BUNDLE_NAMESPACE,
				"(" + BundleNamespace.BUNDLE_NAMESPACE + "=lib)");
		requirement.getDirectives().put(
				Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE,
				Namespace.CARDINALITY_MULTIPLE);

		assertThat(wiresOf(consumer, first, second)).hasSize(1);
	}

	/*
	 * The counterpart: in a namespace that does not ignore the directive every
	 * matching capability is wired.
	 */
	@Test
	public void testGenericRequirementWithCardinalityMultiple() {
		TestResource first = new TestResource();
		first.addCapability(TEST_NAMESPACE, "name=lib");
		TestResource second = new TestResource();
		second.addCapability(TEST_NAMESPACE, "name=lib");
		TestResource consumer = new TestResource();
		Requirement requirement = consumer.addRequirement(TEST_NAMESPACE,
				"(name=lib)");
		requirement.getDirectives().put(
				Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE,
				Namespace.CARDINALITY_MULTIPLE);

		assertThat(wiresOf(consumer, first, second)).hasSize(2);
	}

	private List<Wire> wiresOf(TestResource consumer, TestResource... others) {
		TestResource[] all = new TestResource[others.length + 1];
		all[0] = consumer;
		System.arraycopy(others, 0, all, 1, others.length);
		TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(all), null, null);
		Map<Resource,List<Wire>> result = shouldResolve(context);
		return result.get(consumer);
	}

	private static TestResource bundle(String name, Version version) {
		TestResource resource = new TestResource();
		Map<String,Object> identity = new HashMap<>();
		identity.put(IdentityNamespace.IDENTITY_NAMESPACE, name);
		identity.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
				IdentityNamespace.TYPE_BUNDLE);
		identity.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
		resource.addCapability(IdentityNamespace.IDENTITY_NAMESPACE, identity);
		Map<String,Object> bundle = new HashMap<>();
		bundle.put(BundleNamespace.BUNDLE_NAMESPACE, name);
		bundle.put(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
				version);
		resource.addCapability(BundleNamespace.BUNDLE_NAMESPACE, bundle);
		return resource;
	}
}
