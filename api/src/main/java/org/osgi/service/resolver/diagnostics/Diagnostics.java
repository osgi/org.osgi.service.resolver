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
package org.osgi.service.resolver.diagnostics;

import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * The explanation a resolver gives for the decisions it made in one resolve
 * operation, whether or not the resolution succeeded.
 * <p>
 * The explanations are human readable text for diagnostic purposes. Their
 * format is not specified and must not be parsed. An empty result means the
 * resolver has nothing to say about the given element, for example because it
 * did not take part in the resolve operation.
 * 
 * @ThreadSafe
 * @author $Id$
 */
@ProviderType
public interface Diagnostics {

	/**
	 * Explains the outcome for a resource, for example why it was resolved with
	 * certain wires or why it could not be resolved.
	 * 
	 * @param resource A resource of the resolve operation.
	 * @return The explanation or an empty Optional.
	 */
	Optional<String> getMessage(Resource resource);

	/**
	 * Explains the outcome for a requirement of a resource, for example which
	 * of the possible providers was chosen and why the others were not.
	 * 
	 * @param requirement A requirement, its resource identifies the owner.
	 * @return The explanation or an empty Optional.
	 */
	Optional<String> getMessage(Requirement requirement);

	/**
	 * Explains the outcome for a capability of a resource, for example why an
	 * export is not used because the resource is wired to another provider of
	 * the same package.
	 * 
	 * @param capability A capability, its resource identifies the owner.
	 * @return The explanation or an empty Optional.
	 */
	Optional<String> getMessage(Capability capability);
}
