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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.resolver.Resolver;

/**
 * Tests that the bundle providing the Resolver service also registers the
 * implementation with the Java Service Provider mechanism.
 */
public class ResolverServiceLoaderTestCase extends AbstractResolverTestCase {

	private static final String SERVICES_RESOURCE = "META-INF/services/"
			+ Resolver.class.getName();

	@Test
	public void testServiceProviderConfiguration() throws Exception {
		ServiceReference<Resolver> ref = context
				.getServiceReference(Resolver.class);
		assertThat(ref).as("Resolver service").isNotNull();
		Bundle provider = ref.getBundle();

		URL url = provider.getResource(SERVICES_RESOURCE);
		assertThat(url).as(SERVICES_RESOURCE + " in " + provider)
				.isNotNull();

		List<String> classNames = new ArrayList<>();
		try (InputStream in = url.openStream();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(in, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				int comment = line.indexOf('#');
				if (comment >= 0) {
					line = line.substring(0, comment);
				}
				line = line.replace(" ", "").replace("\t", "");
				if (!line.isEmpty()) {
					classNames.add(line);
				}
			}
		}
		assertThat(classNames).as("provider class names").isNotEmpty();

		for (String className : classNames) {
			Class<?> clazz = provider.loadClass(className);
			assertThat(Resolver.class.isAssignableFrom(clazz))
					.as(className + " implements Resolver").isTrue();
			assertThat(Modifier.isPublic(clazz.getModifiers()))
					.as(className + " is public").isTrue();
			assertThat(clazz.getConstructor().getParameterCount())
					.as(className + " has a public no-argument constructor")
					.isZero();
		}
	}
}
