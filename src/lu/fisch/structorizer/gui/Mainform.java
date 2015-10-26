/*
    Structorizer
    A little tool which you can use to create Nassi-Schneiderman Diagrams (NSD)

    Copyright (C) 2009  Bob Fisch

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or any
    later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package lu.fisch.structorizer.gui;

/******************************************************************************************************
 *
 *      Author:         Bob Fisch
 *
 *      Description:    This is the main application form.
 *
 ******************************************************************************************************
 *
 *      Revision List
 *
 *      Author          Date			Description
 *      ------			----			-----------
 *      Bob Fisch       2007.12.11      First Issue
 *
 ******************************************************************************************************
 *
 *      Comment:		/
 *
 ******************************************************************************************************///

import java.io.*;

import java.awt.*;
import java.awt.event.*;


import javax.swing.*;

import lu.fisch.structorizer.io.*;
import lu.fisch.structorizer.parsers.*;
import lu.fisch.structorizer.elements.*;

public class Mainform  extends JFrame implements NSDController
{
	public Diagram diagram = null;
	private Menu menu = null;
	private Editor editor = null;
	
	private String lang = "en.txt";
	private String laf = null;
		
	/******************************
 	 * Setup the Mainform
	 ******************************/
	private void create()
	{
		Ini.getInstance();
		/*
		try {
			ClassPathHacker.addFile("Structorizer.app/Contents/Resources/Java/quaqua-filechooser-only.jar");
			UIManager.setLookAndFeel(
									 "ch.randelshofer.quaqua.QuaquaLookAndFeel"
									 );
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}*/
		
		/******************************
		 * Load values from INI
		 ******************************/
		loadFromINI();
		
		/******************************
		 * Some JFrame specific things
		 ******************************/
		// set window title
		setTitle("Structorizer");
		// set layout (OS default)
		setLayout(null);
		// set windows size
		//setSize(550, 550);
		// show form
		setVisible(true);
		// set action to perfom if closed
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// set icon depending on OS ;-)
		String os = System.getProperty("os.name").toLowerCase();
		setIconImage(IconLoader.ico074.getImage());
		if (os.indexOf("windows") != -1) 
		{
			setIconImage(IconLoader.ico074.getImage());
		} 
		else if (os.indexOf("mac") != -1) 
		{
			setIconImage(IconLoader.icoNSD.getImage());
		}
		
		/******************************
		 * Setup the editor
		 ******************************/
		editor = new Editor(this);
		// get reference tot he diagram
		diagram = getEditor().diagram;
		Container container = getContentPane();
                container.setLayout(new BorderLayout());
		container.add(getEditor(),BorderLayout.CENTER);
		
		/******************************
		 * Setup the menu
		 ******************************/
		menu = new Menu(diagram,this);
		setJMenuBar(menu);		
				
		/******************************
		 * Update the buttons and menu
		 ******************************/
		doButtons();
		
		/******************************
		 * Set onClose event
		 ******************************/
		addWindowListener(new WindowAdapter() 
		{  
                          @Override
						  public void windowClosing(WindowEvent e) 
						  {  
                                                    diagram.saveNSD(true);
                                                    saveToINI();
						  }  
						  
                          @Override
						  public void windowOpened(WindowEvent e) 
						  {  
						  //editor.componentResized(null);
						  //editor.revalidate();
						  //repaint();
						  }  

                          @Override
                          public void windowActivated(WindowEvent e)
						  {  
						  //editor.componentResized(null);
						  //editor.revalidate();
						  //repaint();
        					  }

                          @Override
						  public void windowGainedFocus(WindowEvent e) 
						  {  
						  //editor.componentResized(null);
						  //editor.revalidate();
						  //repaint();
						  }  
						  }); 

		/******************************
		 * Load values from INI
		 ******************************/
		loadFromINI();
		setLang(lang);
		
		/******************************
		 * Resize the toolbar
		 ******************************/
		//editor.componentResized(null);
		getEditor().revalidate();
		repaint();
        getEditor().diagram.redraw();
	}
	

