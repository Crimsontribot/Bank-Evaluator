package scripts.fc.fcpaint;

import java.awt.Color;
import java.awt.Graphics;

import org.tribot.api.Timing;
import org.tribot.script.Script;

public class FCPaint
{
	private final int	PAINT_X		= 4; //The x coordinate for the paint text
	private final int	PAINT_BOT_Y	= 336; //The y coordinate for the paint string on the bottom
	private final int	PAINT_SPACE	= 15; //The space between paint fields		
	
	private FCPaintable paintable; //Paintable object we're painting (we get our paint info from this)
	private Script		script;	//Script we're painting for --> This is so we can call getRunningTime() from this class
	private Color		color;	//The color of the paint
	
	public FCPaint(FCPaintable paintable, Color color)
	{
		this.script = (Script)paintable;
		this.paintable = paintable;
		this.color = color;
	}
	
	public void paint(Graphics g)
	{
		//set paint text color
		g.setColor(color);
		
		String[] info = paintable.getPaintInfo();
		
		//FOR(each paint information field in paintInfo)
		for(int index = 0; index < info.length; index++)
		{
			//draw paint field at the appropriate position on the screen, as defined by constants
			g.drawString(info[index], PAINT_X, PAINT_BOT_Y - (PAINT_SPACE * (info.length - (index + 1))));
			
		} //END FOR
	}
		
	public String getTimeRan()
	{	
		//return the properly formatted string
		return Timing.msToString(script.getRunningTime());
		
	} //END getTimeRan()
	
	public long getPerHour(int amount)
	{	
		//return the projected amount per hour
		return Math.round(amount / (script.getRunningTime() / 3600000.0));
	}
}