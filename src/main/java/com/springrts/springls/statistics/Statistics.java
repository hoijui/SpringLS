/*
	Copyright (c) 2005 Robin Vobruba <hoijui.quaero@gmail.com>

	This program is free software; you can redistribute it and/or gameify
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

package com.springrts.springls.statistics;


import com.springrts.springls.Battle;
import com.springrts.springls.Clients;
import com.springrts.springls.Context;
import com.springrts.springls.ContextReceiver;
import com.springrts.springls.ServerConfiguration;
import com.springrts.springls.Updateable;
import com.springrts.springls.accounts.AccountsService;
import com.springrts.springls.util.Misc;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.apache.commons.configuration.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Statistics file format:
 * <time> <#active-clients> <#active-battles> <#accounts> <#active-accounts> <list-of-games>
 * where <time> is of form: "hhmmss"
 * and "active battles" are battles that are in-game and have 2 or more players
 * in it and <list-of-games> is a list of first k games (where k is 0 or greater)
 * with frequencies of active battles using these games. Example: XTA 0.66 15.
 * Note that delimiter in <list-of-games> is TAB and not SPACE! See code for more
 * info.
 *
 * Aggregated statistics file format:
 * <date> <time> <#active-clients> <#active-battles> <#accounts> <#active-accounts> <list-of-games>
 * where <date> is of form: "ddMMyy"
 * and all other fields are of same format as those from normal statistics file.
 *
 * @author Betalord
 * @author hoijui
 */
public class Statistics implements ContextReceiver, Updateable {

	private static final Logger LOG = LoggerFactory.getLogger(Statistics.class);

	/** in milliseconds */
	private final long saveStatisticsInterval = 1000 * 60 * 20;
	/**
	 * See the <a href="http://ploticus.sourceforge.net/">Ploticus page</a>
	 * for more info.
	 */
	private static final String PLOTICUS_FULLPATH = "./ploticus/bin/pl";
	private static final String STATISTICS_FOLDER = "./stats/";

	private static final long DAY = 1000L * 60L * 60L * 24L;
	private static final String TODAY_FILTER_FORMAT = "ddMMyy";

	/**
	 * Time when we last updated statistics.
	 * @see java.lang.System#currentTimeMillis()
	 */
	private long lastStatisticsUpdate;

	private Context context;


	public Statistics() {

		lastStatisticsUpdate = System.currentTimeMillis();
		context = null;
	}


	@Override
	public void receiveContext(final Context context) {
		this.context = context;
	}

	@Override
	public void update() {

		final Configuration conf = context.getService(Configuration.class);
		boolean recording =
				conf.getBoolean(ServerConfiguration.STATISTICS_STORE);
		if (recording && ((System.currentTimeMillis() - lastStatisticsUpdate)
				> saveStatisticsInterval))
		{
			saveStatisticsToDisk();
		}
	}

	private void ensureStatsDirExists() {

		// create statistics folder if it does not exist yet
		final File file = new File(STATISTICS_FOLDER);
		if (!file.exists()) {
			final boolean success = (file.mkdir());
			if (!success) {
				LOG.error("Unable to create folder: {}", STATISTICS_FOLDER);
			} else {
				LOG.info("Created missing folder: {}", STATISTICS_FOLDER);
			}
		}
	}

	/**
	 * Saves Statistics to permanent storage.
	 * @return -1 on error; otherwise: time (in milliseconds) it took
	 *   to save the statistics file
	 */
	public int saveStatisticsToDisk() {

		long taken;

		try {
			ensureStatsDirExists();

			lastStatisticsUpdate = System.currentTimeMillis();
			taken = autoUpdateStatisticsFile();
			if (taken != -1) {
				createAggregateFile(); // to simplify parsing
				generatePloticusImages();
				LOG.info("*** Statistics saved to disk. Time taken: {} ms.",
						taken);
			}
		} catch (Exception ex) {
			LOG.error("*** Failed saving statistics... Stack trace:", ex);
			taken = -1;
		}

		return (int) taken;
	}

