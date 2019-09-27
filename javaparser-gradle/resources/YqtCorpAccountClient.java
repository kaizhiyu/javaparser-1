package com.sinosun.wdssconfig.client;

import com.sinosun.accountmgr.dao.AccountLimitInfo;
import com.sinosun.accountmgr.dao.YqtCorpAccount;
import com.sinosun.exception.ProxyException;
import com.sinosun.thrift.client.core.AbstractThrift;
import com.sinosun.thrift.client.core.ThriftClient;
import com.sinosun.thrift.dto.ThriftParam;
import com.sinosun.wdssconfig.constants.BaseZKPathConstants;
import com.sinosun.wdssconfig.thrift.YqtCorpAccountTS;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("accountMgr_YqtCorpAccountClient")
@ThriftClient(regZkPath = BaseZKPathConstants.WDSSCONFIG_BASE_ZK_PATH)
public class YqtCorpAccountClient extends YqtCorpAccountTSImpl {
    public void add(List<YqtCorpAccount> yqtYqtCorpAccounts) throws ProxyException {
        doMethod(new ThriftParam(yqtYqtCorpAccounts));
    }

    public List<YqtCorpAccount> find8CompanyIdAndTFList(long cpyId,List<String> tfIdList) throws ProxyException {
        return doMethod(cpyId,new ThriftParam(tfIdList));
    }

}
