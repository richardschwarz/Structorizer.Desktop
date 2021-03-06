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

package lu.fisch.structorizer.elements;

/*
 ******************************************************************************************************
 *
 *      Author:         Bob Fisch
 *
 *      Description:    This class represents an "CASE statement" in a diagram.
 *
 ******************************************************************************************************
 *
 *      Revision List
 *
 *      Author          Date			Description
 *      ------			----			-----------
 *      Bob Fisch       2007.12.12      First Issue
 *      Kay Gürtzig     2015.10.11      Method selectElementByCoord(int,int) replaced by getElementByCoord(int,int,boolean)
 *      Kay Gürtzig     2015.10.11      Comment drawing centralized and breakpoint mechanism prepared
 *      Kay Gürtzig     2015.11.14      Bugfix #31 (= KGU#82) in method copy
 *      Kay Gürtzig     2015.11.14      Bugfix #39 (= KGU#91) in method draw()
 *      Kay Gürtzig     2016.01.02      Bugfix #78 (KGU#119): New method equals(Element)
 *      Kay Gürtzig     2016.01.03      Bugfix #87 (KGU#121): Correction in getElementByCoord(), getIcon()
 *      Kay Gürtzig     2016.02.27      Bugfix #97 (KGU#136): field rect replaced by rect0 in prepareDraw()
 *      Kay Gürtzig     2016.03.02      Bugfix #97 (KGU#136): Translation-neutral selection mechanism
 *      Kay Gürtzig     2016.03.06      Enh. #77 (KGU#117): Method for test coverage tracking added
 *      Kay Gürtzig     2016.03.12      Enh. #124 (KGU#156): Generalized runtime data visualisation
 *      Kay Gürtzig     2016.04.01      Issue #145 (KGU#162): Comment is yet to be shown in switchText mode
 *      Kay Gürtzig     2016.04.24      Issue #169: Method findSelected() introduced, copy() modified (KGU#183)
 *      Kay Gürtzig     2016.07.21      KGU#207: Slight performance improvement in getElementByCoord()
 *      Kay Gürtzig     2016.07.25      Issue #87: Icon for collapsed state corrected (KGU#217)
 *      Kay Gürtzig     2016.07.31      Enh. #128: New mode "comments plus text" supported, drawing code revised
 *                                      (text placement improved, had sometimes exceeded the bounds)
 *      Kay Gürtzig     2016.10.13      Enh. #270: Hatched overlay texture in draw() if disabled
 *      Kay Gürtzig     2016.11.22      Bugfix #294: With hidden default branch, a test coverage couldn't be achieved
 *      Kay Gürtzig     2016.11.24/25   Issue #294 refined (now distinguished among deep and shallow test coverage)
 *      Kay Gürtzig     2016.02.08      Issue #198: vertical cursor traversal fixed (failed in nested Calls)
 *                                      Inheritance changed to implement o more intuitive horizontal cursor navigation
 *
 ******************************************************************************************************
 *
 *      Comment:		/
 *
 ******************************************************************************************************
 */

import java.util.HashMap;
import java.util.Vector;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Point;

import javax.swing.ImageIcon;

import lu.fisch.graphics.*;
import lu.fisch.structorizer.gui.IconLoader;
import lu.fisch.utils.*;


public class Case extends Element implements IFork
{
	
    public Vector<Subqueue> qs = new Vector<Subqueue>();

    //private Rect r = new Rect();
    private int fullWidth = 0;
    private int maxHeight = 0;
    // START KGU#136 2016-03-01: Bugfix #97 - cache the upper left corners of all branches
    private Vector<Integer> x0Branches = new Vector<Integer>();
    private int y0Branches = 0;
    // END KGU#136 2016-03-01
    // START KGU#227 2016-07-31: Enh. #128
    private Rect commentRect = new Rect();
    // END KGU#227 2016-07-31

	// START KGU#258 2016-09-26: Enh. #253
	private static final String[] relevantParserKeys = {"preCase", "postCase"};
	// END KGU#258 2016-09-25
	
