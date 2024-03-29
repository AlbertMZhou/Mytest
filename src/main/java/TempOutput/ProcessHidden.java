package TempOutput;

import com.google.gson.stream.JsonReader;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import entity.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import util.SingleCollect;
import util.Tuple;
import util.Triple;

import java.io.*;
import java.util.*;

public class ProcessHidden {

    private ProcessHidden(){}

    private static ProcessHidden processHiddeninstance = new ProcessHidden();

    HashMap<String, ArrayList<HiddenEntity>> result = new HashMap<>();

    public void setResult(HashMap<String, ArrayList<HiddenEntity>> result) {
        this.result = result;
    }

    public void addEntity(String qualifiedName, HiddenEntity entity){
        if (this.result.containsKey(qualifiedName)){
            this.result.get(qualifiedName).add(entity);
        }else {
            ArrayList<HiddenEntity> hiddenEntities = new ArrayList<>();
            hiddenEntities.add(entity);
            this.result.put(qualifiedName, hiddenEntities);
        }
    }

    public HashMap<String, ArrayList<HiddenEntity>> getResult(){
        return this.result;
    }

    public static ProcessHidden getProcessHiddeninstance() {
        return processHiddeninstance;
    }

    ArrayList<String> wholeFileList = new ArrayList<>();

    public void setWholeFileList(ArrayList<String> wholeFileList) {
        this.wholeFileList = wholeFileList;
    }

    public ArrayList<String> getWholeFileList(){
        return this.wholeFileList;
    }

    static class HiddenEntity {

        String qualifiedName = null;
        ArrayList<String> hiddenApi = new ArrayList<>();
        ArrayList<String> parameter = new ArrayList<>();
        String rawType = null;
        String originalSignature = null;
        boolean isMatch = false;

        String kind = "";

        public String getQualifiedName() {
            return qualifiedName;
        }

        public void setQualifiedName(String qualifiedName) {
            this.qualifiedName = qualifiedName;
        }

        public ArrayList<String> getHiddenApi() {
            return hiddenApi;
        }

        public void addHiddenApi(String hiddenApi){
            this.hiddenApi.add(hiddenApi);
        }

        public void setHiddenApi(ArrayList<String> hiddenApis) {
            this.hiddenApi = hiddenApis;
        }

        public ArrayList<String> getParameter() {
            return parameter;
        }

        public void setParameter(ArrayList<String> parameter) {
            this.parameter = parameter;
        }

        public String getRawType() {
            return rawType;
        }

        public void setRawType(String rawType) {
            this.rawType = rawType;
        }

        public void setOriginalSignature(String originalSignature) {
            this.originalSignature = originalSignature;
        }

        public String getOriginalSignature() {
            return originalSignature;
        }

        public boolean isMatch() {
            return isMatch;
        }

        public void setMatch(boolean match) {
            isMatch = match;
        }

        public void setKind(String kind){
            this.kind = kind;
        }

        public String getKind() {
            return kind;
        }

        @Override
        public String toString() {
            return "hiddenEntity{" +
                    "qualifiedName='" + qualifiedName + '\'' +
                    ", hiddenApi='" + hiddenApi + '\'' +
                    ", parameter=" + parameter +
                    ", rawType='" + rawType + '\'' +
                    ", originalSignature='" + originalSignature + '\'' +
                    '}';
        }
    }

