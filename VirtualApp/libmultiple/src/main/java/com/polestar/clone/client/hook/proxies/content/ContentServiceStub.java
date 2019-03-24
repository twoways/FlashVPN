package com.polestar.clone.client.hook.proxies.content;

import android.os.Build;

import com.polestar.clone.client.hook.base.BinderInvocationProxy;
import com.polestar.clone.client.hook.base.MethodProxy;
import com.polestar.clone.client.hook.base.ReplaceLastUserIdMethodProxy;
import com.polestar.clone.client.hook.base.ReplaceUidMethodProxy;
import com.polestar.clone.client.hook.base.ResultStaticMethodProxy;
import com.polestar.clone.helper.utils.VLog;

import java.lang.reflect.Method;

import mirror.android.content.IContentService;

/**
 * @author Lody
 *
 * @see IContentService
 */

public class ContentServiceStub extends BinderInvocationProxy {

    public ContentServiceStub() {
        super(IContentService.Stub.asInterface, "content");
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            addMethodProxy(new RegisterContentObserver());
            addMethodProxy(new ResultStaticMethodProxy("notifyChange", null));
            addMethodProxy(new ReplaceLastUserIdMethodProxy("getIsSyncableAsUser"));
            addMethodProxy(new ReplaceLastUserIdMethodProxy("setMasterSyncAutomaticallyAsUser"));
            addMethodProxy(new ReplaceLastUserIdMethodProxy("getSyncAdapterTypesAsUser"));
            addMethodProxy(new ReplaceLastUserIdMethodProxy("getSyncStatusAsUser"));
            addMethodProxy(new ReplaceLastUserIdMethodProxy("isSyncPendingAsUser"));
            addMethodProxy(new ReplaceLastUserIdMethodProxy("putCache"));
            addMethodProxy(new ReplaceLastUserIdMethodProxy("getCache"));
            addMethodProxy(new ReplaceLastUserIdMethodProxy("syncAsUser"));
            addMethodProxy(new ReplaceLastUserIdMethodProxy("cancelSyncAsUser"));
        }
    }

    private static class RegisterContentObserver extends MethodProxy {
        @Override
        public String getMethodName() {
            return "registerContentObserver";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            VLog.logbug("RegisterContentObserver", "RegisterContentObserver hooked");
//            if ("com.whatsapp".equals(VClientImpl.get().getCurrentPackage())){
                return null;
//            }else{
//                return super.call(who, method, args);
//            }
        }
    }
}