    // START KGU#91 2015-12-01: Bugfix #39 - Case may NEVER EVER interchange text and comment!
	/**
	 * Returns the content of the text field. Full stop. No swapping here!
	 * @return the text StringList
	 */
    @Override
	public StringList getText(boolean _ignored)
	{
		return getText();
	}

    /* (non-Javadoc)
     * @see lu.fisch.structorizer.elements.Element#getComment(boolean)
     */
    @Override
	public StringList getComment(boolean _alwaysTrueComment)
	{
    	// START KGU#172 2016-04-01: Bugfix #145
		//return getComment();
        if (!_alwaysTrueComment && this.isSwitchTextCommentMode())
        {
        	return StringList.getNew(text.get(0));
        }
        else
        {
        	return comment;
        }
		// END KGU#172 2016-04-01
	}
    // END KGU#91 2015-12-01
    
    
    @Override
    public void setText(String _text)
    {
// START KGU#91 2015-12-01: D.R.Y. - just employ setText(StringList)
    	text.setText(_text);	// Convert to a StringList
    	this.setText(text);
    	
// END KGU#91 2015-12-01

    }

    @Override
    public void setText(StringList _textList)
    {
            Subqueue s = null;

            text=_textList;

            if (qs==null)
            {
                qs = new Vector<Subqueue>();
            }

            // START KGU#91 2015-12-01: Bugfix #39: Don't allow sizes below 2 branches!
            // And don't use method getText() here!
            // There is always a Subqueue for the default branch, even if the default branch
            // is suppressed. Many methods, particularly in the generators, rely upon this!
            while (text.count() < 3)
            {
                text.add("?");
            }
            if (text.get(0).isEmpty())
            {
                text.set(0, "???");
            }
            // END KGU#91 2015-12-01
            while (text.count()-1 > qs.size())
            {
                s = new Subqueue();
                s.parent = this;
                qs.add(s);
            }
            while (text.count()-1 < qs.size())
            {
                qs.removeElementAt(qs.size()-1);
            }
            // END KGU#91 2015-12-01

    }

    public Case()
    {
            super();
    }

    public Case(String _strings)
    {
            super(_strings);
            setText(_strings);
    }

    public Case(StringList _strings)
    {
            super(_strings);
            setText(_strings);
    }

    // START KGU#227 2016-07-31: Apparently helpful method
    protected boolean hasDefaultBranch()
    {
        int nLines = text.count();
        return nLines > 1 && !text.get(nLines-1).trim().equals("%");
    }
    // END KGU#227 2016-07-31
    
