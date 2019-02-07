package controllers;

import commons.dto.Request;
import managers.StaffRoomMgr;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Results;

import java.util.Map;

public class StaffRoomController extends BaseController {

    private StaffRoomMgr mgr = new StaffRoomMgr();

    public Promise<Result> getPeriodDetails() {

        Request request = getRequest();
        Map<String, Object> response = null;

        /*String visitorID = (String) request.get("visitor");
        String periodId = (String) request.get("periodId");
        String classId = (String) request.get("classId");
        String date = (String) request.get("date");
        String id = (String) request.get("id");*/
        String topic = (String) request.get("topic");
        try {
            response = mgr.getPeriodDetails(topic);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Promise.<Result>pure(Results.ok(Json.toJson(response)).as("application/json"));
    }

    public Promise<Result> getTimeTable(String date, String id) {

        Map<String, Object> response = null;
        try {
            response = mgr.getTimeTable(date, id);
        } catch (Exception e) {
        }
        return Promise.<Result>pure(Results.ok(Json.toJson(response)).as("application/json"));
    }

    public Promise<Result> getQuestions(String topic)  {
        Map<String, Object> response = null;
        try {
            response = mgr.getQuestions(topic);
        } catch (Exception e) {
        }
        return Promise.<Result>pure(Results.ok(Json.toJson(response)).as("application/json"));
    }
}
