package scripts;

import java.awt.Color;
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL; 
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tribot.api.General;
import org.tribot.api2007.Banking;
import org.tribot.api2007.Login;
import org.tribot.api2007.Player;
import org.tribot.api2007.WebWalking;
import org.tribot.api2007.types.RSItem;
import org.tribot.api2007.types.RSItemDefinition;
import org.tribot.script.Script;
import org.tribot.script.ScriptManifest;
import org.tribot.script.interfaces.Painting;
import org.tribot.util.Util;

import scripts.fc.fcpaint.FCPaint;
import scripts.fc.fcpaint.FCPaintable;

@ScriptManifest(authors = { "Crimson" }, category = "Tools", name = "Bank Evaluator")

public class BankEvaluator extends Script implements FCPaintable, Painting {

	ArrayList<RSItem> bankList = new ArrayList<RSItem>();
	ArrayList<RSItem> memberItems = new ArrayList<RSItem>();
	ArrayList<RSItem> f2pItems = new ArrayList<RSItem>();
	private boolean alive = true;
	private boolean filterStarted = false;
	private boolean filterDone = false;
 	final FCPaint PAINT = new FCPaint(this, Color.GREEN);
 	private int originalCount = 0;
	private int f2pWealth = 0;
	private int memberWealth = 0;
	public static final Pattern overallPattern = Pattern.compile("(?:\"overall\":)([0-9]+)"); //Thanks @IanC

	@Override
	public void run() {
		while (alive) {
			sleep(300, 600);
			checkIngame();
			if (Banking.isInBank()) {
				if (!Banking.isBankScreenOpen()) {
					Banking.openBank();
				} else {
					priceCheck();
				}
			} else {
				WebWalking.walkToBank();
			}
		}
	}

	private void checkIngame() {
		while (Login.getLoginState() != Login.STATE.INGAME) {
			sleep(General.random(1000, 3000));
			System.out.println("Waiting to be logged in...");
		}
	}

	private void setBankList() {
		RSItem[] currentBank = Banking.getAll();
		if (currentBank.length == 0) {
			System.out.println("Your bank seems to be empty. Stopping.");
			alive = false;
		}
		try {
			for (RSItem item : currentBank) {
				bankList.add(item);
				System.out.println("Loaded: " + RSItemDefinition.get(item.getID()).getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void priceCheck() {
		originalCount = Banking.getAll().length;
		if (bankList.isEmpty()) {
			setBankList();
		}
		if (!bankList.isEmpty() && !filterStarted) {
			filterList();
		}
		if (filterDone) {
			makeTextFile();
		}
	}

	private void filterList() {
		filterStarted = true;
		try {
			for (RSItem item : bankList) {
				if (RSItemDefinition.get(item.getID()).isMembersOnly()) {
					memberItems.add(item);
				} else {
					f2pItems.add(item);
				}
			}
			System.out.println(originalCount + " was original size. " + memberItems.size() + " Member items. "
					+ f2pItems.size() + " F2P items.");
			if (memberItems.size() + f2pItems.size() != originalCount) {
				System.out.println("Wrong total?");
			} else {
				System.out.println("Correct total.");
			}
			filterDone = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static int getOverallPrice(int id) throws MalformedURLException, IOException {
		String url = "http://api.rsbuddy.com/grandExchange?a=guidePrice&i="+id;
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new URL(String.format(url, id)).openConnection().getInputStream()));
		String line;while((line=reader.readLine())!=null)
	
		{
			Matcher matcher = overallPattern.matcher(line);
			if (matcher.find() && matcher.groupCount() > 0) {
				int overallPrice = Integer.parseInt(matcher.group(1));
				if (overallPrice == 0) {
					overallPrice = 1;
				}
				return overallPrice;
			}
		}
		return 1;
	}

	private void makeTextFile() { //Intended to use google GSON for this, but could not get tribot to load external .jar Will revisit later
		try {
			File dir = new File(Util.getWorkingDirectory() + "/banks/");
			File file = new File(dir.getAbsolutePath() + "/" + Player.getRSPlayer().getName() + "-"
					+ System.currentTimeMillis() + ".txt");

			if (!dir.exists()) {
				dir.mkdir();
			}

			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			file.setWritable(true);

			bw.write("#F2P");
			bw.newLine();

			for (RSItem item : f2pItems) {
				int price = getOverallPrice(item.getID());
				bw.write(item.getStack() + " x " + RSItemDefinition.get(item.getID()).getName() + " @ "
						+ NumberFormat.getNumberInstance(Locale.US).format(price) + " each = "
						+ NumberFormat.getNumberInstance(Locale.US).format(price * item.getStack()));
				f2pWealth += (price * item.getStack());
				bw.newLine();
			}

			bw.newLine();
			bw.write("#Total F2P: " + NumberFormat.getNumberInstance(Locale.US).format(f2pWealth));
			bw.newLine();
			bw.newLine();
			bw.write("#Members");
			bw.newLine();

			for (RSItem item : memberItems) {
				int price = getOverallPrice(item.getID());
				bw.write(item.getStack() + " x " + RSItemDefinition.get(item.getID()).getName() + " @ "
						+ NumberFormat.getNumberInstance(Locale.US).format(price) + " each = "
						+ NumberFormat.getNumberInstance(Locale.US).format(price * item.getStack()));
				memberWealth += (price * item.getStack());
				bw.newLine();
			}

			bw.newLine();
			bw.write("#Total Members: " + NumberFormat.getNumberInstance(Locale.US).format(memberWealth));
			bw.newLine();
			bw.newLine();
			bw.write("F2P Total: " + NumberFormat.getNumberInstance(Locale.US).format(f2pWealth) + ", Member Total: "
					+ NumberFormat.getNumberInstance(Locale.US).format(memberWealth) + ", overall: "
					+ NumberFormat.getNumberInstance(Locale.US).format(memberWealth + f2pWealth));

			bw.close();

			System.out.println("F2P Total: " + NumberFormat.getNumberInstance(Locale.US).format(f2pWealth)
					+ ", Member Total: " + NumberFormat.getNumberInstance(Locale.US).format(memberWealth)
					+ ", overall: " + NumberFormat.getNumberInstance(Locale.US).format(memberWealth + f2pWealth));

			alive = false;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onPaint(Graphics g) {
		PAINT.paint(g);
	}
	
	@Override
	public String[] getPaintInfo() {
		return new String[] {"Bank size: "+originalCount, "F2P items: "+f2pItems.size(), "F2P wealth: "+NumberFormat.getNumberInstance(Locale.US).format(f2pWealth), "Member items: "+memberItems.size(), "Member wealth: "+NumberFormat.getNumberInstance(Locale.US).format(memberWealth), "Total wealth: "+NumberFormat.getNumberInstance(Locale.US).format(memberWealth+f2pWealth)};
	}

}