    public Rect prepareDraw(Canvas _canvas)
    {
            // START KGU#136 2016-03-01: Bugfix #97
            if (this.isRectUpToDate) return rect0;
            
            this.x0Branches.clear();
            this.y0Branches = 0;
            // END KGU#136 2016-03-01
            
            // KGU#136 2016-02-27: Bugfix #97 - all rect references replaced by rect0
            if (isCollapsed()) 
            {
            	rect0 = Instruction.prepareDraw(_canvas, getCollapsedText(), this);
        		// START KGU#136 2016-03-01: Bugfix #97
        		isRectUpToDate = true;
        		// END KGU#136 2016-03-01
            	return rect0;
            }

            rect0.top = 0;
            rect0.left = 0;

            FontMetrics fm = _canvas.getFontMetrics(font);

            // Lest the sum of the paddings per branch should gather too many lost remainders 
            int padding = 2 * (E_PADDING/2);
            rect0.right = padding;

            int nBranches = getText().count() - 1;

            // Width of the header
            // KGU#91 2015-12-01: Bugfix #39. Though an empty Case text doesn't make sense, the code shouldn't run havoc
            // START KGU#172 2016-04-01: Bugfix #145 in switch text/comment mode we must present the entire comment here
//            if (nBranches > 0)
//            {
//            	if (getText().get(nBranches).equals("%")) nBranches--;
//            	rect0.right = Math.max(padding, getWidthOutVariables(_canvas, getText().get(0), this) + padding);
//            }
            StringList selectorLines = new StringList();
            if (nBranches > 0)
            {
            	if (getText().get(nBranches).equals("%")) nBranches--;
            	selectorLines.add(getText().get(0));
            }
        	if (this.isSwitchTextCommentMode())
        	{
        		selectorLines = this.getComment();
        	}
        	// FIXME: The required extra padding must be proportional to the fontsize
        	int extrapadding = padding + (selectorLines.count()-1) * (3 * padding + fm.getHeight());
        	// START KGU#227 2016-07-31: Enh. #128 - compute the dimensions of the comment area
        	commentRect = new Rect();
        	if (Element.E_COMMENTSPLUSTEXT)
        	{
        		commentRect = this.writeOutCommentLines(_canvas, 0, 0, false, false);
        		rect0.right = Math.max(rect0.right, commentRect.right + extrapadding);
        	}
        	// END KGU#227 2016-07-31
        	for (int i = 0; i < selectorLines.count(); i++)
        	{
        		rect0.right = Math.max(rect0.right, getWidthOutVariables(_canvas, selectorLines.get(i), this) + extrapadding);
        	}
        	// END KGU#172 2016-04-01
            // Total width of the branches
            int width = 0;
            int[] textWidths = new int[nBranches];
            for(int i = 0; i < nBranches; i++)
            {
            	// Instead of computing the text width three times (!?) we just store the result the first time
            	// FIXME (KGU): By the way, why don't we do it right (i.e. including substructure) in the first place?
            	textWidths[i] = getWidthOutVariables(_canvas, getText().get(i+1), this) + padding/2; 
            	width += textWidths[i];
            }
        	if (rect0.right < width)
        	{
        		rect0.right = width;
        	}

        	// START KGU#172 2016-04-01: Bugfix #144: The header my contain more than one line if comments are visible
            //rect0.bottom = 2 * (padding) + 2 * fm.getHeight();
            rect0.bottom = 2 * (padding) + (selectorLines.count() + 1) * fm.getHeight();
            // END KGU#172 2016-04-01
        	// START KGU#227 2016-07-31: Enh. #128 - add the height if the comment area
            rect0.bottom += commentRect.bottom;
            // END KGU#227 2016-07-31
            // START KGU#136 2016-03-01: Bugfix #97
            this.y0Branches = rect0.bottom;
            // END KGU#136 2016-03-01

            //Rect rtt = null;

            fullWidth = 0;
            maxHeight = 0;

            if (qs.size() > 0)
            {
            	for (int i = 0; i < nBranches; i++)
            	{
            		// START KGU#136 2016-03-01: Bugfix #97
            		x0Branches.addElement(fullWidth);
            		// END KGU#136 2016-03-01
            		Rect rtt = qs.get(i).prepareDraw(_canvas);
            		fullWidth = fullWidth + Math.max(rtt.right, textWidths[i]);
            		if (maxHeight < rtt.bottom)
            		{
            			maxHeight = rtt.bottom;
            		}
            	}
            }

            rect0.right = Math.max(rect0.right, fullWidth);
            rect0.bottom = rect0.bottom + maxHeight;

    		// START KGU#136 2016-03-01: Bugfix #97
    		isRectUpToDate = true;
    		// END KGU#136 2016-03-01
    		return rect0;
    }

