package limsChannelSelector_jnh;

/** ===============================================================================
* HPA_LIMS_Channel_Selector_JNH.java Version 0.0.1
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.plugin.PlugIn;
import loci.common.RandomAccessInputStream;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.services.OMEXMLService;
import loci.formats.tiff.TiffParser;
import loci.formats.tiff.TiffSaver;

public class LimsChannelSelector_Main implements PlugIn {
	// Name variables
	static final String PLUGINNAME = "HPA LIMS Channel Selector";
	static final String PLUGINVERSION = "0.0.1";
	static final String PLUGINURL = "https://github.com/CellProfiling/HPA_LIMS_Channel_Selector/";

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
	boolean logIfAChannelWasOversat = true;
	
	
	String outPath = "E:" + System.getProperty("file.separator") + System.getProperty("file.separator") + "Selected Channels"
			+ System.getProperty("file.separator");
	
	String inputType [] = new String [] {"Separate z planes into individual image folders (LIMS style)","Separate fields of view into individual folders (canonical OME tif style)"};
	String selectedInputType = inputType [0];
	
	int channelA = 4, channelB = 5;
	
	String decisionType [] = new String [] {"Decide by well","Decide by image"};
	String selectedDecisionType = decisionType [0];
	
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
	int factorization = 3;
	double percentile = 99.9995;
	
	
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

		gd.setInsets(10,0,0);	gd.addMessage("Decision Type:", SubHeadingFont);
		gd.setInsets(0,0,0);		gd.addChoice("Decision style:", decisionType, selectedDecisionType);	
				
		gd.setInsets(0,0,0);		gd.addStringField("Filepath to output directory", outPath, 35);
		gd.setInsets(0,0,0);		gd.addMessage("This path defines where outputfiles will be stored.", InstructionsFont);
		gd.setInsets(0,0,0);		gd.addMessage("Make sure this path does not contain identically named files - the program may overwrite them.", InstructionsFont);

		gd.setInsets(10,0,0);	gd.addMessage("Logging settings (troubleshooting options)", SubHeadingFont);		
		gd.setInsets(0,0,0);		gd.addCheckbox("Log all processing steps extensively", extendedLogging);
		gd.setInsets(5,0,0);		gd.addCheckbox("Log initial screening original file", logInitialFileScreening);
		gd.setInsets(5,0,0);		gd.addCheckbox("Log the OME metadata XML before and after extending", logWholeOMEXMLComments);
		gd.setInsets(5,0,0);		gd.addCheckbox("Log if a channel was oversaturated", logIfAChannelWasOversat);
		
		gd.addHelp(PLUGINURL);
		
		gd.showDialog();
		//show Dialog-----------------------------------------------------------------

		//read and process variables--------------------------------------------------	
		selectedInputType = gd.getNextChoice();
		channelA = (int) Math.round(gd.getNextNumber());
		channelB = (int) Math.round(gd.getNextNumber());
		selectedDecisionType = gd.getNextChoice();
		outPath = gd.getNextString();
		extendedLogging = gd.getNextBoolean();
		logInitialFileScreening = gd.getNextBoolean();
		logWholeOMEXMLComments = gd.getNextBoolean();
		logIfAChannelWasOversat = gd.getNextBoolean();
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
					}
					continue scanning;
				}
				if (omeTifFilesPresent == false) {
					if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
						IJ.log(od.filesToOpen.get(task).getName() + " was skipped since no tif files present");
					}
					continue scanning;
				}
				if (invalidFiles) {					
						IJ.log("WARNING: " + od.filesToOpen.get(task).getName() + " contained files not matching file name requirements.");
				}

				/**
				 * Here it is checked whether an identically named file is already in the list
				 * (otherwise would load each file again and again...
				 */
				tempFile = od.filesToOpen.get(task).getAbsolutePath();
				if(selectedInputType == inputType[0] && tempFile.contains("_Z")) {
					tempFile = tempFile.substring(0,tempFile.lastIndexOf("_Z"));
				}
				
				if(selectedDecisionType == decisionType[0]) {
					//Decide by well
					tempFile = tempFile.substring(0,tempFile.lastIndexOf(System.getProperty("file.separator")));
					
					for (int ff = 0; ff < allFiles.size(); ff++) {
						if (allFiles.get(ff).equals(tempFile)) {
							continue scanning;
						}
					}			

					// Copy new files to all files list
					allFiles.add(tempFile);
				}else if(selectedDecisionType == decisionType[1]) {
					//Decide by image
					for (int ff = 0; ff < allFiles.size(); ff++) {
						if (allFiles.get(ff).equals(tempFile)) {
							continue scanning;
						}
					}			

					// Copy new files to all files list
					allFiles.add(tempFile);
				}
				
				if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
					IJ.log("ACCEPTED: " + tempFile);
				}
			}
			
			// Generate arrays based on unique names
			tasks = allFiles.size();
			name = new String[tasks];
			fullPath = new String[tasks];
			for (int task = 0; task < allFiles.size(); task++) {
				tempFile = allFiles.get(task);
				fullPath[task] = tempFile;
				name[task] = tempFile.substring(tempFile.lastIndexOf(System.getProperty("file.separator")) + 1);
				if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
					IJ.log("FULL PATH: " + fullPath[task]);
					IJ.log("Name:" + name[task]);
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
		int subTasks = 1;
		String subTasksPath [] = new String [0];
		String subTasksName [] = new String [0];
//		double allValuesA [][], allValuesB [][];
		boolean overSatA [], overSatB [], chooseA [];
				
		for (int task = 0; task < tasks; task++) {
			running: while (continueProcessing) {
				/**
				 * Create a list of the tasks ("subTasks") that we need to process and decide on together
				 */				
				subTasksPath = new String [0];
				subTasksName = new String [0];
				
				if(selectedDecisionType == decisionType[0]) {
					//Decide by well
					subTasksPath = getImageFolderPathsInDirectory(new File(fullPath[task]));
					subTasksName = new String [subTasksPath.length];
					for(int p = 0; p < subTasksPath.length; p++) {
						subTasksName [p] = subTasksPath [p].substring(subTasksPath [p].lastIndexOf(System.getProperty("file.separator")) + 1);
					}
				}else if(selectedDecisionType == decisionType[1]) {
					//Decide by image
					subTasksPath = new String [] {fullPath[task]};
					subTasksName = new String [] {name[task]};
					
				}
				
				/**
				 * Iterate over the subTasks to determine the percentile values in the different regions of each image
				 */
				subTasks = subTasksPath.length;
//				allValuesA = new double [subTasks][factorization*factorization];
//				allValuesB = new double [subTasks][factorization*factorization];	
				overSatA = new boolean [subTasks];
				overSatB = new boolean [subTasks];
				chooseA = new boolean [subTasks];
				int chosenImagesA = 0, overSatCtA, overSatCtB;
				
				for (int subTask = 0; subTask < subTasks; subTask++) {
					// Check how many planes there are by checking how many folders with the name are in the folder
					int nrOfPlanes = 0;
					{
						String[] fileList = new File(subTasksPath[subTask].substring(0, subTasksPath[subTask].lastIndexOf(subTasksName[subTask])-1)).list();
						
						for (int f = 0; f < fileList.length; f++) {
							if (fileList[f].endsWith(subTasksName[subTask])) {
								nrOfPlanes = 1;
								break;
							}else if (fileList[f].contains(subTasksName[subTask] + "_Z")){
								nrOfPlanes ++;
							}
						}

						tmpMsg = "Found " + nrOfPlanes + " planes for file " + subTasksName[subTask] + " (subTask " + subTask + " / " + subTasks + ")!";
						progress.updateBarText(tmpMsg);
						if(extendedLogging) {
							progress.notifyMessage("subTasks " + (subTask+1) + " in task " + (task+1) + ": " + tmpMsg, ProgressDialog.LOG);
						}
					}
									
					// Open the channels to be analysed
					ImagePlus impChannelA = openChannel(subTasksPath [subTask], subTasksName[subTask], nrOfPlanes, (channelA-1), subTask);
					ImagePlus impChannelB = openChannel(subTasksPath [subTask], subTasksName[subTask], nrOfPlanes, (channelB-1), subTask);
					
					if(impChannelA == null) {
						tmpMsg = "Could not open channel A images for " + subTasksName[subTask] + ". Need to skip task " + task + "!";
						progress.notifyMessageAndDisplayInBar("subTask " + (subTask+1) + " in task " + (task+1) + ": " + tmpMsg, ProgressDialog.ERROR);
						break running;
					}
					
					if(impChannelB == null) {
						tmpMsg = "Could not open channel B images for " + subTasksName[subTask] + ". Need to skip task " + task + "!";
						progress.notifyMessageAndDisplayInBar("subTask " + (subTask+1) + " in task " + (task+1) + ": " + tmpMsg, ProgressDialog.ERROR);
						break running;
					}
					
//					impChannelA.show();
//					new WaitForUserDialog("Check A").show();
//					impChannelA.hide();
//					
//					impChannelB.show();
//					new WaitForUserDialog("Check B").show();
//					impChannelB.hide();
					
					double [] valuesA = getPercentileValuesBySections (impChannelA, factorization, percentile);
					impChannelA.changes = false;
					impChannelA.close();
					
					double [] valuesB = getPercentileValuesBySections (impChannelB, factorization, percentile);
					impChannelB.changes = false;
					impChannelB.close();
					
					// Get median values from the different image regions and check for oversaturation
					double valueA = getMedian(valuesA);					
					double valueB = getMedian(valuesB);
					
					if(valueA >= Math.pow(2.0, impChannelA.getBitDepth())-1) {
						overSatA [subTask] = true;
					}else {
						overSatA [subTask] = false;
					}
					
					if(valueB >= Math.pow(2.0, impChannelB.getBitDepth())-1) {
						overSatB [subTask] = true;
					}else {
						overSatB [subTask] = false;
					}
					
					//Decide based on which channel is brighter and not oversaturated
					if(overSatA [subTask] && overSatB [subTask]) {
						if(getMin(valuesA) < getMin(valuesB)) {
							chooseA[subTask] = true;
						}else {
							chooseA[subTask] = false;
						}
					}else if(valueA > valueB) {
						chooseA [subTask] = true;
						if(overSatA [subTask]){
							chooseA [subTask] = false;
						}
					}else {						
						chooseA [subTask] = false;
						if(overSatB [subTask]){
							chooseA [subTask] = true;
						}
					}
					
					tmpMsg = "Decision: Selected channel ";
					if(chooseA [subTask]){
						tmpMsg += "A";
					}else{
						tmpMsg += "B";
					}
					tmpMsg += ". Reasons: A Oversat.?:" + overSatA [subTask] + "."
							+ " B Oversat.?:" + overSatB [subTask] + "."
							+ " Value A: " + valueA + "."
							+ " Value B: " + valueB + ".";
					progress.updateBarText(tmpMsg);
					if(extendedLogging || (logIfAChannelWasOversat && (overSatA [subTask] || overSatB [subTask]))) {
						progress.notifyMessageAndDisplayInBar("subTasks " + (subTask+1) + " in task " + (task+1) + ": " + tmpMsg, ProgressDialog.LOG);
					}

					progress.addToBar(0.9/subTasks/2.0);
				}
				
				chosenImagesA = 0;
				overSatCtA = 0;
				overSatCtB = 0;
				for (int subTask = 0; subTask < subTasks; subTask++) {
					if(chooseA [subTask]) {
						chosenImagesA++;
					}
					if(overSatA [subTask]) {
						overSatCtA++;
					}
					if(overSatB [subTask]) {
						overSatCtB++;
					}
				}
				
				/**
				 * Check which channel is preferred for 50% of the images
				 */
				if(chosenImagesA >= subTasks/2.0) {
					tmpMsg = "Channel A (ID " + channelA + ") was selected because "
							+ chosenImagesA + " of " + subTasks + " images were better in this channel (Found "
									+ overSatCtA + " oversaturated images in A and "
									+ overSatCtB + " oversaturated images in B).";
					if(extendedLogging || (logIfAChannelWasOversat && (overSatCtA > 0 || overSatCtB > 0))) {
						progress.notifyMessageAndDisplayInBar("Task " + (task+1) + ": " + tmpMsg, ProgressDialog.LOG);
					}
					
					moveAndRemoveChannel(subTasksPath, subTasksName, outPath, channelB);
				}else {
					tmpMsg = "Channel B (ID " + channelB + ") was selected because "
							+ (subTasks-chosenImagesA) + " of " + subTasks + " images were better in this channel (Found "
									+ overSatCtA + " oversaturated images in A and "
									+ overSatCtB + " oversaturated images in B).";
					if(extendedLogging || (logIfAChannelWasOversat && (overSatCtA > 0 || overSatCtB > 0))) {
						progress.notifyMessageAndDisplayInBar("Task " + (task+1) + ": " + tmpMsg, ProgressDialog.LOG);
					}

					moveAndRemoveChannel(subTasksPath, subTasksName, outPath, channelA);				
				}
				
				/**
				 * Finish
				 */
				break running;
			}
			processingDone = true;
			progress.updateBarText("processing finished!");
			progress.setBar(1.0);
			
			progress.moveTask(task);
			System.gc();	
		}
	}

	/**
	 * @param filePathPrefix
	 * @param name
	 * @param nrOfPlanes
	 * @param channel: 0 <= channel < nrOfChannels
	 * @param subTask
	 */	
	private ImagePlus openChannel(String filePathPrefix, String name, int nrOfPlanes, int channel, int subTask) {
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
			if(extendedLogging) {
				progress.notifyMessage("subTask " + (subTask+1) + ": " + tmpMsg, ProgressDialog.LOG);
			}
			
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
				if(extendedLogging) {
					progress.notifyMessage("subTask " + (subTask+1) + ": " + tmpMsg, ProgressDialog.LOG);
				}
				
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
	
	private double [] getPercentileValuesBySections (ImagePlus imp, int sectionFactor, double chosenPercentile) {
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
			
			index = (int)Math.round(sectionLists.get(i).size()*(chosenPercentile/100.0))-1;
			outValues [i] = sectionLists.get(i).get(index);
			if(extendedLogging) progress.notifyMessage("For section " + (i+1) + " the " + chosenPercentile + " value was "
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
	
	private String [] getImageFolderPathsInDirectory (File dir){
		LinkedList<File> taskFiles = new LinkedList<File>();
		taskFiles.add(dir);
		String tmpMsg;
		
		tmpMsg = ("Now exploring " + dir.getAbsolutePath().substring(dir.getAbsolutePath().lastIndexOf(System.getProperty("file.separator"))) + " to find matching files...");
		progress.updateBarText(tmpMsg);
		if(extendedLogging) {
			progress.notifyMessage(tmpMsg, ProgressDialog.LOG);
		}
		
		// Explore the directory and find images to be converted
		int subTasks = taskFiles.size();
					
		for (int task = 0; task < subTasks; task++) {
			String[] fileList = taskFiles.get(task).list();
			for (int f = 0; f < fileList.length; f++) {
				if (fileList[f].equals("metadata")) {
					continue;
				}
				File fi = new File(taskFiles.get(task).getAbsolutePath() + System.getProperty("file.separator") + fileList[f]);
				if(fi.isDirectory()) {
					taskFiles.add(fi);
					subTasks = taskFiles.size();
				}
			}
		}
					
		String tempFile;
		boolean withMetaData = false, omeTifFilesPresent = false, invalidFiles = false;
		LinkedList<String> allFiles = new LinkedList<String>();
		scanning: for (int task = 0; task < subTasks; task++) {
			// Get all files in the folder
			String[] fileList = taskFiles.get(task).list();

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
					if(new File(taskFiles.get(task).getAbsolutePath() + System.getProperty("file.separator") + fileList[f] 
							+ System.getProperty("file.separator") + "image.ome.xml").exists()) {
						withMetaData = true;
						break;
					}else {
						tmpMsg = (taskFiles.get(task).getAbsolutePath() + System.getProperty("file.separator") 
							+ fileList[f] + " was skipped since metadata xml file was lacking!");
						progress.updateBarText(tmpMsg);
						if(extendedLogging) {
							progress.notifyMessage(tmpMsg, ProgressDialog.LOG);
						}
					}
				}else if (fileList[f].endsWith(".ome.tif")) {
					if (fileList[f].contains("_Z")) {
						omeTifFilesPresent = true;
					}else {
						if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
							tmpMsg = ("" 
									+ fileList[f]
									+ " did not contain _Z and thus it will be skipped!");
							progress.updateBarText(tmpMsg);
							progress.notifyMessage(tmpMsg, ProgressDialog.LOG);
						}
						invalidFiles = true;
						continue;
					}
				}else {
					if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
						tmpMsg = ("" 
								+ fileList[f]
								+ " is neither a metadata folder nor an .ome.tif file and thus it will be skipped!");
						progress.updateBarText(tmpMsg);
						progress.notifyMessage(tmpMsg, ProgressDialog.LOG);
					}
					invalidFiles = true;						
					continue;
				}
				
			}
			if (withMetaData == false) {
				if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
					tmpMsg = (taskFiles.get(task).getName() + " was skipped since missing MetaData folder");
					progress.updateBarText(tmpMsg);
					progress.notifyMessage(tmpMsg, ProgressDialog.LOG);
				}
				continue scanning;
			}
			if (omeTifFilesPresent == false) {
				if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
					tmpMsg = (taskFiles.get(task).getName() + " was skipped since no tif files present");
					progress.updateBarText(tmpMsg);					
					progress.notifyMessage(tmpMsg, ProgressDialog.LOG);
				}
				continue scanning;
			}
			if (invalidFiles) {					
					tmpMsg = ("WARNING: " + taskFiles.get(task).getName() + " contained files not matching file name requirements.");
					progress.updateBarText(tmpMsg);
					progress.notifyMessage(tmpMsg, ProgressDialog.LOG);
			}

			/**
			 * Here it is checked whether an identically named file is already in the list
			 * (otherwise would load each file again and again...
			 */
			tempFile = taskFiles.get(task).getAbsolutePath();
			if(selectedInputType == inputType[0] && tempFile.contains("_Z")) {
				tempFile = tempFile.substring(0,tempFile.lastIndexOf("_Z"));
			}
						
			//Decide by image
			for (int ff = 0; ff < allFiles.size(); ff++) {
				if (allFiles.get(ff).equals(tempFile)) {
					continue scanning;
				}
			}			

			// Copy new files to all files list
			allFiles.add(tempFile);
						
			if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
				tmpMsg = ("ACCEPTED: " + tempFile);
				progress.updateBarText(tmpMsg);
				progress.notifyMessage(tmpMsg, ProgressDialog.LOG);
			}
		}
		
		// Generate arrays based on unique names
		subTasks = allFiles.size();
		String fullPaths [] = new String[subTasks];
		for (int task = 0; task < allFiles.size(); task++) {
			tempFile = allFiles.get(task);
			fullPaths[task] = tempFile;
			if(extendedLogging || logInitialFileScreening || logWholeOMEXMLComments) {
				tmpMsg = ("FULL PATH to be submitted: " + fullPaths[task]);
				progress.updateBarText(tmpMsg);
				progress.notifyMessage(tmpMsg, ProgressDialog.LOG);
			}
		}
		allFiles.clear();
		allFiles = null;
					
		if (subTasks == 0) {
			tmpMsg = ("No acceptable folders found in " + dir + "!");
			progress.updateBarText(tmpMsg);
			progress.notifyMessage(tmpMsg, ProgressDialog.NOTIFICATION);
		}
		return fullPaths;		
	}
	
	private boolean overSaturatedMedian (double [] values, double satValue) {
		double value = 0.0;
		if(values.length == 1) {
			value = values [0];
		}else {
			Arrays.sort(values);			
		}
		
		if(values.length % 2 == 0) {
			value = (values[(int)(values.length/2.0)-1]+values[(int)(values.length/2.0)])/2.0;
		}else {
			value = values[(int)(values.length/2.0)];
			
		}
		
		if(value >= satValue) {
			String tmpMsg = ("Oversaturation test: " + true + ". Value: " + value
					+ ". SatValue: " + satValue + ". From all values: ");
			for(int i = 0; i < values.length; i++) {
				tmpMsg += values [i] + ";";
			}
			progress.updateBarText(tmpMsg);
			if(extendedLogging)	progress.notifyMessage(tmpMsg, ProgressDialog.LOG);	
			return true;
		}else {
			String tmpMsg = ("Oversaturation test: " + false + ". Value: " + value
					+ ". SatValue: " + satValue + ". From all values: ");
			for(int i = 0; i < values.length; i++) {
				tmpMsg += values [i] + ";";
			}
			progress.updateBarText(tmpMsg);
			if(extendedLogging)	progress.notifyMessage(tmpMsg, ProgressDialog.LOG);	
			return false;
		}
	}
	
	private double getMedian (double [] values) {
		double value = 0.0;
		if(values.length == 1) {
			value = values [0];
		}else {
			Arrays.sort(values);
			if(values.length % 2 == 0) {
				value = (values[(int)(values.length/2.0)-1]+values[(int)(values.length/2.0)])/2.0;
			}else {
				value = values[(int)(values.length/2.0)];				
			}
		}
		if(extendedLogging) {
			String tmpMsg = ("Median value: " + value
					+ ". All values: ");
			for(int i = 0; i < values.length; i++) {
				tmpMsg += values [i] + ";";
			}
			progress.updateBarText(tmpMsg);
			progress.notifyMessage(tmpMsg, ProgressDialog.LOG);
		}
		return value;
	}
	

	private double getMin (double [] values) {
		double value = 0.0;
		if(values.length > 1) {
			Arrays.sort(values);
		}
		value = values [0];

		if(extendedLogging) {
			String tmpMsg = ("Min value: " + value
					+ ". All values: ");
			for(int i = 0; i < values.length; i++) {
				tmpMsg += values [i] + ";";
			}
			progress.updateBarText(tmpMsg);	progress.notifyMessage(tmpMsg, ProgressDialog.LOG);	
		}		
		return value;
	}
	
	/**
	 * @param channelIDToRemove: 1-based
	 */
	private void moveAndRemoveChannel(String [] subTasksPath, String [] subTasksName, String outFolder, int channelIDToRemove) {
		int subTasks = subTasksPath.length;
		String tmpMsg;
		
		tasking: for (int subTask = 0; subTask < subTasks; subTask++) {
			// Check how many planes there are by checking how many folders with the name are in the folder
			int nrOfPlanes = 0;
			{
				String[] fileList = new File(subTasksPath[subTask].substring(0, subTasksPath[subTask].lastIndexOf(subTasksName[subTask])-1)).list();
				
				for (int f = 0; f < fileList.length; f++) {
					if (fileList[f].endsWith(subTasksName[subTask])) {
						nrOfPlanes = 1;
						break;
					}else if (fileList[f].contains(subTasksName[subTask] + "_Z")){
						nrOfPlanes ++;
					}
				}

				tmpMsg = "Found " + nrOfPlanes + " planes for file " + subTasksName[subTask] + "!";
				progress.updateBarText(tmpMsg);
				if(extendedLogging)	progress.notifyMessage("subTasks " + (subTask+1) + ": " + tmpMsg, ProgressDialog.LOG);
			}
			
			// Check how many channels there are
			int nrOfChannels = 0;
			{
				String[] fileList = new File(subTasksPath[subTask]).list();
				if(!new File(subTasksPath[subTask]).exists()) {
					fileList = new File(subTasksPath[subTask] + "_Z00").list();
				}
				
				for (int f = 0; f < fileList.length; f++) {
					if (fileList[f].endsWith(".ome.tif") && fileList[f].contains(subTasksName[subTask] + "_Z") && fileList[f].contains("_C")) {
						nrOfChannels++;
					}
				}
				
				/**
				 * Sanity check, all channels available
				 */
				String path;
				for(int p = 0; p < nrOfPlanes; p++){
					for(int c = 0; c < nrOfChannels; c++){
						path = subTasksPath [subTask];
						
						if(!new File(path).exists()) {
							if(p < 10) {
								path += "_Z0" + p;
							}else {
								path += "_Z" + p;							
							}
						}
						
						path += System.getProperty("file.separator") + subTasksName [subTask];
						
						if(p < 10) {
							path += "_Z0" + p;
						}else {
							path += "_Z" + p;							
						}
						
						if(c < 10) {
							path += "_C0" + c;
						}else {
							path += "_C" + c;							
						}
						path += ".ome.tif";
						
						if(!new File(path).exists()) {
							tmpMsg = "ERROR: Channel " + (c+1) + " was missing in " + subTasksPath[subTask] + ", could not find " + path + "!";
							progress.notifyMessage("subTasks " + (subTask+1) + ": " + tmpMsg, ProgressDialog.ERROR);
							continue tasking;
						}
					}	
				}
				
				tmpMsg = "Found " + nrOfChannels + " channels for file " + subTasksName[subTask] + "!";
				progress.updateBarText(tmpMsg);
				if(extendedLogging)	progress.notifyMessage("subTasks " + (subTask+1) + ": " + tmpMsg, ProgressDialog.LOG);
			}

			// Copy files
			{
				String path, outFilePath;
				int newChannelID;
				for(int p = 0; p < nrOfPlanes; p++){
					for(int c = 0; c < nrOfChannels; c++){
						if((c + 1) == channelIDToRemove) {
							continue;
						}
						if((c + 1) > channelIDToRemove) {
							newChannelID = c-1;
						}else {
							newChannelID = c;
						}
						
						//Determine input file path
						path = subTasksPath [subTask];

						if(!new File(path).exists()) {
							if(path.endsWith(System.getProperty("file.separator"))) {
								path = path.substring(0,path.lastIndexOf(System.getProperty("file.separator")));
							}
							if(p < 10) {
								path += "_Z0" + p;
							}else {
								path += "_Z" + p;							
							}
						}
						
						path += System.getProperty("file.separator") + subTasksName [subTask];
						
						if(p < 10) {
							path += "_Z0" + p;
						}else {
							path += "_Z" + p;							
						}
						if(c < 10) {
							path += "_C0" + c;
						}else {
							path += "_C" + c;							
						}
						path += ".ome.tif";
						
						//Determine output file path
						outFilePath = subTasksPath [subTask].substring(0,subTasksPath [subTask].lastIndexOf(subTasksName [subTask])-1);
						outFilePath = outFilePath.substring(outFilePath.lastIndexOf(System.getProperty("file.separator"))+1);
						outFilePath = outFolder + (System.getProperty("file.separator")) + outFilePath + (System.getProperty("file.separator"));
						
						outFilePath += subTasksName [subTask];
						if(nrOfPlanes > 1) {
							if(outFilePath.endsWith(System.getProperty("file.separator"))) {
								outFilePath = outFilePath.substring(0,outFilePath.lastIndexOf(System.getProperty("file.separator")));
							}
							if(p < 10) {
								outFilePath += "_Z0" + p;
							}else {
								outFilePath += "_Z" + p;							
							}
							outFilePath += System.getProperty("file.separator");
						}
						if(!new File(outFilePath).exists()) {
							new File(outFilePath).mkdirs();
						}
						
						outFilePath += subTasksName [subTask];
						if(outFilePath.endsWith(System.getProperty("file.separator"))) {
							outFilePath = outFilePath.substring(0,outFilePath.lastIndexOf(System.getProperty("file.separator")));
						}
						if(p < 10) {
							outFilePath += "_Z0" + p;
						}else {
							outFilePath += "_Z" + p;							
						}
						if(newChannelID < 10) {
							outFilePath += "_C0" + newChannelID;
						}else {
							outFilePath += "_C" + newChannelID;							
						}
						outFilePath += ".ome.tif";
						
						// Copy
						File srcFile = new File(path);
						File destFile = new File(outFilePath);
						if(destFile.exists()) {
							progress.notifyMessage("subTask " + (subTask + 1) + "/" + tasks + ": " 
									+ "There are identical images in the target folder. Did not overwrite the image: " + outFilePath, ProgressDialog.ERROR);
							continue;
						}
						try {
							FileUtils.copyFile(srcFile, destFile, true);
						} catch (IOException e) {
							String out = "";
							for (int err = 0; err < e.getStackTrace().length; err++) {
								out += " \n " + e.getStackTrace()[err].toString();
							}
							progress.notifyMessage("IO-ERROR when trying to copy tiff file from " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath() +  "." 
									+ "\nError message: " + e.getMessage()
									+ "\nError localized message: " + e.getLocalizedMessage()
									+ "\nError cause: " + e.getCause() 
									+ "\nDetailed message:"
									+ "\n" + out,
									ProgressDialog.ERROR);
							continue;
						}
						
						// Correct tif comment
						{
							if(!removeChannelFromOMEXML(destFile.getAbsolutePath(), channelIDToRemove)) {
								progress.notifyMessage("ERROR: Failed to correct OME-XML file in " + destFile.getAbsolutePath() +  ". Consequently, file has incorrect OME metadata!",
										ProgressDialog.ERROR);
								continue;
							}
						}
					}
					

					//Copy original metadata file
					{
						String metadataPath = subTasksPath [subTask];
						if(!new File(metadataPath).exists()) {
							if(p < 10) {
								metadataPath += "_Z0" + p;
							}else {
								metadataPath += "_Z" + p;							
							}
						}
						
						metadataPath += System.getProperty("file.separator") + "metadata" + System.getProperty("file.separator") + "image.ome.xml";
						
						//Determine output file path
						outFilePath = subTasksPath [subTask].substring(0,subTasksPath [subTask].lastIndexOf(subTasksName [subTask])-1);
						outFilePath = outFilePath.substring(outFilePath.lastIndexOf(System.getProperty("file.separator"))+1);
						outFilePath = outFolder + (System.getProperty("file.separator")) + outFilePath + (System.getProperty("file.separator"));
						
						outFilePath += subTasksName [subTask];
						if(nrOfPlanes > 1) {
							if(outFilePath.endsWith(System.getProperty("file.separator"))) {
								outFilePath = outFilePath.substring(0,outFilePath.lastIndexOf(System.getProperty("file.separator")));
							}
							if(p < 10) {
								outFilePath += "_Z0" + p;
							}else {
								outFilePath += "_Z" + p;							
							}
							outFilePath += System.getProperty("file.separator");
						}
						outFilePath += "metadata" + System.getProperty("file.separator");
						
						if(!new File(outFilePath).exists()) {
							new File(outFilePath).mkdirs();
						}
						
						outFilePath += "image.ome.xml";					
						
						tmpMsg = "Copying " + metadataPath + " to " + outFilePath + "!";
						if(extendedLogging)	progress.notifyMessageAndDisplayInBar("subTasks " + (subTask+1) + ": " + tmpMsg, ProgressDialog.LOG);
						
						try {
							FileUtils.copyFile(new File(metadataPath),
									new File(outFilePath), 
									true);
						} catch (IOException e) {
							String out = "";
							for (int err = 0; err < e.getStackTrace().length; err++) {
								out += " \n " + e.getStackTrace()[err].toString();
							}
							progress.notifyMessage("IO-ERROR when trying to copy metadata file " + metadataPath + " to " + outFilePath +  "." 
									+ "\nError message: " + e.getMessage()
									+ "\nError localized message: " + e.getLocalizedMessage()
									+ "\nError cause: " + e.getCause() 
									+ "\nDetailed message:"
									+ "\n" + out,
									ProgressDialog.ERROR);
							continue;
						}
					}
				}
			}
			
			progress.addToBar(0.9/subTasks/2.0);
		}
		
	}
	
	/**
	 * Removes annoations for a channel in the OME XML metadata of a .ome.tiff file located at @param path.
	 * @param channel: channel to be removed (1-based, so channel > 0, channel <= nr of channels.
	 * @return true if channel was successfully removed from the OME metadata
	 */
	private boolean removeChannelFromOMEXML (String path, int channelToBeRemoved) {
		String comment = "";
		String tmpMsg;
		/**
		 * Reading the TIFF comment = OME XML Metadata
		 */
		try {
			comment = getTiffComment(path);
		} catch (IOException e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			progress.notifyMessage("IO-ERROR when trying to read the tiff comment from " + path + "." 
					+ "\nError message: " + e.getMessage()
					+ "\nError localized message: " + e.getLocalizedMessage()
					+ "\nError cause: " + e.getCause() 
					+ "\nDetailed message:"
					+ "\n" + out,
					ProgressDialog.ERROR);
			return false;
		}
		
		/**
		 * Creating an xml document out of it
		 */
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document omeXMLDoc = null;
		try {
			db = dbf.newDocumentBuilder();
			omeXMLDoc = db.parse(new org.xml.sax.InputSource(new StringReader(comment)));
			omeXMLDoc.getDocumentElement().normalize();
			
		} catch (ParserConfigurationException e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			progress.notifyMessage("Could not create XML Document from Tiff comment for " + path + "." 
					+ "\nError message: " + e.getMessage()
					+ "\nError localized message: " + e.getLocalizedMessage()
					+ "\nError cause: " + e.getCause() 
					+ "\nDetailed message:"
					+ "\n" + out,
					ProgressDialog.ERROR);
		} catch (SAXException e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			progress.notifyMessage("Could not create XML Document from Tiff comment for " + path + "." 
					+ "\nError message: " + e.getMessage()
					+ "\nError localized message: " + e.getLocalizedMessage()
					+ "\nError cause: " + e.getCause() 
					+ "\nDetailed message:"
					+ "\n" + out,
					ProgressDialog.ERROR);
		} catch (IOException e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			progress.notifyMessage("Could not create XML Document from Tiff comment for " + path + "." 
					+ "\nError message: " + e.getMessage()
					+ "\nError localized message: " + e.getLocalizedMessage()
					+ "\nError cause: " + e.getCause() 
					+ "\nDetailed message:"
					+ "\n" + out,
					ProgressDialog.ERROR);
		}
				
		/**
		 * Modifying comment
		 */
		
		//Remove wrong channel and correct channel ids among channels
		{
			NodeList channelList = omeXMLDoc.getElementsByTagName("Channel");
			
			tmpMsg = "Found " + channelList.getLength() + " channels in OME XML of " + path + "!";
			progress.updateBarText(tmpMsg);
			if(extendedLogging)	progress.notifyMessage("" + tmpMsg, ProgressDialog.LOG);
			
			String id;
			int channelNr;
			for(int c = channelList.getLength()-1; c >= 0; c--) {
				id = channelList.item(c).getAttributes().getNamedItem("ID").getNodeValue();
				channelNr = Integer.parseInt(id.substring(id.lastIndexOf(":")+1));
				if(channelNr == (channelToBeRemoved-1)) {

					tmpMsg = "Deleting channel with id " + id 
							+ " and name " + channelList.item(c).getAttributes().getNamedItem("Name").getNodeValue() 
							+ " from OME XML in " + path + "!";
					progress.updateBarText(tmpMsg);
					if(extendedLogging)	progress.notifyMessage("" + tmpMsg, ProgressDialog.LOG);
					
					channelList.item(c).getParentNode().removeChild(channelList.item(c));
				}else if(channelNr > (channelToBeRemoved-1)) {
					id = id.substring(0,id.lastIndexOf(":")+1);
					id += "" + (channelNr-1);
					channelList.item(c).getAttributes().getNamedItem("ID").setNodeValue(id);
				}
			}
		}
		
		//Correct channels in TiffData
		{
			NodeList tiffDataList = omeXMLDoc.getElementsByTagName("TiffData");
			
			tmpMsg = "Found " + tiffDataList.getLength() + " tiff data in OME XML of " + path + "!";
			progress.updateBarText(tmpMsg);
			if(extendedLogging)	progress.notifyMessage("" + tmpMsg, ProgressDialog.LOG);
			
			int firstC;
			String tempName;
			String newCString, oldCString;
			for(int tD = tiffDataList.getLength()-1; tD >= 0; tD--) {
				firstC = Integer.parseInt(tiffDataList.item(tD).getAttributes().getNamedItem("FirstC").getNodeValue());
				if(firstC == (channelToBeRemoved-1)) {

					tmpMsg = "Deleting channel with id " + firstC
							+ " and name " 
							+ getFirstNodeWithName(tiffDataList.item(tD).getChildNodes(), "UUID").getAttributes().getNamedItem("FileName").getNodeValue()
							+ " from OME XML in " + path + "!";
					progress.updateBarText(tmpMsg);
					if(extendedLogging)	progress.notifyMessage("" + tmpMsg, ProgressDialog.LOG);
					
					tiffDataList.item(tD).getParentNode().removeChild(tiffDataList.item(tD));
				}else if(firstC > (channelToBeRemoved-1)) {
					tiffDataList.item(tD).getAttributes().getNamedItem("FirstC").setNodeValue("" + (firstC-1));
					
					tempName = getFirstNodeWithName(tiffDataList.item(tD).getChildNodes(), "UUID").getAttributes().getNamedItem("FileName").getNodeValue();

					oldCString = "_C";
					if((firstC) < 10) {
						oldCString += "0";
					}					
					oldCString += "" + (firstC);
					
					newCString = "_C";
					if((firstC-1) < 10) {
						newCString += "0";
					}
					newCString += "" + (firstC-1);
					
					tempName = tempName.replace(oldCString, newCString);
					
					if(extendedLogging) {
						tmpMsg = "Correcting tiffData object with C=" + firstC
								+ " and name " 
								+ getFirstNodeWithName(tiffDataList.item(tD).getChildNodes(), "UUID").getAttributes().getNamedItem("FileName").getNodeValue()
								+ " to name "
								+ tempName
								+ " (in OME XML in " + path + ")!";
						progress.updateBarText(tmpMsg);
						progress.notifyMessage("" + tmpMsg, ProgressDialog.LOG);
					}
					
					getFirstNodeWithName(tiffDataList.item(tD).getChildNodes(), "UUID").getAttributes().getNamedItem("FileName").setNodeValue(tempName);
				}
			}
		}
		
		//Correct channels in planes
		{
			NodeList planeList = omeXMLDoc.getElementsByTagName("Plane");
			
			tmpMsg = "Found " + planeList.getLength() + " planes in OME XML of " + path + "!";
			progress.updateBarText(tmpMsg);
			if(extendedLogging)	progress.notifyMessage("" + tmpMsg, ProgressDialog.LOG);
			
			int theC;
			for(int tD = planeList.getLength()-1; tD >= 0; tD--) {
				theC = Integer.parseInt(planeList.item(tD).getAttributes().getNamedItem("TheC").getNodeValue());
				if(theC == (channelToBeRemoved-1)) {

					tmpMsg = "Deleting plane from channel " + theC + "from OME XML in " + path + "!";
					if(extendedLogging)	progress.notifyMessageAndDisplayInBar("" + tmpMsg, ProgressDialog.LOG);
					
					planeList.item(tD).getParentNode().removeChild(planeList.item(tD));
				}else if(theC > (channelToBeRemoved-1)) {
					planeList.item(tD).getAttributes().getNamedItem("TheC").setNodeValue("" + (theC-1));
				}
			}
		}
		
		//Correct Size C
		int NrOfChannels = 0;
		{
			if(omeXMLDoc.getElementsByTagName("Pixels").getLength() > 1) {
				tmpMsg = "WARNING: More than one 'Pixels' node exists in OME XML file. This program might edit the wrong 'Pixels' node, "
						+ "since it will always edit the first existing node)!";
				progress.notifyMessage("" + tmpMsg, ProgressDialog.NOTIFICATION);
			}
			Node pixelsNode = omeXMLDoc.getElementsByTagName("Pixels").item(0);
			NrOfChannels = Integer.parseInt(pixelsNode.getAttributes().getNamedItem("SizeC").getNodeValue());
			pixelsNode.getAttributes().getNamedItem("SizeC").setNodeValue(""+(NrOfChannels-1));
			
			tmpMsg = "Adjusted SizeC to " + pixelsNode.getAttributes().getNamedItem("SizeC").getNodeValue() + "in OME XML of " + path + "!";
			if(extendedLogging)	progress.notifyMessageAndDisplayInBar("" + tmpMsg, ProgressDialog.LOG);
		}
		
		//Add note to description of the image
		{
			if(omeXMLDoc.getElementsByTagName("Image").getLength() > 1) {
				tmpMsg = "WARNING: More than one 'Image' node exists in OME XML file. This program might add the descriotion to the wrong 'Image' node, "
						+ "since it will always edit the first existing 'Image' node)!";
				progress.notifyMessage("" + tmpMsg, ProgressDialog.NOTIFICATION);
			}
			Node descriptionNode = getFirstNodeWithName(omeXMLDoc.getElementsByTagName("Image").item(0).getChildNodes(), "Description");
			String description = descriptionNode.getTextContent();
			description += " Channel " + channelToBeRemoved + " / " + NrOfChannels + " was removed from this image using the ImageJ plugin '" + PLUGINNAME + "' "
					+ "(Version " + PLUGINVERSION + ", more information at " + PLUGINURL + ").";
			description += " After removing the channel, all other channel IDs, numbers, and names were shifted/corrected to create a " + (NrOfChannels-1) + "-channel image, in detail:";
			for(int c = 0; c < NrOfChannels; c++) {
				if((c+1) == channelToBeRemoved) {
					description += " Channel " + (c+1) + " was removed;";
				}else if((c+1) > channelToBeRemoved) {
					description += " Channel " + (c+1) + " was relabeled to be channel " + (c) + ";";
				}
			}
			description = description.substring(0,description.length()-1);
			description += ". ";
			description += "See original metadata annotations for the original image dimensions and information.";
			
			descriptionNode.setTextContent(description);		
			
			tmpMsg = "Adjusted SizeC to " + descriptionNode.getTextContent() + "in OME XML of " + path + "!";
			if(extendedLogging)	progress.notifyMessageAndDisplayInBar("" + tmpMsg, ProgressDialog.LOG);
		}
		
		String outComment = "";		
		//Retrieve comment from Document
		{
			try {
				javax.xml.transform.TransformerFactory transformerFactory = TransformerFactory.newInstance();
				javax.xml.transform.Transformer transformer;
				transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
				
				StringWriter stringWriter = new StringWriter();
				transformer.transform(new DOMSource(omeXMLDoc), new StreamResult(stringWriter));
				outComment = stringWriter.toString();
				if(extendedLogging) {
					progress.notifyMessage("Metadata XML has been modified...", ProgressDialog.LOG);	
				}
			} catch (TransformerConfigurationException e) {
				String out = "";
				for (int err = 0; err < e.getStackTrace().length; err++) {
					out += " \n " + e.getStackTrace()[err].toString();
				}
				progress.notifyMessage("Could not initialize transformer for XMLs. No easy fix available. Report problem to developer!"
						+ "\nError message: " + e.getMessage()
						+ "\nError localized message: " + e.getLocalizedMessage()
						+ "\nError cause: " + e.getCause() 
						+ "\nDetailed message:"
						+ "\n" + out,
						ProgressDialog.ERROR);	
			} catch (TransformerException e) {
				String out = "";
				for (int err = 0; err < e.getStackTrace().length; err++) {
					out += " \n " + e.getStackTrace()[err].toString();
				}
				progress.notifyMessage("" 
						+ "Error when writing modified xml." 
						+ "\nError message: " + e.getMessage()
						+ "\nError localized message: " + e.getLocalizedMessage()
						+ "\nError cause: " + e.getCause() 
						+ "\nDetailed message:"
						+ "\n" + out,
						ProgressDialog.ERROR);
			}
		}
		if(logWholeOMEXMLComments) {
			progress.notifyMessage("" 
					+ "OME XML Comment before correction:"
					+ "\n" + comment,
					ProgressDialog.LOG);
			progress.notifyMessage("" 
					+ "OME XML Comment after correction:"
					+ "\n" + outComment,
					ProgressDialog.LOG);
		}
		
		/**
		 * Saving the modified comment
		 */
		try {
			replaceTiffCommentInFile(path, outComment);
		} catch (IOException e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			progress.notifyMessage("IO-ERROR when trying to replace the tiff comment in " + path + "." 
					+ "\nError message: " + e.getMessage()
					+ "\nError localized message: " + e.getLocalizedMessage()
					+ "\nError cause: " + e.getCause() 
					+ "\nDetailed message:"
					+ "\n" + out,
					ProgressDialog.ERROR);
			return false;
		} catch (FormatException e) {
			String out = "";
			for (int err = 0; err < e.getStackTrace().length; err++) {
				out += " \n " + e.getStackTrace()[err].toString();
			}
			progress.notifyMessage("FORMAT-ERROR when trying to replace the tiff comment in" + path + "." 
					+ "\nError message: " + e.getMessage()
					+ "\nError localized message: " + e.getLocalizedMessage()
					+ "\nError cause: " + e.getCause() 
					+ "\nDetailed message:"
					+ "\n" + out,
					ProgressDialog.ERROR);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Open the tif at @param file and @return the tif comment (= OME XML String)
	 * @throws IOException 
	 * */
	private String getTiffComment(String path) throws IOException {
		progress.updateBarText("Retrieving tif comment from " + path + ")");
		TiffParser tp = new TiffParser(path);
		String comment = "" + tp.getComment();
		tp = null;
		System.gc();
		return comment;
	}
			
	/**
	 * @param path: The absolute file path to the tiff file, whose tiff comment shall be replaced with the new @param comment
	 * @throws IOException when TiffSave or InputStream or overwriting the comment fails due to an IOException
	 * @throws FormatException when TiffSaver fails to overwrite the Tiff comment
	 */
	private void replaceTiffCommentInFile (String path, String comment) throws IOException, FormatException {
		/**
		 * Saving modified omexml tif comment into copied image
		 * */
		progress.updateBarText("Saving modified tif comment into file " + path + ")");
		TiffSaver saver = new TiffSaver(path);
	    RandomAccessInputStream in = new RandomAccessInputStream(path);
	    saver.overwriteComment(in, comment);
		in.close();
		progress.updateBarText("Saving " + path + " done!");
		if(extendedLogging)	progress.notifyMessage("Saved " + path, ProgressDialog.LOG);
		
	}
	
	/**
	 * Find the first node with a specific name in a NodeList
	 * @param A NodeList in which a Node shall be found
	 * @param The name of the Node that shall be found as a String
	 * @return First node in the list called 'nodes' that has the given name
	 */
	private static Node getFirstNodeWithName(NodeList nodes, String name) {
		for(int n = 0; n < nodes.getLength(); n++) {
			if(nodes.item(n).getNodeName().equals(name)) {
				return nodes.item(n);
			}
		}
		return null;
	}
}// end main class