	/******************************
	 * Load & save INI-file
	 ******************************/
	public void loadFromINI()
	{
		try
		{
			Ini ini = Ini.getInstance();
			ini.load();
			ini.load();

			double scaleFactor = Double.valueOf(ini.getProperty("scaleFactor","1")).intValue();
                        IconLoader.setScaleFactor(scaleFactor);
			
                        // position
			int top = Integer.valueOf(ini.getProperty("Top","0")).intValue();
			int left = Integer.valueOf(ini.getProperty("Left","0")).intValue();
			int width = Integer.valueOf(ini.getProperty("Width","750")).intValue();
			int height = Integer.valueOf(ini.getProperty("Height","550")).intValue();

                        // reset do defaults if wrong values
                        if (top<0) top=0;
                        if (left<0) left=0;
                        if (width<=0) width=750;
                        if (height<=0) height=550;

			// language	
			lang=ini.getProperty("Lang","en.txt");
                        setLang(lang);
                        
                        // colors
                        Element.loadFromINI();
                        updateColors();
                        
                        // parser
                        D7Parser.loadFromINI();

			// look & feel
			laf=ini.getProperty("laf","Mac OS X");
			setLookAndFeel(laf);
			
			// size
			setPreferredSize(new Dimension(width,height));
			setSize(width,height);
			setLocation(new Point(top,left));
			validate();

			if(diagram!=null) 
			{
				// current directory
				diagram.currentDirectory = new File(ini.getProperty("currentDirectory", System.getProperty("file.separator")));
				
				// din
				if (ini.getProperty("DIN","0").equals("1")) // default = 0
				{
					diagram.setDIN();
				}
				// comments
				if (ini.getProperty("showComments","1").equals("0")) // default = 1
				{
					diagram.setComments(false);
				}
				if (ini.getProperty("switchTextComments","0").equals("1")) // default = 0
				{
					diagram.setToggleTC(true);
				}
				// comments
				if (ini.getProperty("varHightlight","1").equals("1")) // default = 0
				{
					diagram.setHightlightVars(true);
				}
				// analyser
                /*
				if (ini.getProperty("analyser","0").equals("0")) // default = 0
				{
					diagram.setAnalyser(false);
				}
                 * */
			}
			
			// recent files
			try
			{	
				if(diagram!=null)
				{
					for(int i=9;i>=0;i--)
					{
						if(ini.keySet().contains("recent"+i))
						{
							if(!ini.getProperty("recent"+i,"").trim().equals(""))
							{
								diagram.addRecentFile(ini.getProperty("recent"+i,""),false);
							}
						}
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.out.println(e.getMessage());
			}
			
			// analyser
			Root.check1 = ini.getProperty("check1","1").equals("1");
			Root.check2 = ini.getProperty("check2","1").equals("1");
			Root.check3 = ini.getProperty("check3","1").equals("1");
			Root.check4 = ini.getProperty("check4","1").equals("1");
			Root.check5 = ini.getProperty("check5","1").equals("1");
			Root.check6 = ini.getProperty("check6","1").equals("1");
			Root.check7 = ini.getProperty("check7","1").equals("1");
			Root.check8 = ini.getProperty("check8","1").equals("1");
			Root.check9 = ini.getProperty("check9","1").equals("1");
			Root.check10 = ini.getProperty("check10","1").equals("1");
			Root.check11 = ini.getProperty("check11","1").equals("1");
			Root.check12 = ini.getProperty("check12","1").equals("1");
			Root.check13 = ini.getProperty("check13","1").equals("1");

			
			doButtons();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			System.out.println(e);

			setPreferredSize(new Dimension(500,500));
			setSize(500,500);
			setLocation(new Point(0,0));
			validate();
		}
	}

	public void saveToINI()
	{
		try
		{
			Ini ini = Ini.getInstance();
			ini.load();

			// position
			ini.setProperty("Top",Integer.toString(getLocationOnScreen().x));
			ini.setProperty("Left",Integer.toString(getLocationOnScreen().y));
			ini.setProperty("Width",Integer.toString(getWidth()));
			ini.setProperty("Height",Integer.toString(getHeight()));

			// current directory
			if(diagram!=null)
			{
				if(diagram.currentDirectory!=null)
				{
					ini.setProperty("currentDirectory",diagram.currentDirectory.getAbsolutePath());
				}
			}
			
			// language
			ini.setProperty("Lang",lang);
			
			// DIN, comments
			ini.setProperty("DIN",(Element.E_DIN?"1":"0"));
			ini.setProperty("showComments",(Element.E_SHOWCOMMENTS?"1":"0"));
			ini.setProperty("switchTextComments",(Element.E_TOGGLETC?"1":"0"));
			ini.setProperty("varHightlight",(Element.E_VARHIGHLIGHT?"1":"0"));
			//ini.setProperty("analyser",(Element.E_ANALYSER?"1":"0"));
			
			// look and feel
			if(laf!=null)
			{
				ini.setProperty("laf", laf);
			}
			
			// recent files
			if(diagram!=null)
			{
				if(diagram.recentFiles.size()!=0)
				{
					for(int i=0;i<diagram.recentFiles.size();i++)
					{
						//System.out.println(i);
						ini.setProperty("recent"+String.valueOf(i),(String) diagram.recentFiles.get(i));
					} 
				}
			}
			
			ini.save();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
	
	/******************************
	 * This method dispatches the
	 * command to all sublisteners
	 ******************************/
	public void doButtons()
	{
		if(menu!=null)
		{
			menu.doButtonsLocal();
		}
		
		if (getEditor()!=null)
		{
			getEditor().doButtonsLocal();
		}
		
		doButtonsLocal();
	}
	
	public String getLookAndFeel()
	{
		return laf;
	}
	
	public void setLookAndFeel(String _laf)
	{
		if(_laf!=null)
		{
			//System.out.println("Setting: "+_laf);
			laf=_laf;
			
			UIManager.LookAndFeelInfo plafs[] = UIManager.getInstalledLookAndFeels();
			for(int j = 0; j < plafs.length; ++j)
			{
				//System.out.println("Listing: "+plafs[j].getName());
				if(_laf.equals(plafs[j].getName()))
				{
					//System.out.println("Found: "+plafs[j].getName());
					try
					{
						UIManager.setLookAndFeel(plafs[j].getClassName());
						SwingUtilities.updateComponentTreeUI(this);
					}
					catch (Exception e)
					{
						// show error
						JOptionPane.showOptionDialog(null,e.getMessage(),
													 "Error ...",
													 JOptionPane.OK_OPTION,JOptionPane.ERROR_MESSAGE,null,null,null);
					}
				}
			}
		}
	}
	
	public void setLang(String _langfile)
	{
		lang=_langfile;
		
		if(menu!=null)
		{
			menu.setLangLocal(_langfile);
		}
		
		if (getEditor()!=null)
		{
			getEditor().setLangLocal(_langfile);
		}
	}
	
    public void setLangLocal(String _langfile) {}

    public String getLang()
    {
            return lang;
    }

    public void savePreferences()
    {
            //System.out.println("Saving");
            saveToINI();
            D7Parser.saveToINI();
            Element.saveToINI();
            Root.saveToINI();
    }

    /******************************
     * Local listener (empty)
     ******************************/
    public void doButtonsLocal()
    {
        boolean done=false;
        if(diagram!=null)
            if(diagram.root!=null)
                if(diagram.root.filename!=null)
                    if(!diagram.root.filename.equals(""))
                 {
                    File f = new File(diagram.root.filename);
                    this.setTitle("Structorizer - "+f.getName());
                    done=true;
                 }
        if(done==false) this.setTitle("Structorizer");
    }

    public void updateColors() 
    {
        if(editor!=null)
            editor.updateColors();
    }
		
    /******************************
     * Constructor
     ******************************/
    public Mainform()
    {
        super();
        create();
    }

    /**
     * @return the editor
     */
    public Editor getEditor()
    {
        return editor;
    }

    public JFrame getFrame()
    {
        return this;
    }
	
}