    public void draw(Canvas _canvas, Rect _top_left)
    {
    	if(isCollapsed()) 
    	{
    		Instruction.draw(_canvas, _top_left, getCollapsedText(), this);
    		return;
    	}
    	// START KGU#172 2016-04-01: Bugfix #145
    	boolean isSwitchMode = this.isSwitchTextCommentMode();
    	// END KGU#172 2016-04-01

    	Rect myrect = new Rect();
    	// START KGU 2015-10-13: All highlighting rules now encapsulated by this new method
    	Color drawColor = getFillColor();
    	// END KGU 2015-10-13
    	FontMetrics fm = _canvas.getFontMetrics(Element.font);

    	Canvas canvas = _canvas;
    	canvas.setBackground(drawColor);
    	canvas.setColor(drawColor);

    	// START KGU#136 2016-03-01: Bugfix #97 - store rect in 0-bound (relocatable) way
    	//rect = _top_left.copy();
    	rect = new Rect(0, 0, 
    			_top_left.right - _top_left.left, _top_left.bottom - _top_left.top);
    	Point ref = this.getDrawPoint();
    	this.topLeft.x = _top_left.left - ref.x;
    	this.topLeft.y = _top_left.top - ref.y;
    	// END KGU#136 2016-03-01

    	int minHeight = 2 * fm.getHeight() + 4 * (E_PADDING / 2);
    	// START KGU#172 2016-04-01: Bugfix #145 - we might have to put several comment lines in here
    	StringList headerText = StringList.getNew(this.getText().get(0));
    	if (isSwitchMode)
    	{
    		headerText = this.getComment();
    	}
    	if (headerText.count() > 1)
    	{
    		minHeight += (headerText.count() - 1) * fm.getHeight();
    	}
    	// END KGU#172 2016-04-01
    	// START KGU#227 2016-07-31: Enh. #128 - add the height of the embedded comment
    	minHeight += commentRect.bottom;
    	// END KGU#227 2016-07-31

    	// fill shape
    	canvas.setColor(drawColor);
    	myrect = _top_left.copy();
    	myrect.left += 1;
    	myrect.top += 1;
    	//myrect.bottom -= 1;
    	myrect.bottom = _top_left.top + minHeight;
    	//myrect.right-=1;
    	canvas.fillRect(myrect);

    	// draw shape
    	myrect = _top_left.copy();
    	myrect.bottom = _top_left.top + minHeight;

    	int y = myrect.top + E_PADDING;
    	int a = myrect.left + (myrect.right - myrect.left) / 2;
    	int b = myrect.top;
    	int c = myrect.left + fullWidth-1;
    	int d = myrect.bottom-1;
    	// About the horizontal position of the cleave
    	int x = ((y-b)*(c-a) + a*(d-b)) / (d-b);
    	
    	// START KGU#227 2016-07-31: Enh. #128
    	boolean hasDefaultBranch = this.hasDefaultBranch();
    	// Draw the 1st line of the comment if requested
    	if (Element.E_COMMENTSPLUSTEXT)
    	{
    		// Perfect right-bound position if there is no default branch
    		int xStart = myrect.right - commentRect.right - E_PADDING/2;
    		// Otherwise use the calculated weighted centre
    		if (hasDefaultBranch)
    		{
    			xStart = Math.min(xStart, x - commentRect.right/2);
    		}
    		this.writeOutCommentLines(_canvas,
    				xStart,
    				myrect.top + E_PADDING / 3,
    				true, false);
    	}
    	// END KGU#227 2016-07-31

    	// draw the selection expression (text 0)
    	// START KGU#91 2015-12-01: Bugfix #39 Nonsense replaced
    	//for (int i=0; i<1; i++)
    	int nLines = this.getText().count();
    	if (nLines > 0)
    	// END KGU#91 2015-12-01
    	{
    		canvas.setColor(Color.BLACK);
    		// START KGU#172 2016-04-01: Bugfix #145
//    		String text = this.getText().get(0);	// Text can't be empty, see setText()
//    		int divisor = 2;
//    		if (nLines > 1 && this.getText().get(nLines-1).equals("%")) divisor = nLines;
//    		writeOutVariables(canvas,
//    				x - getWidthOutVariables(_canvas, text, this) / divisor,
//    				myrect.top + E_PADDING / 3 + fm.getHeight(),
//    				text,this
//    				);
    		StringList text = StringList.getNew(this.getText().get(0));	// Text can't be empty, see setText()
    		if (isSwitchMode)
    		{
    			text = this.getComment();
    		}
      		int divisor = 2;
    		if (nLines > 1 && hasDefaultBranch) divisor = nLines;
    		for (int ln = 0; ln < text.count(); ln++)
    		{
    			int textWidth = getWidthOutVariables(_canvas, text.get(ln), this);
    			// Without default branch all text can be placed right-bound
    			int xStart = myrect.right - textWidth - E_PADDING/2;
    			// With default branch we should centre it weightedly 
    			if (hasDefaultBranch)
    			{
    				xStart = Math.min(xStart, x - textWidth / divisor);
    			}
    	  		writeOutVariables(canvas,
        				xStart,
        				// START KGU#227 2016-07-31: Enh. #128 - consider comment
        				//myrect.top + E_PADDING / 3 + (ln + 1) * fm.getHeight(),
        				myrect.top + E_PADDING / 3 + commentRect.bottom + (ln + 1) * fm.getHeight(),
        				// END KGU#227 2016-07-31
        				text.get(ln), this
        				);
        			
    		}
    		// END KGU#172 2016-04-01

    		// START KGU#156 2016-03-11: Enh. #124
    		// write the run-time info if enabled
    		this.writeOutRuntimeInfo(canvas, myrect.right - (hasDefaultBranch ? Element.E_PADDING : Element.E_PADDING/2), myrect.top);
    		// END KGU#156 2016-03-11

    	}


    	// draw comment
    	if(Element.E_SHOWCOMMENTS==true && !comment.getText().trim().equals(""))
    	{
    		this.drawCommentMark(canvas, myrect);
    	}
    	// START KGU 2015-10-11
    	// draw breakpoint bar if necessary
    	this.drawBreakpointMark(canvas, myrect);
    	// END KGU 2015-10-11


    	// draw lines
    	canvas.setColor(Color.BLACK);
    	int lineWidth = 0;
    	// if the last line is '%', do not draw an else part
    	int count = nLines - 2;
    	Rect rtt = null;

    	// START KGU#91 2015-12-01: Performance optimisation on occasion of bugfix #39
    	int[] textWidths = new int[count+1];
    	// END KGU#91 2015-12-01

    	for(int i = 0; i < count; i++)
    	{
    		rtt=((Subqueue) qs.get(i)).prepareDraw(_canvas);
    		// START KGU#91 2015-12-01: Once to calculate it is enough
    		//lineWidth = lineWidth + Math.max(rtt.right, getWidthOutVariables(_canvas, getText().get(i+1), this) + Math.round(E_PADDING / 2));
    		textWidths[i] = getWidthOutVariables(_canvas, getText().get(i+1), this);
    		lineWidth += Math.max(rtt.right, textWidths[i] + E_PADDING / 2);
    		// END KGU#91 2015-12-01
    	}

    	// START KGU#91 2015-12-01: Bugfix #39: We should be aware of pathological cases...
    	//if(  ((String) getText().get(getText().count()-1)).equals("%") )
    	if( !hasDefaultBranch )
    		// END KGU#91 2015-12-01
    	{
    		lineWidth = _top_left.right;
    	}
    	// START KGU#91 2015-12-01
    	else {
    		textWidths[count] = getWidthOutVariables(_canvas, getText().get(count+1), this);
    	}
    	// END KGU#91 2015-12-01

    	int ax = myrect.left;
    	int ay = myrect.top;
    	int bx = myrect.left + lineWidth;
    	int by = myrect.bottom-1 - fm.getHeight() - E_PADDING / 2;

    	// START KGU#91 2015-12-01: Bugfix #39: We should be aware of pathological cases...
    	//if(  ((String) getText().get(getText().count()-1)).equals("%") )
    	if( !hasDefaultBranch )
    		// END KGU#91 2015-12-01
    	{
    		bx = myrect.right;
    	}

    	canvas.moveTo(ax,ay);
    	canvas.lineTo(bx,by);

    	// START KGU#91 2015-12-01: Bugfix #39
    	//if( ! ((String) text.get(text.count()-1)).equals("%") )
    	if ( hasDefaultBranch )
    		// END KGU#91 2015-12-01
    	{
    		canvas.lineTo(bx, myrect.bottom-1);
    		canvas.lineTo(bx, by);
    		canvas.lineTo(myrect.right, myrect.top);
    	}
		// START KGU#277 2016-10-13: Enh. #270
		if (this.disabled) {
			canvas.hatchRect(myrect, 5, 10);
		}
		// END KGU#277 2016-10-13


    	// draw children
    	myrect = _top_left.copy();
    	myrect.top = _top_left.top + minHeight -1;

    	if (qs.size()!=0)
    	{

    		// if the last line isn't '%', then draw an else part
    		//count = qs.size()-1;	// FIXME: On editing, this might be greater than nLines!
    		// START KGU#91 2015-12-01: Bugfix #39
    		//if( ((String) getText().get(getText().count()-1)).equals("%"))
    		if (hasDefaultBranch)
    			// END KGU 2015-12-01
    		{
    			count++;
    		}

    		for(int i = 0; i < count ; i++)
    		{

    			rtt=((Subqueue) qs.get(i)).prepareDraw(_canvas);

    			if (i==count-1)
    			{
    				myrect.right = _top_left.right;
    			}
    			else
    			{
    				// START KGU#91 2015-12-01
    				//myrect.right=myrect.left+Math.max(rtt.right,getWidthOutVariables(_canvas,getText().get(i+1),this)+Math.round(E_PADDING / 2))+1;
    				myrect.right = myrect.left + Math.max(rtt.right, textWidths[i] + E_PADDING / 2) + 1;
    				// END KGU#91-12-01
    			}

    			// draw child
    			((Subqueue) qs.get(i)).draw(_canvas,myrect);

    			// draw criterion text
    			writeOutVariables(canvas,
    					// START KGU#91 2015-12-01: Performance may be improved here
    					//myrect.right + (myrect.left-myrect.right) / 2 - Math.round(getWidthOutVariables(_canvas,getText().get(i+1),this) / 2),
    					myrect.right + (myrect.left-myrect.right) / 2 - textWidths[i] / 2,
    					// END KGU#91 2915-12-01
    					myrect.top - E_PADDING / 4, //+fm.getHeight(),
    					getText().get(i+1),this);

    			// draw bottom up line
    			if((i != qs.size()-2) && (i != count-1))
    			{
    				canvas.moveTo(myrect.right-1,myrect.top);
    				int mx = myrect.right-1;
    				//int my = myrect.top-fm.getHeight();
    				int sx = mx;
    				int sy = (sx*(by-ay) - ax*by + ay*bx) / (bx-ax);
    				canvas.lineTo(sx,sy+1);
    			}

    			myrect.left = myrect.right-1;

    		}
    	}

    	canvas.setColor(Color.BLACK);
    	canvas.drawRect(_top_left);
    }

