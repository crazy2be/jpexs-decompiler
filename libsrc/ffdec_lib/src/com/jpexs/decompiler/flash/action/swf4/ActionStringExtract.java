/*
 *  Copyright (C) 2010-2014 JPEXS, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library. */
package com.jpexs.decompiler.flash.action.swf4;

import com.jpexs.decompiler.flash.action.Action;
import com.jpexs.decompiler.flash.action.model.StringExtractActionItem;
import com.jpexs.decompiler.graph.GraphTargetItem;
import com.jpexs.decompiler.graph.TranslateStack;
import java.util.HashMap;
import java.util.List;

public class ActionStringExtract extends Action {

    public ActionStringExtract() {
        super(0x15, 0);
    }

    @Override
    public String toString() {
        return "StringExtract";
    }

    @Override
    public void translate(TranslateStack stack, List<GraphTargetItem> output, HashMap<Integer, String> regNames, HashMap<String, GraphTargetItem> variables, HashMap<String, GraphTargetItem> functions, int staticOperation, String path) {
        GraphTargetItem count = stack.pop();
        GraphTargetItem index = stack.pop();
        GraphTargetItem value = stack.pop();
        stack.push(new StringExtractActionItem(this, value, index, count));
    }
}
