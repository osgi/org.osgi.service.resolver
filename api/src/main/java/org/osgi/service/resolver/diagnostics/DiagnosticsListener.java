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

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Implemented by a {@code ResolveContext} that is interested in the
 * {@link Diagnostics} of a resolve operation.
 * <p>
 * A resolver that supports diagnostics must call
 * {@link #publishDiagnostic(Diagnostics)} once per resolve operation, for
 * successful as well as for failed operations. Resolvers that do not support
 * diagnostics never call it.
 * 
 * @author $Id$
 */
@ConsumerType
public interface DiagnosticsListener {

	/**
	 * Called by the resolver with the diagnostics of a resolve operation.
	 * <p>
	 * The call happens before the resolve operation returns or throws a
	 * {@code ResolutionException}.
	 * 
	 * @param diagnostics The diagnostics of the resolve operation. Must not be
	 *            {@code null}.
	 */
	void publishDiagnostic(Diagnostics diagnostics);
}
