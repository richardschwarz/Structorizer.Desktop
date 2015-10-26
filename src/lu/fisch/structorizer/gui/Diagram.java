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
 *      Description:    This class represents the visual diagram itself.
 *
 ******************************************************************************************************
 *
 *      Revision List
 *
 *      Author          Date			Description
 *      ------		----			-----------
 *      Bob Fisch       2007.12.09      First Issue
 *
 ******************************************************************************************************
 *
 *      Comment:		/
     *
 ******************************************************************************************************///

import java.awt.*;
import java.awt.Font;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.print.*;
import java.awt.datatransfer.*;

import net.iharder.dnd.*; //http://iharder.sourceforge.net/current/java/filedrop/

import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.imageio.*;

import org.freehep.graphicsio.emf.*;
import org.freehep.graphicsio.pdf.*;
import org.freehep.graphicsio.swf.*;
//import org.freehep.graphicsio.svg.*;	// => does not work!

import lu.fisch.graphics.*;
import lu.fisch.utils.*;
import lu.fisch.structorizer.parsers.*;
import lu.fisch.structorizer.io.*;
import lu.fisch.structorizer.generators.*;
import lu.fisch.structorizer.elements.*;
import static lu.fisch.structorizer.elements.Element.E_TOGGLETC;
import lu.fisch.structorizer.executor.Executor;
import lu.fisch.turtle.TurtleBox;

import org.freehep.graphicsio.svg.SVGGraphics2D;

public class Diagram extends JPanel implements MouseMotionListener, MouseListener, Printable, MouseWheelListener {

    public Root root = new Root();
    private TurtleBox turtle = null; //

    private Element selected = null;

    private boolean mouseMove = false;
    private int mouseX = -1;
    private int mouseY = -1;
    private Element selectedDown = null;
    private Element selectedUp = null;
    private Element selectedMoved = null;
    private int selX = -1;
    private int selY = -1;
    private int mX = -1;
    private int mY = -1;

    private NSDController NSDControl = null;

    private JList errorlist = null;

    private Element eCopy = null;

    public File currentDirectory = new File(System.getProperty("user.home"));
    public File lastExportDir = null;

    // recently opened files
    protected Vector<String> recentFiles = new Vector<String>();

    // popup for comment
    private JLabel lblPop = new JLabel("",SwingConstants.CENTER);
    private JPopupMenu pop = new JPopupMenu();

    // toolbar management
    public Vector<MyToolbar> toolbars = new Vector<MyToolbar>();


    public boolean getAnalyser()
    {
        return Element.E_ANALYSER;
    }

