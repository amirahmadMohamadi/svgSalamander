/*
 * SVG Salamander
 * Copyright (c) 2004, Mark McKay
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or 
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 *   - Redistributions of source code must retain the above 
 *     copyright notice, this list of conditions and the following
 *     disclaimer.
 *   - Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials 
 *     provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE. 
 * 
 * Mark McKay can be contacted at mark@kitfox.com.  Salamander and other
 * projects can be found at http://www.kitfox.com
 *
 * Created on January 26, 2004, 1:59 AM
 */
package com.kitfox.svg;

import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.animation.TrackBase;
import com.kitfox.svg.animation.TrackManager;
import com.kitfox.svg.pathcmd.BuildHistory;
import com.kitfox.svg.pathcmd.PathCommand;
import com.kitfox.svg.pathcmd.PathParser;
import com.kitfox.svg.xml.StyleAttribute;
import com.kitfox.svg.xml.StyleSheet;
import com.kitfox.svg.xml.XMLParseUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
abstract public class SVGElement implements Serializable
{

    public static final long serialVersionUID = 0;
    public static final String SVG_NS = "http://www.w3.org/2000/svg";
    protected SVGElement parent = null;
    protected final ArrayList<SVGElement> children = new ArrayList<>();
    protected String id = null;
    /**
     * CSS class. Used for applying style sheet information.
     */
    protected String cssClass = null;
    /**
     * Styles defined for this element via the <b>style</b> attribute.
     */
    protected final HashMap<String, StyleAttribute> inlineStyles = new HashMap<>();
    /**
     * Presentation attributes set for this element. Ie, any attribute other
     * than the <b>style</b> attribute.
     */
    protected final HashMap<String, StyleAttribute> presAttribs = new HashMap<>();
    /**
     * This element may override the URI we resolve against with an xml:base
     * attribute. If so, a copy is placed here. Otherwise, we defer to our
     * parent for the resolution base
     */
    protected URI xmlBase = null;
    /**
     * The diagram this element belongs to
     */
    protected SVGDiagram diagram;
    /**
     * Link to the universe we reside in
     */
    protected final TrackManager trackManager = new TrackManager();
    boolean dirty = true;

    /**
     * Creates a new instance of SVGElement
     */
    public SVGElement()
    {
        this(null, null, null);
    }

    public SVGElement(String id, SVGElement parent)
    {
        this(id, null, parent);
    }

    public SVGElement(String id, String cssClass, SVGElement parent)
    {
        this.id = id;
        this.cssClass = cssClass;
        this.parent = parent;
    }

    abstract public String getTagName();

    public SVGElement getParent()
    {
        return parent;
    }

    void setParent(SVGElement parent)
    {
        this.parent = parent;
    }
    
    /**
     * @param retVec
     * @return an ordered list of nodes from the root of the tree to this node
     */
    public List<SVGElement> getPath(List<SVGElement> retVec)
    {
        if (retVec == null)
        {
            retVec = new ArrayList<>();
        }

        if (parent != null)
        {
            parent.getPath(retVec);
        }
        retVec.add(this);

        return retVec;
    }

    /**
     * @param retVec - A list to add all children to. If null, a new list is
     * created and children of this group are added.
     *
     * @return The list containing the children of this group
     */
    public List<SVGElement> getChildren(List<SVGElement> retVec)
    {
        if (retVec == null)
        {
            retVec = new ArrayList<>();
        }

        retVec.addAll(children);

        return retVec;
    }

    /**
     * @param id - Id of svg element to return
     * @return the child of the given id, or null if no such child exists.
     */
    public SVGElement getChild(String id)
    {
        for (SVGElement ele : children) {
            String eleId = ele.getId();
            if (eleId != null && eleId.equals(id))
            {
                return ele;
            }
        }

        return null;
    }

    /**
     * Searches children for given element. If found, returns index of child.
     * Otherwise returns -1.
     * @param child
     * @return index of child
     */
    public int indexOfChild(SVGElement child)
    {
        return children.indexOf(child);
    }