	/**
	 * Will append statistics file (or create one if it doesn't exist)
	 * and add latest statistics to it.
	 * @return milliseconds taken to calculate statistics, or -1 if it fails
	 */
	private long autoUpdateStatisticsFile() {

		final String fileName = STATISTICS_FOLDER + now("ddMMyy") + ".dat";
		final long startTime = System.currentTimeMillis();

		int activeBattlesCount = 0;
		for (int i = 0; i < context.getBattles().getBattlesSize(); i++) {
			final Battle battle = context.getBattles().getBattleByIndex(i);
			// at least 1 client + founder == 2 players
			if ((battle.getClientsSize() > 0) && battle.inGame()) {
				activeBattlesCount++;
			}
		}

		final String topGames = currentlyPopularGamesList();

		Writer outF = null;
		Writer out = null;
		try {
			outF = new FileWriter(fileName, true);
			out = new BufferedWriter(outF);
			Clients clients = context.getClients();
			AccountsService accounts = context.getAccountsService();
			out.write(now("HHmmss"));
			out.write(" "); out.write("" + clients.getClientsSize());
			out.write(" "); out.write("" + activeBattlesCount);
			out.write(" "); out.write("" + accounts.getAccountsSize());
			out.write(" "); out.write("" + accounts.getActiveAccountsSize());
			out.write(" "); out.write(topGames);
			out.write(Misc.EOL);
		} catch (final IOException ex) {
			LOG.error("Unable to access file <" + fileName + ">. Skipping ...",
					ex);
			return -1;
		} finally {
			try {
				if (out != null) {
					out.close();
				} else if (outF != null) {
					outF.close();
				}
			} catch (final IOException ex) {
				LOG.trace("Failed closing statistics file-writer for file: "
						+ fileName, ex);
			}
		}

		final long updateTime = System.currentTimeMillis() - startTime;

		LOG.info("Statistics have been updated and stored to disk in {} ms",
				updateTime);

		return updateTime;
	}

	/**
	 * This will create "statistics.dat" file which will contain all records
	 * from the last 7 days.
	 */
	private boolean createAggregateFile() {

		final String fileName = STATISTICS_FOLDER + "statistics.dat";

		Writer outF = null;
		Writer out = null;
		try {
			// overwrite if it exists, or create new one
			outF = new FileWriter(fileName, false);
			out = new BufferedWriter(outF);
			String line;

			final SimpleDateFormat formatter = new SimpleDateFormat("ddMMyy");
			final Date today = today();
			long msPerDay = 1000 * 60 * 60 * 24;
			// get file names for last 7 days (that is today + previous 6 days)
			for (int i = 7; i > 0; i--) {
				final Date day = new Date();
				day.setTime(today.getTime() - (((long) i - 1) * msPerDay));
				final String dayStr = formatter.format(day);
				final File fileDay = new File(STATISTICS_FOLDER
						+ formatter.format(dayStr) + ".dat");
				Reader inF = null;
				BufferedReader in = null;
				try {
					inF = new FileReader(fileDay);
					in = new BufferedReader(inF);
					LOG.trace("Found stats: <{}>", fileDay.getAbsolutePath());
					while ((line = in.readLine()) != null) {
						out.write(dayStr);
						out.write(' ');
						out.write(line);
						out.write(Misc.EOL);
					}
				} catch (final IOException ex) {
					LOG.trace("Skipped stats: <" + fileDay.getAbsolutePath()
							+ ">", ex);
				} finally {
					if (in != null) {
						in.close();
					} else if (inF != null) {
						inF.close();
					}
				}
			}
		} catch (final IOException ex) {
			LOG.error("Unable to access file <" + fileName + ">. Skipping ...",
					ex);
			return false;
		} finally {
			try {
				if (out != null) {
					out.close();
				} else if (outF != null) {
					outF.close();
				}
			} catch (final IOException ex) {
				LOG.trace("Failed closing aggregate statistics file-writer for"
						+ " file: " + fileName, ex);
			}
		}

		return true;
	}

