package com.axelor.apps.fleet.web;

import com.axelor.apps.fleet.db.Calculator;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Singleton;

@Singleton
public class CalculatorController {

  public void addValue(ActionRequest request, ActionResponse response) {

    // Addition of two Numbers

    Calculator calculator = request.getContext().asType(Calculator.class);

    Integer first = calculator.getFirstValue();
    Integer second = calculator.getSecondValue();

    Integer result = first + second;

    response.setValue("result", result);

    /*
    *  set multiple values
    *
    * Map< String, Object> setValues=new HashMap<>();
    setValues.put("firstValue", 15);
    setValues.put("secondValue", 25);

    response.setValues(setValues);*/

  }
}
