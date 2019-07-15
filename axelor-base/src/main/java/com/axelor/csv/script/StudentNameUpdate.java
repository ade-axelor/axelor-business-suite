package com.axelor.csv.script;

import com.axelor.apps.base.db.Workers;
import java.util.Map;

public class StudentNameUpdate {

  /**
   * This method is called with <code>call</code> attribute from the <code>&lt;input&gt;</code> tag.
   *
   * <p>This method is called for each record being imported.
   *
   * @param bean the bean instance created from the imported record
   * @param values the value map that represents the imported data
   * @return the bean object to persist (in most cases the same bean object)
   */
  public Object changeStudentName(Object bean, Map<String, Object> values) {

    System.err.println("Second Call : call method");

    assert bean instanceof Workers;

    Workers workers = (Workers) bean;

    System.err.println("Title is : " + values.get("title"));

    System.err.println("City is : " + values.get("CITY"));

    System.err.println(
        "Id : "
            + workers.getId()
            + " \t Name : "
            + workers.getName()
            + " \t City : "
            + workers.getCity());

    /*values.put("name", workers.getName()+" Kumar");
    values.put("city", workers.getCity()+" CITY");*/

    workers.setName(values.get("title") + " " + workers.getName() + " Kumar");

    workers.setCity(workers.getCity() + " " + values.get("CITY"));

    return workers;
  }

  /**
   * This method is called with <code>prepare-context</code> attribute from the <code>&lt;input&gt;
   * </code> tag. It prepares the global context.
   */
  public void addTitleToName(Map<String, Object> context) {

    System.err.println("first call : prepare-context");

    context.put("title", "Mr.");
    context.put("CITY", "DREAM CITY");
  }
}
