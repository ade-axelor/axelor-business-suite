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
package com.axelor.apps.account.web;

import com.axelor.apps.account.db.Account;
import com.axelor.apps.account.db.AccountChart;
import com.axelor.apps.account.db.AccountConfig;
import com.axelor.apps.account.db.repo.AccountChartRepository;
import com.axelor.apps.account.db.repo.AccountConfigRepository;
import com.axelor.apps.account.db.repo.AccountRepository;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.account.service.AccountChartService;
import com.axelor.apps.base.db.BankDetails;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.CompanyDepartment;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.TradingName;
import com.axelor.apps.base.db.repo.CompanyRepository;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Set;

@Singleton
public class AccountChartController {

  @Inject AccountChartService accountChartsService;

  @Inject CompanyRepository companyRepo;

  @Inject AccountConfigRepository accountConfigRepo;

  @Inject AccountRepository accountRepo;

  @Inject AccountChartRepository accountChartRepo;

  public void installChart(ActionRequest request, ActionResponse response) throws AxelorException {

    AccountConfig accountConfig = request.getContext().asType(AccountConfig.class);
    AccountChart act = accountChartRepo.find(accountConfig.getAccountChart().getId());
    Company company = companyRepo.find(accountConfig.getCompany().getId());
    accountConfig = accountConfigRepo.find(accountConfig.getId());
    List<? extends Account> accountList =
        accountRepo
            .all()
            .filter("self.company.id = ?1 AND self.parentAccount != null", company.getId())
            .fetch();

    if (accountList.isEmpty()) {
      if (accountChartsService.installAccountChart(act, company, accountConfig))
        response.setFlash(I18n.get(IExceptionMessage.ACCOUNT_CHART_1));
      else response.setFlash(I18n.get(IExceptionMessage.ACCOUNT_CHART_2));
      response.setReload(true);

    } else response.setFlash(I18n.get(IExceptionMessage.ACCOUNT_CHART_3));
  }

  public void Test(ActionRequest request, ActionResponse response) throws AxelorException {

    AccountConfig accountConfig = request.getContext().asType(AccountConfig.class);
    AccountChart act = accountChartRepo.find(accountConfig.getAccountChart().getId());
    Company company1 = companyRepo.find(accountConfig.getCompany().getId());

    Company company = companyRepo.find(1l);

    // one to one  ( second side decide what will be return )

    AccountConfig accountConfigObject = company.getAccountConfig();

    // BiDirectional : One to One

    Company bi_company = accountConfigObject.getCompany();

    Currency currencyObject = company.getCurrency(); // many to one  Uni - directional

    /* one to many bidirectional   */

    List<CompanyDepartment> cdList = company.getCompanyDepartmentList();
    for (CompanyDepartment companyDepartment : cdList) {

      // one to many

      Company companyObject = companyDepartment.getCompany();
    }
    company.getTradingNameSet();

    // many to many  ( bi-directional)

    Set<TradingName> tradingNameSet = company.getTradingNameSet();
    for (TradingName tradingName : tradingNameSet) {

      Set<Company> companySet = tradingName.getCompanySet();
    }

    // many to many ( uni-directional )
    Set<BankDetails> bankDetailsSet = company.getBankDetailsSet();
    for (BankDetails bankDetails : bankDetailsSet) {}
  }
}