    private void create(String _string)
    {
		// load different things from INI-file
		Element.loadFromINI();
		D7Parser.loadFromINI();

		this.addMouseListener(this);
		this.addMouseMotionListener(this);
                this.addMouseWheelListener(this);


		new  FileDrop( this, new FileDrop.Listener()
								{
									public void  filesDropped( java.io.File[] files )
									{
										boolean found = false;
										for (int i = 0; i < files.length; i++)
										{
											String filename = files[i].toString();

					  if(filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".nsd"))
					  {
					  /*
					  // only save if something has been changed
					  saveNSD(true);

					  // load the new file
					  NSDParser parser = new NSDParser();
					  root = parser.parse(filename);
					  root.filename=filename;
					  currentDirectory = new File(filename);
					  redraw();*/
					  openNSD(filename);
					  }
					  else if (
							   (filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".mod"))
							   ||
							   (filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".pas"))
							   ||
							   (filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".dpr"))
							   ||
							   (filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".lpr"))
							   )
					  {
					  // only save if something has been changed
					  saveNSD(true);
					  // load and parse source-code
					  D7Parser d7 = new D7Parser("D7Grammar.cgt");
					  Root rootNew = d7.parse(filename);
					  if (d7.error.equals(""))
					  {
						boolean hil = root.hightlightVars;
						root = rootNew;
						root.hightlightVars=hil;
						currentDirectory = new File(filename);
						//System.out.println(root.getFullText().getText());
						root.getVarNames();
						analyse();
					  }
					  else
					  {
						// show error
					  JOptionPane.showOptionDialog(null,d7.error,
												   "Parser Error",
												   JOptionPane.OK_OPTION,JOptionPane.ERROR_MESSAGE,null,null,null);
					  }

					  redraw();
					  }


										}
									}
								}
						);

		root.setText(StringList.getNew(_string));

		// popup for comment
		JPanel jp = new JPanel();
		jp.setOpaque(true);
		lblPop.setPreferredSize(new Dimension(30,12));
		jp.add(lblPop);
		pop.add(jp);
	}

	public void hideComments()
	{
		pop.setVisible(false);
	}

	public void mouseMoved(MouseEvent e)
	{
        if(Element.E_TOGGLETC) root.setSwitchTextAndComments(true);
		if(e.getSource()==this && NSDControl!=null)
		{
			if (Element.E_SHOWCOMMENTS==true && ((Editor) NSDControl).popup.isVisible()==false)
			{
				Element selEle = root.getElementByCoord(e.getX(),e.getY());

				if (selEle!=null)
				{
					if (!selEle.getComment().getText().trim().equals(""))
					{
						if(!lblPop.getText().equals("<html>"+BString.replace(BString.encodeToHtml(selEle.getComment().getText()),"\n","<br>")+"</html>"))
						{
							lblPop.setText("<html>"+BString.replace(BString.encodeToHtml(selEle.getComment().getText()),"\n","<br>")+"</html>");
						}
						int maxWidth = 0;
						int si = 0;
						for(int i=0;i<selEle.getComment().count();i++)
						{
							if(maxWidth<selEle.getComment().get(i).length())
							{
								maxWidth=selEle.getComment().get(i).length();
								si=i;
							}
						}
						lblPop.setPreferredSize(new Dimension(8+lblPop.getFontMetrics(lblPop.getFont()).stringWidth(selEle.getComment().get(si)),
															  selEle.getComment().count()*16));

						int x = ((JComponent) e.getSource()).getLocationOnScreen().getLocation().x;
						int y = ((JComponent) e.getSource()).getLocationOnScreen().getLocation().y;
						pop.setLocation(x+e.getX(),
										y+e.getY()+16);
						pop.setVisible(true);
					}
					else
					{
						pop.setVisible(false);
					}
				}
				else
				{
					pop.setVisible(false);
				}
			}
			else
			{
				pop.setVisible(false);
			}
		}
        if(Element.E_TOGGLETC) root.setSwitchTextAndComments(false);
	}

	public void mouseDragged(MouseEvent e)
	{
		if(e.getSource()==this)
		{
			Element bSome = root.selectElementByCoord(e.getX(),e.getY());

			if(bSome!=null)
			{
                                mX = e.getX();
                                mY = e.getY();
                                //System.out.println("DRAGGED "+mX+" "+mY);
                            
				bSome.setSelected(true);
				selectedDown.setSelected(true);

                                boolean doRedraw = false;
                                
				if((selectedDown!=null) && (e.getX()!=mouseX) && (e.getY()!=mouseY) && (selectedMoved!=bSome))
				{
					mouseMove=true;
					if(selectedDown.getClass().getSimpleName().equals("Root") ||
					   selectedDown.getClass().getSimpleName().equals("Subqueue") ||
					   bSome.getClass().getSimpleName().equals("Root")||
					   root.checkChild(bSome, selectedDown))
					{
						Element.E_DRAWCOLOR=Color.RED;
					}
					else
					{
						Element.E_DRAWCOLOR=Color.GREEN;
					}
					/*
					 selectedDown.draw(new Canvas((Graphics2D)this.getGraphics()), selectedDown.rect);
					 if(bSome!=null)
					 {
					 bSome.draw(new Canvas((Graphics2D)this.getGraphics()), bSome.rect);

					 }
					 */
					doRedraw= true;
				}
                                
                                if(selX!=-1 && selY!=-1)
                                {
                                    doRedraw= true;
                                }
                                
                                if(doRedraw)
                                    redraw();

			}
			selectedMoved = bSome;
		}
	}

	public void mousePressed(MouseEvent e)
	{
		if(e.getSource()==this)
		{
			//System.out.println("PRessed");
                        mouseX = e.getX();
			mouseY = e.getY();

			Element.E_DRAWCOLOR=Color.YELLOW;
			Element ele = root.selectElementByCoord(e.getX(),e.getY());

                        if(ele!=null)
                        {
                            mX = mouseX;
                            mY = mouseY;
                            selX = mouseX-ele.getRect().left;
                            selY = mouseY-ele.getRect().top;
                        }

                        if (ele!=null && ele!=selected)
			{
				ele.setSelected(true);
				selected=ele;
				if(selectedDown!=ele)
				{
					redraw();
				}
				selectedDown=ele;
				selectedUp=ele;
			}

			if(selected!=null)
			{
				if ( !selected.getClass().getSimpleName().equals("Subqueue") &&
					!selected.getClass().getSimpleName().equals("Root"))
				{
					mouseMove=false;
				}
			}

			if(NSDControl!=null) NSDControl.doButtons();
		}
    }

    public void mouseReleased(MouseEvent e)
	{
		if(e.getSource()==this)
		{
                        //System.out.println("Released");
			boolean doDraw = false;

                        if(selX!=-1 && selY!=-1 && selectedDown!=null)
                        {
                            selX = -1;
                            selY = -1;
                            doDraw=true;
                        }

                        if ((mouseMove==true) && (selectedDown!=null))
			{
				Element.E_DRAWCOLOR=Color.YELLOW;
				if ( !selectedDown.getClass().getSimpleName().equals("Subqueue") &&
					!selectedDown.getClass().getSimpleName().equals("Root"))
				{
					selectedUp = root.selectElementByCoord(e.getX(),e.getY());
					if(selectedUp!=null)
					{
						selectedUp.setSelected(false);
						if( !selectedUp.getClass().getSimpleName().equals("Root") &&
						   selectedUp!=selectedDown &&
						   root.checkChild(selectedUp,selectedDown)==false
						   )
						{
							root.addUndo();
							NSDControl.doButtons();

							root.removeElement(selectedDown);
							selectedUp.setSelected(false);
							root.addAfter(selectedUp,selectedDown);
							selectedDown.setSelected(true);
							doDraw=true;
						}
						else
						{
							selectedUp.setSelected(false);
							selectedDown.setSelected(true);
							doDraw=true;
						}
					}
				}
				else
				{
					selectedUp = root.selectElementByCoord(e.getX(),e.getY());
                                        if(selectedUp!=null) selectedUp.setSelected(false);
					doDraw=true;
				}
			}

			mouseMove=false;

			if(doDraw==true)
			{
				redraw();
				analyse();
			}

			if(NSDControl!=null) NSDControl.doButtons();
		}
	}

    public void mouseEntered(MouseEvent e)
	{
	}

    public void mouseExited(MouseEvent e)
	{
	}

    public void mouseClicked(MouseEvent e)
	{
                // select the element
		if (e.getClickCount() == 1)
		{
			if (e.getSource()==this)
			{
                                //System.out.println("Clicked");
                                /*Element selly = root.selectElementByCoord(e.getX(),e.getY());
                                if(selly!=selected && selected!=null) 
                                {
                                    selected.setSelected(false);
                                }
                                selected=selly;

                                // redra the diagram
                                //redraw();
                                // do the button thing
                                if(NSDControl!=null) NSDControl.doButtons();
/**/
                               /*
                                // select the element
				Element selly = root.selectElementByCoord(e.getX(),e.getY());
				if(selected!=selly)
				{
                                        selected=selly;
                                        if(selected!=null)
                                        {
        					selected.setSelected(true);
                                        }
                                        // redra the diagram
                                        redraw();
                                        // do the button thing
                                        if(NSDControl!=null) NSDControl.doButtons();
				}*/
			}
			else
			{
                                // an error has been selected
				if(errorlist.getSelectedIndex()!=-1)
				{
                                        // get the selected error
					Element ele = ((DetectedError) root.errors.get(errorlist.getSelectedIndex())).getElement();
					if(ele!=null)
					{
                                                // deselect any previous selected element
						if(selected!=null) {selected.selected=false;}
                                                // select the new one
						selected = ele;
						ele.selected=true;
                                                // redraw the diagram
						redraw();
                                                // do the button thing
						if(NSDControl!=null) NSDControl.doButtons();
					}
				}
			}
		}
                // edit the element
		else if ((e.getClickCount() == 2))
		{
			if(e.getSource()==this)
			{
                                // selected the right element
				//selected = root.selectElementByCoord(e.getX(),e.getY());
                                if(selected!=null)
                                {
                                    // edit it
                                    editNSD();
                                    selected.setSelected(true);
                                    redraw();
                                    // do the button thing
                                    if(NSDControl!=null) NSDControl.doButtons();
                                }
			}
			else
			{
                                // the error list has been clicked
				if(errorlist.getSelectedIndex()!=-1)
				{
                                        // select the right element
					selected = ((DetectedError) root.errors.get(errorlist.getSelectedIndex())).getElement();
                                        // edit it
					editNSD();
                                        // do the button things
					if(NSDControl!=null) NSDControl.doButtons();
				}
			}
		}
	}


	public void redraw()
	{
                if (root.hightlightVars==true)
		{
			root.getVarNames();
		}
                
                Rect rect = root.prepareDraw(this.getGraphics());
                Dimension d = new Dimension(rect.right-rect.left,rect.bottom-rect.top);
		this.setPreferredSize(d);
		//this.setSize(d);
		this.setMaximumSize(d);
		this.setMinimumSize(d);
		//this.setSize(new Dimension(rect.right-rect.left,rect.bottom-rect.top));
		//this.validate();

                ((JViewport) this.getParent()).revalidate();
                
		//redraw(this.getGraphics());
                this.repaint();
	}

	public void redraw(Graphics _g)
	{
        if (Element.E_TOGGLETC) root.setSwitchTextAndComments(true);
		root.draw(_g);
                
		lu.fisch.graphics.Canvas canvas = new lu.fisch.graphics.Canvas((Graphics2D) _g);
		Rect rect;
		rect = new Rect(root.width+1,0,this.getWidth(),this.getHeight());
		canvas.setColor(Color.LIGHT_GRAY);
		canvas.fillRect(rect);
		rect = new Rect(0,root.height+1,this.getWidth(),this.getHeight());
		canvas.setColor(Color.LIGHT_GRAY);
		canvas.fillRect(rect);
		rect = new Rect(root.width+1,root.height+1,this.getWidth(),this.getHeight());
		canvas.setColor(Color.LIGHT_GRAY);
		canvas.fillRect(rect);

        
                // draw dragged element
                if(selX!=-1 && selY!=-1 && selectedDown!=null && mX!=mouseX && mY!=mouseY)
                {
                    _g.setColor(Color.BLACK);
                    rect = selectedDown.getRect();
                    Rect copyRect = rect.copy();
                    int w = rect.right-rect.left;
                    int h = rect.bottom-rect.top;
                    rect.left=mX-selX;
                    rect.top =mY-selY;
                    rect.right=rect.left+w;
                    rect.bottom=rect.top+h;
                    ((Graphics2D)_g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                    selectedDown.draw(canvas, rect);
                    ((Graphics2D)_g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                    selectedDown.rect=copyRect;
                    //_g.drawRect(mX-selX, mY-selY, w, h);
                }/**/

                if (Element.E_TOGGLETC) root.setSwitchTextAndComments(false);
}

        @Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		if(root!=null)
		{
			redraw(g);
		}
	}

	public Element getSelected()
	{
		return selected;
	}

	/**
	* Method: print <p>
	*
	* This class is responsible for rendering a page using
	* the provided parameters. The result will be a grid
	* where each cell will be half an inch by half an inch.
	*
	* @param g a value of type Graphics
	* @param pageFormat a value of type PageFormat
	* @param page a value of type int
	* @return a value of type int
	*/
	public int print (Graphics g, PageFormat pageFormat, int page)
	{  
		if (page == 0)
		{
			Graphics2D g2d = (Graphics2D) g;

			g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

			/*if (pageFormat.getOrientation() != PageFormat.PORTRAIT)
			{*/
				double sX = (pageFormat.getImageableWidth()-1)/root.width;
				double sY = (pageFormat.getImageableHeight()-1)/root.height;
				double sca = Math.min(sX,sY);
				if (sca>1) {sca=1;}
				g2d.scale(sca,sca);
			/*}
			else
			{
				double sX = (pageFormat.getImageableWidth()-1)/root.width;
				double sY = (pageFormat.getImageableHeight()-1)/root.height;
				double sca = Math.min(sX,sY);
				//if (sca>1) {sca=1;}
				g2d.scale(sca,sca);
			}*/

			root.draw(g);

			return (PAGE_EXISTS);
		}
		else
		{
			return (NO_SUCH_PAGE);
		}
	}

	/*****************************************
	 * New method
	 *****************************************/
	public void newNSD()
	{
		// only save if something has been changed
		saveNSD(true);

		// create en empty diagram
		boolean HV = root.hightlightVars;
		root = new Root();
		root.hightlightVars=HV;
		root.hasChanged=true;
		redraw();
		analyse();
	}


	/*****************************************
	 * Open method
	 *****************************************/
	public void openNSD()
	{
		// only save if something has been changed
		saveNSD(true);

		// open an existing file
		// create dialog
		JFileChooser dlgOpen = new JFileChooser();
		dlgOpen.setDialogTitle("Open file ...");
		// set directory
		if(root.getFile()!=null)
		{
			dlgOpen.setCurrentDirectory(root.getFile());
		}
		else
		{
			dlgOpen.setCurrentDirectory(currentDirectory);
		}
		// config dialogue
		dlgOpen.addChoosableFileFilter(new StructogramFilter());
		// show & get result
		int result = dlgOpen.showOpenDialog(this);
		// react on result
		if (result == JFileChooser.APPROVE_OPTION)
		{
			/*
			NSDParser parser = new NSDParser();
			root = parser.parse(dlgOpen.getSelectedFile().toURI().toString());
			root.filename=dlgOpen.getSelectedFile().getAbsoluteFile().toString();
			currentDirectory = new File(root.filename);
			redraw();
			*/
			openNSD(dlgOpen.getSelectedFile().getAbsoluteFile().toString());
		}
	}

	public void openNSD(String _filename)
	{
		try
		{
			File f = new File(_filename);
			//System.out.println(f.toURI().toString());
			if(f.exists()==true)
			{
				// only save if something has been changed
				saveNSD(true);

				// open an existing file
				NSDParser parser = new NSDParser();
				boolean hil = root.hightlightVars;
				root = parser.parse(f.toURI().toString());
				root.hightlightVars=hil;
				root.filename=_filename;
				currentDirectory = new File(root.filename);
				addRecentFile(root.filename);
				redraw();
				analyse();
			}
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
	}


	/*****************************************
	 * SaveAs method
	 *****************************************/
	public void saveAsNSD()
	{
		JFileChooser dlgSave = new JFileChooser();
		dlgSave.setDialogTitle("Save file as ...");
		// set directory
		if(root.getFile()!=null)
		{
			dlgSave.setCurrentDirectory(root.getFile());
		}
		else
		{
			dlgSave.setCurrentDirectory(currentDirectory);
		}

		// propose name
		String nsdName = root.getText().get(0);
		nsdName.replace(':', '_');
		if(nsdName.indexOf(" (")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf(" ("));}
		if(nsdName.indexOf("(")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf("("));}
		dlgSave.setSelectedFile(new File(nsdName));

		dlgSave.addChoosableFileFilter(new StructogramFilter());
		int result = dlgSave.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION)
		{
			root.filename=dlgSave.getSelectedFile().getAbsoluteFile().toString();
			if(!root.filename.substring(root.filename.length()-4, root.filename.length()).toLowerCase().equals(".nsd"))
			{
				root.filename+=".nsd";
			}

                        File f = new File(root.filename);
                        boolean writeNow = true;
                        if(f.exists())
                        {
                            writeNow=false;
                            int res = JOptionPane.showConfirmDialog(
                                this,"A file with the specified name exists.\nDo you want to owerwrite it?",
                                "Owerwrite?",JOptionPane.YES_NO_OPTION);
                            if(res==JOptionPane.YES_OPTION) writeNow=true;
                        }

                        if(writeNow==false)
                        {
                            JOptionPane.showMessageDialog(this,"Your file has not been saved. Please repeat the save operation!");
                        }
                        else
                        {

                            try
                            {
                                    FileOutputStream fos = new FileOutputStream(root.filename);
                                    Writer out = new OutputStreamWriter(fos, "UTF8");
                                    XmlGenerator xmlgen = new XmlGenerator();
                                    out.write(xmlgen.generateCode(root,"\t"));
                                    out.close();
                                    /*
                                    BTextfile outp = new BTextfile(root.filename);
                                    outp.rewrite();
                                    XmlGenerator xmlgen = new XmlGenerator();
                                    outp.write(xmlgen.generateCode(root,"\t"));
                                    //outp.write(diagram.root.getXML());
                                    outp.close();
                                    /**/

                                    root.hasChanged=false;
                                    addRecentFile(root.filename);
                            }
                            catch(Exception e)
                            {
                                    JOptionPane.showOptionDialog(this,"Error while saving the file!","Error",JOptionPane.OK_OPTION,JOptionPane.ERROR_MESSAGE,null,null,null);
                            }
                         }
		}
	}

	/*****************************************
	 * Save method
	 *****************************************/
	public void saveNSD(boolean _checkChanged)
	{
		// only save if something has been changed
		if(root.hasChanged==true)
		{
			int res = 0;

			if(_checkChanged==true)
			{
				res = JOptionPane.showOptionDialog(this,
												   "Do you want to save the current NSD-File?",
												   "Question",
												   JOptionPane.YES_NO_OPTION,
												   JOptionPane.QUESTION_MESSAGE,
												   null,null,null);
			}

			if (res==0)
			{
				// if root has not yet been saved
				boolean saveIt = true;

				if(root.filename.equals(""))
				{
					JFileChooser dlgSave = new JFileChooser();
					dlgSave.setDialogTitle("Save file ...");
					// set directory
					if(root.getFile()!=null)
					{
						dlgSave.setCurrentDirectory(root.getFile());
					}
					else
					{
						dlgSave.setCurrentDirectory(currentDirectory);
					}

					// propose name
					String nsdName = root.getText().get(0);
					nsdName.replace(':', '_');
					if(nsdName.indexOf(" (")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf(" ("));}
					if(nsdName.indexOf("(")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf("("));}
					dlgSave.setSelectedFile(new File(nsdName));

					dlgSave.addChoosableFileFilter(new StructogramFilter());
					int result = dlgSave.showSaveDialog(this);

					/***** file_exists check here!
					/*
					 if (exportFile.exists()) {
					 returnval = JOptionPane.showConfirmDialog(frame, "File "
					 + exportFile.getName()
					 + " exists. Do you want to continue ?", "File exists",
					 JOptionPane.OK_CANCEL_OPTION,
					 JOptionPane.WARNING_MESSAGE);
					 }

					 if(file.exists())
					 {
					 JOptionPane.showMessageDialog(null,file);
					 int response = JOptionPane.showConfirmDialog (null,
					 "Overwrite existing file?","Confirm Overwrite",
					 JOptionPane.OK_CANCEL_OPTION,
					 JOptionPane.QUESTION_MESSAGE);
					 if (response == JOptionPane.CANCEL_OPTION)
					 {
					 return;
					 }
					 else
					 */

					if (result == JFileChooser.APPROVE_OPTION)
					{
						root.filename=dlgSave.getSelectedFile().getAbsoluteFile().toString();
						if(!root.filename.substring(root.filename.length()-4, root.filename.length()).toLowerCase().equals(".nsd"))
						{
							root.filename+=".nsd";
						}
					}
					else
					{
						saveIt = false;
					}
				}

				if (saveIt == true)
				{
					try
					{
                                                FileOutputStream fos = new FileOutputStream(root.filename);
                                                Writer out = new OutputStreamWriter(fos, "UTF8");
                                                XmlGenerator xmlgen = new XmlGenerator();
                                                out.write(xmlgen.generateCode(root,"\t"));
                                                out.close();
                                                /*
                                                BTextfile outp = new BTextfile(root.filename);
						outp.rewrite();
						XmlGenerator xmlgen = new XmlGenerator();
						outp.write(xmlgen.generateCode(root,"\t"));
						//outp.write(diagram.root.getXML());
						outp.close();/**/

						root.hasChanged=false;
						addRecentFile(root.filename);
					}
					catch(Exception e)
					{
						JOptionPane.showOptionDialog(this,"Error while saving the file!\n"+e.getMessage(),"Error",JOptionPane.OK_OPTION,JOptionPane.ERROR_MESSAGE,null,null,null);
					}
				}
			}
		}
	}

	/*****************************************
	 * Undo method
	 *****************************************/
	public void undoNSD()
	{
		root.undo();
		redraw();
		analyse();
	}

	/*****************************************
	 * Redo method
	 *****************************************/
	public void redoNSD()
	{
		root.redo();
		redraw();
		analyse();
	}

	public boolean canPaste()
	{
		boolean cond = (eCopy!=null && selected!=null);
		if(selected!=null)
		{
			cond = cond && !selected.getClass().getSimpleName().equals("Root");
		}

		return cond;
	}

	public boolean canCutCopy()
	{
		boolean cond = (selected!=null);
		if(selected!=null)
		{
			cond = cond && !selected.getClass().getSimpleName().equals("Root");
			cond = cond && !selected.getClass().getSimpleName().equals("Subqueue");
		}

		return cond;
	}

	/*****************************************
	 * setColor method
	 *****************************************/
	public void setColor(Color _color)
	{
		if(getSelected()!=null)
		{
			getSelected().setColor(_color);
			//getSelected().setSelected(false);
			//selected=null;
			redraw();
		}
	}


	/*****************************************
	 * Copy method
	 *****************************************/
	public void copyNSD()
	{
		if (selected!=null)
		{
			eCopy = selected.copy();
		}
	}

	/*****************************************
	 * cut method
	 *****************************************/
	public void cutNSD()
	{
		if (selected!=null)
		{
			eCopy = selected.copy();
			eCopy.setSelected(false);
			root.addUndo();
			root.removeElement(selected);
			root.hasChanged=true;
			redraw();
			selected=null;
			analyse();
		}
	}

	/*****************************************
	 * paste method
	 *****************************************/
	public void pasteNSD()
	{
		if (selected!=null && eCopy!=null)
		{
			root.addUndo();
			selected.setSelected(false);
			Element nE = eCopy.copy();
			nE.setSelected(true);
			root.addAfter(selected,nE);
			root.hasChanged=true;
			selected=nE;
			redraw();
			analyse();
		}
	}

	/*****************************************
	 * edit method
	 *****************************************/
	public void editNSD()
	{
		Element element = getSelected();
		if(element!=null)
		{
			if (element.getClass().getSimpleName().equals("Subqueue"))
			{
				EditData data = new EditData();
				data.title="Add new instruction ...";

				showInputBox(data);

				if(data.result==true)
				{
					Element ele = new Instruction(data.text.getText());
					ele.setComment(data.comment.getText());
					root.addUndo();
					root.hasChanged=true;
					((Subqueue) element).addElement(ele);
					ele.setSelected(true);
					selected=ele;
					redraw();
				}
			}
			else
			{
				EditData data = new EditData();
				data.title="Edit element ...";
				data.text.setText(element.getText().getText());
				data.comment.setText(element.getComment().getText());

				showInputBox(data);

				if(data.result==true)
				{
					if (!element.getClass().getSimpleName().equals("Root"))
					{
						root.addUndo();
					}
					if (!(element instanceof Forever))
					{
						element.setText(data.text.getText());
					}
					element.setComment(data.comment.getText());
					root.hasChanged=true;
					redraw();
				}
			}

			analyse();
		}
	}

	/*****************************************
	 * moveUp method
	 *****************************************/
	public void moveUpNSD()
	{
		root.addUndo();
		root.moveUp(getSelected());
		redraw();
		analyse();
	}

	/*****************************************
	 * moveDown method
	 *****************************************/
	public void moveDownNSD()
	{
		root.addUndo();
		root.moveDown(getSelected());
		redraw();
		analyse();
	}

	/*****************************************
	 * delete method
	 *****************************************/
	public void deleteNSD()
	{
		root.addUndo();
		root.removeElement(getSelected());
		redraw();
		analyse();
	}

	public void addNewElement(Element _ele, String _title, String _pre, boolean _after)
	{
		if (getSelected()!=null)
		{
			EditData data = new EditData();
			data.title=_title;
			data.text.setText(_pre);
			showInputBox(data);
			if(data.result == true)
			{
				if (!(_ele instanceof Forever))
				{
					_ele.setText(data.text.getText());
				}
				_ele.setComment(data.comment.getText());
				root.addUndo();
				if(_after==true)
				{
					root.addAfter(getSelected(),_ele);
				}
				else
				{
					root.addBefore(getSelected(),_ele);
				}
				_ele.setSelected(true);
				selected=_ele;
				redraw();
				analyse();
			}
		}
	}


	/*****************************************
	 * print method
	 *****************************************/
	public void printNSD()
	{
		/*
		// printing support
		//--- Create a printerJob object
		PrinterJob printJob = PrinterJob.getPrinterJob ();
		//--- Set the printable class to this one since we
		//--- are implementing the Printable interface
		printJob.setPrintable (this);
		//--- Show a print dialog to the user. If the user
		//--- clicks the print button, then print, otherwise
		//--- cancel the print job
		if (printJob.printDialog())
		{
			try
			{
				printJob.print();
			}
			catch (Exception PrintException)
			{
				PrintException.printStackTrace();
			}
		}
		*/
		//PrintPreview.print(this);
		/*
		PrintPreview pp = new PrintPreview(this,"Print Previwe");
		Point p = getLocationOnScreen();
		pp.setLocation(Math.round(p.x+(getVisibleRect().width-pp.getWidth())/2), Math.round(p.y)+(getVisibleRect().height-pp.getHeight())/2);
		pp.setVisible(true);
		*/
		PrintPreview pp = new PrintPreview(NSDControl.getFrame(),this);
		pp.setLang(NSDControl.getLang());
		Point p = getLocationOnScreen();
		pp.setLocation(Math.round(p.x+(getVisibleRect().width-pp.getWidth())/2+this.getVisibleRect().x),
					   Math.round(p.y)+(getVisibleRect().height-pp.getHeight())/2+this.getVisibleRect().y);
		pp.setVisible(true);
	}

	/*****************************************
	 * about method
	 *****************************************/
	public void aboutNSD()
	{
		About about = new About(NSDControl.getFrame());
		Point p = getLocationOnScreen();
		about.setLocation(Math.round(p.x+(getVisibleRect().width-about.getWidth())/2+this.getVisibleRect().x),
						  Math.round(p.y)+(getVisibleRect().height-about.getHeight())/2+this.getVisibleRect().y);
		about.setLang(NSDControl.getLang());
		about.setVisible(true);
	}

	/*****************************************
	 * export picture method
	 *****************************************/
        public void exportPNGmulti()
        {

		root.selectElementByCoord(-1,-1);
		redraw();

		JFileChooser dlgSave = new JFileChooser("Export diagram as Multi-PNG ...");
		// set directory
		if (lastExportDir!=null)
                {
                    dlgSave.setCurrentDirectory(lastExportDir);
                }
                else if(root.getFile()!=null)
		{
			dlgSave.setCurrentDirectory(root.getFile());
		}
		else
		{
			dlgSave.setCurrentDirectory(currentDirectory);
		}
		// propose name
		String nsdName = root.getText().get(0);
		nsdName.replace(':', '_');
		if(nsdName.indexOf(" (")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf(" ("));}
		if(nsdName.indexOf("(")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf("("));}
		dlgSave.setSelectedFile(new File(nsdName));

		dlgSave.addChoosableFileFilter(new lu.fisch.structorizer.io.PNGFilter());
		int result = dlgSave.showSaveDialog(NSDControl.getFrame());
		if (result == JFileChooser.APPROVE_OPTION)
		{
                    lastExportDir=dlgSave.getSelectedFile().getParentFile();
			String filename=dlgSave.getSelectedFile().getAbsoluteFile().toString();
			if(!filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".png"))
			{
				filename+=".png";
			}

			File file = new File(filename);
                        boolean writeDown = true;

                        if(file.exists())
			{
                            int response = JOptionPane.showConfirmDialog (null,
                                            "Overwrite existing file?","Confirm Overwrite",
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE);
                            if (response == JOptionPane.NO_OPTION)
                            {
				writeDown=false;
                            }
                        }
                        if(writeDown==true)
                        {
                            int cols = Integer.valueOf(JOptionPane.showInputDialog(null, "Into how many columns do you want to split the output?", "1"));
                            int rows = Integer.valueOf(JOptionPane.showInputDialog(null, "Into how many rows do you want to split the output?", "3"));
                            
                            BufferedImage image = new BufferedImage(root.width+1,root.height+1,BufferedImage.TYPE_4BYTE_ABGR);
                            printAll(image.getGraphics());
                            // source: http://answers.yahoo.com/question/index?qid=20110821001157AAcdXVk
                            // source: http://kalanir.blogspot.com/2010/02/how-to-split-image-into-chunks-java.html
                            try
                            {
                                // 1. Load image file into memory
                                //File file = new File("mario.png"); // mario.png in the same working directory
                                //FileInputStream fis = new FileInputStream(file);
                                //BufferedImage image = ImageIO.read(fis);

                                // 2. Decide the number of pieces, and calculate the size of each chunk
                                //int rows = 4;
                                //int cols = 6;
                                int chunks = rows * cols;

                                int chunkWidth = image.getWidth() / cols;
                                int chunkHeight = image.getHeight() / rows;

                                // 3. Define an Image array to hold image chunks
                                int count = 0;
                                BufferedImage imgs[] = new BufferedImage[chunks];

                                // 4. Fill the Image array with splitted image parts
                                for (int x = 0; x < rows; x++)
                                {
                                    for (int y = 0; y < cols; y++)
                                    {
                                        //Initialize the image array with image chunks
                                        imgs[count] = new BufferedImage(chunkWidth, chunkHeight, image.getType());

                                        // draws the image chunk
                                        Graphics2D gr = imgs[count++].createGraphics();
                                        gr.drawImage(image, 0, 0, chunkWidth, chunkHeight, chunkWidth * y, chunkHeight * x, chunkWidth * y + chunkWidth, chunkHeight * x + chunkHeight, null);
                                        gr.dispose();
                                    }
                                }

                                // 5. Save mini images into image files
                                for (int i = 0; i < imgs.length; i++)
                                {
                                    File f = new File(file.getAbsolutePath().replace(".png", "-"+i+".png"));
                                    ImageIO.write(imgs[i], "png", f);
                                }     
                            }
                            catch(Exception e)
                            {
                                JOptionPane.showOptionDialog(this,"Error while saving the images!","Error",JOptionPane.OK_OPTION,JOptionPane.ERROR_MESSAGE,null,null,null);
                            }
                        }
		}
        }
        
	public void exportPNG()
	{
		root.selectElementByCoord(-1,-1);
		redraw();

		JFileChooser dlgSave = new JFileChooser("Export diagram as PNG ...");
		// set directory
		if (lastExportDir!=null)
                {
                    dlgSave.setCurrentDirectory(lastExportDir);
                }
                else if(root.getFile()!=null)
		{
			dlgSave.setCurrentDirectory(root.getFile());
		}
		else
		{
			dlgSave.setCurrentDirectory(currentDirectory);
		}
		// propose name
		String nsdName = root.getText().get(0);
		nsdName.replace(':', '_');
		if(nsdName.indexOf(" (")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf(" ("));}
		if(nsdName.indexOf("(")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf("("));}
		dlgSave.setSelectedFile(new File(nsdName));

		dlgSave.addChoosableFileFilter(new lu.fisch.structorizer.io.PNGFilter());
		int result = dlgSave.showSaveDialog(NSDControl.getFrame());
		if (result == JFileChooser.APPROVE_OPTION)
		{
                    lastExportDir=dlgSave.getSelectedFile().getParentFile();
			String filename=dlgSave.getSelectedFile().getAbsoluteFile().toString();
			if(!filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".png"))
			{
				filename+=".png";
			}

			File file = new File(filename);
                        boolean writeDown = true;

                        if(file.exists())
			{
                            int response = JOptionPane.showConfirmDialog (null,
                                            "Overwrite existing file?","Confirm Overwrite",
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE);
                            if (response == JOptionPane.NO_OPTION)
                            {
				writeDown=false;
                            }
                        }
                        if(writeDown==true)
                        {
                            BufferedImage bi = new BufferedImage(root.width+1,root.height+1,BufferedImage.TYPE_4BYTE_ABGR);
                            printAll(bi.getGraphics());
                            try
                            {
                                    ImageIO.write(bi, "png", file);
                            }
                            catch(Exception e)
                            {
                                    JOptionPane.showOptionDialog(this,"Error while saving the image!","Error",JOptionPane.OK_OPTION,JOptionPane.ERROR_MESSAGE,null,null,null);
                            }
                        }
		}
	}

	public void exportEMF()
	{
		root.selectElementByCoord(-1,-1);
		redraw();

		JFileChooser dlgSave = new JFileChooser("Export diagram as EMF ...");
		// set directory
                if (lastExportDir!=null)
                {
                    dlgSave.setCurrentDirectory(lastExportDir);
                }
                else if(root.getFile() != null)
		{
			dlgSave.setCurrentDirectory(root.getFile());
		}
		else
		{
			dlgSave.setCurrentDirectory(currentDirectory);
		}
		// propose name
		String nsdName = root.getText().get(0);
		nsdName.replace(':', '_');
		if(nsdName.indexOf(" (")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf(" ("));}
		if(nsdName.indexOf("(")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf("("));}
		dlgSave.setSelectedFile(new File(nsdName));

		dlgSave.addChoosableFileFilter(new lu.fisch.structorizer.io.EMFFilter());
		int result = dlgSave.showSaveDialog(NSDControl.getFrame());
		if (result == JFileChooser.APPROVE_OPTION)
		{
                    lastExportDir=dlgSave.getSelectedFile().getParentFile();
			String filename=dlgSave.getSelectedFile().getAbsoluteFile().toString();
			if(!filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".emf"))
			{
				filename+=".emf";
			}

			File file = new File(filename);
                        boolean writeDown = true;

                        if(file.exists())
			{
                            int response = JOptionPane.showConfirmDialog (null,
                                            "Overwrite existing file?","Confirm Overwrite",
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE);
                            if (response == JOptionPane.NO_OPTION)
                            {
				writeDown=false;
                            }
                        }
                        if(writeDown==true)
                        {
                            try
                            {
                                    EMFGraphics2D emf = new EMFGraphics2D(new FileOutputStream(filename),new Dimension(root.width+12, root.height+12)) ;

                                    emf.startExport();
                                    lu.fisch.graphics.Canvas c = new lu.fisch.graphics.Canvas(emf);
                                    lu.fisch.graphics.Rect myrect = root.prepareDraw(c);
                                    myrect.left+=6;
                                    myrect.top+=6;
                                    root.draw(c,myrect);
                                    emf.endExport();
                            }
                            catch (Exception e)
                            {
                                    e.printStackTrace();
                            }
                        }
		}
	}

	public void exportSVG() // does not work!!
	{
		root.selectElementByCoord(-1,-1);
		redraw();

		JFileChooser dlgSave = new JFileChooser("Export diagram as SVG ...");
		// set directory
		if (lastExportDir!=null)
                {
                    dlgSave.setCurrentDirectory(lastExportDir);
                }
                else if(root.getFile()!=null)
		{
			dlgSave.setCurrentDirectory(root.getFile());
		}
		else
		{
			dlgSave.setCurrentDirectory(currentDirectory);
		}
		// propose name
		String nsdName = root.getText().get(0);
		nsdName.replace(':', '_');
		if(nsdName.indexOf(" (")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf(" ("));}
		if(nsdName.indexOf("(")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf("("));}
		dlgSave.setSelectedFile(new File(nsdName));

		dlgSave.addChoosableFileFilter(new lu.fisch.structorizer.io.SVGFilter());
		int result = dlgSave.showSaveDialog(NSDControl.getFrame());
		if (result == JFileChooser.APPROVE_OPTION)
		{
                    lastExportDir=dlgSave.getSelectedFile().getParentFile();
			String filename=dlgSave.getSelectedFile().getAbsoluteFile().toString();
			if(!filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".svg"))
			{
				filename+=".svg";
			}

			File file = new File(filename);
                        boolean writeDown = true;

                        if(file.exists())
			{
                            int response = JOptionPane.showConfirmDialog (null,
                                            "Overwrite existing file?","Confirm Overwrite",
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE);
                            if (response == JOptionPane.NO_OPTION)
                            {
				writeDown=false;
                            }
                        }
                        if(writeDown==true)
                        {
                            try
                            {
                                    SVGGraphics2D svg = new SVGGraphics2D(new FileOutputStream(filename),new Dimension(root.width+12, root.height+12)) ;
                                    svg.startExport();
                                    lu.fisch.graphics.Canvas c = new lu.fisch.graphics.Canvas(svg);
                                    lu.fisch.graphics.Rect myrect = root.prepareDraw(c);
                                    myrect.left+=6;
                                    myrect.top+=6;
                                    root.draw(c,myrect);
                                    svg.endExport();
                                    
                                    // re-read the file ...
                                    StringBuffer buffer = new StringBuffer();
                                    InputStreamReader isr = new InputStreamReader(new FileInputStream(filename));
                                    Reader in = new BufferedReader(isr);
                                    int ch;
                                    while ((ch = in.read()) > -1)
                                    {
                                        buffer.append((char)ch);
                                    }

                                    // ... and encode it UTF-8
                                    FileOutputStream fos = new FileOutputStream(filename);
                                    Writer out = new OutputStreamWriter(fos, "UTF-8");
                                    out.write(buffer.toString());
                                    out.close();
                                    
                            }
                            catch (Exception e)
                            {
                                    e.printStackTrace();
                            }
                       }
		}

                root.selectElementByCoord(-1,-1);
		redraw();
	}

	public void exportSWF()
	{
		root.selectElementByCoord(-1,-1);
		redraw();

		JFileChooser dlgSave = new JFileChooser("Export diagram as SWF ...");
		// set directory
		if (lastExportDir!=null)
                {
                    dlgSave.setCurrentDirectory(lastExportDir);
                }
                else if(root.getFile()!=null)
		{
			dlgSave.setCurrentDirectory(root.getFile());
		}
		else
		{
			dlgSave.setCurrentDirectory(currentDirectory);
		}
		// propose name
		String nsdName = root.getText().get(0);
		nsdName.replace(':', '_');
		if(nsdName.indexOf(" (")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf(" ("));}
		if(nsdName.indexOf("(")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf("("));}
		dlgSave.setSelectedFile(new File(nsdName));

		dlgSave.addChoosableFileFilter(new lu.fisch.structorizer.io.SWFFilter());
		int result = dlgSave.showSaveDialog(NSDControl.getFrame());
		if (result == JFileChooser.APPROVE_OPTION)
		{
                    lastExportDir=dlgSave.getSelectedFile().getParentFile();
			String filename=dlgSave.getSelectedFile().getAbsoluteFile().toString();
			if(!filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".swf"))
			{
				filename+=".swf";
			}

			File file = new File(filename);
                        boolean writeDown = true;

                        if(file.exists())
			{
                            int response = JOptionPane.showConfirmDialog (null,
                                            "Overwrite existing file?","Confirm Overwrite",
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE);
                            if (response == JOptionPane.NO_OPTION)
                            {
				writeDown=false;
                            }
                        }
                        if(writeDown==true)
                        {
                            try
                            {
                                    SWFGraphics2D svg = new SWFGraphics2D(new FileOutputStream(filename),new Dimension(root.width+12, root.height+12)) ;

                                    svg.startExport();
                                    lu.fisch.graphics.Canvas c = new lu.fisch.graphics.Canvas(svg);
                                    lu.fisch.graphics.Rect myrect = root.prepareDraw(c);
                                    myrect.left+=6;
                                    myrect.top+=6;
                                    root.draw(c,myrect);
                                    svg.endExport();
                            }
                            catch (Exception e)
                            {
                                    e.printStackTrace();
                            }
                       }
		}
	}

	public void exportPDF()
	{
		root.selectElementByCoord(-1,-1);
		redraw();

		JFileChooser dlgSave = new JFileChooser("Export diagram as PDF ...");
		// set directory
		if (lastExportDir!=null)
                {
                    dlgSave.setCurrentDirectory(lastExportDir);
                }
                else if(root.getFile()!=null)
		{
			dlgSave.setCurrentDirectory(root.getFile());
		}
		else
		{
			dlgSave.setCurrentDirectory(currentDirectory);
		}
		// propose name
		String nsdName = root.getText().get(0);
		nsdName.replace(':', '_');
		if(nsdName.indexOf(" (")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf(" ("));}
		if(nsdName.indexOf("(")>=0) {nsdName=nsdName.substring(0,nsdName.indexOf("("));}
		dlgSave.setSelectedFile(new File(nsdName));

		dlgSave.addChoosableFileFilter(new lu.fisch.structorizer.io.PDFFilter());
		int result = dlgSave.showSaveDialog(NSDControl.getFrame());
		if (result == JFileChooser.APPROVE_OPTION)
		{
                    lastExportDir=dlgSave.getSelectedFile().getParentFile();
			String filename=dlgSave.getSelectedFile().getAbsoluteFile().toString();
			if(!filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".pdf"))
			{
				filename+=".pdf";
			}

			File file = new File(filename);
                        boolean writeDown = true;

                        if(file.exists())
			{
                            int response = JOptionPane.showConfirmDialog (null,
                                            "Overwrite existing file?","Confirm Overwrite",
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE);
                            if (response == JOptionPane.NO_OPTION)
                            {
				writeDown=false;
                            }
                        }
                        if(writeDown==true)
                        {
                            try
                            {
                                    PDFGraphics2D svg = new PDFGraphics2D(new FileOutputStream(filename),new Dimension(root.width+12, root.height+12)) ;

                                    svg.startExport();
                                    lu.fisch.graphics.Canvas c = new lu.fisch.graphics.Canvas(svg);
                                    lu.fisch.graphics.Rect myrect = root.prepareDraw(c);
                                        myrect.left+=6;
                                        myrect.top+=6;
                                    root.draw(c,myrect);
                                    svg.endExport();
                            }
                            catch (Exception e)
                            {
                                    e.printStackTrace();
                            }
                        }
		}
	}


	/*****************************************
	 * export code methods
	 *****************************************/
	public void importPAS()
	{
		// only save if something has been changed
		saveNSD(true);

		String filename = "";

		JFileChooser dlgOpen = new JFileChooser();
		dlgOpen.setDialogTitle("Import Pascal Code ...");
		// set directory
		if(root.getFile()!=null)
		{
			dlgOpen.setCurrentDirectory(root.getFile());
		}
		else
		{
			dlgOpen.setCurrentDirectory(currentDirectory);
		}

		dlgOpen.addChoosableFileFilter(new PascalFilter());
		int result = dlgOpen.showOpenDialog(NSDControl.getFrame());
		filename=dlgOpen.getSelectedFile().getAbsoluteFile().toString();

		if (result == JFileChooser.APPROVE_OPTION)
		{
			// load and pas source-code
			D7Parser d7 = new D7Parser("D7Grammar.cgt");
			Root rootNew = d7.parse(filename);
			if (d7.error.equals(""))
			{
				boolean hil = root.hightlightVars;
				root = rootNew;
				root.hightlightVars = hil;
			}
			else
			{
				// show error
				JOptionPane.showOptionDialog(null,d7.error,
											 "Parser Error",
											 JOptionPane.OK_OPTION,JOptionPane.ERROR_MESSAGE,null,null,null);
			}

			redraw();
			analyse();
		}
	}

	/*****************************************
	 * export code methods
	 *****************************************/
	public void export(String _generatorClassName)
	{
		try
		{
			Class genClass = Class.forName(_generatorClassName);
			Generator gen = (Generator) genClass.newInstance();
			gen.exportCode(root,currentDirectory,NSDControl.getFrame());
		}
		catch(Exception e)
		{
			JOptionPane.showOptionDialog(this,"Error while using generator "+_generatorClassName+"\n"+e.getMessage(),"Error",JOptionPane.OK_OPTION,JOptionPane.ERROR_MESSAGE,null,null,null);
		}
	}

	/*****************************************
	 * update method
	 *****************************************/

	public void updateNSD()
	{
		//Desktop desk = Desktop.getDesktop();
		// java.awt.Desktop (since 1.6 !!)

		JOptionPane.showOptionDialog(this,"<html>Goto <a href=\"http://structorizer.fisch.lu\">http://structorizer.fisch.lu</a> to look for updates<br>and news about Structorizer.</html>",
									 "Update",
								 	 JOptionPane.OK_OPTION,JOptionPane.INFORMATION_MESSAGE,null,null,null);
	}

	/*****************************************
	 * the preferences dialog methods
	 *****************************************/

	public void colorsNSD()
	{
		Colors colors = new Colors(NSDControl.getFrame());
		Point p = getLocationOnScreen();
		colors.setLocation(Math.round(p.x+(getVisibleRect().width-colors.getWidth())/2+this.getVisibleRect().x),
						   Math.round(p.y+(getVisibleRect().height-colors.getHeight())/2+this.getVisibleRect().y));

		// set fields
		colors.color0.setBackground(Element.color0);
		colors.color1.setBackground(Element.color1);
		colors.color2.setBackground(Element.color2);
		colors.color3.setBackground(Element.color3);
		colors.color4.setBackground(Element.color4);
		colors.color5.setBackground(Element.color5);
		colors.color6.setBackground(Element.color6);
		colors.color7.setBackground(Element.color7);
		colors.color8.setBackground(Element.color8);
		colors.color9.setBackground(Element.color9);

		colors.setLang(NSDControl.getLang());
		colors.pack();
		colors.setVisible(true);

		// get fields
		Element.color0=colors.color0.getBackground();
		Element.color1=colors.color1.getBackground();
		Element.color2=colors.color2.getBackground();
		Element.color3=colors.color3.getBackground();
		Element.color4=colors.color4.getBackground();
		Element.color5=colors.color5.getBackground();
		Element.color6=colors.color6.getBackground();
		Element.color7=colors.color7.getBackground();
		Element.color8=colors.color8.getBackground();
		Element.color9=colors.color9.getBackground();

		NSDControl.updateColors();

		// save fields to ini-file
		Element.saveToINI();

	}

	public void preferencesNSD()
	{
		Preferences preferences = new Preferences(NSDControl.getFrame());
		Point p = getLocationOnScreen();
		preferences.setLocation(Math.round(p.x+(getVisibleRect().width-preferences.getWidth())/2+this.getVisibleRect().x),
								Math.round(p.y+(getVisibleRect().height-preferences.getHeight())/2+this.getVisibleRect().y));

		// set fields
		preferences.edtAltT.setText(Element.preAltT);
		preferences.edtAltF.setText(Element.preAltF);
		preferences.edtAlt.setText(Element.preAlt);
		preferences.txtCase.setText(Element.preCase);
		preferences.edtFor.setText(Element.preFor);
		preferences.edtWhile.setText(Element.preWhile);
		preferences.edtRepeat.setText(Element.preRepeat);
                
                preferences.altPadRight.setSelected(Element.altPadRight);

		preferences.setLang(NSDControl.getLang());
		preferences.pack();
		preferences.setVisible(true);

		// get fields
		Element.preAltT=preferences.edtAltT.getText();
		Element.preAltF=preferences.edtAltF.getText();
		Element.preAlt=preferences.edtAlt.getText();
		Element.preCase=preferences.txtCase.getText();
		Element.preFor=preferences.edtFor.getText();
		Element.preWhile=preferences.edtWhile.getText();
		Element.preRepeat=preferences.edtRepeat.getText();
                Element.altPadRight=preferences.altPadRight.isSelected();

		// save fields to ini-file
		Element.saveToINI();
                redraw();
	}

	public void parserNSD()
	{
		ParserPreferences parserPreferences = new ParserPreferences(NSDControl.getFrame());
		Point p = getLocationOnScreen();
		parserPreferences.setLocation(Math.round(p.x+(getVisibleRect().width-parserPreferences.getWidth())/2+this.getVisibleRect().x),
								Math.round(p.y+(getVisibleRect().height-parserPreferences.getHeight())/2+this.getVisibleRect().y));

		// set fields
		parserPreferences.edtAltPre.setText(D7Parser.preAlt);
		parserPreferences.edtAltPost.setText(D7Parser.postAlt);
		parserPreferences.edtCasePre.setText(D7Parser.preCase);
		parserPreferences.edtCasePost.setText(D7Parser.postCase);
		parserPreferences.edtForPre.setText(D7Parser.preFor);
		parserPreferences.edtForPost.setText(D7Parser.postFor);
		parserPreferences.edtWhilePre.setText(D7Parser.preWhile);
		parserPreferences.edtWhilePost.setText(D7Parser.postWhile);
		parserPreferences.edtRepeatPre.setText(D7Parser.preRepeat);
		parserPreferences.edtRepeatPost.setText(D7Parser.postRepeat);
		parserPreferences.edtInput.setText(D7Parser.input);
		parserPreferences.edtOutput.setText(D7Parser.output);

		parserPreferences.setLang(NSDControl.getLang());
		parserPreferences.pack();
		parserPreferences.setVisible(true);
                
                if(parserPreferences.OK)
                {

                    // get fields
                    D7Parser.preAlt=parserPreferences.edtAltPre.getText();
                    D7Parser.postAlt=parserPreferences.edtAltPost.getText();
                    D7Parser.preCase=parserPreferences.edtCasePre.getText();
                    D7Parser.postCase=parserPreferences.edtCasePost.getText();
                    D7Parser.preFor=parserPreferences.edtForPre.getText();
                    D7Parser.postFor=parserPreferences.edtForPost.getText();
                    D7Parser.preWhile=parserPreferences.edtWhilePre.getText();
                    D7Parser.postWhile=parserPreferences.edtWhilePost.getText();
                    D7Parser.preRepeat=parserPreferences.edtRepeatPre.getText();
                    D7Parser.postRepeat=parserPreferences.edtRepeatPost.getText();
                    D7Parser.input=parserPreferences.edtInput.getText();
                    D7Parser.output=parserPreferences.edtOutput.getText();


                    // save fields to ini-file
                    D7Parser.saveToINI();
                }
	}

	public void analyserNSD()
	{
		AnalyserPreferences analyserPreferences = new AnalyserPreferences(NSDControl.getFrame());
		Point p = getLocationOnScreen();
		analyserPreferences.setLocation(Math.round(p.x+(getVisibleRect().width-analyserPreferences.getWidth())/2+this.getVisibleRect().x),
						   Math.round(p.y+(getVisibleRect().height-analyserPreferences.getHeight())/2+this.getVisibleRect().y));

		// set fields
		analyserPreferences.check1.setSelected(Root.check1);
		analyserPreferences.check2.setSelected(Root.check2);
		analyserPreferences.check3.setSelected(Root.check3);
		analyserPreferences.check4.setSelected(Root.check4);
		analyserPreferences.check5.setSelected(Root.check5);
		analyserPreferences.check6.setSelected(Root.check6);
		analyserPreferences.check7.setSelected(Root.check7);
		analyserPreferences.check8.setSelected(Root.check8);
		analyserPreferences.check9.setSelected(Root.check9);
		analyserPreferences.check10.setSelected(Root.check10);
		analyserPreferences.check11.setSelected(Root.check11);
		analyserPreferences.check12.setSelected(Root.check12);
		analyserPreferences.check13.setSelected(Root.check13);

		analyserPreferences.setLang(NSDControl.getLang());
		analyserPreferences.pack();
		analyserPreferences.setVisible(true);

		// get fields
		Root.check1=analyserPreferences.check1.isSelected();
		Root.check2=analyserPreferences.check2.isSelected();
		Root.check3=analyserPreferences.check3.isSelected();
		Root.check4=analyserPreferences.check4.isSelected();
		Root.check5=analyserPreferences.check5.isSelected();
		Root.check6=analyserPreferences.check6.isSelected();
		Root.check7=analyserPreferences.check7.isSelected();
		Root.check8=analyserPreferences.check8.isSelected();
		Root.check9=analyserPreferences.check9.isSelected();
		Root.check10=analyserPreferences.check10.isSelected();
		Root.check11=analyserPreferences.check11.isSelected();
		Root.check12=analyserPreferences.check12.isSelected();
		Root.check13=analyserPreferences.check13.isSelected();

		// save fields to ini-file
		Root.saveToINI();

		// re-analyse
		root.getVarNames();
		analyse();
	}

    public void exportOptions()
    {
        try
        {
            Ini ini = Ini.getInstance();
            ini.load();
            ExportOptionDialoge eod = new ExportOptionDialoge(NSDControl.getFrame());
            if(ini.getProperty("genExportComments","0").equals("true"))
                eod.commentsCheckBox.setSelected(true);
            else 
                eod.commentsCheckBox.setSelected(false);
            // START KGU 2014-11-18
            eod.setLang(NSDControl.getLang());
            // END KGU 2014-11-18
            eod.setVisible(true);
            
            if(eod.goOn==true)
            {
                ini.setProperty("genExportComments", String.valueOf(eod.commentsCheckBox.isSelected()));
                ini.save();
            }
        } 
        catch (FileNotFoundException ex)
        {
            ex.printStackTrace();
        } 
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

	public void fontNSD()
	{
		FontChooser fontChooser = new FontChooser(NSDControl.getFrame());
		Point p = getLocationOnScreen();
		fontChooser.setLocation(Math.round(p.x+(getVisibleRect().width-fontChooser.getWidth())/2+this.getVisibleRect().x),
								Math.round(p.y+(getVisibleRect().height-fontChooser.getHeight())/2+this.getVisibleRect().y));

		// set fields
		fontChooser.setFont(Element.getFont());
		fontChooser.setLang(NSDControl.getLang());
		fontChooser.setVisible(true);

		// get fields
		Element.setFont(fontChooser.getCurrentFont());

		// save fields to ini-file
		Element.saveToINI();

		// redraw diagram
		redraw();
	}

	public void fontUpNSD()
	{
		// change font size
		Element.setFont(new Font(Element.getFont().getFamily(),Font.PLAIN,Element.getFont().getSize()+2));

		// save size
		Element.saveToINI();

		// redraw diagram
		redraw();
	}

	public void fontDownNSD()
	{
		if(Element.getFont().getSize()-2>=4)
		{
			// change font size
			Element.setFont(new Font(Element.getFont().getFamily(),Font.PLAIN,Element.getFont().getSize()-2));

			// save size
			Element.saveToINI();

			// redraw diagram
			redraw();
		}
	}

	/*****************************************
	 * setter method
	 *****************************************/
	public void toggleDIN()
	{
		Element.E_DIN = !(Element.E_DIN);
		NSDControl.doButtons();
		redraw();
	}

	public void setDIN()
	{
		Element.E_DIN = true;
		NSDControl.doButtons();
		redraw();
	}

	public boolean getDIN()
	{
		return Element.E_DIN;
	}

	public void setNice(boolean _nice)
	{
		root.isNice=_nice;
                root.hasChanged=true;
		redraw();
	}

	public boolean isNice()
	{
		return root.isNice;
	}

	public void setFunction()
	{
		root.isProgram=false;
                root.hasChanged=true;
		redraw();
	}

	public void setProgram()
	{
		root.isProgram=true;
                root.hasChanged=true;
		redraw();
	}

	public boolean isProgram()
	{
		return root.isProgram;
	}


	public void setComments(boolean _comments)
	{
		Element.E_SHOWCOMMENTS=_comments;
		NSDControl.doButtons();
		redraw();
	}

	public void setToggleTC(boolean _tc)
	{
		Element.E_TOGGLETC=_tc;
		NSDControl.doButtons();
		redraw();
	}

	public boolean drawComments()
	{
		return Element.E_SHOWCOMMENTS;
	}

	public void setHightlightVars(boolean _highlight)
	{
		Element.E_VARHIGHLIGHT=_highlight;
		root.hightlightVars=_highlight;
		NSDControl.doButtons();
		redraw();
	}

	public void toggleAnalyser()
	{
		setAnalyser(!Element.E_ANALYSER);
		if(Element.E_ANALYSER==true)
		{
			analyse();
		}
	}

	public void setAnalyser(boolean _analyse)
	{
		Element.E_ANALYSER=_analyse;
		NSDControl.doButtons();
	}

	/*****************************************
	 * inputbox methods
	 *****************************************/
	public void showInputBox(EditData _data)
	{
            if(NSDControl!=null)
            {
		InputBox inputbox = new InputBox(NSDControl.getFrame(),true);
		//Point p = getLocationOnScreen();
		// position inputbox in the middle of this component

		//inputbox.setLocation(Math.round(p.x+(this.getVisibleRect().width-inputbox.getWidth())/2+this.getVisibleRect().x),
		//					 Math.round(p.y+(this.getVisibleRect().height-inputbox.getHeight())/2+this.getVisibleRect().y));

                inputbox.setLocationRelativeTo(NSDControl.getFrame());

		// set title
		inputbox.setTitle(_data.title);

		// set field
		inputbox.txtText.setText(_data.text.getText());
		inputbox.txtComment.setText(_data.comment.getText());

		inputbox.OK=false;
		inputbox.setLang(NSDControl.getLang());
		inputbox.setVisible(true);

		// get fields
		_data.text.setText(inputbox.txtText.getText());
		_data.comment.setText(inputbox.txtComment.getText());
		_data.result=inputbox.OK;

                inputbox.dispose();
             }
	}

	/*****************************************
	 * CLIPBOARD INTERACTIONS
     *****************************************/
	public void copyToClipboardPNG()
	{
                root.selectElementByCoord(-1,-1);
		redraw();

                Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		//DataFlavor pngFlavor = new DataFlavor("image/png","Portable Network Graphics");

		// get diagram
		BufferedImage image = new BufferedImage(root.width+1,root.height+1, BufferedImage.TYPE_INT_ARGB);
		root.draw(image.getGraphics());

		// put image to clipboard
		ImageSelection imageSelection = new ImageSelection(image);
		systemClipboard.setContents(imageSelection, null);
	}

	public void copyToClipboardEMF()
	{
                root.selectElementByCoord(-1,-1);
		redraw();

                Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

		try
		{
			ByteArrayOutputStream myEMF = new ByteArrayOutputStream();
			EMFGraphics2D emf = new EMFGraphics2D(myEMF,new Dimension(root.width+6, root.height+1)) ;
			emf.setFont(Element.getFont());

			emf.startExport();
			lu.fisch.graphics.Canvas c = new lu.fisch.graphics.Canvas(emf);
			lu.fisch.graphics.Rect myrect = root.prepareDraw(c);
			root.draw(c,myrect);
			emf.endExport();

			systemClipboard.setContents(new EMFSelection(myEMF),null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if(selected!=null)
        {
            if(e.getWheelRotation()<-1) selected.setCollapsed(true);
            else if(e.getWheelRotation()>1)  selected.setCollapsed(false);
            
            redraw();
        }
    }

    void toggleTextComments() {
    	Element.E_TOGGLETC=!Element.E_TOGGLETC;
    	repaint();
    }

	// Inner class is used to hold an image while on the clipboard.
	public static class EMFSelection implements Transferable, ClipboardOwner
		{
			public static final DataFlavor emfFlavor = new DataFlavor("image/emf", "Enhanced Meta File");
			// the Image object which will be housed by the ImageSelection
			private ByteArrayOutputStream os;

			static
			{
				try
				{
					SystemFlavorMap sfm = (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
					sfm.addUnencodedNativeForFlavor(emfFlavor, "ENHMETAFILE");
				}
				catch(Exception e)
				{
					System.err.println(e.getMessage());
				}
			}

			private static DataFlavor[] supportedFlavors = {emfFlavor};

			public EMFSelection(ByteArrayOutputStream os)
			{
				this.os = os;
			}

			// Returns the supported flavors of our implementation
			public DataFlavor[] getTransferDataFlavors()
			{
				return supportedFlavors;
			}

			// Returns true if flavor is supported
			public boolean isDataFlavorSupported(DataFlavor flavor)
			{
				for(int i=0;i<supportedFlavors.length;i++)
				{
					DataFlavor f = supportedFlavors[i];
					if (f.equals(flavor))
					{
						return true;
					}
				}
				return false;
			}

			// Returns Image object housed by Transferable object
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,IOException
			{
				if (flavor.equals(emfFlavor))
				{
					return (new ByteArrayInputStream(os.toByteArray()));
				}
				else
				{
					System.out.println("Hei !!!");
					throw new UnsupportedFlavorException(flavor);
				}
			}

			public void lostOwnership(Clipboard arg0, Transferable arg1) {}

		}

	// Inner class is used to hold an image while on the clipboard.
	public static class ImageSelection implements Transferable
		{
			// the Image object which will be housed by the ImageSelection
			private Image image;

			public ImageSelection(Image image)
			{
				this.image = image;
			}

			// Returns the supported flavors of our implementation
			public DataFlavor[] getTransferDataFlavors()
			{
				return new DataFlavor[] {DataFlavor.imageFlavor};
			}

			// Returns true if flavor is supported
			public boolean isDataFlavorSupported(DataFlavor flavor)
			{
				return DataFlavor.imageFlavor.equals(flavor);
			}

			// Returns Image object housed by Transferable object
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,IOException
			{
				if (!DataFlavor.imageFlavor.equals(flavor))
				{
					throw new UnsupportedFlavorException(flavor);
				}
				// else return the payload
				return image;
			}
		}

	/*****************************************
	 * CONSTRUCTOR
     *****************************************/
    public Diagram(Editor _editor, String _string)
    {
        super(true);
        this.setDoubleBuffered(true);	// we don't need double buffering, because the drawing
                                                                        // itself does it allready!
        this.setBackground(Color.LIGHT_GRAY);

        if(_editor!=null)
        {
            errorlist=_editor.errorlist;
            NSDControl = _editor;
        }
        create(_string);
    }


	/*****************************************
	 * ANALYSER
     *****************************************/
	protected void analyse()
	{
		if(Element.E_ANALYSER==true && errorlist!=null)
		{
			//System.out.println("Analysing ...");

			// Olà - The use of a thread to show the errorlist
			// seams not to work, because the list is not always
			// shown. Concurrency problem?

			/*
			 Analyser analyser = new Analyser(root,errorlist);
			 analyser.start();
			 /**/

			//System.out.println("Working ...");
			Vector vec = root.analyse();
			DefaultListModel errors = (DefaultListModel) errorlist.getModel();
			errors.clear();

			for(int i=0;i<vec.size();i++)
			{
				errors.addElement((DetectedError) vec.get(i));
			}

			errorlist.repaint();
			errorlist.validate();
		}
	}

	 /*****************************************
	  * Recently used files
	  *****************************************/
	public void addRecentFile(String _filename)
	{
		addRecentFile(_filename,true);
	}

	public void addRecentFile(String _filename, boolean saveINI)
	{
		if(recentFiles.contains(_filename))
		{
			recentFiles.remove(_filename);
		}
		recentFiles.insertElementAt(_filename,0);
		while(recentFiles.size()>10)	// FIXME (KGU 2014-11-25) hard-coded "magic number"
		{
			recentFiles.removeElementAt(recentFiles.size()-1);
		}
		NSDControl.doButtons();
		if(saveINI==true) {NSDControl.savePreferences();}
	}

    /*****************************************
     * Run
	 *****************************************/
    public void goRun()
    {
                Executor executor = Executor.getInstance(this,null);
                /*
                String str = JOptionPane.showInputDialog(null, "Please enter the animation delay!", "50");
                if(str!=null)
                {
                    executor.setDelay(Integer.valueOf(str));
                    executor.execute(this.root);
                }
                */
    }

    public void goTurtle()
    {
                if(turtle==null)
				{
					turtle= new TurtleBox(500,500);
				}
				turtle.setVisible(true);
                Executor executor = Executor.getInstance(this,turtle);
                /*
                 String str = JOptionPane.showInputDialog(null, "Please enter the animation delay!", "50");
                if(str!=null)
                {
                    executor.setDelay(Integer.valueOf(str));
                    executor.execute(this.root);
                }
                */

    }
}