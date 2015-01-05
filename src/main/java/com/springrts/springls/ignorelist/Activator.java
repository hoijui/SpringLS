/*
	Copyright (c) 2011 Robin Vobruba <hoijui.quaero@gmail.com>

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.springrts.springls.ignorelist;


import com.springrts.springls.Context;
import com.springrts.springls.ServerConfiguration;
import org.apache.commons.configuration.Configuration;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a simple bundle activator for the
 * <tt>IgnoreListService</tt> and its default implementations.
 */
public class Activator implements BundleActivator {

	private final Logger log = LoggerFactory.getLogger(Activator.class);

	@Override
	public void start(final BundleContext context) {

		final Context springLsContext
				= Context.getService(context, Context.class);
		final IgnoreListService banService = createBanService(springLsContext);
		context.registerService(IgnoreListService.class.getName(), banService, null);
	}

	private IgnoreListService createBanService(final Context context) {

		IgnoreListService banService = null;

		final Configuration conf = context.getService(Configuration.class);
		final boolean lanMode = conf.getBoolean(ServerConfiguration.LAN_MODE);

		if (!lanMode) {
			try {
				banService = new JPAIgnoreListService();
			} catch (final Exception pex) {
				log.warn("Failed to access database for ban entries,"
						+ " bans are not supported!", pex);
			}
		}

		if (banService == null) {
			banService = new DummyIgnoreListService();
		}

		return banService;
	}

	@Override
	public void stop(final BundleContext context) {
	}
}