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

package com.springrts.springls.motd;

import com.springrts.springls.Context;
import com.springrts.springls.commands.CommandProcessors;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a simple bundle activator for the default
 * <tt>MessageOfTheDay</tt> service and related commands.
 */
public class Activator implements BundleActivator {

	private final Logger log = LoggerFactory.getLogger(Activator.class);

	@Override
	public void start(final BundleContext context) {

		final Context springLsContext
				= Context.getService(context, Context.class);

		final MessageOfTheDay messageOfTheDay = new MessageOfTheDay();
		messageOfTheDay.setContext(springLsContext);

		final boolean contentAvailable = messageOfTheDay.read();

		if (contentAvailable) {
			context.registerService(MessageOfTheDay.class.getName(),
					messageOfTheDay, null);
		}

		try {
			CommandProcessors.add(context, CommandProcessors.load(
					UpdateMotdCommandProcessor.class));
		} catch (final Exception ex) {
			log.error("Failed to install the IP2Country command-processors."
					+ " These commands will not be available.", ex);
		}
	}

	@Override
	public void stop(final BundleContext context) {
	}
}
