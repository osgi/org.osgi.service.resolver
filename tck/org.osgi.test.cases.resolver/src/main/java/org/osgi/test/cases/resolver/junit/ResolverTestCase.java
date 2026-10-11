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
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.resolver.Resolver;
import org.osgi.test.cases.resolver.junit.AbstractResolverTestCase.TestResource.TestCapability;

/**
 * Testing the resolver with a synthetic namespace.
 * 
 * @author <a href="mailto:rellermeyer@us.ibm.com">Jan S. Rellermeyer</a>
 */
public class ResolverTestCase extends AbstractResolverTestCase {

	@Test
	public void testNull() throws Exception {
		final Resolver resolver = getResolverService();

		try {
			resolver.resolve(null);
		} catch (final Exception e) {
			// expected
			return;
		}
		// nothing has happened? That's wrong.
		fail("");
	}

	@Test
	public void testEmpty() throws Exception {
		try {
			final Resolver resolver = getResolverService();
			final Map<Resource, List<Wire>> result = resolver
					.resolve(new ResolveContext() {

						@Override
						public List<Capability> findProviders(
								final Requirement requirement) {
							fail("");
							return null;
						}

						@Override
						public int insertHostedCapability(
								final List<Capability> capabilities,
								final HostedCapability hostedCapability) {
							fail("");
							return -1;
						}

						@Override
						public boolean isEffective(final Requirement requirement) {
							fail("");
							return false;
						}

						@Override
						public Map<Resource, Wiring> getWirings() {
							fail("");
							return null;
						}
					});
			assertThat(result).isEmpty();
		} catch (final Throwable t) {
			fail("Unexpected exception", t);
		}
	}

