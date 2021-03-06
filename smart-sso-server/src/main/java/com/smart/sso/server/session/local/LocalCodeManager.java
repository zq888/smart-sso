package com.smart.sso.server.session.local;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.smart.sso.server.common.CodeContent;
import com.smart.sso.server.common.ExpirationPolicy;
import com.smart.sso.server.common.TimeoutParamter;
import com.smart.sso.server.session.CodeManager;

/**
 * 本地授权码管理
 * 
 * @author Joe
 */
public class LocalCodeManager extends TimeoutParamter implements CodeManager, ExpirationPolicy {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Map<String, DummyCode> codeMap = new ConcurrentHashMap<>();
	
	public LocalCodeManager() {
	    // 默认ST失效为10秒
        this(10);
    }
	
	public LocalCodeManager(int timeout) {
		this.timeout = timeout;
    }

	@Override
	public void generate(String code, String service, String tgt) {
		codeMap.put(code, new DummyCode(new CodeContent(service, tgt), System.currentTimeMillis() + timeout * 1000));
	}

	@Override
	public CodeContent validate(String code) {
		DummyCode dc = codeMap.remove(code);
        if (dc == null || System.currentTimeMillis() > dc.expired) {
            return null;
        }
        return dc.codeContent;
	}
	
	@Scheduled(cron = "0 */1 * * * ?")
	@Override
    public void verifyExpired() {
        for (Entry<String, DummyCode> entry : codeMap.entrySet()) {
            String code = entry.getKey();
            DummyCode dc = entry.getValue();
            // 已过期
            if (System.currentTimeMillis() > dc.expired) {
                codeMap.remove(code);
                logger.debug("code : " + code + "已失效");
            }
        }
    }
	
    private class DummyCode {
    	private CodeContent codeContent;
        private long expired; // 过期时间

        public DummyCode(CodeContent codeContent, long expired) {
            this.codeContent = codeContent;
            this.expired = expired;
        }
    }
}
