package org.openstreetmap.josm;

import jssc.SerialPort;
import jssc.SerialPortException;
import org.openstreetmap.josm.gui.MainApplication;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SimulatedSerialPort extends SerialPort {

    private List<byte[]> imageBytes = new ArrayList<>();
    private int currIndex = 0;

    public SimulatedSerialPort(String serialPort) {
        super(serialPort);

        //Create a file chooser
        final JFileChooser fc = new JFileChooser();

        fc.setCurrentDirectory(new File(System.getProperty("user.home")));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        //In response to a button click:
        int result = fc.showOpenDialog(MainApplication.parent);

        if(result == JFileChooser.APPROVE_OPTION) {
            File[] files = fc.getSelectedFile().listFiles();
            for (File file : files) {
                try {
                    imageBytes.add(Files.readAllBytes(file.toPath()));
                } catch (IOException ioex) {
                }
            }
        }

    }

    @Override
    public byte[] readBytes() throws SerialPortException {
        if (currIndex < imageBytes.size()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException iex) {}
            return imageBytes.get(currIndex++);
        }
        else {
            return null;
        }
    }

    @Override
    public boolean openPort() throws SerialPortException {
        return true;
    }

    @Override
    public boolean setParams(int baudRate, int dataBits, int stopBits, int parity) throws SerialPortException {
        return true;
    }
}