    // START KGU#122 2016-01-03: Enh. #87 - Collapsed elements may be marked with an element-specific icon
    @Override
    protected ImageIcon getIcon()
    {
    	// START KGU#217 2016-07-25: Issue #87 - Was wrong icon number
    	//return IconLoader.ico057;
    	return IconLoader.ico064;
    	// END KGU#217 2016-07-25
    }
    // END KGU#122 2016-01-03

    @Override
    public Element getElementByCoord(int _x, int _y, boolean _forSelection)
    {
    	Element selMe = super.getElementByCoord(_x, _y, _forSelection);
		// START KGU#121 2016-01-03: Bugfix #87 - A collapsed element has no visible substructure!
    	// START KGU#207 2016-07-21: If this element isn't hit then there is no use searching the substructure
		//if (!this.isCollapsed())
		if (!this.isCollapsed() && (selMe != null || _forSelection))
		// START KGU#207 2016-07-21
		{
		// END KGU#121 2016-01-03
			Element selCh = null;

			// START KGU#296 2016-11-22: Bugfix #294 - ignore default branch if hidden
			//for(int i = 0; i < qs.size(); i++)
			int nBranches = qs.size();
			if (!hasDefaultBranch()) nBranches--;
			for(int i = 0; i < nBranches; i++)
			// END KGU#296 2016-11-22
			{
				// START KGU#136 2016-03-01: Bugfix #97
				//Element pre = ((Subqueue) qs.get(i)).getElementByCoord(_x,_y, _forSelection);
				int xOff = rect0.right;
				if (i < x0Branches.size()) {
					xOff = x0Branches.get(i);
				}
				// START KGU#346 2017-02-08: Bugfix #198: Failed with higher nesting level
				//Element pre = qs.get(i).getElementByCoord(_x-xOff, _y-y0Branches, _forSelection);
				Element pre = qs.get(i).getElementByCoord(_x-xOff, _y-y0Branches+1, _forSelection);
				// END KGU#346 2017-02-08
				// END KGU#136 2016-03-01
				if (pre!=null)
				{
					selCh = pre;
				}
			}

			if(selCh!=null)
			{
				if (_forSelection) selected = false;
				selMe = selCh;
			}
		// START KGU#121 2016-01-03: Bugfix #87 (continued)
		}
		// END KGU#121 2016-01-03

    	return selMe;
    }
    // END KGU 2015-10-09
    
