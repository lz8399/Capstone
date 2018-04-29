package org.openstreetmap.josm;

import jssc.SerialPort;
import jssc.SerialPortException;
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

        for (File file : new File("C:\\Users\\Kevin\\git\\Capstone\\simulated_serialport_data").listFiles())
        {
            try {
                imageBytes.add(Files.readAllBytes(file.toPath()));
            } catch (IOException ioex) {}
        }

    }

    @Override
    public byte[] readBytes() throws SerialPortException {
        if (currIndex < imageBytes.size()) {
            try {
                Thread.sleep(3000);
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
