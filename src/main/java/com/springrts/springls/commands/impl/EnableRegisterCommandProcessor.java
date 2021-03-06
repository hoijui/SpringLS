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

package com.springrts.springls.commands.impl;


import com.springrts.springls.Account;
import com.springrts.springls.Client;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.SupportedCommand;
import com.springrts.springls.util.ProtocolUtil;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("ENABLEREGISTER")
public class EnableRegisterCommandProcessor extends AbstractCommandProcessor {

	public EnableRegisterCommandProcessor() {
		super(Account.Access.ADMIN);
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException
	{
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		if (args.size() == 1) {
			boolean enableRegistration
					= ProtocolUtil.numberToBool(Byte.parseByte(args.get(0)));
			getContext().getAccountsService()
					.setRegistrationEnabled(enableRegistration);
		}
		client.sendLine(String.format("SERVERMSG The REGISTER command is %s",
				getContext().getAccountsService().isRegistrationEnabled()
				? "enabled" : "disabled"));

		return true;
	}
}