	// START KGU#346 2017-02-08: Issue #198 Provide a relative rect for the head
	/**
	 * Returns a copy of the (relocatable i. e. 0-bound) extension rectangle
	 * of the head partition (discriminating expression and branch labels). 
	 * @return a rectangle starting at (0,0) and spanning to (width, head height) 
	 */
	public Rect getHeadRect()
	{
		return new Rect(rect.left, rect.top, rect.right, this.y0Branches);
	}
	// END KGU#346 2017-02-08

	// START KGU#183 2016-04-24: Issue #169 
	/* (non-Javadoc)
	 * @see lu.fisch.structorizer.elements.Element#findSelected()
	 */
	public Element findSelected()
	{
		Element sel = selected ? this : null;
		for (int i = 0; sel == null && i < this.qs.size(); i++)
		{
			sel = qs.elementAt(i).findSelected();
		}
		return sel;
	}
	// END KGU#183 2016-04-24
	    
    public Element copy() // Problem here???
    {
            Element ele = new Case(this.getText().getText());
            copyDetails(ele, true);
            ((Case) ele).qs.clear();
            for(int i=0; i < qs.size(); i++)
            {
                    Subqueue ss = (Subqueue) ((Subqueue) this.qs.get(i)).copy();
                    ss.parent=ele;
                    ((Case) ele).qs.add(ss);
            }
            return ele;
    }

