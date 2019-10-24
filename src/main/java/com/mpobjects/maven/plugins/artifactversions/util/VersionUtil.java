/*
 * Copyright 2019, MP Objects, http://www.mp-objects.com
 */
package com.mpobjects.maven.plugins.artifactversions.util;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 */
public final class VersionUtil {

	/**
	 * Based on GenericVersion.Tokenizer.
	 */
	private static class Tokenizer {

		private int index;

		private final int length;

		private String qualifier;

		private final String version;

		private Tokenizer(String aVersion) {
			version = aVersion;
			length = version.length();
		}

		public String getQualifier() {
			return qualifier;
		}

		public boolean next() {
			while (index < length) {
				if (nextQualifier()) {
					return true;
				}
			}
			return false;
		}

		private boolean nextQualifier() {
			if (index >= length) {
				return false;
			}

			int start = -1;
			int end = -1;

			for (; index < length; ++index) {
				char c = version.charAt(index);

				if ('.' == c || '-' == c || '_' == c) {
					if (end == -1) {
						end = index;
					}
					++index;
					break;
				} else if (Character.isDigit(c)) {
					if (start > -1) {
						// numbers after the qualifier
						end = index;
					}
				} else if (start == -1) {
					// start of the qualifier
					start = index;
				}
			}

			if (start > -1) {
				qualifier = version.substring(start, end != -1 ? end : length);
				return true;
			} else {
				return false;
			}
		}

	}

	private VersionUtil() {
		// nop
	}

	public static Set<String> getQualifiers(String aVersionString) {
		if (aVersionString == null || aVersionString.length() == 0) {
			return Collections.emptySet();
		}
		Set<String> qualifiers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (Tokenizer tok = new Tokenizer(aVersionString); tok.next();) {
			qualifiers.add(tok.getQualifier());
		}
		return qualifiers;
	}

}
