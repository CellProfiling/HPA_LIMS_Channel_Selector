package limsChannelSelector_jnh;

/** ===============================================================================
* HPA_Convert_OPERA_To_LIMS-OMETIF_JNH.java Version 0.0.1
* 
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*  
* See the GNU General Public License for more details.
*  
* Copyright (C) Jan Niklas Hansen
* Date: December 18, 2023 (This Version: December 18, 2023)
*   
* For any questions please feel free to contact me (jan.hansen@scilifelab.se).
* =============================================================================== */

import java.awt.Font;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.plugin.PlugIn;

public class LimsChannelSelector_Main implements PlugIn {
	// Name variables
	static final String PLUGINNAME = "HPA LIMS Channel Selector";
	static final String PLUGINVERSION = "0.0.1";

	// Fix fonts
	static final Font SuperHeadingFont = new Font("Sansserif", Font.BOLD, 16);
	static final Font HeadingFont = new Font("Sansserif", Font.BOLD, 14);
	static final Font SubHeadingFont = new Font("Sansserif", Font.BOLD, 12);
	static final Font TextFont = new Font("Sansserif", Font.PLAIN, 12);
	static final Font InstructionsFont = new Font("Sansserif", 2, 12);
	static final Font RoiFont = new Font("Sansserif", Font.PLAIN, 20);

	// Fix formats
	DecimalFormat dformat6 = new DecimalFormat("#0.000000");
	DecimalFormat dformat3 = new DecimalFormat("#0.000");
	DecimalFormat dformat0 = new DecimalFormat("#0");
	DecimalFormat dformatDialog = new DecimalFormat("#0.000000");

	static final String[] nrFormats = { "US (0.00...)", "Germany (0,00...)" };

	static SimpleDateFormat NameDateFormatter = new SimpleDateFormat("yyMMdd_HHmmss");
	static SimpleDateFormat FullDateFormatter = new SimpleDateFormat("yyyy-MM-dd	HH:mm:ss");
	static SimpleDateFormat FullDateFormatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	// Progress Dialog
	ProgressDialog progress;
	boolean processingDone = false;
	boolean continueProcessing = true;
	boolean deleteManually = false;

	// -----------------define params for Dialog-----------------
	int tasks = 1;
	boolean extendedLogging = false;
	boolean logInitialFileScreening = false;
	boolean logWholeOMEXMLComments = false;
		
	String outPath = "E:" + System.getProperty("file.separator") + System.getProperty("file.separator") + "Selected Channels"
			+ System.getProperty("file.separator");
	
	String inputType [] = new String [] {"Separate z planes into individual image folders (LIMS style)","Separate fields of view into individual folders (canonical OME tif style)"};
	String selectedInputType = inputType [0];
	
	int channelA = 4, channelB = 5;
	
	// -----------------define params for Dialog-----------------
	
	
	//TODO Remove unused variables
	// Temporary variables used for the original metadata file
	String loadedSrcMetadataFilePath = "";
	String srcMetadataFilePath = "";
	File srcMetaDataFile = null;
	Document srcMetaDoc = null;
	Node srcImagesNode = null, srcWellsNode = null, srcPlatesNode = null;
	double srcZStepSizeInMicronAcrossWholeOPERAFile = -1.0;
	String srcLoadingLog = "";
	int srcLoadingLogMode = ProgressDialog.LOG;
	
	
	// Temporary variables used for the individual OPERA metadata file
	String loadedMetadataFilePath = "";
	int loadedTask = -1;
	String metadataFilePath = "";
	File metaDataFile = null;
	Document metaDoc = null;
	Node imagesNode = null, wellsNode = null, platesNode = null;
	double zStepSizeInMicronAcrossWholeOPERAFile = -1.0;
	String loadingLog = "";
	int loadingLogMode = ProgressDialog.LOG;
	
	
	
	// For xpath loading
	XPathFactory xPathfactory = null;
	XPath xp = null;
	
