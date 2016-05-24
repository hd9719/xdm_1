package org.sdg.xdman.gui;
import java.util.ArrayList;
public interface YoutubeListener
 {
	public void parsingComplete(ArrayList<String> list);

	public void parsingFailed();
}
