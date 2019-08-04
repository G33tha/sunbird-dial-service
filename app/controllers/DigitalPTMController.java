package controllers;

import commons.dto.Request;
import commons.dto.Response;
import managers.DigitalPTMMgr;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.Result;

import java.util.Map;

public class DigitalPTMController extends BaseController  {

    DigitalPTMMgr ptmMgr = new DigitalPTMMgr();

    public Promise<Result> create() {
        String apiId = "ekstep.learning.ptm.create";
        Request request = getRequest();
        try {
            Map<String, Object> map = (Map<String, Object>) request.get("ptm");
            Response response = ptmMgr.upsert(map);
            return getResponseEntity(response, apiId, null);
        } catch (Exception e) {
            return getExceptionResponseEntity(e, apiId, null);
        }
    }

    public Promise<Result> update(String visitor) {
        String apiId = "ekstep.learning.ptm.update";
        Request request = getRequest();
        try {
            Map<String, Object> map = (Map<String, Object>) request.get("ptm");
            map.put("visitor", visitor);
            Response response = ptmMgr.upsert(map);
            return getResponseEntity(response, apiId, null);
        } catch (Exception e) {
            return getExceptionResponseEntity(e, apiId, null);
        }
    }

    public Promise<Result> read(String id, String visitor) {
        String apiId = "ekstep.learning.ptm.info";
        try {
            Response response = ptmMgr.read(id, visitor);
            return getResponseEntity(response, apiId, null);
        } catch (Exception e) {
            return getExceptionResponseEntity(e, apiId, null);
        }
    }
}
