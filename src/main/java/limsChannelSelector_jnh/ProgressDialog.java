package limsChannelSelector_jnh;

/**
 * Parts of this code were inherited from MotiQ (https://github.com/hansenjn/MotiQ).
 * @author Jan Niklas Hansen
 * 
 * And they were further customized in 2023.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.SwingConstants;

import ij.IJ;

public class ProgressDialog extends javax.swing.JFrame implements ActionListener{
	String dataLeft [], dataRight[], notifications [];
	public boolean notificationsAvailable = false, errorsAvailable = false;
	int task, tasks;
	
	static final int ERROR = 0, NOTIFICATION = 1, LOG = 2;;
	JPanel bgPanel;
	JScrollPane jScrollPaneLeft, jScrollPaneRight, jScrollPaneBottom;
	JList <String> ListeLeft, ListeRight, ListeBottom;
	
	private JProgressBar progressBar = new JProgressBar();
	private double taskFraction = 0.0;
	
	public ProgressDialog(String [] taskList) {
		super();
		initGUI();
		dataLeft = taskList.clone();
		tasks = taskList.length;
		for(int i = 0; i < tasks; i++){
			if(dataLeft[i]!=""){
				dataLeft [i] = (i+1) + ": " + dataLeft [i]; 
			}			
		}
		ListeLeft.setListData(dataLeft);
		taskFraction = 0.0;
		task = 1;
	}
	
	public ProgressDialog(String [] taskList, int [] seriesList, int addToSeriesNumber) {
		super();
		initGUI();
		dataLeft = taskList.clone();
		tasks = taskList.length;
		for(int i = 0; i < tasks; i++){
			if(dataLeft[i]!=""){
				dataLeft [i] = (i+1) + ": " + dataLeft [i] + ", series " + (seriesList [i] + addToSeriesNumber); 
			}
		}
		ListeLeft.setListData(dataLeft);
		taskFraction = 0.0;
		task = 1;
	}
	
	public ProgressDialog(String [] taskList, String [] seriesList) {
		super();
		if(taskList.length != seriesList.length) {
			IJ.error("File loading error... nSeries != nTasks");
		}
		initGUI();
		dataLeft = taskList.clone();
		tasks = taskList.length;
		for(int i = 0; i < tasks; i++){
			if(dataLeft[i]!=""){
				dataLeft [i] = (i+1) + ": " + dataLeft [i] + ", Series: " + seriesList [i]; 
			}			
		}
		ListeLeft.setListData(dataLeft);
		taskFraction = 0.0;
		task = 1;
	}
	
	private void initGUI() {
		int prefXSize = 600, prefYSize = 500;
		this.setMinimumSize(new java.awt.Dimension(prefXSize, prefYSize+40));
		this.setSize(prefXSize, prefYSize+40);			
		this.setTitle("Multi-Task-Manager - by JN Hansen (\u00a9 2016)");
//		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		//Surface
			bgPanel = new JPanel();
			bgPanel.setLayout(new BoxLayout(bgPanel, BoxLayout.Y_AXIS));
			bgPanel.setVisible(true);
			bgPanel.setPreferredSize(new java.awt.Dimension(prefXSize,prefYSize-20));
			{//TOP: Display tasks left, and tasks that were run right
				int subXSize = prefXSize, subYSize = 200;
				JPanel topPanel = new JPanel();
				topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
				topPanel.setVisible(true);
				topPanel.setPreferredSize(new java.awt.Dimension(subXSize,subYSize));
				{
					JPanel imPanel = new JPanel();
					imPanel.setLayout(new BorderLayout());
					imPanel.setVisible(true);
					imPanel.setPreferredSize(new java.awt.Dimension((int)((double)(subXSize/2.0)),subYSize));
					{
						JLabel spacer = new JLabel("Remaining files to process:",SwingConstants.LEFT);
						spacer.setMinimumSize(new java.awt.Dimension((int)((double)(subXSize/2.0)-20),60));
						spacer.setVisible(true);
						imPanel.add(spacer,BorderLayout.NORTH); 
					}
					{
						jScrollPaneLeft = new JScrollPane();
						jScrollPaneLeft.setHorizontalScrollBarPolicy(30);
						jScrollPaneLeft.setVerticalScrollBarPolicy(20);
						jScrollPaneLeft.setPreferredSize(new java.awt.Dimension((int)((double)(subXSize/2.0)-10), subYSize-60));
						imPanel.add(jScrollPaneLeft,BorderLayout.CENTER); 
						{
							ListModel <String> ListeModel = new DefaultComboBoxModel <String>(new String[] { "" });
							ListeLeft = new JList <String> ();
							jScrollPaneLeft.setViewportView(ListeLeft);
							ListeLeft.setModel(ListeModel);
						}
					}	
					topPanel.add(imPanel);
				}
				{
					JPanel imPanel = new JPanel();
					imPanel.setLayout(new BorderLayout());
					imPanel.setVisible(true);
					imPanel.setPreferredSize(new java.awt.Dimension((int)((double)(subXSize/2.0)),subYSize));
					{
						JLabel spacer = new JLabel("Processed files:",SwingConstants.LEFT);
						spacer.setMinimumSize(new java.awt.Dimension((int)((double)(subXSize/2.0)-20),60));
						spacer.setVisible(true);
						imPanel.add(spacer,BorderLayout.NORTH); 
					}
					{	
						jScrollPaneRight = new JScrollPane();
						jScrollPaneRight.setHorizontalScrollBarPolicy(30);
						jScrollPaneRight.setVerticalScrollBarPolicy(20);
						jScrollPaneRight.setPreferredSize(new java.awt.Dimension((int)((double)(subXSize/2.0)-10), subYSize-60));
						imPanel.add(jScrollPaneRight,BorderLayout.CENTER); 
						{
							ListModel <String> ListeModel = new DefaultComboBoxModel <String> (new String[] { "" });
							ListeRight = new JList <String> ();
							jScrollPaneRight.setViewportView(ListeRight);
							ListeRight.setModel(ListeModel);
						}
					}
					topPanel.add(imPanel);
				}				
				bgPanel.add(topPanel);
			}
			{
				JPanel spacer = new JPanel();
				spacer.setMaximumSize(new java.awt.Dimension(prefXSize,10));
				spacer.setVisible(true);
				bgPanel.add(spacer);
			}
			{
				progressBar = new JProgressBar();
				progressBar = new JProgressBar(0, 100);
				progressBar.setPreferredSize(new java.awt.Dimension(prefXSize,40));
				progressBar.setStringPainted(true);
				progressBar.setValue(0);
				progressBar.setString("no analysis started!");
				bgPanel.add(progressBar);	
			}
			{
				JPanel spacer = new JPanel();
				spacer.setMaximumSize(new java.awt.Dimension(prefXSize,10));
				spacer.setVisible(true);
				bgPanel.add(spacer);
			}
			{
				JPanel imPanel = new JPanel();
				imPanel.setLayout(new BorderLayout());
				imPanel.setVisible(true);
				imPanel.setPreferredSize(new java.awt.Dimension(prefXSize,140));
				{
					JLabel spacer = new JLabel("Notifications:", SwingConstants.LEFT);
					spacer.setMinimumSize(new java.awt.Dimension(prefXSize,40));
					spacer.setVisible(true);
					imPanel.add(spacer, BorderLayout.NORTH);
				}
				{	
					jScrollPaneBottom = new JScrollPane();
					jScrollPaneBottom.setHorizontalScrollBarPolicy(30);
					jScrollPaneBottom.setVerticalScrollBarPolicy(20);
					jScrollPaneBottom.setPreferredSize(new java.awt.Dimension(prefXSize, 100));
					imPanel.add(jScrollPaneBottom, BorderLayout.CENTER);
					{
						ListModel <String> ListeModel = new DefaultComboBoxModel <String> (new String[] { "" });
						ListeBottom = new JList <String> ();
						jScrollPaneBottom.setViewportView(ListeBottom);
						ListeBottom.setModel(ListeModel);
					}
				}
				bgPanel.add(imPanel);
			}
			getContentPane().add(bgPanel);		
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) {
//		Object eventQuelle = ae.getSource();
//		if (eventQuelle == abortButton){
//			abort = true;
////			updateDisplay();
//		}	
	}
	
	public void moveTask(int i){
		if(dataRight == null){
			dataRight = new String [2];
			dataRight [0] = "" + dataLeft[0];
			
			String [] dataLeftCopy = dataLeft.clone();
			dataLeft = new String [dataLeft.length-1];
			for(int j = 1; j < dataLeftCopy.length; j++){
				dataLeft[j-1] = dataLeftCopy[j];
			}
		}else if(i==(tasks-1)){
			String [] dataRightCopy = dataRight.clone();
			dataRight = new String [dataRight.length+1];
			for(int j = 0; j < dataRightCopy.length; j++){
				dataRight[j+1] = dataRightCopy[j];
			}
			dataRight[0] = ""+dataLeft[0];			
			dataLeft = new String [2];
		}else{
			String [] dataRightCopy = dataRight.clone();
			dataRight = new String [dataRight.length+1];
			for(int j = 0; j < dataRightCopy.length; j++){
				dataRight[j+1] = dataRightCopy[j];
			}
			dataRight[0] = ""+dataLeft[0];
						
			String [] dataLeftCopy = dataLeft.clone();
			dataLeft = new String [dataLeft.length-1];
			for(int j = 1; j < dataLeftCopy.length; j++){
				dataLeft[j-1] = dataLeftCopy[j];
			}			
		}	
		ListeLeft.setListData(dataLeft);
		ListeRight.setListData(dataRight);
		try {
			jScrollPaneLeft.updateUI();
			jScrollPaneRight.updateUI();
			bgPanel.updateUI();			
		}catch(Exception e) {			
		}
		
		if(task == tasks){
			if(errorsAvailable){
				replaceBarText("processing done but some tasks failed (see notifications)!");
				progressBar.setValue(100); 		
				progressBar.setStringPainted(true);
				progressBar.setForeground(Color.red);
			}else if(notificationsAvailable){
				replaceBarText("processing done, but some notifications are available!");
				progressBar.setValue(100); 
				progressBar.setStringPainted(true);
				progressBar.setForeground(new Color(255,130,0));
			}else{
				replaceBarText("processing done!");
				progressBar.setStringPainted(true);
				progressBar.setForeground(new Color(0,140,0));
			}
			progressBar.setValue(100);
		}else{
			taskFraction = 0.0;
			task++;
		}
	}
	
	public void notifyMessage(String message, int type){
		if(type == ERROR){
			errorsAvailable = true;
		}else if(type == NOTIFICATION){
			notificationsAvailable = true;
		}
		
		if(notifications==null){
			notifications = new String [2];
			notifications [0] = message;
		}else{
			String [] notificationsCopy = notifications.clone();
			notifications = new String [notifications.length+1];
			for(int j = 0; j < notificationsCopy.length; j++){
				notifications[j+1] = notificationsCopy[j];
			}
			notifications [0] = message;
		}
		ListeBottom.setListData(notifications);
		try {
			jScrollPaneBottom.updateUI();
			bgPanel.updateUI();			
		}catch(Exception e) {			
		}
	}
	
	public void notifyMessageAndDisplayInBar(String message, int type){
		this.notifyMessage(message, type);
		this.updateBarText(message);
	}
	
	public void addToBar(double addFractionOfTask){
		taskFraction += addFractionOfTask;
		if(taskFraction >= 1.0){
			taskFraction = 0.9;
		}
		progressBar.setValue((int)Math.round(((double)(task-1)/tasks)*100.0+taskFraction*(100/tasks)));
		try {
			bgPanel.updateUI();			
		}catch(Exception e) {			
		}
	}
	
	public void setBar(double fractionOfTask){
		taskFraction = fractionOfTask;
		if(taskFraction > 1.0){
			taskFraction = 0.9;
		}
		progressBar.setValue((int)Math.round(((double)(task-1)/tasks)*100.0+taskFraction*(100/tasks)));
		try {
			bgPanel.updateUI();			
		}catch(Exception e) {			
		}
	}
	
	public void updateBarText(String text){
		progressBar.setString("Task " + task + "/" + tasks + ": " + text);
		try {
			bgPanel.updateUI();			
		}catch(Exception e) {			
		}
	}
	
	public void replaceBarText(String text){			
		progressBar.setString(text);
		try {
			bgPanel.updateUI();			
		}catch(Exception e) {			
		}
	}
}