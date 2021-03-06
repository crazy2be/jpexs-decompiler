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
 * License along with this library.
 */
package com.jpexs.decompiler.flash.action.swf5;

import com.jpexs.decompiler.flash.SWFInputStream;
import com.jpexs.decompiler.flash.SWFOutputStream;
import com.jpexs.decompiler.flash.action.Action;
import com.jpexs.decompiler.flash.action.ActionList;
import com.jpexs.decompiler.flash.action.model.FunctionActionItem;
import com.jpexs.decompiler.flash.action.parser.ActionParseException;
import com.jpexs.decompiler.flash.action.parser.pcode.FlasmLexer;
import com.jpexs.decompiler.flash.action.parser.script.VariableActionItem;
import com.jpexs.decompiler.flash.exporters.modes.ScriptExportMode;
import com.jpexs.decompiler.flash.helpers.GraphTextWriter;
import com.jpexs.decompiler.graph.GraphSourceItemContainer;
import com.jpexs.decompiler.graph.GraphTargetItem;
import com.jpexs.decompiler.graph.TranslateStack;
import com.jpexs.helpers.Helper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ActionDefineFunction extends Action implements GraphSourceItemContainer {

    public String functionName;
    public String replacedFunctionName;
    public List<String> paramNames = new ArrayList<>();
    public List<String> replacedParamNames;
    //public List<Action> code;
    public int codeSize;
    private int version;
    public List<String> constantPool;

    public ActionDefineFunction(String functionName, List<String> paramNames, int codeSize, int version) {
        super(0x9B, 0);
        this.functionName = functionName;
        this.codeSize = codeSize;
        this.version = version;
        this.paramNames = paramNames;
    }

    public ActionDefineFunction(int actionLength, SWFInputStream sis, int version) throws IOException {
        super(0x9B, actionLength);
        this.version = version;
        functionName = sis.readString("functionName");
        int numParams = sis.readUI16("numParams");
        for (int i = 0; i < numParams; i++) {
            paramNames.add(sis.readString("paramName"));
        }
        codeSize = sis.readUI16("codeSize");
    }

    public ActionDefineFunction(FlasmLexer lexer) throws IOException, ActionParseException {
        super(0x9B, -1);
        functionName = lexString(lexer);
        int numParams = (int) lexLong(lexer);
        for (int i = 0; i < numParams; i++) {
            paramNames.add(lexString(lexer));
        }
        lexBlockOpen(lexer);
    }

    @Override
    public long getHeaderSize() {
        return getBytes(version).length;
    }

    @Override
    public byte[] getBytes(int version) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SWFOutputStream sos = new SWFOutputStream(baos, version);
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        try {
            sos.writeString(functionName);
            sos.writeUI16(paramNames.size());
            for (String s : paramNames) {
                sos.writeString(s);
            }
            sos.writeUI16(codeSize);
            sos.close();

            baos2.write(surroundWithAction(baos.toByteArray(), version));
        } catch (IOException e) {
            throw new Error("This should never happen.", e);
        }
        return baos2.toByteArray();
    }

    @Override
    public String getASMSource(ActionList container, Set<Long> knownAddreses, ScriptExportMode exportMode) {
        StringBuilder paramStr = new StringBuilder();
        for (int i = 0; i < paramNames.size(); i++) {
            paramStr.append("\"").append(Helper.escapeString(paramNames.get(i))).append("\" ");
        }

        return "DefineFunction \"" + Helper.escapeString(functionName) + "\" " + paramNames.size() + " " + paramStr + " {" + (codeSize == 0 ? "\r\n}" : "");// + "\r\n" +Action.actionsToString(getAddress() + getHeaderLength(),getItems(container) , knownAddreses, constantPool, version, hex, getFileAddress() + hdrSize) + "}";
    }

    @Override
    public GraphTextWriter getASMSourceReplaced(ActionList container, Set<Long> knownAddreses, ScriptExportMode exportMode, GraphTextWriter writer) {
        List<String> oldParamNames = paramNames;
        if (replacedParamNames != null) {
            paramNames = replacedParamNames;
        }
        String oldFunctionName = functionName;
        if (replacedFunctionName != null) {
            functionName = replacedFunctionName;
        }
        String ret = getASMSource(container, knownAddreses, exportMode);
        paramNames = oldParamNames;
        functionName = oldFunctionName;
        writer.appendNoHilight(ret);
        return writer;

    }

    @Override
    public void translate(TranslateStack stack, List<GraphTargetItem> output, HashMap<Integer, String> regNames, HashMap<String, GraphTargetItem> variables, HashMap<String, GraphTargetItem> functions, int staticOperation, String path) {
    }

    @Override
    public HashMap<Integer, String> getRegNames() {
        return new HashMap<>();
    }

    @Override
    public void translateContainer(List<List<GraphTargetItem>> content, TranslateStack stack, List<GraphTargetItem> output, HashMap<Integer, String> regNames, HashMap<String, GraphTargetItem> variables, HashMap<String, GraphTargetItem> functions) {
        FunctionActionItem fti = new FunctionActionItem(this, functionName, paramNames, content.get(0), constantPool, 1, new ArrayList<VariableActionItem>());
        //ActionGraph.translateViaGraph(regNames, variables, functions, code, version)
        stack.push(fti);
        functions.put(functionName, fti);
    }

    @Override
    public String toString() {
        return "DefineFunction";
    }

    @Override
    public boolean parseDivision(long size, FlasmLexer lexer) {
        codeSize = (int) (size - getHeaderSize());
        return false;
    }

    @Override
    public List<Long> getContainerSizes() {
        List<Long> ret = new ArrayList<>();
        ret.add((Long) (long) codeSize);
        return ret;
    }

    @Override
    public void setContainerSize(int index, long size) {
        if (index == 0) {
            codeSize = (int) size;
        } else {
            throw new IllegalArgumentException("Index must be 0.");
        }
    }

    @Override
    public String getASMSourceBetween(int pos) {
        return "";
    }

    @Override
    public String getName() {
        return "function";
    }
}
