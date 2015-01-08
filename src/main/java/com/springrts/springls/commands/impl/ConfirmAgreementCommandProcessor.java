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


import com.springrts.springls.Client;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;

/**
 * Allows a user to "activate" his account after registration.
 * Updates a client temporary account. He is not logged in yet,
 * since he needs to confirm the agreement before the server will allow him
 * to log in.
 * @author hoijui
 */
@SupportedCommand("CONFIRMAGREEMENT")
public class ConfirmAgreementCommandProcessor extends AbstractCommandProcessor {

	public ConfirmAgreementCommandProcessor() {
		super();
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		client.getAccount().setAgreementAccepted(true);
		final boolean mergeOk = getContext().getAccountsService()
				.mergeAccountChanges(client.getAccount(),
				client.getAccount().getName());
		if (!mergeOk) {
			processingError(
					"Failed saving 'agreement accepted' state to persistent"
					+ " storage for user: " + client.getAccount().getName());
		}
	}
}
