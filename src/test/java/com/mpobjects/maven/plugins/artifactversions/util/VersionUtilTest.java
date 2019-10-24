/*
 * Copyright 2019, MP Objects, http://www.mp-objects.com
 */
package com.mpobjects.maven.plugins.artifactversions.util;

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;

/**
 *
 */
public class VersionUtilTest {

	@Test
	public void testGetQualifiers() {
		Set<String> qualifiers = VersionUtil.getQualifiers("1.0.0-SNAPSHOT");
		Assert.assertEquals(Collections.singleton("SNAPSHOT"), qualifiers);

		qualifiers = VersionUtil.getQualifiers("1.0.0-alpha1");
		Assert.assertEquals(Collections.singleton("alpha"), qualifiers);

		qualifiers = VersionUtil.getQualifiers("1.0.0alpha1");
		Assert.assertEquals(Collections.singleton("alpha"), qualifiers);

		qualifiers = VersionUtil.getQualifiers("1.0-sp-foo");
		Assert.assertEquals(Sets.newHashSet("foo", "sp"), qualifiers);
	}

}