	private boolean generatePloticusImages() {

		boolean ret;

		try {
			final SimpleDateFormat dayFormatter = new SimpleDateFormat("ddMMyy");
			// from (today_00:00 - 6 days) till today_00:00
			final Date endDate = today(); // today_00:00
			final Date startDate = new Date(endDate.getTime() - (6L * DAY));
			final String startDateString = dayFormatter.format(startDate);
			final String endDateString = dayFormatter.format(endDate);

			final SimpleDateFormat lastUpdateFormatter
					= new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss (z)");

			final long upTime = System.currentTimeMillis()
					- context.getServer().getStartTime();

			// generate "server stats diagram":
			final String[] cmds = new String[8];
			cmds[0] = PLOTICUS_FULLPATH;
			cmds[1] = "-png";
			cmds[2] = STATISTICS_FOLDER + "info.pl";
			cmds[3] = "-o";
			cmds[4] = STATISTICS_FOLDER + "info.png";
			cmds[5] = "lastupdate=" + lastUpdateFormatter.format(new Date());
			cmds[6] = "uptime=" + Misc.timeToDHM(upTime);
			cmds[7] = "clients=" + context.getClients().getClientsSize();
			Runtime.getRuntime().exec(cmds).waitFor();

			// generate "online clients diagram":
			StringBuilder cmd = new StringBuilder(PLOTICUS_FULLPATH)
					.append(" -png ")
					.append(STATISTICS_FOLDER)
					.append("clients.pl -o ").append(STATISTICS_FOLDER)
					.append("clients.png startdate=").append(startDateString)
					.append(" enddate=").append(endDateString)
					.append(" datafile=").append(STATISTICS_FOLDER)
					.append("statistics.dat");
			Runtime.getRuntime().exec(cmd.toString()).waitFor();

			// generate "active battles diagram":
			cmd = new StringBuilder(PLOTICUS_FULLPATH)
					.append(" -png ").append(STATISTICS_FOLDER)
					.append("battles.pl -o ").append(STATISTICS_FOLDER)
					.append("battles.png startdate=").append(startDateString)
					.append(" enddate=").append(endDateString)
					.append(" datafile=").append(STATISTICS_FOLDER)
					.append("statistics.dat");
			Runtime.getRuntime().exec(cmd.toString()).waitFor();

			// generate "accounts diagram":
			cmd = new StringBuilder(PLOTICUS_FULLPATH)
					.append(" -png ").append(STATISTICS_FOLDER)
					.append("accounts.pl -o ").append(STATISTICS_FOLDER)
					.append("accounts.png startdate=").append(startDateString)
					.append(" enddate=").append(endDateString)
					.append(" datafile=").append(STATISTICS_FOLDER)
					.append("statistics.dat");
			Runtime.getRuntime().exec(cmd.toString()).waitFor();

			// generate "popular games chart":
			final String[] params = getPopularGamesList(now("ddMMyy")).split("\t");
			cmd = new StringBuilder(PLOTICUS_FULLPATH)
					.append(" -png ").append(STATISTICS_FOLDER)
					.append("games.pl -o ").append(STATISTICS_FOLDER)
					.append("games.png count=")
					.append(Integer.parseInt(params[0]))
					.append(" enddate=").append(endDateString)
					.append(" datafile=").append(STATISTICS_FOLDER)
					.append("statistics.dat");
			for (int i = 1; i < params.length; i++) {
				if ((i % 2) != 0) {
					// odd index
					cmd.append("game").append((i + 1) / 2).append("=")
							.append(params[i]);
				} else {
					// even index
					cmd.append("gamefreq").append(i / 2).append("=")
							.append(params[i]);
				}
			}
			Runtime.getRuntime().exec(cmd.toString()).waitFor();

			ret = true;
		} catch (final InterruptedException ex) {
			LOG.error("Failed generating ploticus charts!", ex);
			ret = false;
		} catch (final IOException ex) {
			LOG.error("Failed generating ploticus charts!", ex);
			ret = false;
		}

		return ret;
	}

	/**
	 * Returns the list of games being played right now (top 5 games only)
	 * with frequencies (number of battles).
	 * @return [list-len] "gamename1" [numBattles1] "gamename2" [numBattles2]" ...
	 *   Where delimiter is TAB (not SPACE).
	 *   An empty list is denoted by 0 value for list-len.
	 */
	private String currentlyPopularGamesList() {

		final List<GameBattles> gameBattles = new ArrayList<GameBattles>();

		for (int i = 0; i < context.getBattles().getBattlesSize(); i++) {
			final Battle battle = context.getBattles().getBattleByIndex(i);
			if (battle.inGame() && (battle.getClientsSize() >= 1)) {
				// add to list or update in list:

				int gameIndex = gameBattles.indexOf(battle.getGameName());
				if (gameIndex == -1) {
					gameBattles.add(new GameBattles(battle.getGameName(), 1));
				} else {
					gameBattles.get(gameIndex).addBattles(1);
				}
			}
		}

		return createGamePopularityString(gameBattles);
	}

	private static class GameBattles implements Comparable<GameBattles> {

		public static final Comparator<GameBattles> BATTLES_COMPARATOR
				= new Comparator<GameBattles>() {
			@Override
			public int compare(GameBattles gameBattles1, GameBattles gameBattles2) {
				return gameBattles1.getBattles() - gameBattles2.getBattles();
			}
		};

		private final String name;
		private int battles = 0;

		GameBattles(final String name, final int battles) {

			this.name = name;
			this.battles = battles;
		}

		public String getName() {
			return name;
		}

		public int getBattles() {
			return battles;
		}

		public void addBattles(final int additionalBattles) {
			this.battles += additionalBattles;
		}

		@Override
		public int compareTo(final GameBattles other) {
			return getName().compareTo(other.getName());
		}

		@Override
		public boolean equals(final Object other) {

			if (other instanceof String) {
				return getName().equals((String) other);
			} else if (other instanceof GameBattles) {
				return getName().equals(((GameBattles) other).getName());
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 23 * hash + (this.name != null ? this.name.hashCode() : 0);
			return hash;
		}
	}

