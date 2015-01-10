/*
	Copyright (c) 2010 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls.commands;


import com.springrts.springls.Account;
import com.springrts.springls.Battle;
import com.springrts.springls.Client;
import com.springrts.springls.Context;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.configuration.Configuration;
import org.osgi.framework.BundleContext;

/**
 * Utility base class for command processors.
 * @author hoijui
 */
public abstract class AbstractCommandProcessor implements CommandProcessor {

	public static final int ARGS_MIN_NOCHECK = -1;
	public static final int ARGS_MAX_NOCHECK = -1;
	private static final Account.Access ACCESS_NOCHECK = null;
	private Context context;
	private final String commandName;
	private final CommandArguments arguments;
	private final Account.Access accessMin;
	private final boolean battleRequired;
	private final boolean battleFounderRequired;
//	private boolean sendingServerMsgOnError;
	private static final String DEFAULT_TO_CLIENT_ERROR_COMMAND_NAME = "SERVERMSG";
	private String toClientErrorCommandName;
	private final List<CommandProcessingExceptionListener> parsingExceptionListeners;

	protected AbstractCommandProcessor(
			final CommandArguments arguments,
			final Account.Access accessMin,
			final boolean battleRequired,
			final boolean battleFounderRequired)
	{
		this.context = null;
		this.commandName
				= CommandProcessors.extractCommandName(this.getClass());
		this.arguments = arguments;
		this.accessMin = accessMin;
		this.battleRequired = battleRequired;
		this.battleFounderRequired = battleFounderRequired;
//		this.sendingServerMsgOnError = false;
		this.toClientErrorCommandName = null;
		this.parsingExceptionListeners
				= new LinkedList<CommandProcessingExceptionListener>();
//		this.argsNamed = new HashSet<NamedArgument>(Arrays.asList(argsNamed));
//		if (battleFounderRequired && !battleRequired) {
//			throw new IllegalArgumentException("The client can never be founder"
//					+ " of a battle but not in a battle");
//		}
//		if ((argsWordMin > argsWordMax) && (argsWordMax != ARGS_MAX_NOCHECK)) {
//			throw new IllegalArgumentException("The minimium amount of word"
//					+ " arguments can not be bigger then the maximum");
//		}
//		if ((argsSentenceMin > argsSentenceMax)
//				&& (argsSentenceMax != ARGS_MAX_NOCHECK))
//		{
//			throw new IllegalArgumentException("The minimium amount of sentence"
//					+ " arguments can not be bigger then the maximum");
//		}
//		if ((argsWordMin != argsWordMax) && (argsSentenceMax != 0)) {
//			throw new IllegalArgumentException("Due to arguments separators"
//					+ " (SPACE for words, TAB for sentences),"
//					+ " it is not possible to use sentence arguments together"
//					+ " with a variable ammount of word arguments");
//		}
//		if (!this.argsNamed.isEmpty() && !usingNamedArguments) {
//			throw new IllegalArgumentException("Named arguments are given,"
//					+ " but we are told to not use named arguments");
//		}
//		if (usingNamedArguments && ((argsWordMax > 0) || (argsSentenceMax > 0)))
//		{
//			throw new IllegalArgumentException("Named arguments can not be"
//					+ " mixed with indexed (word and/or sentence) arguments");
//		}
	}
	protected AbstractCommandProcessor(
			final CommandArguments arguments,
			final Account.Access accessMin,
			final boolean battleRequired)
	{
		this(
				arguments,
				accessMin,
				battleRequired,
				false);
	}
	protected AbstractCommandProcessor(
			final Account.Access accessMin,
			final boolean battleRequired,
			final boolean battleFounderRequired)
	{
		this(
				new CommandArguments(false),
				accessMin,
				battleRequired,
				battleFounderRequired);
	}
	protected AbstractCommandProcessor(
			final CommandArguments arguments,
			final Account.Access accessMin)
	{
		this(
				arguments,
				accessMin,
				false,
				false);
	}
	protected AbstractCommandProcessor(
			final Account.Access accessMin,
			final boolean battleRequired)
	{
		this(
				accessMin,
				battleRequired,
				false);
	}
	protected AbstractCommandProcessor(final CommandArguments arguments) {
		this(arguments, ACCESS_NOCHECK);
	}
	protected AbstractCommandProcessor(final Account.Access accessMin) {
		this(new CommandArguments(false), accessMin);
	}
	protected AbstractCommandProcessor() {
		this(new CommandArguments(false));
	}

	@Override
	public CommandArguments getArguments() {
		return this.arguments;
	}

