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
import com.springrts.springls.ServerNotification;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;

/**
 * @author hoijui
 */
@SupportedCommand("CHANGEACCOUNTACCESS")
public class ChangeAccountAccessCommandProcessor extends AbstractCommandProcessor {

	public ChangeAccountAccessCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("username"),
						new Argument("accessBits", Integer.class, Argument.PARSER_TO_INTEGER)),
				Account.Access.ADMIN);
	}

	@Override
	public boolean process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		final String username = (String)args.getWords().get(0);
		final int newAccessBifField = (Integer)args.getWords().get(1);

		Account account
				= getContext().getAccountsService().getAccount(username);
		if (account == null) {
			return false;
		}

		final int oldAccessBitField = account.getAccessBitField();
		final Account accountNew = account.clone();
		accountNew.setAccess(Account.extractAccess(newAccessBifField));
		accountNew.setBot(Account.extractBot(newAccessBifField));
		accountNew.setInGameTime(Account.extractInGameTime(newAccessBifField));
		accountNew.setAgreementAccepted(Account.extractAgreementAccepted(newAccessBifField));
		final boolean mergeOk = getContext().getAccountsService().mergeAccountChanges(accountNew, accountNew.getName());
		if (mergeOk) {
			account = accountNew;
		} else {
			client.sendLine(String.format(
					"SERVERMSG Changing ACCESS for account <%s> failed.",
					account.getName()));
			return false;
		}

		getContext().getAccountsService().saveAccounts(false); // save changes
		// FIXME Do this just in case if rank got changed?
		//Client target=context.getClients().getClient(commands[1]);
		//target.setRank(client.account.getRank().ordinal());
		//if(target.alive)
		//	context.getClients().notifyClientsOfNewClientStatus(target);

		client.sendLine(String.format(
				"SERVERMSG You have changed ACCESS for <%s> successfully.",
				account.getName()));

		// add server notification:
		final ServerNotification srvNotif = new ServerNotification(
				"Account access changed by admin");
		srvNotif.addLine(String.format(
				"Admin <%s> has changed access/status bits for account <%s>.",
				client.getAccount().getName(), account.getName()));
		srvNotif.addLine(String.format("Old access code: %d. New code: %d",
				oldAccessBitField, newAccessBifField));
		getContext().getServerNotifications().addNotification(srvNotif);

		return true;
	}
}