    /**
     * Swaps 2 elements in children.
     *
     * @param i index of first child
     * @param j index of second child
     * @throws com.kitfox.svg.SVGException
     */
    public void swapChildren(int i, int j) throws SVGException
    {
        if ((children == null) || (i < 0) || (i >= children.size()) || (j < 0) || (j >= children.size()))
        {
            return;
        }

        SVGElement temp = children.get(i);
        children.set(i, children.get(j));
        children.set(j, temp);
        build();
    }

    /**
     * Called during SAX load process to notify that this tag has begun the
     * process of being loaded
     *
     * @param attrs - Attributes of this tag
     * @param helper - An object passed to all SVG elements involved in this
     * build process to aid in sharing information.
     * @param parent
     * @throws org.xml.sax.SAXException
     */
    public void loaderStartElement(SVGLoaderHelper helper, Attributes attrs, SVGElement parent) throws SAXException
    {
        //Set identification info
        this.parent = parent;
        this.diagram = helper.diagram;

        this.id = attrs.getValue("id");
        if (this.id != null && !this.id.equals(""))
        {
            this.id = this.id.intern();
            diagram.setElement(this.id, this);
        }

        String className = attrs.getValue("class");
        this.cssClass = (className == null || className.equals("")) ? null : className.intern();
        //docRoot = helper.docRoot;
        //universe = helper.universe;

        //Parse style string, if any
        String style = attrs.getValue("style");
        if (style != null)
        {
            HashMap<?, ?> map = XMLParseUtil.parseStyle(style, inlineStyles);
        }

        String base = attrs.getValue("xml:base");
        if (base != null && !base.equals(""))
        {
            try
            {
                xmlBase = new URI(base);
            } catch (URISyntaxException e)
            {
                throw new SAXException(e);
            }
        }

        //Place all other attributes into the presentation attribute list
        int numAttrs = attrs.getLength();
        for (int i = 0; i < numAttrs; i++)
        {
            String name = attrs.getQName(i).intern();
            String value = attrs.getValue(i);

            presAttribs.put(name, new StyleAttribute(name, value == null ? null : value.intern()));
        }
    }

    public void removeAttribute(String name, int attribType)
    {
        switch (attribType)
        {
            case AnimationElement.AT_CSS:
                inlineStyles.remove(name);
                break;
            case AnimationElement.AT_XML:
                presAttribs.remove(name);
        }
    }

    public void addAttribute(String name, int attribType, String value) throws SVGElementException
    {
        if (hasAttribute(name, attribType))
        {
            throw new SVGElementException(this, "Attribute " + name + "(" + AnimationElement.animationElementToString(attribType) + ") already exists");
        }

        //Alter layout for id attribute
        if ("id".equals(name))
        {
            if (diagram != null)
            {
                diagram.removeElement(id);
                diagram.setElement(value, this);
            }
            this.id = value;
        }

        switch (attribType)
        {
            case AnimationElement.AT_CSS:
                inlineStyles.put(name, new StyleAttribute(name, value));
                return;
            case AnimationElement.AT_XML:
                presAttribs.put(name, new StyleAttribute(name, value));
                return;
        }

        throw new SVGElementException(this, "Invalid attribute type " + attribType);
    }

    public boolean hasAttribute(String name, int attribType) throws SVGElementException
    {
        switch (attribType)
        {
            case AnimationElement.AT_CSS:
                return inlineStyles.containsKey(name);
            case AnimationElement.AT_XML:
                return presAttribs.containsKey(name);
            case AnimationElement.AT_AUTO:
                return inlineStyles.containsKey(name) || presAttribs.containsKey(name);
        }

        throw new SVGElementException(this, "Invalid attribute type " + attribType);
    }

    /**
     * @return a set of Strings that correspond to CSS attributes on this element
     */
    public Set<String> getInlineAttributes()
    {
        return inlineStyles.keySet();
    }

    /**
     * @return a set of Strings that correspond to XML attributes on this element
     */
    public Set<String> getPresentationAttributes()
    {
        return presAttribs.keySet();
    }

    /**
     * Called after the start element but before the end element to indicate
     * each child tag that has been processed
     * @param helper
     * @param child
     * @throws com.kitfox.svg.SVGElementException
     */
    public void loaderAddChild(SVGLoaderHelper helper, SVGElement child) throws SVGElementException
    {
        children.add(child);
        child.parent = this;
        child.setDiagram(diagram);

        //Add info to track if we've scanned animation element
        if (child instanceof AnimationElement)
        {
            trackManager.addTrackElement((AnimationElement) child);
        }
    }

