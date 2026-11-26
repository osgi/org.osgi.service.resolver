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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * A resolve context that, like an OSGi framework, associates the
 * {@code bundle-symbolic-name} and {@code bundle-version} of the host with the
 * packages a fragment exports when it matches an import. The fragment declares
 * its own name and version.
 */
class HostAttributesResolveContext
		extends AbstractResolverTestCase.TestResolveContext {

	private final Resource	fragment;
	private final Resource	host;

	HostAttributesResolveContext(Collection<Resource> mandatory,
			Collection<Resource> optional, Resource fragment, Resource host) {
		super(mandatory, optional, null);
		this.fragment = fragment;
		this.host = host;
	}

	@Override
	protected boolean matches(Requirement requirement, Capability capability) {
		if (capability.getResource() != fragment || !PackageNamespace.PACKAGE_NAMESPACE
				.equals(capability.getNamespace())) {
			return super.matches(requirement, capability);
		}
		Map<String,Object> view = new HashMap<>(capability.getAttributes());
		Capability hostBundle = host
				.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE).get(0);
		view.put(PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE,
				hostBundle.getAttributes().get(BundleNamespace.BUNDLE_NAMESPACE));
		view.put(PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
				hostBundle.getAttributes().get(
						BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
		String filter = requirement.getDirectives()
				.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		try {
			return filter == null
					|| FrameworkUtil.createFilter(filter).matches(view);
		} catch (InvalidSyntaxException e) {
			return false;
		}
	}
}
