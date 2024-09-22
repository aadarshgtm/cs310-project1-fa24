package edu.jsu.mcis.cs310;

import com.github.cliftonlabs.json_simple.*;
import com.opencsv.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class ClassSchedule {

    private final String CSV_FILENAME = "jsu_sp24_v1.csv";
    private final String JSON_FILENAME = "jsu_sp24_v1.json";

    private final String CRN_COL_HEADER = "crn";
    private final String SUBJECT_COL_HEADER = "subject";
    private final String NUM_COL_HEADER = "num";
    private final String DESCRIPTION_COL_HEADER = "description";
    private final String SECTION_COL_HEADER = "section";
    private final String TYPE_COL_HEADER = "type";
    private final String CREDITS_COL_HEADER = "credits";
    private final String START_COL_HEADER = "start";
    private final String END_COL_HEADER = "end";
    private final String DAYS_COL_HEADER = "days";
    private final String WHERE_COL_HEADER = "where";
    private final String SCHEDULE_COL_HEADER = "schedule";
    private final String INSTRUCTOR_COL_HEADER = "instructor";
    private final String SUBJECTID_COL_HEADER = "subjectid";

    public String convertCsvToJsonString(List<String[]> csv) {
        if (csv == null || csv.isEmpty()) {
            return "{}";
        }

        Iterator<String[]> csvIterator = csv.iterator();

        // Headers
        String[] headers = csvIterator.next();

        // Map to store Headers
        Map<String, Integer> headerIndexMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerIndexMap.put(headers[i], i);
        }

        // Validate required headers
        String[] requiredHeaders = {
                TYPE_COL_HEADER, SCHEDULE_COL_HEADER, NUM_COL_HEADER,
                SUBJECT_COL_HEADER, DESCRIPTION_COL_HEADER, CREDITS_COL_HEADER,
                CRN_COL_HEADER, SECTION_COL_HEADER, START_COL_HEADER, END_COL_HEADER,
                DAYS_COL_HEADER, WHERE_COL_HEADER, INSTRUCTOR_COL_HEADER
        };
        for (String header : requiredHeaders) {
            if (!headerIndexMap.containsKey(header)) {
                throw new IllegalArgumentException("Missing required header: " + header);
            }
        }

        JsonObject scheduleTypeMap = new JsonObject();
        JsonObject subjectMap = new JsonObject();
        JsonObject courseMap = new JsonObject();
        JsonArray sectionArray = new JsonArray();

        while (csvIterator.hasNext()) {
            String[] rowData = csvIterator.next();

            try {
                String type = getValue(rowData, headerIndexMap, TYPE_COL_HEADER);
                String schedule = getValue(rowData, headerIndexMap, SCHEDULE_COL_HEADER);
                if (!scheduleTypeMap.containsKey(type)) {
                    scheduleTypeMap.put(type, schedule);
                }

                String num = getValue(rowData, headerIndexMap, NUM_COL_HEADER);
                String subjectId = num.replaceAll("\\d", "").replaceAll("\\s", "");
                if (!subjectMap.containsKey(subjectId)) {
                    String subjectHeader = getValue(rowData, headerIndexMap, SUBJECT_COL_HEADER);
                    subjectMap.put(subjectId, subjectHeader);
                }

                String numNoLetters = num.replaceAll("[A-Z]", "").replaceAll("\\s", "");
                if (!courseMap.containsKey(num)) {
                    String description = getValue(rowData, headerIndexMap, DESCRIPTION_COL_HEADER);
                    String creditsStr = getValue(rowData, headerIndexMap, CREDITS_COL_HEADER);
                    int credits = parseInteger(creditsStr, 0);

                    JsonObject course = new JsonObject();
                    course.put(SUBJECTID_COL_HEADER, subjectId);
                    course.put(NUM_COL_HEADER, numNoLetters);
                    course.put(DESCRIPTION_COL_HEADER, description);
                    course.put(CREDITS_COL_HEADER, credits);
                    courseMap.put(num, course);
                }

                String crnStr = getValue(rowData, headerIndexMap, CRN_COL_HEADER);
                int crn = parseInteger(crnStr, 0);
                String sectionHeader = getValue(rowData, headerIndexMap, SECTION_COL_HEADER);
                String start = getValue(rowData, headerIndexMap, START_COL_HEADER);
                String end = getValue(rowData, headerIndexMap, END_COL_HEADER);
                String days = getValue(rowData, headerIndexMap, DAYS_COL_HEADER);
                String where = getValue(rowData, headerIndexMap, WHERE_COL_HEADER);
                String allInstructors = getValue(rowData, headerIndexMap, INSTRUCTOR_COL_HEADER);

                List<String> instructors = new ArrayList<>();
                if (allInstructors != null && !allInstructors.isEmpty()) {
                    instructors = Arrays.asList(allInstructors.split(",\\s*"));
                }
                JsonArray instructorArray = new JsonArray();
                for (String instructor : instructors) {
                    instructorArray.add(instructor);
                }

                JsonObject sectionDetails = new JsonObject();
                sectionDetails.put(CRN_COL_HEADER, crn);
                sectionDetails.put(SECTION_COL_HEADER, sectionHeader);
                sectionDetails.put(START_COL_HEADER, start);
                sectionDetails.put(END_COL_HEADER, end);
                sectionDetails.put(DAYS_COL_HEADER, days);
                sectionDetails.put(WHERE_COL_HEADER, where);
                sectionDetails.put(INSTRUCTOR_COL_HEADER, instructorArray);
                sectionDetails.put(NUM_COL_HEADER, numNoLetters);
                sectionDetails.put(TYPE_COL_HEADER, type);
                sectionDetails.put(SUBJECTID_COL_HEADER, subjectId);

                // Add section object to section array
                sectionArray.add(sectionDetails);
            } catch (Exception e) {
                // Handle exception, e.g., log and skip this row
                continue;
            }
        }

        JsonObject courseListMap = new JsonObject();
        courseListMap.put("scheduletype", scheduleTypeMap);
        courseListMap.put("subject", subjectMap);
        courseListMap.put("course", courseMap);
        courseListMap.put("section", sectionArray);

        return Jsoner.serialize(courseListMap);
    }

    private String getValue(String[] rowData, Map<String, Integer> headerIndexMap, String columnHeader) {
        Integer index = headerIndexMap.get(columnHeader);
        if (index == null || index >= rowData.length) {
            return null;
        }
        return rowData[index];
    }

    private int parseInteger(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String convertJsonToCsvString(JsonObject json) {
        StringWriter writer = new StringWriter();
        CSVWriter csvWriter = new CSVWriter(writer, '\t', '"', '\\', "\n");
        String[] header = {
                CRN_COL_HEADER, SUBJECT_COL_HEADER, NUM_COL_HEADER, DESCRIPTION_COL_HEADER,
                SECTION_COL_HEADER, TYPE_COL_HEADER, CREDITS_COL_HEADER, START_COL_HEADER,
                END_COL_HEADER, DAYS_COL_HEADER, WHERE_COL_HEADER, SCHEDULE_COL_HEADER,
                INSTRUCTOR_COL_HEADER
        };
        csvWriter.writeNext(header);

        JsonObject scheduleTypeMap = (JsonObject) json.get("scheduletype");
        JsonObject subjectMap = (JsonObject) json.get("subject");
        JsonObject courseMap = (JsonObject) json.get("course");
        JsonArray sectionArray = (JsonArray) json.get("section");

        for (Object obj : sectionArray) {
            if (obj instanceof JsonObject) {
                JsonObject sectionDetails = (JsonObject) obj;
                try {
                    String crn = String.valueOf(sectionDetails.get(CRN_COL_HEADER));
                    String subjectId = (String) sectionDetails.get(SUBJECTID_COL_HEADER);
                    String num = subjectId + " " + sectionDetails.get(NUM_COL_HEADER);
                    String section = (String) sectionDetails.get(SECTION_COL_HEADER);
                    String type = (String) sectionDetails.get(TYPE_COL_HEADER);
                    String start = (String) sectionDetails.get(START_COL_HEADER);
                    String end = (String) sectionDetails.get(END_COL_HEADER);
                    String days = (String) sectionDetails.get(DAYS_COL_HEADER);
                    String where = (String) sectionDetails.get(WHERE_COL_HEADER);

                    JsonArray instructorArray = (JsonArray) sectionDetails.get(INSTRUCTOR_COL_HEADER);
                    StringBuilder instructorBuilder = new StringBuilder();
                    for (int j = 0; j < instructorArray.size(); j++) {
                        instructorBuilder.append(instructorArray.getString(j));
                        if (j < instructorArray.size() - 1) {
                            instructorBuilder.append(", ");
                        }
                    }
                    String instructor = instructorBuilder.toString();
                    String schedule = (String) scheduleTypeMap.get(type);

                    JsonObject courseDetails = (JsonObject) courseMap.get(num);
                    String description = (String) courseDetails.get(DESCRIPTION_COL_HEADER);
                    String credits = String.valueOf(courseDetails.get(CREDITS_COL_HEADER));

                    String subjectName = (String) subjectMap.get(subjectId);

                    String[] record = {
                            crn, subjectName, num, description, section, type, credits,
                            start, end, days, where, schedule, instructor
                    };
                    csvWriter.writeNext(record);
                } catch (Exception e) {
                    // Handle exception, e.g., log and skip this section
                    continue;
                }
            }
        }

        return writer.toString();
    }

    public JsonObject getJson() {

        JsonObject json = getJson(getInputFileData(JSON_FILENAME));
        return json;

    }

    public JsonObject getJson(String input) {

        JsonObject json = null;

        try {
            json = (JsonObject) Jsoner.deserialize(input);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return json;

    }

    public List<String[]> getCsv() {

        List<String[]> csv = getCsv(getInputFileData(CSV_FILENAME));
        return csv;

    }

    public List<String[]> getCsv(String input) {

        List<String[]> csv = null;

        try {

            CSVReader reader = new CSVReaderBuilder(new StringReader(input))
                    .withCSVParser(new CSVParserBuilder().withSeparator('\t').build()).build();
            csv = reader.readAll();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return csv;

    }

    public String getCsvString(List<String[]> csv) {

        StringWriter writer = new StringWriter();
        CSVWriter csvWriter = new CSVWriter(writer, '\t', '"', '\\', "\n");

        csvWriter.writeAll(csv);

        return writer.toString();

    }

    private String getInputFileData(String filename) {

        StringBuilder buffer = new StringBuilder();
        String line;

        ClassLoader loader = ClassLoader.getSystemClassLoader();

        try {

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(loader.getResourceAsStream("resources" + File.separator + filename)));

            while ((line = reader.readLine()) != null) {
                buffer.append(line).append('\n');
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return buffer.toString();

    }

}