    public String process_type_signature(String value){
        switch (value) {
            case "Z":
                return "boolean";
            case "B":
                return "byte";
            case "C":
                return "char";
            case "S":
                return "short";
            case "I":
                return "int";
            case "J":
                return "long";
            case "F":
                return "float";
            case "D":
                return "double";
            case "V":
                return "void";
        }
        if (value.startsWith("L")){
            String t = value.substring(1).replace("/", ".").replace("$", ".");
            if (t.contains("java")){
                String[] temp = t.split("\\.");
                t = temp[temp.length - 1];
            }
            if (t.endsWith(";")){
                t = t.substring(0, t.length()-1);
            }
            return t;
        }else if (value.contains("[")){
            return process_type_signature(value.substring(1))+"-";
        }else {
            return "";
        }
//        else if (value.startsWith("Z") || value.startsWith("B") || value.startsWith("C") || value.startsWith("S")
//                || value.startsWith("I") || value.startsWith("J") || value.startsWith("F") || value.startsWith("D") ){
//            process_type_signature(value.substring(0, 1));
//            process_type_signature(value.substring(1));
//        }
    }

    public String process_class(String class_descriptor) {
        if (class_descriptor.contains("$$")){
            //pending
            return null;
        } else if (class_descriptor.contains("$")){
            return class_descriptor.substring(1).replace("$", ".").replace("/", ".").replace(";", "");
        } else {
            return class_descriptor.substring(1).replace("/", ".").replace(";", "");
        }
    }


    public Tuple<String, String> process_field(String field) {
        if (field.contains(":")){
            String fieldName = field.split(":",2)[0];
            String fieldType = process_type_signature(field.split(":", 2)[1]);
            return new Tuple<>(fieldName, fieldType);
        }
        return null;
    }


    public ArrayList<String> process_parameter(String parameter){
        ArrayList<String> result = new ArrayList<>();
        boolean isList = false;
        if (parameter.contains(";")){
            for (String par : parameter.split(";")){
                if (process_type_signature(par).equals("")){
                    for (char s : par.toCharArray()){
                        if (s == '['){
                            isList = true;
                            continue;
                        }
                        if (isList){
                            result.add(process_type_signature("[".concat(String.valueOf(s))));
                            isList = false;
                            continue;
                        }
                        if (String.valueOf(s).equals("L")){
                            break;
                        }
                        result.add(process_type_signature(String.valueOf(s)));
                    }
                    if (par.contains("L")){
                        result.add(process_type_signature("L"+ par.split("L",2)[1]));
                    }
                } else {
                    result.add(process_type_signature(par));
                }
            }
        }
        else if (parameter.contains("[")){
            for (char s : parameter.toCharArray()){
                if (s == '['){
                    isList = true;
                    continue;
                }
                if (isList){
                    result.add(process_type_signature("[".concat(String.valueOf(s))));
                    isList = false;
                    continue;
                }
                result.add(process_type_signature(String.valueOf(s)));
            }
        }
        else {
            for (char s : parameter.toCharArray()){
                result.add(process_type_signature(String.valueOf(s)));
            }
        }
        return result;
    }

    public Triple<String, ArrayList<String>, String> process_method(String method) {
        String methodName;
        ArrayList<String> parameters;
        String returnType;
        if (method.contains("(")) {
            methodName = method.split("\\(")[0];
            if (methodName.equals("<init>")) {
                methodName = "Constructor";
            } else if (methodName.equals("<clinit>")) {
                methodName = "Class_Constructor";
//                return null;
            }
            parameters = process_parameter(method.split("\\(")[1].split("\\)")[0]);
            returnType = process_type_signature(method.split("\\(")[1].split("\\)")[1]);
            return new Triple<>(methodName, parameters, returnType);
        }
        return null;
    }


