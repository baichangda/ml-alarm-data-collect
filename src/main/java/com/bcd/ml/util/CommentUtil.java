package com.bcd.ml.util;

import com.bcd.base.exception.BaseRuntimeException;
import com.bcd.parser.impl.gb32960.data.VehicleCommonData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class CommentUtil {

    public static List<String[]> getVehicleCommonDataComment() throws IOException {
        Class curClass = VehicleCommonData.class;
        final String curDirPath = curClass.getResource("").getPath().replace("build" + File.separator + "classes" + File.separator + "java" + File.separator + "main", "src" + File.separator + "main" + File.separator + "java");
        final String curFileJavaName = curDirPath + File.separator + curClass.getSimpleName() + ".java";
        Function<String, List<String[]>> func = path -> {
            try {
                final List<String> allLines = Files.readAllLines(Paths.get(path));
                final List<String[]> curInfos = new ArrayList<>();
                String[] curInfo = null;
                for (String line : allLines) {
                    if (line.contains("//")) {
                        curInfo = new String[3];
                        curInfo[0] = line.trim().substring(2);
                        continue;
                    }
                    if (line.contains(";") && curInfo != null) {
                        final String trim = line.trim();
                        int blank_1_index = trim.lastIndexOf(" ");
                        int blank_2_index = trim.lastIndexOf(" ", blank_1_index - 1);
                        if (blank_2_index == -1) {
                            curInfo[1] = trim.substring(0, blank_1_index);
                        } else {
                            curInfo[1] = trim.substring(blank_2_index + 1, blank_1_index);
                        }
                        curInfo[2] = trim.substring(blank_1_index + 1, trim.length() - 1);
                        curInfos.add(curInfo);
                        curInfo = null;
                    }
                }
                return curInfos;
            } catch (IOException e) {
                throw BaseRuntimeException.getException(e);
            }
        };

        final List<String[]> curFileInfos = func.apply(curFileJavaName);
        List<String[]> resList = new ArrayList<>();
        for (String[] info : curFileInfos) {
            String field_path = curDirPath + File.separator + info[1] + ".java";
            final List<String[]> field_fileInfos = func.apply(field_path);
            for (String[] field_fileInfo : field_fileInfos) {
                String[] content = new String[]{info[0] + "_" + field_fileInfo[0], info[2] + "_" + field_fileInfo[2]};
                resList.add(content);
            }
        }

        return resList;
    }

    public static String toMarkdownTable(List<String[]> contentList) {
        StringBuilder sb = new StringBuilder();
        sb.append("|属性|描述|\n");
        sb.append("|----|----|\n");
        for (String[] strings : contentList) {
            sb.append("|" + strings[1] + "|" + strings[0] + "|\n");
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        System.out.println(toMarkdownTable(getVehicleCommonDataComment()));
    }
}
