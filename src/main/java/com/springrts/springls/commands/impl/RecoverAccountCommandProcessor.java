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
import com.springrts.springls.accounts.AccountsService;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * @author hoijui
 */
@SupportedCommand("RECOVERACCOUNT")
public class RecoverAccountCommandProcessor extends AbstractCommandProcessor {

	public RecoverAccountCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("email"),
						new Argument("username", true)),
				Account.Access.NONE);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final String email = (String)args.getWords().get(0);
		final String emailLower = email.toLowerCase();

		final AccountsService accountsService = getContext().getAccountsService();
		final List<Account> fittingAccounts = accountsService.findAccountsByEmail(emailLower);

		if (!fittingAccounts.isEmpty()) {
			if (args.getWords().size() > 1) {
				final String username = (String)args.getWords().get(1);
				final Account toRecover = accountsService.getAccount(username);
				if ((toRecover == null) || !fittingAccounts.contains(toRecover)) {
					// the requested username does not exist,
					// or does not have the given email address set
					return false;
				}
				fittingAccounts.clear();
				fittingAccounts.add(toRecover);
			}
			sendRecoverEmail(emailLower, fittingAccounts);
		} else {
			// no account has the given email assigned to it
			return false;
		}
	}

	private void sendRecoverEmail(final String email, final List<Account> toRecover) {

		final String subject = "Spring RTS lobby account recovery";
		String content =
				"You requested recovery of one or multiple accounts"
				+ " associated with this email."
				+ " If you did not do this yourself,"
				+ " you may safely ignore this email.\n\n"
				+ "Your accounts are:";
		for (final Account account : toRecover) {
			content += " " + account.getName();
		}

		sendEmail(email, subject, content);
	}

	private void sendEmail(final String toEmail, final String subject, final String content) {

		// Sender's email ID needs to be mentioned
		final String from = "web@gmail.com";

		// Assuming you are sending email from localhost
		final String host = "localhost";

		// Get system properties
		final Properties properties = System.getProperties();

		// Setup mail server
		properties.setProperty("mail.smtp.host", host);

		// Get the default Session object.
		final Session session = Session.getDefaultInstance(properties);

		try {
			// Create a default MimeMessage object.
			final MimeMessage message = new MimeMessage(session);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress(from));

			// Set To: header field of the header.
			message.addRecipient(Message.RecipientType.TO,
					new InternetAddress(toEmail));

			// Set Subject: header field
			message.setSubject(subject);

			// Now set the actual message
			message.setText(content);

			// Send message
			Transport.send(message);
			System.out.println("Sent message successfully....");

			final Transport transport = session.getTransport("smtp");
//			transport.connect(user, password);
//			transport.sendMessage(message, new Address(toEmail));
			// TODO implement this stuff, and clean it up a lot!
		} catch (final MessagingException mex) {
			mex.printStackTrace();
		}
	}
}
