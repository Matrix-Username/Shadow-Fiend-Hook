package com.skiy.sf.nmhooklib.data

import com.skiy.sf.xposed.XC_MethodHook
import java.lang.reflect.Member

class SFHookParams(val javaParam: XC_MethodHook.MethodHookParam) {

    /** The hooked method/constructor. */
    var method: Member = javaParam.method

    /** The `this` reference for an instance method, or `null` for static methods. */
    var thisObject: Any? = javaParam.thisObject

    /** Arguments to the method call. */
    var args: Array<Any> = javaParam.args

    private var result: Any? = javaParam.result
    private var throwable: Throwable? = javaParam.throwable

    /** Returns the result of the method call. */
    fun getResult(): Any? {
        return result
    }

    /** Modify the result of the method call. */
    fun setResult(result: Any?) {
        javaParam.setResult(result)
    }

    /** Returns the `Throwable` thrown by the method, or `null`. */
    fun getThrowable(): Throwable? {
        return throwable
    }

    /** Returns true if an exception was thrown by the method. */
    fun hasThrowable(): Boolean {
        return throwable != null
    }

    /** Modify the exception thrown of the method call. */
    fun setThrowable(throwable: Throwable?) {
        javaParam.setThrowable(throwable)
    }

    /** Returns the result of the method call, or throws the Throwable caused by it. */
    @Throws(Throwable::class)
    fun getResultOrThrowable(): Any? {
        if (throwable != null)
            throw throwable!!
        return result
    }
}