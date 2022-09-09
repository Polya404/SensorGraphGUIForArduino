import com.fazecast.jSerialComm.SerialPort;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.Scanner;

public class Run {
    static SerialPort chosenPort;

    public static void main(String[] args) {
        // create and configure the window
        JFrame window = new JFrame();
        window.setTitle("Sensor Graph GUI");
        window.setSize(600, 400);
        window.setLayout(new BorderLayout());
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // create a drop-down box and connect button, then place them at the top of the window
        JComboBox<String> portList = new JComboBox<>();
        JButton connectionButton = new JButton("Connect");
        JPanel topPanel = new JPanel();
        topPanel.add(portList);
        topPanel.add(connectionButton);
        window.add(topPanel, BorderLayout.NORTH);

        // populate the drop-down box
        SerialPort[] portNames = SerialPort.getCommPorts();
        for (SerialPort portName : portNames) {
            portList.addItem(portName.getSystemPortName());
        }

        // create the line graph
        XYSeries series = new XYSeries("Light Sensor Readings");
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart("Light Sensor Readings", " Time(seconds)", "ADC Reading", dataset, PlotOrientation.VERTICAL, true, true, true);
        window.add(new ChartPanel(chart), BorderLayout.CENTER);

        // configure the connect button ahd use another thread to listen for data
        connectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (connectionButton.getText().equals("Connect")) {
                    // attempt to connect to serial port
                    chosenPort = SerialPort.getCommPort(Objects.requireNonNull(portList.getSelectedItem()).toString());
                    chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
                    if (chosenPort.openPort()) {
                        connectionButton.setText("Disconnect");
                        portList.setEnabled(false);
                    }

                    // create a new thread that listens for incoming text and populates the graph
                    Thread thread = new Thread(() -> {
                        Scanner scanner = new Scanner(chosenPort.getInputStream());
                        int x = 0;
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            int number = Integer.parseInt(line);
                            series.add(x++, number);
                        }
                        scanner.close();
                    });
                    thread.start();
                } else {
                    // disconnect from the serial port
                    chosenPort.closePort();
                    portList.setEnabled(true);
                    connectionButton.setText("Connect");
                }
            }
        });

        // show the window
        window.setVisible(true);
    }
}
