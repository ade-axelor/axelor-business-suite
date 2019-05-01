/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.fleet.web;

import java.util.List;

import com.axelor.apps.fleet.db.AdharCard;
import com.axelor.apps.fleet.db.Course;
import com.axelor.apps.fleet.db.Hobbies;
import com.axelor.apps.fleet.db.MobileNo;
import com.axelor.apps.fleet.db.Student;
import com.axelor.apps.fleet.db.Vehicle;
import com.axelor.apps.fleet.db.repo.StudentRepository;
import com.axelor.apps.fleet.service.VehicleService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class VehicleController {

  @Inject private VehicleService vehicleService;
  
  @Inject private StudentRepository studentRepository;

  public void setVehicleName(ActionRequest request, ActionResponse response) {
    Vehicle vehicle = request.getContext().asType(Vehicle.class);
    String actualName = vehicleService.setVehicleName(vehicle);
    response.setValue("name", actualName);
  }
  
  
  public void fetchAllStudentData(ActionRequest request, ActionResponse response) {
	 
	  List<Student> listStd=studentRepository.all().fetch();
	  
	  for (Student student : listStd) {
			 
		  System.err.println("Name : "+student.getName());
		 
		  for(MobileNo mobileNo:student.getMobileNoList()) {
			  System.err.println("SimCard : "+mobileNo.getSimcard().getName()+" Mobile No : "+mobileNo.getMobileNo());
		  }
		  
		  System.out.println("Adhar Card No : "+student.getAdharcardNo().getAdharNumber());
		    
		  for(Hobbies hobbies:student.getHobbiesSet()) {
			  System.err.println("Hobbies : "+hobbies.getName());
		  }	  
		  System.out.println("Course : "+student.getCourseName().getCourseName());		
	}  
  }
}
