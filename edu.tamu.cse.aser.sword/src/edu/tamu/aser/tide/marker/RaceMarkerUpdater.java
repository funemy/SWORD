package edu.tamu.aser.tide.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.IMarkerUpdater;

public class RaceMarkerUpdater implements IMarkerUpdater{

	@Override
	public String[] getAttribute() {
		return null;
	}

	@Override
	public String getMarkerType() {
		return "edu.umd.cs.findbugs.plugin.eclipse.findbugsMarkerScariest";
	}

	@Override
	public boolean updateMarker(IMarker marker, IDocument doc, Position line) {
		if(marker instanceof BugMarker){
			BugMarker bugMarker = (BugMarker) marker;
		}
		return true;
	}

}