	/*
	 * "The returned map is the property of the caller and can be modified by
	 * the caller"
	 */
	@Test
	public void testModifiable() throws Exception {
		final TestResource r1 = new TestResource();
		r1.addRequirement(null, "(cap=*)");
		final TestResource r2 = new TestResource();
		r2.addCapability(null, "cap=true");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2), null, null);
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		assertThat(result.remove(r1)).isNotNull();
		assertThat(result.remove(r2)).isNotNull();
		result.put(r1, null);
		result.clear();
	}

	/*
	 * test that the result of a resolution preserves the equals relationship
	 * with the original values
	 */
	@Test
	public void testResolutionIdentity() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement req = r1.addRequirement(null, "(cap=*)");
		final TestResource r2 = new TestResource();
		final Capability cap = r2.addCapability(null, "cap=true");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2), null, null);
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(req);
		final Set<Resource> keys = result.keySet();
		assertThat(keys.size()).isEqualTo(2);
		keys.remove(r2);
		assertThat(keys.size()).isEqualTo(1);
		final Resource key = keys.toArray(new Resource[1])[0];
		assertThat(key).isEqualTo(r1);
		assertThat(r1).isEqualTo(key);
		final List<Wire> wires = result.get(r1);
		assertThat(wires).isNotNull();
		assertThat(wires.size()).isEqualTo(1);
		final Wire wire = wires.get(0);
		final TestWire testWire = new TestWire(r1, req, r2, cap);
		assertThat(testWire).isEqualTo(wire);
		assertThat(wire).isEqualTo(testWire);
		assertThat(wire.getProvider()).isEqualTo(r2);
		assertThat(r2).isEqualTo(wire.getProvider());
	}

	/*
	 * mandatory resource with no requirements
	 */
	@Test
	public void testMandatoryResources0() throws Exception {
		final TestResource r1 = new TestResource();
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1), null, null);
		shouldResolve(context);
	}

	/*
	 * the resolver must never match any requirements and capabilities and
	 * should therefore blindly trust the resolve context to return only
	 * matching capabilities. Here, we play foul and expect the resolver to
	 * ignore that.
	 */
	@Test
	public void testMatching0() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement req = r1.addRequirement(null, "(cap=*)");
		final TestResource r2 = new TestResource();
		final Capability cap = r2.addCapability(null, "match=false");

		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2), null, null) {

			public List<Capability> findProviders(final Requirement requirement) {
				super.findProviders(requirement);
				return Arrays.asList(cap);
			}
		};

		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(req);
		context.checkWires(result, new TestWire(r1, req, r2, cap));
	}

	/*
	 * the resolver must never match any requirements and capabilities and
	 * should therefore blindly trust the resolve context to return only
	 * matching capabilities. Here, we play foul by returning a different
	 * namespace and expect the resolver to ignore that.
	 */
	@Test
	public void testMatching1() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement req = r1.addRequirement(null, "(cap=*)");
		final TestResource r2 = new TestResource();
		final Capability cap = r2.addCapability("other", "cap=true");

		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2), null, null) {

			public List<Capability> findProviders(final Requirement requirement) {
				super.findProviders(requirement);
				return Arrays.asList(cap);
			}
		};

		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(req);
		context.checkWires(result, new TestWire(r1, req, r2, cap));
	}

	/*
	 * even though the resolver gets the mandatory and optional resources it
	 * must not search these for capabilities not returned by findResources.
	 */
	@Test
	public void testMatching2() throws Exception {
		final TestResource r1 = new TestResource();
		r1.addRequirement(null, "(cap=*)");
		final TestResource r2 = new TestResource();
		r2.addCapability(null, "cap=true");

		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2), null, null) {

			@Override
			public List<Capability> findProviders(final Requirement requirement) {
				super.findProviders(requirement);
				// the returned list must be mutable
				// Collections.emptyList() is therefore not an option.
				return new ArrayList<Capability>();
			}
		};
		shouldNotResolve(context);
	}

	/*
	 * "the same is true for the existing wiring state used.
	 */
	@Test
	public void testMatching3() throws Exception {
		final TestResource r1 = new TestResource();
		r1.addRequirement(null, "(cap=*)");
		final TestResource r2 = new TestResource();
		final Capability cap = r2.addCapability(null, "cap=true");

		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1), null, null) {

			@Override
			public Map<Resource, Wiring> getWirings() {
				final Map<Resource, Wiring> result = new HashMap<Resource, Wiring>();
				result.put(r2, new Wiring() {

					public List<Capability> getResourceCapabilities(
							String namespace) {
						return Arrays.asList(cap);
					}

					public List<Requirement> getResourceRequirements(
							String namespace) {
						return Collections.emptyList();
					}

					public List<Wire> getProvidedResourceWires(String namespace) {
						return Collections.emptyList();
					}

					public List<Wire> getRequiredResourceWires(String namespace) {
						return Collections.emptyList();
					}

					public Resource getResource() {
						return r2;
					}

				});
				return result;
			}

			@Override
			public List<Capability> findProviders(final Requirement requirement) {
				super.findProviders(requirement);
				// the returned list must be mutable
				// Collections.emptyList() is therefore not an option.
				return new ArrayList<Capability>();
			}
		};
		shouldNotResolve(context);
	}

	/*
	 * mandatory resource with unresolved requirement
	 */
	@Test
	public void testMandatoryResources1() throws Exception {
		final TestResource r1 = new TestResource();
		r1.addRequirement(null, "(foo=bar)");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1), null, null);
		shouldNotResolve(context);
	}

	/*
	 * mandatory resource with resolvable requirement
	 */
	@Test
	public void testMandatoryResources2() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement req = r1.addRequirement(null, "(foo=bar)");
		final TestResource r2 = new TestResource();
		final Capability cap = r2.addCapability(null, "foo=bar");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2), null, null);
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(req);
		context.checkWires(result, new TestWire(r1, req, r2, cap));
	}

	/*
	 * mandatory resource with unresolvable requirement
	 */
	@Test
	public void testMandatoryResources3() throws Exception {
		final TestResource r1 = new TestResource();
		r1.addRequirement(null, "(foo=bar)");
		final TestResource r2 = new TestResource();
		r2.addCapability(null, "foo=else");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2), null, null);
		shouldNotResolve(context);
	}

	/*
	 * mandatory resource but capability is in wrong namespace
	 */
	@Test
	public void testMandatoryResources4() throws Exception {
		final TestResource r1 = new TestResource();
		r1.addRequirement(null, "(foo=bar)");
		final TestResource r2 = new TestResource();
		r2.addCapability("wrong", "foo=bar");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2), null, null);
		shouldNotResolve(context);
	}

	/*
	 * mandatory resources, one cannot be fulfilled
	 */
	@Test
	public void testMandatoryResources5() throws Exception {
		final TestResource r1 = new TestResource();
		r1.addRequirement(null, "(foo=bar)");
		final TestResource r2 = new TestResource();
		r2.addCapability(null, "foo=bar");
		final TestResource r3 = new TestResource();
		r3.addRequirement(null, "(not=there)");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2, r3), null, null);
		shouldNotResolve(context);
	}

	/*
	 * mandatory resources, one can be resolved through a third resource
	 * provided by the resolve context
	 */
	@Test
	public void testMandatoryResources6() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement r1_req = r1.addRequirement(null, "(foo=bar)");
		final TestResource r2 = new TestResource();
		final Capability r2_cap = r2.addCapability(null, "foo=bar");
		final TestResource r3 = new TestResource();
		final Requirement r3_req = r3.addRequirement(null, "(is=*)");
		final TestResource r4 = new TestResource();
		final Capability r4_cap = r4.addCapability(null, "is=there");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2, r3), null,
				Arrays.<Resource> asList(r4));
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(r1_req, r3_req);
		context.checkWires(result, new TestWire(r1, r1_req, r2, r2_cap),
				new TestWire(r3, r3_req, r4, r4_cap));
	}

	/*
	 * mandatory resources, an unresolvable resource returned by findProviders
	 * should not prevent resolution as long as there are alternatives
	 */
	@Test
	public void testMandatoryResources7() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement r1_req = r1.addRequirement(null, "(foo=bar)");
		final TestResource r2 = new TestResource();
		r2.addCapability(null, "foo=bar");
		final Requirement r2_req = r2.addRequirement(null, "(unavailable=*)");
		final TestResource r3 = new TestResource();
		final Capability r3_cap = r3.addCapability(null, "foo=bar");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1), null, Arrays.<Resource> asList(
						r2, r3));
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(r1_req, r2_req);
		context.checkWires(result, new TestWire(r1, r1_req, r3, r3_cap));
	}

	/*
	 * duplicate mandatory resource
	 */
	@Test
	public void testMandatoryResources8() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement req = r1.addRequirement(null, "(foo=bar)");
		final TestResource r2 = new TestResource();
		final Capability cap = r2.addCapability(null, "foo=bar");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r1), null,
				Arrays.<Resource> asList(r2));
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(req);
		context.checkWires(result, new TestWire(r1, req, r2, cap));
		final List<Wire> wires = result.get(r1);
		assertThat(wires).isNotNull();
		assertThat(wires.size()).isEqualTo(1);
	}

	/*
	 * circular dependencies of two
	 */
	@Test
	public void testCircularDependency0() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement r1_req = r1.addRequirement(null, "(r2=*)");
		final Capability r1_cap = r1.addCapability(null, "r1=true");
		final TestResource r2 = new TestResource();
		final Requirement r2_req = r2.addRequirement(null, "(r1=*)");
		final Capability r2_cap = r2.addCapability(null, "r2=42");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2), null, null);
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(r1_req, r2_req);
		context.checkWires(result, new TestWire(r1, r1_req, r2, r2_cap),
				new TestWire(r2, r2_req, r1, r1_cap));
	}

	/*
	 * circular dependencies of three
	 */
	@Test
	public void testCircularDependency1() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement r1_req = r1.addRequirement(null, "(r3=*)");
		final Capability r1_cap = r1.addCapability(null, "r1=true");
		final TestResource r2 = new TestResource();
		final Requirement r2_req = r2.addRequirement(null, "(r1=*)");
		final Capability r2_cap = r2.addCapability(null, "r2=42");
		final TestResource r3 = new TestResource();
		final Requirement r3_req = r3.addRequirement(null, "(r2=*)");
		final Capability r3_cap = r3.addCapability(null, "r3=abc");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2, r3), null, null);
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(r1_req, r2_req, r3_req);
		context.checkWires(result, new TestWire(r1, r1_req, r3, r3_cap),
				new TestWire(r2, r2_req, r1, r1_cap), new TestWire(r3, r3_req,
						r2, r2_cap));
	}

	/*
	 * the resolver should respect the order of the findProviders list in this
	 * simple situation
	 */
	@Test
	public void testOrder() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement req = r1.addRequirement(null, "(cap=*)");
		final TestResource r2 = new TestResource();
		final Capability cap = r2.addCapability(null, "cap=true");
		final TestResource r3 = new TestResource();
		r3.addCapability(null, "cap=42");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2, r3), null, null);
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(req);
		context.checkWires(result, new TestWire(r1, req, r2, cap));
	}

	/*
	 * the resolver needs to respect existing wiring state and create a
	 * consistent resolution
	 */