    protected void setDiagram(SVGDiagram diagram)
    {
        this.diagram = diagram;
        diagram.setElement(id, this);
        for (SVGElement ele : children) {
            ele.setDiagram(diagram);
        }
    }

    public void removeChild(SVGElement child) throws SVGElementException
    {
        if (!children.contains(child))
        {
            throw new SVGElementException(this, "Element does not contain child " + child);
        }

        children.remove(child);
    }

    /**
     * Called during load process to add text scanned within a tag
     * @param helper
     * @param text
     */
    public void loaderAddText(SVGLoaderHelper helper, String text)
    {
    }

    /**
     * Called to indicate that this tag and the tags it contains have been
     * completely processed, and that it should finish any load processes.
     * @param helper
     * @throws com.kitfox.svg.SVGParseException
     */
    public void loaderEndElement(SVGLoaderHelper helper) throws SVGParseException
    {
//        try
//        {
//            build();
//        }
//        catch (SVGException se)
//        {
//            throw new SVGParseException(se);
//        }
    }

    /**
     * Called by internal processes to rebuild the geometry of this node from
     * it's presentation attributes, style attributes and animated tracks.
     * @throws com.kitfox.svg.SVGException
     */
    protected void build() throws SVGException
    {
        StyleAttribute sty = new StyleAttribute();

        if (getPres(sty.setName("id")))
        {
            String newId = sty.getStringValue();
            if (!newId.equals(id))
            {
                diagram.removeElement(id);
                id = newId;
                diagram.setElement(this.id, this);
            }
        }
        if (getPres(sty.setName("class")))
        {
            cssClass = sty.getStringValue();
        }
        if (getPres(sty.setName("xml:base")))
        {
            xmlBase = sty.getURIValue();
        }

        //Build children
        for (int i = 0; i < children.size(); ++i)
        {
            SVGElement ele = (SVGElement) children.get(i);
            ele.build();
        }
    }

    public URI getXMLBase()
    {
        return xmlBase != null ? xmlBase
            : (parent != null ? parent.getXMLBase() : diagram.getXMLBase());
    }

    /**
     * @return the id assigned to this node. Null if no id explicitly set.
     */
    public String getId()
    {
        return id;
    }
    LinkedList<SVGElement> contexts = new LinkedList<>();

    /**
     * Hack to allow nodes to temporarily change their parents. The Use tag will
     * need this so it can alter the attributes that a particular node uses.
     * @param context
     */
    protected void pushParentContext(SVGElement context)
    {
        contexts.addLast(context);
    }

    protected SVGElement popParentContext()
    {
        return (SVGElement) contexts.removeLast();
    }

    protected SVGElement getParentContext()
    {
        return contexts.isEmpty() ? null : (SVGElement) contexts.getLast();
    }

    public SVGRoot getRoot()
    {
        return parent == null ? null : parent.getRoot();
    }

    /*
     * Returns the named style attribute.  Checks for inline styles first, then
     * internal and extranal style sheets, and finally checks for presentation
     * attributes.
     * @param styleName - Name of attribute to return
     * @param recursive - If true and this object does not contain the
     * named style attribute, checks attributes of parents abck to root until
     * one found.
     */
    public boolean getStyle(StyleAttribute attrib) throws SVGException
    {
        return getStyle(attrib, true);
    }

    public void setAttribute(String name, int attribType, String value) throws SVGElementException
    {
        StyleAttribute styAttr;


        switch (attribType)
        {
            case AnimationElement.AT_CSS:
            {
                styAttr = (StyleAttribute) inlineStyles.get(name);
                break;
            }
            case AnimationElement.AT_XML:
            {
                styAttr = (StyleAttribute) presAttribs.get(name);
                break;
            }
            case AnimationElement.AT_AUTO:
            {
                styAttr = (StyleAttribute) inlineStyles.get(name);

                if (styAttr == null)
                {
                    styAttr = (StyleAttribute) presAttribs.get(name);
                }
                break;
            }
            default:
                throw new SVGElementException(this, "Invalid attribute type " + attribType);
        }

        if (styAttr == null)
        {
            throw new SVGElementException(this, "Could not find attribute " + name + "(" + AnimationElement.animationElementToString(attribType) + ").  Make sure to create attribute before setting it.");
        }

        //Alter layout for relevant attributes
        if ("id".equals(styAttr.getName()))
        {
            if (diagram != null)
            {
                diagram.removeElement(this.id);
                diagram.setElement(value, this);
            }
            this.id = value;
        }

        styAttr.setStringValue(value);
    }