	// START KGU#119 2016-01-02: Bugfix #78
	/**
	 * Returns true iff _another is of same class, all persistent attributes are equal, and
	 * all substructure of _another recursively equals the substructure of this. 
	 * @param another - the Element to be compared
	 * @return true on recursive structural equality, false else
	 */
	@Override
	public boolean equals(Element _another)
	{
		boolean isEqual = super.equals(_another) && this.qs.size() == ((Case)_another).qs.size();
		for(int i = 0; isEqual && i < this.qs.size(); i++)
		{
			isEqual = this.qs.get(i).equals(((Case)_another).qs.get(i));
		}
		return isEqual;
	}
	// END KGU#119 2016-01-02

	// START KGU#117 2016-03-07: Enh. #77
	/* (non-Javadoc)
	 * @see lu.fisch.structorizer.elements.Element#combineCoverage(lu.fisch.structorizer.elements.Element)
	 */
	@Override
	public boolean combineRuntimeData(Element _cloneOfMine)
	{
		boolean isEqual = super.combineRuntimeData(_cloneOfMine);
		// START KGU#296 2016-11-22: Bugfix #294 - Ignore default branch if hidden.
		//for(int i = 0; isEqual && i < this.qs.size(); i++)
		int nBranches = this.qs.size();
		if (!this.hasDefaultBranch()) nBranches--;
		for(int i = 0; isEqual && i < nBranches; i++)
		// END KGU#296 2016-11-22
		{
			isEqual = this.qs.get(i).combineRuntimeData(((Case)_cloneOfMine).qs.get(i));
		}
		return isEqual;
	}
	// END KGU#117 2016-03-07

	// START KGU#156 2016-03-13: Enh. #124
	protected String getRuntimeInfoString()
	{
		String info = this.getExecCount() + " / ";
		String stepInfo = null;
		switch (E_RUNTIMEDATAPRESENTMODE)
		{
		case TOTALSTEPS_LIN:
		case TOTALSTEPS_LOG:
			stepInfo = Integer.toString(this.getExecStepCount(true));
			if (!this.isCollapsed()) {
				stepInfo = "(" + stepInfo + ")";
			}
			break;
		default:
			stepInfo = Integer.toString(this.getExecStepCount(this.isCollapsed()));
		}
		return info + stepInfo;
	}
	// END KGU#156 2016-03-11