/* Disabling: see bug 2435
	@Test
	public void testExistingWiringState0() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement r1_req1 = r1.addRequirement(null, "(cap=*)");
		final Requirement r1_req2 = r1.addRequirement(null, "(cap2=*)");
		final TestResource r2 = new TestResource();
		final Capability r2_cap = r2.addCapability(null, "cap=42");
		final Requirement r2_req = r2.addRequirement(null, "(cap2=*)");
		final TestResource r3 = new TestResource();
		r3.addCapability(null, "cap2=true");
		final TestResource r4 = new TestResource();
		final Capability r4_cap = r4.addCapability(null, "cap2=42");

		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1), null, Arrays.<Resource> asList(
						r2, r3, r4)) {
			@Override
			public Map<Resource, Wiring> getWirings() {
				final Map<Resource, Wiring> result = new HashMap<Resource, Wiring>();
				result.put(r2, new Wiring() {

					private List<Wire> wireList = Arrays
							.<Wire> asList(new TestWire(r2, r2_req, r4, r4_cap));

					public List<Capability> getResourceCapabilities(
							String namespace) {
						return Arrays.asList(r2_cap);
					}

					public List<Requirement> getResourceRequirements(
							String namespace) {
						return Arrays.asList(r2_req);
					}

					public List<Wire> getProvidedResourceWires(String namespace) {
						return Collections.emptyList();
					}

					public List<Wire> getRequiredResourceWires(String namespace) {
						return wireList;
					}

					public Resource getResource() {
						return r2;
					}

				});

				result.put(r4, new Wiring() {

					public List<Capability> getResourceCapabilities(
							String namespace) {
						return Arrays.asList(r4_cap);
					}

					public List<Requirement> getResourceRequirements(
							String namespace) {
						return Collections.emptyList();
					}

					public List<Wire> getProvidedResourceWires(String namespace) {
						return Collections.emptyList();
					}

					public List<Wire> getRequiredResourceWires(String namespace) {
						return Collections.emptyList();
					}

					public Resource getResource() {
						return r4;
					}

				});

				return result;
			}

		};
		final Map<Resource, List<Wire>> result = shouldResolve1(context);

		context.checkFindProviderCalls(r1_req1, r1_req2);
		// FIXME: remove debug output
		System.err.println("I HAVE WIRES FOR " + result.get(r1));
		System.err.println("R3 is " + r3);
		System.err.println("R4 is " + r4);
		// FIXME: end debug output
		context.checkWires(result, new TestWire(r1, r1_req1, r2, r2_cap),
				new TestWire(r1, r1_req2, r4, r4_cap));
	}
*/
	/*
	 * if the existing wiring is fully resolved, the result of the resolution is
	 * expected to be empty (delta state)
	 */
	@Test
	public void testExistingWiringState1() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement req = r1.addRequirement(null, "(cap=*)");
		final TestResource r2 = new TestResource();
		final Capability cap = r2.addCapability(null, "cap=42");

		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1, r2), null, null) {
			@Override
			public Map<Resource, Wiring> getWirings() {
				final Map<Resource, Wiring> result = new HashMap<Resource, Wiring>();
				result.put(r1, new Wiring() {

					private List<Wire> wireList = Arrays
							.<Wire> asList(new TestWire(r1, req, r2, cap));

					public List<Capability> getResourceCapabilities(
							String namespace) {
						return Arrays.asList(cap);
					}

					public List<Requirement> getResourceRequirements(
							String namespace) {
						return Arrays.asList(req);
					}

					public List<Wire> getProvidedResourceWires(String namespace) {
						return wireList;
					}

					public List<Wire> getRequiredResourceWires(String namespace) {
						return wireList;
					}

					public Resource getResource() {
						return r1;
					}

				});
				result.put(r2, new Wiring() {

					public List<Capability> getResourceCapabilities(
							String namespace) {
						return Arrays.asList(cap);
					}

					public List<Requirement> getResourceRequirements(
							String namespace) {
						return Collections.emptyList();
					}

					public List<Wire> getProvidedResourceWires(String namespace) {
						return Collections.emptyList();
					}

					public List<Wire> getRequiredResourceWires(String namespace) {
						return Collections.emptyList();
					}

					public Resource getResource() {
						return r2;
					}

				});
				return result;
			}

		};
		final Map<Resource, List<Wire>> result = shouldResolve1(context);
		assertThat(result).isEmpty();
	}

	/*
	 * resolution should ignore non-effecive requirements
	 */
	@Test
	public void testEffective0() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement req1 = r1.addRequirement(null, "(foo=bar)");
		final Requirement req2 = r1.addRequirement(null, "(unavailable=*)");
		final TestResource r2 = new TestResource();
		final Capability cap = r2.addCapability(null, "foo=bar");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1), null,
				Arrays.<Resource> asList(r2)) {
			@Override
			public boolean isEffective(final Requirement requirement) {
				return requirement != req2;
			}
		};
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(req1);
		context.checkWires(result, new TestWire(r1, req1, r2, cap));
	}

	/*
	 * resolver should ignore effective directives and only rely on the resolve
	 * context to declare which requirements are effective
	 */
	@Test
	public void testEffective1() throws Exception {
		final TestResource r1 = new TestResource();
		r1.addRequirement(null, "(foo=bar)");
		final Requirement req2 = r1.addRequirement(null, "(unavailable=*)");
		req2.getDirectives().put(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE,
				Namespace.EFFECTIVE_ACTIVE);
		final TestResource r2 = new TestResource();
		r2.addCapability(null, "foo=bar");
		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1), null,
				Arrays.<Resource> asList(r2));
		shouldNotResolve(context);
	}

	/*
	 * resolution should ignore mandatory directive in non osgi.wiring.*
	 * namespaces
	 */
	@Test
	public void testMandatoryDirective() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement req1 = r1.addRequirement(null, "(foo=bar)");
		final TestResource r2 = new TestResource();
		final Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("foo", "bar");
		final TestCapability cap = r2.new TestCapability(null, attributes) {

			public Map<String, String> getDirectives() {
				final Map<String, String> result = new HashMap<String, String>();
				result.put("mandatory", "not_provided");
				return result;
			}

		};
		r2.addCapability(cap);

		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1), null,
				Arrays.<Resource> asList(r2));
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(req1);
		context.checkWires(result, new TestWire(r1, req1, r2, cap));
	}

	/*
	 * resolver should ignore the singleton directive
	 */
	@Test
	public void testSingletonDirective() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement req1 = r1.addRequirement(null, "(foo=bar)");
		final TestResource r2 = new TestResource();
		final Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("foo", "bar");
		final TestCapability cap = r2.new TestCapability(null, attributes) {

			public Map<String, String> getDirectives() {
				final Map<String, String> result = new HashMap<String, String>();
				result.put("singleton", "true");
				return result;
			}

		};
		r2.addCapability(cap);

		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1), null,
				Arrays.<Resource> asList(r2));
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(req1);
		context.checkWires(result, new TestWire(r1, req1, r2, cap));
	}

	/*
	 * resolver should ignore unresolvable optional resources
	 */
	@Test
	public void testOptionalResources0() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement r1_req = r1.addRequirement(null, "(foo=bar)");
		final TestResource r2 = new TestResource();
		final Capability cap = r2.addCapability(null, "foo=bar");
		final TestResource r3 = new TestResource();
		final Requirement r3_req = r3.addRequirement(null, "(not=provided)");

		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1), Arrays.<Resource> asList(r3),
				Arrays.<Resource> asList(r2));
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(r1_req, r3_req);
		context.checkWires(result, new TestWire(r1, r1_req, r2, cap));
	}

	/*
	 * resolution should make an effort and resolve optional resources
	 */
	@Test
	public void testOptionalResources1() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement req = r1.addRequirement(null, "(foo=bar)");
		final TestResource r2 = new TestResource();
		final Capability cap = r2.addCapability(null, "foo=bar");

		final TestResolveContext context = new TestResolveContext(null,
				Arrays.<Resource> asList(r1), Arrays.<Resource> asList(r2));
		final Map<Resource, List<Wire>> result = shouldResolve(context);
		context.checkFindProviderCalls(req);
		context.checkWires(result, new TestWire(r1, req, r2, cap));
	}

	/*
	 * in this simple case we can test the unresolvable requirements of the
	 * exception
	 */
	@Test
	public void testUnresolvableRequirements() throws Exception {
		final TestResource r1 = new TestResource();
		final Requirement req = r1.addRequirement(null, "(foo=bar)");

		final TestResolveContext context = new TestResolveContext(
				Arrays.<Resource> asList(r1), null, null);
		final ResolutionException re = shouldNotResolve(context);
		assertThat(re).isNotNull();
		final Collection<Requirement> unresolvable = re
				.getUnresolvedRequirements();
		assertThat(unresolvable).isNotNull();
		assertThat(unresolvable.size()).isEqualTo(1);
		assertThat(unresolvable.toArray()[0]).isEqualTo(req);
		assertThat(req).isEqualTo(unresolvable.toArray()[0]);
	}
}
