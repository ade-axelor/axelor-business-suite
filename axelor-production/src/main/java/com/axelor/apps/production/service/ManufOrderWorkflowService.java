/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2017 Axelor (<http://axelor.com>).
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
package com.axelor.apps.production.service;

import com.axelor.app.production.db.IManufOrder;
import com.axelor.app.production.db.IOperationOrder;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.ProductService;
import com.axelor.apps.production.db.CostSheet;
import com.axelor.apps.production.db.ManufOrder;
import com.axelor.apps.production.db.OperationOrder;
import com.axelor.apps.production.db.repo.ManufOrderRepository;
import com.axelor.apps.production.db.repo.OperationOrderRepository;
import com.axelor.apps.production.db.repo.ProductionConfigRepository;
import com.axelor.apps.production.service.app.AppProductionService;
import com.axelor.apps.production.service.config.ProductionConfigService;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class ManufOrderWorkflowService {
	protected OperationOrderWorkflowService operationOrderWorkflowService;
	protected OperationOrderRepository operationOrderRepo;
	protected ManufOrderStockMoveService manufOrderStockMoveService;
	protected ManufOrderRepository manufOrderRepo;

	protected LocalDateTime now;

	@Inject
	public ManufOrderWorkflowService(OperationOrderWorkflowService operationOrderWorkflowService, OperationOrderRepository operationOrderRepo,
									 ManufOrderStockMoveService manufOrderStockMoveService, ManufOrderRepository manufOrderRepo,
									 AppProductionService appProductionService) {
		this.operationOrderWorkflowService = operationOrderWorkflowService;
		this.operationOrderRepo = operationOrderRepo;
		this.manufOrderStockMoveService = manufOrderStockMoveService;
		this.manufOrderRepo = manufOrderRepo;

		now = appProductionService.getTodayDateTime().toLocalDateTime();
	}


	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public ManufOrder plan(ManufOrder manufOrder) throws AxelorException {
		if (manufOrder.getOperationOrderList() != null) {
			for (OperationOrder operationOrder : manufOrder.getOperationOrderList()) {
				operationOrderWorkflowService.plan(operationOrder);
			}
		}

		manufOrder.setPlannedEndDateT(this.computePlannedEndDateT(manufOrder));

		if (!manufOrder.getIsConsProOnOperation()) {
			manufOrderStockMoveService.createToConsumeStockMove(manufOrder);
		}

		manufOrderStockMoveService.createToProduceStockMove(manufOrder);
		manufOrder.setStatusSelect(IManufOrder.STATUS_PLANNED);
		manufOrder.setManufOrderSeq(Beans.get(ManufOrderService.class).getManufOrderSeq());

		return manufOrderRepo.save(manufOrder);
	}

	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void start(ManufOrder manufOrder) throws AxelorException {

		manufOrder.setRealStartDateT(Beans.get(AppProductionService.class).getTodayDateTime().toLocalDateTime());

		int beforeOrAfterConfig = Beans
				.get(ProductionConfigService.class)
				.getProductionConfig(manufOrder.getCompany())
				.getStockMoveRealizeOrderSelect();
		if (beforeOrAfterConfig == ProductionConfigRepository.REALIZE_START) {
			manufOrderStockMoveService.finishStockMove(manufOrder.getInStockMove());
		}
		manufOrder.setStatusSelect(IManufOrder.STATUS_IN_PROGRESS);
		manufOrderRepo.save(manufOrder);
	}

	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void pause(ManufOrder manufOrder) {
		if (manufOrder.getOperationOrderList() != null) {
			for (OperationOrder operationOrder : manufOrder.getOperationOrderList()) {
				if (operationOrder.getStatusSelect() == IOperationOrder.STATUS_IN_PROGRESS) {
					operationOrderWorkflowService.pause(operationOrder);
				}
			}
		}

		manufOrder.setStatusSelect(IManufOrder.STATUS_STANDBY);
		manufOrderRepo.save(manufOrder);
	}

	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void resume(ManufOrder manufOrder) {
		if (manufOrder.getOperationOrderList() != null) {
			for (OperationOrder operationOrder : manufOrder.getOperationOrderList()) {
				if (operationOrder.getStatusSelect() == IOperationOrder.STATUS_STANDBY) {
					operationOrderWorkflowService.resume(operationOrder);
				}
			}
		}

		manufOrder.setStatusSelect(IManufOrder.STATUS_IN_PROGRESS);
		manufOrderRepo.save(manufOrder);
	}

	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void finish(ManufOrder manufOrder) throws AxelorException {
		if (manufOrder.getOperationOrderList() != null) {
			for (OperationOrder operationOrder : manufOrder.getOperationOrderList()) {
				if (operationOrder.getStatusSelect() != IOperationOrder.STATUS_FINISHED) {
					if (operationOrder.getStatusSelect() != IOperationOrder.STATUS_IN_PROGRESS && operationOrder.getStatusSelect() != IOperationOrder.STATUS_STANDBY) {
						operationOrderWorkflowService.start(operationOrder);
					}

					operationOrderWorkflowService.finish(operationOrder);
				}
			}
		}

		manufOrderStockMoveService.finish(manufOrder);
		//create cost sheet
		CostSheet costSheet = Beans.get(CostSheetService.class).computeCostPrice(manufOrder);

		//update price in product
        Product product = manufOrder.getProduct();
		product.setLastProductionPrice(costSheet.getCostPrice());

		//update costprice in product
		if(product.getCostTypeSelect() == ProductRepository.COST_TYPE_LAST_PRODUCTION_PRICE){
			product.setCostPrice(product.getLastProductionPrice());
			if (product.getAutoUpdateSalePrice()) {
				Beans.get(ProductService.class).updateSalePrice(product);
			}
		}

		manufOrder.setRealEndDateT(Beans.get(AppProductionService.class).getTodayDateTime().toLocalDateTime());
		manufOrder.setStatusSelect(IManufOrder.STATUS_FINISHED);
		manufOrderRepo.save(manufOrder);
	}

	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void cancel(ManufOrder manufOrder) throws AxelorException {
		if (manufOrder.getOperationOrderList() != null) {
			for (OperationOrder operationOrder : manufOrder.getOperationOrderList()) {
				if (operationOrder.getStatusSelect() != IOperationOrder.STATUS_CANCELED) {
					operationOrderWorkflowService.cancel(operationOrder);
				}
			}
		}

		manufOrderStockMoveService.cancel(manufOrder);
		manufOrder.setStatusSelect(IManufOrder.STATUS_CANCELED);
		manufOrderRepo.save(manufOrder);
	}
	
	
	public LocalDateTime computePlannedEndDateT(ManufOrder manufOrder)  {
		
		OperationOrder lastOperationOrder = getLastOperationOrder(manufOrder);
		
		if(lastOperationOrder != null)  {
			
			return lastOperationOrder.getPlannedEndDateT();
			
		}
		
		return manufOrder.getPlannedStartDateT();
		
	}
	
	@Transactional
	public void allOpFinished(ManufOrder manufOrder) throws AxelorException  {
		int count = 0;
		List<OperationOrder> operationOrderList = manufOrder.getOperationOrderList();
		for (OperationOrder operationOrderIt : operationOrderList) {
			if(operationOrderIt.getStatusSelect() == IOperationOrder.STATUS_FINISHED){
				count++;
			}
		}

		if(count == operationOrderList.size()){
			this.finish(manufOrder);
		}
	}

	/**
	 * Returns last operation order (highest priority) of given {@link ManufOrder}
	 *
	 * @param manufOrder A manufacturing order
	 * @return Last operation order of {@code manufOrder}
	 */
	public OperationOrder getLastOperationOrder(ManufOrder manufOrder) {
		return operationOrderRepo.all().filter("self.manufOrder = ?", manufOrder).order("-plannedEndDateT").fetchOne();
	}

	/**
	 * Update planned dates.
	 * 
	 * @param manufOrder
	 * @param plannedStartDateT
	 */
	@Transactional(rollbackOn = { AxelorException.class, Exception.class })
	public void updatePlannedDates(ManufOrder manufOrder, LocalDateTime plannedStartDateT) {
		manufOrder.setPlannedStartDateT(plannedStartDateT);

		if (manufOrder.getOperationOrderList() != null) {
			for (OperationOrder operationOrder : manufOrder.getOperationOrderList()) {
				operationOrder.setPlannedStartDateT(null);
				operationOrder.setPlannedEndDateT(null);
				operationOrder.setPlannedDuration(null);
			}

			for (OperationOrder operationOrder : manufOrder.getOperationOrderList()) {
				operationOrder
						.setPlannedStartDateT(operationOrderWorkflowService.getLastOperationOrder(operationOrder));
				operationOrder.setPlannedEndDateT(operationOrderWorkflowService.computePlannedEndDateT(operationOrder));
				operationOrder.setPlannedDuration(operationOrderWorkflowService.getDuration(
						Duration.between(operationOrder.getPlannedStartDateT(), operationOrder.getPlannedEndDateT())));
			}
		}

		manufOrder.setPlannedEndDateT(computePlannedEndDateT(manufOrder));
	}

}