	// START KGU#117 2016-03-06: Enh. #77
	/* (non-Javadoc)
	 * @see lu.fisch.structorizer.elements.Element#isTestCovered(boolean)
	 */
	public boolean isTestCovered(boolean _deeply)
	{
		boolean covered = true;
    	if (qs!= null)
    	{
    		// START KGU#296 2016-11-22: Issue #294 - hidde default branch prevented full coverage
    		//for (int i = 0; covered && i < qs.size(); i++)
    		int nBranches = qs.size();
    		// START KGU#296 2016-11-24: Issue #294: For deep coverage the hidden branch is also to be checked
    		//if (!hasDefaultBranch()) {
    		if (!_deeply && !hasDefaultBranch()) {
    		// END KGU#296 2016-11-24
    			nBranches--;
    		}
    		for (int i = 0; covered && i < nBranches; i++)
    		// END KGU#296 2016-11-22
    		{
    			covered = qs.get(i).isTestCovered(_deeply);
    		}
    	}		
		return covered;
	}
	// END KGU#117 2016-03-06

	// START KGU 2015-10-16
	/* (non-Javadoc)
	 * @see lu.fisch.structorizer.elements.Element#addFullText(lu.fisch.utils.StringList, boolean)
	 */
	@Override
    protected void addFullText(StringList _lines, boolean _instructionsOnly)
    {
    	if (!_instructionsOnly) {
    		_lines.add(this.getText());	// Text of the condition
    	}
    	if (qs!= null)
    	{
    		int nBranches = qs.size();
    		if (!hasDefaultBranch()) nBranches--;
    		for (int i = 0; i < nBranches; i++)
    		{
    			qs.get(i).addFullText(_lines, _instructionsOnly);
    		}
    	}
    }
    // END KGU 2015-10-16

	// START KGU#199 2016-07-07: Enh. #188 - ensure Call elements for known subroutines
	/* (non-Javadoc)
	 * @see lu.fisch.structorizer.elements.Element#convertToCalls(lu.fisch.utils.StringList)
	 */
	@Override
	public void convertToCalls(StringList _signatures) {
    	if (qs!= null)
    	{
    		for (int i = 0; i < qs.size(); i++)
    		{
    			qs.get(i).convertToCalls(_signatures);
    		}
    	}
	}
	// END KGU#199 2016-07-07

	/* (non-Javadoc)
	 * @see lu.fisch.structorizer.elements.Element#traverse(lu.fisch.structorizer.elements.IElementVisitor)
	 */
	@Override
	public boolean traverse(IElementVisitor _visitor) {
		boolean proceed = _visitor.visitPreOrder(this);
		if (qs != null) {
			for (int i = 0; proceed && i < qs.size(); i++)
			{
				proceed = qs.get(i).traverse(_visitor);
			}
		}
		if (proceed)
		{
			proceed = _visitor.visitPostOrder(this);
		}
		return proceed;
	}

	// START KGU#258 2016-09-26: Enh. #253 - This may have to be moved to Element for live refactoring
	/* (non-Javadoc)
	 * @see lu.fisch.structorizer.elements.Element#getRelevantParserKeys()
	 */
	@Override
	protected String[] getRelevantParserKeys() {
		return relevantParserKeys;
	}

    /* (non-Javadoc)
     * @see lu.fisch.structorizer.elements.Element#refactorKeywords(java.util.HashMap, boolean)
     */
	@Override
    public void refactorKeywords(HashMap<String, StringList> _splitOldKeywords, boolean _ignoreCase)
    {
    	String[] relevantKeywords = getRelevantParserKeys();
    	if (text.count() > 0)
    	{
    		text.set(0, refactorLine(text.get(0), _splitOldKeywords, relevantKeywords, _ignoreCase));
    		relevantKeywords = new String[]{"postCase"};
    		for (int i = 1; i < text.count(); i++)
    		{
    			if (!text.get(i).equals("%"))
    			{
    				text.set(i, refactorLine(text.get(i), _splitOldKeywords, relevantKeywords, _ignoreCase));
    			}
    		}
    	}
	}
	// END KGU#258 2016-09-25

}