    public boolean getStyle(StyleAttribute attrib, boolean recursive) throws SVGException
    {
        return getStyle(attrib, recursive, true);
    }
    
    /**
     * Copies the current style into the passed style attribute. Checks for
     * inline styles first, then internal and extranal style sheets, and finally
     * checks for presentation attributes. Recursively checks parents.
     *
     * @param attrib - Attribute to write style data to. Must have it's name set
     * to the name of the style being queried.
     * @param recursive - If true and this object does not contain the named
     * style attribute, checks attributes of parents back to root until one
     * found.
     * @param evalAnimation
     * @return 
     * @throws com.kitfox.svg.SVGException 
     */
    public boolean getStyle(StyleAttribute attrib, boolean recursive, boolean evalAnimation)
            throws SVGException
    {
        String styName = attrib.getName();

        //Check for local inline styles
        StyleAttribute styAttr = (StyleAttribute)inlineStyles.get(styName);

        attrib.setStringValue(styAttr == null ? "" : styAttr.getStringValue());

        //Evalutate corresponding track, if one exists
        if (evalAnimation)
        {
            TrackBase track = trackManager.getTrack(styName, AnimationElement.AT_CSS);
            if (track != null)
            {
                track.getValue(attrib, diagram.getUniverse().getCurTime());
                return true;
            }
        }

        //Return if we've found a non animated style
        if (styAttr != null)
        {
            return true;
        }


        //Check for presentation attribute
        StyleAttribute presAttr = (StyleAttribute)presAttribs.get(styName);

        attrib.setStringValue(presAttr == null ? "" : presAttr.getStringValue());

        //Evalutate corresponding track, if one exists
        if (evalAnimation)
        {
            TrackBase track = trackManager.getTrack(styName, AnimationElement.AT_XML);
            if (track != null)
            {
                track.getValue(attrib, diagram.getUniverse().getCurTime());
                return true;
            }
        }

        //Return if we've found a presentation attribute instead
        if (presAttr != null)
        {
            return true;
        }

        //Check for style sheet
        SVGRoot root = getRoot();
        if (root != null)
        {
            StyleSheet ss = root.getStyleSheet();
            if (ss != null)
            {
                return ss.getStyle(attrib, getTagName(), cssClass);
            }
        }

        //If we're recursive, check parents
        if (recursive)
        {
            SVGElement parentContext = getParentContext();
            if (parentContext != null)
            {
                return parentContext.getStyle(attrib, true);
            }
            if (parent != null)
            {
                return parent.getStyle(attrib, true);
            }
        }

        //Unsuccessful reading style attribute
        return false;
    }

    /**
     * @param styName
     * @return the raw style value of this attribute. Does not take the
     * presentation value or animation into consideration. Used by animations to
     * determine the base to animate from.
     */
    public StyleAttribute getStyleAbsolute(String styName)
    {
        //Check for local inline styles
        return (StyleAttribute) inlineStyles.get(styName);
    }

    /**
     * Copies the presentation attribute into the passed one.
     *
     * @param attrib
     * @return - True if attribute was read successfully
     * @throws com.kitfox.svg.SVGException
     */
    public boolean getPres(StyleAttribute attrib) throws SVGException
    {
        String presName = attrib.getName();

        //Make sure we have a corresponding presentation attribute
        StyleAttribute presAttr = (StyleAttribute) presAttribs.get(presName);

        //Copy presentation value directly
        attrib.setStringValue(presAttr == null ? "" : presAttr.getStringValue());

        //Evalutate corresponding track, if one exists
        TrackBase track = trackManager.getTrack(presName, AnimationElement.AT_XML);
        if (track != null)
        {
            track.getValue(attrib, diagram.getUniverse().getCurTime());
            return true;
        }

        //Return if we found presentation attribute
        if (presAttr != null)
        {
            return true;
        }

        return false;
    }

