package org.htmlcleaner;

/**  Copyright (c) 2013, Marton Szeles
    All rights reserved.

    Redistribution and use of this software in source and binary forms,
    with or without modification, are permitted provided that the following
    conditions are met:

    * Redistributions of source code must retain the above
      copyright notice, this list of conditions and the
      following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the
      following disclaimer in the documentation and/or other
      materials provided with the distribution.

    * The name of HtmlCleaner may not be used to endorse or promote
      products derived from this software without specific prior
      written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
**/


import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.htmlcleaner.conditional.TagNodeEmptyContentCondition;


import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;


public class GUI extends JFrame {
	private JTextField inputText;
	private JTextField outputText;

	private String inputSrc;
	private String outputSrc; 
	private ImageIcon imgThisImg;

	private String whatYouSelected;
	private String whatYouSelected2;

	public GUI() throws MalformedURLException, IOException {
		Container container = getContentPane();
		container.setLayout(null);


		//IMAGE

		JLabel lblImage = new JLabel("");
		lblImage.setToolTipText("Visit homepage");
		lblImage.setBounds(283, 11, 209, 52);
		getContentPane().add(lblImage);

		imgThisImg = new ImageIcon(this.getClass().getResource("images/logo.jpg"));
		lblImage.setIcon(imgThisImg);

		lblImage.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent me) {  
				setCursor(new Cursor(Cursor.HAND_CURSOR));  
			}  
			public void mouseExited(MouseEvent me) {  
				setCursor(Cursor.getDefaultCursor());  
			}  
			public void mouseClicked(MouseEvent clickLink) {
				try {
					Desktop.getDesktop().browse(new URL("http://htmlcleaner.sourceforge.net/index.php").toURI());
				} catch (IOException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}

			}	
		});

		// INPUT source ------------
		JLabel lblInputFile = new JLabel("Input File / URL:");
		lblInputFile.setForeground(Color.RED);
		lblInputFile.setBounds(44, 96, 114, 14); // coordinates
		container.add(lblInputFile);

		inputText = new JTextField();
		inputText.setToolTipText("Enter path of local HTML file or enter URL starting with \"http://\"l");
		inputText.setBounds(168, 91, 399, 25);
		getContentPane().add(inputText);
		inputText.setColumns(10);

		// OUTPUT source ------------
		JLabel lblOutputFile = new JLabel("Output File:");
		lblOutputFile.setForeground(Color.RED);
		lblOutputFile.setBounds(44, 138, 114, 14);
		getContentPane().add(lblOutputFile);

		outputText = new JTextField();
		outputText.setToolTipText("Enter the path of the output file");
		outputText.setBounds(168, 133, 399, 25);
		getContentPane().add(outputText);
		outputText.setColumns(10);



		// filechoosers

		// FILECHOOSER INPUT BUTTON
		JButton btnChooseFile = new JButton("Choose File");
		btnChooseFile.setToolTipText("Choose input file");
		btnChooseFile.setBounds(588, 91, 126, 24);
		getContentPane().add(btnChooseFile);


		btnChooseFile.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent cleanStart) {
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter(
						"HTML & XML files", "html", "xml");
				chooser.setFileFilter(filter);
				int returnVal = chooser.showOpenDialog(getParent());
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					whatYouSelected = chooser.getSelectedFile().getPath();
					inputText.setText(whatYouSelected);
					String temp = whatYouSelected.substring(0, whatYouSelected.lastIndexOf('.'));
					outputText.setText(temp + ".xml");
				}

			}
		});

		// FILECHOOSER OUTPUT BUTTON
		JButton btnChooseFile2 = new JButton("Choose File");
		btnChooseFile2.setToolTipText("Choose output file");
		btnChooseFile2.setBounds(588, 134, 126, 23);
		getContentPane().add(btnChooseFile2);


		btnChooseFile2.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent cleanStart) {
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter(
						"HTML & XML files", "html", "xml");
				chooser.setFileFilter(filter);
				int returnVal = chooser.showOpenDialog(getParent());
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					whatYouSelected2 = chooser.getSelectedFile().getPath();
					outputText.setText(whatYouSelected2);
				}

			}
		});

		//CLEAR FIELDS BUTTON
		JButton clearButton = new JButton("Clear fields");
		clearButton.setToolTipText("Clear the input/output fields");
		clearButton.setBackground(new Color(240, 128, 128));
		clearButton.setForeground(new Color(128, 128, 128));
		clearButton.setBounds(441, 171, 126, 14);
		getContentPane().add(clearButton);

		clearButton.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent clearFields) {

				inputText.setText("");
				outputText.setText("");

			}
		});

		//PROPERTIES LABEL		

		JLabel lblNewLabel = new JLabel("Properties");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblNewLabel.setBounds(29, 181, 154, 25);
		getContentPane().add(lblNewLabel);

		// Checkboxes
		final CleanerProperties props = new CleanerProperties();

		//1
		final JCheckBox setAdvancedXmlEscape = new JCheckBox(
				"Advanced XML-Escape");
		setAdvancedXmlEscape.setBackground(new Color(255, 250, 250));
		setAdvancedXmlEscape.setSelected(true);
		setAdvancedXmlEscape.setBounds(29, 222, 222, 25);
		getContentPane().add(setAdvancedXmlEscape);

		setAdvancedXmlEscape.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setAdvancedXmlEscape.isSelected()){
					props.setAdvancedXmlEscape(true);
				}else{
					props.setAdvancedXmlEscape(true);
				}
			}
		});

		//2

		final JCheckBox TranslateSpecialEntities = new JCheckBox(
				"Translate Special Entities");
		TranslateSpecialEntities.setBackground(new Color(255, 250, 250));
		TranslateSpecialEntities.setSelected(true);
		TranslateSpecialEntities.setBounds(29, 250, 222, 25);
		getContentPane().add(TranslateSpecialEntities);

		TranslateSpecialEntities.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (TranslateSpecialEntities.isSelected()){
					props.setTranslateSpecialEntities(true);
				}else{
					props.setTranslateSpecialEntities(false);
				}
			}
		});

		//3
		final JCheckBox setRecognizeUnicodeChars = new JCheckBox(
				"Recognize Unicode Chars");
		setRecognizeUnicodeChars.setBackground(new Color(255, 250, 250));
		setRecognizeUnicodeChars.setSelected(true);
		setRecognizeUnicodeChars.setBounds(29, 278, 222, 25);
		getContentPane().add(setRecognizeUnicodeChars);

		setRecognizeUnicodeChars.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setRecognizeUnicodeChars.isSelected()){
					props.setRecognizeUnicodeChars(true);
				}else{
					props.setRecognizeUnicodeChars(false);
				}
			}
		});

		//4
		final JCheckBox setUseCdataForScriptAndStyle = new JCheckBox(
				"CDATA for Script & Style");
		setUseCdataForScriptAndStyle.setBackground(new Color(255, 250, 250));
		setUseCdataForScriptAndStyle.setSelected(true);
		setUseCdataForScriptAndStyle.setBounds(29, 306, 222, 25);
		getContentPane().add(setUseCdataForScriptAndStyle);

		setUseCdataForScriptAndStyle.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setUseCdataForScriptAndStyle.isSelected()){
					props.setUseCdataForScriptAndStyle(true);
				}else{
					props.setUseCdataForScriptAndStyle(false);
				}
			}
		});

		//5
		final JCheckBox setOmitUnknownTags = new JCheckBox(
				"Omit Unknown Tags");
		setOmitUnknownTags.setBackground(new Color(255, 250, 250));
		setOmitUnknownTags.setBounds(29, 334, 222, 25);
		getContentPane().add(setOmitUnknownTags);

		setOmitUnknownTags.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setOmitUnknownTags.isSelected()){
					props.setOmitUnknownTags(true);
				}else{
					props.setOmitUnknownTags(false);
				}
			}
		});

		//6
		final JCheckBox setTreatUnknownTagsAsContent = new JCheckBox(
				"Unknown Tags as Content");
		setTreatUnknownTagsAsContent.setBackground(new Color(255, 250, 250));
		setTreatUnknownTagsAsContent.setBounds(29, 362, 222, 29);
		getContentPane().add(setTreatUnknownTagsAsContent);

		setTreatUnknownTagsAsContent.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setTreatUnknownTagsAsContent.isSelected()){
					props.setTreatUnknownTagsAsContent(true);
				}else{
					props.setTreatUnknownTagsAsContent(false);
				}
			}
		});

		//7
		final JCheckBox setOmitDeprecatedTags = new JCheckBox(
				"Omit Deprecated Tags");
		setOmitDeprecatedTags.setBackground(new Color(255, 250, 250));
		setOmitDeprecatedTags.setBounds(253, 221, 239, 25);
		getContentPane().add(setOmitDeprecatedTags);

		setOmitDeprecatedTags.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setOmitDeprecatedTags.isSelected()){
					props.setOmitDeprecatedTags(true);
				}else{
					props.setOmitDeprecatedTags(false);
				}
			}
		});	

		//8
		final JCheckBox setTreatDeprecatedTagsAsContent = new JCheckBox(
				"Deprecated Tags as Content");
		setTreatDeprecatedTagsAsContent.setBackground(new Color(255, 250, 250));
		setTreatDeprecatedTagsAsContent.setBounds(253, 249, 239, 25);
		getContentPane().add(setTreatDeprecatedTagsAsContent);

		setTreatDeprecatedTagsAsContent.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setTreatDeprecatedTagsAsContent.isSelected()){
					props.setTreatDeprecatedTagsAsContent(true);
				}else{
					props.setTreatDeprecatedTagsAsContent(false);
				}
			}
		});

		//9
		final JCheckBox setOmitComments = new JCheckBox(
				"Omit Comments");
		setOmitComments.setBackground(new Color(255, 250, 250));
		setOmitComments.setBounds(253, 277, 239, 25);
		getContentPane().add(setOmitComments);

		setOmitComments.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setOmitComments.isSelected()){
					props.setOmitComments(true);
				}else{
					props.setOmitComments(false);
				}
			}
		});	

		//10
		final JCheckBox setOmitXmlDeclaration = new JCheckBox(
				"Omit Xml Declaration");
		setOmitXmlDeclaration.setBackground(new Color(255, 250, 250));
		setOmitXmlDeclaration.setBounds(253, 305, 239, 25);
		getContentPane().add(setOmitXmlDeclaration);

		setOmitXmlDeclaration.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setOmitXmlDeclaration.isSelected()){
					props.setOmitXmlDeclaration(true);
				}else{
					props.setOmitXmlDeclaration(false);
				}
			}
		});	

		//11
		final JCheckBox setOmitDoctypeDeclaration = new JCheckBox(
				"Omit Doctype Declaration");
		setOmitDoctypeDeclaration.setBackground(new Color(255, 250, 250));
		setOmitDoctypeDeclaration.setSelected(true);
		setOmitDoctypeDeclaration.setBounds(253, 333, 239, 25);
		getContentPane().add(setOmitDoctypeDeclaration);

		setOmitDoctypeDeclaration.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setOmitDoctypeDeclaration.isSelected()){
					props.setOmitDoctypeDeclaration(true);
				}else{
					props.setOmitDoctypeDeclaration(false);
				}
			}
		});

		//12
		final JCheckBox setUseEmptyElementTags = new JCheckBox(
				"Use Empty Element Tags");
		setUseEmptyElementTags.setBackground(new Color(255, 250, 250));
		setUseEmptyElementTags.setSelected(true);
		setUseEmptyElementTags.setBounds(253, 361, 239, 30);
		getContentPane().add(setUseEmptyElementTags);

		setUseEmptyElementTags.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setUseEmptyElementTags.isSelected()){
					props.setUseEmptyElementTags(true);
				}else{
					props.setUseEmptyElementTags(false);
				}

			}
		});

		//13
		final JCheckBox setAllowMultiWordAttributes = new JCheckBox(
				"Allow Multi-Word Attributes");
		setAllowMultiWordAttributes.setBackground(new Color(255, 250, 250));
		setAllowMultiWordAttributes.setSelected(true);
		setAllowMultiWordAttributes.setBounds(494, 222, 238, 25);
		getContentPane().add(setAllowMultiWordAttributes);

		setAllowMultiWordAttributes.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setAllowMultiWordAttributes.isSelected()){
					props.setAllowMultiWordAttributes(true);
				}else{
					props.setAllowMultiWordAttributes(false);
				}
			}
		});

		//14
		final JCheckBox setAllowHtmlInsideAttributes = new JCheckBox(
				"Allow HTML Inside Attributes");
		setAllowHtmlInsideAttributes.setBackground(new Color(255, 250, 250));
		setAllowHtmlInsideAttributes.setBounds(494, 250, 238, 25);
		getContentPane().add(setAllowHtmlInsideAttributes);

		setAllowHtmlInsideAttributes.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setAllowHtmlInsideAttributes.isSelected()){
					props.setAllowHtmlInsideAttributes(true);
				}else{
					props.setAllowHtmlInsideAttributes(false);
				}
			}
		});	

		//15
		final JCheckBox setIgnoreQuestAndExclam = new JCheckBox(
				"Ignore Quest & Exclam");
		setIgnoreQuestAndExclam.setBackground(new Color(255, 250, 250));
		setIgnoreQuestAndExclam.setSelected(true);
		setIgnoreQuestAndExclam.setBounds(494, 306, 238, 28);
		getContentPane().add(setIgnoreQuestAndExclam);

		setIgnoreQuestAndExclam.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setIgnoreQuestAndExclam.isSelected()){
					props.setIgnoreQuestAndExclam(true);
				}else{
					props.setIgnoreQuestAndExclam(false);
				}
			}
		});

		//16
		final JCheckBox setNamespacesAware = new JCheckBox(
				"Namespaces Aware");
		setNamespacesAware.setBackground(new Color(255, 250, 250));
		setNamespacesAware.setBounds(494, 278, 238, 25);
		getContentPane().add(setNamespacesAware);

		setNamespacesAware.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				if (setNamespacesAware.isSelected()){
					props.setNamespacesAware(true);
				}else{
					props.setNamespacesAware(false);
				}
			}
		});

		//PROPERTIES link


		JLabel lblHelp = new JLabel("Settings behaviour explained");
		lblHelp.setToolTipText("Cick here for details");
		lblHelp.setBackground(new Color(255, 250, 250));
		lblHelp.setForeground(Color.BLUE);
		lblHelp.setBounds(498, 372, 234, 29);
		getContentPane().add(lblHelp);

		lblHelp.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent me) {  
				setCursor(new Cursor(Cursor.HAND_CURSOR));  
			}  
			public void mouseExited(MouseEvent me) {  
				setCursor(Cursor.getDefaultCursor());  
			}  
			public void mouseClicked(MouseEvent clickLink) {
				try {
					Desktop.getDesktop().browse(new URL("http://htmlcleaner.sourceforge.net/parameters.php").toURI());
				} catch (IOException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}

			}	
		});	


		// CLEAN Button
		JButton btnClean = new JButton("Clean");
		btnClean.setToolTipText("Start the cleaning: generate clean XML");
		btnClean.setFont(new Font("Tahoma", Font.PLAIN, 18));
		btnClean.setBounds(614, 412, 100, 25);
		getContentPane().add(btnClean);





		btnClean.addMouseListener(new MouseAdapter() {
			@SuppressWarnings("deprecation")
			public void mouseClicked(MouseEvent cleanStart) {
				inputSrc = inputText.getText();
				outputSrc = outputText.getText();

				TagNode tagNode;

				if ( inputSrc.startsWith("http://") || inputSrc.startsWith("https://") ) {		// It's a URL

					try {
						if (props.getHtmlVersion()==4)
							tagNode = new HtmlCleaner(Html4TagProvider.INSTANCE,props).clean(new URL(inputSrc), "utf-8");
						else
							tagNode = new HtmlCleaner(props).clean(new URL(inputSrc), "utf-8");
						new PrettyXmlSerializer(props).writeToFile( // OUTPUT
								tagNode, outputSrc, "utf-8");
					} catch (MalformedURLException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					}	


				}else{														// It's a FILE
					try {
				        props.addPruneTagNodeCondition(new TagNodeEmptyContentCondition(props.getTagInfoProvider()));
						if (props.getHtmlVersion()==4)
							tagNode = new HtmlCleaner(Html4TagProvider.INSTANCE,props).clean( // INPUT
									new File(inputSrc), "utf-8");
						else
							tagNode = new HtmlCleaner(props).clean( // INPUT
									new File(inputSrc), "utf-8");

						new PrettyXmlSerializer(props).writeToFile( // OUTPUT
								tagNode, outputSrc, "utf-8");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		setVisible(true); // displaying the window
		setSize(767, 497); // size
		setTitle("HTML Cleaner");
		setIconImage(Toolkit.getDefaultToolkit().getImage("icon.png"));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // enabling exiting on X
		container.setBackground(Color.WHITE);
		
		JLabel lblUse = new JLabel("Use:");
		lblUse.setBounds(498, 339, 27, 20);
		getContentPane().add(lblUse);
		
		String com[]={"Html 4","Html 5"};
		JComboBox comboBox = new JComboBox(com);
		comboBox.setSelectedIndex(1);
		comboBox.setBounds(531, 334, 62, 25);
		getContentPane().add(comboBox);
		
		comboBox.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					String item=(String) e.getItem();
					
					if (item.compareTo("Html 4")==0){
						props.setHtmlVersion(HtmlCleaner.HTML_4);
					}
					else{
						props.setHtmlVersion(HtmlCleaner.HTML_5);
					}
					props.reset();
					setOmitUnknownTags.setSelected(false);
					setTreatUnknownTagsAsContent.setSelected(false);
					setOmitDeprecatedTags.setSelected(false);
					setTreatDeprecatedTagsAsContent.setSelected(false); 
					setOmitComments.setSelected(false);
					setOmitXmlDeclaration.setSelected(false);
					setAllowHtmlInsideAttributes.setSelected(false);
					setNamespacesAware.setSelected(false);
					
			       }
				
			}
		});
		
		
		

	}

	public static void main(String[] args)  {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					new GUI();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});  
	}
}