	@Override
	public void setContext(final Context context) {
		this.context = context;
	}
	protected Context getContext() {
		return context;
	}
	protected Configuration getConfiguration() {
		return context.getService(Configuration.class);
	}
	protected BundleContext getBundleContext() {
		return context.getFramework().getBundleContext();
	}
	public <T> T getService(final Class<T> serviceClass) {
		return Context.getService(getBundleContext(), serviceClass);
	}

	/**
	 * Returns the name of the command supported by this processor.
	 * @see SupportedCommand
	 * @return command name extracted from the {@link SupportedCommand} annotation
	 */
	public String getCommandName() {
		return this.commandName;
	}

//	protected void setSendingServerMsgOnError(final boolean sendingServerMsgOnError) {
//		this.sendingServerMsgOnError = sendingServerMsgOnError;
//	}
//
//	protected boolean isSendingServerMsgOnError() {
//		return this.sendingServerMsgOnError;
//	}

	protected void setToClientErrorCommandName(final String toClientErrorCommandName) {
		this.toClientErrorCommandName = toClientErrorCommandName;
	}

	protected String getToClientErrorCommandName() {
		return toClientErrorCommandName;
	}

	protected boolean isUsingCustomToClientErrorCommandName() {
		return (toClientErrorCommandName != null);
	}

	/**
	 * Returns the command as it was given to the server.
	 * @param args the arguments of the command
	 * @return command-name + " " + arg0 + " " + arg1 ...
	 * @deprecated unused
	 */
	public String reconstructFullCommand(final List<String> args) {

		final StringBuilder fullCommand = new StringBuilder(getCommandName());

		for (final String arg : args) {
			fullCommand.append(' ').append(arg);
		}

		return fullCommand.toString();
	}

//	/**
//	 * Returns the minimum number of word arguments required by the command.
//	 * @return
//	 */
//	public int getArgsWordMin() {
//		return this.argsWordMin;
//	}
//
//	/**
//	 * Returns the maximum number of word arguments supported by the command.
//	 * @return
//	 */
//	public int getArgsWordMax() {
//		return this.argsWordMax;
//	}
//
//	/**
//	 * Returns the minimum number of sentence arguments required by the command.
//	 * @return
//	 */
//	public int getArgsSentenceMin() {
//		return this.argsSentenceMin;
//	}
//
//	/**
//	 * Returns the maximum number of sentence arguments supported by the command.
//	 * @return
//	 */
//	public int getArgsSentenceMax() {
//		return this.argsSentenceMax;
//	}
//
//	/**
//	 * Returns the named arguments supported by this command, though there can be others,
//	 * which will be ignored.
//	 * @return all named arguments supported by this command
//	 */
//	public Set<NamedArgument> getArgsNamed() {
//		return this.argsNamed;
//	}

	/**
	 * Returns the minimum access right required to execute the command.
	 * @return minimum access right required to execute, or <code>null</code>.
	 *   if no check should be performed.
	 */
	public Account.Access getAccessMin() {
		return this.accessMin;
	}

	/**
	 * Returns whether this command requires the sending client to be in a
	 * battle.
	 * @return true if the sending client is required to be in a battle.
	 */
	public boolean isBattleRequired() {
		return this.battleRequired;
	}

	/**
	 * Returns whether this command requires the sending client to be the
	 * founder of a battle.
	 * @return true if the sending client is requires to be the founder of a
	 *   battle.
	 */
	public boolean isBattleFounderRequired() {
		return this.battleFounderRequired;
	}

	/**
	 * Returns whether the client is currently in a battle.
	 * @param client to find the battle for
	 * @return true if the client is currently in a battle.
	 */
	protected Battle getBattle(final Client client) {
		return getContext().getBattles().getBattleByID(client.getBattleID());
	}

	/**
	 * Checks whether the client is currently in a battle.
	 * @param client to check if it is currently in a battle
	 * @return true if the client is currently in a battle.
	 */
	protected boolean isInBattle(final Client client) {
		return (getBattle(client) != null);
	}

	/**
	 * Returns whether the client is currently founder of a battle.
	 * @param client to check if it is the battles founder
	 * @return true if the client is currently founder of a battle.
	 */
	protected boolean isBattleFounder(final Client client) {

		final Battle battle = getContext().getBattles()
					.getBattleByID(client.getBattleID());
		return ((battle != null) && (battle.getFounder() == client));
	}

	protected void addParsingExceptionListener(final CommandProcessingExceptionListener lst) {
		parsingExceptionListeners.add(lst);
	}

