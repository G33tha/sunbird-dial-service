package managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StaffRoomMgr {

    private static Map<String, Object> timeTable = new HashMap<>();

    private ObjectMapper mapper = new ObjectMapper();
    public StaffRoomMgr() {
        loadTimeTable();
    }

    public Map<String,Object> getPeriodDetails(String topic) throws Exception {
        /*String studentId = "";
        if(id.startsWith("STU"))
            studentId = id;*/
       // Map<String,Object> request = prepareRequest(visitorID, periodId, studentId, classId, date);

        //Map<String, Object> request = prepareReq(topic);

        Map<String, Object> request = prepareReq(topic);

        System.out.println(mapper.writeValueAsString(request));

        HttpResponse<String> httpResponse = Unirest.post("http://52.172.188.118:8082/druid/v2").header
                ("Content-Type", "application/json").body(mapper
                .writeValueAsString(request))
                .asString();

        List<Map<String, Object>> resultList = (List<Map<String, Object>>) mapper.readValue(httpResponse.getBody(), List
                .class);
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> perfDetails = new ArrayList<>();

        for(Map<String, Object> result : resultList) {
            Map<String, Object> event = (Map<String, Object>) result.get("event");
            perfDetails.add(new HashMap<String, Object>(){{
                put("studentId", event.get("studentName"));
                put("rate", event.get("average_performance"));
            }});
        }

        response.put("performanceDetails", perfDetails);


        System.out.println("Response :::::" + response);
        return response;
    }

    private Map<String,Object> prepareResponse(List<Map<String, Object>> resultList) {
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, Object>> attendanceDetails = new ArrayList<>();
        List<Map<String, Object>> engagementDetails = new ArrayList<>();
        List<Map<String, Object>> performanceDetails = new ArrayList<>();
        float attCount= 0.0f;
        String period = "";

        Map<String, List<Float>> egTopic = new HashMap<>();
        Map<String, List<Float>> pfTopic = new HashMap<>();

        if(null != resultList && !resultList.isEmpty()){
                for(Map<String, Object> result: resultList) {
                    Map<String, Object> event = (Map<String, Object>) result.get("event");
                    String eid = (String) event.get("eid");
                    period = (String) event.get("period");
                    switch(eid) {
                        case "DC_ATTENDANCE" : attendanceDetails.add(new HashMap<String, Object>(){{
                            put("studentId" , event.get("studentId"));
                            put("topics" , event.get("topics"));
                            put("present" , (100 == (Integer.parseInt((String) event.get("edata_value"))))? "Yes":
                                    "No");
                        }});attCount+=1; break;

                        case "DC_PERFORMANCE" : performanceDetails.add(new HashMap<String, Object>(){{
                            put("studentId" , event.get("studentId"));
                            put("topics" , event.get("topics"));
                            put("rate" , event.get("edata_value"));
                        }});
                        if(null == pfTopic.get(event.get("topics"))){
                            pfTopic.put((String) event.get("topics"), new ArrayList<>());
                        }
                        pfTopic.get(event.get("topics")).add(Float.parseFloat((String) event.get
                                ("edata_value"))); break;

                        case "DC_ENGAGEMENT" : engagementDetails.add(new HashMap<String, Object>(){{
                            put("studentId" , event.get("studentId"));
                            put("topics" , event.get("topics"));
                            put("rate" , event.get("edata_value"));
                        }});
                            if(null == egTopic.get(event.get("topics"))){
                                egTopic.put((String) event.get("topics"), new ArrayList<>());
                            }
                            egTopic.get(event.get("topics")).add(Float.parseFloat((String) event.get
                                    ("edata_value"))); break;
                    }


                }
        }

        /*List<Map<String, Object>> tt = (List<Map<String, Object>>) timeTable.get(date+ "_" + id);

        for(Map<String, Object> p: tt){
            if(StringUtils.equalsIgnoreCase(period, (String) p.get("period"))){
                resultMap.putAll(p);
            }
        }*/


        resultMap.put("topics", egTopic.keySet());
        float finalAttCount = attCount;
        /*resultMap.put("engagement", new HashMap<String, Object>(){{
            put("topics", new ArrayList<Map<String, Object>>(){{
                for(String topic: egTopic.keySet()){
                    float sum = egTopic.get(topic).stream().reduce(0f, Float::sum);
                    add(new HashMap<String, Object>(){{
                        put("id", topic);
                        put("score", (100.0f*sum)/(finalAttCount * 100.0f));
                    }});
                }
            }});
        }});*/

        resultMap.put("performance", new HashMap<String, Object>(){{
            put("topics", new ArrayList<Map<String, Object>>(){{
                for(String topic: pfTopic.keySet()){
                    float sum = pfTopic.get(topic).stream().reduce(0f, Float::sum);
                    add(new HashMap<String, Object>(){{
                        put("id", topic);
                        put("score", (100.0f*sum)/(finalAttCount * 100.0f));
                    }});
                }
            }});
        }});

        //if(id.startsWith("STU"))
           // resultMap.put("attendance", ((attCount*100.0)/1.0));
        //else
            //resultMap.put("attendance", ((attCount*100.0)/5.0));
        //resultMap.put("attendanceDetails", attendanceDetails);
        //resultMap.put("engagementDetails", engagementDetails);
        resultMap.put("performanceDetails", performanceDetails);
        return  resultMap;

    }

    private Map<String,Object> prepareRequest(String visitorID, String periodId, String studentId, String classId,
                                              String date) {
        return new HashMap<String, Object>() {{
            put("queryType","groupBy");
            put("dataSource","telemetry");
            put("granularity","all");
            put("dimensions", Arrays.asList("eid", "visitorName", "studentId","studentName", "period", "topics",
                    "classroomId", "edata_value"));
            put("aggregations", new ArrayList<>());
            put("filter", new HashMap<String, Object>(){{
                put("type", "and");
                put("fields", new ArrayList<Map<String, Object>>(){{
                    add(new HashMap<String, Object>(){{
                        put("type", "selector");
                        put("dimension", "period");
                        put("value", periodId);
                    }});
                    add(new HashMap<String, Object>(){{
                        put("type", "selector");
                        put("dimension", "classroomId");
                        put("value", classId);
                    }});
                    if(StringUtils.isNotBlank(visitorID)){
                        add(new HashMap<String, Object>(){{
                            put("type", "selector");
                            put("dimension", "visitorId");
                            put("value", visitorID);
                        }});
                    }
                    if(StringUtils.isNotBlank(studentId)){
                        add(new HashMap<String, Object>(){{
                            put("type", "selector");
                            put("dimension", "studentId");
                            put("value", studentId);
                        }});
                    }

                    add(new HashMap<String, Object>(){{
                        put("type", "or");
                        put("fields", new ArrayList<Map<String, Object>>(){{
                            add(new HashMap<String, Object>(){{
                                put("type", "selector");
                                put("dimension", "eid");
                                put("value", "DC_ENGAGEMENT");
                            }});
                            add(new HashMap<String, Object>(){{
                                put("type", "selector");
                                put("dimension", "eid");
                                put("value", "DC_PERFORMANCE");
                            }});
                            add(new HashMap<String, Object>(){{
                                put("type", "selector");
                                put("dimension", "eid");
                                put("value", "DC_ATTENDANCE");
                            }});
                        }});
                    }});
                }});
            }});
            put("intervals", date + "T00:00:00.000/" + date +"T23:59:59.000");
        }};
    }


    private Map<String, Object> prepareReq(String topic) throws IOException {
        String requestStr = "{\"queryType\":\"groupBy\",\"dataSource\":\"telemetry\"," +
                "\"dimensions\":[\"studentName\"]," +
                "\"metrics\":[],\"limitSpec\":{\"type\":\"default\",\"limit\":200,\"columns\":[\"studentName\"," +
                "\"average_performance\"]},\"filter\":{\"type\":\"and\",\"fields\":[{\"type\":\"selector\"," +
                "\"dimension\":\"eid\",\"value\":\"DC_PERFORMANCE\"},{\"type\":\"selector\",\"dimension\":\"topics\"," +
                "\"value\":\"" + topic + "\"}]},\"aggregations\":[{\"type\":\"count\",\"name\":\"rowCount\"}," +
                "{\"type\":\"doubleSum\",\"name\":\"total_edata_value\",\"fieldName\":\"edata_value\"}],\"postAggregations\":[{\"type\":\"arithmetic\",\"name\":\"average_performance\",\"fn\":\"/\",\"fields\":[{\"type\":\"fieldAccess\",\"name\":\"total_edata_value\",\"fieldName\":\"total_edata_value\"},{\"type\":\"fieldAccess\",\"name\":\"rowCount\",\"fieldName\":\"rowCount\"}]}],\"granularity\":\"all\",\"intervals\":[\"2018-01-24T00:00:00.000/2020-01-24T23:59:59.000\"],\"pagingSpec\":{\"pagingIdentifiers\":{},\"threshold\":100}}";

        Map<String, Object> request = mapper.readValue(requestStr, Map.class);
        return request;
    }



    public Map<String,Object> getTimeTable(String date, String id) {
        return new HashMap<String, Object>() {{
            put(date+ "_" + id, timeTable.get(date+ "_" + id));
        }};

    }




    private void loadTimeTable() {
        timeTable.put("2019-01-23_TCH1", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T10:00:00+00:00");
                put("end", "2019-01-23T11:00:00+00:00");
                put("grade", "Class 3");
                put("subject","EVS");
                put("period", "PTCH1_1");
                put("topics", Arrays.asList("Taste", "Smell"));
                put("students", Arrays.asList("STU1", "STU2", "STU3", "STU4", "STU5"));
            }});
        }});

        timeTable.put("2019-01-23_TCH2", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T11:00:00+00:00");
                put("end", "2019-01-23T12:00:00+00:00");
                put("grade", "Class 4");
                put("subject","Geography");
                put("period", "PTCH2_1");
                put("topics", Arrays.asList("Sun", "Planet", "Earth"));
                put("students", Arrays.asList("STU6", "STU7", "STU8", "STU9", "STU10"));
            }});
        }});

        timeTable.put("2019-01-23_TCH3", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T14:00:00+00:00");
                put("end", "2019-01-23T15:00:00+00:00");
                put("grade", "Class 4");
                put("subject","EVS");
                put("period", "PTCH3_1");
                put("topics", Arrays.asList("Types of Birds"));
                put("students", Arrays.asList("STU6", "STU7", "STU8", "STU9", "STU10"));
            }});
        }});

        timeTable.put("2019-01-23_TCH4", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T14:00:00+00:00");
                put("end", "2019-01-23T15:00:00+00:00");
                put("grade", "Class 5");
                put("subject","Geography");
                put("period", "PTCH4_1");
                put("topics", Arrays.asList("Tropic of cancer"));
                put("students", Arrays.asList("STU11", "STU12", "STU13", "STU14", "STU15"));
            }});
        }});

        timeTable.put("2019-01-23_TCH5", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T15:00:00+00:00");
                put("end", "2019-01-23T16:00:00+00:00");
                put("grade", "Class 8");
                put("subject","Science");
                put("period", "PTCH5_1");
                put("topics", Arrays.asList("Inertia", "Mass"));
                put("students", Arrays.asList("STU16", "STU17", "STU18", "STU19", "STU20"));
            }});
        }});

        timeTable.put("2019-01-24_TCH1", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 3");
                put("subject","EVS");
                put("period", "PTCH1_2");
                put("topics", new ArrayList<>());
                put("students", Arrays.asList("STU1", "STU2", "STU3", "STU4", "STU5"));
            }});
        }});

        timeTable.put("2019-01-24_TCH2", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 4");
                put("subject","Geography");
                put("period", "PTCH2_2");
                put("topics", new ArrayList<>());
                put("students", Arrays.asList("STU6", "STU7", "STU8", "STU9", "STU10"));
            }});
        }});

        timeTable.put("2019-01-24_TCH3", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 4");
                put("subject","EVS");
                put("period", "PTCH3_2");
                put("topics", new ArrayList<>());
                put("students", Arrays.asList("STU6", "STU7", "STU8", "STU9", "STU10"));
            }});
        }});

        timeTable.put("2019-01-24_TCH4", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 5");
                put("subject","Geography");
                put("period", "PTCH4_2");
                put("topics", new ArrayList<>());
                put("students", Arrays.asList("STU11", "STU12", "STU13", "STU14", "STU15"));
            }});
        }});

        timeTable.put("2019-01-24_TCH5", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 8");
                put("subject","Science");
                put("period", "PTCH5_2");
                put("topics", new ArrayList<>());
                put("students", Arrays.asList("STU16", "STU17", "STU18", "STU19", "STU20"));
            }});
        }});

        timeTable.put("2019-01-23_STU1", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T10:00:00+00:00");
                put("end", "2019-01-23T11:00:00+00:00");
                put("grade", "Class 3");
                put("subject","EVS");
                put("period", "PTCH1_1");
                put("teacherId", "TCH1");
                put("topics", Arrays.asList("Taste", "Smell"));
            }});
        }});

        timeTable.put("2019-01-23_STU2", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T10:00:00+00:00");
                put("end", "2019-01-23T11:00:00+00:00");
                put("grade", "Class 3");
                put("subject","EVS");
                put("period", "PTCH1_1");
                put("teacherId", "TCH1");
                put("topics", Arrays.asList("Taste", "Smell"));
            }});
        }});

        timeTable.put("2019-01-23_STU3", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T10:00:00+00:00");
                put("end", "2019-01-23T11:00:00+00:00");
                put("grade", "Class 3");
                put("subject","EVS");
                put("period", "PTCH1_1");
                put("teacherId", "TCH1");
                put("topics", Arrays.asList("Taste", "Smell"));
            }});
        }});

        timeTable.put("2019-01-23_STU4", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T10:00:00+00:00");
                put("end", "2019-01-23T11:00:00+00:00");
                put("grade", "Class 3");
                put("subject","EVS");
                put("period", "PTCH1_1");
                put("teacherId", "TCH1");
                put("topics", Arrays.asList("Taste", "Smell"));
            }});
        }});

        timeTable.put("2019-01-23_STU5", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T10:00:00+00:00");
                put("end", "2019-01-23T11:00:00+00:00");
                put("grade", "Class 3");
                put("subject","EVS");
                put("period", "PTCH1_1");
                put("teacherId", "TCH1");
                put("topics", Arrays.asList("Taste", "Smell"));
            }});
        }});

        timeTable.put("2019-01-24_STU1", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 3");
                put("subject","EVS");
                put("period", "PTCH1_2");
                put("teacherId", "TCH1");
                put("topics", new ArrayList<>());
            }});
        }});

        timeTable.put("2019-01-24_STU2", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 3");
                put("subject","EVS");
                put("period", "PTCH1_2");
                put("teacherId", "TCH1");
                put("topics", new ArrayList<>());
            }});
        }});

        timeTable.put("2019-01-24_STU3", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 3");
                put("subject","EVS");
                put("period", "PTCH1_2");
                put("teacherId", "TCH1");
                put("topics", new ArrayList<>());
            }});
        }});

        timeTable.put("2019-01-24_STU4", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 3");
                put("subject","EVS");
                put("period", "PTCH1_2");
                put("teacherId", "TCH1");
                put("topics", new ArrayList<>());
            }});
        }});

        timeTable.put("2019-01-24_STU5", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 3");
                put("subject","EVS");
                put("period", "PTCH1_2");
                put("teacherId", "TCH1");
                put("topics", new ArrayList<>());
            }});
        }});


        timeTable.put("2019-01-23_STU6", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T11:00:00+00:00");
                put("end", "2019-01-23T12:00:00+00:00");
                put("grade", "Class 3");
                put("subject","Geography");
                put("period", "PTCH2_1");
                put("topics", Arrays.asList("Sun", "Planet", "Earth"));
                put("teacherId", "TCH2");
            }});
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T14:00:00+00:00");
                put("end", "2019-01-23T15:00:00+00:00");
                put("grade", "Class 4");
                put("subject","EVS");
                put("period", "PTCH3_1");
                put("topics", Arrays.asList("Types of Birds"));
                put("teacherId", "TCH3");
            }});
        }});

        timeTable.put("2019-01-23_STU7", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T11:00:00+00:00");
                put("end", "2019-01-23T12:00:00+00:00");
                put("grade", "Class 3");
                put("subject","Geography");
                put("period", "PTCH2_1");
                put("topics", Arrays.asList("Sun", "Planet", "Earth"));
                put("teacherId", "TCH2");
            }});
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T14:00:00+00:00");
                put("end", "2019-01-23T15:00:00+00:00");
                put("grade", "Class 4");
                put("subject","EVS");
                put("period", "PTCH3_1");
                put("topics", Arrays.asList("Types of Birds"));
                put("teacherId", "TCH3");
            }});
        }});

        timeTable.put("2019-01-23_STU8", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T11:00:00+00:00");
                put("end", "2019-01-23T12:00:00+00:00");
                put("grade", "Class 3");
                put("subject","Geography");
                put("period", "PTCH2_1");
                put("topics", Arrays.asList("Sun", "Planet", "Earth"));
                put("teacherId", "TCH2");
            }});
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T14:00:00+00:00");
                put("end", "2019-01-23T15:00:00+00:00");
                put("grade", "Class 4");
                put("subject","EVS");
                put("period", "PTCH3_1");
                put("topics", Arrays.asList("Types of Birds"));
                put("teacherId", "TCH3");
            }});
        }});

        timeTable.put("2019-01-23_STU9", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T11:00:00+00:00");
                put("end", "2019-01-23T12:00:00+00:00");
                put("grade", "Class 3");
                put("subject","Geography");
                put("period", "PTCH2_1");
                put("topics", Arrays.asList("Sun", "Planet", "Earth"));
                put("teacherId", "TCH2");
            }});
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T14:00:00+00:00");
                put("end", "2019-01-23T15:00:00+00:00");
                put("grade", "Class 4");
                put("subject","EVS");
                put("period", "PTCH3_1");
                put("topics", Arrays.asList("Types of Birds"));
                put("teacherId", "TCH3");
            }});
        }});

        timeTable.put("2019-01-23_STU10", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T11:00:00+00:00");
                put("end", "2019-01-23T12:00:00+00:00");
                put("grade", "Class 3");
                put("subject","Geography");
                put("period", "PTCH2_1");
                put("topics", Arrays.asList("Sun", "Planet", "Earth"));
                put("teacherId", "TCH2");
            }});
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T14:00:00+00:00");
                put("end", "2019-01-23T15:00:00+00:00");
                put("grade", "Class 4");
                put("subject","EVS");
                put("period", "PTCH3_1");
                put("topics", Arrays.asList("Types of Birds"));
                put("teacherId", "TCH3");
            }});
        }});


        timeTable.put("2019-01-24_STU6", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 4");
                put("subject","Geography");
                put("period", "PTCH2_2");
                put("topics", new ArrayList<>());
                put("teacherId", "TCH2");
            }});
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 4");
                put("subject","EVS");
                put("period", "PTCH3_2");
                put("topics", new ArrayList<>());
                put("teacherId", "TCH3");
            }});
        }});

        timeTable.put("2019-01-24_STU7", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 4");
                put("subject","Geography");
                put("period", "PTCH2_2");
                put("topics", new ArrayList<>());
                put("teacherId", "TCH2");
            }});
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 4");
                put("subject","EVS");
                put("period", "PTCH3_2");
                put("topics", new ArrayList<>());
                put("teacherId", "TCH3");
            }});
        }});

        timeTable.put("2019-01-24_STU8", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 4");
                put("subject","Geography");
                put("period", "PTCH2_2");
                put("topics", new ArrayList<>());
                put("teacherId", "TCH2");
            }});
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 4");
                put("subject","EVS");
                put("period", "PTCH3_2");
                put("topics", new ArrayList<>());
                put("teacherId", "TCH3");
            }});
        }});

        timeTable.put("2019-01-24_STU9", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 4");
                put("subject","Geography");
                put("period", "PTCH2_2");
                put("topics", new ArrayList<>());
                put("teacherId", "TCH2");
            }});
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 4");
                put("subject","EVS");
                put("period", "PTCH3_2");
                put("topics", new ArrayList<>());
                put("teacherId", "TCH3");
            }});
        }});

        timeTable.put("2019-01-24_STU10", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 4");
                put("subject","Geography");
                put("period", "PTCH2_2");
                put("topics", new ArrayList<>());
                put("teacherId", "TCH2");
            }});
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 4");
                put("subject","EVS");
                put("period", "PTCH3_2");
                put("topics", new ArrayList<>());
                put("teacherId", "TCH3");
            }});
        }});


        timeTable.put("2019-01-23_STU11", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T14:00:00+00:00");
                put("end", "2019-01-23T15:00:00+00:00");
                put("grade", "Class 5");
                put("subject","Geography");
                put("period", "PTCH4_1");
                put("teacherId", "TCH4");
                put("topics", Arrays.asList("Tropic of cancer"));
            }});
        }});

        timeTable.put("2019-01-23_STU12", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T14:00:00+00:00");
                put("end", "2019-01-23T15:00:00+00:00");
                put("grade", "Class 5");
                put("subject","Geography");
                put("period", "PTCH4_1");
                put("teacherId", "TCH4");
                put("topics", Arrays.asList("Tropic of cancer"));
            }});
        }});

        timeTable.put("2019-01-23_STU13", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T14:00:00+00:00");
                put("end", "2019-01-23T15:00:00+00:00");
                put("grade", "Class 5");
                put("subject","Geography");
                put("period", "PTCH4_1");
                put("teacherId", "TCH4");
                put("topics", Arrays.asList("Tropic of cancer"));
            }});
        }});

        timeTable.put("2019-01-23_STU14", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T14:00:00+00:00");
                put("end", "2019-01-23T15:00:00+00:00");
                put("grade", "Class 5");
                put("subject","Geography");
                put("period", "PTCH4_1");
                put("teacherId", "TCH4");
                put("topics", Arrays.asList("Tropic of cancer"));
            }});
        }});

        timeTable.put("2019-01-23_STU15", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T14:00:00+00:00");
                put("end", "2019-01-23T15:00:00+00:00");
                put("grade", "Class 5");
                put("subject","Geography");
                put("period", "PTCH4_1");
                put("teacherId", "TCH4");
                put("topics", Arrays.asList("Tropic of cancer"));
            }});
        }});




        timeTable.put("2019-01-24_STU11", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 5");
                put("subject","Geography");
                put("period", "PTCH4_2");
                put("teacherId", "TCH4");
                put("topics", new ArrayList<>());
            }});
        }});

        timeTable.put("2019-01-24_STU12", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 5");
                put("subject","Geography");
                put("period", "PTCH4_2");
                put("teacherId", "TCH4");
                put("topics", new ArrayList<>());
            }});
        }});

        timeTable.put("2019-01-24_STU13", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 5");
                put("subject","Geography");
                put("period", "PTCH4_2");
                put("teacherId", "TCH4");
                put("topics", new ArrayList<>());
            }});
        }});

        timeTable.put("2019-01-24_STU14", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 5");
                put("subject","Geography");
                put("period", "PTCH4_2");
                put("teacherId", "TCH4");
                put("topics", new ArrayList<>());
            }});
        }});

        timeTable.put("2019-01-24_STU15", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 5");
                put("subject","Geography");
                put("period", "PTCH4_2");
                put("teacherId", "TCH4");
                put("topics", new ArrayList<>());
            }});
        }});

        timeTable.put("2019-01-23_STU16", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T15:00:00+00:00");
                put("end", "2019-01-23T16:00:00+00:00");
                put("grade", "Class 8");
                put("subject","Science");
                put("period", "PTCH5_1");
                put("teacherId", "TCH5");
                put("topics", Arrays.asList("Inertia", "Mass"));
            }});
        }});

        timeTable.put("2019-01-23_STU17", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T15:00:00+00:00");
                put("end", "2019-01-23T16:00:00+00:00");
                put("grade", "Class 8");
                put("subject","Science");
                put("period", "PTCH5_1");
                put("teacherId", "TCH5");
                put("topics", Arrays.asList("Inertia", "Mass"));
            }});
        }});

        timeTable.put("2019-01-23_STU18", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T15:00:00+00:00");
                put("end", "2019-01-23T16:00:00+00:00");
                put("grade", "Class 8");
                put("subject","Science");
                put("period", "PTCH5_1");
                put("teacherId", "TCH5");
                put("topics", Arrays.asList("Inertia", "Mass"));
            }});
        }});

        timeTable.put("2019-01-23_STU19", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T15:00:00+00:00");
                put("end", "2019-01-23T16:00:00+00:00");
                put("grade", "Class 8");
                put("subject","Science");
                put("period", "PTCH5_1");
                put("teacherId", "TCH5");
                put("topics", Arrays.asList("Inertia", "Mass"));
            }});
        }});

        timeTable.put("2019-01-23_STU20", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-23T15:00:00+00:00");
                put("end", "2019-01-23T16:00:00+00:00");
                put("grade", "Class 8");
                put("subject","Science");
                put("period", "PTCH5_1");
                put("teacherId", "TCH5");
                put("topics", Arrays.asList("Inertia", "Mass"));
            }});
        }});

        timeTable.put("2019-01-24_STU16", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 8");
                put("subject","Science");
                put("period", "PTCH5_2");
                put("teacherId", "TCH5");
                put("topics", new ArrayList<>());
            }});
        }});

        timeTable.put("2019-01-24_STU17", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 8");
                put("subject","Science");
                put("period", "PTCH5_2");
                put("teacherId", "TCH5");
                put("topics", new ArrayList<>());
            }});
        }});

        timeTable.put("2019-01-24_STU18", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 8");
                put("subject","Science");
                put("period", "PTCH5_2");
                put("teacherId", "TCH5");
                put("topics", new ArrayList<>());
            }});
        }});

        timeTable.put("2019-01-24_STU19", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:00+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 8");
                put("subject","Science");
                put("period", "PTCH5_2");
                put("teacherId", "TCH5");
                put("topics", new ArrayList<>());
            }});
        }});

        timeTable.put("2019-01-24_STU20", new ArrayList() {{
            add(new HashMap<String, Object>(){{
                put("start", "2019-01-24T13:00:0>0+00:00");
                put("end", "2019-01-24T18:00:00+00:00");
                put("grade", "Class 8");
                put("subject","Science");
                put("period", "PTCH5_2");
                put("teacherId", "TCH5");
                put("topics", new ArrayList<>());
            }});
        }});

    }


    public Map<String,Object> getQuestions(String topic) throws Exception {
        String url = "https://dev.sunbirded.org/action/composite/v3/search";
        String request = "{\"request\":{\"filters\":{\"objectType\":\"Content\",\"identifier\":[\"do_112694410769801216167\",\"do_112694406199902208165\",\"do_112694412415705088170\",\"do_112694423412555776189\"],\"status\":[]}}}";

        HttpResponse<String> httpResponse = Unirest.post(url).header("Content-Type", "application/json").body(request)
                .asString();
        Map<String, Object> resp = mapper.readValue(httpResponse.getBody(), Map.class);
        return resp;
    }
}
