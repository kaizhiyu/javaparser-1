package com.sinosun.wdssconfig.thrift.impl;

import com.sinosun.accountmgr.dao.AccountLimitInfo;
import com.sinosun.accountmgr.dao.YqtCorpAccount;
import com.sinosun.contants.ErrorCodeEnum;
import com.sinosun.exception.ServiceException;
import com.sinosun.log.BaseLog;
import com.sinosun.mybatis.core.DBSource;
import com.sinosun.thrift.dto.ThriftParam;
import com.sinosun.thrift.dto.ThriftReturn;
import com.sinosun.thrift.server.core.HeartThriftServerImpl;
import com.sinosun.thrift.server.core.ThriftServer;
import com.sinosun.wdssconfig.constants.BaseZKPathConstants;
import com.sinosun.wdssconfig.service.YqtCorpAccountManager;
import com.sinosun.wdssconfig.thrift.YqtCorpAccountTS;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@ThriftServer(regZkPath = BaseZKPathConstants.WDSSCONFIG_BASE_ZK_PATH)
@Component("accountMgr_YqtCorpAccountTSImpl")
public class YqtCorpAccountTSImpl {
	@Autowired
    private YqtCorpAccountManager yqtCorpAccountManager;

    public ThriftReturn add(ThriftParam YQTCorpAccounts) throws TException {
        try {
        	List<YqtCorpAccount> yqtCorpAccounts = YQTCorpAccounts.getParamObj();
			DBSource.setDBSource("Yqt");
            yqtCorpAccountManager.doAdd(yqtCorpAccounts);
            return ThriftReturn.createSuccess();
        } catch (ServiceException e) {
            BaseLog.getErrorLog().error(e.getMessage(), e);
            return ThriftReturn.createFailed(e.getErrorCode(), e.getMessage());
        }
    }

	public ThriftReturn find8CompanyIdAndTFList(long cpyId, ThriftParam tfList) throws TException {
		List<String> tfIdList = new ArrayList<>();
		if(null != tfList){
			tfIdList = tfList.getParamObj();
		}

		if(cpyId == 0 ) {
			return ThriftReturn.createFailed(ErrorCodeEnum.PARAM.getCode(),"参数不能为空");
		}
		try {
			List<YqtCorpAccount> yqtCorpOpens = yqtCorpAccountManager.find8CompanyIdAndTFList(cpyId,tfIdList);
			return ThriftReturn.createSuccess(yqtCorpOpens);
		} catch (ServiceException e) {
			BaseLog.getErrorLog().error(e.getMessage(), e);
			return ThriftReturn.createFailed(e.getErrorCode(), e.getMessage());
		}	}
}
