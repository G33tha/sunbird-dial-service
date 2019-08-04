package managers;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import commons.dto.Response;
import commons.exception.ResponseCode;
import org.apache.commons.lang3.StringUtils;
import utils.CassandraConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DigitalPTMMgr extends BaseManager {

    public Response upsert(Map<String, Object> map) throws Exception {
        insert(map);
        Response respone = getSuccessResponse();
        respone.put("identifier", map.get("visitor"));
        return respone;
    }

    public Response read(String id, String visitorId) throws Exception {
        if (StringUtils.isBlank(id))
            return ERROR("ERR_INVALID_REQUEST",
                    "Invalid Request.", ResponseCode.CLIENT_ERROR);
        Map<String, Object> record = readData(id, visitorId);
        Response resp = getSuccessResponse();
        resp.put("ptm", record);
        return resp;
    }


    public void insert(Map<String, Object> request)
            throws Exception {
        Map<String, Object> data = getInsertData(request);
        Insert query = QueryBuilder.insertInto("devcon", "digital_ptm");
        data.entrySet().forEach(entry -> query.value(entry.getKey(), entry.getValue()));
        Session session = CassandraConnector.getSession();
        session.execute(query);
    }

    private Map<String,Object> getInsertData(Map<String, Object> request) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("visitor", request.get("visitor"));
        data.put("period", request.get("period"));
        data.put("school", request.get("school"));
        data.put("grade", request.get("grade"));
        data.put("subject", request.get("subject"));
        data.put("teacher", request.get("teacher"));
        data.put("student", request.get("student"));
        data.put("notes", request.get("notes"));
        return data;
    }


    public Map<String, Object> readData(String periodId, String visitorId) {
        Select.Where query = QueryBuilder.select().all().from("devcon", "digital_ptm").allowFiltering()
                .where(QueryBuilder.eq("period", periodId)).and(QueryBuilder.eq("visitor", visitorId));

        Session session = CassandraConnector.getSession();
        ResultSet resutls = session.execute(query);
        List<Row> rows = resutls.all();
        if(null == rows || rows.isEmpty()){
            return new HashMap<>();
        }
        Row row = rows.get(0);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("visitor", row.getString("visitor"));
        data.put("period", row.getString("period"));
        data.put("school", row.getString("school"));
        data.put("grade", row.getString("grade"));
        data.put("subject", row.getString("subject"));
        data.put("teacher", row.getString("teacher"));
        data.put("student", row.getString("student"));
        data.put("notes", row.getString("notes"));
        return data;

    }
}
