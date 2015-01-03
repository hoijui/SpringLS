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

package com.springrts.springls.ip2country;


import com.springrts.springls.Account;
import com.springrts.springls.Client;
import com.springrts.springls.util.Misc;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import com.springrts.springls.util.ProtocolUtil;
import java.net.InetAddress;
import java.util.List;

/**
 * Lets an administrator convert an IP into a (2-chars wide) country code.
 * NOTE This command is SpringLS specific, not part of the official lobby protocol!
 * @author hoijui
 */
@SupportedCommand("IP2COUNTRY")
public class Ip2CountryCommandProcessor extends AbstractCommandProcessor {

	public Ip2CountryCommandProcessor() {
		super(1, 1, Account.Access.ADMIN);
	}

	@Override
	public boolean process(final Client client, final List<String> args)
			throws CommandProcessingException
	{
		final boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		final String ipAddress = args.get(0);

		final InetAddress addr = Misc.parseIp(ipAddress);
		if (addr == null) {
			client.sendLine("SERVERMSG Invalid IP address/range: " + ipAddress);
			return false;
		}

		String country = ProtocolUtil.COUNTRY_UNKNOWN;
		final IP2Country service = getContext().getService(IP2Country.class);
		if (service != null) {
			country = service.getCountryCode(addr);
		}

		client.sendLine("SERVERMSG Country = " + country);

		return true;
	}
}