	// Developer variables
	static final boolean LOGPOSITIONCONVERSIONFORDIAGNOSIS = false;// This fixed variable is just used when working on the code and to retrieve certain log output only
	static final boolean LOGZDISTFINDING = false;// This fixed variable is just used when working on the code and to retrieve certain log output only

	@Override
	public void run(String arg) {
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		// ---------------------------------INIT JOBS----------------------------------
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		
		dformat6.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		dformat3.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		dformat0.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		dformatDialog.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));

		String name[] = { "", "" };
		String dir[] = { "", "" };
		String fullPath[] = { "", "" };
		
		xPathfactory = XPathFactory.newInstance();
		xp = xPathfactory.newXPath();

		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		// --------------------------REQUEST USER-SETTINGS-----------------------------
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		
		GenericDialog gd = new GenericDialog(PLUGINNAME + " - set parameters");	
		//show Dialog-----------------------------------------------------------------
		gd.setInsets(0,0,0);		gd.addMessage(PLUGINNAME + ", Version " + PLUGINVERSION + ", \u00a9 2023 JN Hansen", SuperHeadingFont);	
		

		gd.setInsets(15,0,0);	gd.addMessage("Notes:", SubHeadingFont);
		
		gd.setInsets(0,0,0);		gd.addMessage("The plugin processes output folders for LIMS or Memento,");
		gd.setInsets(0,0,0);		gd.addMessage("created by the ImageJ plugin 'HPA Convert Opera-Tifs to LIMS-OME-Tif'.", InstructionsFont);

		gd.setInsets(10,0,0);	gd.addMessage("Input files", SubHeadingFont);
		gd.setInsets(0,0,0);		gd.addMessage("A dialog will be shown when you press OK that allows you to list Index.idx.xml files to be processed.", InstructionsFont);
		
		gd.setInsets(10,0,0);	gd.addMessage("Input Type:", SubHeadingFont);
		gd.setInsets(0,0,0);		gd.addChoice("Input style", inputType, selectedInputType);	
		
		gd.setInsets(10,0,0);	gd.addMessage("Channels from which the less oversaturated channel should be selected:", SubHeadingFont);
		gd.setInsets(0, 0, 0);	gd.addNumericField("Channel A",channelA,0);
		gd.setInsets(0, 0, 0);	gd.addNumericField("Channel A",channelB,0);

		gd.setInsets(0,0,0);		gd.addStringField("Filepath to output directory", outPath, 35);
		gd.setInsets(0,0,0);		gd.addMessage("This path defines where outputfiles will be stored.", InstructionsFont);
		gd.setInsets(0,0,0);		gd.addMessage("Make sure this path does not contain identically named files - the program may overwrite them.", InstructionsFont);

		gd.setInsets(10,0,0);	gd.addMessage("Logging settings (troubleshooting options)", SubHeadingFont);		
		gd.setInsets(0,0,0);		gd.addCheckbox("Log all processing steps extensively", extendedLogging);
		gd.setInsets(5,0,0);		gd.addCheckbox("Log initial screening original file", logInitialFileScreening);
		gd.setInsets(5,0,0);		gd.addCheckbox("Log the OME metadata XML before and after extending", logWholeOMEXMLComments);
		
		gd.addHelp("https://github.com/CellProfiling/HPA_Convert_OPERA_To_LIMS-OMETIF");
		
		gd.showDialog();
		//show Dialog-----------------------------------------------------------------

		//read and process variables--------------------------------------------------	
		selectedInputType = gd.getNextChoice();
		channelA = (int) Math.round(gd.getNextNumber());
		channelB = (int) Math.round(gd.getNextNumber());
		outPath = gd.getNextString();
		extendedLogging = gd.getNextBoolean();
		logInitialFileScreening = gd.getNextBoolean();
		logWholeOMEXMLComments = gd.getNextBoolean();
		//read and process variables--------------------------------------------------
		if (gd.wasCanceled()) return;
		
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		// -------------------------------LOAD FILES-----------------------------------
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&

		{
			/**
			 * Explore XML file to read information
			 */
			OpenFilesDialog od = new OpenFilesDialog(true);
			od.setLocation(0, 0);
			od.setVisible(true);

			od.addWindowListener(new java.awt.event.WindowAdapter() {
				public void windowClosing(WindowEvent winEvt) {
					return;
				}
			});

			// Waiting for od to be done
			while (od.done == false) {
				try {
					Thread.currentThread().sleep(50);
				} catch (Exception e) {
				}
			}
			
			// Explore the directory and find images to be converted
			tasks = od.filesToOpen.size();
			
			for (int task = 0; task < tasks; task++) {
				String[] fileList = od.filesToOpen.get(task).list();
//					IJ.log(od.filesToOpen.get(task).getName() + " has " + fileList.length + " subfiles/-folders!");
				for (int f = 0; f < fileList.length; f++) {
					if (fileList[f].equals("metadata")) {
						continue;
					}
					File fi = new File(od.filesToOpen.get(task).getAbsolutePath() + System.getProperty("file.separator") + fileList[f]);
					if(fi.isDirectory()) {
						od.filesToOpen.add(fi);
						tasks = od.filesToOpen.size();
//							IJ.log(fi.getAbsolutePath() + " was added to folder list! (#tasks " + tasks + ")");
					}else {
//							IJ.log(fi.getAbsolutePath() + " is no directory, skipped!");
					}
				}
			}
			
			String tempFile;
			boolean withMetaData = false, omeTifFilesPresent = false, invalidFiles = false;
			LinkedList<String> allFiles = new LinkedList<String>();
			scanning: for (int task = 0; task < tasks; task++) {
				// Get all files in the folder
				String[] fileList = od.filesToOpen.get(task).list();

				/**
				 * Now, the script scans through all file names in the folder and verifies if
				 * they are tif files and if so it checks whether they are named correctly and have the correct file ending.
				 * _Z00_C00.ome.tif
				 */
				withMetaData = false;
				omeTifFilesPresent = false;
				invalidFiles = false;
				for (int f = 0; f < fileList.length; f++) {
					if (fileList[f].equals("metadata")) {
						if(new File(od.filesToOpen.get(task).getAbsolutePath() + System.getProperty("file.separator") + fileList[f] 
								+ System.getProperty("file.separator") + "image.ome.xml").exists()) {
							withMetaData = true;
							break;
						}else {
							IJ.log(od.filesToOpen.get(task).getAbsolutePath() + System.getProperty("file.separator") 
								+ fileList[f] + " was skipped since metadata xml file was lacking!");									
						}
					}else if (fileList[f].endsWith(".ome.tif")) {
						if (fileList[f].contains("_Z")) {
							omeTifFilesPresent = true;
						}else {
							if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
								IJ.log("" 
										+ fileList[f]
										+ " did not contain _Z and thus it will be skipped!");
							}
							invalidFiles = true;
							continue;
						}
					}else {
						if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
							IJ.log("" 
									+ fileList[f]
									+ " is neither a metadata folder nor an .ome.tif file and thus it will be skipped!");
						}
						invalidFiles = true;						
						continue;
					}
					
				}
				if (withMetaData == false) {
					if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
						IJ.log(od.filesToOpen.get(task).getName() + " was skipped since missing MetaData folder");
						continue scanning;
					}
				}
				if (omeTifFilesPresent == false) {
					if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
						IJ.log(od.filesToOpen.get(task).getName() + " was skipped since no tif files present");
						continue scanning;
					}
				}
				if (invalidFiles) {					
						IJ.log("WARNING: " + od.filesToOpen.get(task).getName() + " contained files not matching file name requirements.");
				}

				/**
				 * Here it is checked whether an identically named file is already in the list
				 * (otherwise would load each file again and again...
				 */
				tempFile = od.filesToOpen.get(task).getAbsolutePath();
				if(selectedInputType == inputType[0]) {
					tempFile = tempFile.substring(0,tempFile.lastIndexOf("_Z"));
				}
				
				for (int ff = 0; ff < allFiles.size(); ff++) {
					if (allFiles.get(ff).equals(tempFile)) {
						continue scanning;
					}
				}			

				// Copy new files to all files list
				allFiles.add(tempFile);

				if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
					IJ.log("ACCEPTED: " + tempFile);
				}
			}
			
			// Generate arrays based on unique names
			tasks = allFiles.size();
			name = new String[tasks];
			dir = new String[tasks];
			fullPath = new String[tasks];
			for (int task = 0; task < allFiles.size(); task++) {
				tempFile = allFiles.get(task);
				fullPath[task] = tempFile;
				name[task] = tempFile.substring(tempFile.lastIndexOf(System.getProperty("file.separator")) + 1);
				dir[task] = tempFile;

				if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
					IJ.log("FULL PATH: " + fullPath[task]);
					IJ.log("name:" + name[task]);
					IJ.log("dir:" + dir[task]);	
				}
			}
			allFiles.clear();
			allFiles = null;
						
			if (tasks == 0) {
				new WaitForUserDialog("No folders selected!").show();
				return;
			}		
		}
		
		if(logInitialFileScreening) {
			IJ.log("Task list has been finished, starting processing now."  + " Time: " + FullDateFormatter2.format(new Date()));
		}
		
		// add progressDialog
		progress = new ProgressDialog(name);
		progress.setLocation(0, 0);
		progress.setVisible(true);
		progress.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent winEvt) {
				if (processingDone == false) {
					IJ.error("Script stopped...");
				}
				continueProcessing = false;
				return;
			}
		});

		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
		// -----------------------------PROCESS TASKS----------------------------------
		// &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
				
		String tmpMsg = "";
		for (int task = 0; task < tasks; task++) {
			running: while (continueProcessing) {
				// Check how many planes there are by checking how many folders with the name are in the folder
				int nrOfPlanes = 0;
				{
					String[] fileList = new File(fullPath[task].substring(0, fullPath[task].lastIndexOf(name[task])-1)).list();

					
					for (int f = 0; f < fileList.length; f++) {
						if (fileList[f].endsWith(name[task])) {
							nrOfPlanes = 1;
							break;
						}else if (fileList[f].contains(name[task] + "_Z")){
							nrOfPlanes ++;
						}
					}

					tmpMsg = "Found " + nrOfPlanes + " planes for file " + name[task] + "!";
					progress.updateBarText(tmpMsg);
					if(extendedLogging)	progress.notifyMessage("Task " + (task+1) + ": " + tmpMsg, ProgressDialog.LOG);
				}
								
				// Open the channels to be analyzed
				ImagePlus impChannelA = openChannel(fullPath [task], name[task], nrOfPlanes, (channelA-1), task);
				ImagePlus impChannelB = openChannel(fullPath [task], name[task], nrOfPlanes, (channelB-1), task);
				
				impChannelA.show();
				new WaitForUserDialog("Check A").show();
				impChannelA.hide();
				impChannelB.show();
				new WaitForUserDialog("Check B").show();
				impChannelB.hide();
				
				double [] valuesA = getPercentileValuesBySections (impChannelA, 5, 99.995);
				double [] valuesB = getPercentileValuesBySections (impChannelB, 5, 99.995);
				
				// TODO Develop processing of files
				
				/**
				 * Finish
				 */
				processingDone = true;
				progress.updateBarText("processing finished!");
				progress.setBar(0.9);
				break running;
			}
			progress.moveTask(task);
			System.gc();	
		}
	}

	/**
	 * @param filePathPrefix
	 * @param name
	 * @param nrOfPlanes
	 * @param channel: 0 <= channel < nrOfChannels
	 * @param task
	 */	
	private ImagePlus openChannel(String filePathPrefix, String name, int nrOfPlanes, int channel, int task) {
		ImagePlus imp = null;
		String channelString = "0", planeString = "0", tempString = "";
		if(channel > 9) {
			channelString = channel + "";
		}else {
			channelString += channel + "";
		}
		
		if(nrOfPlanes == 1) {
			tempString = filePathPrefix + "" + System.getProperty("file.separator") + name + "_Z00_C" + channelString + ".ome.tif";
			
			String tmpMsg = "Opening " + name + " (" +  tempString + ")...";
			progress.updateBarText(tmpMsg);
			if(extendedLogging)	progress.notifyMessage("Task " + (task+1) + ": " + tmpMsg, ProgressDialog.LOG);
			
			imp = IJ.openImage(tempString);
		}else {
			ImageStack stackOut = new ImageStack(10,10);	//Random values for initialization because intialized later	
			int width = 10, height = 10;
			int bits = 8;
			
			for(int z = 0; z < nrOfPlanes; z++) {
				planeString = "0";
				if(z > 9) {
					planeString = "" + z;
				}else {
					planeString += "" + z;
				}

				tempString = filePathPrefix + "_Z" + planeString + "" + System.getProperty("file.separator") + name + "_Z" + planeString + "_C" + channelString + ".ome.tif";
				
				String tmpMsg = "Opening " + name + "_Z" + planeString + "_C" + channelString + ".ome.tif" + " (" +  tempString + ")...";
				progress.updateBarText(tmpMsg);
				if(extendedLogging)	progress.notifyMessage("Task " + (task+1) + ": " + tmpMsg, ProgressDialog.LOG);
				
				imp = IJ.openImage(tempString);
				
				if(z == 0){
					width = imp.getWidth();
					height = imp.getHeight();
					bits = imp.getBitDepth();
					stackOut = new ImageStack(width,height);
				}
				stackOut.addSlice(imp.getProcessor());
			}
			
			imp = IJ.createImage("Channel " + (channel+1), width, height, imp.getNSlices(), bits);
			imp.setStack(stackOut);
		}
		
		return imp;
	}
	
	private double [] getPercentileValuesBySections (ImagePlus imp, int sectionFactor, double percentile) {
		int width = imp.getWidth();
		int height = imp.getHeight();
		
		double outValues [] = new double [sectionFactor * sectionFactor];
		Arrays.fill(outValues, 0.0);
		
		ArrayList <ArrayList <Double>> sectionLists = new ArrayList <ArrayList <Double>>(sectionFactor * sectionFactor);
		for(int i = 0; i < sectionFactor * sectionFactor; i++) {
			sectionLists.add(new ArrayList <Double> (((int)(width/(double)sectionFactor)+1)*((int)(height/(double)sectionFactor)+1)));
		}
		
		int coord, xCoord, yCoord;
		
		for(int z = 0; z < imp.getStackSize(); z++) {
			for(int x = 0; x < width; x ++) {
				for(int y = 0; y < height; y ++) {
					xCoord = (int) ((double) x / (double) width * (double) sectionFactor);
					yCoord = (int) ((double) y / (double) height * (double) sectionFactor);
					coord = yCoord * sectionFactor + xCoord;
					
					sectionLists.get(coord).add(imp.getStack().getVoxel(x, y, z));
				}
			}
		}
		
		int index;
		for(int i = 0; i < sectionFactor * sectionFactor; i++) {
			Collections.sort(sectionLists.get(i));
//			if(extendedLogging) {
//				IJ.log("Values for section" + (i+1));
//				for(int j = 0; j < sectionLists.get(i).size(); j++) {
//					IJ.log("Value " + j + ":	" + sectionLists.get(i).get(j));					
//				}
//			}
			
			index = (int)Math.round(sectionLists.get(i).size()*(percentile/100.0))-1;
			outValues [i] = sectionLists.get(i).get(index);
			if(extendedLogging) progress.notifyMessage("For section " + (i+1) + " the " + percentile + " value was "
					+ outValues [i]
					+ ", determined at index " + index + " for a list of size " + sectionLists.get(i).size(),
					ProgressDialog.LOG);
			if(extendedLogging) progress.notifyMessage("For section " + (i+1) + " max, min, last, first were " 
					+ Collections.max(sectionLists.get(i))
					+ ", "
					+ Collections.min(sectionLists.get(i))
					+ ", "
					+ sectionLists.get(i).get(sectionLists.get(i).size()-1)
					+ ", "
					+ sectionLists.get(i).get(0)
					+".",
					ProgressDialog.LOG);
		}
		
		return outValues;
	}
}// end main class