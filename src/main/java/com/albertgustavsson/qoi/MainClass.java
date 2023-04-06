package com.albertgustavsson.qoi;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainClass {
	private final static Logger logger = LogManager.getLogger(MainClass.class);
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			logger.error("Required parameters: <filename>");
		}
		String filename = args[0];

		QOIDecoder decoder = new QOIDecoder();
		BufferedImage image = decoder.getImage(filename);

		logger.debug("Image has been decoded.");

		ImageIcon icon = new ImageIcon(image);
		JFrame frame = new JFrame();
		frame.setLayout(new FlowLayout());
		frame.setSize(image.getWidth() + 50, image.getHeight() + 50);
		JLabel lbl = new JLabel();
		lbl.setIcon(icon);
		frame.add(lbl);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