	/**
	 * Returns a list of the top 5 popular games for a certain date.
	 * The date must be in the format "ddMMyy". It will take the first entry for
	 * every new hour and add it to the list. Other entries for the same hour
	 * will be ignored.
	 * @return [list-len] "gameName1" [numBattles1] "gameName2" [numBattles2]" ...
	 *   Where delimiter is TAB (not SPACE).
	 *   An empty list is denoted by 0 value for list-len.
	 * @see #currentlyPopularGamesList()
	 */
	private String getPopularGamesList(final String date) {

		String popularGamesList;

		final File file = new File(STATISTICS_FOLDER + date + ".dat");
		Reader inF = null;
		BufferedReader in = null;
		try {
			byte lastHour = -1;
			String line;
			inF = new FileReader(file);
			in = new BufferedReader(inF);
			final List<GameBattles> gameBattles = new ArrayList<GameBattles>();
			while ((line = in.readLine()) != null) {
				final byte sHour = Byte.parseByte(line.substring(0, 2)); // 00 .. 23
				if (lastHour == sHour) {
					continue; // skip this input line
				}
				lastHour = sHour;
				final String gameFrequencyStr = Misc.makeSentence(line.split(" "), 5);
				final String[] gameFrequenciesRaw = gameFrequencyStr.split("\t");
				if ((gameFrequenciesRaw.length % 2) != 1) {
					// the number of arguments must be odd
					// -> numGames + (numGames * (gameName + gameFrequency))
					throw new Exception("Bad game list format");
				}
				final int numGames = Integer.parseInt(gameFrequenciesRaw[0]);
				if (gameFrequenciesRaw.length != (1 + (numGames * 2))) {
					throw new Exception("Bad game list format");
				}
				for (int i = 0; i < numGames; i++) {
					final int i2 = i * 2;
					final String name = gameFrequenciesRaw[i2 + 1];
					final int battles = Integer.parseInt(gameFrequenciesRaw[i2 + 2]);

					final int gameIndex = gameBattles.indexOf(name);
					if (gameIndex == -1) {
						gameBattles.add(new GameBattles(name, battles));
					} else {
						gameBattles.get(gameIndex).addBattles(battles);
					}
				}
			}

			popularGamesList = createGamePopularityString(gameBattles);
		} catch (final Exception ex) {
			LOG.error("Error in getPopularGamesList(). Skipping ...", ex);
			popularGamesList = "0";
		} finally {
			try {
				if (in != null) {
					in.close();
				} else if (inF != null) {
					inF.close();
				}
			} catch (final IOException ex) {
				LOG.trace("Failed closing statistics file-reader for file: "
						+ file.getAbsolutePath(), ex);
			}
		}

		return popularGamesList;
	}

	private static String createGamePopularityString(
			final List<GameBattles> gameBattles)
	{
		// now generate a list of top 5 games with frequencies:
		final StringBuilder result = new StringBuilder(512);
		final int numGames = Math.min(5, gameBattles.size()); // return 5 or less games
		result.append(numGames);
		// Note: do not cut the array by numGames,
		//   or sorting will not have any effect!
		Collections.sort(gameBattles, GameBattles.BATTLES_COMPARATOR);
		for (final GameBattles game : gameBattles) {
			result.append("\t").append(game.getName());
			result.append("\t").append(game.getBattles());
		}

		return result.toString();
	}

	/**
	 * Usage (some examples):
	 *
	 * @param format examples:
	 *   "dd MMMMM yyyy"
	 *   "yyyyMMdd"
	 *   "dd.MM.yy"
	 *   "MM/dd/yy"
	 *   "yyyy.MM.dd G 'at' hh:mm:ss z"
	 *   "EEE, MMM d, ''yy"
	 *   "h:mm a"
	 *   "H:mm:ss:SSS"
	 *   "K:mm a,z"
	 *   "yyyy.MMMMM.dd GGG hh:mm aaa"
	 *
	 * Taken from <a href="http://www.rgagnon.com/javadetails/java-0106.html">
	 * here</a>.
	 *
	 * Also see
	 * <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/text/SimpleDateFormat.html">
	 * SimpleDateFormat JavaDoc</a> for more info.
	 */
	private static String now(final String format) {

		final Date now = new Date();
		final SimpleDateFormat formatter = new SimpleDateFormat(format);
		final String current = formatter.format(now);

		return current;
	}

	private static Date today() {

		Date today = null;

		try {
			today = new SimpleDateFormat(TODAY_FILTER_FORMAT)
					.parse(now(TODAY_FILTER_FORMAT));
		} catch (final ParseException ex) {
			// this could not possible ever happen!
			LOG.error("Failed creating date string for 'today'", ex);
		}

		return today;
	}
}
