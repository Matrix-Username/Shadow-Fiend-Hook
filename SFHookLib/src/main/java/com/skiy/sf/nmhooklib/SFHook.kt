package com.skiy.sf.nmhooklib

import com.skiy.sf.nmhooklib.data.SFHookParams
import com.skiy.sf.nmhooklib.utils.EMPTY
import com.skiy.sf.neip.NEIPManager
import com.skiy.sf.xposed.XC_MethodHook
import com.skiy.sf.xposed.XposedBridge
import java.lang.reflect.Member

object SFHook {

    init {
        NEIPManager.reconstructLibs(NEIPManager.getContextReflectively()!!)
    }

    fun hook(
        targetMember: Member,
        replacement: ((SFHookParams) -> Any?) = EMPTY,
        beforeMethodCalled: (SFHookParams) -> Unit = {},
        afterMethodCalled: (SFHookParams) -> Unit = {},
    ) {

        XposedBridge.hookMethod(targetMember, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                beforeMethodCalled(SFHookParams(param))

                if (replacement != EMPTY)
                    param.setResult(replacement.invoke(SFHookParams(param)))
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                afterMethodCalled(SFHookParams(param))
            }
        })

    }



}