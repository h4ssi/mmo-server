/*
 * Copyright 2016 Florian Hassanen
 *
 * This file is part of mmo-server.
 *
 * mmo-server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * mmo-server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with mmo-server.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package mmo.server.doclet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class JsonExport {
    public static boolean start(RootDoc doc) {
        String outDir = null;

        for (String[] ops : doc.options()) {
            if ("-d".equals(ops[0])) {
                outDir = ops[1];
            }
        }

        File file = new File((outDir == null ? "" : outDir + "/") + "protocol.json");

        List<Message> messages = new LinkedList<>();
        for (ClassDoc c : doc.classes()) {
            if (isProtocolClass(c)) {
                List<Property> properties = new ArrayList<>();
                messages.add(new Message(c.simpleTypeName(), c.commentText(), getServer(c), getClient(c), properties));

                collectProperties(c, properties);
            }
        }

        ObjectMapper m = new ObjectMapper();
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            m.writerWithDefaultPrettyPrinter().writeValue(file, messages);

            return true;
        } catch (IOException e) {
            doc.printError(e.getMessage());
            return false;
        }
    }

    private static void collectProperties(ClassDoc c, List<Property> properties) {
        for (FieldDoc f : c.fields(false)) {
            if (isJsonUnwrapped(f)) {
                collectProperties(f.type().asClassDoc(), properties);
            } else {
                properties.add(new Property(f.name(), f.commentText(), getServer(f), getClient(f)));
            }
        }
        if (c.superclass() != null) {
            collectProperties(c.superclass(), properties);
        }
    }

    private static boolean isJsonUnwrapped(FieldDoc f) {
        for (AnnotationDesc a : f.annotations()) {
            if (JsonUnwrapped.class.getName().equals(a.annotationType().qualifiedTypeName())) {
                return true;
            }
        }
        return false;
    }

    private static String getServer(Doc doc) {
        for (Tag t : doc.tags()) {
            if ("@server".equals(t.name()) || "@both".equals(t.name())) {
                return t.text();
            }
        }
        return null;
    }

    private static String getClient(Doc doc) {
        for (Tag t : doc.tags()) {
            if ("@client".equals(t.name()) || "@both".equals(t.name())) {
                return t.text();
            }
        }
        return null;
    }

    public static int optionLength(String option) {
        if ("-d".equals(option)) {
            return 2;
        }
        return 0;
    }

    private static boolean isProtocolClass(ClassDoc c) {
        for (ClassDoc i : c.interfaces()) {
            if (mmo.server.message.Message.class.getName().equals(i.qualifiedTypeName())) {
                return true;
            }
        }
        return c.superclass() != null && isProtocolClass(c.superclass());
    }
}