    public void convertCSV2DB(String csvPath) {
        HiddenEntity entity = null;
        try (CSVReader csvReader = new CSVReader(new FileReader(csvPath));) {
            String[] rows = null;
            while ((rows = csvReader.readNext()) != null) {
                for (String e: rows){
                    if (!e.equals("")){
                        if (e.contains("->")){
                            if (entity != null
                                    && entity.getQualifiedName() != null){
                                this.addEntity(entity.getQualifiedName(), entity);
                            }
                            entity = new HiddenEntity();
                            entity.setOriginalSignature(e);
                            if (process_class(e.split("->", 2)[0]) == null){
                                continue;
                            }
                            String classQualifiedName = process_class(e.split("->", 2)[0]);
                            if (e.split("->", 2)[1].contains("(")){
                                if (process_method(e.split("->", 2)[1]) == null){
                                    continue;
                                }
                                String methodName = process_method(e.split("->", 2)[1]).getLeft();
                                ArrayList<String> parameter = process_method(e.split("->", 2)[1]).getMiddle();
                                String returnType = process_method(e.split("->", 2)[1]).getRight();
                                entity.setKind("Method");
                                if (methodName.equals("Constructor")){
                                    String[] temp = classQualifiedName.split("\\.");
                                    methodName = temp[temp.length - 1];
                                }
                                String entityQualifiedName = classQualifiedName+"."+methodName;
                                if (methodName.equals("Class_Constructor")){
                                    entityQualifiedName = classQualifiedName;
                                    entity.setKind("Type");
                                }
                                entity.setQualifiedName(entityQualifiedName);
                                entity.setParameter(parameter);
                                entity.setRawType(returnType);
                            } else {
                                String fieldName = process_field(e.split("->", 2)[1]).getL();
                                String fieldType = process_field(e.split("->", 2)[1]).getR();
                                entity.setRawType(fieldType);
                                entity.setQualifiedName(classQualifiedName+"."+fieldName);
                                entity.setKind("Field");
                            }
                        }else {
                            if (entity != null && entity.qualifiedName != null){
                                entity.addHiddenApi(e);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
    }

    boolean comparePara(List<String> para1, String[] para2){
        if (para1.size() == 0 && para2.length == 0){
            return true;
        }
        else if (para1.size() == para2.length){
            boolean flag = false;
            for (String s : para2) {
                // s: original method entity parameter type
                for (String value : para1) {
                    // value: hidden entity
                    //Exist Map-String-int in original Method's parameter, however, the hidden file only provide Map
                    if (s.contains(value)) {
                        flag = true;
                        break;
                    } else if ((s.equals("Integer") && value.equals("int")) || (value.equals("Integer") && s.equals("int"))){
                        flag = true;
                        break;
                    }  else if ((s.equals("Boolean") && value.equals("boolean")) || (value.equals("Boolean") && s.equals("boolean"))) {
                        flag = true;
                        break;
                    } else if ((s.equals("Long") && value.equals("long")) || (value.equals("Long") && s.equals("long"))) {
                        flag = true;
                        break;
                    } else if ((s.equals("Byte") && value.equals("byte")) || (value.equals("Byte") && s.equals("byte"))) {
                        flag = true;
                        break;
                    } else if ((s.equals("Character") && value.equals("char")) || (value.equals("Character") && s.equals("char"))) {
                        flag = true;
                        break;
                    } else if ((s.equals("double") && value.equals("Double")) || (value.equals("double") && s.equals("Double"))) {
                        flag = true;
                        break;
                    } else if ((s.equals("float") && value.equals("Float")) || (value.equals("float") && s.equals("Float"))) {
                        flag = true;
                        break;
                    } else if ((s.equals("short") && value.equals("Short")) || (value.equals("short") && s.equals("Short"))) {
                        flag = true;
                        break;
                    } else if (s.contains("-") && value.contains("-") && s.contains(value.replace("-", ""))){
                        flag = true;
                        break;
                    } else {
                        flag = false;
//                        System.out.println(value);
//                        System.out.println(s);
                    }
                }
            }
            return flag;
        }else {
            return false;
        }
    }

    SingleCollect singleCollect = SingleCollect.getSingleCollectInstance();

    public String checkHidden(TypeEntity entity){
        String qualifiedName = entity.getQualifiedName();
        if (entity instanceof ClassEntity && ((ClassEntity) entity).getAnonymousRank()!=0){
            qualifiedName = qualifiedName.replace("Anonymous_Class", String.valueOf(((ClassEntity) entity).getAnonymousRank()));
        }
        if (this.result.containsKey(qualifiedName)){
            if (this.result.get(qualifiedName).size() == 1){
                this.result.get(qualifiedName).get(0).setMatch(true);
//                this.result.get(qualifiedName).get(0).setKind("Type");
                return refactorHidden(this.result.get(qualifiedName).get(0).getHiddenApi());
            }
            else {
                for (HiddenEntity hiddenEntity: this.result.get(qualifiedName)){
                    if (hiddenEntity.getOriginalSignature().contains("<clinit>")){
                        hiddenEntity.setMatch(true);
//                        hiddenEntity.setKind("Type");
                        return refactorHidden(hiddenEntity.getHiddenApi());
                    }
                }
            }
        }
        return null;
    }

    public String checkHidden(MethodEntity entity, String parType){
        String qualifiedName = entity.getQualifiedName();
        BaseEntity parentClass = singleCollect.getEntityById(entity.getParentId());
        if (parentClass instanceof ClassEntity && ((ClassEntity) parentClass).getAnonymousRank() != 0){
            qualifiedName = qualifiedName.replace("Anonymous_Class", String.valueOf(((ClassEntity) parentClass).getAnonymousRank()));
        }
        if (this.result.containsKey(qualifiedName)){
            if (this.result.get(qualifiedName).size() == 1){
                this.result.get(qualifiedName).get(0).setMatch(true);
//                this.result.get(qualifiedName).get(0).setKind("Method");
                return refactorHidden(this.result.get(qualifiedName).get(0).getHiddenApi());
            }
            else {
                for (HiddenEntity hiddenEntity: this.result.get(qualifiedName)){
                    if (entity.getRawType() != null && hiddenEntity.getRawType() != null){
                        if (entity.isConstructor()){
                            if (comparePara(hiddenEntity.getParameter(), parType.split(" "))){
                                hiddenEntity.setMatch(true);
//                                hiddenEntity.setKind("Method");
                                return refactorHidden(hiddenEntity.getHiddenApi());
                            }
                        } else if (JsonString.processRawType(entity.getRawType()).contains(hiddenEntity.getRawType())){
                            if (comparePara(hiddenEntity.getParameter(), parType.split(" "))){
                                hiddenEntity.setMatch(true);
//                                hiddenEntity.setKind("Method");
                                return refactorHidden(hiddenEntity.getHiddenApi());
                            }
                        }

                    }
                }
            }
        }
//        for (HiddenEntity hiddenEntity: this.getResult()){
//            if (entity.getQualifiedName().equals(hiddenEntity.getQualifiedName())
//                    && entity.getRawType().equals(hiddenEntity.getRawType())){
//                if (!hiddenEntity.getParameter().isEmpty() && comparePara(hiddenEntity.getParameter(), parType.split(" "))){
//                    return hiddenEntity.getHiddenApi().toString();
//                }
//            }
//        }
        return null;
    }

    public String checkHidden(VariableEntity entity){
        String qualifiedName = entity.getQualifiedName();
        BaseEntity parentClass = singleCollect.getEntityById(entity.getParentId());
        if (parentClass instanceof ClassEntity && ((ClassEntity) parentClass).getAnonymousRank() != 0){
            qualifiedName = qualifiedName.replace("Anonymous_Class", String.valueOf(((ClassEntity) parentClass).getAnonymousRank()));
        }
        if (this.result.containsKey(qualifiedName)){
            for (HiddenEntity hiddenEntity: this.result.get(qualifiedName)){
                if (JsonString.processRawType(entity.getRawType()).contains(hiddenEntity.getRawType())){
                    hiddenEntity.setMatch(true);
//                    hiddenEntity.setKind("Field");
                    return refactorHidden(hiddenEntity.getHiddenApi());
                }
            }
        }
//        for (HiddenEntity hiddenEntity: this.getResult()){
//            if (entity.getQualifiedName().equals(hiddenEntity.getQualifiedName())
//                    && entity.getRawType().equals(hiddenEntity.getRawType())){
//                return hiddenEntity.getHiddenApi().toString();
//            }
//        }
        return null;
    }

    public String refactorHidden(ArrayList<String> hiddenApi){
        String hidden = "";
        for (String temp: hiddenApi){
            hidden = hidden.concat(temp+" ");
        }
        hidden = hidden.substring(0, hidden.length()-1);
        return hidden;
    }

    public boolean checkBaseHidden(String signature){
        String path = signature.split(";", 2)[0].substring(1);
        if (path.contains("$")){
            path = path.split("\\$", 2)[0];
        }
        for (String filePath : getWholeFileList()){
            if (filePath.endsWith(path+".java")){
                return true;
            }
        }
        return false;
    }

    public void outputResult() throws IOException {
        //output not match
        String fileName = "base-enre-out/hidden-not-match.csv";
        Writer out = null;
        FileOutputStream fileOs = null;
        fileOs = new FileOutputStream(fileName);
        out = new OutputStreamWriter(fileOs, "GBK");
        //字符数组是头行
        CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader( "inBase", "Type", "OriginalSignature", "processName", "processRawType", "processParameter").withQuote(null));
        List<Object> objects = new ArrayList<>();
        for (ArrayList<HiddenEntity> hiddenEntities : this.getResult().values()) {
            for (HiddenEntity entity : hiddenEntities){
                if (!entity.isMatch() && !entity.getOriginalSignature().contains("$$") && !entity.getOriginalSignature().startsWith("Lcom/google/android/collect")
                        && !entity.getOriginalSignature().startsWith("Lcom/google/android/gles_jni") && !entity.getOriginalSignature().startsWith("Lcom/google/android/mms")
                        && !entity.getOriginalSignature().startsWith("Lcom/google/android/rappor") && !entity.getOriginalSignature().startsWith("Lcom/google/android/util")
                        && !entity.getOriginalSignature().startsWith("Lcom/sun") && !entity.getOriginalSignature().startsWith("Ldalvik")
                        && !entity.getOriginalSignature().startsWith("Lgov/nist") && !entity.getOriginalSignature().startsWith("Ljava/")
                        && !entity.getOriginalSignature().startsWith("Ljavax/") && !entity.getOriginalSignature().startsWith("Ljdk/")
                        && !entity.getOriginalSignature().startsWith("Llibcore/") && !entity.getOriginalSignature().startsWith("Lorg/")
                        && !entity.getOriginalSignature().startsWith("Lsun/") && !entity.getOriginalSignature().startsWith("Landroid/Manifest$")){
                    objects.add(checkBaseHidden(entity.getOriginalSignature()));
                    objects.add(entity.getKind());
                    objects.add(entity.getOriginalSignature());
                    objects.add(entity.getQualifiedName());
                    objects.add(entity.getRawType());
                    objects.add(entity.getParameter().toString());
                    //打印一行
                    printer.printRecord(objects);
                    //打印完后注意将数组clear掉
                    objects.clear();
                }
            }

        }
        out.flush();

    }

    public void checkMatch(String qualifiedName, String rawType, String parameterType){
        //check match
        if (ProcessHidden.getProcessHiddeninstance().getResult().containsKey(qualifiedName)){
            for (HiddenEntity hiddenEntity: ProcessHidden.getProcessHiddeninstance().getResult().get(qualifiedName)){
                if (rawType!=null && rawType.equals(hiddenEntity.getRawType())){
                    if (parameterType!=null){
                        String hiddenPars = "";
                        for (String par: hiddenEntity.getParameter()){
                            hiddenPars = hiddenPars.concat(JsonString.processRawType(par)+" ");
                        }
                        if (!hiddenPars.equals("")){
                            hiddenPars = hiddenPars.substring(0, hiddenPars.length()-1);
                        }
                        if (parameterType.equals(hiddenPars)){
                            hiddenEntity.setMatch(true);
                        }
                    } else {
                        hiddenEntity.setMatch(true);
                    }
                }
            }
        }
    }

    public void outputConvertInfo(String outputFilePath) throws IOException {
        Writer out;
        FileOutputStream fileOs;
        fileOs = new FileOutputStream(outputFilePath);
        out = new OutputStreamWriter(fileOs, "GBK");
        //字符数组是头行
        CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("isBase", "Kind","OriginalSignature", "processName", "processRawType", "processParameter").withQuote(null));
        List<Object> objects = new ArrayList<>();
        for (ArrayList<HiddenEntity> hiddenEntities : this.getResult().values()) {
            for (HiddenEntity entity : hiddenEntities){
                objects.add(checkBaseHidden(entity.getOriginalSignature()));
                objects.add(entity.getKind());
                objects.add(entity.getOriginalSignature());
                objects.add(entity.getQualifiedName());
                objects.add(entity.getRawType());
                objects.add(entity.getParameter().toString());
                //打印一行
                printer.printRecord(objects);
                //打印完后注意将数组clear掉
                objects.clear();
            }
        }
        out.flush();
    }

    public static void main(String[] args) throws IOException {
        ProcessHidden processHidden = ProcessHidden.getProcessHiddeninstance();
        processHidden.convertCSV2DB("E:\\Android\\hiddenapi-flags.csv");
        FileReader in = new FileReader("base-enre-out\\base-out-with-hidden.json");
        JsonReader reader = new JsonReader(in);
        reader.beginObject();
        String rootName = null;
        while (reader.hasNext()){
            rootName = reader.nextName();
            if ("variables".equals(rootName)){
                System.out.println("Begin reading files...");
                reader.beginArray();
                while (reader.hasNext()){
                    reader.beginObject();
                    String k = null;
                    String qualifiedName = null;
                    String rawType = null;
                    String parameterType = null;
                    boolean isMatch = false;
                    while (reader.hasNext()){
                        k = reader.nextName();
                        switch (k) {
                            case "hidden":
                                isMatch = true;
                                reader.skipValue();
                                break;
                            case "qualifiedName":
                                qualifiedName = reader.nextString();
                                break;
                            case "rawType":
                                rawType = reader.nextString();
                                break;
                            case "parameter":
                                reader.beginObject();
                                while (reader.hasNext()){
                                    if (reader.nextName().equals("types")){
                                        parameterType = reader.nextString();
                                    } else {
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();
                                break;
                            default:
                                reader.skipValue();
                                break;
                        }
                    }
                    reader.endObject();
                    if (isMatch){
                        processHidden.checkMatch(qualifiedName, rawType, parameterType);
                    }
                }
            } else {
                reader.beginObject();
                while (reader.hasNext()) {
                    System.out.println(reader.nextName() + ":" + reader.nextString());
                }
                reader.endObject();
            }
        }
        //output not match
        String fileName = "base-enre-out\\hidden-not-match.csv";
        Writer out = null;
        FileOutputStream fileOs = null;
        fileOs = new FileOutputStream(fileName);
        out = new OutputStreamWriter(fileOs, "GBK");
        //字符数组是头行
        CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("OriginalSignature", "processName", "processRawType", "processParameter").withQuote(null));
        List<Object> objects = new ArrayList<>();
        for (ArrayList<HiddenEntity> hiddenEntities : processHidden.getResult().values()) {
            for (HiddenEntity entity : hiddenEntities){
                if (!entity.isMatch()){
                    objects.add(entity.getOriginalSignature());
                    objects.add(entity.getQualifiedName());
                    objects.add(entity.getRawType());
                    objects.add(entity.getParameter().toString());
                    //打印一行
                    printer.printRecord(objects);
                    //打印完后注意将数组clear掉
                    objects.clear();
                }
            }

        }
        out.flush();
    }
}
