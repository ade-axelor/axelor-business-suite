package com.axelor.apps.base.db.repo;

import com.axelor.apps.base.db.Workers;
import java.util.Map;

public class WorkersBasePopulateRepository extends WorkersRepository {

  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {

    if (!context.containsKey("json-enhance")) {
      return json;
    }

    try {
      Long id = (Long) json.get("id");
      Workers workers = find(id);

      json.put("emailKey", workers.getEmailsSet().get(0));
    } catch (Exception e) {

    }

    return json;
  }
}