	protected void removeParsingExceptionListener(final CommandProcessingExceptionListener lst) {
		parsingExceptionListeners.remove(lst);
	}

	protected void fireParsingExceptionOccurred(final CommandProcessingException exception) {

		final CommandProcessingExceptionEvent evt = new CommandProcessingExceptionEvent(this, exception);
		for (final CommandProcessingExceptionListener lst : parsingExceptionListeners) {
			lst.parsingExceptionOccurred(evt);
		}
	}

//	@Override
//	public ParsedCommandArguments parseArguments(
//			Client client,
//			String command,
//			int argsStartIndex)
//	{
//		ParsedCommandArguments parsedArgs = null;
//		try {
//			parsedArgs = getArguments().parse(client, command, argsStartIndex);
//		} catch (CommandParsingException ex) {
//			fireParsingExceptionOccurred(ex);
//		}
//
//		return parsedArgs;
//	}

	protected void processingError(final Client client, final String message)
			throws CommandProcessingException
	{
//		if (isSendingServerMsgOnError()) {
			final String finalCmdName = isUsingCustomToClientErrorCommandName()
					? toClientErrorCommandName
					: DEFAULT_TO_CLIENT_ERROR_COMMAND_NAME;
			client.sendLine(finalCmdName + ' ' + message);
//		}
		processingError(message);
	}

	protected void processingError(final Client client, final Throwable cause)
			throws CommandProcessingException
	{
//		if (isSendingServerMsgOnError()) {
			final String finalCmdName = isUsingCustomToClientErrorCommandName()
					? toClientErrorCommandName
					: DEFAULT_TO_CLIENT_ERROR_COMMAND_NAME;
			client.sendLine(finalCmdName + ' ' + cause.getMessage());
//		}
		processingError(cause);
	}

	protected void processingError(final String message)
			throws CommandProcessingException
	{
		throw new CommandProcessingException(getCommandName(), message);
	}

	protected void processingError(final Throwable cause)
			throws CommandProcessingException
	{
		throw new CommandProcessingException(getCommandName(), "<NO-MESSAGE>", cause);
	}

	protected void processingError() throws CommandProcessingException {
		processingError("<NO-MESSAGE>");
		return;
	}

	/**
	 * Process one instance of the command.
	 * @param client
	 * @param args
	 */
	protected abstract void process(
			Client client,
			ParsedCommandArguments args)
			throws CommandProcessingException;

	/**
	 * Perform common checks and pre-parse the arguments.
	 * @param client
	 * @param args
	 */
	@Override
	public void process(
			final Client client,
			final String commandClean,
			final int argsStartIndex)
			throws CommandProcessingException
	{
		final ParsedCommandArguments parsedArgs;
		try {
			// parse command args
			parsedArgs = getArguments().parse(
					getCommandName(), commandClean, argsStartIndex);

			runPreProcessChecks(client, parsedArgs);
		} catch (final CommandProcessingException ex) {
			if (isUsingCustomToClientErrorCommandName()) {
				processingError(client, ex);
				return;
			}
			fireParsingExceptionOccurred(ex); // NOTE should be un-required by now...
			throw ex;
		}

		try {
			process(client, parsedArgs);
		} catch (final CommandProcessingException ex) {
			fireParsingExceptionOccurred(ex); // NOTE should be un-required by now...
			throw ex;
		}
	}

	private void runPreProcessChecks(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		if ((getAccessMin() != ACCESS_NOCHECK)
				&& client.getAccount().getAccess().isLessThen(getAccessMin()))
		{
			throw new InsufficientAccessCommandProcessingException(
					getCommandName(),
					getAccessMin(),
					client.getAccount().getAccess());
		}

//		if ((getArgsMin() != ARGS_MIN_NOCHECK)
//				&& (args.size() < getArgsMin()))
//		{
//			throw new TooFewArgumentsCommandProcessingException(
//					getCommandName(),
//					getArgsMin(),
//					args.size());
//		}
//		if ((getArgsMax() != ARGS_MAX_NOCHECK)
//				&& (args.size() > getArgsMax()))
//		{
//			throw new TooManyArgumentsCommandProcessingException(
//					getCommandName(),
//					getArgsMax(),
//					args.size());
//		}

		if (isBattleFounderRequired() && !isBattleFounder(client)) {
			throw new CommandProcessingException(getCommandName(),
					"This command requires the issuing client to be founder of"
					+ " the battle");
		} else if (isBattleRequired() && !isInBattle(client)) {
			throw new CommandProcessingException(getCommandName(),
					"This command requires the issuing client to in a battle");
		}
	}
}
