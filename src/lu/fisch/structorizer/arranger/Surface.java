/*
    Structorizer :: Arranger
    A little tool which you can use to arrange Nassi-Schneiderman Diagrams (NSD)

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


package lu.fisch.structorizer.arranger;

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lu.fisch.graphics.Rect;
import lu.fisch.structorizer.elements.Element;
import lu.fisch.structorizer.elements.Root;
import lu.fisch.structorizer.elements.Updater;
import lu.fisch.structorizer.gui.Mainform;
import lu.fisch.structorizer.io.PNGFilter;
import lu.fisch.structorizer.parsers.NSDParser;
import net.iharder.dnd.FileDrop;

/**
 *
 * @author robertfisch
 */
public class Surface extends javax.swing.JPanel implements MouseListener, MouseMotionListener, Updater, WindowListener {

    private Vector<Diagram> diagrams = new Vector<Diagram>();

    private Point mousePoint = null;
    private Point mouseRelativePoint = null;
    private boolean mousePressed = false;
    private Diagram mouseSelected = null;


    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
        if(diagrams!=null)
        {
            for(int d=0;d<diagrams.size();d++)
            {
                Diagram diagram = diagrams.get(d);
                Root root = diagram.root;
                Point point = diagram.point;

                root.draw(g, point, this);
            }
        }
    }

    private void create()
    {
        new  FileDrop(this, new FileDrop.Listener()
        {
            public void  filesDropped( java.io.File[] files )
            {
                boolean found = false;
                for (int i = 0; i < files.length; i++)
                {
                    String filename = files[i].toString();
                    if(filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".nsd"))
                    {
                        // open an existing file
                        NSDParser parser = new NSDParser();
                        File f = new File(filename);
                        Root root = parser.parse(f.toURI().toString());
                        root.filename=filename;
                        addDiagram(root);
                    }
                }
            }
	});

        this.addMouseListener(this);
        this.addMouseMotionListener(this);

    }

    public void exportPNG(Frame frame)
    {
        JFileChooser dlgSave = new JFileChooser("Export diagram as PNG ...");
        // propose name
        //String uniName = directoryName.substring(directoryName.lastIndexOf('/')+1).trim();
        //dlgSave.setSelectedFile(new File(uniName));

        dlgSave.addChoosableFileFilter(new PNGFilter());
        int result = dlgSave.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION)
        {
            // correct the filename, if necessaray
            String filename=dlgSave.getSelectedFile().getAbsoluteFile().toString();
            if(!filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".png"))
            {
                    filename+=".png";
            }

            // deselect any diagram
            if(diagrams!=null)
            {
                for(int d=0;d<diagrams.size();d++)
                {
                    diagrams.get(d).root.setSelected(false);
                }
                repaint();
            }

            // set up the file
            File file = new File(filename);
            // create the image
            BufferedImage bi = new BufferedImage(this.getWidth(), this.getHeight(),BufferedImage.TYPE_4BYTE_ABGR);
            paint(bi.getGraphics());
            // save the file
            try
            {
                ImageIO.write(bi, "png", file);
            }
            catch(Exception e)
            {
                JOptionPane.showOptionDialog(frame,"Error while saving the image!","Error",JOptionPane.OK_OPTION,JOptionPane.ERROR_MESSAGE,null,null,null);
            }
        }
    }

    public Rect getDrawingRect()
    {
        Rect r = new Rect(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);


        if(diagrams!=null)
        {
            if(diagrams.size()>0)
            for(int d=0;d<diagrams.size();d++)
            {
                Diagram diagram = diagrams.get(d);
                Root root = diagram.root;
                Rect rect = root.getRect();
                r.left=Math.min(rect.left,r.left);
                r.top=Math.min(rect.top,r.top);
                r.right=Math.max(rect.right,r.right);
                r.bottom=Math.max(rect.bottom,r.bottom);
            }
            else  r = new Rect(0,0,0,0);
        }
        else r = new Rect(0,0,0,0);

        return r;
    }

    public void addDiagram(Root root)
    {
        Rect rect = getDrawingRect();

        int top = 0;
        int left = 0;

        top  = rect.top+10;
        left = rect.right+10;

        if(left>this.getWidth())
        {
            top = rect.bottom+10;
            left = rect.left+10;
        }

        Point point = new Point(left,top);
        Diagram diagram = new Diagram(root,point);
        diagrams.add(diagram);
        repaint();
    }


    /** Creates new form Surface */
    public Surface()
    {
        initComponents();
        create();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @return the diagrams
     */
    public Vector<Diagram> getDiagrams()
    {
        return diagrams;
    }

    /**
     * @param diagrams the diagrams to set
     */
    public void setDiagrams(Vector<Diagram> diagrams)
    {
        this.diagrams = diagrams;
    }

    public void mouseClicked(MouseEvent e)
    {
        mousePressed(e);
        if(e.getClickCount()==2 && mouseSelected!=null)
        {
            // create editor
            Mainform form = mouseSelected.mainform;
            if(form==null)
            {
                form=new Mainform();
                form.addWindowListener(this);
            }

            // change the default closing behaviour
            form.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            // store mainform in diagram
            mouseSelected.mainform=form;

            // register this as "updater"
            mouseSelected.root.addUpdater(this);

            // affect the new diagram to the editor
            form.diagram.root=mouseSelected.root;
            // redraw the diagram
            form.diagram.redraw();
            form.setVisible(true);

            mouseSelected=null;
            mousePressed=false;
            this.repaint();
        }
    }

    public void mousePressed(MouseEvent e)
    {
        mousePoint = e.getPoint();
        mousePressed = true;
        if(diagrams!=null)
        for(int d=0;d<diagrams.size();d++)
        {
            Diagram diagram = diagrams.get(d);
            Root root = diagram.root;

            Element ele = root.selectElementByCoord(mousePoint.x-diagram.point.x,
                                                    mousePoint.y-diagram.point.y);
            if(ele!=null)
            {
                mouseSelected=diagram;
                mouseRelativePoint = new Point(mousePoint.x-mouseSelected.point.x,
                                               mousePoint.y-mouseSelected.point.y);
                root.selectElementByCoord(-1, -1);
                root.setSelected(true);
                repaint();
            }

        }
    }

    public void mouseReleased(MouseEvent e)
    {
        mousePressed = false;
        mouseSelected = null;
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mouseDragged(MouseEvent e)
    {
        if (mousePressed == true)
        {
            if(mouseSelected!=null)
            {
                mouseSelected.point.setLocation(e.getPoint().x-mouseRelativePoint.x,
                                                e.getPoint().y-mouseRelativePoint.y);
                repaint();
            }
        }
    }

    public void mouseMoved(MouseEvent e)
    {
    }



    public void update(Root source)
    {
        this.repaint();
    }


    
    // Windows listener for the mainform
    // I need this to unregister the updater
    public void windowOpened(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
         if(e.getSource() instanceof Mainform)
        {
            Mainform mainform = (Mainform) e.getSource();
            // unregister updater
            mainform.diagram.root.removeUpdater(this);
            // remove mainform reference
            if(diagrams!=null)
            {
                for(int d=0;d<diagrams.size();d++)
                {
                    Diagram diagram = diagrams.get(d);
                    Root root = diagram.root;
                    Point point = diagram.point;

                   if(mainform.diagram.root==root) diagram.mainform=null;
                }
            }
        }
   }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowDeactivated(WindowEvent e)
    {
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}