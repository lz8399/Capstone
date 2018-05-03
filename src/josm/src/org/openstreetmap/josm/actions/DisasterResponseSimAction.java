package org.openstreetmap.josm.actions;

import org.openstreetmap.josm.DisasterResponse;
import org.openstreetmap.josm.tools.Shortcut;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

public class DisasterResponseSimAction extends JosmAction {

    /**
     * Constructs a new {@code CreateCircleAction}.
     */
    public DisasterResponseSimAction() {
        super(tr("Run Disaster Response Simulation"), "aligncircle", tr("Run simulation of Disaster Response software."),
                Shortcut.registerShortcut("tools:createcircle", tr("Tool: {0}", tr("Create Circle")),
                        KeyEvent.VK_O, Shortcut.SHIFT), true, "createcircle", true);
        putValue("help", ht("/Action/CreateCircle"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        // monitor serial port for incoming images and save them to disk
        DisasterResponse.simulatedDisasterResponse();
    }
}
