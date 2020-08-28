package com.liviugheorghe.gettersandsettersgenerator.java;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Generator {


    private final String variableRegex = "(public|private|protected)\\s*(static)?\\s*(final)?\\s*(volatile|transient)?\\s*((?!.*class)[a-zA-Z0-9_$]+)(\\<.*?\\>)?\\s*([a-zA-Z0-9_$,\\s]+)(?!.*\\((.*)\\) \\{)";
    private final String staticVariableRegex = "(static)?";
    private final String variableTypeRegex = "((?!.*class)[a-zA-Z0-9_$]+)(\\<.*?\\>)?";
    private final String variableNameRegex = "([a-zA-Z0-9_$]+)";
    private final Pattern variableRegexPattern = Pattern.compile(variableRegex);

    private final String filename;
    private String className;
    private final StringBuilder fileContent = new StringBuilder();

    public Generator(String filename) {
        this.filename = filename;
        this.accessModifierLevel = AccessModifierLevel.PROTECTED;
    }

    private AccessModifierLevel accessModifierLevel;
    private final List<String> generatedGetters = new ArrayList<>();
    private final List<String> generatedSetters = new ArrayList<>();
    private final List<ClassMember> variables = new ArrayList<>();
    private String constructor;


    public enum AccessModifierLevel {
        PROTECTED,
        PRIVATE
    }

    public enum GenerationStrategy {
        SAME_FILE,
        NEW_FILE
    }

    public void setAccessModifierLevel(AccessModifierLevel accessModifier) {
        this.accessModifierLevel = accessModifier;
    }

    private static class ClassMember {
        private String accessModifier;
        private String name;
        private boolean isStatic;

        public boolean isStatic() {
            return isStatic;
        }

        public void setStatic(boolean isStatic) {
            this.isStatic = isStatic;
        }

        public String getAccessModifier() {
            return accessModifier.trim();
        }

        public void setAccessModifier(String accessModifier) {
            this.accessModifier = accessModifier;
        }

        public String getName() {
            return name.trim();
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type.trim();
        }

        public void setType(String type) {
            this.type = type;
        }

        private String type;

        public ClassMember() {
        }

        @Override
        public String toString() {
            return "ClassMember{" +
                    "accessModifier='" + accessModifier + '\'' +
                    ", name='" + name + '\'' +
                    ", isStatic=" + isStatic +
                    ", type='" + type + '\'' +
                    '}';
        }

        public ClassMember(String accessModifier, String name, String type, boolean isStatic) {
            this.accessModifier = accessModifier;
            this.name = name;
            this.type = type;
            this.isStatic = isStatic;
        }
    }


    public void commitToFile() throws IOException {
        int firstSemicolonPosition  = fileContent.indexOf("{");

        Optional<String> gettersOptional =
        generatedGetters.stream().reduce(
                (accumulator,current) -> accumulator + current
        );

        Optional<String> settersOptional =
                generatedSetters.stream().reduce(
                        (accumulator,current) -> accumulator + current
                );

        fileContent.insert(firstSemicolonPosition+2, constructor);
        int lastSemicolonPosition  = fileContent.lastIndexOf("}");
        fileContent.insert(lastSemicolonPosition-1,gettersOptional.orElse("") + "\n" + settersOptional.orElse(""));

        //System.out.println(fileContent.toString());
        writeToFile(fileContent.toString());

    }

    public void writeToFile(String fileContent) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filename));
        bufferedWriter.write(fileContent);
        bufferedWriter.flush();
        bufferedWriter.close();
    }


    public void generate(GenerationStrategy generationStrategy) throws IOException {

        //Parse file for getting the members variables
        parseFile();
        for(ClassMember variable : variables) {
            generateSetterForVariable(variable);
            generateGetterForVariable(variable);
        }
        generateConstructor();
        commitToFile();
    }

    private void generateSetterForVariable(ClassMember variable) {
        String setter = String.format(
                "\n\tpublic%s void %s(%s %s) {\n\t\tthis.%s = %s;\n\t}\n",
                (variable.isStatic() ? " static" : ""),
                "set"+variable.getName().substring(0,1).toUpperCase()+variable.getName().substring(1),
                variable.getType(),
                variable.getName(),
                variable.getName(),
                variable.getName()
        );
        generatedSetters.add(setter);

    }

    private void generateGetterForVariable(ClassMember variable) {
        String getter = String.format(
                "\n\tpublic%s %s %s() {\n\t\treturn this.%s;\n\t}\n",
                (variable.isStatic() ? " static" : ""),
                variable.getType(),
                "get"+variable.getName().substring(0,1).toUpperCase()+variable.getName().substring(1),
                variable.getName()
        );
        generatedGetters.add(getter);

    }

    private void generateConstructor() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n\tpublic ").append(className).append("(");
        for(int i=0;i<variables.size();i++)
        {
            stringBuilder.append(variables.get(i).getType()).append(" ").append(variables.get(i).getName());
            stringBuilder.append((i < variables.size() -1) ? ", " : ") {\n");
        }

        for(int i=0;i<variables.size();i++)
        {
            stringBuilder.append(String.format("\t\tthis.%s = %s;",variables.get(i).getName(),variables.get(i).getName()));
            stringBuilder.append((i < variables.size() -1) ? "\n" : "\n\t}\n");
        }

        constructor = stringBuilder.toString();
    }

    private void parseFile() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = bufferedReader.readLine()) != null) {


            fileContent.append(line);
            fileContent.append("\n");
            if(className == null) {
                String classNameRegex = "class\\s+[a-zA-Z0-9_$]+";
                Pattern pattern = Pattern.compile(classNameRegex);
                Matcher matcher = pattern.matcher(line);
                if(matcher.find()) {
                    className = getClassName(matcher.group(0));
                }
            }

            Matcher variableMatcher = variableRegexPattern.matcher(line);
            if(!variableMatcher.find()) continue;
            ClassMember variable = new ClassMember();
            if(getVariableAccessModifier(line,variableMatcher).equals("public")) continue;
            variable.setAccessModifier(getVariableAccessModifier(line,variableMatcher));
            variable.setName(getVariableName(line,variableMatcher));
            variable.setType(getVariableType(line,variableMatcher));
            variable.setStatic(isVariableStatic(line,variableMatcher));
            variables.add(variable);

        }
        bufferedReader.close();
    }

    private String getClassName(String group) {
        try {
            return group.split(" ")[1];
        } catch(Exception e) {
            return "";
        }
    }

    private boolean stringMatchesRegex(String regex,String str) {
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(str).find();
    }

    private boolean isVariableStatic(String variable,Matcher variableMatcher) {
        try {
            return stringMatchesRegex(staticVariableRegex,variableMatcher.group(2));
        } catch(Exception e) {
            return false;
        }
    }

    private String getVariableType(String variable,Matcher variableMatcher) {
        try {
            String type = variableMatcher.group(5);
            try {
                String generic  = variableMatcher.group(6);
                if(generic != null)
                    type += generic;
            } catch(Exception ignored) {};
            if(stringMatchesRegex(staticVariableRegex,type))
                return type;
            return "";
        } catch(Exception e) {
            return "";
        }
    }

    private String getVariableName(String variable,Matcher variableMatcher) {
        try {
            String name = variableMatcher.group(7);
            return (name != null) ? name : "";
        } catch(Exception e) {
            return "";
        }
    }

    private String getVariableAccessModifier(String variable,Matcher variableMatcher) {
        String accessModifierRegex = "(public|private|protected)";
        Matcher matcher = Pattern.compile(accessModifierRegex).matcher(variable);
        if(!matcher.find()) return "";
        return matcher.group(0);
    }
}