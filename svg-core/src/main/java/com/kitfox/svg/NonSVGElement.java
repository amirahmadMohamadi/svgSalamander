/*
 * Copyright (c) 2024, Mohammadi
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.kitfox.svg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Mohammadi
 */
public class NonSVGElement implements Serializable {

    protected String tag;
    protected String qName;
    protected String nsUri;

    protected NonSVGElement parent;
    protected final HashMap<String, String> attributes = new HashMap<>();
    protected final ArrayList<NonSVGElement> children = new ArrayList<>();

    public NonSVGElement() {
        this.tag = null;
        this.parent = null;
    }

    public String getTagName() {
        return this.tag;
    }

    public void setTagName(String tag) {
        this.tag = tag;
    }

    public String getQName() {
        return qName;
    }

    public void setQName(String qName) {
        this.qName = qName;
    }

    public String getNsUri() {
        return nsUri;
    }

    public void setNsUri(String nsUri) {
        this.nsUri = nsUri;
    }

    public NonSVGElement getParent() {
        return this.parent;
    }

    public void setParent(NonSVGElement parent) {
        this.parent = parent;
    }

    /**
     * @param retVec - A list to add all children to. If null, a new list is
     * created and children of this group are added.
     *
     * @return The list containing the children of this group
     */
    public List<NonSVGElement> getChildren(List<NonSVGElement> retVec) {
        if (retVec == null) {
            retVec = new ArrayList<>();
        }

        retVec.addAll(children);

        return retVec;
    }

    public void addChild(NonSVGElement element)
    {
        this.children.add(element);
    }
    
    public HashMap<String, String> getAttributes()
    {
        return this.attributes;
    }
}
