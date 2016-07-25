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

public class priceGrabber extends Script implements FCPaintable, Painting {

	ArrayList<RSItem> itemList = new ArrayList<RSItem>();
	ArrayList<RSItem> memberItems = new ArrayList<RSItem>();
	ArrayList<RSItem> f2pItems = new ArrayList<RSItem>();
	private boolean output = true;
	private boolean alive = true;
	private boolean filterStarted = false;
	private boolean filterDone = false;
	private boolean hasMessaged = false;
 	final FCPaint PAINT = new FCPaint(this, Color.GREEN);
 	private int originalCount = 0;
	private int totalf2p = 0;
	private int totalmembers = 0;

	// private RSItem[] b;

	@Override
	public void run() {
		while (alive) {
			sleep();
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
		while (!alive) {
			sleep();
			if (!hasMessaged) {
				System.out.println("You've reached the end of the script.");
				hasMessaged = true;
			}
		}
	}

	private void checkIngame() {
		while (Login.getLoginState() != Login.STATE.INGAME) {
			sleep(General.random(1000, 3000));
			System.out.println("Waiting to be logged in...");
		}
	}

	private void sleep() {
		General.sleep(300, 600);
	}

	private void handleList() {
		RSItem[] currentBank = Banking.getAll();
		if (currentBank.length == 0) {
			System.out.println("Your bank seems to be empty. Stopping.");
			alive = false;
		}
		try {
			for (RSItem q : currentBank) {
				itemList.add(q);
				System.out.println("Loaded: " + RSItemDefinition.get(q.getID()).getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void priceCheck() {
		originalCount = Banking.getAll().length;
		if (itemList.isEmpty()) {
			handleList();
		}
		if (!itemList.isEmpty() && !filterStarted) {
			filterList();
		}
		if (filterDone) {
			makeJson();
		}
	}

	private void filterList() {
		filterStarted = true;
		try {
			for (RSItem item : itemList) {
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

	public static final Pattern OVERALL_PATTERN = Pattern.compile("(?:\"overall\":)([0-9]+)");
	
	public static int osbuddy(int id) throws MalformedURLException, IOException {
		String LOOKUP_STRING_FORMAT = "http://api.rsbuddy.com/grandExchange?a=guidePrice&i="+id;
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new URL(String.format(LOOKUP_STRING_FORMAT, id)).openConnection().getInputStream()));
		String line;while((line=reader.readLine())!=null)
	
		{
			Matcher matcher = OVERALL_PATTERN.matcher(line);
			if (matcher.find() && matcher.groupCount() > 0) {
				int price = Integer.parseInt(matcher.group(1));
				//prices.put(id, price);
				if (price == 0) {
					price = 1;
				}
				return price;
			}
		}
		return 1;
	}

	private void makeJson() {
		if (!output) {
			System.out.println("Skipping output generation.");
		} else {
			
			try {
				File dir = new File(Util.getWorkingDirectory()+"/banks/");
				File file = new File(dir.getAbsolutePath()+"/"+Player.getRSPlayer().getName()+"-"+System.currentTimeMillis()+".txt");
				
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
					int price = osbuddy(item.getID());
					bw.write(item.getStack()+" x "+RSItemDefinition.get(item.getID()).getName()+" @ "+NumberFormat.getNumberInstance(Locale.US).format(price)+" each = "+NumberFormat.getNumberInstance(Locale.US).format(price*item.getStack()));
					totalf2p += (price*item.getStack());
					bw.newLine();
				}
				
				bw.newLine();
				bw.write("#Total F2P: "+NumberFormat.getNumberInstance(Locale.US).format(totalf2p));
				bw.newLine();
				bw.newLine();
				bw.write("#Members");
				bw.newLine();
				
				for (RSItem item : memberItems) {
					int price = osbuddy(item.getID());
					bw.write(item.getStack()+" x "+RSItemDefinition.get(item.getID()).getName()+" @ "+NumberFormat.getNumberInstance(Locale.US).format(price)+" each = "+NumberFormat.getNumberInstance(Locale.US).format(price*item.getStack()));
					totalmembers += (price*item.getStack());
					bw.newLine();
				}
				
				bw.newLine();
				bw.write("#Total Members: "+NumberFormat.getNumberInstance(Locale.US).format(totalmembers));
				bw.newLine();
				bw.newLine();
				bw.write("F2P Total: "+NumberFormat.getNumberInstance(Locale.US).format(totalf2p) + ", Member Total: "+NumberFormat.getNumberInstance(Locale.US).format(totalmembers) + ", overall: "+NumberFormat.getNumberInstance(Locale.US).format(totalmembers+totalf2p));
				
				bw.close();
				
				System.out.println("F2P Total: "+NumberFormat.getNumberInstance(Locale.US).format(totalf2p) + ", Member Total: "+NumberFormat.getNumberInstance(Locale.US).format(totalmembers) + ", overall: "+NumberFormat.getNumberInstance(Locale.US).format(totalmembers+totalf2p));
				
				alive=false;
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onPaint(Graphics g) {
		PAINT.paint(g);
	}
	
	@Override
	public String[] getPaintInfo() {
		return new String[] {"Bank size: "+originalCount, "F2P items: "+f2pItems.size(), "F2P wealth: "+NumberFormat.getNumberInstance(Locale.US).format(totalf2p), "Member items: "+memberItems.size(), "Member wealth: "+NumberFormat.getNumberInstance(Locale.US).format(totalmembers), "Total wealth: "+NumberFormat.getNumberInstance(Locale.US).format(totalmembers+totalf2p), "Script finished? "+hasMessaged};
	}

}