    /**
     * @param styName
     * @return the raw presentation value of this attribute. Ignores any
     * modifications applied by style attributes or animation. Used by
     * animations to determine the starting point to animate from
     */
    public StyleAttribute getPresAbsolute(String styName)
    {
        //Check for local inline styles
        return (StyleAttribute) presAttribs.get(styName);
    }

    private static final Pattern TRANSFORM_PATTERN = Pattern.compile("\\w+\\([^)]*\\)");
    static protected AffineTransform parseTransform(String val) throws SVGException
    {
        final Matcher matchExpression = TRANSFORM_PATTERN.matcher("");

        AffineTransform retXform = new AffineTransform();

        matchExpression.reset(val);
        while (matchExpression.find())
        {
            retXform.concatenate(parseSingleTransform(matchExpression.group()));
        }

        return retXform;
    }

    private static final Pattern WORD_PATTERN = Pattern.compile("([a-zA-Z]+|-?\\d+(\\.\\d+)?([eE]-?\\d+)?|-?\\.\\d+([eE]-?\\d+)?)");
    static public AffineTransform parseSingleTransform(String val) throws SVGException
    {
        final Matcher matchWord = WORD_PATTERN.matcher("");

        AffineTransform retXform = new AffineTransform();

        matchWord.reset(val);
        if (!matchWord.find())
        {
            //Return identity transformation if no data present (eg, empty string)
            return retXform;
        }

        String function = matchWord.group().toLowerCase();

        LinkedList<String> termList = new LinkedList<>();
        while (matchWord.find())
        {
            termList.add(matchWord.group());
        }


        double[] terms = new double[termList.size()];
        Iterator<String> it = termList.iterator();
        int count = 0;
        while (it.hasNext())
        {
            terms[count++] = XMLParseUtil.parseDouble(it.next());
        }

        //Calculate transformation
        switch (function) {
            case "matrix":
                retXform.setTransform(terms[0], terms[1], terms[2], terms[3], terms[4], terms[5]);
                break;
            case "translate":
                if (terms.length == 1)
                {
                    retXform.setToTranslation(terms[0], 0);
                } else
                {
                    retXform.setToTranslation(terms[0], terms[1]);
                }   break;
            case "scale":
                if (terms.length > 1)
                {
                    retXform.setToScale(terms[0], terms[1]);
                } else
                {
                    retXform.setToScale(terms[0], terms[0]);
                }   break;
            case "rotate":
                if (terms.length > 2)
                {
                    retXform.setToRotation(Math.toRadians(terms[0]), terms[1], terms[2]);
                } else
                {
                    retXform.setToRotation(Math.toRadians(terms[0]));
                }   break;
            case "skewx":
                retXform.setToShear(Math.toRadians(terms[0]), 0.0);
                break;
            case "skewy":
                retXform.setToShear(0.0, Math.toRadians(terms[0]));
                break;
            default:
                throw new SVGException("Unknown transform type");
        }

        return retXform;
    }

    static protected PathCommand[] parsePathList(String list)
    {
        return new PathParser(list).parsePathCommand();
    }

    static protected GeneralPath buildPath(String text, int windingRule)
    {
        PathCommand[] commands = parsePathList(text);

        int numKnots = 2;
        for (PathCommand command : commands) {
            numKnots += command.getNumKnotsAdded();
        }


        GeneralPath path = new GeneralPath(windingRule, numKnots);

        BuildHistory hist = new BuildHistory();

        for (PathCommand cmd : commands) {
            cmd.appendPath(path, hist);
        }

        return path;
    }

    /**
     * Updates all attributes in this diagram associated with a time event. Ie,
     * all attributes with track information.
     *
     * @param curTime
     * @return - true if this node has changed state as a result of the time
     * update
     * @throws com.kitfox.svg.SVGException
     */
    abstract public boolean updateTime(double curTime) throws SVGException;

    public int getNumChildren()
    {
        return children.size();
    }

    public SVGElement getChild(int i)
    {
        return (SVGElement) children.get(i);
    }

    public double lerp(double t0, double t1, double alpha)
    {
        return (1 - alpha) * t0 + alpha * t1;
    }
}
