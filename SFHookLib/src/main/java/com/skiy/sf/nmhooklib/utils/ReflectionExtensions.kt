package com.skiy.sf.nmhooklib.utils

import com.skiy.sf.nmhooklib.SFHook
import com.skiy.sf.nmhooklib.data.SFHookParams
import java.lang.reflect.Member
import java.lang.reflect.Method

val EMPTY: (SFHookParams) -> Unit = {}

fun Class<*>.findMethodByName(methodName: String): Method? {
    return this.declaredMethods.find { it.name == methodName }
}

infix fun Member.replaceMethodReturnTo(replacement: ((SFHookParams) -> Any?)) {
    SFHook.hook(targetMember = this, replacement = replacement)
}

infix fun Member.beforeMethodCalled(beforeMethodCalled: (SFHookParams) -> Unit) {
    SFHook.hook(targetMember = this, beforeMethodCalled = beforeMethodCalled)
}

infix fun Member.afterMethodCalled(afterMethodCalled: (SFHookParams) -> Unit) {
    SFHook.hook(targetMember = this, afterMethodCalled = afterMethodCalled)
}